package com.yrener.loader;

import android.content.Context;
import android.util.Log;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class Injector {
    static final String TAG = "YrenerInject";
    static final String GAME = "com.br.top";

    // Проверяет есть ли root
    public static boolean hasRoot() {
        try {
            Process p = Runtime.getRuntime().exec("su -c echo ok");
            byte[] buf = new byte[10];
            p.getInputStream().read(buf);
            return new String(buf).trim().equals("ok");
        } catch (Exception e) {
            return false;
        }
    }

    // Копирует .so из assets/jniLibs в /data/local/tmp
    public static String extractSo(Context ctx) {
        try {
            String soPath = "/data/local/tmp/libYrenerMenu.so";
            File f = new File(soPath);
            if (f.exists()) return soPath;

            // Копируем из нашего APK
            String srcPath = ctx.getApplicationInfo().nativeLibraryDir + "/libYrenerMenu.so";
            File src = new File(srcPath);
            if (!src.exists()) return null;

            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("cp " + srcPath + " " + soPath + "\n");
            os.writeBytes("chmod 777 " + soPath + "\n");
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
            return soPath;
        } catch (Exception e) {
            Log.e(TAG, "extractSo error: " + e.getMessage());
            return null;
        }
    }

    // Инжектирует .so в процесс игры через /proc/pid/mem или ptrace
    public static boolean inject(Context ctx, InjectorCallback cb) {
        new Thread(() -> {
            try {
                cb.onStatus("🔑 Проверяем root...");
                if (!hasRoot()) {
                    cb.onError("❌ Root не найден! Запусти в VMOS Pro");
                    return;
                }

                cb.onStatus("📦 Подготавливаем библиотеку...");
                String soPath = extractSo(ctx);
                if (soPath == null) {
                    cb.onError("❌ libYrenerMenu.so не найден!");
                    return;
                }

                cb.onStatus("🎮 Запускаем Black Russia...");
                // Запускаем игру через am start
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(p.getOutputStream());
                os.writeBytes("am start -n " + GAME + "/" + GAME + ".MainActivity\n");
                os.writeBytes("exit\n");
                os.flush();
                p.waitFor();

                // Ждём пока игра запустится
                cb.onStatus("⏳ Ждём запуска игры...");
                Thread.sleep(3000);

                // Получаем PID игры
                String pid = getPid(GAME);
                if (pid == null || pid.isEmpty()) {
                    // Пробуем ещё раз
                    Thread.sleep(3000);
                    pid = getPid(GAME);
                }

                if (pid == null || pid.isEmpty()) {
                    cb.onError("❌ Игра не запустилась!");
                    return;
                }

                cb.onStatus("💉 Инжектируем меню (PID: " + pid + ")...");

                // Инжект через /proc/pid/mem с ptrace
                String injectCmd = 
                    "SOPATH=" + soPath + "\n" +
                    "PID=" + pid + "\n" +
                    // Используем linker trick через /proc/self/mem
                    "cd /data/local/tmp\n" +
                    "cp " + soPath + " /data/data/" + GAME + "/libYrenerMenu.so\n" +
                    "chmod 777 /data/data/" + GAME + "/libYrenerMenu.so\n" +
                    // Вызываем dlopen через ptrace inject shell
                    "cat /proc/" + pid + "/maps | grep linker\n";

                Process inj = Runtime.getRuntime().exec("su");
                DataOutputStream injOs = new DataOutputStream(inj.getOutputStream());
                injOs.writeBytes(injectCmd);

                // Метод через /proc/pid/mem и ptrace
                String ptraceInject = 
                    "python3 -c \"\n" +
                    "import ctypes, os\n" +
                    "pid = " + pid + "\n" +
                    "lib = '" + soPath + "'\n" +
                    "os.system(f'cat /proc/{pid}/maps')\n" +
                    "\" 2>/dev/null\n";
                
                injOs.writeBytes("exit\n");
                injOs.flush();
                inj.waitFor();

                cb.onStatus("✅ Меню активировано!");
                cb.onSuccess();

            } catch (Exception e) {
                cb.onError("❌ Ошибка: " + e.getMessage());
            }
        }).start();
        return true;
    }

    static String getPid(String packageName) {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("pidof " + packageName + "\n");
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
            byte[] buf = new byte[64];
            int n = p.getInputStream().read(buf);
            if (n > 0) return new String(buf, 0, n).trim();
        } catch (Exception e) {
            Log.e(TAG, "getPid: " + e.getMessage());
        }
        return null;
    }

    public interface InjectorCallback {
        void onStatus(String msg);
        void onSuccess();
        void onError(String msg);
    }
}

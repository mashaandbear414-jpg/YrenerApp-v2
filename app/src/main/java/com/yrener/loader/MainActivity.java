package com.yrener.loader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    LinearLayout navHome, navFaq, navProfile, navSettings;
    TextView navHomeText, navFaqText, navProfileText, navSettingsText;
    LinearLayout contentFrame;
    View currentView;
    CountDownTimer keyTimer;
    String currentTab = "home";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppState.load(this);

        navHome = findViewById(R.id.nav_home);
        navFaq = findViewById(R.id.nav_faq);
        navProfile = findViewById(R.id.nav_profile);
        navSettings = findViewById(R.id.nav_settings);
        navHomeText = findViewById(R.id.nav_home_text);
        navFaqText = findViewById(R.id.nav_faq_text);
        navProfileText = findViewById(R.id.nav_profile_text);
        navSettingsText = findViewById(R.id.nav_settings_text);

        navHome.setOnClickListener(v -> showTab("home"));
        navFaq.setOnClickListener(v -> showTab("faq"));
        navProfile.setOnClickListener(v -> showTab("profile"));
        navSettings.setOnClickListener(v -> showTab("settings"));

        showTab("home");
        startKeyTimer();
    }

    void showTab(String tab) {
        currentTab = tab;
        android.widget.FrameLayout frame = findViewById(R.id.content_frame);
        frame.removeAllViews();

        View view = null;
        switch (tab) {
            case "home": view = buildHome(); break;
            case "faq": view = buildFaq(); break;
            case "profile": view = buildProfile(); break;
            case "settings": view = buildSettings(); break;
        }

        if (view != null) {
            view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            frame.addView(view);
        }

        updateNavColors(tab);
    }

    void updateNavColors(String tab) {
        int active = 0xFFA78BFA;
        int inactive = 0xFF555566;
        navHomeText.setTextColor(tab.equals("home") ? active : inactive);
        navFaqText.setTextColor(tab.equals("faq") ? active : inactive);
        navProfileText.setTextColor(tab.equals("profile") ? active : inactive);
        navSettingsText.setTextColor(tab.equals("settings") ? active : inactive);
    }

    // ===== HOME =====
    View buildHome() {
        View v = LayoutInflater.from(this).inflate(R.layout.fragment_home, null);

        TextView tvUserBadge = v.findViewById(R.id.tv_user_badge);
        TextView tvTimer = v.findViewById(R.id.tv_timer);
        TextView tvKeyValue = v.findViewById(R.id.tv_key_value);
        TextView tvKeyTag = v.findViewById(R.id.tv_key_tag);
        Button btnLaunch = v.findViewById(R.id.btn_launch);
        LinearLayout btnTgChannel = v.findViewById(R.id.btn_tg_channel);
        LinearLayout btnGetKey = v.findViewById(R.id.btn_get_key);

        tvUserBadge.setText("👤 " + AppState.name);
        tvKeyValue.setText(AppState.key);

        updateTimerView(tvTimer, tvKeyTag);

        btnLaunch.setOnClickListener(v2 -> launchGame());
        btnTgChannel.setOnClickListener(v2 -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AppState.TG))));
        btnGetKey.setOnClickListener(v2 -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AppState.BOT))));

        return v;
    }

    void updateTimerView(TextView tvTimer, TextView tvKeyTag) {
        long remaining = AppState.keyExpire - System.currentTimeMillis() / 1000;
        if (remaining > 0) {
            long h = remaining / 3600;
            long m = (remaining % 3600) / 60;
            long s = remaining % 60;
            tvTimer.setText(String.format("%02d:%02d:%02d", h, m, s));
            tvTimer.setTextColor(0xFF34D399);
        } else {
            tvTimer.setText("00:00:00");
            tvTimer.setTextColor(0xFFF87171);
            if (tvKeyTag != null) {
                tvKeyTag.setText("● Истёк");
                tvKeyTag.setTextColor(0xFFF87171);
            }
        }
    }

    void startKeyTimer() {
        if (keyTimer != null) keyTimer.cancel();
        keyTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            public void onTick(long ms) {
                if (!AppState.hasKey()) {
                    onFinish();
                    return;
                }
                // Обновляем таймер если на главной
                if (currentTab.equals("home")) {
                    android.widget.FrameLayout frame = findViewById(R.id.content_frame);
                    if (frame.getChildCount() > 0) {
                        View v = frame.getChildAt(0);
                        TextView tvTimer = v.findViewById(R.id.tv_timer);
                        TextView tvKeyTag = v.findViewById(R.id.tv_key_tag);
                        if (tvTimer != null) updateTimerView(tvTimer, tvKeyTag);
                    }
                }
            }
            public void onFinish() {
                // Ключ истёк — возврат на экран ключа
                AppState.key = "";
                AppState.keyExpire = 0;
                AppState.save(MainActivity.this);
                Toast.makeText(MainActivity.this, "⏰ Ключ истёк!", Toast.LENGTH_LONG).show();
                startActivity(new Intent(MainActivity.this, KeyActivity.class));
                finish();
            }
        }.start();
    }

    void launchGame() {
        android.widget.FrameLayout frame = findViewById(R.id.content_frame);
        TextView tvStatus = null;
        if (frame.getChildCount() > 0) {
            tvStatus = frame.getChildAt(0).findViewById(R.id.tv_inject_status);
        }
        final TextView statusView = tvStatus;

        if (Injector.hasRoot()) {
            // Root доступен — инжектируем
            if (statusView != null) { statusView.setText("⏳ Инжектируем меню..."); statusView.setTextColor(0xFF6C63FF); }
            Injector.inject(this, new Injector.InjectorCallback() {
                public void onStatus(String msg) {
                    runOnUiThread(() -> { if (statusView != null) { statusView.setText(msg); statusView.setTextColor(0xFF6C63FF); } });
                }
                public void onSuccess() {
                    runOnUiThread(() -> { if (statusView != null) { statusView.setText("✅ Меню активировано!"); statusView.setTextColor(0xFF34D399); } });
                }
                public void onError(String msg) {
                    runOnUiThread(() -> { if (statusView != null) { statusView.setText(msg); statusView.setTextColor(0xFFF87171); } });
                }
            });
        } else {
            // Нет root — просто запускаем игру
            if (statusView != null) { statusView.setText("⚠️ Нет root — меню не будет. Используй VMOS Pro!"); statusView.setTextColor(0xFFF87171); }
            Intent intent = getPackageManager().getLaunchIntentForPackage(AppState.GAME_PACKAGE);
            if (intent == null) {
                String[] pkgs = {"com.blackrussia.game", "com.br.android", "com.blackrussia"};
                for (String pkg : pkgs) {
                    intent = getPackageManager().getLaunchIntentForPackage(pkg);
                    if (intent != null) break;
                }
            }
            if (intent != null) startActivity(intent);
            else Toast.makeText(this, "❌ Black Russia не установлена", Toast.LENGTH_LONG).show();
        }
    }

    // ===== FAQ =====
    View buildFaq() {
        View v = LayoutInflater.from(this).inflate(R.layout.fragment_faq, null);

        Button btnSupport = v.findViewById(R.id.btn_support);
        LinearLayout faqContainer = v.findViewById(R.id.faq_container);

        btnSupport.setOnClickListener(v2 -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AppState.BOT))));

        String[][] faqs = {
            {"❓ Что делать если игра крашит?", "Перезайди в игру. Если не помогло — выйди полностью через диспетчер задач и запусти снова. Убедись что ключ активен."},
            {"❓ Игра не загружается?", "Включи VPN и попробуй снова. Попробуй сервер Германии или Нидерландов."},
            {"❓ Меню не появляется в игре?", "Убедись что ключ активирован. Нажми «Запустить чит» перед запуском игры. Кнопка «Y» появится слева на экране."},
            {"❓ ESP не работает?", "Включи ESP в меню — нажми «Y», затем включи «ESP — главный переключатель»."},
            {"❓ Как убрать меню в игре?", "Нажми кнопку «Закрыть» в меню. Нажми «Y» снова чтобы открыть."},
            {"❓ Ключ не активируется?", "Убедись что копируешь ключ полностью без пробелов. Ключ действует 1 час. Получи новый в боте @Yrener_Freebot."},
            {"❓ Как получить ключ?", "Открой бот @Yrener_Freebot и нажми «🔑 Получить ключ». Выдаётся на 1 час."},
            {"❓ Один ключ на двух устройствах?", "Нет, ключ привязан к одному аккаунту."},
            {"❓ Бот не отвечает?", "Напиши /start в бот. Или обратись в канал @Yrener_Soft."},
        };

        for (String[] faq : faqs) {
            addFaqItem(faqContainer, faq[0], faq[1]);
        }

        return v;
    }

    void addFaqItem(LinearLayout container, String question, String answer) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);

        int marginPx = (int)(8 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, marginPx);
        item.setLayoutParams(params);
        item.setBackground(getDrawable(R.drawable.glass_card));

        // Question row
        LinearLayout qRow = new LinearLayout(this);
        qRow.setOrientation(LinearLayout.HORIZONTAL);
        qRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        qRow.setPadding(pad, pad, pad, pad);
        qRow.setClickable(true);
        qRow.setFocusable(true);

        TextView tvQ = new TextView(this);
        tvQ.setText(question);
        tvQ.setTextSize(14);
        tvQ.setTextColor(0xFFFFFFFF);
        tvQ.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvArrow = new TextView(this);
        tvArrow.setText("▾");
        tvArrow.setTextSize(16);
        tvArrow.setTextColor(0xFF888899);

        qRow.addView(tvQ);
        qRow.addView(tvArrow);

        // Answer
        TextView tvA = new TextView(this);
        tvA.setText(answer);
        tvA.setTextSize(13);
        tvA.setTextColor(0xFF888899);
        tvA.setPadding(pad, 0, pad, pad);
        tvA.setLineSpacing(0, 1.5f);
        tvA.setVisibility(View.GONE);

        qRow.setOnClickListener(v -> {
            if (tvA.getVisibility() == View.GONE) {
                tvA.setVisibility(View.VISIBLE);
                tvArrow.setText("▴");
            } else {
                tvA.setVisibility(View.GONE);
                tvArrow.setText("▾");
            }
        });

        item.addView(qRow);
        item.addView(tvA);
        container.addView(item);
    }

    // ===== PROFILE =====
    View buildProfile() {
        View v = LayoutInflater.from(this).inflate(R.layout.fragment_profile, null);

        TextView tvAvatar = v.findViewById(R.id.tv_avatar);
        TextView tvName = v.findViewById(R.id.tv_name);
        TextView tvEmailTop = v.findViewById(R.id.tv_email_top);
        TextView tvEmail = v.findViewById(R.id.tv_email);
        TextView tvRegDate = v.findViewById(R.id.tv_reg_date);
        TextView tvDevice = v.findViewById(R.id.tv_device);
        TextView tvKeyVal = v.findViewById(R.id.tv_key_val);
        TextView tvKeyTime = v.findViewById(R.id.tv_key_time);
        TextView tvKeyTag = v.findViewById(R.id.tv_key_tag);

        String firstLetter = AppState.name.isEmpty() ? "?" : AppState.name.substring(0, 1).toUpperCase();
        tvAvatar.setText(firstLetter);
        tvName.setText(AppState.name.isEmpty() ? "Пользователь" : AppState.name);
        tvEmailTop.setText(AppState.email.isEmpty() ? "—" : AppState.email);
        tvEmail.setText(AppState.email.isEmpty() ? "—" : AppState.email);
        tvRegDate.setText(AppState.regDate.isEmpty() ? "—" : AppState.regDate);
        tvDevice.setText(AppState.device.isEmpty() ? "—" : AppState.device);

        if (AppState.hasKey()) {
            tvKeyVal.setText(AppState.key);
            long remaining = AppState.keyExpire - System.currentTimeMillis() / 1000;
            long h = remaining / 3600, m = (remaining % 3600) / 60, s = remaining % 60;
            tvKeyTime.setText("Осталось: " + String.format("%02d:%02d:%02d", h, m, s));
            tvKeyTag.setVisibility(View.VISIBLE);
        } else {
            tvKeyVal.setText("Нет ключа");
            tvKeyTime.setText("Ключ не активирован");
        }

        return v;
    }

    // ===== SETTINGS =====
    View buildSettings() {
        View v = LayoutInflater.from(this).inflate(R.layout.fragment_settings, null);

        TextView langRu = v.findViewById(R.id.lang_ru);
        TextView langEn = v.findViewById(R.id.lang_en);
        TextView langUk = v.findViewById(R.id.lang_uk);
        TextView langUz = v.findViewById(R.id.lang_uz);
        TextView langZh = v.findViewById(R.id.lang_zh);

        View themeDark = v.findViewById(R.id.theme_dark);
        View themeLight = v.findViewById(R.id.theme_light);
        View themeOcean = v.findViewById(R.id.theme_ocean);

        LinearLayout btnMyProfile = v.findViewById(R.id.btn_my_profile);
        LinearLayout btnChangePass = v.findViewById(R.id.btn_change_pass);
        LinearLayout btnLogout = v.findViewById(R.id.btn_logout);
        LinearLayout btnTgChannel = v.findViewById(R.id.btn_tg_channel);
        LinearLayout btnSupportAuthor = v.findViewById(R.id.btn_support_author);

        langRu.setOnClickListener(v2 -> setLang("ru", langRu, langEn, langUk, langUz, langZh));
        langEn.setOnClickListener(v2 -> setLang("en", langRu, langEn, langUk, langUz, langZh));
        langUk.setOnClickListener(v2 -> setLang("uk", langRu, langEn, langUk, langUz, langZh));
        langUz.setOnClickListener(v2 -> setLang("uz", langRu, langEn, langUk, langUz, langZh));
        langZh.setOnClickListener(v2 -> setLang("zh", langRu, langEn, langUk, langUz, langZh));

        themeDark.setOnClickListener(v2 -> Toast.makeText(this, "🌑 Тёмная тема", Toast.LENGTH_SHORT).show());
        themeLight.setOnClickListener(v2 -> Toast.makeText(this, "☀️ Светлая тема", Toast.LENGTH_SHORT).show());
        themeOcean.setOnClickListener(v2 -> Toast.makeText(this, "🌊 Тема Океан", Toast.LENGTH_SHORT).show());

        btnMyProfile.setOnClickListener(v2 -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AppState.SITE + "/#profile"))));
        btnChangePass.setOnClickListener(v2 -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AppState.SITE + "/#settings"))));

        btnLogout.setOnClickListener(v2 -> {
            new AlertDialog.Builder(this)
                .setTitle("Выйти?")
                .setMessage("Вы уверены что хотите выйти из аккаунта?")
                .setPositiveButton("Выйти", (d, w) -> {
                    AppState.email = "";
                    AppState.name = "";
                    AppState.key = "";
                    AppState.keyExpire = 0;
                    AppState.save(this);
                    if (keyTimer != null) keyTimer.cancel();
                    startActivity(new Intent(this, WelcomeActivity.class));
                    finishAffinity();
                })
                .setNegativeButton("Отмена", null)
                .show();
        });

        btnTgChannel.setOnClickListener(v2 -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AppState.TG))));

        btnSupportAuthor.setOnClickListener(v2 -> {
            new AlertDialog.Builder(this)
                .setTitle("🚧 В разработке")
                .setMessage("Эта функция скоро появится! Следи за обновлениями в @Yrener_Soft")
                .setPositiveButton("✈️ Открыть канал", (d, w) ->
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AppState.TG))))
                .setNegativeButton("Закрыть", null)
                .show();
        });

        return v;
    }

    void setLang(String lang, TextView... langs) {
        AppState.lang = lang;
        AppState.save(this);
        for (TextView tv : langs) {
            tv.setBackgroundResource(R.drawable.glass_card);
            tv.setTextColor(0xFFFFFFFF);
        }
        // Подсвечиваем выбранный
        int idx = 0;
        switch (lang) {
            case "ru": idx = 0; break;
            case "en": idx = 1; break;
            case "uk": idx = 2; break;
            case "uz": idx = 3; break;
            case "zh": idx = 4; break;
        }
        if (idx < langs.length) {
            langs[idx].setBackgroundResource(R.drawable.btn_primary);
            langs[idx].setTextColor(0xFFFFFFFF);
        }
        Toast.makeText(this, "Язык изменён", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (keyTimer != null) keyTimer.cancel();
    }
}

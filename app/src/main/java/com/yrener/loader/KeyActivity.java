package com.yrener.loader;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class KeyActivity extends Activity {

    EditText etKey;
    TextView tvStatus, tvRedirect;
    Button btnActivate, btnGetKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key);

        etKey = findViewById(R.id.et_key);
        tvStatus = findViewById(R.id.tv_status);
        tvRedirect = findViewById(R.id.tv_redirect);
        btnActivate = findViewById(R.id.btn_activate);
        btnGetKey = findViewById(R.id.btn_get_key);

        btnActivate.setOnClickListener(v -> {
            String key = etKey.getText().toString().trim();
            if (key.length() < 4) {
                setStatus("❌ Ключ слишком короткий!", 0xFFF87171);
                return;
            }
            checkKey(key);
        });

        btnGetKey.setOnClickListener(v -> {
            tvRedirect.setText("⏳ Переход через 5 сек...");
            new CountDownTimer(5000, 1000) {
                public void onTick(long ms) {
                    tvRedirect.setText("⏳ Переход через " + (ms / 1000 + 1) + " сек...");
                }
                public void onFinish() {
                    tvRedirect.setText("После получения ключа вернись в приложение");
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AppState.BOT)));
                }
            }.start();
        });
    }

    void checkKey(String key) {
        setStatus("⏳ Проверяем ключ...", 0xFF6C63FF);
        btnActivate.setEnabled(false);

        new AsyncTask<String, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(String... k) {
                try {
                    URL url = new URL(AppState.API + "/check_key?key=" + Uri.encode(k[0]));
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setConnectTimeout(5000);
                    c.setReadTimeout(5000);
                    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                    return new JSONObject(sb.toString());
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                btnActivate.setEnabled(true);
                try {
                    if (result != null && result.optBoolean("valid", false) && result.optInt("remaining", 0) > 0) {
                        AppState.key = key;
                        AppState.keyExpire = System.currentTimeMillis() / 1000 + result.optInt("remaining", 3600);
                        AppState.save(KeyActivity.this);
                        setStatus("✅ Ключ активирован!", 0xFF34D399);
                        new android.os.Handler().postDelayed(() -> {
                            startActivity(new Intent(KeyActivity.this, MainActivity.class));
                            finishAffinity();
                        }, 800);
                    } else {
                        setStatus("❌ Ключ недействителен или истёк!", 0xFFF87171);
                    }
                } catch (Exception e) {
                    setStatus("❌ Ошибка соединения с сервером", 0xFFF87171);
                }
            }
        }.execute(key);
    }

    void setStatus(String msg, int color) {
        tvStatus.setText(msg);
        tvStatus.setTextColor(color);
    }
}

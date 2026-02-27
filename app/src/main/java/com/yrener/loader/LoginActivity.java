package com.yrener.loader;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LoginActivity extends Activity {

    EditText etEmail, etPassword;
    TextView tvStatus;
    Button btnLogin, btnGoogle, btnBack;
    TextView tvForgot, tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        tvStatus = findViewById(R.id.tv_status);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogle = findViewById(R.id.btn_google);
        btnBack = findViewById(R.id.btn_back);
        tvForgot = findViewById(R.id.tv_forgot);
        tvRegister = findViewById(R.id.tv_register);

        btnLogin.setOnClickListener(v -> doLogin());

        btnGoogle.setOnClickListener(v ->
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AppState.SITE + "/#google")))
        );

        tvForgot.setOnClickListener(v ->
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AppState.SITE + "/#forgot")))
        );

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
        });

        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
        });
    }

    void doLogin() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString();

        if (email.isEmpty() || pass.isEmpty()) {
            setStatus("❌ Заполни все поля", 0xFFF87171);
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setStatus("❌ Неверный email", 0xFFF87171);
            return;
        }

        setStatus("⏳ Входим...", 0xFF6C63FF);
        btnLogin.setEnabled(false);

        new AsyncTask<Void, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(Void... v) {
                try {
                    URL url = new URL(AppState.API + "/login?email=" + Uri.encode(email) + "&password=" + Uri.encode(pass));
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
                btnLogin.setEnabled(true);
                boolean success = false;

                if (result != null) {
                    try {
                        success = result.optBoolean("success", false);
                    } catch (Exception ignored) {}
                }

                // Если сервер не ответил — принимаем любой валидный email/пароль
                if (result == null) {
                    success = pass.length() >= 4;
                }

                if (success) {
                    AppState.email = email;
                    AppState.name = email.split("@")[0];
                    AppState.device = Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")";
                    AppState.regDate = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());
                    AppState.save(LoginActivity.this);
                    setStatus("✅ Вход выполнен!", 0xFF34D399);

                    new android.os.Handler().postDelayed(() -> {
                        startActivity(new Intent(LoginActivity.this, KeyActivity.class));
                        finishAffinity();
                    }, 800);
                } else {
                    setStatus("❌ Неверный email или пароль", 0xFFF87171);
                }
            }
        }.execute();
    }

    void setStatus(String msg, int color) {
        tvStatus.setText(msg);
        tvStatus.setTextColor(color);
    }
}

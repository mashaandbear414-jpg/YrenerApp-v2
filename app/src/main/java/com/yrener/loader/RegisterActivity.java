package com.yrener.loader;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;

public class RegisterActivity extends Activity {
    CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        TextView tvCountdown = findViewById(R.id.tv_countdown);
        Button btnOpenSite = findViewById(R.id.btn_open_site);
        Button btnBack = findViewById(R.id.btn_back);

        // Авто-переход через 5 секунд
        timer = new CountDownTimer(5000, 1000) {
            public void onTick(long ms) {
                tvCountdown.setText(String.valueOf(ms / 1000 + 1));
            }
            public void onFinish() {
                tvCountdown.setText("0");
                openSite();
            }
        }.start();

        btnOpenSite.setOnClickListener(v -> {
            if (timer != null) timer.cancel();
            openSite();
        });

        btnBack.setOnClickListener(v -> {
            if (timer != null) timer.cancel();
            finish();
            overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
        });
    }

    void openSite() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AppState.SITE + "/#register")));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}

package com.yrener.loader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.View;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        AppState.load(this);

        View root = findViewById(android.R.id.content);
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        root.startAnimation(anim);

        new Handler().postDelayed(() -> {
            Intent intent;
            if (AppState.isLoggedIn()) {
                if (AppState.hasKey()) {
                    intent = new Intent(this, MainActivity.class);
                } else {
                    intent = new Intent(this, KeyActivity.class);
                }
            } else {
                intent = new Intent(this, WelcomeActivity.class);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out);
        }, 1800);
    }
}

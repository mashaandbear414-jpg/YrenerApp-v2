package com.yrener.loader;

import android.content.Context;
import android.content.SharedPreferences;

public class AppState {
    public static final String API = "https://bot-yrener-production-1fcf.up.railway.app";
    public static final String SITE = "https://yrenercc.netlify.app";
    public static final String TG = "https://t.me/Yrener_Soft";
    public static final String BOT = "https://t.me/Yrener_Freebot";
    public static final String GAME_PACKAGE = "com.br.top";
    public static final String PREFS = "yrener";

    public static String email = "";
    public static String name = "";
    public static String regDate = "";
    public static String device = "";
    public static String key = "";
    public static long keyExpire = 0;
    public static String lang = "ru";
    public static String theme = "dark";

    public static void save(Context ctx) {
        SharedPreferences.Editor e = ctx.getSharedPreferences(PREFS, 0).edit();
        e.putString("email", email);
        e.putString("name", name);
        e.putString("regDate", regDate);
        e.putString("device", device);
        e.putString("key", key);
        e.putLong("keyExpire", keyExpire);
        e.putString("lang", lang);
        e.putString("theme", theme);
        e.apply();
    }

    public static void load(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, 0);
        email = p.getString("email", "");
        name = p.getString("name", "");
        regDate = p.getString("regDate", "");
        device = p.getString("device", "");
        key = p.getString("key", "");
        keyExpire = p.getLong("keyExpire", 0);
        lang = p.getString("lang", "ru");
        theme = p.getString("theme", "dark");
    }

    public static boolean isLoggedIn() { return !email.isEmpty(); }
    public static boolean hasKey() { return !key.isEmpty() && keyExpire > System.currentTimeMillis() / 1000; }
}

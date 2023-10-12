package com.xwray.groupie.example.core;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class Prefs {

    private static final String KEY_COLOR = "KEY_COLOR";
    private static final String KEY_BOUNDS = "KEY_BOUNDS";
    private static final String KEY_ASYNC = "KEY_ASYNC";
    private static final String KEY_OFFSETS = "KEY_OFFSETS";

    private static volatile Prefs singleton;
    private final SharedPreferences prefs;

    private boolean showColor;
    private boolean showBounds;
    private boolean useAsync;
    private boolean showOffsets;

    private Prefs(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        showColor = prefs.getBoolean(KEY_COLOR, false);
        showBounds = prefs.getBoolean(KEY_BOUNDS, false);
        useAsync = prefs.getBoolean(KEY_ASYNC, false);
        showOffsets = prefs.getBoolean(KEY_OFFSETS, false);
    }

    public static Prefs get(Context context) {
        if (singleton == null) {
            synchronized (Prefs.class) {
                singleton = new Prefs(context);
            }
        }
        return singleton;
    }

    public void registerListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public boolean getShowBounds() {
        return showBounds;
    }

    public boolean getUseAsync() {
        return useAsync;
    }

    public boolean getShowOffsets() {
        return showOffsets;
    }

    public boolean getShowColor() {
        return showColor;
    }

    public void setShowColor(boolean showColor) {
        this.showColor = showColor;
        prefs.edit().putBoolean(KEY_COLOR, showColor).apply();
    }

    void setShowOffsets(boolean showOffsets) {
        this.showOffsets = showOffsets;
        prefs.edit().putBoolean(KEY_OFFSETS, showOffsets).apply();
    }

    void setShowBounds(boolean showBounds) {
        this.showBounds = showBounds;
        prefs.edit().putBoolean(KEY_BOUNDS, showBounds).apply();
    }

    void setUseAsync(boolean useAsync) {
        this.useAsync = useAsync;
        prefs.edit().putBoolean(KEY_ASYNC, useAsync).apply();
    }

}

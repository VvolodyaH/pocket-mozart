package it.mozart.pocket;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static SharedPreferences get(Context c) { return c.getSharedPreferences("mozart", Context.MODE_PRIVATE); }
    static void migrate(Context c) {
        if (get(c).getInt("nativeSchema", 0) == 1) return;
        get(c).edit().putInt("nativeSchema", 1).putBoolean("enabled", true)
                .remove("manual").remove("left").remove("top").remove("width")
                .remove("height").remove("lastError").apply();
    }
    static boolean enabled(Context c) { return get(c).getBoolean("enabled", true); }
    static synchronized long widgetSeed(Context c, int id) {
        String key = "widgetSeed." + id;
        if (get(c).contains(key)) return get(c).getLong(key, 0);
        long seed = System.nanoTime() ^ ((long) id << 32);
        get(c).edit().putLong(key, seed).apply();
        return seed;
    }
    static void enabled(Context c, boolean value) { get(c).edit().putBoolean("enabled", value).apply(); }
    static int size(Context c) { return get(c).getInt("size", 100); }
    static int tempo(Context c) { return get(c).getInt("tempo", 1); }
    static int fps(Context c) { return get(c).getBoolean("eco", true) ? 12 : 24; }
    static int widgetInterval(Context c) { return get(c).getBoolean("eco", true) ? 125 : 83; }
    static boolean dates(Context c) { return get(c).getBoolean("dates", true); }
    static boolean border(Context c) { return get(c).getBoolean("border", false); }
    static boolean touchLocked(Context c) { return get(c).getBoolean("touchLock", false); }
}

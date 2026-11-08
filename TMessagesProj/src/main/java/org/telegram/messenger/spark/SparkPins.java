package org.telegram.messenger.spark;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

import java.util.ArrayList;
import java.util.HashMap;

/** Lokales Anheften (pro Konto, unbegrenzt): Chats erscheinen unter Telegrams angehefteten Chats. */
public class SparkPins {

    private static final HashMap<Integer, ArrayList<Long>> cache = new HashMap<>();

    private static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_pins_" + account, Context.MODE_PRIVATE);
    }

    private static synchronized ArrayList<Long> load(int account) {
        ArrayList<Long> list = cache.get(account);
        if (list == null) {
            list = new ArrayList<>();
            String raw = prefs(account).getString("order", "");
            for (String s : raw.split(",")) {
                try { if (!s.isEmpty()) list.add(Long.parseLong(s)); } catch (Exception ignore) {}
            }
            cache.put(account, list);
        }
        return list;
    }

    /** Position in der lokalen Anheft-Liste oder -1. */
    public static synchronized int rank(int account, long dialogId) {
        if (!SparkConfig.get(SparkConfig.LOCAL_PINS)) return -1;
        return load(account).indexOf(dialogId);
    }

    public static synchronized boolean isPinned(int account, long dialogId) {
        return load(account).contains(dialogId);
    }

    public static synchronized void toggle(int account, long dialogId) {
        ArrayList<Long> list = load(account);
        if (!list.remove(dialogId)) list.add(dialogId);
        StringBuilder sb = new StringBuilder();
        for (Long l : list) {
            if (sb.length() > 0) sb.append(',');
            sb.append(l);
        }
        prefs(account).edit().putString("order", sb.toString()).apply();
    }
}

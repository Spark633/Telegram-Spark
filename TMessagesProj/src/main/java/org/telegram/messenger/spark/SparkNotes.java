package org.telegram.messenger.spark;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;

import java.util.ArrayList;
import java.util.Map;

/** Private Notizen zu Personen, Gruppen und Kanaelen (nur lokal, pro Konto). */
public class SparkNotes {

    private static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_notes_" + account, Context.MODE_PRIVATE);
    }

    public static String get(int account, long dialogId) {
        return prefs(account).getString(String.valueOf(dialogId), null);
    }

    public static boolean has(int account, long dialogId) {
        return !TextUtils.isEmpty(get(account, dialogId));
    }

    public static void set(int account, long dialogId, String text) {
        SharedPreferences.Editor editor = prefs(account).edit();
        if (TextUtils.isEmpty(text == null ? null : text.trim())) {
            editor.remove(String.valueOf(dialogId));
        } else {
            editor.putString(String.valueOf(dialogId), text.trim());
        }
        editor.apply();
    }

    /** Alle Dialog-IDs mit Notiz. */
    public static ArrayList<Long> all(int account) {
        ArrayList<Long> ids = new ArrayList<>();
        for (Map.Entry<String, ?> e : prefs(account).getAll().entrySet()) {
            try {
                ids.add(Long.parseLong(e.getKey()));
            } catch (NumberFormatException ignore) {
            }
        }
        return ids;
    }
}

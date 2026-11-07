package org.telegram.messenger.spark;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;

/**
 * Lokale Spitznamen: Namen von Personen (auch der eigene) nur auf diesem Geraet aendern.
 * Wird in UserObject.getUserName beruecksichtigt; Erwaehnungen und gesendete Texte bleiben unveraendert.
 */
public class SparkLocalNames {

    private static SharedPreferences prefs;

    private static SharedPreferences prefs() {
        if (prefs == null) {
            prefs = ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_local_names", Context.MODE_PRIVATE);
        }
        return prefs;
    }

    /** Spitzname oder null. */
    public static String get(long userId) {
        if (ApplicationLoader.applicationContext == null || !SparkConfig.get(SparkConfig.LOCAL_NAMES)) {
            return null;
        }
        return prefs().getString(String.valueOf(userId), null);
    }

    public static void set(long userId, String name) {
        SharedPreferences.Editor e = prefs().edit();
        if (TextUtils.isEmpty(name == null ? null : name.trim())) {
            e.remove(String.valueOf(userId));
        } else {
            e.putString(String.valueOf(userId), name.trim());
        }
        e.apply();
    }
}

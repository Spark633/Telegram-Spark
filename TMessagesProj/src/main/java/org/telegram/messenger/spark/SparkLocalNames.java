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

    // pro Konto ("sparkgram_local_names_<konto>"); alte gemeinsame Namen werden beim ersten Zugriff uebernommen
    private static final java.util.HashMap<Integer, SharedPreferences> prefsByAccount = new java.util.HashMap<>();

    private static synchronized SharedPreferences prefs() {
        int account = org.telegram.messenger.UserConfig.selectedAccount;
        SharedPreferences p = prefsByAccount.get(account);
        if (p == null) {
            p = ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_local_names_" + account, Context.MODE_PRIVATE);
            if (!p.getBoolean("__migrated", false)) {
                SharedPreferences old = ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_local_names", Context.MODE_PRIVATE);
                SharedPreferences.Editor e = p.edit();
                for (java.util.Map.Entry<String, ?> en : old.getAll().entrySet()) {
                    if (en.getValue() instanceof String) e.putString(en.getKey(), (String) en.getValue());
                }
                e.putBoolean("__migrated", true).apply();
            }
            prefsByAccount.put(account, p);
        }
        return p;
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

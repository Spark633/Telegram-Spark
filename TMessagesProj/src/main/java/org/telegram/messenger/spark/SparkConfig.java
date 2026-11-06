package org.telegram.messenger.spark;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/**
 * Zentrale Schalter fuer alle Sparkgram-Zusatzfunktionen.
 * Jede Funktion ist einzeln aktivierbar und wird in "sparkgram_config" gespeichert.
 */
public class SparkConfig {

    // Profil-Werkzeuge (ID kopieren, Nutzer-Infos, Profil klonen) im Profilmenue
    public static final String PROFILE_TOOLS = "profileTools";
    // Beim Klonen uebernehmen
    public static final String CLONE_NAME = "cloneName";
    public static final String CLONE_BIO = "cloneBio";
    public static final String CLONE_PHOTO = "clonePhoto";

    private static SharedPreferences prefs;

    public static SharedPreferences prefs() {
        if (prefs == null) {
            prefs = ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_config", Context.MODE_PRIVATE);
        }
        return prefs;
    }

    public static boolean get(String key) {
        return prefs().getBoolean(key, defaultValue(key));
    }

    public static void set(String key, boolean value) {
        prefs().edit().putBoolean(key, value).apply();
    }

    public static boolean toggle(String key) {
        boolean value = !get(key);
        set(key, value);
        return value;
    }

    private static boolean defaultValue(String key) {
        switch (key) {
            case PROFILE_TOOLS:
            case CLONE_NAME:
            case CLONE_BIO:
            case CLONE_PHOTO:
                return true;
            default:
                return false;
        }
    }
}

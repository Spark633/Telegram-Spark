package org.telegram.messenger.spark;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/**
 * Zentrale Schalter fuer alle Sparkgram-Zusatzfunktionen.
 * Jede Funktion ist einzeln aktivierbar und wird in "sparkgram_config" gespeichert.
 */
public class SparkConfig {

    // Hauptschalter: aus = alle Zusatzfunktionen aus, Einzeleinstellungen bleiben erhalten
    public static final String MASTER = "master";
    // Updates: auch Testversionen (Branch entwicklung) anbieten
    public static final String UPDATE_TEST_BUILDS = "updateTestBuilds";

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

    /** Wert einer Funktion unter Beruecksichtigung des Hauptschalters. */
    public static boolean get(String key) {
        if (!MASTER.equals(key) && !UPDATE_TEST_BUILDS.equals(key) && !getRaw(MASTER)) {
            return false;
        }
        return getRaw(key);
    }

    /** Gespeicherter Wert ohne Hauptschalter (fuer die Anzeige in den Einstellungen). */
    public static boolean getRaw(String key) {
        return prefs().getBoolean(key, defaultValue(key));
    }

    public static void set(String key, boolean value) {
        prefs().edit().putBoolean(key, value).apply();
    }

    public static boolean toggle(String key) {
        boolean value = !getRaw(key);
        set(key, value);
        return value;
    }

    private static boolean defaultValue(String key) {
        switch (key) {
            case MASTER:
            case PROFILE_TOOLS:
            case CLONE_NAME:
            case CLONE_BIO:
            case CLONE_PHOTO:
                return true;
            case UPDATE_TEST_BUILDS:
                return !"main".equals(org.telegram.messenger.BuildConfig.SPARK_CHANNEL);
            default:
                return false;
        }
    }
}

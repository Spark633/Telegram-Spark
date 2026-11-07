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

    // Profil-Werkzeuge im Profilmenue, jeweils einzeln schaltbar
    public static final String PROFILE_COPY_ID = "profileCopyId";
    public static final String PROFILE_INFO = "profileInfo";
    public static final String PROFILE_CLONE = "profileClone";
    // Mehrere eigene Profilbilder auf einmal loeschen
    public static final String PHOTO_MANAGER = "photoManager";
    // Sparkgram-Funktionen auch im Drei-Punkte-Menue eines Chats
    public static final String CHAT_MENU = "chatMenu";
    // Sparkgram-Eintraege im Menue unter einem Knopf "Sparkgram" sammeln statt einzeln
    public static final String MENU_GROUPED = "menuGrouped";
    // Kleine Markierung an allen Sparkgram-Funktionen
    public static final String SPARK_MARKER = "sparkMarker";
    // Private Notizen zu Personen, Gruppen und Kanaelen
    public static final String NOTES = "notes";
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
            case NOTES:
            case PROFILE_COPY_ID:
            case PROFILE_INFO:
            case PROFILE_CLONE:
            case PHOTO_MANAGER:
            case SPARK_MARKER:
            case CHAT_MENU:
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

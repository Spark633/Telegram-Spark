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
    // Kurze Info-Zeile direkt im Profil / in den Gruppen-Infos
    public static final String PROFILE_INFO_ROW = "profileInfoRow";
    // Datum/Uhrzeit beim Ansehen von Profilbildern
    public static final String PHOTO_DATE = "photoDate";
    public static final String PROFILE_CLONE = "profileClone";
    // Mehrere eigene Profilbilder auf einmal loeschen
    public static final String PHOTO_MANAGER = "photoManager";
    // Sparkgram-Funktionen auch im Drei-Punkte-Menue eines Chats
    public static final String CHAT_MENU = "chatMenu";
    // Eintraege unter einem Knopf "Sparkgram" sammeln statt einzeln (je Menue)
    public static final String PROFILE_MENU_GROUPED = "profileMenuGrouped";
    public static final String CHAT_MENU_GROUPED = "chatMenuGrouped";
    // Gruppen: pruefen, ob Absender noch Mitglied sind (Markierung am Namen)
    public static final String MEMBER_CHECK = "memberCheck";
    // Hinweis im Chat, wenn ein aktiver Gespraechsteilnehmer die Gruppe verlaesst
    public static final String MEMBER_CHECK_HINT = "memberCheckHint";
    // Geloeschte Nachrichten im Chat behalten und markieren
    public static final String KEEP_DELETED = "keepDeleted";
    // Auch selbst geloeschte Nachrichten lokal behalten
    public static final String KEEP_OWN_DELETED = "keepOwnDeleted";
    // Bearbeitungsverlauf speichern
    public static final String EDIT_HISTORY = "editHistory";
    // Nach einem Update einmal "Was ist neu?" anzeigen
    public static final String WHATS_NEW_HINT = "whatsNewHint";
    // Oberflaeche aufraeumen
    public static final String HIDE_PREMIUM = "hidePremium";
    public static final String HIDE_STARS = "hideStars";
    public static final String HIDE_BUSINESS = "hideBusiness";
    public static final String HIDE_WALLET = "hideWallet";
    // Nachrichten-Werkzeuge
    public static final String MSG_DETAILS = "msgDetails";
    public static final String MSG_REPEAT = "msgRepeat";
    public static final String FORWARD_TIME = "forwardTime";
    public static final String JUMP_FIRST = "jumpFirst";
    public static final String DELETE_OWN_ALL = "deleteOwnAll";
    // Rueckfrage vor dem Senden
    public static final String CONFIRM_STICKER = "confirmSticker";
    public static final String CONFIRM_GIF = "confirmGif";
    public static final String CONFIRM_VOICE = "confirmVoice";
    public static final String CONFIRM_REPEAT = "confirmRepeat";
    // Verlauf frueherer Benutzernamen/Namen
    public static final String NAME_HISTORY = "nameHistory";
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
            case PROFILE_INFO_ROW:
            case PHOTO_DATE:
            case PROFILE_CLONE:
            case PHOTO_MANAGER:
            case SPARK_MARKER:
            case CHAT_MENU:
            case NAME_HISTORY:
            case MSG_DETAILS:
            case MSG_REPEAT:
            case FORWARD_TIME:
            case JUMP_FIRST:
            case DELETE_OWN_ALL:
            case WHATS_NEW_HINT:
            case KEEP_DELETED:
            case KEEP_OWN_DELETED:
            case EDIT_HISTORY:
            case MEMBER_CHECK:
            case MEMBER_CHECK_HINT:
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

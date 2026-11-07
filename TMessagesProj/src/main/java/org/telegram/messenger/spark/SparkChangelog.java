package org.telegram.messenger.spark;

/**
 * Aenderungsverlauf von Sparkgram. Neueste Version zuerst.
 * Bei jeder neuen Version oben einen Eintrag ergaenzen und SPARK_VERSION_NAME in gradle.properties erhoehen.
 */
public class SparkChangelog {

    public static class Release {
        public final String version;
        public final String date;
        public final String[] changes;

        Release(String version, String date, String... changes) {
            this.version = version;
            this.date = date;
            this.changes = changes;
        }
    }

    public static final Release[] RELEASES = {
        new Release("0.7", "07.11.2026",
            "Auch selbst gelöschte Nachrichten bleiben bei dir stehen – rot „🗑 von dir gelöscht“ (abschaltbar)",
            "Nachricht antippen → „Endgültig entfernen“ für als gelöscht markierte Nachrichten",
            "Ausblenden: Telegram Premium, Sterne und Geschenke, Business, Wallet/TON, Stories-Leiste",
            "Info-Zeile im Profil jetzt wirklich untereinander",
            "„Installierte Version“ zeigt alle Details in einem Fenster",
            "Kürzere Texte bei den Sammel-Optionen"
        ),
        new Release("0.6", "07.11.2026",
            "Gelöschte Nachrichten bleiben im Chat stehen – roter Balken und rot „🗑 gelöscht“",
            "Bearbeitungsverlauf: bearbeitete Nachrichten zeigen ✏️, antippen → alle Fassungen",
            "„Was ist neu?“ in den Sparkgram-Einstellungen, Hinweis nach Updates",
            "Versionsnummern für Sparkgram",
            "Info-Zeile im Profil jetzt untereinander",
            "Profilbilder in der Profil-Bibliothek in Farbe statt grau",
            "Hauptschalter oben angeheftet, Konten-Hinweis nicht mehr ausgegraut"
        ),
        new Release("0.5", "07.11.2026",
            "Sammel-Knopf „Sparkgram ✦“ im Profil- und Chat-Menü (einstellbar)",
            "„🚪 raus“ neben Namen in Gruppen, Hinweis wenn jemand aus dem Gespräch geht",
            "Bremse gegen Telegram-Limits (Wartezeiten und Stundenbudget)",
            "Info-Zeile direkt im Profil und in Gruppen-Infos",
            "Datum und Uhrzeit beim Ansehen von Profilbildern",
            "Mehr Infos: Konto erstellt (geschätzt), weitere Benutzernamen, Gruppentyp, deine Rolle, geschützte Inhalte, verknüpfte Gruppe/Kanal, hervorgegangen aus"
        ),
        new Release("0.4", "07.11.2026",
            "Eigener Schalter für jede Funktion, Einstellungen ausgegraut ohne Hauptschalter",
            "✦-Markierung an allen Sparkgram-Optionen",
            "Profil-Bibliothek mit Bildern, Vorschau und Vollbild",
            "Mehrere Profilbilder auf einmal löschen",
            "Gruppen-Infos: öffentlich/privat und Link kopieren",
            "Sparkgram-Funktionen auch im Chat-Menü"
        ),
        new Release("0.3", "07.11.2026",
            "Deutsche Texte in den Sparkgram-Einstellungen",
            "Anzeige der installierten Version",
            "Schnellere Builds"
        ),
        new Release("0.2", "06.11.2026",
            "Absturz beim Start behoben",
            "Hauptschalter für alle Zusatzfunktionen",
            "Updates direkt aus GitHub",
            "Private Notizen zu Personen, Gruppen und Kanälen",
            "Absturzbericht in den Einstellungen"
        ),
        new Release("0.1", "06.11.2026",
            "Sparkgram als eigene App neben Telegram, bis zu 16 Konten",
            "Einstellungsseite „Sparkgram-Funktionen“",
            "ID kopieren und Infos im Profilmenü",
            "Profil klonen mit Sicherung und Profil-Bibliothek"
        ),
    };

    public static Release current() {
        String v = org.telegram.messenger.BuildConfig.SPARK_VERSION;
        for (Release r : RELEASES) {
            if (r.version.equals(v)) {
                return r;
            }
        }
        return null;
    }

    /** true, wenn seit dem letzten Start eine neue Version installiert wurde (einmalig). */
    public static boolean shouldShowWhatsNew() {
        String v = org.telegram.messenger.BuildConfig.SPARK_VERSION;
        String seen = SparkConfig.prefs().getString("lastSeenVersion", null);
        if (v.equals(seen)) {
            return false;
        }
        SparkConfig.prefs().edit().putString("lastSeenVersion", v).apply();
        return seen != null && SparkConfig.getRaw(SparkConfig.WHATS_NEW_HINT) && current() != null;
    }
}

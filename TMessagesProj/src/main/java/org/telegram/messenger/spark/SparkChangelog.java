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
        new Release("0.11", "07.11.2026",
            "Online-Protokoll: im Profil-Menü sehen, wann jemand online war (Zeiträume mit Dauer); „kürzlich“ & Co. getrennt mit ⚠ markiert",
            "Suche nach Name, @Benutzername oder ID – auch nach lokalen und früheren Namen (Sparkgram-Einstellungen → Personen)",
            "Sprachnachrichten: beim Loslassen öffnet sich Telegrams Vorschau (anhören, zuschneiden, senden oder verwerfen) – wie bei der Freihand-Aufnahme",
            "Profil: zeigt Telegram nur „kürzlich“, steht darunter „✦ zuletzt echt gesehen: …“ aus dem Online-Protokoll",
            "Gelöschte Konten zeigen den Namen, den Sparkgram vorher kannte („Name (gelöscht)“)",
            "Schlagwort-Benachrichtigung heißt jetzt „✦ Schlagwort: …“",
            "Übersetzen: „Chat übersetzen“ auch ohne Premium – über Google oder mit eigenem DeepL-Schlüssel",
            "Einzelne Nachrichten werden bei gewähltem DeepL ebenfalls über DeepL übersetzt",
            "Eigene Nachricht vor dem Senden übersetzen: Text eintippen → Drei-Punkte-Menü → „Eingabe übersetzen“ (Sprache wird pro Chat gemerkt, mit Rückgängig)",
            "Schlagwort-Alarm erkennt jetzt alle Nachrichten (auch Privatchats, kleine Gruppen und nachgeladene Nachrichten)",
            "Sprachnachrichten: Rückfrage jetzt auch beim Senden aus der Vorschau (freihändig aufgenommen / zugeschnitten)",
            "Mitglieder & Aktivität: Anzeige bleibt oben stehen, alle Mitglieder werden komplett geladen",
            "Admins anzeigen: anonyme Admins werden mit „· anonym“ markiert",
            "Notiz-Zeile im Profil zeigt „…“, wenn die Notiz weitergeht"
        ),
        new Release("0.10", "07.11.2026",
            "Rückfrage bei Sprachnachrichten funktioniert jetzt auch mit Telegrams neuem Aufnahme-Weg",
            "Anzeige, wie oft eine Nachricht weitergeleitet wurde (⤳ 12×, bei Kanal-Beiträgen)",
            "Sparkgram-Einstellungen zeigen oben, für welches Konto sie gelten",
            "Notiz-Zeile im Profil: „Noch keine Notiz“ bzw. nur eine kurze Vorschau",
            "Ortszeit in der Info-Zeile kurz: Uhrzeit und UTC-Zeitzone",
            "Gruppen: „Admins anzeigen“ mit Titeln (auch als normales Mitglied)",
            "Gruppen: „Mitglieder & Aktivität“ – Mitgliederliste mit Anzahl Nachrichten, zuletzt aktiv und ✅/❌ (bei verborgener Liste aus dem Verlauf)",
            "Kanäle: „Bekannte Autoren“ aus signierten Beiträgen",
            "Schlagwort-Alarm: Benachrichtigung bei Schlagwörtern, Trefferliste mit Sprung zur Nachricht"
        ),
        new Release("0.9", "07.11.2026",
            "Notiz direkt im Profil als eigene Zeile – zeigt die Notiz oder „Notiz hinzufügen“",
            "Rote Kante sitzt jetzt exakt in der Blase und ist schmaler",
            "Sprachnachrichten: neues Fenster nach der Aufnahme mit Anhören, Senden und Verwerfen (behebt Fehler mit leerer Vorschau)",
            "Optionale Rückfrage vor dem Wiederholen einer Nachricht",
            "Profil-Bibliothek: Original immer oben, Favoriten ⭐ darunter",
            "Frühere Namen und Benutzernamen auf Anfrage: Profilmenü → „Frühere Namen“ (mit Zeitraum)",
            "„Ausblenden → Telegram Premium“ blendet auch Premium-Optionen wie „Teilen deaktivieren“ aus – optional automatisch aus, sobald du Premium hast",
            "Namen nur für dich ändern (auch deinen eigenen) – Profilmenü → „Name nur für mich“",
            "Ortszeit von Kontakten: Zeitzone im Profilmenü festlegen, aktuelle Uhrzeit steht in der Info-Zeile",
            "Weiterleiten standardmäßig ohne Absender (abschaltbar)",
            "Sparkgram-Einstellungen gelten jetzt pro Konto (bisherige Einstellungen werden übernommen)"
        ),
        new Release("0.8", "07.11.2026",
            "Eigenes Sparkgram-App-Symbol: hellblauer Flieger mit orangen Funken (auch für Themed Icons)",
            "Updates-Bereich: „Nach Updates suchen“ und „Testversionen“ direkt unter der installierten Version",
            "Rote Kante gelöschter Nachrichten folgt der Rundung der Blase und läuft spitz aus",
            "Profil-Bibliothek: „Original-Profil öffnen“, findet die Person über ihre ID (auch nach Namensänderung)",
            "Nachrichten-Details: IDs, genaue Zeiten, Weiterleitungsherkunft, Datei-Infos",
            "Nachricht wiederholen (+1)",
            "Originalzeit bei weitergeleiteten Nachrichten (↪)",
            "Chat-Menü: zur ersten Nachricht springen, alle eigenen Nachrichten löschen (gebremst)",
            "Eigene Nachrichten erst auflisten und gezielt auswählen, bevor gelöscht wird",
            "Rückfrage vor dem Senden von Stickern und GIFs, Sprachnachrichten erst anhören",
            "Frühere Benutzernamen und Namen von Personen in den Infos (ab jetzt gemerkt)"
        ),
        new Release("0.7", "07.11.2026",
            "Auch selbst gelöschte Nachrichten bleiben bei dir stehen – rot „🗑 von dir gelöscht“ (abschaltbar)",
            "Nachricht antippen → „Endgültig entfernen“ für als gelöscht markierte Nachrichten",
            "Ausblenden: Telegram Premium, Sterne und Geschenke, Business, Wallet/TON",
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

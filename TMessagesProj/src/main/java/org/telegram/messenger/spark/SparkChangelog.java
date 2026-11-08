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
        new Release("0.17", "08.11.2026",
            "Verlassene Gruppen: Verlauf bleibt beim Verlassen erhalten, Gruppe wird nicht mehr automatisch entfernt",
            "Auch ältere verlassene Gruppen aus Telegrams Speicher werden angezeigt (Hinweis in den Einstellungen)",
            "Neu: „✦ Erinnern“ im Nachrichten-Menü – Erinnerung zu einer Nachricht (30 min bis morgen früh oder eigene Zeit), kommt als geplante Nachricht in Gespeicherte mit Link",
            "Neu: fremdsprachige Chats automatisch übersetzen (Übersetzen, abschaltbar; einmal selbst aus = bleibt aus)",
            "Duplikate in Gespeichertes: gleiche Fotos, Videos, Dateien, Sticker und Texte finden und aufräumen (älteste bleibt)",
            "Chat- und Profil-Menü: wieder ein Eintrag „✦ Sparkgram“, darin die Gruppen als Unterpunkte",
            "Lokal angeheftete Chats: eigenes oranges Anheft-Symbol (unterscheidbar von normal angehefteten)",
            "Download-Knopf: stehendes Symbol, Animation nur bei laufendem Download"
        ),
        new Release("0.16", "08.11.2026",
            "Verlassene Gruppen bleiben jetzt auch nach Neustart/Neuladen der Chatliste erhalten",
            "Gelöschte Privatchats bleiben lokal im Ordner „Verlassen“ (auf dem Server gelöscht); nochmal löschen = endgültig",
            "Symbole: 🚪 verlassen, 🔨 entfernt, 🗑 gelöscht – in der Chatliste und in der Liste",
            "Stummschalten pro Person: nur Benachrichtigungen aus oder zusätzlich Nachrichten einklappen",
            "Chat- und Profil-Menü: Funktionen in Unterpunkten gesammelt (Infos …, Notiz & Medien …, Übersetzen …, Löschen & Export …); Sammlung, Chat-Sticker, Übersetzen, Löschen und Export auch im Profil",
            "Lokal anheften auch beim Markieren von Chats in der Chatübersicht",
            "Export: zusätzlich „In Gespeicherte“ ablegen",
            "Download-Knopf nur noch animiert, während wirklich etwas lädt",
            "Ungelesen pro Konto: Zähler in der Kontoauswahl",
            "Versteckte Konten: in Kontoauswahl und Einstellungen ausblenden (Konten → Versteckte Konten)",
            "Sortierung: ungelesene Chats oben (abschaltbar)"
        ),
        new Release("0.15", "08.11.2026",
            "Schlagwort-Benachrichtigung: zeigt klar Chat und Absender, antippen springt direkt zur Nachricht",
            "Update: bei GitHub-Störung (z. B. 503) automatische Wiederholung, sonst direkt zur GitHub-Seite der neuesten Version",
            "Download-Knopf in der Chatübersicht immer anzeigen (abschaltbar)",
            "Export: Service-Nachrichten mit echtem Text (z. B. „X ist beigetreten“), lange werden umgebrochen",
            "Ghost-Modus: nach dem Senden erst nach ca. 7 s offline; als gelesen nur Privatchats, nicht Gruppen",
            "Gelöschte Service-Nachrichten: rote Blase statt roter Schrift",
            "Ausgeblendete Nachrichten: mehrere hintereinander werden zu „N ausgeblendete Nachrichten“ zusammengefasst, Antippen deckt alle auf",
            "Zur ersten Nachricht: hebt die erste echte Nachricht hervor",
            "Nachrichten-Menü: nur noch „Stummschalten“; „Bereich markieren“ verständlicher beschriftet",
            "Nachrichten-Menü anpassen: Telegram- und Sparkgram-Einträge einzeln ausblenden (Darstellung)",
            "Lokal anheften: beliebig viele Chats oben halten (Chat-Menü), direkt unter den angehefteten",
            "Gelöschte & verlassene Chats: Liste mit Grund (gelöscht/verlassen/entfernt), Name, Benutzername und letzter Nachricht",
            "Verlassene oder entfernte Gruppen bleiben in der Chatliste (rot mit 🚪) und lassen sich mit ihren Nachrichten öffnen",
            "Ordner „🚪 Verlassen“ in der Chatliste: alle verlassenen/entfernten Gruppen auf einen Blick (nur lokal, wird nicht an Telegram gesendet)",
            "Zwischenablage-Schutz: Kopiertes nach 30 s – 5 min automatisch löschen (Privatsphäre)"
        ),
        new Release("0.14", "08.11.2026",
            "Export, Sicherung und Medien speichern funktionieren jetzt (Ordner Download/Sparkgram, mit Bestätigung)",
            "Update in der App: lädt die feste APK, zeigt Fortschritt in %, fragt nach der Installations-Erlaubnis und meldet Fehler",
            "Ghost-Modus: nach dem Senden sofort wieder offline, und der Chat gilt als gelesen",
            "QR-Anmeldung reagiert jetzt auf Telegrams Scan-Signal (Code wechselt nicht mehr ständig)",
            "Chats übersetzen: Nachricht für Nachricht, leere werden übersprungen, ein Fehler stoppt nicht mehr alles",
            "Zur ersten Nachricht: springt nach oben und lässt sie kurz aufblinken",
            "Mitglieder & Aktivität: „Weiter durchsuchen“ fest oben angeheftet",
            "Person stummschalten auch über das Nachrichten-Menü; Liste der Stummgeschalteten; Nachrichten in Gruppen ausblenden",
            "Gelöschte Beitrittsmeldungen und andere Service-Nachrichten werden rot angezeigt",
            "Ausgeblendete Nachrichten (stummgeschaltete Personen) wie bei Discord: „🔕 Ausgeblendete Nachricht von … · antippen“ zeigt sie an",
            "QR-Code aus einem Screenshot (Galerie-Knopf) meldet jetzt auch Web/Desktop an",
            "„Weiterleiten ohne Absender“ wieder als Schalter (standardmäßig an)",
            "Doppeltipp auf eine Nachricht wählbar: Reaktion, Antworten, Kopieren, Wiederholen oder Lesezeichen",
            "Lesezeichen mit Schlagwörtern: Nachricht antippen → „✦ Lesezeichen“, Liste mit Filter, antippen springt zur Nachricht",
            "„Alle Nachrichten dieser Person“ im Nachrichten-Menü in Gruppen",
            "Bot-Postfach: Nachrichten an deinen Bot ansehen und als Bot antworten",
            "Lokale Namen gelten jetzt pro Konto",
            "Einstellungen aufgeräumt: kürzere Texte, Unterpunkte auch bei ausgeschaltetem Punkt aufklappbar, Sicherung unter „Konten“"
        ),
        new Release("0.13", "07.11.2026",
            "„Zur ersten Nachricht“ funktioniert jetzt in allen Chats (fragt die älteste Nachricht direkt ab)",
            "Chat-Menü: „Chat übersetzen an/aus“ – übersetzt jeden Chat, auch wenn Telegram ihn nicht als fremdsprachig erkennt",
            "„Sticker dieses Chats“ als Raster mit den echten Stickern (Anzahl als Zahl, lange drücken = mehr)",
            "Mitglieder & Aktivität: „Weiter durchsuchen“ steht jetzt oben",
            "Suche nach Name/ID wieder entfernt",
            "Ghost-Modus (Privatsphäre): keine Lesebestätigung, kein „schreibt …“, nicht online – einzeln wählbar; im Chat-Menü „Jetzt als gelesen melden“",
            "Chat exportieren als PDF oder HTML (Chat-Menü): 1.000 / 5.000 / 20.000 Nachrichten oder ganzer Chat → Downloads",
            "PDF im Chat-Look: eigene Nachrichten rechts in Grün, andere links, Fotos/Sticker/Vorschaubilder eingebettet",
            "Sicherung & Konten übertragen: Konten-Liste und Sparkgram-Daten exportieren; auf dem neuen Gerät ist die Nummer beim Anmelden schon eingetragen",
            "Konto per QR-Code hinzufügen: mit einem schon angemeldeten Gerät scannen – kein SMS-/Telegram-Code nötig (nur ggf. Passwort)"
        ),
        new Release("0.12", "07.11.2026",
            "Einstellungen: Punkte mit Unterpunkten lassen sich auf- und zuklappen (Zeile antippen; Schalter = an/aus)",
            "Übersetzen: Nachrichten und ganze Chats lassen sich wieder übersetzen (Telegram hatte die Funktion für Sparkgram abgeschaltet)",
            "Suche nach Name / ID mit festem Suchfeld oben",
            "Chat: „zuletzt aktiv / online gesehen“ steht jetzt als eigene Zeile unter dem Status",
            "Profil: die Zeile unter dem Status ist jetzt sauber mittig",
            "Mitglieder & Aktivität: Pause nach je 1000 Nachrichten ist zurück, die Liste bleibt oben",
            "Sprachnachrichten: keine extra Rückfrage mehr beim Senden aus der Freihand-Vorschau",
            "Bis zu 1000 Nachrichten markieren – Weiterleiten und Löschen laufen automatisch in 100er-Paketen",
            "Bereich markieren: während der Auswahl eine andere Nachricht lange drücken markiert alles dazwischen",
            "Über eigenen Bot senden: Bot-Token eintragen, dann im Chat-Menü „Über Bot senden“",
            "Sammlung: unbegrenzt Sticker, GIFs und Sprachnachrichten mit eigenen Ordnern – im Chat antippen = senden",
            "„✦ Medien …“ an Nachrichten: speichern (Downloads), ohne Absender weitersenden, Pack-Link kopieren, ganzes Pack herunterladen, in eigenes Pack übernehmen",
            "„Sticker dieses Chats“: alle im Chat benutzten Packs mit Anzahl",
            "Galerie-Knopf im QR-Scanner",
            "Einzelne Personen stummschalten (Profil-Menü): keine Benachrichtigungen mehr von ihnen, auch in Gruppen",
            "Textvorlagen: im Chat-Menü einfügen, in den Einstellungen verwalten",
            "Statistiken: Anzahl Chats, Personen, Bots, Gruppen, Kanäle, Archiv, Ungelesenes"
        ),
        new Release("0.11", "07.11.2026",
            "Online-Protokoll: im Profil-Menü sehen, wann jemand online war (Zeiträume mit Dauer); „kürzlich“ & Co. getrennt mit ⚠ markiert",
            "Suche nach Name, @Benutzername oder ID – auch nach lokalen und früheren Namen (Sparkgram-Einstellungen → Personen)",
            "Sprachnachrichten: beim Loslassen öffnet sich Telegrams Vorschau (anhören, zuschneiden, senden oder verwerfen) – wie bei der Freihand-Aufnahme",
            "Profil und Chat: unter bzw. neben dem Status steht, wann Sparkgram die Person zuletzt online gesehen hat oder wann sie zuletzt in gemeinsamen Chats geschrieben/getippt hat",
            "Gelöschte Konten zeigen den Namen, den Sparkgram vorher kannte („Name (gelöscht)“)",
            "Schlagwort-Benachrichtigung heißt jetzt „✦ Schlagwort: …“ und erscheint oben bei den Unterhaltungen (wie normale Chats)",
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

# Sparkgram – Entwicklungsnotizen

Arbeitsnotizen zum Projektstand, zu Absprachen und technischen Details.
Wird bei jeder größeren Änderung mitgepflegt, damit der Stand jederzeit nachvollziehbar ist.

## Absprachen mit Spark633

- Sprache: immer Deutsch. Spark633 nutzt Android.
- Git: Autor **und** Committer `Spark633 <33431793+Spark633@users.noreply.github.com>`.
  Keine Hinweise auf KI-Werkzeuge in Commits, Code, Dateien, PRs oder Kommentaren.
  Keine Signatur – Hinweise einer Prüfung auf „Unverified“ werden bewusst ignoriert.
- Branches: Arbeit auf `entwicklung`. `main` erst nach ausdrücklichem OK
  (Vorschlag: PR von Spark633 selbst + „Squash and merge“ → verifizierter Commit).
- **Nicht sofort pushen.** Änderungen lokal sammeln, pushen erst auf „push“/„bauen“
  (oder wenn Spark633 „push, sofern keine Rückfrage“ sagt).
- Keine PRs/Issues/Kommentare auf GitHub ohne ausdrückliche Bitte.
- Nach jedem fertigen Bau: Neuerungen **und** eine Testliste („was am besten testen“) mitschicken.
- Fester APK-Link: `https://github.com/Spark633/Telegram-Spark/releases/download/build-entwicklung/Sparkgram-entwicklung.apk`
- Jede Funktion bekommt einen eigenen Schalter; Sparkgram-Optionen mit ✦ markiert.
- Vorsicht vor Telegram-Limits: alles mit vielen Anfragen über `SparkRateLimit`.

## Projekt

- Basis: offizieller Telegram-Android-Quellcode 13.1.1 (DrKLO/Telegram), GPL.
- App: `TMessagesProj_AppStandalone`, Paket `com.spark633.sparkgram`, Name „Sparkgram“.
- Version: `SPARK_VERSION_NAME` in `gradle.properties`; Änderungsliste in
  `TMessagesProj/src/main/java/org/telegram/messenger/spark/SparkChangelog.java` (neueste oben).
- App-Symbol: `TMessagesProj_AppStandalone/src/main/res/mipmap-*/ic_launcher_sa*`,
  Vorlage `.github/sparkgram-icon.svg` (aktuell: hellblauer Flieger, orange Funken, dunkler Grund),
  Alternative `.github/sparkgram-icon-orange.svg`.

## Bauen (GitHub Actions)

- Workflow `.github/workflows/build.yml`, baut nur arm64, mit ccache (~10–15 min, nativ unverändert).
- Secrets: `TELEGRAM_API_ID`, `TELEGRAM_API_HASH`, `SPARK_KEYSTORE_PASSWORD`
  (Keystore verschlüsselt: `.github/signing/sparkgram.keystore.enc`, Alias `sparkgram`).
- Release `build-<branch>` wird bei jedem Bau ersetzt (APK mit Zeitstempel + fester Name).
- Fehler finden: GitHub-MCP `get_job_logs` (job_id, tail_lines ~560) → Datei → nach `error:` filtern.
  Direkter Download der Logs ist über den Proxy gesperrt.
- Lokale Prüfung (kein Android-SDK hier):
  `javac -Xmaxerrs 1000000 -XDshould-stop.at=FLOW -sourcepath . <Dateien>` und Meldungen wie
  „cannot find symbol / is ambiguous“ ignorieren (fehlende Bibliotheken). Echte Fehler (z. B.
  „effectively final“) fallen sonst unter das 100-Fehler-Limit!

## Wichtige Erkenntnisse

- Texte **nur** in `TMessagesProj/src/main/res/values/strings.xml` (Telegrams Build liest nur diese Datei,
  sonst `LOC_ERR`).
- Native Verbindungen: `jni/tgnet/ConnectionsManager.cpp` hatte fest 5 Instanzen → auf Array umgebaut
  (sonst Absturz bei >5 Konten). `MAX_ACCOUNT_COUNT` Java 16 / nativ 17.
- Telegram-Listen färben Icons grau → eigene Bilder über `SparkUi.CircleBitmapDrawable`.
- Blasenumriss für Zeichnungen: `MessageDrawable.makePath()`.
- Sprachnachricht-Vorschau über `stopRecording(2)` erzeugte Fehler → stattdessen Rückfrage
  direkt vor `sendMessage` in `MediaController`.
- Nach Login hing einmal die Chatliste → Debug-Menü (Versionszeile 2× tippen) → „Reset Dialogs“.

## Code-Übersicht (alles unter `TMessagesProj/src/main/java/org/telegram/`)

- `messenger/spark/` – Logik:
  `SparkConfig` (Schalter, **pro Konto**; global: Updates/„Was ist neu“), `SparkProfiles` (Profil-Bibliothek),
  `SparkNotes`, `SparkMembership` (🚪 raus), `SparkRateLimit`, `SparkMessageStore` (gelöscht/Verlauf, SQLite),
  `SparkBulkDelete`, `SparkNameHistory`, `SparkLocalNames`, `SparkTimezones`, `SparkRegDate`,
  `SparkUpdater`, `SparkChangelog`, `SparkCrashLog`.
- `ui/spark/` – Oberfläche: `SparkSettingsActivity`, `SparkProfileMenu` (Profil-/Chat-Menü, Infos),
  `SparkProfileLibraryActivity`, `SparkProfilePreviewActivity`, `SparkPhotoManagerActivity`,
  `SparkNotesActivity`, `SparkOwnMessagesActivity`, `SparkChangelogActivity`, `SparkUi` (Helfer).
- Eingriffe in Telegram-Code (Suche nach „Sparkgram“): `ProfileActivity`, `ChatActivity`,
  `ChatMessageCell`, `ChatActionCell`, `ChatActivityEnterView`, `MessagesController`, `MediaController`,
  `SettingsActivity`, `LaunchActivity`, `PhotoViewer`, `UserObject`, `MessagePreviewParams`,
  `StoriesController` (nichts mehr aktiv), `ApplicationLoader`, `BuildVars`, `UserConfig`, `ContactsController`.

## Stand der Versionen

- 0.1–0.8: siehe `SparkChangelog.java`.
- 0.9: Notiz-Zeile, exakte rote Kante, Sprachnachricht-Fenster, Rückfrage Wiederholen,
  Bibliothek-Favoriten, frühere Namen auf Anfrage, Premium-Optionen ausblendbar (+Automatik pro Konto),
  lokale Namen, Ortszeit, Weiterleiten ohne Absender, Einstellungen pro Konto.
- 0.10 (in Arbeit): Sprachnachricht-Rückfrage auch im V2-Aufnahmeweg (MediaController.completeAudioRecordingV2),
  Weiterleitungszähler, Kontoname als Untertitel, kurze Notiz-Vorschau, kurze Ortszeit,
  Etappe 2 teilweise: Admins anzeigen, Aktive Mitglieder (✅/❌), Bekannte Autoren (`SparkGroupListActivity`),
  stille Mitglieder aus Mitgliederliste, Schlagwort-Alarm (`SparkKeywords`, `SparkKeywordsActivity`,
  Einhängepunkt neben `rememberRaw` in `MessagesController`).

## Roadmap (offen)

- **Etappe 3 Rest:** Weiterleiten ohne Limit, schneller markieren / Auswahl-Limit, Bild-Link kopieren.
- **Etappe 2 Rest:** Admin-Funktion (mit Spark633 abstimmen).
- **Etappe 4:** Übersetzen (einzeln, ganzer Chat, automatisch, eigene vor dem Senden; Google + DeepL),
  Senden über eigenen Bot.
- **Etappe 5:** Online-Protokoll (echte Zeiten; „kürzlich“ getrennt markiert, fließt nicht in
  „zuletzt aktiv gesehen“ ein), Suche nach Name/ID. (Lokale Namen, Ortszeit, Schlagwort-Alarm: erledigt.)
- **Etappe 6:** Sticker/Packs herunterladen, fremde Sticker ins eigene Pack, alle Sticker eines Chats,
  unbegrenzte Favoriten + Ordner, Sprachnachrichten speichern/erneut senden, Galerie-Knopf im QR-Scanner.
- **Etappe 7:** Chat-Anzahl, Ordner „Gelöschte Chats“, Ungelesen pro Konto, mehr Anheften (lokal),
  einzelne stummschalten, In-App-Benachrichtigungen, Export PDF/HTML, Bookmarks mit Schlagwörtern,
  Duplikate in „Gespeichertes“, RP: Persona pro Chat, Textvorlagen, Zitieren mit Formatierung,
  Nachrichtenverlauf einer Person, Auto-Ordner, Sortierung, Statistiken, Backup.
- **Etappe 8:** Ghost-Modus (gelesen/schreibt/online getrennt), Menüs aufräumen, Doppeltipp-Aktion,
  App-Sperre, versteckte Konten, Zwischenablage-Schutz, Proxy-Verwaltung (+ Anleitung).
- Nebenher: eigenes Firebase für Push, mehr Konten ohne Start-Nachteil, Übernahme nach `main`.
- Gestrichen: Sprachnachricht→Text, „Nachricht zeichnen“, Gerätetyp anderer, „zuletzt online“ erzwingen,
  Stories ausblenden, nachträgliches „für alle löschen“ nach „nur für mich“.

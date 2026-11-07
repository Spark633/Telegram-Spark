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
- Schlagwort-Alarm/Online-Protokoll bei geschlossener App: nur mit Telegrams „Keep-Alive Service“ +
  „Background Connection“ (Benachrichtigungen → ganz unten), kein eigenes Push.
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
  Achtung: „cannot find symbol“ kann bei eigenen Dateien echt sein (z. B. fehlender
  `import static org.telegram.messenger.LocaleController.getString;`) – neue Dateien darauf prüfen.

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
  `SparkUpdater`, `SparkChangelog`, `SparkCrashLog`, `SparkKeywords`, `SparkTranslate`, `SparkOnlineLog`.
- `ui/spark/` – Oberfläche: `SparkSettingsActivity`, `SparkProfileMenu` (Profil-/Chat-Menü, Infos),
  `SparkProfileLibraryActivity`, `SparkProfilePreviewActivity`, `SparkPhotoManagerActivity`,
  `SparkNotesActivity`, `SparkOwnMessagesActivity`, `SparkChangelogActivity`, `SparkUi` (Helfer).
- Eingriffe in Telegram-Code (Suche nach „Sparkgram“): `ProfileActivity`, `ChatActivity`,
  `ChatMessageCell`, `ChatActionCell`, `ChatActivityEnterView`, `MessagesController`, `MediaController`,
  `SettingsActivity`, `LaunchActivity`, `PhotoViewer`, `UserObject`, `MessagePreviewParams`,
  `TranslateController`, `TranslateAlert2`, `StoriesController` (nichts mehr aktiv), `ApplicationLoader`, `BuildVars`, `UserConfig`, `ContactsController`.

## Stand der Versionen

- 0.1–0.8: siehe `SparkChangelog.java`.
- 0.9: Notiz-Zeile, exakte rote Kante, Sprachnachricht-Fenster, Rückfrage Wiederholen,
  Bibliothek-Favoriten, frühere Namen auf Anfrage, Premium-Optionen ausblendbar (+Automatik pro Konto),
  lokale Namen, Ortszeit, Weiterleiten ohne Absender, Einstellungen pro Konto.
- 0.10: Sprachnachricht-Rückfrage auch im V2-Aufnahmeweg (MediaController.completeAudioRecordingV2),
  Weiterleitungszähler, Kontoname als Untertitel, kurze Notiz-Vorschau, kurze Ortszeit,
  Etappe 2 teilweise: Admins anzeigen, Aktive Mitglieder (✅/❌), Bekannte Autoren (`SparkGroupListActivity`),
  stille Mitglieder aus Mitgliederliste, Schlagwort-Alarm (`SparkKeywords`, `SparkKeywordsActivity`,
  Einhängepunkt neben `rememberRaw` in `MessagesController`).

- 0.11 (in Arbeit): Etappe 4 Teil 1 Übersetzen – `SparkTranslate` (Google/DeepL, Schlüssel in globalen Prefs),
  Schalter `TRANSLATE_FREE`; Eingriffe in `TranslateController` (`isFeatureAvailable`, `sparkUseAlternative`)
  und `TranslateAlert2` (DeepL vor Google in `alternativeTranslateInternal`, Einzelnachricht → `translateAlt`).
  Mitgliederliste: Kopf fest oben, bis 10000 Mitglieder, Komplett-Scan; anonyme Admins markiert; Notiz „…“.
  Etappe 4 Teil 2a: „Eingabe übersetzen“ (`SparkProfileMenu.translateInput`, Schalter `TRANSLATE_OWN`,
  Sprache pro Chat in globalen Prefs `translateOut_<dialogId>`).
  Schlagwort-Check zusätzlich in Kurz-Updates (`TL_updateShortMessage/ShortChatMessage`) und in beiden
  `getDifference`-Wegen, doppelte Treffer per `SEEN`-Menge verhindert.
  Sprachnachricht-Rückfrage auch in `ChatActivityEnterView.sendMessageInternal` (Vorschau, `audioToSend`).
  Sprachnachricht zuschneiden im Rückfrage-Fenster (`SparkUi.confirmVoice` mit Dokument, `MediaController.cropOpusFile`,
  Wellenform/Dauer neu).
  Etappe 5: `SparkOnlineLog` (SQLite `sparkgram_online_<account>.db`, 30 Tage, Hooks: `TL_updateUserStatus` und
  `putUser` nur für Kontakte), `SparkOnlineLogActivity` (Profil-Menü 915), `SparkSearchActivity`
  (lokal: Nutzer/Chats im Speicher, lokale + frühere Namen via `SparkNameHistory.search`; online: `resolveUsername`
  bzw. `users.getUsers` mit access_hash 0, nur auf Knopfdruck).
  Sprachnachricht: beim Loslassen (CONFIRM_VOICE) `MediaController.toggleRecordingPause` → Telegrams Vorschau
  (wie der Paid-Message-Weg in `ChatActivityEnterView`), Flag `sparkVoicePreview` verhindert doppelte Rückfrage.
  Das Fenster in `SparkUi.confirmVoice` (mit Reglern) bleibt für die übrigen Sendewege.
  Profil: `sparkSeenView` unter `onlineTextView[1]` (folgt Translation/Alpha über deren Overrides),
  `SparkOnlineLog.seenLine` (neueste aus Online-Protokoll und Tabelle `active`: letzte Nachricht/Tippen pro Person,
  Hooks neben `SparkKeywords.check` und bei `TL_update*UserTyping`); im Chat angehängt in
  `ChatAvatarContainer` (kompakt). Gelöschte Konten: `UserObject.getUserName` → `SparkNameHistory.lastKnown`
  (Schalter `DELETED_NAMES`; gelöschte Konten werden nicht mehr in den Verlauf geschrieben).

- 0.12 (in Arbeit): einklappbare Oberpunkte (`UItem.asExpandableSwitch`, `expanded`-Set in
  `SparkSettingsActivity`), Server-Abschaltung der Übersetzung (`translations*Enabled = "disabled"`) wird bei
  `TRANSLATE_FREE` übergangen (MessagesController, TranslateController, TranslateAlert2 → Google/DeepL),
  Suche mit angeheftetem `EditTextBoldCursor`, Chat-Kopf: `sparkLineView` als 3. Zeile in `ChatAvatarContainer`
  (Titel/Status rücken 5 dp hoch), Profil-Zeile zentriert per `measureText`, Mitgliederliste: Pause zurück,
  `update()` hält die Liste oben. Freihand-Rückfrage entfernt (Telegrams Vorschau reicht).
  Etappe 3 Rest: `SparkConfig.selectLimit()` (ChatActivity, 2 Stellen), `deleteMessages` teilt >100 auf
  (Weiterleiten teilt Telegram selbst), `ChatActivity.sparkSelectRange` (Langdruck im Auswahlmodus).

## Roadmap (offen)

- **Etappe 3 Rest:** Bild-Link kopieren (unklar, was genau gemeint ist).
- **Etappe 2 Rest:** Admin-Funktion (mit Spark633 abstimmen).
- **Etappe 4 Rest:** automatisch übersetzen, Senden über eigenen Bot.
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

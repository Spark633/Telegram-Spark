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
  Bot: `SparkBot` (Bot API sendMessage, Token global in Prefs, Chat-ID -100… für Kanäle/Supergruppen,
  Antwort nur in Supergruppen/Kanälen, da dort die IDs gleich sind), Chat-Menü 916.
  Etappe 6: `SparkCollection` (SQLite, ganze Nachricht serialisiert, Senden als Kopie ohne Absender →
  Original muss noch existieren), `SparkCollectionActivity`, `SparkMedia` (Nachrichtenmenü `OPTION_SPARK_MEDIA` 904),
  `SparkChatStickersActivity` (Chat-Menü 917), QR-Galerie: `CameraScanActivity.sparkGallery()`.
  Etappe 7 Teil 1: `SparkPeople` (stummgeschaltete Personen, Filter am Anfang von
  `NotificationsController.processNewMessages`, Profil-Menü 919), `SparkTemplates` + `SparkTemplatesActivity`
  (Chat-Menü 920), `SparkStatsActivity` (Chat-Anzahl & Co.).

- 0.13 (in Arbeit): Suche entfernt (auf Wunsch), Sprung zur ersten Nachricht per `getHistory(offset_id=1,
  add_offset=-10)`, `TranslateController.sparkForceTranslate` (Chat-Menü 921), Sticker-Raster in
  `SparkChatStickersActivity`, Weiter-Knopf oben in `SparkGroupListActivity`.
  Ghost-Modus: `SparkConfig.ghost(part)`; Eingriffe in MessagesController: Status-Timer (nie online),
  `sendTyping` (früh raus), `completeReadTask` (kein readHistory, außer geheime Chats);
  `SparkExport.markReadNow` (Chat-Menü 922).
  PDF im Chat-Look (`SparkExport.drawBubble`, Bilder vorher per `FileLoader.loadFile` geladen, max. 600, Wartezeit
  begrenzt). Sicherung: `SparkBackup` (JSON: Konten + Prefs; keine Sitzungen/Keys/Tokens), `SparkBackupActivity`,
  Konto-Daten nach Nutzer-ID zugeordnet (`applyPending` in LaunchActivity.onResume), Nummer vorbelegt über
  `LoginActivity.sparkPrefillPhone` (in `PhoneView.fillNumber`).
  QR-Anmeldung: `LoginActivity.sparkQrMode` → `sparkStartQr` (auth.exportLoginToken alle 3 s abfragen, QR erst kurz vor
  Ablauf erneuern, bei `loginTokenMigrateTo` wie beim Passkey-Login `setDefaultDatacenterId` + importLoginToken,
  bei SESSION_PASSWORD_NEEDED Telegrams Passwort-Seite, Erfolg über `onAuthSuccess`). Altes Gerät scannt über
  Einstellungen → Geräte (tg://login?token=…). Export: `SparkExport.exportChat` (Chat-Menü 923, PDF über `android.graphics.pdf.PdfDocument` + StaticLayout, oder HTML; in Downloads).

- 0.14 (in Arbeit): `SparkUi.saveToDownloads` (MediaStore, Download/Sparkgram) statt `MediaController.saveFile`
  (verwirft Dateien aus dem internen Speicher!). Updater: feste APK `Sparkgram-<kanal>.apk`, Fallback bei 404,
  Installations-Erlaubnis. Ghost: `MessagesController.sparkGhostAfterSend` (aus beiden `SendMessagesHelper.sendMessage`).
  QR: `TL_updateLoginToken` in `processUpdates` → `LoginActivity.sparkOnLoginToken`. Chat-Übersetzung:
  `TranslateController.sparkAlternative` (seriell, 150 ms). Stummschalten: `OPTION_SPARK_MUTE` 905,
  `ChatActivity.sparkHideMuted` (3 Ladestellen), `SparkMutedActivity`. `ChatActionCell`: „🗑 gelöscht“.
  `SparkBot.fetch/inbox/sendRaw` + `SparkBotInboxActivity` (getUpdates). Lokale Namen pro Konto (Migration).
  Feste Vorgaben in `SparkConfig.get`: KEEP_OWN_DELETED=KEEP_DELETED, FORWARD_NO_SENDER=false, PHOTO_MANAGER/SELECT_NO_LIMIT=true.
  Einklappen statt Ausblenden: `ChatActivity.sparkHideMuted` macht die Nachricht zur Service-Zeile
  (`type=TYPE_DATE, contentType=1`, `MessageObject.sparkCollapsed`), Tippen → `sparkReveal` (replaceMessageObjects).
  QR aus Galerie: `CameraScanActivity` ruft jetzt wie der Kamera-Weg `processQr` (sonst wurde Login nie angenommen).
  Doppeltipp: `SparkConfig.doubleTap()` (global "doubleTap"), Eingriff in `hasDoubleTap`/`onDoubleTap`, `ChatActivity.sparkDoubleTap`.
  Lesezeichen: `SparkBookmarks` (Prefs pro Konto, JSON), `SparkBookmarksActivity`, Menü 906; Personensuche: Menü 907
  (`openSearchWithText("")` + `searchUserButton` + `searchUserMessages`, wie `checkInstantSearch`).
  Achtung: Python `open(p,'w')` vor dem Lesen leert die Datei – nie `open(p,'w').write(open(p).read())`.

- 0.15 (in Arbeit): Updater-Wiederholung + `Release.unknown` (fester Link), `ALWAYS_DOWNLOADS`
  (`DialogsActivity.updateProxyButton`), Export `serviceText` via MessageObject, Ghost 7 s/12 s + nur Privatchats lesen,
  `ChatActionCell.sparkDeletedRed` (rote Blase in drawBackground), `sparkReveal` deckt Folge auf.
  Zusammenfassen ausgeblendeter Folgen: `ChatActivity.sparkPrepareCollapsed` beim Binden, `ChatActionCell.sparkMergedHidden`
  (Höhe 0, kein Zeichnen). Menü aufräumen: `SparkMenu` + Filter vor `options.isEmpty()` in ChatActivity.createMenu.
  Lokal anheften: `SparkPins` + `dialogComparator` (nach Server-Pins), Chat-Menü 924. Gelöschte Chats:
  `SparkDeletedChats.remember` in `deleteDialog(did, onlyHistory 0/3, revoke)`. Zwischenablage: `SparkClipboard`
  in beiden `AndroidUtilities.addToClipboard`.
  Verlassene/entfernte Chats behalten: `MessagesController.sparkKeepChat` an 4 Stellen (2× Verlassen in
  deleteParticipantFromChat, TL_updateChannel isNotInChat, TL_updateChat kicked), rot in `DialogCell` (isNotInChat).
  Lokaler Ordner: `MessagesController.SparkLeftFilter` (id 1999999, includesDialog = isNotInChat), `sparkEnsureLeftFilter`
  nach `processLoadedDialogFilters`; nie an den Server: ausgelassen in `FilterTabsView`- und `FiltersSetupActivity`-Reihenfolge,
  in der Ordnerverwaltung entfernt (`sparkFilterSuppress`), kein Bearbeiten-Menü am Tab.

## Roadmap (offen)

- **Etappe 3 Rest:** Bild-Link kopieren (unklar, was genau gemeint ist).
- **Etappe 2 Rest:** Admin-Funktion (mit Spark633 abstimmen).
- **Etappe 4 Rest:** automatisch übersetzen.
- **Etappe 7 Rest:** Ungelesen pro Konto,
  In-App-Benachrichtigungen,
  Duplikate in „Gespeichertes“, RP: Persona pro Chat, Zitieren mit Formatierung,
  Auto-Ordner, Sortierung. (Backup: erledigt, ohne Datenbanken wie Sammlung/Online-Protokoll.)
- **Etappe 8 Rest:**
  App-Sperre, versteckte Konten, Proxy-Verwaltung (+ Anleitung).
- Nebenher: eigenes Firebase für Push, mehr Konten ohne Start-Nachteil, Übernahme nach `main`.
- Gestrichen: Sprachnachricht→Text, „Nachricht zeichnen“, Gerätetyp anderer, „zuletzt online“ erzwingen,
  Stories ausblenden, nachträgliches „für alle löschen“ nach „nur für mich“.

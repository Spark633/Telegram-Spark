package org.telegram.ui.spark;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.spark.SparkConfig;
import org.telegram.messenger.spark.SparkCrashLog;
import org.telegram.messenger.spark.SparkUpdater;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * Einstellungsseite "Sparkgram-Funktionen": jede Zusatzfunktion hat einen eigenen Schalter.
 * Ist der Hauptschalter aus, wird alles darunter ausgegraut und ist nicht aenderbar.
 */
public class SparkSettingsActivity extends UniversalFragment {

    private static final int ID_MASTER = 1;
    private static final int ID_COPY_ID = 2;
    private static final int ID_INFO = 3;
    private static final int ID_CLONE = 4;
    private static final int ID_CLONE_NAME = 5;
    private static final int ID_CLONE_BIO = 6;
    private static final int ID_CLONE_PHOTO = 7;
    private static final int ID_PROFILE_LIBRARY = 8;
    private static final int ID_PHOTO_MANAGER = 9;
    private static final int ID_PHOTO_MANAGER_OPEN = 10;
    private static final int ID_NOTES = 11;
    private static final int ID_NOTES_LIST = 12;
    private static final int ID_MARKER = 13;
    private static final int ID_VERSION = 14;
    private static final int ID_UPDATE_CHECK = 15;
    private static final int ID_UPDATE_TEST = 16;
    private static final int ID_REPO = 17;
    private static final int ID_CRASH = 18;
    private static final int ID_CHAT_MENU = 19;
    private static final int ID_PROFILE_MENU_GROUPED = 21;
    private static final int ID_CHAT_MENU_GROUPED = 22;
    private static final int ID_MEMBER_CHECK = 23;
    private static final int ID_MEMBER_HINT = 24;
    private static final int ID_MEMBER_INTERVAL = 25;
    private static final int ID_INFO_ROW = 26;
    private static final int ID_PHOTO_DATE = 27;
    private static final int ID_KEEP_DELETED = 28;
    private static final int ID_EDIT_HISTORY = 29;
    private static final int ID_CHANGELOG = 30;
    private static final int ID_WHATS_NEW_HINT = 31;
    private static final int ID_KEEP_OWN_DELETED = 32;
    private static final int ID_HIDE_PREMIUM = 33;
    private static final int ID_MSG_DETAILS = 40;
    private static final int ID_MSG_REPEAT = 41;
    private static final int ID_FORWARD_TIME = 42;
    private static final int ID_JUMP_FIRST = 43;
    private static final int ID_DELETE_OWN_ALL = 44;
    private static final int ID_CONFIRM_STICKER = 45;
    private static final int ID_CONFIRM_GIF = 46;
    private static final int ID_CONFIRM_VOICE = 47;
    private static final int ID_NAME_HISTORY = 48;
    private static final int ID_CONFIRM_REPEAT = 49;
    private static final int ID_HIDE_PREMIUM_AUTO = 50;
    private static final int ID_LOCAL_NAMES = 51;
    private static final int ID_TIMEZONES = 52;
    private static final int ID_FORWARD_NO_SENDER = 53;
    private static final int ID_FORWARD_COUNT = 54;
    private static final int ID_GROUP_ADMINS = 55;
    private static final int ID_GROUP_ACTIVE = 56;
    private static final int ID_CHANNEL_AUTHORS = 57;
    private static final int ID_KEYWORDS = 58;
    private static final int ID_KEYWORDS_OPEN = 59;
    private static final int ID_TRANSLATE_FREE = 60;
    private static final int ID_TRANSLATE_PROVIDER = 61;
    private static final int ID_DEEPL_KEY = 62;
    private static final int ID_TRANSLATE_OWN = 63;
    private static final int ID_ONLINE_LOG = 64;
    private static final int ID_SEARCH = 65;
    private static final int ID_DELETED_NAMES = 66;
    private static final int ID_SELECT_NO_LIMIT = 67;
    private static final int ID_SELECT_RANGE = 68;
    private static final int ID_QR_GALLERY = 69;
    private static final int ID_STICKER_TOOLS = 70;
    private static final int ID_VOICE_TOOLS = 71;
    private static final int ID_COLLECTION = 72;
    private static final int ID_COLLECTION_OPEN = 73;
    private static final int ID_BOT_SEND = 74;
    private static final int ID_BOT_TOKEN = 75;
    private static final int ID_STATS = 76;
    private static final int ID_TEMPLATES = 77;
    private static final int ID_TEMPLATES_OPEN = 78;
    private static final int ID_MUTE_PEOPLE = 79;
    private static final int ID_GHOST = 80;
    private static final int ID_GHOST_READ = 81;
    private static final int ID_GHOST_TYPING = 82;
    private static final int ID_GHOST_OFFLINE = 83;
    private static final int ID_EXPORT = 84;
    private static final int ID_BACKUP = 85;
    private static final int ID_MUTE_HIDE = 86;
    private static final int ID_MUTE_LIST = 87;
    private static final int ID_BOT_INBOX = 88;
    private static final int ID_DOUBLE_TAP = 89;
    private static final int ID_BOOKMARKS = 90;
    private static final int ID_BOOKMARKS_OPEN = 91;
    private static final int ID_USER_MESSAGES = 92;
    private static final int ID_ALWAYS_DOWNLOADS = 93;
    private static final int ID_LOCAL_PINS = 94;
    private static final int ID_DELETED_CHATS = 95;
    private static final int ID_DELETED_CHATS_OPEN = 96;
    private static final int ID_CLIPBOARD = 97;
    private static final int ID_CLIPBOARD_TIME = 98;
    private static final int ID_MENU_EDIT = 99;
    private static final int ID_KEEP_LEFT = 100;
    private static final int ID_LEFT_FOLDER = 101;
    private static final int ID_UNREAD_FIRST = 102;
    private static final int ID_HIDDEN_ACCOUNTS = 103;
    private static final int ID_AUTO_FOLDERS = 104;
    private static final int ID_PERSONA = 106;
    private static final int ID_ADMIN_TOOLS = 107;
    private static final int ID_ADMIN_TEMPLATES = 108;
    private static final int ID_LEFT_ONLY_FOLDER = 109;
    private static final int ID_LEAVE_BACKUP = 110;
    private static final int ID_KICK_NOTIFY = 111;
    private static final int ID_KICK_INAPP = 112;
    private static final int ID_KICK_SYSTEM = 113;
    private static final int ID_KICK_STICKY = 114;
    private static final int ID_DUPLICATES = 105;
    private static final int ID_HIDE_STARS = 34;
    private static final int ID_HIDE_BUSINESS = 35;
    private static final int ID_HIDE_WALLET = 36;

    @Override
    protected CharSequence getTitle() {
        return page == 0 ? getString(R.string.SparkSettings) : PAGE_NAMES[page];
    }

    // ---------------------------------------------------------------- Unterseiten (Wunsch Spark633)
    private final int page; // 0 = Uebersicht
    private static final int ID_PAGE_BASE = 9000;
    private static final int ID_DIAG_LOG = 115;
    private static final int ID_JOIN_REQUESTS = 116;
    private static final String[] PAGE_NAMES = {"", "Profil & Personen", "Nachrichten", "Gruppen", "Medien & Bot", "Privatsphäre", "Organisation", "Übersetzen", "Darstellung"};
    private static final String[] PAGE_INFO = {"", "Profil-Werkzeuge, Personen, Notizen", "Werkzeuge, gelöschte Nachrichten, Vor dem Senden fragen, Schlagwort-Alarm",
        "Admins, Admin-Vorlagen, gelöschte & verlassene Chats", "Medien, Bot", "Ghost-Modus, Zwischenablage, Ausblenden", "Lesezeichen, Vorlagen, Statistiken, Duplikate, Auto-Ordner …",
        "Google/DeepL, Eingabe übersetzen", "Chat-Menü, ✦-Markierung, Download-Knopf, Nachrichten-Menü"};
    private static final int[] PAGE_ICONS = {0, R.drawable.msg_contacts, R.drawable.msg_discussion, R.drawable.msg_groups, R.drawable.msg_photos,
        R.drawable.msg_secret, R.drawable.msg_folders, R.drawable.msg_translate, R.drawable.msg_theme};

    public SparkSettingsActivity() {
        this(0);
    }

    public SparkSettingsActivity(int page) {
        this.page = page;
    }

    /** Ueberschrift -> Seite, in der gewuenschten Reihenfolge. */
    private String[][] pageSections() {
        return new String[][]{
            {},
            {getString(R.string.SparkHeaderProfile), getString(R.string.SparkHeaderPeople), getString(R.string.SparkNotes)},
            {getString(R.string.SparkHeaderMsgTools), getString(R.string.SparkHeaderMessages), getString(R.string.SparkHeaderConfirm), getString(R.string.SparkKeywords)},
            {getString(R.string.SparkHeaderGroups)},
            {"Medien", "Bot"},
            {"Privatsphäre", getString(R.string.SparkHeaderHide)},
            {"Organisation"},
            {getString(R.string.SparkHeaderTranslate)},
            {getString(R.string.SparkHeaderLook)},
        };
    }

    /** Liste in Bloecke (je Ueberschrift) zerlegen und nach Seite zusammensetzen. */
    private void arrangePages(ArrayList<UItem> items) {
        ArrayList<UItem> pre = new ArrayList<>();
        java.util.LinkedHashMap<String, ArrayList<UItem>> blocks = new java.util.LinkedHashMap<>();
        ArrayList<UItem> cur = pre;
        for (UItem it : items) {
            if (it.viewType == org.telegram.ui.Components.UniversalAdapter.VIEW_TYPE_HEADER && it.text != null) {
                cur = new ArrayList<>();
                blocks.put(it.text.toString(), cur);
            }
            cur.add(it);
        }
        items.clear();
        String[][] sections = pageSections();
        if (page == 0) {
            items.addAll(pre);
            items.add(UItem.asHeader("Funktionen"));
            for (int p = 1; p < PAGE_NAMES.length; p++) {
                items.add(UItem.asButton(ID_PAGE_BASE + p, PAGE_ICONS[p], PAGE_NAMES[p], ""));
            }
            items.add(UItem.asShadow("Tippe auf einen Bereich. Mit der Lupe oben findest du jede einzelne Einstellung."));
            java.util.HashSet<String> used = new java.util.HashSet<>();
            for (String[] sec : sections) java.util.Collections.addAll(used, sec);
            // alles, was keiner Seite zugeordnet ist (Konten, Updates …), bleibt unten auf der Uebersicht
            for (java.util.Map.Entry<String, ArrayList<UItem>> e : blocks.entrySet()) {
                if (!used.contains(e.getKey())) items.addAll(e.getValue());
            }
        } else {
            for (String h : sections[page]) {
                ArrayList<UItem> b = blocks.get(h);
                if (b != null) items.addAll(b);
            }
        }
    }

    private static UItem check(int id, int text, String key) {
        return UItem.asCheck(id, SparkUi.mark(getString(text))).setChecked(SparkConfig.getRaw(key));
    }

    // Oberpunkte mit Unterpunkten: Zeile antippen = auf-/zuklappen, Schalter = an/aus
    private final java.util.HashSet<String> expanded = new java.util.HashSet<>();

    private UItem expandable(int id, int text, String key, int subCount) {
        return UItem.asExpandableSwitch(id, SparkUi.mark(getString(text)), String.valueOf(subCount))
            .setChecked(SparkConfig.getRaw(key))
            .setCollapsed(!expanded.contains(key))
            .setClickCallback(v -> {
                if (!SparkConfig.getRaw(key)) expanded.add(key);
                toggle(key);
            });
    }

    private String sparkQuery;

    private boolean searching() {
        return sparkQuery != null && !sparkQuery.trim().isEmpty();
    }

    private boolean open(String key) {
        return searching() || expanded.contains(key);
    }

    private void expand(String key) {
        if (!expanded.remove(key)) {
            expanded.add(key);
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private static UItem subCheck(int id, int text, String key) {
        return UItem.asCheck(id, "      " + getString(text)).setChecked(SparkConfig.getRaw(key));
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        final boolean on = SparkConfig.getRaw(SparkConfig.MASTER);
        // Hauptschalter ist oben fest angeheftet (siehe createView), hier nur der Hinweis
        items.add(UItem.asShadow(getString(R.string.SparkMasterInfo)));

        final int start = items.size();

        items.add(UItem.asHeader(getString(R.string.SparkHeaderProfile)));
        items.add(check(ID_COPY_ID, R.string.SparkCopyIdToggle, SparkConfig.PROFILE_COPY_ID));
        items.add(check(ID_INFO, R.string.SparkInfoToggle, SparkConfig.PROFILE_INFO));
        items.add(check(ID_INFO_ROW, R.string.SparkInfoRowToggle, SparkConfig.PROFILE_INFO_ROW));
        items.add(check(ID_PHOTO_DATE, R.string.SparkPhotoDateToggle, SparkConfig.PHOTO_DATE));
        items.add(check(ID_NAME_HISTORY, R.string.SparkNameHistoryToggle, SparkConfig.NAME_HISTORY));
        items.add(check(ID_LOCAL_NAMES, R.string.SparkLocalNamesToggle, SparkConfig.LOCAL_NAMES));
        items.add(check(ID_TIMEZONES, R.string.SparkTimezonesToggle, SparkConfig.TIMEZONES));
        items.add(expandable(ID_CLONE, R.string.SparkCloneToggle, SparkConfig.PROFILE_CLONE, 4));
        if (open(SparkConfig.PROFILE_CLONE)) {
            items.add(subCheck(ID_CLONE_NAME, R.string.SparkCloneName, SparkConfig.CLONE_NAME));
            items.add(subCheck(ID_CLONE_BIO, R.string.SparkCloneBio, SparkConfig.CLONE_BIO));
            items.add(subCheck(ID_CLONE_PHOTO, R.string.SparkClonePhoto, SparkConfig.CLONE_PHOTO));
            items.add(UItem.asButton(ID_PROFILE_LIBRARY, R.drawable.msg_contacts, getString(R.string.SparkProfileLibrary)));
        }
        items.add(UItem.asButton(ID_PHOTO_MANAGER_OPEN, R.drawable.msg_photos, "Profilbilder verwalten"));
        items.add(UItem.asShadow(getString(R.string.SparkProfileToolsInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderMessages)));
        items.add(check(ID_KEEP_DELETED, R.string.SparkKeepDeletedToggle, SparkConfig.KEEP_DELETED));
        items.add(check(ID_EDIT_HISTORY, R.string.SparkEditHistoryToggle, SparkConfig.EDIT_HISTORY));
        items.add(UItem.asShadow(getString(R.string.SparkMessagesInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderMsgTools)));
        items.add(check(ID_MSG_DETAILS, R.string.SparkMsgDetailsToggle, SparkConfig.MSG_DETAILS));
        items.add(check(ID_MSG_REPEAT, R.string.SparkMsgRepeatToggle, SparkConfig.MSG_REPEAT));
        items.add(check(ID_FORWARD_TIME, R.string.SparkForwardTimeToggle, SparkConfig.FORWARD_TIME));
        items.add(check(ID_FORWARD_NO_SENDER, R.string.SparkForwardNoSenderToggle, SparkConfig.FORWARD_NO_SENDER));
        items.add(check(ID_FORWARD_COUNT, R.string.SparkForwardCountToggle, SparkConfig.FORWARD_COUNT));
        items.add(check(ID_JUMP_FIRST, R.string.SparkJumpFirstToggle, SparkConfig.JUMP_FIRST));
        items.add(check(ID_DELETE_OWN_ALL, R.string.SparkDeleteOwnToggle, SparkConfig.DELETE_OWN_ALL));
        items.add(check(ID_SELECT_RANGE, R.string.SparkSelectRangeToggle, SparkConfig.SELECT_RANGE));
        items.add(check(ID_USER_MESSAGES, R.string.SparkUserMessagesToggle, SparkConfig.USER_MESSAGES));
        items.add(UItem.asButton(ID_DOUBLE_TAP, SparkUi.mark("Doppeltipp"), SparkConfig.DOUBLE_TAP_NAMES[Math.max(0, Math.min(SparkConfig.DOUBLE_TAP_NAMES.length - 1, SparkConfig.prefs().getInt("doubleTap", 0)))]));
        items.add(UItem.asShadow(getString(R.string.SparkMsgToolsInfo)));

        items.add(UItem.asHeader("Medien"));
        items.add(expandable(ID_COLLECTION, R.string.SparkCollectionToggle, SparkConfig.COLLECTION, 1));
        if (open(SparkConfig.COLLECTION)) {
            items.add(UItem.asButton(ID_COLLECTION_OPEN, R.drawable.msg_saved, "Sammlung öffnen"));
        }
        items.add(check(ID_STICKER_TOOLS, R.string.SparkStickerToolsToggle, SparkConfig.STICKER_TOOLS));
        items.add(check(ID_VOICE_TOOLS, R.string.SparkVoiceToolsToggle, SparkConfig.VOICE_TOOLS));
        items.add(check(ID_QR_GALLERY, R.string.SparkQrGalleryToggle, SparkConfig.QR_GALLERY));
        items.add(UItem.asShadow(getString(R.string.SparkMediaInfo)));

        items.add(UItem.asHeader("Bot"));
        items.add(expandable(ID_BOT_SEND, R.string.SparkBotSendToggle, SparkConfig.BOT_SEND, 2));
        if (open(SparkConfig.BOT_SEND)) {
            String bn = org.telegram.messenger.spark.SparkBot.botName();
            items.add(UItem.asButton(ID_BOT_TOKEN, "      Bot-Token", android.text.TextUtils.isEmpty(org.telegram.messenger.spark.SparkBot.token()) ? "fehlt" : (bn != null ? bn : "gesetzt")));
            items.add(UItem.asButton(ID_BOT_INBOX, "      Bot-Postfach"));
        }
        items.add(UItem.asShadow(getString(R.string.SparkBotInfo)));

        items.add(UItem.asHeader("Privatsphäre"));
        items.add(expandable(ID_GHOST, R.string.SparkGhostToggle, SparkConfig.GHOST, 3));
        if (open(SparkConfig.GHOST)) {
            items.add(subCheck(ID_GHOST_READ, R.string.SparkGhostRead, SparkConfig.GHOST_READ));
            items.add(subCheck(ID_GHOST_TYPING, R.string.SparkGhostTyping, SparkConfig.GHOST_TYPING));
            items.add(subCheck(ID_GHOST_OFFLINE, R.string.SparkGhostOffline, SparkConfig.GHOST_OFFLINE));
        }
        items.add(expandable(ID_CLIPBOARD, R.string.SparkClipboardToggle, SparkConfig.CLIPBOARD_CLEAR, 1));
        if (open(SparkConfig.CLIPBOARD_CLEAR)) {
            items.add(UItem.asButton(ID_CLIPBOARD_TIME, "      Löschen nach", org.telegram.messenger.spark.SparkClipboard.delaySeconds() + " s"));
        }
        items.add(UItem.asShadow(getString(R.string.SparkGhostInfo)));

        items.add(UItem.asHeader("Organisation"));
        items.add(UItem.asButton(ID_STATS, R.drawable.msg_stats, "Statistiken"));
        items.add(UItem.asButton(ID_DUPLICATES, R.drawable.msg_saved, "Duplikate in Gespeichertes"));
        items.add(UItem.asButton(ID_AUTO_FOLDERS, R.drawable.msg_folders, "Auto-Ordner"));
        items.add(expandable(ID_BOOKMARKS, R.string.SparkBookmarksToggle, SparkConfig.BOOKMARKS, 1));
        if (open(SparkConfig.BOOKMARKS)) {
            items.add(UItem.asButton(ID_BOOKMARKS_OPEN, "      Lesezeichen öffnen"));
        }
        items.add(expandable(ID_TEMPLATES, R.string.SparkTemplatesToggle, SparkConfig.TEMPLATES, 1));
        if (open(SparkConfig.TEMPLATES)) {
            items.add(UItem.asButton(ID_TEMPLATES_OPEN, R.drawable.msg_stories_caption, "Vorlagen verwalten"));
        }
        items.add(UItem.asCheck(ID_PERSONA, SparkUi.mark("RP-Persona pro Chat (Chat-Menü)")).setChecked(SparkConfig.getRaw(SparkConfig.PERSONA)));
        items.add(expandable(ID_MUTE_PEOPLE, R.string.SparkMutePeopleToggle, SparkConfig.MUTE_PEOPLE, 1));
        if (open(SparkConfig.MUTE_PEOPLE)) {
            items.add(UItem.asButton(ID_MUTE_LIST, "      Stummgeschaltete (" + org.telegram.messenger.spark.SparkPeople.allMuted(currentAccount).size() + ")"));
        }
        items.add(check(ID_EXPORT, R.string.SparkExportToggle, SparkConfig.EXPORT));
        items.add(check(ID_LOCAL_PINS, R.string.SparkLocalPinsToggle, SparkConfig.LOCAL_PINS));
        items.add(check(ID_UNREAD_FIRST, R.string.SparkUnreadFirstToggle, SparkConfig.UNREAD_FIRST));
        items.add(UItem.asShadow(getString(R.string.SparkOrgInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderPeople)));
        items.add(check(ID_ONLINE_LOG, R.string.SparkOnlineLogToggle, SparkConfig.ONLINE_LOG));
        items.add(check(ID_DELETED_NAMES, R.string.SparkDeletedNamesToggle, SparkConfig.DELETED_NAMES));
        items.add(UItem.asShadow(getString(R.string.SparkOnlineLogInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderTranslate)));
        items.add(expandable(ID_TRANSLATE_FREE, R.string.SparkTranslateFreeToggle, SparkConfig.TRANSLATE_FREE, 2));
        items.add(check(ID_TRANSLATE_OWN, R.string.SparkTranslateOwnToggle, SparkConfig.TRANSLATE_OWN));
        if (open(SparkConfig.TRANSLATE_FREE)) {
            items.add(UItem.asButton(ID_TRANSLATE_PROVIDER, "      " + getString(R.string.SparkTranslateProvider),
                org.telegram.messenger.spark.SparkTranslate.provider() == org.telegram.messenger.spark.SparkTranslate.PROVIDER_DEEPL ? "DeepL" : "Google"));
            if (org.telegram.messenger.spark.SparkTranslate.provider() == org.telegram.messenger.spark.SparkTranslate.PROVIDER_DEEPL) {
                String key = org.telegram.messenger.spark.SparkTranslate.deeplKey();
                items.add(UItem.asButton(ID_DEEPL_KEY, "      " + getString(R.string.SparkDeeplKey), key == null ? "fehlt" : "••••" + key.substring(Math.max(0, key.length() - 4))));
            }
        }
        items.add(UItem.asShadow(getString(R.string.SparkTranslateInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkKeywords)));
        items.add(expandable(ID_KEYWORDS, R.string.SparkKeywordsToggle, SparkConfig.KEYWORDS, 1));
        if (open(SparkConfig.KEYWORDS)) {
            items.add(UItem.asButton(ID_KEYWORDS_OPEN, R.drawable.msg_mention, getString(R.string.SparkKeywordsOpen)));
        }
        items.add(UItem.asShadow(getString(R.string.SparkKeywordsInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderConfirm)));
        items.add(check(ID_CONFIRM_STICKER, R.string.SparkConfirmSticker, SparkConfig.CONFIRM_STICKER));
        items.add(check(ID_CONFIRM_GIF, R.string.SparkConfirmGif, SparkConfig.CONFIRM_GIF));
        items.add(check(ID_CONFIRM_VOICE, R.string.SparkConfirmVoice, SparkConfig.CONFIRM_VOICE));
        items.add(check(ID_CONFIRM_REPEAT, R.string.SparkConfirmRepeat, SparkConfig.CONFIRM_REPEAT));
        items.add(UItem.asShadow(getString(R.string.SparkConfirmInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderGroups)));
        items.add(check(ID_GROUP_ADMINS, R.string.SparkAdminsList, SparkConfig.GROUP_ADMINS));
        items.add(UItem.asCheck(ID_JOIN_REQUESTS, SparkUi.mark("Knopf „Beitrittsanfragen“ in der Chatliste")).setChecked(SparkConfig.getRaw(SparkConfig.JOIN_REQUESTS)));
        items.add(expandable(ID_ADMIN_TOOLS, R.string.SparkAdminToolsToggle, SparkConfig.ADMIN_TOOLS, 1));
        if (open(SparkConfig.ADMIN_TOOLS)) {
            items.add(UItem.asButton(ID_ADMIN_TEMPLATES, R.drawable.msg_admins, "Admin-Vorlagen verwalten"));
        }
        items.add(check(ID_GROUP_ACTIVE, R.string.SparkActiveMembers, SparkConfig.GROUP_ACTIVE));
        items.add(check(ID_CHANNEL_AUTHORS, R.string.SparkKnownAuthorsToggle, SparkConfig.CHANNEL_AUTHORS));
        items.add(expandable(ID_MEMBER_CHECK, R.string.SparkMemberCheckToggle, SparkConfig.MEMBER_CHECK, 2));
        if (open(SparkConfig.MEMBER_CHECK)) {
            items.add(subCheck(ID_MEMBER_HINT, R.string.SparkMemberHintToggle, SparkConfig.MEMBER_CHECK_HINT));
            items.add(UItem.asButton(ID_MEMBER_INTERVAL, "      " + getString(R.string.SparkMemberInterval), (org.telegram.messenger.spark.SparkMembership.intervalMs() / 1000) + " s"));
        }
        items.add(expandable(ID_DELETED_CHATS, R.string.SparkDeletedChatsToggle, SparkConfig.DELETED_CHATS, 4));
        if (open(SparkConfig.DELETED_CHATS)) {
            items.add(subCheck(ID_KEEP_LEFT, R.string.SparkKeepLeftToggle, SparkConfig.KEEP_LEFT_CHATS));
            items.add(subCheck(ID_LEFT_FOLDER, R.string.SparkLeftFolderToggle, SparkConfig.LEFT_FOLDER));
            if (SparkConfig.getRaw(SparkConfig.LEFT_FOLDER)) {
                items.add(UItem.asCheck(ID_LEFT_ONLY_FOLDER, "      Nur im Ordner „Verlassen“ anzeigen").setChecked(SparkConfig.getRaw(SparkConfig.LEFT_ONLY_FOLDER)));
            }
            items.add(UItem.asCheck(ID_LEAVE_BACKUP, "      Vor dem Verlassen Verlauf sichern").setChecked(SparkConfig.getRaw(SparkConfig.LEAVE_BACKUP)));
            items.add(UItem.asCheck(ID_KICK_NOTIFY, "      Melden, wenn ich entfernt werde").setChecked(SparkConfig.getRaw(SparkConfig.KICK_NOTIFY)));
            if (SparkConfig.getRaw(SparkConfig.KICK_NOTIFY)) {
                items.add(UItem.asCheck(ID_KICK_INAPP, "            als Einblendung oben in der App").setChecked(SparkConfig.prefs().getBoolean("kickNotifyInApp", true)));
                if (SparkConfig.prefs().getBoolean("kickNotifyInApp", true)) {
                    items.add(UItem.asCheck(ID_KICK_STICKY, "                  bleibt, bis ich sie wegklicke").setChecked(SparkConfig.prefs().getBoolean("kickNotifySticky", false)));
                }
                items.add(UItem.asCheck(ID_KICK_SYSTEM, "            als Android-Benachrichtigung").setChecked(SparkConfig.prefs().getBoolean("kickNotifySystem", true)));
            }
            items.add(UItem.asButton(ID_DELETED_CHATS_OPEN, "      Liste öffnen"));
            items.add(UItem.asShadow("Verlassene Gruppen zeigt Sparkgram auch dann, wenn sie schon länger in Telegrams Speicher auf diesem Gerät liegen. Dabei können auch Gruppen auftauchen, die du nur über einen Link angesehen hast oder in die dich jemand ungefragt hinzugefügt hat."));
        }
        items.add(UItem.asShadow(getString(R.string.SparkMemberInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkNotes)));
        items.add(expandable(ID_NOTES, R.string.SparkNotesToggle, SparkConfig.NOTES, 1));
        if (open(SparkConfig.NOTES)) {
            items.add(UItem.asButton(ID_NOTES_LIST, R.drawable.msg_stories_caption, getString(R.string.SparkNotesAll)));
        }
        items.add(UItem.asShadow(getString(R.string.SparkNotesInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderHide)));
        items.add(expandable(ID_HIDE_PREMIUM, R.string.SparkHidePremium, SparkConfig.HIDE_PREMIUM, 1));
        if (open(SparkConfig.HIDE_PREMIUM)) {
            items.add(subCheck(ID_HIDE_PREMIUM_AUTO, R.string.SparkHidePremiumAuto, SparkConfig.HIDE_PREMIUM_AUTO));
        }
        items.add(check(ID_HIDE_STARS, R.string.SparkHideStars, SparkConfig.HIDE_STARS));
        items.add(check(ID_HIDE_BUSINESS, R.string.SparkHideBusiness, SparkConfig.HIDE_BUSINESS));
        items.add(check(ID_HIDE_WALLET, R.string.SparkHideWallet, SparkConfig.HIDE_WALLET));
        items.add(UItem.asShadow(getString(R.string.SparkHideInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderLook)));
        items.add(check(ID_CHAT_MENU, R.string.SparkChatMenuToggle, SparkConfig.CHAT_MENU));
        items.add(check(ID_MARKER, R.string.SparkMarkerToggle, SparkConfig.SPARK_MARKER));
        items.add(check(ID_ALWAYS_DOWNLOADS, R.string.SparkAlwaysDownloadsToggle, SparkConfig.ALWAYS_DOWNLOADS));
        items.add(UItem.asButton(ID_MENU_EDIT, R.drawable.msg_list, "Nachrichten-Menü anpassen"));
        items.add(UItem.asShadow(getString(R.string.SparkLookInfo)));

        // Ohne Hauptschalter: alles bis hier ausgrauen
        if (!on) {
            for (int i = start; i < items.size(); i++) {
                items.get(i).setEnabled(false);
            }
        }

        // Konten: immer aktiv, nicht vom Hauptschalter betroffen
        items.add(UItem.asHeader(getString(R.string.SparkHeaderAccounts)));
        items.add(UItem.asButton(ID_BACKUP, R.drawable.msg_download, "Sicherung & Konten übertragen"));
        items.add(UItem.asButton(ID_HIDDEN_ACCOUNTS, R.drawable.msg_secret, "Versteckte Konten"));
        items.add(UItem.asShadow(getString(R.string.SparkAccountsInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderUpdates)));
        items.add(UItem.asButton(ID_VERSION, R.drawable.msg_info, getString(R.string.SparkVersion), SparkUpdater.buildLabel()));
        items.add(UItem.asButton(ID_UPDATE_CHECK, R.drawable.msg_retry, getString(R.string.SparkUpdateCheck)));
        items.add(UItem.asCheck(ID_UPDATE_TEST, getString(R.string.SparkUpdateTestBuilds)).setChecked(SparkConfig.getRaw(SparkConfig.UPDATE_TEST_BUILDS)));
        items.add(UItem.asButton(ID_CHANGELOG, R.drawable.msg_stories_caption, "Was ist neu?"));
        items.add(UItem.asCheck(ID_WHATS_NEW_HINT, "Nach Updates „Was ist neu?“ zeigen").setChecked(SparkConfig.getRaw(SparkConfig.WHATS_NEW_HINT)));
        items.add(UItem.asButton(ID_REPO, R.drawable.msg_link2, getString(R.string.SparkRepo)));
        items.add(UItem.asButton(ID_DIAG_LOG, R.drawable.msg_log, "Diagnose-Protokoll"));
        items.add(UItem.asShadow(getString(R.string.SparkUpdateInfo)));

        if (SparkCrashLog.read(getContext()) != null) {
            items.add(UItem.asButton(ID_CRASH, R.drawable.msg_report, getString(R.string.SparkCrashLog)).red());
            items.add(UItem.asShadow(null));
        }

        // Suche: nur passende Eintraege (Name, Beschreibung oder Wert) zeigen
        if (searching()) {
            String q = sparkQuery.trim().toLowerCase();
            // Ueberschrift -> Bereich (fuer die Anzeige der Kategorie)
            java.util.HashMap<String, String> area = new java.util.HashMap<>();
            String[][] secs = pageSections();
            for (int p = 1; p < secs.length; p++) for (String h : secs[p]) area.put(h, PAGE_NAMES[p]);
            java.util.LinkedHashMap<String, ArrayList<UItem>> found = new java.util.LinkedHashMap<>();
            String curHeader = "";
            int count = 0;
            for (UItem it : items) {
                if (it.viewType == org.telegram.ui.Components.UniversalAdapter.VIEW_TYPE_HEADER && it.text != null) {
                    curHeader = it.text.toString();
                    continue;
                }
                if (it.id == 0) continue;
                String t = ((it.text != null ? it.text : "") + " " + (it.subtext != null ? it.subtext : "") + " " + (it.textValue != null ? it.textValue : "")).toLowerCase();
                if (!t.contains(q)) continue;
                String cat = area.containsKey(curHeader) ? area.get(curHeader) + " › " + curHeader : curHeader;
                if (area.containsKey(curHeader) && area.get(curHeader).equals(curHeader)) cat = curHeader;
                ArrayList<UItem> list = found.get(cat);
                if (list == null) found.put(cat, list = new ArrayList<>());
                list.add(it);
                count++;
            }
            items.clear();
            for (java.util.Map.Entry<String, ArrayList<UItem>> e : found.entrySet()) {
                items.add(UItem.asHeader(e.getKey()));
                items.addAll(e.getValue());
            }
            items.add(UItem.asShadow(count == 0 ? "Nichts gefunden zu „" + sparkQuery.trim() + "“." : count + " Treffer zu „" + sparkQuery.trim() + "“"));
        } else {
            arrangePages(items);
        }
    }

    private void sparkRefreshLeftFolder() {
        getMessagesController().sparkEnsureLeftFilter();
        getNotificationCenter().postNotificationName(org.telegram.messenger.NotificationCenter.dialogFiltersUpdated);
    }

    private void toggle(String key) {
        SparkConfig.toggle(key);
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private org.telegram.ui.Cells.TextCheckCell masterCell;

    @Override
    public View createView(android.content.Context context) {
        View view = super.createView(context);
        // Konto, fuer das diese Einstellungen gelten
        org.telegram.tgnet.TLRPC.User me = getUserConfig().getCurrentUser();
        if (me != null) {
            actionBar.setSubtitle("Konto: " + org.telegram.messenger.UserObject.getUserName(me));
        }
        if (page != 0) {
            return view; // Unterseite: ohne Hauptschalter und Suche
        }
        // Hauptschalter oben fest angeheftet, bleibt beim Scrollen sichtbar
        masterCell = new org.telegram.ui.Cells.TextCheckCell(context);
        masterCell.setBackgroundColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhite));
        masterCell.setTextAndCheck(getString(R.string.SparkMaster), SparkConfig.getRaw(SparkConfig.MASTER), false);
        masterCell.setOnClickListener(v -> {
            boolean value = SparkConfig.toggle(SparkConfig.MASTER);
            masterCell.setChecked(value);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        masterCell.setElevation(AndroidUtilities.dp(2));
        // Suche oben rechts
        org.telegram.ui.ActionBar.ActionBarMenuItem searchItem = actionBar.createMenu().addItem(0, R.drawable.outline_header_search);
        searchItem.setIsSearchField(true).setActionBarMenuItemSearchListener(new org.telegram.ui.ActionBar.ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onTextChanged(android.widget.EditText editText) {
                sparkQuery = editText.getText().toString();
                if (listView != null && listView.adapter != null) listView.adapter.update(false);
            }

            @Override
            public void onSearchCollapse() {
                sparkQuery = null;
                if (listView != null && listView.adapter != null) listView.adapter.update(false);
            }
        });
        searchItem.setSearchFieldHint("Einstellung suchen");
        ((android.widget.FrameLayout) view).addView(masterCell, org.telegram.ui.Components.LayoutHelper.createFrame(org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, 50, android.view.Gravity.TOP));
        listView.setPadding(0, AndroidUtilities.dp(50), 0, 0);
        listView.setClipToPadding(false);
        return view;
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id > ID_PAGE_BASE && item.id < ID_PAGE_BASE + PAGE_NAMES.length) {
            presentFragment(new SparkSettingsActivity(item.id - ID_PAGE_BASE));
            return;
        }
        if (!item.enabled) {
            return;
        }
        switch (item.id) {
            case ID_MASTER: toggle(SparkConfig.MASTER); break;
            case ID_COPY_ID: toggle(SparkConfig.PROFILE_COPY_ID); break;
            case ID_INFO: toggle(SparkConfig.PROFILE_INFO); break;
            case ID_INFO_ROW: toggle(SparkConfig.PROFILE_INFO_ROW); break;
            case ID_PHOTO_DATE: toggle(SparkConfig.PHOTO_DATE); break;
            case ID_KEEP_DELETED: toggle(SparkConfig.KEEP_DELETED); break;
            case ID_KEEP_OWN_DELETED: toggle(SparkConfig.KEEP_OWN_DELETED); break;
            case ID_HIDE_PREMIUM: expand(SparkConfig.HIDE_PREMIUM); break;
            case ID_MSG_DETAILS: toggle(SparkConfig.MSG_DETAILS); break;
            case ID_MSG_REPEAT: toggle(SparkConfig.MSG_REPEAT); break;
            case ID_FORWARD_TIME: toggle(SparkConfig.FORWARD_TIME); break;
            case ID_JUMP_FIRST: toggle(SparkConfig.JUMP_FIRST); break;
            case ID_DELETE_OWN_ALL: toggle(SparkConfig.DELETE_OWN_ALL); break;
            case ID_SELECT_NO_LIMIT: toggle(SparkConfig.SELECT_NO_LIMIT); break;
            case ID_SELECT_RANGE: toggle(SparkConfig.SELECT_RANGE); break;
            case ID_QR_GALLERY: toggle(SparkConfig.QR_GALLERY); break;
            case ID_STICKER_TOOLS: toggle(SparkConfig.STICKER_TOOLS); break;
            case ID_VOICE_TOOLS: toggle(SparkConfig.VOICE_TOOLS); break;
            case ID_COLLECTION: expand(SparkConfig.COLLECTION); break;
            case ID_COLLECTION_OPEN: presentFragment(new SparkCollectionActivity(0)); break;
            case ID_BOT_SEND: expand(SparkConfig.BOT_SEND); break;
            case ID_BOT_INBOX: presentFragment(new SparkBotInboxActivity()); break;
            case ID_USER_MESSAGES: toggle(SparkConfig.USER_MESSAGES); break;
            case ID_ALWAYS_DOWNLOADS: toggle(SparkConfig.ALWAYS_DOWNLOADS); break;
            case ID_LOCAL_PINS: toggle(SparkConfig.LOCAL_PINS); break;
            case ID_UNREAD_FIRST: {
                toggle(SparkConfig.UNREAD_FIRST);
                getMessagesController().sortDialogs(null);
                getNotificationCenter().postNotificationName(org.telegram.messenger.NotificationCenter.dialogsNeedReload);
                break;
            }
            case ID_HIDDEN_ACCOUNTS: presentFragment(new SparkHiddenAccountsActivity()); break;
            case ID_KEEP_LEFT: toggle(SparkConfig.KEEP_LEFT_CHATS); sparkRefreshLeftFolder(); break;
            case ID_LEFT_FOLDER: toggle(SparkConfig.LEFT_FOLDER); sparkRefreshLeftFolder(); break;
            case ID_DELETED_CHATS: expand(SparkConfig.DELETED_CHATS); break;
            case ID_DELETED_CHATS_OPEN: presentFragment(new SparkDeletedChatsActivity()); break;
            case ID_CLIPBOARD: expand(SparkConfig.CLIPBOARD_CLEAR); break;
            case ID_MENU_EDIT: presentFragment(new SparkMenuActivity()); break;
            case ID_CLIPBOARD_TIME: {
                int[] secs = org.telegram.messenger.spark.SparkClipboard.SECONDS;
                CharSequence[] names = new CharSequence[secs.length];
                for (int i = 0; i < secs.length; i++) names[i] = secs[i] < 60 ? secs[i] + " Sekunden" : (secs[i] / 60) + " Minute" + (secs[i] >= 120 ? "n" : "");
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
                b.setTitle(SparkUi.mark("Zwischenablage leeren nach"));
                b.setItems(names, (d, w) -> {
                    org.telegram.messenger.spark.SparkClipboard.setDelaySeconds(secs[w]);
                    listView.adapter.update(true);
                });
                showDialog(b.create());
                break;
            }
            case ID_BOOKMARKS: expand(SparkConfig.BOOKMARKS); break;
            case ID_BOOKMARKS_OPEN: presentFragment(new SparkBookmarksActivity()); break;
            case ID_DOUBLE_TAP: {
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
                b.setTitle(SparkUi.mark("Doppeltipp auf eine Nachricht"));
                b.setItems(SparkConfig.DOUBLE_TAP_NAMES, (d, w) -> {
                    SparkConfig.setDoubleTap(w);
                    listView.adapter.update(true);
                });
                showDialog(b.create());
                break;
            }
            case ID_STATS: presentFragment(new SparkStatsActivity()); break;
            case ID_DUPLICATES: presentFragment(new SparkDuplicatesActivity()); break;
            case ID_AUTO_FOLDERS: presentFragment(new SparkAutoFoldersActivity()); break;
            case ID_PERSONA: toggle(SparkConfig.PERSONA); break;
            case ID_ADMIN_TOOLS: expand(SparkConfig.ADMIN_TOOLS); break;
            case ID_LEAVE_BACKUP: toggle(SparkConfig.LEAVE_BACKUP); break;
            case ID_JOIN_REQUESTS: toggle(SparkConfig.JOIN_REQUESTS); break;
            case ID_KICK_NOTIFY: toggle(SparkConfig.KICK_NOTIFY); break;
            case ID_KICK_INAPP:
            case ID_KICK_SYSTEM:
            case ID_KICK_STICKY: {
                String k = item.id == ID_KICK_INAPP ? "kickNotifyInApp" : item.id == ID_KICK_SYSTEM ? "kickNotifySystem" : "kickNotifySticky";
                boolean def = item.id != ID_KICK_STICKY;
                SparkConfig.prefs().edit().putBoolean(k, !SparkConfig.prefs().getBoolean(k, def)).apply();
                listView.adapter.update(true);
                break;
            }
            case ID_LEFT_ONLY_FOLDER:
                toggle(SparkConfig.LEFT_ONLY_FOLDER);
                getMessagesController().sortDialogs(null);
                getNotificationCenter().postNotificationName(org.telegram.messenger.NotificationCenter.dialogsNeedReload);
                break;
            case ID_ADMIN_TEMPLATES: presentFragment(new SparkAdminActivity()); break;
            case ID_TEMPLATES: expand(SparkConfig.TEMPLATES); break;
            case ID_TEMPLATES_OPEN: presentFragment(new SparkTemplatesActivity()); break;
            case ID_MUTE_PEOPLE: expand(SparkConfig.MUTE_PEOPLE); break;
            case ID_MUTE_HIDE: toggle(SparkConfig.MUTE_HIDE); break;
            case ID_MUTE_LIST: presentFragment(new SparkMutedActivity()); break;
            case ID_EXPORT: toggle(SparkConfig.EXPORT); break;
            case ID_BACKUP: presentFragment(new SparkBackupActivity()); break;
            case ID_GHOST: expand(SparkConfig.GHOST); break;
            case ID_GHOST_READ: toggle(SparkConfig.GHOST_READ); break;
            case ID_GHOST_TYPING: toggle(SparkConfig.GHOST_TYPING); break;
            case ID_GHOST_OFFLINE: toggle(SparkConfig.GHOST_OFFLINE); break;
            case ID_BOT_TOKEN: SparkMedia.editBotToken(this, () -> listView.adapter.update(true)); break;
            case ID_CONFIRM_STICKER: toggle(SparkConfig.CONFIRM_STICKER); break;
            case ID_CONFIRM_GIF: toggle(SparkConfig.CONFIRM_GIF); break;
            case ID_CONFIRM_VOICE: toggle(SparkConfig.CONFIRM_VOICE); break;
            case ID_CONFIRM_REPEAT: toggle(SparkConfig.CONFIRM_REPEAT); break;
            case ID_HIDE_PREMIUM_AUTO: toggle(SparkConfig.HIDE_PREMIUM_AUTO); break;
            case ID_LOCAL_NAMES: toggle(SparkConfig.LOCAL_NAMES); break;
            case ID_TIMEZONES: toggle(SparkConfig.TIMEZONES); break;
            case ID_FORWARD_NO_SENDER: toggle(SparkConfig.FORWARD_NO_SENDER); break;
            case ID_FORWARD_COUNT: toggle(SparkConfig.FORWARD_COUNT); break;
            case ID_GROUP_ADMINS: toggle(SparkConfig.GROUP_ADMINS); break;
            case ID_GROUP_ACTIVE: toggle(SparkConfig.GROUP_ACTIVE); break;
            case ID_CHANNEL_AUTHORS: toggle(SparkConfig.CHANNEL_AUTHORS); break;
            case ID_KEYWORDS: expand(SparkConfig.KEYWORDS); break;
            case ID_TRANSLATE_FREE: expand(SparkConfig.TRANSLATE_FREE); break;
            case ID_TRANSLATE_OWN: toggle(SparkConfig.TRANSLATE_OWN); break;
            case ID_ONLINE_LOG: toggle(SparkConfig.ONLINE_LOG); break;
            case ID_DELETED_NAMES: toggle(SparkConfig.DELETED_NAMES); break;
            case ID_TRANSLATE_PROVIDER: {
                int p = org.telegram.messenger.spark.SparkTranslate.provider();
                org.telegram.messenger.spark.SparkTranslate.setProvider(p == 0 ? 1 : 0);
                listView.adapter.update(true);
                break;
            }
            case ID_DEEPL_KEY: {
                final android.widget.EditText input = new android.widget.EditText(getParentActivity());
                input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                input.setHint("xxxxxxxx-xxxx-…:fx");
                input.setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhiteBlackText));
                input.setHintTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhiteHintText));
                android.widget.FrameLayout box = new android.widget.FrameLayout(getParentActivity());
                box.addView(input, org.telegram.ui.Components.LayoutHelper.createFrame(org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT, 0, 24, 0, 24, 0));
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
                b.setTitle(getString(R.string.SparkDeeplKey));
                b.setMessage("Den Schlüssel findest du in deinem DeepL-Konto unter „API-Schlüssel“. Er wird nur auf diesem Gerät gespeichert.");
                b.setView(box);
                b.setPositiveButton(getString(R.string.Save), (d, w) -> {
                    org.telegram.messenger.spark.SparkTranslate.setDeeplKey(input.getText().toString());
                    listView.adapter.update(true);
                });
                b.setNeutralButton("Entfernen", (d, w) -> {
                    org.telegram.messenger.spark.SparkTranslate.setDeeplKey(null);
                    listView.adapter.update(true);
                });
                b.setNegativeButton(getString(R.string.Cancel), null);
                showDialog(b.create());
                break;
            }
            case ID_KEYWORDS_OPEN: presentFragment(new SparkKeywordsActivity()); break;
            case ID_NAME_HISTORY: toggle(SparkConfig.NAME_HISTORY); break;
            case ID_HIDE_STARS: toggle(SparkConfig.HIDE_STARS); break;
            case ID_HIDE_BUSINESS: toggle(SparkConfig.HIDE_BUSINESS); break;
            case ID_HIDE_WALLET: toggle(SparkConfig.HIDE_WALLET); break;

            case ID_CHANGELOG: presentFragment(new SparkChangelogActivity()); break;
            case ID_WHATS_NEW_HINT: toggle(SparkConfig.WHATS_NEW_HINT); break;
            case ID_EDIT_HISTORY: toggle(SparkConfig.EDIT_HISTORY); break;
            case ID_CLONE: expand(SparkConfig.PROFILE_CLONE); break;
            case ID_CLONE_NAME: toggle(SparkConfig.CLONE_NAME); break;
            case ID_CLONE_BIO: toggle(SparkConfig.CLONE_BIO); break;
            case ID_CLONE_PHOTO: toggle(SparkConfig.CLONE_PHOTO); break;
            case ID_PHOTO_MANAGER: expand(SparkConfig.PHOTO_MANAGER); break;
            case ID_NOTES: expand(SparkConfig.NOTES); break;
            case ID_MARKER: toggle(SparkConfig.SPARK_MARKER); break;
            case ID_CHAT_MENU: toggle(SparkConfig.CHAT_MENU); break;
            case ID_PROFILE_MENU_GROUPED: toggle(SparkConfig.PROFILE_MENU_GROUPED); break;
            case ID_CHAT_MENU_GROUPED: toggle(SparkConfig.CHAT_MENU_GROUPED); break;
            case ID_MEMBER_CHECK: expand(SparkConfig.MEMBER_CHECK); break;
            case ID_MEMBER_HINT: toggle(SparkConfig.MEMBER_CHECK_HINT); break;
            case ID_MEMBER_INTERVAL: {
                int index = SparkConfig.prefs().getInt("memberCheckInterval", 0);
                index = (index + 1) % org.telegram.messenger.spark.SparkMembership.INTERVALS.length;
                SparkConfig.prefs().edit().putInt("memberCheckInterval", index).apply();
                listView.adapter.update(true);
                break;
            }
            case ID_UPDATE_TEST: toggle(SparkConfig.UPDATE_TEST_BUILDS); break;
            case ID_PROFILE_LIBRARY: presentFragment(new SparkProfileLibraryActivity()); break;
            case ID_PHOTO_MANAGER_OPEN: presentFragment(new SparkPhotoManagerActivity()); break;
            case ID_NOTES_LIST: presentFragment(new SparkNotesActivity()); break;
            case ID_UPDATE_CHECK: checkForUpdate(); break;
            case ID_REPO: Browser.openUrl(getParentActivity(), SparkUpdater.REPO_URL); break;
            case ID_DIAG_LOG: {
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
                b.setTitle(SparkUi.mark("Diagnose-Protokoll"));
                b.setMessage(org.telegram.messenger.spark.SparkLog.all());
                b.setPositiveButton("Kopieren", (d, w) -> {
                    AndroidUtilities.addToClipboard(org.telegram.messenger.spark.SparkLog.all());
                    BulletinFactory.of(this).createCopyBulletin("Protokoll kopiert").show();
                });
                b.setNeutralButton("Leeren", (d, w) -> org.telegram.messenger.spark.SparkLog.clear());
                b.setNegativeButton("Schließen", null);
                showDialog(b.create());
                break;
            }
            case ID_VERSION: {
                String c = org.telegram.messenger.BuildConfig.SPARK_COMMIT;
                final String text = "Sparkgram-Version: " + org.telegram.messenger.BuildConfig.SPARK_VERSION
                    + "\nKanal: " + org.telegram.messenger.BuildConfig.SPARK_CHANNEL
                    + "\nGebaut: " + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.GERMANY).format(new java.util.Date(org.telegram.messenger.BuildConfig.SPARK_BUILD_TIME))
                    + "\nCommit: " + (c == null || c.isEmpty() ? "lokal" : c)
                    + "\nTelegram-Basis: " + org.telegram.messenger.BuildConfig.BUILD_VERSION_STRING;
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(getString(R.string.SparkVersion));
                builder.setMessage(text);
                builder.setPositiveButton(getString(R.string.OK), null);
                builder.setNeutralButton(getString(R.string.Copy), (d, w) -> {
                    AndroidUtilities.addToClipboard(text);
                    BulletinFactory.of(this).createCopyBulletin(getString(R.string.TextCopied)).show();
                });
                showDialog(builder.create());
                break;
            }
            case ID_CRASH: {
                final String log = SparkCrashLog.read(getContext());
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(getString(R.string.SparkCrashLog));
                builder.setMessage(log == null ? "" : (log.length() > 3000 ? log.substring(0, 3000) + "…" : log));
                builder.setPositiveButton(getString(R.string.Copy), (d, w) -> {
                    AndroidUtilities.addToClipboard(log);
                    BulletinFactory.of(this).createCopyBulletin(getString(R.string.TextCopied)).show();
                });
                builder.setNeutralButton(getString(R.string.Delete), (d, w) -> {
                    SparkCrashLog.clear(getContext());
                    listView.adapter.update(true);
                });
                builder.setNegativeButton(getString(R.string.Close), null);
                showDialog(builder.create());
                break;
            }
        }
    }

    private void checkForUpdate() {
        final AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progress.show();
        SparkUpdater.check((release, error) -> {
            progress.dismiss();
            if (getParentActivity() == null) {
                return;
            }
            if (release == null) {
                BulletinFactory.of(this).createErrorBulletin(LocaleController.formatString(R.string.SparkError, String.valueOf(error))).show();
                return;
            }
            if (release.unknown) {
                AlertDialog.Builder ub = new AlertDialog.Builder(getParentActivity());
                ub.setTitle(SparkUi.mark("Update"));
                ub.setMessage("GitHub antwortet gerade nicht (" + error + "). Die neueste Version findest du direkt auf der GitHub-Seite.");
                ub.setPositiveButton("GitHub öffnen", (d, w) -> Browser.openUrl(getParentActivity(), SparkUpdater.REPO_URL + "/releases/tag/build-" + release.channel));
                ub.setNegativeButton(getString(R.string.Cancel), null);
                showDialog(ub.create());
                return;
            }
            if (!release.isNewer()) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, getString(R.string.SparkUpdateNone)).show();
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(getString(R.string.SparkUpdateAvailable));
            builder.setMessage(release.apkName + "\n" + AndroidUtilities.formatFileSize(release.apkSize));
            builder.setPositiveButton(getString(R.string.SparkUpdateInstall), (d, w) -> downloadUpdate(release));
            builder.setNegativeButton(getString(R.string.Cancel), null);
            showDialog(builder.create());
        });
    }

    private void downloadUpdate(SparkUpdater.Release release) {
        final AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_LOADING);
        progress.setMessage("Lade Update … 0 %");
        progress.setCanCancel(false);
        progress.show();
        SparkUpdater.download(release, p -> progress.setMessage("Lade Update … " + (int) (p * 100) + " %"), (file, error) -> {
            try { progress.dismiss(); } catch (Throwable ignore) {}
            if (getParentActivity() == null) return;
            String problem = file == null ? "Download fehlgeschlagen: " + error : SparkUpdater.install(getParentActivity(), file);
            if (problem != null) {
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
                b.setTitle(SparkUi.mark("Update"));
                b.setMessage(problem + "\n\nAlternativ über GitHub herunterladen.");
                b.setPositiveButton("GitHub öffnen", (d, w) -> Browser.openUrl(getParentActivity(), SparkUpdater.REPO_URL + "/releases/tag/build-" + release.channel));
                b.setNegativeButton(getString(R.string.Cancel), null);
                showDialog(b.create());
            }
        });
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}

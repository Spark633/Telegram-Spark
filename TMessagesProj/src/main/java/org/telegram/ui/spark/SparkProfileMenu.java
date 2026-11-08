package org.telegram.ui.spark;

import static org.telegram.messenger.LocaleController.getString;

import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.spark.SparkConfig;
import org.telegram.messenger.spark.SparkNotes;
import org.telegram.messenger.spark.SparkProfiles;
import org.telegram.messenger.spark.SparkTranslate;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;

/** Sparkgram-Eintraege im Drei-Punkte-Menue von Profilen, Gruppen und Kanaelen. */
public class SparkProfileMenu {

    public static final int ID_COPY_ID = 900;
    public static final int ID_INFO = 901;
    public static final int ID_CLONE = 902;
    public static final int ID_NOTE = 903;
    public static final int ID_PHOTOS = 904;
    public static final int ID_GROUP = 905;
    public static final int ID_JUMP_FIRST = 906;
    public static final int ID_DELETE_OWN = 907;
    public static final int ID_NAME_HISTORY = 908;
    public static final int ID_LOCAL_NAME = 909;
    public static final int ID_TIMEZONE = 910;
    public static final int ID_ADMINS = 911;
    public static final int ID_ACTIVE = 912;
    public static final int ID_AUTHORS = 913;
    public static final int ID_TRANSLATE_INPUT = 914;
    public static final int ID_ONLINE_LOG = 915;
    public static final int ID_BOT_SEND = 916;
    public static final int ID_CHAT_STICKERS = 917;
    public static final int ID_COLLECTION = 918;
    public static final int ID_MUTE_PERSON = 919;
    public static final int ID_TEMPLATES = 920;
    public static final int ID_TRANSLATE_CHAT = 921;
    public static final int ID_MARK_READ = 922;
    public static final int ID_EXPORT = 923;
    public static final int ID_LOCAL_PIN = 924;
    public static final int ID_PERSONA = 925;
    // Admin (nur fuer Admins der Gruppe)
    public static final int ID_ADMIN_TEMPLATES = 941;
    public static final int ID_ADMIN_COUNTS = 942;
    public static final int ID_ADMIN_LOG = 943;
    public static final int ID_ADMIN_REQUESTS = 944;
    public static final int ID_ADMIN_SUSPICIOUS = 945;

    public static boolean isSparkItem(int id) {
        return id >= ID_COPY_ID && id <= 950;
    }

    public interface Adder {
        void add(int id, int icon, CharSequence text);
    }

    /** Drei-Punkte-Menue im Profil. */
    public static void addItems(BaseFragment fragment, ActionBarMenuItem menu, long userId, long chatId) {
        if (menu == null) {
            return;
        }
        addItems(fragment, (id, icon, text) -> {
            if (id == GAP) menu.addColoredGap(); else menu.addSubItem(id, icon, text);
        }, userId, chatId, SparkConfig.get(SparkConfig.PROFILE_MENU_GROUPED));
    }

    /** Drei-Punkte-Menue im Chat (falls in den Einstellungen aktiviert). */
    public static void addChatItems(BaseFragment fragment, ActionBarMenuItem menu, long userId, long chatId) {
        if (menu == null || !SparkConfig.get(SparkConfig.CHAT_MENU)) {
            return;
        }
        addItems(fragment, (id, icon, text) -> {
            if (id == GAP) menu.lazilyAddColoredGap(); else menu.lazilyAddSubItem(id, icon, text);
        }, userId, chatId, SparkConfig.get(SparkConfig.CHAT_MENU_GROUPED));
    }

    private static class Entry {
        final int id;
        final int icon;
        final CharSequence text;

        Entry(int id, int icon, CharSequence text) {
            this.id = id;
            this.icon = icon;
            this.text = text;
        }
    }

    /** Alle aktiven Sparkgram-Eintraege fuer dieses Profil/diesen Chat. */
    private static ArrayList<Entry> entries(BaseFragment fragment, long userId, long chatId, boolean marked) {
        ArrayList<Entry> list = new ArrayList<>();
        if (userId == 0 && chatId == 0) {
            return list;
        }
        final int account = fragment.getCurrentAccount();
        final boolean self = userId != 0 && userId == UserConfig.getInstance(account).getClientUserId();
        if (SparkConfig.get(SparkConfig.NOTES)) {
            long did = userId != 0 ? userId : -chatId;
            boolean has = SparkNotes.has(account, did);
            list.add(new Entry(ID_NOTE, R.drawable.msg_stories_caption, label(getString(has ? R.string.SparkNoteEdit : R.string.SparkNoteAdd), marked)));
        }
        if (SparkConfig.get(SparkConfig.PROFILE_COPY_ID)) {
            list.add(new Entry(ID_COPY_ID, R.drawable.msg_copy, label(getString(R.string.SparkCopyId), marked)));
        }
        if (SparkConfig.get(SparkConfig.PROFILE_INFO)) {
            list.add(new Entry(ID_INFO, R.drawable.msg_info, label(getString(R.string.SparkUserInfo), marked)));
        }
        if (userId != 0 && SparkConfig.get(SparkConfig.LOCAL_NAMES)) {
            list.add(new Entry(ID_LOCAL_NAME, R.drawable.msg_edit, label(getString(R.string.SparkLocalName), marked)));
        }
        if (userId != 0 && !self && SparkConfig.get(SparkConfig.TIMEZONES)) {
            list.add(new Entry(ID_TIMEZONE, R.drawable.msg_recent, label(getString(R.string.SparkTimezone), marked)));
        }
        if (userId != 0 && !self && SparkConfig.get(SparkConfig.MUTE_PEOPLE)) {
            boolean m = org.telegram.messenger.spark.SparkPeople.isMuted(account, userId);
            list.add(new Entry(ID_MUTE_PERSON, m ? R.drawable.msg_unmute : R.drawable.msg_mute, label(m ? "Benachrichtigungen von Person an" : "Person überall stummschalten", marked)));
        }
        if (userId != 0 && !self && SparkConfig.get(SparkConfig.ONLINE_LOG)) {
            list.add(new Entry(ID_ONLINE_LOG, R.drawable.msg_log, label(getString(R.string.SparkOnlineLog), marked)));
        }
        if (userId != 0 && !self && SparkConfig.get(SparkConfig.NAME_HISTORY)) {
            list.add(new Entry(ID_NAME_HISTORY, R.drawable.msg_log, label(getString(R.string.SparkNameHistory), marked)));
        }
        if (userId != 0 && !self && SparkConfig.get(SparkConfig.PROFILE_CLONE)) {
            list.add(new Entry(ID_CLONE, R.drawable.msg_customize, label(getString(R.string.SparkCloneProfile), marked)));
        }
        if (self && SparkConfig.get(SparkConfig.PHOTO_MANAGER)) {
            list.add(new Entry(ID_PHOTOS, R.drawable.msg_photos, label(getString(R.string.SparkPhotoManager), marked)));
        }
        // Gruppen- und Kanal-Listen
        if (chatId != 0) {
            TLRPC.Chat chat = MessagesController.getInstance(account).getChat(chatId);
            boolean broadcast = ChatObject.isChannel(chat) && !chat.megagroup;
            if (chat != null && !broadcast) {
                if (SparkConfig.get(SparkConfig.GROUP_ADMINS)) {
                    list.add(new Entry(ID_ADMINS, R.drawable.msg_admins, label(getString(R.string.SparkAdminsList), marked)));
                }
                if (SparkConfig.get(SparkConfig.GROUP_ACTIVE)) {
                    list.add(new Entry(ID_ACTIVE, R.drawable.msg_groups, label(getString(R.string.SparkActiveMembers), marked)));
                }
            }
            if (chat != null && broadcast && SparkConfig.get(SparkConfig.CHANNEL_AUTHORS)) {
                list.add(new Entry(ID_AUTHORS, R.drawable.msg_edit, label(getString(R.string.SparkKnownAuthors), marked)));
            }
        }
        // Admin-Werkzeuge (nur Gruppen, in denen man Admin ist)
        if (chatId != 0 && SparkConfig.get(SparkConfig.ADMIN_TOOLS)) {
            TLRPC.Chat chat = MessagesController.getInstance(account).getChat(chatId);
            boolean broadcast = ChatObject.isChannel(chat) && !chat.megagroup;
            if (chat != null && !broadcast && (chat.creator || ChatObject.hasAdminRights(chat))) {
                list.add(new Entry(ID_ADMIN_TEMPLATES, R.drawable.msg_admins, label("Admin-Vorlagen", marked)));
                list.add(new Entry(ID_ADMIN_COUNTS, R.drawable.msg_stats, label("Verwarnungen in dieser Gruppe", marked)));
                if (ChatObject.isChannel(chat)) {
                    list.add(new Entry(ID_ADMIN_SUSPICIOUS, R.drawable.msg_block, label("Verdächtige & gelöschte Konten", marked)));
                    list.add(new Entry(ID_ADMIN_LOG, R.drawable.msg_log, label("Admin-Protokoll", marked)));
                }
                if (ChatObject.canUserDoAdminAction(chat, ChatObject.ACTION_INVITE)) {
                    list.add(new Entry(ID_ADMIN_REQUESTS, R.drawable.msg_requests, label("Beitrittsanfragen", marked)));
                }
            }
        }
        // auch im Profil verfuegbar
        if (!self) {
            if (SparkConfig.get(SparkConfig.COLLECTION)) {
                list.add(new Entry(ID_COLLECTION, R.drawable.msg_saved, label("Sammlung", marked)));
            }
            if (SparkConfig.get(SparkConfig.STICKER_TOOLS)) {
                list.add(new Entry(ID_CHAT_STICKERS, R.drawable.msg_sticker, label("Sticker dieses Chats", marked)));
            }
            if (SparkConfig.get(SparkConfig.TRANSLATE_FREE)) {
                list.add(new Entry(ID_TRANSLATE_CHAT, R.drawable.msg_translate, label("Chat übersetzen an/aus", marked)));
            }
            if (SparkConfig.get(SparkConfig.DELETE_OWN_ALL)) {
                list.add(new Entry(ID_DELETE_OWN, R.drawable.msg_delete, label(getString(R.string.SparkDeleteOwnAll), marked)));
            }
            if (SparkConfig.get(SparkConfig.EXPORT)) {
                list.add(new Entry(ID_EXPORT, R.drawable.msg_download, label("Chat exportieren (PDF/HTML)", marked)));
            }
            if (SparkConfig.get(SparkConfig.LOCAL_PINS)) {
                boolean lp = org.telegram.messenger.spark.SparkPins.isPinned(account, userId != 0 ? userId : -chatId);
                list.add(new Entry(ID_LOCAL_PIN, lp ? R.drawable.msg_unpin : R.drawable.msg_pin, label(lp ? "Lokal lösen" : "Lokal anheften", marked)));
            }
            if (SparkConfig.ghost(SparkConfig.GHOST_READ)) {
                list.add(new Entry(ID_MARK_READ, R.drawable.msg_markread, label("Jetzt als gelesen melden", marked)));
            }
        }
        // nur im geoeffneten Chat (brauchen das Eingabefeld bzw. den Verlauf)
        if (fragment instanceof org.telegram.ui.ChatActivity) {
            if (SparkConfig.get(SparkConfig.TEMPLATES)) {
                list.add(new Entry(ID_TEMPLATES, R.drawable.msg_stories_caption, label("Textvorlagen", marked)));
            }
            if (SparkConfig.get(SparkConfig.PERSONA) && !self) {
                long pd = userId != 0 ? userId : -chatId;
                String pn = org.telegram.messenger.spark.SparkPersona.name(account, pd);
                boolean pa = org.telegram.messenger.spark.SparkPersona.isActive(account, pd);
                list.add(new Entry(ID_PERSONA, R.drawable.msg_openprofile, label(pn == null ? "Persona" : "Persona: " + pn + (pa ? "" : " (pausiert)"), marked)));
            }
            if (SparkConfig.get(SparkConfig.BOT_SEND) && !android.text.TextUtils.isEmpty(org.telegram.messenger.spark.SparkBot.token())) {
                list.add(new Entry(ID_BOT_SEND, R.drawable.msg_bot, label("Über Bot senden", marked)));
            }
            if (SparkConfig.get(SparkConfig.TRANSLATE_OWN)) {
                list.add(new Entry(ID_TRANSLATE_INPUT, R.drawable.msg_translate, label(getString(R.string.SparkTranslateInput), marked)));
            }
            if (SparkConfig.get(SparkConfig.JUMP_FIRST)) {
                list.add(new Entry(ID_JUMP_FIRST, R.drawable.msg_go_up, label(getString(R.string.SparkJumpFirst), marked)));
            }
        }
        // Reihenfolge nach Gruppen (Wunsch Spark633), nicht genannte am Ende
        java.util.Collections.sort(list, (a, b) -> Integer.compare(rank(a.id), rank(b.id)));
        return list;
    }

    // Gruppen: Infos | Persönliches & Medien | Übersetzen | Löschen & Export | Rest
    private static final int[][] ORDER = {
        {ID_COPY_ID, ID_INFO, ID_LOCAL_NAME, ID_ADMINS, ID_ACTIVE, ID_TIMEZONE, ID_ONLINE_LOG, ID_NAME_HISTORY},
        {ID_NOTE, ID_COLLECTION, ID_TEMPLATES, ID_PERSONA, ID_CHAT_STICKERS, ID_BOT_SEND},
        {ID_TRANSLATE_CHAT, ID_TRANSLATE_INPUT},
        {ID_DELETE_OWN, ID_EXPORT},
        {ID_ADMIN_TEMPLATES, ID_ADMIN_COUNTS, ID_ADMIN_SUSPICIOUS, ID_ADMIN_LOG, ID_ADMIN_REQUESTS},
    };

    private static int rank(int id) {
        int r = 0;
        for (int g = 0; g < ORDER.length; g++) {
            for (int i = 0; i < ORDER[g].length; i++) {
                if (ORDER[g][i] == id) return (g == 3 ? 5000 : g == 4 ? 50 : g * 100) + i; // Admin direkt nach Infos, "Löschen & Export" ganz unten
            }
        }
        return 1000 + id; // Rest
    }

    private static int groupOf(int id) {
        for (int g = 0; g < ORDER.length; g++) {
            for (int x : ORDER[g]) if (x == id) return g;
        }
        return 99; // keine Gruppe
    }

    /** Pseudo-ID fuer eine Trennlinie im Menue (derzeit ungenutzt). */
    public static final int GAP = -12345;

    // Unterpunkte: jede Gruppe als eigener Eintrag, der ihre Funktionen sammelt
    public static final int ID_SUBGROUP_BASE = 930;
    private static final String[] GROUP_NAMES = {"Infos", "Notiz & Medien", "Übersetzen", "Löschen & Export", "Admin"};
    private static final int[] GROUP_ICONS = {R.drawable.msg_info, R.drawable.msg_saved, R.drawable.msg_translate, R.drawable.msg_download, R.drawable.msg_admins};

    private static void showSubGroup(BaseFragment fragment, long userId, long chatId, int group) {
        final ArrayList<Entry> list = new ArrayList<>();
        for (Entry e : entries(fragment, userId, chatId, false)) {
            if (groupOf(e.id) == group) list.add(e);
        }
        if (list.isEmpty()) return;
        // Titelzeile mit Zurueck-Pfeil links neben dem Namen
        CharSequence[] texts = new CharSequence[list.size()];
        int[] icons = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            texts[i] = list.get(i).text;
            icons[i] = list.get(i).icon;
        }
        SparkUi.showListDialog(fragment, SparkUi.mark(GROUP_NAMES[group]), () -> showGroup(fragment, userId, chatId), texts, icons,
            which -> handle(fragment, list.get(which).id, userId, chatId));
    }

    private static CharSequence label(String text, boolean marked) {
        return marked ? SparkUi.mark(text) : text;
    }

    public static void addItems(BaseFragment fragment, Adder menu, long userId, long chatId, boolean grouped) {
        // Immer ein einziger Eintrag "✦ Sparkgram"; darin Gruppen als Unterpunkte (Wunsch Spark633)
        if (!entries(fragment, userId, chatId, false).isEmpty()) {
            menu.add(ID_GROUP, R.drawable.settings_features, SparkUi.mark("Sparkgram"));
        }
    }

    /** Auswahlfenster "Sparkgram": Gruppen (mehrere Funktionen) als Unterpunkte, einzelne Funktionen direkt. */
    private static void showGroup(BaseFragment fragment, long userId, long chatId) {
        final ArrayList<Entry> all = entries(fragment, userId, chatId, false);
        if (all.isEmpty()) {
            return;
        }
        int[] count = new int[GROUP_NAMES.length];
        for (Entry e : all) {
            int g = groupOf(e.id);
            if (g < GROUP_NAMES.length) count[g]++;
        }
        final ArrayList<Entry> rows = new ArrayList<>();
        boolean[] added = new boolean[GROUP_NAMES.length];
        for (Entry e : all) {
            int g = groupOf(e.id);
            if (g < GROUP_NAMES.length && count[g] > 1) {
                if (!added[g]) {
                    added[g] = true;
                    rows.add(new Entry(ID_SUBGROUP_BASE + g, GROUP_ICONS[g], GROUP_NAMES[g] + " …"));
                }
            } else {
                rows.add(e);
            }
        }
        CharSequence[] texts = new CharSequence[rows.size()];
        int[] icons = new int[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            texts[i] = rows.get(i).text;
            icons[i] = rows.get(i).icon;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(SparkUi.mark("Sparkgram"));
        builder.setItems(texts, icons, (dialog, which) -> handle(fragment, rows.get(which).id, userId, chatId));
        fragment.showDialog(builder.create());
    }

    public static String chatType(TLRPC.Chat chat) {
        if (!ChatObject.isChannel(chat)) {
            return "Gruppe";
        }
        if (chat.gigagroup) {
            return "Broadcast-Gruppe";
        }
        if (chat.megagroup) {
            return chat.forum ? "Forum (Themen)" : "Supergruppe";
        }
        return "Kanal";
    }

    public static String role(TLRPC.Chat chat) {
        if (chat.creator) {
            return "Eigentümer";
        }
        if (chat.left) {
            return "kein Mitglied";
        }
        if (chat.admin_rights != null) {
            ArrayList<String> r = new ArrayList<>();
            TLRPC.TL_chatAdminRights a = chat.admin_rights;
            if (a.change_info) r.add("Infos ändern");
            if (a.delete_messages) r.add("Nachrichten löschen");
            if (a.ban_users) r.add("Nutzer sperren");
            if (a.invite_users) r.add("einladen");
            if (a.pin_messages) r.add("anheften");
            if (a.add_admins) r.add("Admins ernennen");
            if (a.post_messages) r.add("posten");
            if (a.edit_messages) r.add("bearbeiten");
            if (a.manage_call) r.add("Videochats");
            return r.isEmpty() ? "Admin" : "Admin (" + TextUtils.join(", ", r) + ")";
        }
        return "Mitglied";
    }

    private static String slowMode(int seconds) {
        if (seconds < 60) return seconds + " s";
        if (seconds < 3600) return (seconds / 60) + " min";
        return (seconds / 3600) + " h";
    }

    private static void editLocalName(BaseFragment fragment, long userId) {
        final int account = fragment.getCurrentAccount();
        TLRPC.User user = MessagesController.getInstance(account).getUser(userId);
        final String real = user != null ? org.telegram.messenger.ContactsController.formatName(user.first_name, user.last_name) : "";
        final android.widget.EditText input = new android.widget.EditText(fragment.getParentActivity());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        String cur = org.telegram.messenger.spark.SparkLocalNames.get(userId);
        input.setText(cur == null ? "" : cur);
        input.setHint(real);
        input.setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhiteBlackText));
        input.setHintTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhiteHintText));
        android.widget.FrameLayout box = new android.widget.FrameLayout(fragment.getParentActivity());
        box.addView(input, org.telegram.ui.Components.LayoutHelper.createFrame(org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT, 0, 24, 0, 24, 0));
        AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity());
        b.setTitle(SparkUi.mark(getString(R.string.SparkLocalName)));
        b.setMessage("Nur auf diesem Gerät sichtbar. Echter Name: " + real);
        b.setView(box);
        b.setPositiveButton(getString(R.string.Save), (d, w) -> {
            org.telegram.messenger.spark.SparkLocalNames.set(userId, input.getText().toString());
            org.telegram.messenger.NotificationCenter.getInstance(account).postNotificationName(org.telegram.messenger.NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_NAME);
            org.telegram.messenger.NotificationCenter.getInstance(account).postNotificationName(org.telegram.messenger.NotificationCenter.dialogsNeedReload);
        });
        if (cur != null) {
            b.setNeutralButton("Zurücksetzen", (d, w) -> {
                org.telegram.messenger.spark.SparkLocalNames.set(userId, null);
                org.telegram.messenger.NotificationCenter.getInstance(account).postNotificationName(org.telegram.messenger.NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_NAME);
                org.telegram.messenger.NotificationCenter.getInstance(account).postNotificationName(org.telegram.messenger.NotificationCenter.dialogsNeedReload);
            });
        }
        b.setNegativeButton(getString(R.string.Cancel), null);
        fragment.showDialog(b.create());
    }

    private static void chooseTimezone(BaseFragment fragment, long userId) {
        String[][] zones = org.telegram.messenger.spark.SparkTimezones.ZONES;
        String cur = org.telegram.messenger.spark.SparkTimezones.get(userId);
        CharSequence[] names = new CharSequence[zones.length + 1];
        names[0] = "— keine (entfernen)";
        for (int i = 0; i < zones.length; i++) {
            names[i + 1] = (zones[i][1].equals(cur) ? "✓ " : "") + zones[i][0];
        }
        AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity());
        b.setTitle(SparkUi.mark(getString(R.string.SparkTimezone)));
        b.setItems(names, (d, which) -> {
            org.telegram.messenger.spark.SparkTimezones.set(userId, which == 0 ? null : zones[which - 1][1]);
            org.telegram.messenger.NotificationCenter.getInstance(fragment.getCurrentAccount()).postNotificationName(org.telegram.messenger.NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_STATUS);
            String t = org.telegram.messenger.spark.SparkTimezones.localTime(userId);
            if (t != null) {
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, "Ortszeit: " + t).show();
            }
        });
        fragment.showDialog(b.create());
    }

    /** Fruehere Benutzernamen und Namen einer Person – nur auf Anfrage. */
    private static void showNameHistory(BaseFragment fragment, long userId) {
        final int account = fragment.getCurrentAccount();
        TLRPC.User user = MessagesController.getInstance(account).getUser(userId);
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("dd.MM.yy", java.util.Locale.GERMANY);
        StringBuilder sb = new StringBuilder();
        String curUser = user != null ? UserObject.getPublicUsername(user) : null;
        String curName = user != null ? UserObject.getUserName(user) : null;
        sb.append("Benutzernamen:");
        int n = 0;
        for (org.telegram.messenger.spark.SparkNameHistory.Item it : org.telegram.messenger.spark.SparkNameHistory.get(account).history(userId, org.telegram.messenger.spark.SparkNameHistory.KIND_USERNAME)) {
            boolean now = curUser != null && curUser.equalsIgnoreCase(it.value);
            sb.append("\n• @").append(it.value).append(now ? " (aktuell)" : "").append(" – ").append(df.format(new java.util.Date(it.firstSeen))).append(" bis ").append(now ? "heute" : df.format(new java.util.Date(it.lastSeen)));
            n++;
        }
        if (n == 0) sb.append("\n• noch keine erfasst");
        sb.append("\n\nNamen:");
        n = 0;
        for (org.telegram.messenger.spark.SparkNameHistory.Item it : org.telegram.messenger.spark.SparkNameHistory.get(account).history(userId, org.telegram.messenger.spark.SparkNameHistory.KIND_NAME)) {
            boolean now = it.value.equals(curName);
            sb.append("\n• ").append(it.value).append(now ? " (aktuell)" : "").append(" – ").append(df.format(new java.util.Date(it.firstSeen))).append(" bis ").append(now ? "heute" : df.format(new java.util.Date(it.lastSeen)));
            n++;
        }
        if (n == 0) sb.append("\n• noch keine erfasst");
        sb.append("\n\nErfasst wird, was Sparkgram seit der Installation gesehen hat.");
        final String text = sb.toString();
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(SparkUi.mark(getString(R.string.SparkNameHistory)));
        builder.setMessage(text);
        builder.setPositiveButton(getString(R.string.OK), null);
        builder.setNeutralButton(getString(R.string.Copy), (d, w) -> {
            AndroidUtilities.addToClipboard(text);
            BulletinFactory.of(fragment).createCopyBulletin(getString(R.string.TextCopied)).show();
        });
        fragment.showDialog(builder.create());
    }

    private static void confirmDeleteOwn(BaseFragment fragment, long dialogId) {
        final int account = fragment.getCurrentAccount();
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(getString(R.string.SparkDeleteOwnAll));
        builder.setMessage(getString(R.string.SparkDeleteOwnConfirm));
        builder.setPositiveButton(getString(R.string.Delete), (d, w) -> {
            final AlertDialog progress = new AlertDialog(fragment.getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
            final org.telegram.messenger.spark.SparkBulkDelete[] job = new org.telegram.messenger.spark.SparkBulkDelete[1];
            progress.setCanCancel(true);
            progress.setOnCancelListener(di -> {
                if (job[0] != null) job[0].cancel();
            });
            progress.show();
            job[0] = new org.telegram.messenger.spark.SparkBulkDelete(account, dialogId, new org.telegram.messenger.spark.SparkBulkDelete.Listener() {
                @Override
                public void onProgress(int found, int deleted, boolean searching) {
                    try {
                        progress.setMessage(searching ? "Suche … " + found + " gefunden" : "Lösche … " + deleted + " / " + found);
                    } catch (Exception ignore) {
                    }
                }

                @Override
                public void onDone(int deleted, String error) {
                    try {
                        progress.dismiss();
                    } catch (Exception ignore) {
                    }
                    if (error != null) {
                        BulletinFactory.of(fragment).createErrorBulletin(LocaleController.formatString(R.string.SparkError, error)).show();
                    } else {
                        BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, deleted + " eigene Nachrichten gelöscht").show();
                    }
                }
            });
            job[0].start();
        });
        builder.setNeutralButton(getString(R.string.SparkDeleteOwnList), (d, w) -> fragment.presentFragment(new SparkOwnMessagesActivity(dialogId)));
        builder.setNegativeButton(getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        fragment.showDialog(dialog);
        android.widget.TextView button = (android.widget.TextView) dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_text_RedBold));
        }
    }

    /** Info-Zeile direkt im Profil anzeigen? */
    public static boolean showInfoRow() {
        return SparkConfig.get(SparkConfig.PROFILE_INFO_ROW);
    }

    /** Kurze Zusammenfassung fuer die Info-Zeile im Profil bzw. in den Gruppen-Infos. */
    public static String infoLine(int account, long userId, long chatId) {
        MessagesController mc = MessagesController.getInstance(account);
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(peerId(account, userId, chatId));
        if (userId != 0) {
            TLRPC.UserFull full = mc.getUserFull(userId);
            if (SparkConfig.get(SparkConfig.TIMEZONES)) {
                String lt = org.telegram.messenger.spark.SparkTimezones.shortTime(userId);
                if (lt != null) {
                    sb.append("\nOrtszeit: ").append(lt);
                }
            }
            sb.append("\nKonto erstellt: ").append(org.telegram.messenger.spark.SparkRegDate.shortText(userId));
            if (full != null && full.profile_photo != null && full.profile_photo.date > 0) {
                sb.append("\nProfilbild seit: ").append(LocaleController.formatDateChat(full.profile_photo.date));
            }
            if (full != null && full.common_chats_count > 0) {
                sb.append("\nGemeinsame Gruppen: ").append(full.common_chats_count);
            }
        } else {
            TLRPC.Chat chat = mc.getChat(chatId);
            TLRPC.ChatFull full = mc.getChatFull(chatId);
            if (chat != null) {
                sb.append("\nTyp: ").append(chatType(chat)).append(ChatObject.getPublicUsername(chat) != null ? ", öffentlich" : ", privat");
                int members = full != null && full.participants_count > 0 ? full.participants_count : chat.participants_count;
                if (members > 0) {
                    sb.append("\nMitglieder: ").append(members);
                    if (full != null && full.online_count > 0) {
                        sb.append(" (").append(full.online_count).append(" online)");
                    }
                }
                if (full != null && full.slowmode_seconds > 0) {
                    sb.append("\nSlow Mode: ").append(slowMode(full.slowmode_seconds));
                }
            }
        }
        return sb.toString();
    }

    public static String peerId(int account, long userId, long chatId) {
        if (userId != 0) {
            return String.valueOf(userId);
        }
        TLRPC.Chat chat = MessagesController.getInstance(account).getChat(chatId);
        return (ChatObject.isChannel(chat) ? "-100" : "-") + chatId;
    }

    /** Aelteste Nachricht des Chats bei Telegram erfragen und dorthin springen (IDs beginnen nicht immer bei 1). */
    private static void jumpToFirst(org.telegram.ui.ChatActivity chat, long dialogId) {
        TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
        req.peer = chat.getMessagesController().getInputPeer(dialogId);
        req.offset_id = 1;
        req.add_offset = -10;
        req.limit = 10;
        final AlertDialog progress = new AlertDialog(chat.getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progress.showDelayed(400);
        chat.getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            try { progress.dismiss(); } catch (Throwable ignore) {}
            int first = 0;
            if (res instanceof TLRPC.messages_Messages) {
                TLRPC.messages_Messages m = (TLRPC.messages_Messages) res;
                chat.getMessagesController().putUsers(m.users, false);
                chat.getMessagesController().putChats(m.chats, false);
                int firstReal = 0;
                for (TLRPC.Message msg : m.messages) {
                    if (msg.id > 0 && (first == 0 || msg.id < first)) first = msg.id;
                    // erste echte Nachricht (Service-Nachrichten blinken bei Telegram nicht)
                    if (msg.id > 0 && !(msg instanceof TLRPC.TL_messageService) && (firstReal == 0 || msg.id < firstReal)) firstReal = msg.id;
                }
                if (firstReal != 0) first = firstReal;
            }
            if (first == 0) {
                BulletinFactory.of(chat).createErrorBulletin("Erste Nachricht nicht gefunden" + (err != null ? " (" + err.text + ")" : "")).show();
                return;
            }
            chat.scrollToMessageId(first, 0, true, 0, false, 0); // wie beim Antippen einer Antwort: oben + aufblinken
        }));
    }

    /** RP-Persona fuer diesen Chat festlegen, pausieren oder entfernen. */
    private static void editPersona(BaseFragment fragment, long dialogId) {
        final int account = fragment.getCurrentAccount();
        final String cur = org.telegram.messenger.spark.SparkPersona.name(account, dialogId);
        final boolean active = org.telegram.messenger.spark.SparkPersona.isActive(account, dialogId);
        final android.widget.EditText input = new android.widget.EditText(fragment.getParentActivity());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setSingleLine(true);
        input.setText(cur == null ? "" : cur);
        input.setSelection(input.length());
        input.setHint("Name der Figur, z. B. Luna");
        input.setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhiteBlackText));
        input.setHintTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhiteHintText));
        android.widget.FrameLayout box = new android.widget.FrameLayout(fragment.getParentActivity());
        box.addView(input, org.telegram.ui.Components.LayoutHelper.createFrame(org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT, 0, 24, 0, 24, 0));
        AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity());
        b.setTitle(SparkUi.mark("Persona in diesem Chat"));
        b.setMessage("Deine Nachrichten in diesem Chat beginnen dann mit dem Namen in Fett, z. B. „Luna: Hallo“. Mit [], {}, (), <>, -- oder == wird die Nachricht zusätzlich eingeschlossen: „Luna []“ → „Luna: [Hallo]“, nur „[]“ → „[Hallo]“. Nur in diesem Chat, nur auf diesem Gerät gespeichert.");
        b.setView(box);
        b.setPositiveButton("Speichern", (d, w) -> {
            org.telegram.messenger.spark.SparkPersona.set(account, dialogId, input.getText().toString());
            String n = org.telegram.messenger.spark.SparkPersona.name(account, dialogId);
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, n == null ? "✦ Persona entfernt" : "✦ Du schreibst hier als " + n).show();
        });
        if (cur != null) {
            b.setNeutralButton(active ? "Pausieren" : "Fortsetzen", (d, w) -> {
                org.telegram.messenger.spark.SparkPersona.setActive(account, dialogId, !active);
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.info, !active ? "✦ Persona " + cur + " wieder aktiv" : "✦ Persona pausiert").show();
            });
            b.setNegativeButton("Entfernen", (d, w) -> {
                org.telegram.messenger.spark.SparkPersona.set(account, dialogId, null);
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.info, "✦ Persona entfernt").show();
            });
        } else {
            b.setNegativeButton("Abbrechen", null);
        }
        fragment.showDialog(b.create());
    }

    /** Vorlage auswaehlen und ins Eingabefeld einfuegen. */
    private static void insertTemplate(org.telegram.ui.ChatActivity chat) {
        java.util.ArrayList<String> list = org.telegram.messenger.spark.SparkTemplates.list();
        java.util.ArrayList<CharSequence> names = new java.util.ArrayList<>();
        for (String t : list) names.add(t.length() > 70 ? t.substring(0, 70).replace('\n', ' ') + " …" : t.replace('\n', ' '));
        names.add("＋ Neue Vorlage …");
        names.add("⚙ Vorlagen verwalten");
        AlertDialog.Builder b = new AlertDialog.Builder(chat.getParentActivity());
        b.setTitle(SparkUi.mark("Textvorlagen"));
        b.setItems(names.toArray(new CharSequence[0]), (d, which) -> {
            if (which < list.size()) {
                org.telegram.ui.Components.ChatActivityEnterView input = chat.getChatActivityEnterView();
                if (input == null) return;
                CharSequence cur = input.getFieldText();
                String text = list.get(which);
                input.setFieldText(TextUtils.isEmpty(cur) ? text : cur + (cur.toString().endsWith(" ") ? "" : " ") + text);
                input.openKeyboard();
            } else if (which == list.size()) {
                SparkTemplatesActivity.edit(chat, -1, () -> insertTemplate(chat));
            } else {
                chat.presentFragment(new SparkTemplatesActivity());
            }
        });
        chat.showDialog(b.create());
    }

    private static final String[][] LANGS = {
        {"en", "Englisch"}, {"de", "Deutsch"}, {"es", "Spanisch"}, {"fr", "Französisch"}, {"it", "Italienisch"},
        {"pt", "Portugiesisch"}, {"nl", "Niederländisch"}, {"pl", "Polnisch"}, {"tr", "Türkisch"}, {"ru", "Russisch"},
        {"uk", "Ukrainisch"}, {"ar", "Arabisch"}, {"ja", "Japanisch"}, {"zh", "Chinesisch"}, {"ko", "Koreanisch"}
    };

    private static String langName(String code) {
        for (String[] l : LANGS) if (l[0].equals(code)) return l[1];
        return code;
    }

    /** Text im Eingabefeld uebersetzen und ersetzen (mit Rueckgaengig). Sprache wird pro Chat gemerkt. */
    public static void translateInput(org.telegram.ui.ChatActivity chat, long dialogId, boolean chooseOnly) {
        org.telegram.ui.Components.ChatActivityEnterView input = chat.getChatActivityEnterView();
        if (input == null || chat.getParentActivity() == null) {
            return;
        }
        final CharSequence original = input.getFieldText();
        if (TextUtils.isEmpty(original) || original.toString().trim().isEmpty()) {
            BulletinFactory.of(chat).createSimpleBulletin(R.raw.info, "Schreib zuerst deine Nachricht ins Eingabefeld.").show();
            return;
        }
        final String key = "translateOut_" + dialogId;
        String last = SparkConfig.prefs().getString(key, null);
        String[] names = new String[LANGS.length];
        for (int i = 0; i < LANGS.length; i++) {
            names[i] = LANGS[i][1] + (LANGS[i][0].equals(last) ? "  ✓" : "");
        }
        AlertDialog.Builder b = new AlertDialog.Builder(chat.getParentActivity());
        b.setTitle(SparkUi.mark("Übersetzen in …"));
        b.setItems(names, (d, which) -> {
            String lang = LANGS[which][0];
            SparkConfig.prefs().edit().putString(key, lang).apply();
            doTranslateInput(chat, input, original, lang);
        });
        b.setNegativeButton(getString(R.string.Cancel), null);
        chat.showDialog(b.create());
    }

    private static void doTranslateInput(org.telegram.ui.ChatActivity chat, org.telegram.ui.Components.ChatActivityEnterView input, CharSequence original, String lang) {
        final AlertDialog progress = new AlertDialog(chat.getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progress.showDelayed(300);
        org.telegram.ui.Components.TranslateAlert2.alternativeTranslate(original.toString(), "auto", lang, (res, rateLimit) -> {
            try { progress.dismiss(); } catch (Throwable ignore) {}
            if (TextUtils.isEmpty(res)) {
                BulletinFactory.of(chat).createErrorBulletin(rateLimit ? "Zu viele Übersetzungen – bitte kurz warten." : "Übersetzen fehlgeschlagen.").show();
                return;
            }
            input.setFieldText(res);
            BulletinFactory.of(chat).createSimpleBulletin(R.raw.info, "Übersetzt in " + langName(lang) + (SparkTranslate.useDeepl() ? " (DeepL)" : ""),
                "Rückgängig", () -> input.setFieldText(original)).show();
        });
    }

    public static void handle(BaseFragment fragment, int id, long userId, long chatId) {
        final int account = fragment.getCurrentAccount();
        if (id >= ID_SUBGROUP_BASE && id < ID_SUBGROUP_BASE + GROUP_NAMES.length) {
            showSubGroup(fragment, userId, chatId, id - ID_SUBGROUP_BASE);
            return;
        }
        switch (id) {
            case ID_MUTE_PERSON: {
                boolean m = !org.telegram.messenger.spark.SparkPeople.isMuted(account, userId);
                org.telegram.messenger.spark.SparkPeople.setMuted(account, userId, m);
                BulletinFactory.of(fragment).createSimpleBulletin(m ? R.raw.ic_mute : R.raw.ic_unmute,
                    m ? "✦ Keine Benachrichtigungen mehr von dieser Person – in allen Chats" : "✦ Benachrichtigungen von dieser Person wieder an").show();
                break;
            }
            case ID_LOCAL_PIN: {
                long did = userId != 0 ? userId : -chatId;
                org.telegram.messenger.spark.SparkPins.toggle(account, did);
                boolean now = org.telegram.messenger.spark.SparkPins.isPinned(account, did);
                MessagesController.getInstance(account).sortDialogs(null);
                org.telegram.messenger.NotificationCenter.getInstance(account).postNotificationName(org.telegram.messenger.NotificationCenter.dialogsNeedReload);
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, now ? "✦ Lokal angeheftet" : "✦ Lokal gelöst").show();
                break;
            }
            case ID_MARK_READ:
                SparkExport.markReadNow(fragment, userId != 0 ? userId : -chatId);
                break;
            case ID_EXPORT:
                SparkExport.exportChat(fragment, userId != 0 ? userId : -chatId);
                break;
            case ID_TRANSLATE_CHAT: {
                long did = userId != 0 ? userId : -chatId;
                org.telegram.messenger.TranslateController tc = org.telegram.messenger.MessagesController.getInstance(account).getTranslateController();
                boolean on = !tc.isTranslatingDialog(did);
                tc.sparkForceTranslate(did, on);
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.info, on ? "✦ Chat wird übersetzt (in " + tc.getDialogTranslateTo(did) + ")" : "✦ Übersetzung aus").show();
                break;
            }
            case ID_PERSONA:
                editPersona(fragment, userId != 0 ? userId : -chatId);
                break;
            case ID_ADMIN_TEMPLATES:
                fragment.presentFragment(new SparkAdminActivity());
                break;
            case ID_ADMIN_COUNTS:
                SparkAdminActivity.showGroupCounters(fragment, chatId);
                break;
            case ID_ADMIN_SUSPICIOUS:
                SparkAdminActivity.scanSuspicious(fragment, chatId);
                break;
            case ID_ADMIN_LOG: {
                TLRPC.Chat c = MessagesController.getInstance(account).getChat(chatId);
                if (c != null) fragment.presentFragment(new org.telegram.ui.ChannelAdminLogActivity(c));
                break;
            }
            case ID_ADMIN_REQUESTS:
                fragment.presentFragment(new org.telegram.ui.MemberRequestsActivity(chatId));
                break;
            case ID_TEMPLATES:
                if (fragment instanceof org.telegram.ui.ChatActivity) {
                    insertTemplate((org.telegram.ui.ChatActivity) fragment);
                }
                break;
            case ID_COLLECTION:
                fragment.presentFragment(new SparkCollectionActivity(userId != 0 ? userId : -chatId));
                break;
            case ID_BOT_SEND:
                if (fragment instanceof org.telegram.ui.ChatActivity) {
                    SparkMedia.sendViaBot((org.telegram.ui.ChatActivity) fragment, userId != 0 ? userId : -chatId);
                }
                break;
            case ID_CHAT_STICKERS:
                fragment.presentFragment(new SparkChatStickersActivity(userId != 0 ? userId : -chatId));
                break;
            case ID_ONLINE_LOG:
                fragment.presentFragment(new SparkOnlineLogActivity(userId));
                break;
            case ID_TRANSLATE_INPUT:
                if (fragment instanceof org.telegram.ui.ChatActivity) {
                    translateInput((org.telegram.ui.ChatActivity) fragment, userId != 0 ? userId : -chatId, false);
                }
                break;
            case ID_GROUP:
                showGroup(fragment, userId, chatId);
                break;
            case ID_ADMINS:
                fragment.presentFragment(new SparkGroupListActivity(chatId, SparkGroupListActivity.MODE_ADMINS));
                break;
            case ID_ACTIVE:
                fragment.presentFragment(new SparkGroupListActivity(chatId, SparkGroupListActivity.MODE_ACTIVE));
                break;
            case ID_AUTHORS:
                fragment.presentFragment(new SparkGroupListActivity(chatId, SparkGroupListActivity.MODE_AUTHORS));
                break;
            case ID_LOCAL_NAME:
                editLocalName(fragment, userId);
                break;
            case ID_TIMEZONE:
                chooseTimezone(fragment, userId);
                break;
            case ID_NAME_HISTORY:
                showNameHistory(fragment, userId);
                break;
            case ID_JUMP_FIRST:
                if (fragment instanceof org.telegram.ui.ChatActivity) {
                    jumpToFirst((org.telegram.ui.ChatActivity) fragment, userId != 0 ? userId : -chatId);
                }
                break;
            case ID_DELETE_OWN:
                confirmDeleteOwn(fragment, userId != 0 ? userId : -chatId);
                break;
            case ID_COPY_ID: {
                String value = peerId(account, userId, chatId);
                AndroidUtilities.addToClipboard(value);
                BulletinFactory.of(fragment).createCopyBulletin(getString(R.string.SparkIdCopied) + ": " + value).show();
                break;
            }
            case ID_INFO:
                showInfo(fragment, userId, chatId);
                break;
            case ID_CLONE:
                confirmClone(fragment, userId);
                break;
            case ID_PHOTOS:
                fragment.presentFragment(new SparkPhotoManagerActivity());
                break;
            case ID_NOTE:
                SparkNotesActivity.edit(fragment, userId != 0 ? userId : -chatId, () ->
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, getString(R.string.SparkNoteSaved)).show());
                break;
        }
    }

    private static void line(StringBuilder sb, String key, Object value) {
        if (value == null || TextUtils.isEmpty(String.valueOf(value))) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(key).append(": ").append(value);
    }

    private static String yes(boolean b) {
        return b ? "ja" : "nein";
    }

    private static String date(int unixTime) {
        if (unixTime <= 0) {
            return null;
        }
        return LocaleController.formatDateTime(unixTime, false);
    }

    private static void showInfo(BaseFragment fragment, long userId, long chatId) {
        final int account = fragment.getCurrentAccount();
        MessagesController mc = MessagesController.getInstance(account);
        StringBuilder sb = new StringBuilder();
        final String[] link = new String[1];
        String title;
        if (userId != 0) {
            TLRPC.User user = mc.getUser(userId);
            if (user == null) {
                return;
            }
            TLRPC.UserFull full = mc.getUserFull(userId);
            title = UserObject.getUserName(user);
            line(sb, "ID", user.id);
            line(sb, "Konto erstellt", org.telegram.messenger.spark.SparkRegDate.longText(user.id));
            String username = UserObject.getPublicUsername(user);
            line(sb, "Benutzername", username != null ? "@" + username : null);
            if (user.usernames != null && user.usernames.size() > 1) {
                StringBuilder more = new StringBuilder();
                for (TLRPC.TL_username u : user.usernames) {
                    if (u.active && u.username != null && !u.username.equals(username)) {
                        if (more.length() > 0) more.append(", ");
                        more.append("@").append(u.username);
                    }
                }
                line(sb, "Weitere Benutzernamen", more.length() > 0 ? more.toString() : null);
            }
            if (!TextUtils.isEmpty(user.phone)) {
                line(sb, "Telefon", "+" + user.phone);
            }
            if (user.photo != null && user.photo.dc_id != 0) {
                line(sb, "Rechenzentrum", "DC" + user.photo.dc_id);
            }
            line(sb, "Premium", yes(user.premium));
            if (user.bot) {
                line(sb, "Bot", yes(true));
            }
            if (user.verified) {
                line(sb, "Verifiziert", yes(true));
            }
            if (user.scam || user.fake) {
                line(sb, "Warnung", user.scam ? "als Betrug markiert" : "als Fake markiert");
            }
            if (full != null) {
                line(sb, "Gemeinsame Gruppen", full.common_chats_count);
                if (full.profile_photo != null) {
                    line(sb, "Profilbild gesetzt", date(full.profile_photo.date));
                }
            }
        } else {
            TLRPC.Chat chat = mc.getChat(chatId);
            if (chat == null) {
                return;
            }
            TLRPC.ChatFull full = mc.getChatFull(chatId);
            title = chat.title;
            line(sb, "ID", peerId(account, 0, chatId));
            String username = ChatObject.getPublicUsername(chat);
            line(sb, "Benutzername", username != null ? "@" + username : null);
            line(sb, "Typ", chatType(chat));
            line(sb, "Deine Rolle", role(chat));
            if (chat.usernames != null && chat.usernames.size() > 1) {
                StringBuilder more = new StringBuilder();
                for (TLRPC.TL_username u : chat.usernames) {
                    if (u.active && u.username != null && !u.username.equals(username)) {
                        if (more.length() > 0) more.append(", ");
                        more.append("@").append(u.username);
                    }
                }
                line(sb, "Weitere Benutzernamen", more.length() > 0 ? more.toString() : null);
            }
            if (chat.photo != null && chat.photo.dc_id != 0) {
                line(sb, "Rechenzentrum", "DC" + chat.photo.dc_id);
            }
            int members = full != null && full.participants_count > 0 ? full.participants_count : chat.participants_count;
            if (members > 0) {
                line(sb, "Mitglieder", members);
            }
            if (full != null && full.online_count > 0) {
                line(sb, "Gerade online", full.online_count);
            }
            line(sb, "Beigetreten", date(chat.date));
            if (full != null && full.slowmode_seconds > 0) {
                line(sb, "Slow Mode", slowMode(full.slowmode_seconds));
            }
            line(sb, "Geschützte Inhalte", chat.noforwards ? "ja (Weiterleiten/Speichern verboten)" : "nein");
            if (chat.join_request) {
                line(sb, "Beitritt", "nur mit Anfrage");
            }
            if (full != null && full.participants_hidden) {
                line(sb, "Mitgliederliste", "verborgen");
            }
            if (ChatObject.isChannel(chat) && !chat.megagroup && chat.signatures) {
                line(sb, "Signaturen", "an");
            }
            if (full != null && full.linked_chat_id != 0) {
                TLRPC.Chat linked = mc.getChat(full.linked_chat_id);
                String linkedName = linked != null ? linked.title : String.valueOf(full.linked_chat_id);
                line(sb, ChatObject.isChannel(chat) && !chat.megagroup ? "Diskussionsgruppe" : "Verknüpfter Kanal", linkedName + " (ID -100" + full.linked_chat_id + ")");
            }
            if (full != null && full.migrated_from_chat_id != 0) {
                line(sb, "Hervorgegangen aus", "alter Gruppe (ID -" + full.migrated_from_chat_id + ")");
            }
            if (username != null) {
                line(sb, "Sichtbarkeit", "öffentlich");
                link[0] = "https://t.me/" + username;
            } else {
                line(sb, "Sichtbarkeit", "privat");
                if (full != null && full.exported_invite != null && !TextUtils.isEmpty(full.exported_invite.link)) {
                    link[0] = full.exported_invite.link;
                }
            }
            line(sb, "Link", link[0] != null ? link[0] : "kein Einladungslink sichtbar (nur für Admins)");
            if (full != null && full.chat_photo != null) {
                line(sb, "Bild gesetzt", date(full.chat_photo.date));
            }
        }
        String note = SparkNotes.get(account, userId != 0 ? userId : -chatId);
        if (!TextUtils.isEmpty(note)) {
            sb.append("\n\n").append(getString(R.string.SparkNote)).append(":\n").append(note);
        }
        final String text = sb.toString();
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(title);
        builder.setMessage(text);
        if (link[0] != null) {
            builder.setPositiveButton(getString(R.string.SparkCopyLink), (dialog, which) -> {
                AndroidUtilities.addToClipboard(link[0]);
                BulletinFactory.of(fragment).createCopyBulletin(getString(R.string.LinkCopied)).show();
            });
            builder.setNegativeButton(getString(R.string.OK), null);
        } else {
            builder.setPositiveButton(getString(R.string.OK), null);
        }
        builder.setNeutralButton(getString(R.string.SparkCopyAll), (dialog, which) -> {
            AndroidUtilities.addToClipboard(text);
            BulletinFactory.of(fragment).createCopyBulletin(getString(R.string.TextCopied)).show();
        });
        fragment.showDialog(builder.create());
    }

    private static void confirmClone(BaseFragment fragment, long userId) {
        final int account = fragment.getCurrentAccount();
        final TLRPC.User user = MessagesController.getInstance(account).getUser(userId);
        if (user == null) {
            return;
        }
        final String name = UserObject.getUserName(user);
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(getString(R.string.SparkCloneProfile));
        builder.setMessage(LocaleController.formatString(R.string.SparkCloneConfirm, name));
        builder.setPositiveButton(getString(R.string.SparkCloneProfile), (dialog, which) -> {
            final AlertDialog progress = new AlertDialog(fragment.getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
            progress.show();
            SparkProfiles.cloneFrom(account, user, (ok, error) -> {
                progress.dismiss();
                if (ok) {
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, LocaleController.formatString(R.string.SparkCloneDone, name)).show();
                } else {
                    BulletinFactory.of(fragment).createErrorBulletin(LocaleController.formatString(R.string.SparkError, String.valueOf(error))).show();
                }
            });
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        fragment.showDialog(builder.create());
    }
}

package org.telegram.ui.spark;

import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.spark.SparkMembership;
import org.telegram.messenger.spark.SparkRateLimit;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;
import org.telegram.ui.ProfileActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Gruppen-Listen von Sparkgram:
 * - ADMINS: Admins einer Gruppe mit Titel (auch fuer normale Mitglieder sichtbar)
 * - ACTIVE: wer im Verlauf geschrieben hat, mit Anzahl, zuletzt und ✅/❌ (noch in der Gruppe)
 * - AUTHORS: Kanal-Beitraege mit Signatur – bekannte Autoren
 */
public class SparkGroupListActivity extends UniversalFragment implements NotificationCenter.NotificationCenterDelegate {

    public static final int MODE_ADMINS = 0;
    public static final int MODE_ACTIVE = 1;
    public static final int MODE_AUTHORS = 2;

    private static final int ID_BASE = 1000;
    private static final int ID_MORE = 1;
    private static final int ID_MEMBERS = 2;
    private static final int MEMBER_LIMIT = 10000;
    private static final String FEATURE = "historyScan";
    private static final int PER_HOUR = 400;
    private static final int PAGES_PER_STEP = 10;

    private final long chatId;
    private final int mode;

    private static class Row {
        long userId;
        String name;
        String detail;
        int count;
        int lastDate;
    }

    private final ArrayList<Row> rows = new ArrayList<>();
    private final HashMap<String, Row> byKey = new HashMap<>();
    private boolean loading = true;
    private boolean endReached;
    private int scanned;
    private int offsetId;
    private String error;
    private boolean membersLoading;
    private boolean scanStarted;
    private boolean membersLoaded;
    private boolean membersHidden;
    private int membersOffset;
    private final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.GERMANY);

    public SparkGroupListActivity(long chatId, int mode) {
        this.chatId = chatId;
        this.mode = mode;
    }

    @Override
    public boolean onFragmentCreate() {
        getNotificationCenter().addObserver(this, NotificationCenter.updateInterfaces);
        if (mode == MODE_ADMINS) {
            loadAdmins();
        } else if (mode == MODE_ACTIVE) {
            loadMembers(); // normaler Weg: Mitgliederliste; danach Verlauf fuer Anzahl/zuletzt aktiv
        } else {
            scanStep(PAGES_PER_STEP);
        }
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        getNotificationCenter().removeObserver(this, NotificationCenter.updateInterfaces);
        endReached = true;
        super.onFragmentDestroy();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.updateInterfaces && mode == MODE_ACTIVE) {
            update(); // Mitgliedsstatus hat sich geaendert
        }
    }

    @Override
    protected CharSequence getTitle() {
        switch (mode) {
            case MODE_ADMINS: return "Admins";
            case MODE_ACTIVE: return "Mitglieder & Aktivität";
            default: return "Bekannte Autoren";
        }
    }

    private android.widget.TextView pinnedHeader;

    @Override
    public View createView(android.content.Context context) {
        View v = super.createView(context);
        if (mode == MODE_ACTIVE) {
            // Fortschritt/Anzahl oben angeheftet, Liste scrollt darunter
            pinnedHeader = new android.widget.TextView(context);
            pinnedHeader.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 14);
            pinnedHeader.setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhiteBlueHeader));
            pinnedHeader.setBackgroundColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhite));
            pinnedHeader.setPadding(AndroidUtilities.dp(21), AndroidUtilities.dp(12), AndroidUtilities.dp(21), AndroidUtilities.dp(12));
            pinnedHeader.setElevation(AndroidUtilities.dp(2));
            ((android.widget.FrameLayout) v).addView(pinnedHeader, org.telegram.ui.Components.LayoutHelper.createFrame(org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT, android.view.Gravity.TOP));
            listView.setPadding(0, AndroidUtilities.dp(44), 0, 0);
            listView.setClipToPadding(false);
            updateHeader();
        }
        return v;
    }

    private String headerText() {
        if (!membersLoaded) {
            return "Mitgliederliste wird geladen … " + rows.size();
        }
        return (!membersHidden ? rows.size() + " Mitglieder · " : rows.size() + " Personen · ") + scanned + " Nachrichten durchsucht" + (endReached ? " ✓" : loading ? " …" : "");
    }

    private void updateHeader() {
        if (pinnedHeader != null) {
            pinnedHeader.setText(headerText());
        }
    }

    private void update() {
        updateHeader();
        if (listView != null && listView.adapter != null) {
            // war die Liste oben, bleibt sie oben (sonst springt sie beim Neusortieren in die Mitte)
            boolean atTop = !listView.canScrollVertically(-1);
            listView.adapter.update(!atTop);
            if (atTop) {
                listView.scrollToPosition(0);
            }
        }
    }

    // ------------------------------------------------------------------ Admins

    private void loadAdmins() {
        MessagesController mc = getMessagesController();
        TLRPC.Chat chat = mc.getChat(chatId);
        if (chat == null) {
            loading = false;
            error = "Gruppe nicht gefunden";
            return;
        }
        if (!ChatObject.isChannel(chat)) {
            TLRPC.ChatFull full = mc.getChatFull(chatId);
            if (full != null && full.participants != null) {
                for (TLRPC.ChatParticipant p : full.participants.participants) {
                    boolean owner = p instanceof TLRPC.TL_chatParticipantCreator;
                    if (owner || p instanceof TLRPC.TL_chatParticipantAdmin) {
                        addAdmin(p.user_id, owner ? "Eigentümer" : "Admin");
                    }
                }
            }
            loading = false;
            sortAdmins();
            return;
        }
        TLRPC.TL_channels_getParticipants req = new TLRPC.TL_channels_getParticipants();
        req.channel = MessagesController.getInputChannel(chat);
        req.filter = new TLRPC.TL_channelParticipantsAdmins();
        req.offset = 0;
        req.limit = 200;
        getConnectionsManager().sendRequest(req, (response, err) -> AndroidUtilities.runOnUIThread(() -> {
            loading = false;
            if (response instanceof TLRPC.channels_ChannelParticipants) {
                TLRPC.channels_ChannelParticipants res = (TLRPC.channels_ChannelParticipants) response;
                getMessagesController().putUsers(res.users, false);
                getMessagesController().putChats(res.chats, false);
                for (TLRPC.ChannelParticipant p : res.participants) {
                    long uid = p.peer != null ? org.telegram.messenger.DialogObject.getPeerDialogId(p.peer) : 0;
                    boolean owner = p instanceof TLRPC.TL_channelParticipantCreator;
                    String title = !TextUtils.isEmpty(p.rank) ? p.rank : (owner ? "Eigentümer" : "Admin");
                    if (owner && !TextUtils.isEmpty(p.rank)) title = p.rank + " (Eigentümer)";
                    if (p.admin_rights != null && p.admin_rights.anonymous) title += " · anonym";
                    addAdmin(uid, title);
                }
                sortAdmins();
            } else if (err != null) {
                error = "CHAT_ADMIN_REQUIRED".equals(err.text) ? "Telegram zeigt die Admin-Liste hier nur Admins." : err.text;
            }
            update();
        }));
    }

    private void addAdmin(long uid, String title) {
        Row r = new Row();
        r.userId = uid;
        r.name = SparkNotesActivity.dialogName(currentAccount, uid);
        r.detail = title;
        r.count = title.contains("Eigentümer") ? 1 : 0;
        rows.add(r);
    }

    private void sortAdmins() {
        Collections.sort(rows, (a, b) -> b.count - a.count);
    }

    // ------------------------------------------------------------------ Verlauf durchsuchen

    private void scanStep(int pages) {
        if (endReached || pages <= 0) {
            loading = false;
            update();
            return;
        }
        long wait = SparkRateLimit.waitTime(FEATURE, PER_HOUR);
        if (wait > 0) {
            loading = false;
            error = "Telegram bremst gerade – bitte in " + Math.max(1, wait / 60000) + " Min. weiter durchsuchen.";
            update();
            return;
        }
        loading = true;
        TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
        req.peer = getMessagesController().getInputPeer(-chatId);
        req.offset_id = offsetId;
        req.limit = 100;
        SparkRateLimit.use(FEATURE);
        getConnectionsManager().sendRequest(req, (response, err) -> AndroidUtilities.runOnUIThread(() -> {
            SparkRateLimit.onError(FEATURE, err);
            if (!(response instanceof TLRPC.messages_Messages)) {
                loading = false;
                error = err != null ? err.text : "Fehler beim Laden";
                update();
                return;
            }
            TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
            getMessagesController().putUsers(res.users, false);
            getMessagesController().putChats(res.chats, false);
            int last = 0;
            for (TLRPC.Message m : res.messages) {
                last = m.id;
                scanned++;
                if (m instanceof TLRPC.TL_messageService) {
                    continue;
                }
                if (mode == MODE_AUTHORS) {
                    if (!TextUtils.isEmpty(m.post_author)) {
                        count(m.post_author, 0, m.post_author, m.date);
                    }
                } else {
                    long uid = m.from_id != null ? org.telegram.messenger.DialogObject.getPeerDialogId(m.from_id) : 0;
                    if (uid > 0) {
                        count(String.valueOf(uid), uid, null, m.date);
                    }
                }
            }
            if (res.messages.size() < 100 || last <= 0) {
                endReached = true;
            }
            offsetId = last;
            Collections.sort(rows, (a, b) -> b.count - a.count);
            update();
            // nach PAGES_PER_STEP Seiten Pause -> "Weiter durchsuchen"
            AndroidUtilities.runOnUIThread(() -> scanStep(pages - 1), 700);
        }));
    }

    // ------------------------------------------------------------------ normale Mitgliederliste

    /** Nach der Mitgliederliste: Verlauf durchsuchen (Anzahl, zuletzt aktiv; bei verborgener Liste die einzige Quelle). */
    private void afterMembers() {
        if (!scanStarted) {
            scanStarted = true;
            scanStep(PAGES_PER_STEP);
        }
    }

    /** Laedt die Mitgliederliste (falls sichtbar). */
    private void loadMembers() {
        if (membersLoading || membersLoaded) {
            return;
        }
        TLRPC.Chat chat = getMessagesController().getChat(chatId);
        if (chat == null) {
            afterMembers();
            return;
        }
        if (!ChatObject.isChannel(chat)) {
            TLRPC.ChatFull full = getMessagesController().getChatFull(chatId);
            if (full != null && full.participants != null) {
                for (TLRPC.ChatParticipant p : full.participants.participants) {
                    addMember(p.user_id);
                }
            }
            membersLoaded = true;
            Collections.sort(rows, (a, b) -> b.count - a.count);
            update();
            afterMembers();
            return;
        }
        long wait = SparkRateLimit.waitTime(FEATURE, PER_HOUR);
        if (wait > 0) {
            error = "Telegram bremst gerade – bitte später erneut versuchen.";
            update();
            afterMembers();
            return;
        }
        membersLoading = true;
        TLRPC.TL_channels_getParticipants req = new TLRPC.TL_channels_getParticipants();
        req.channel = MessagesController.getInputChannel(chat);
        req.filter = new TLRPC.TL_channelParticipantsRecent();
        req.offset = membersOffset;
        req.limit = 200;
        SparkRateLimit.use(FEATURE);
        getConnectionsManager().sendRequest(req, (response, err) -> AndroidUtilities.runOnUIThread(() -> {
            SparkRateLimit.onError(FEATURE, err);
            membersLoading = false;
            if (response instanceof TLRPC.channels_ChannelParticipants) {
                TLRPC.channels_ChannelParticipants res = (TLRPC.channels_ChannelParticipants) response;
                getMessagesController().putUsers(res.users, false);
                for (TLRPC.ChannelParticipant p : res.participants) {
                    long uid = p.peer != null ? org.telegram.messenger.DialogObject.getPeerDialogId(p.peer) : 0;
                    addMember(uid);
                }
                membersOffset += res.participants.size();
                if (res.participants.isEmpty() || membersOffset >= Math.min(MEMBER_LIMIT, res.count)) {
                    membersLoaded = true;
                    if (membersOffset == 0) {
                        membersHidden = true;
                    }
                    afterMembers();
                } else {
                    AndroidUtilities.runOnUIThread(this::loadMembers, 700); // naechste Seite
                }
            } else {
                membersLoaded = true;
                membersHidden = true;
                afterMembers();
            }
            Collections.sort(rows, (a, b) -> b.count - a.count);
            update();
        }));
    }

    private void addMember(long uid) {
        if (uid <= 0) {
            return;
        }
        SparkMembership.markIn(currentAccount, chatId, uid);
        String key = String.valueOf(uid);
        if (!byKey.containsKey(key)) {
            Row r = new Row();
            r.userId = uid;
            r.name = SparkNotesActivity.dialogName(currentAccount, uid);
            byKey.put(key, r);
            rows.add(r);
        }
    }

    private void count(String key, long uid, String name, int date) {
        Row r = byKey.get(key);
        if (r == null) {
            r = new Row();
            r.userId = uid;
            r.name = name != null ? name : SparkNotesActivity.dialogName(currentAccount, uid);
            byKey.put(key, r);
            rows.add(r);
        }
        r.count++;
        if (date > r.lastDate) {
            r.lastDate = date;
        }
        if (mode == MODE_ACTIVE && uid > 0) {
            SparkMembership.requestCheck(currentAccount, chatId, uid, date * 1000L);
        }
    }

    // ------------------------------------------------------------------ Anzeige

    private String status(long uid) {
        switch (SparkMembership.state(currentAccount, chatId, uid)) {
            case SparkMembership.IN: return "✅ ";
            case SparkMembership.OUT: return "❌ ";
            default: return "❔ ";
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (mode == MODE_ADMINS) {
            if (loading) {
                items.add(UItem.asShadow("Lade Admins …"));
                return;
            }
            if (error != null) {
                items.add(UItem.asShadow(error));
            }
            items.add(UItem.asHeader(rows.size() + " Admins"));
            for (int i = 0; i < rows.size(); i++) {
                Row r = rows.get(i);
                items.add(UItem.asButton(ID_BASE + i, r.name, r.detail));
            }
            items.add(UItem.asShadow("Antippen öffnet das Profil."));
            return;
        }
        if (mode != MODE_ACTIVE) {
            items.add(UItem.asHeader(scanned + " Nachrichten durchsucht" + (loading ? " …" : "") + (endReached ? " (Anfang erreicht)" : "")));
        }
        if (error != null) {
            items.add(UItem.asShadow(error));
        }
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            String title = (mode == MODE_ACTIVE ? status(r.userId) : "✍️ ") + r.name;
            String value = r.count == 0 ? "0× · still" : r.count + "× · zuletzt " + df.format(new Date(r.lastDate * 1000L));
            items.add(UItem.asButton(ID_BASE + i, title, value));
        }
        if (!endReached && !loading) {
            items.add(UItem.asButton(ID_MORE, R.drawable.msg_retry, mode == MODE_ACTIVE ? "Weiter durchsuchen (+1000 Nachrichten)" : "Weiter zurück durchsuchen (+1000)").accent());
        }
        if (mode == MODE_ACTIVE && membersHidden) {
            items.add(UItem.asShadow("Die Mitgliederliste dieser Gruppe ist verborgen – die Liste stammt deshalb nur aus dem Verlauf, stille Mitleser fehlen."));
        }
        items.add(UItem.asShadow(mode == MODE_ACTIVE
            ? "Grundlage ist die Mitgliederliste (bis " + MEMBER_LIMIT + "); der Verlauf ergänzt, wie oft und wann zuletzt jemand geschrieben hat. Ist die Liste verborgen, stammt alles aus dem Verlauf. ✅ noch in der Gruppe · ❌ nicht mehr · ❔ unbekannt."
            : "Bei Kanälen mit Signatur steht bei jedem Beitrag, wer ihn gepostet hat. Das ist meist nur ein Teil der Admins."));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_MORE) {
            error = null;
            scanStep(PAGES_PER_STEP);
            update();
            return;
        }
        int i = item.id - ID_BASE;
        if (i >= 0 && i < rows.size() && rows.get(i).userId > 0) {
            presentFragment(ProfileActivity.of(rows.get(i).userId));
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}

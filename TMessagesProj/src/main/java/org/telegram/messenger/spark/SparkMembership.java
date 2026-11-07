package org.telegram.messenger.spark;

import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Prueft, ob Absender in Gruppen noch Mitglied sind.
 * Hauptweg: gemeinsame Gruppen (eine Anfrage pro Person deckt alle Gruppen ab).
 * Meldet der Hauptweg "raus", wird bei Supergruppen per Einzelabfrage gegengeprueft,
 * damit keine falschen Markierungen entstehen.
 */
public class SparkMembership {

    public static final int UNKNOWN = 0;
    public static final int IN = 1;
    public static final int OUT = 2;

    private static final long NORMAL_TTL = 6 * 60 * 60 * 1000L;   // normale Pruefung: alle 6 Stunden
    private static final long ACTIVE_WINDOW = 10 * 60 * 1000L;    // "aktiv" = in den letzten 10 Minuten geschrieben
    private static final long REQUEST_GAP = 2500;                 // Pause zwischen Anfragen
    private static final String FEATURE = "memberCheck";
    private static final int PER_HOUR = 120;                      // Stundenbudget fuer Pruefungen

    public interface Listener {
        void onLeft(long chatId, long userId);
    }

    private static class Info {
        int state = UNKNOWN;
        long checkedAt;
        long lastActive;
    }

    // Schluessel: account_chatId_userId
    private static final HashMap<String, Info> infos = new HashMap<>();
    private static final ArrayDeque<long[]> queue = new ArrayDeque<>(); // {account, userId}
    private static final HashSet<String> queued = new HashSet<>();
    // Gruppen, bei denen "gemeinsame Gruppen" nachweislich funktioniert (mind. ein Mitglied wurde dort gefunden)
    private static final HashSet<String> reliableChats = new HashSet<>();
    private static boolean running;

    private static int openAccount = -1;
    private static long openChatId;
    private static Listener openListener;
    private static Runnable activeLoop;

    private static String key(int account, long chatId, long userId) {
        return account + "_" + chatId + "_" + userId;
    }

    private static Info info(int account, long chatId, long userId) {
        String k = key(account, chatId, userId);
        Info i = infos.get(k);
        if (i == null) {
            infos.put(k, i = new Info());
        }
        return i;
    }

    public static boolean enabled() {
        return SparkConfig.get(SparkConfig.MEMBER_CHECK);
    }

    public static int state(int account, long chatId, long userId) {
        if (!enabled()) {
            return UNKNOWN;
        }
        Info i = infos.get(key(account, chatId, userId));
        return i == null ? UNKNOWN : i.state;
    }

    /** Text fuer die Markierung neben dem Namen, oder null. */
    public static CharSequence badge(int account, long chatId, long userId) {
        return state(account, chatId, userId) == OUT ? "🚪 raus" : null;
    }

    /** Wird aufgerufen, wenn eine Nachricht in einer Gruppe angezeigt wird. */
    public static void onMessageShown(int account, MessageObject message) {
        if (!enabled() || message == null || message.messageOwner == null) {
            return;
        }
        long dialogId = message.getDialogId();
        if (dialogId >= 0) {
            return;
        }
        long chatId = -dialogId;
        TLRPC.MessageAction action = message.messageOwner.action;
        if (action != null) {
            if (action instanceof TLRPC.TL_messageActionChatDeleteUser) {
                setState(account, chatId, action.user_id, OUT, message.messageOwner.date * 1000L);
            } else if (action instanceof TLRPC.TL_messageActionChatAddUser) {
                for (Long uid : action.users) {
                    setState(account, chatId, uid, IN, message.messageOwner.date * 1000L);
                }
            } else if (action instanceof TLRPC.TL_messageActionChatJoinedByLink || action instanceof TLRPC.TL_messageActionChatJoinedByRequest) {
                setState(account, chatId, message.getSenderId(), IN, message.messageOwner.date * 1000L);
            }
            return;
        }
        long userId = message.getSenderId();
        if (userId <= 0 || userId == UserConfig.getInstance(account).getClientUserId()) {
            return;
        }
        Info i = info(account, chatId, userId);
        long date = message.messageOwner.date * 1000L;
        if (date > i.lastActive) {
            i.lastActive = date;
        }
        if (System.currentTimeMillis() - i.checkedAt > NORMAL_TTL) {
            enqueue(account, userId);
        }
    }

    /** Person steht sicher in der Mitgliederliste -> ohne Extra-Anfrage als "drin" markieren. */
    public static void markIn(int account, long chatId, long userId) {
        if (userId > 0) {
            setState(account, chatId, userId, IN, System.currentTimeMillis());
        }
    }

    /** Pruefung einer bestimmten Person in einer Gruppe anstossen (z. B. aus der Liste "Aktive Mitglieder"). */
    public static void requestCheck(int account, long chatId, long userId, long lastActive) {
        if (!enabled() || userId <= 0 || userId == UserConfig.getInstance(account).getClientUserId()) {
            return;
        }
        Info i = info(account, chatId, userId);
        if (lastActive > i.lastActive) {
            i.lastActive = lastActive;
        }
        if (System.currentTimeMillis() - i.checkedAt > NORMAL_TTL) {
            enqueue(account, userId);
        }
    }

    private static void setState(int account, long chatId, long userId, int state, long eventTime) {
        if (userId <= 0) {
            return;
        }
        Info i = info(account, chatId, userId);
        if (eventTime > 0 && eventTime < i.checkedAt) {
            return; // neuere Pruefung vorhanden
        }
        int old = i.state;
        i.state = state;
        i.checkedAt = Math.max(i.checkedAt, eventTime > 0 ? eventTime : System.currentTimeMillis());
        if (old != state) {
            NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_NAME);
            if (state == OUT && old == IN && openListener != null && openAccount == account && openChatId == chatId
                    && System.currentTimeMillis() - i.lastActive < ACTIVE_WINDOW && SparkConfig.get(SparkConfig.MEMBER_CHECK_HINT)) {
                openListener.onLeft(chatId, userId);
            }
        }
    }

    private static void enqueue(int account, long userId) {
        String k = account + "_" + userId;
        if (queued.contains(k)) {
            return;
        }
        queued.add(k);
        queue.add(new long[]{account, userId});
        if (!running) {
            running = true;
            AndroidUtilities.runOnUIThread(SparkMembership::processNext);
        }
    }

    private static void processNext() {
        long wait = SparkRateLimit.waitTime(FEATURE, PER_HOUR);
        if (wait > 0) {
            // Bremse aktiv (FLOOD_WAIT oder Stundenbudget): Warteschlange leeren, spaeter neu befuellen
            queue.clear();
            queued.clear();
            running = false;
            return;
        }
        long[] next = queue.poll();
        if (next == null) {
            running = false;
            return;
        }
        final int account = (int) next[0];
        final long userId = next[1];
        queued.remove(account + "_" + userId);
        TLRPC.User user = MessagesController.getInstance(account).getUser(userId);
        if (user == null || user.self) {
            AndroidUtilities.runOnUIThread(SparkMembership::processNext, 50);
            return;
        }
        SparkRateLimit.use(FEATURE);
        TLRPC.TL_messages_getCommonChats req = new TLRPC.TL_messages_getCommonChats();
        req.user_id = MessagesController.getInstance(account).getInputUser(user);
        req.max_id = 0;
        req.limit = 100;
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            SparkRateLimit.onError(FEATURE, error);
            if (response instanceof TLRPC.messages_Chats) {
                HashSet<Long> common = new HashSet<>();
                for (TLRPC.Chat c : ((TLRPC.messages_Chats) response).chats) {
                    common.add(c.id);
                }
                applyCommonChats(account, userId, common);
            }
            AndroidUtilities.runOnUIThread(SparkMembership::processNext, REQUEST_GAP);
        }));
    }

    /** Aktualisiert alle bekannten Gruppen dieser Person anhand der gemeinsamen Gruppen. */
    private static void applyCommonChats(int account, long userId, HashSet<Long> common) {
        String prefix = account + "_";
        String suffix = "_" + userId;
        ArrayList<Long> chatIds = new ArrayList<>();
        for (String k : infos.keySet()) {
            if (k.startsWith(prefix) && k.endsWith(suffix)) {
                try {
                    chatIds.add(Long.parseLong(k.substring(prefix.length(), k.length() - suffix.length())));
                } catch (NumberFormatException ignore) {
                }
            }
        }
        long now = System.currentTimeMillis();
        for (long chatId : chatIds) {
            if (common.contains(chatId)) {
                reliableChats.add(account + "_" + chatId);
                setState(account, chatId, userId, IN, now);
            } else {
                confirmOut(account, chatId, userId);
            }
        }
    }

    /** Gegenprobe bei Supergruppen, damit "raus" nicht faelschlich angezeigt wird. */
    private static void confirmOut(int account, long chatId, long userId) {
        TLRPC.Chat chat = MessagesController.getInstance(account).getChat(chatId);
        if (chat == null || chat.left) {
            return;
        }
        final boolean reliable = reliableChats.contains(account + "_" + chatId);
        if (!ChatObject.isChannel(chat)) {
            if (reliable) {
                setState(account, chatId, userId, OUT, System.currentTimeMillis());
            } else {
                info(account, chatId, userId).checkedAt = System.currentTimeMillis();
            }
            return;
        }
        if (SparkRateLimit.waitTime(FEATURE, PER_HOUR) > 0) {
            return;
        }
        SparkRateLimit.use(FEATURE);
        TLRPC.TL_channels_getParticipant req = new TLRPC.TL_channels_getParticipant();
        req.channel = MessagesController.getInputChannel(chat);
        req.participant = MessagesController.getInstance(account).getInputPeer(userId);
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            SparkRateLimit.onError(FEATURE, error);
            long now = System.currentTimeMillis();
            if (error != null && error.text != null && error.text.startsWith("FLOOD")) {
                return; // gebremst: keine Aussage moeglich
            }
            if (response != null) {
                // Person ist doch Mitglied (gemeinsame Gruppen unvollstaendig)
                setState(account, chatId, userId, IN, now);
            } else if (error != null && error.text != null && error.text.contains("USER_NOT_PARTICIPANT")) {
                setState(account, chatId, userId, OUT, now);
            } else if (reliable) {
                // Server verraet es nicht (z. B. verborgene Mitglieder), aber gemeinsame Gruppen funktionieren hier
                setState(account, chatId, userId, OUT, now);
            } else {
                // Nicht sicher: lieber keine Markierung als eine falsche
                info(account, chatId, userId).checkedAt = now;
            }
        }));
    }

    /** Chat geoeffnet: aktive Gespraechsteilnehmer regelmaessig pruefen. */
    public static void onChatOpened(int account, long chatId, Listener listener) {
        openAccount = account;
        openChatId = chatId;
        openListener = listener;
        if (activeLoop != null) {
            AndroidUtilities.cancelRunOnUIThread(activeLoop);
        }
        activeLoop = new Runnable() {
            @Override
            public void run() {
                if (openAccount != account || openChatId != chatId) {
                    return;
                }
                if (enabled()) {
                    long now = System.currentTimeMillis();
                    String prefix = account + "_" + chatId + "_";
                    for (java.util.Map.Entry<String, Info> e : infos.entrySet()) {
                        if (e.getKey().startsWith(prefix) && now - e.getValue().lastActive < ACTIVE_WINDOW && e.getValue().state != OUT) {
                            try {
                                enqueue(account, Long.parseLong(e.getKey().substring(prefix.length())));
                            } catch (NumberFormatException ignore) {
                            }
                        }
                    }
                }
                AndroidUtilities.runOnUIThread(this, intervalMs());
            }
        };
        AndroidUtilities.runOnUIThread(activeLoop, intervalMs());
    }

    public static void onChatClosed(int account, long chatId) {
        if (openAccount == account && openChatId == chatId) {
            openAccount = -1;
            openChatId = 0;
            openListener = null;
            if (activeLoop != null) {
                AndroidUtilities.cancelRunOnUIThread(activeLoop);
                activeLoop = null;
            }
        }
    }

    public static int[] INTERVALS = {60, 120, 30};

    public static long intervalMs() {
        int index = SparkConfig.prefs().getInt("memberCheckInterval", 0);
        if (index < 0 || index >= INTERVALS.length) {
            index = 0;
        }
        return INTERVALS[index] * 1000L;
    }

    public static String userName(int account, long userId) {
        TLRPC.User u = MessagesController.getInstance(account).getUser(userId);
        String n = u != null ? UserObject.getUserName(u) : null;
        return TextUtils.isEmpty(n) ? String.valueOf(userId) : n;
    }
}

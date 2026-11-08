package org.telegram.messenger.spark;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/**
 * Verlassene Gruppen: kann man wieder beitreten (oeffentlich / Diskussionsgruppe) oder nicht (privat, gesperrt)?
 * Fragt Telegram bei Bedarf nach dem aktuellen Stand der Gruppe.
 */
public class SparkRejoin {

    /** true = Beitreten geht nicht (privat ohne Einladung oder gesperrt) -> "Gruppe löschen" zeigen. */
    public static boolean cantRejoin(TLRPC.Chat chat) {
        if (chat == null || !ChatObject.isChannel(chat) || !ChatObject.isNotInChat(chat)) return false;
        if (!MessagesController.sparkKeepLeftOn()) return false;
        if (chat instanceof TLRPC.TL_channelForbidden || chat.kicked) return true;
        if (ChatObject.isPublic(chat)) return false;   // oeffentlich: Beitreten geht
        if (chat.has_link) return false;               // Diskussionsgruppe eines Kanals: evtl. moeglich
        return true;                                   // privat: nur per Einladungslink
    }

    private static long lastCheck(int account, long chatId) {
        return SparkConfig.prefs().getLong("rejoinCheck_" + account + "_" + chatId, 0);
    }

    /** Aktuellen Stand bei Telegram holen (hoechstens alle maxAgeMs). */
    public static void refresh(int account, TLRPC.Chat chat, long maxAgeMs) {
        if (chat == null || !ChatObject.isChannel(chat) || !ChatObject.isNotInChat(chat)) return;
        long now = System.currentTimeMillis();
        if (now - lastCheck(account, chat.id) < maxAgeMs) return;
        SparkConfig.prefs().edit().putLong("rejoinCheck_" + account + "_" + chat.id, now).apply();
        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.TL_channels_getChannels req = new TLRPC.TL_channels_getChannels();
        req.id.add(MessagesController.getInputChannel(chat));
        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            if (res instanceof TLRPC.messages_Chats) {
                ArrayList<TLRPC.Chat> chats = ((TLRPC.messages_Chats) res).chats;
                if (!chats.isEmpty()) {
                    mc.putChats(chats, false);
                    // "min"-Antworten uebernimmt Telegram ohne Benutzernamen – hier selbst nachtragen
                    for (TLRPC.Chat fresh : chats) {
                        TLRPC.Chat cur = mc.getChat(fresh.id);
                        if (cur instanceof TLRPC.TL_channelForbidden && !(fresh instanceof TLRPC.TL_channelForbidden)) {
                            // Sperre aufgehoben: "gesperrt"-Eintrag durch die frische Gruppe ersetzen (nicht Mitglied)
                            if (fresh.access_hash == 0) fresh.access_hash = cur.access_hash;
                            fresh.min = false;
                            fresh.left = true;
                            fresh.kicked = false;
                            mc.putChat(fresh, false);
                            ArrayList<TLRPC.Chat> save = new ArrayList<>();
                            save.add(fresh);
                            org.telegram.messenger.MessagesStorage.getInstance(account).putUsersAndChats(null, save, true, true);
                            continue;
                        }
                        if (cur == null || cur == fresh || fresh instanceof TLRPC.TL_channelForbidden) continue;
                        cur.username = fresh.username;
                        cur.usernames = fresh.usernames;
                        if (fresh.username != null) cur.flags |= 64; else cur.flags &= ~64;
                        cur.has_link = fresh.has_link;
                        cur.join_request = fresh.join_request;
                        cur.join_to_send = fresh.join_to_send;
                        ArrayList<TLRPC.Chat> save = new ArrayList<>();
                        save.add(cur);
                        org.telegram.messenger.MessagesStorage.getInstance(account).putUsersAndChats(null, save, true, true);
                    }
                    NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_CHAT);
                    NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload);
                }
            }
        }));
    }

    private static long lastAll;

    /** Alle verlassenen Supergruppen einmal am Tag pruefen (mit Pausen). */
    public static void refreshAll(int account) {
        if (!MessagesController.sparkKeepLeftOn()) return;
        long now = System.currentTimeMillis();
        if (now - lastAll < 10 * 60_000L) return;
        lastAll = now;
        MessagesController mc = MessagesController.getInstance(account);
        int delay = 0;
        for (TLRPC.Dialog d : new ArrayList<>(mc.getAllDialogs())) {
            if (d.id >= 0) continue;
            TLRPC.Chat chat = mc.getChat(-d.id);
            if (chat == null || !ChatObject.isChannel(chat) || !ChatObject.isNotInChat(chat)) continue;
            if (now - lastCheck(account, chat.id) < 24 * 3600_000L) continue;
            final TLRPC.Chat c = chat;
            AndroidUtilities.runOnUIThread(() -> refresh(account, c, 24 * 3600_000L), delay);
            delay += 1500;
        }
    }
}

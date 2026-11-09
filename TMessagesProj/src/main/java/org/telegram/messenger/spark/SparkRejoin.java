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

    /** Kleine Gruppe, die inzwischen in eine Supergruppe umgewandelt wurde: die neue Gruppe (oder null). */
    public static TLRPC.Chat migratedTarget(int account, TLRPC.Chat chat) {
        if (chat == null || ChatObject.isChannel(chat)) return null;
        long id = SparkConfig.prefs().getLong("rejoinMigrated_" + account + "_" + chat.id, 0);
        return id == 0 ? null : MessagesController.getInstance(account).getChat(id);
    }

    private static void record(int account, long chatId, String result) {
        SparkConfig.prefs().edit().putString("rejoinResult_" + account + "_" + chatId,
            new java.text.SimpleDateFormat("dd.MM. HH:mm", java.util.Locale.GERMANY).format(new java.util.Date()) + ": " + result).apply();
    }

    /** Kleine Gruppen: fragen, ob sie umgewandelt wurde (z. B. weil sie oeffentlich wurde). */
    private static void refreshBasic(int account, TLRPC.Chat chat) {
        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.TL_messages_getChats req = new TLRPC.TL_messages_getChats();
        req.id.add(chat.id);
        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            if (err != null) {
                record(account, chat.id, "Fehler " + err.text);
                return;
            }
            if (!(res instanceof TLRPC.messages_Chats) || ((TLRPC.messages_Chats) res).chats.isEmpty()) {
                record(account, chat.id, "keine Antwort");
                return;
            }
            // nicht in Telegrams Speicher uebernehmen: "umgewandelt" wuerde den alten Chat sonst ausblenden
            ArrayList<TLRPC.Chat> chats = ((TLRPC.messages_Chats) res).chats;
            TLRPC.Chat fresh = null;
            for (TLRPC.Chat c : chats) if (c.id == chat.id) fresh = c;
            if (fresh == null || fresh.migrated_to == null) {
                record(account, chat.id, "kleine Gruppe, nicht umgewandelt (Beitreten nur per Einladungslink)");
                return;
            }
            // umgewandelt: neue Supergruppe abfragen
            SparkConfig.prefs().edit().putLong("rejoinMigrated_" + account + "_" + chat.id, fresh.migrated_to.channel_id).apply();
            TLRPC.TL_channels_getChannels r2 = new TLRPC.TL_channels_getChannels();
            r2.id.add(fresh.migrated_to);
            final TLRPC.Chat basic = fresh;
            ConnectionsManager.getInstance(account).sendRequest(r2, (res2, err2) -> AndroidUtilities.runOnUIThread(() -> {
                if (res2 instanceof TLRPC.messages_Chats && !((TLRPC.messages_Chats) res2).chats.isEmpty()) {
                    TLRPC.Chat target = ((TLRPC.messages_Chats) res2).chats.get(0);
                    mc.putChats(((TLRPC.messages_Chats) res2).chats, false);
                    String un = ChatObject.getPublicUsername(target);
                    record(account, chat.id, "umgewandelt in Supergruppe, " + (target instanceof TLRPC.TL_channelForbidden ? "kein Zugriff" : un != null ? "öffentlich @" + un : "privat"));
                } else {
                    record(account, chat.id, "umgewandelt in Supergruppe, " + (err2 != null ? "Fehler " + err2.text : "keine Antwort"));
                }
                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_CHAT);
            }));
        }));
    }

    /** Aktuellen Stand bei Telegram holen (hoechstens alle maxAgeMs). */
    public static void refresh(int account, TLRPC.Chat chat, long maxAgeMs) {
        if (chat != null && !ChatObject.isChannel(chat) && ChatObject.isNotInChat(chat)) {
            long now = System.currentTimeMillis();
            if (now - lastCheck(account, chat.id) < maxAgeMs) return;
            SparkConfig.prefs().edit().putLong("rejoinCheck_" + account + "_" + chat.id, now).apply();
            refreshBasic(account, chat);
            return;
        }
        if (chat == null || !ChatObject.isChannel(chat) || !ChatObject.isNotInChat(chat)) return;
        long now = System.currentTimeMillis();
        if (now - lastCheck(account, chat.id) < maxAgeMs) return;
        SparkConfig.prefs().edit().putLong("rejoinCheck_" + account + "_" + chat.id, now).apply();
        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.TL_channels_getChannels req = new TLRPC.TL_channels_getChannels();
        req.id.add(MessagesController.getInputChannel(chat));
        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            // Ergebnis zur Kontrolle merken (Sparkgram → Infos)
            String result;
            if (err != null) {
                result = "Fehler " + err.text;
            } else if (res instanceof TLRPC.messages_Chats && !((TLRPC.messages_Chats) res).chats.isEmpty()) {
                TLRPC.Chat c = ((TLRPC.messages_Chats) res).chats.get(0);
                String un = ChatObject.getPublicUsername(c);
                result = c instanceof TLRPC.TL_channelForbidden ? "kein Zugriff (privat oder gesperrt)" : (un != null ? "öffentlich @" + un : "privat") + (c.min ? " (Kurzfassung)" : "");
            } else {
                result = "keine Antwort";
            }
            SparkConfig.prefs().edit().putString("rejoinResult_" + account + "_" + chat.id,
                new java.text.SimpleDateFormat("dd.MM. HH:mm", java.util.Locale.GERMANY).format(new java.util.Date()) + ": " + result).apply();
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

    public static String lastResult(int account, long chatId) {
        return SparkConfig.prefs().getString("rejoinResult_" + account + "_" + chatId, "noch nicht geprüft");
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
            if (chat == null || !ChatObject.isNotInChat(chat)) continue;
            if (now - lastCheck(account, chat.id) < 24 * 3600_000L) continue;
            final TLRPC.Chat c = chat;
            AndroidUtilities.runOnUIThread(() -> refresh(account, c, 24 * 3600_000L), delay);
            delay += 1500;
        }
    }
}

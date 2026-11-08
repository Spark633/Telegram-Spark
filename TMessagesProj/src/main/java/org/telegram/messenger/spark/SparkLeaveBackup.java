package org.telegram.messenger.spark;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Vor dem Verlassen einer Gruppe den Verlauf (bis MAX Nachrichten) herunterladen und lokal speichern,
 * damit er nach dem Verlassen vollstaendig lesbar bleibt.
 */
public class SparkLeaveBackup {

    private static final int MAX = 10000;
    private static final HashSet<String> passing = new HashSet<>();
    private static final HashSet<String> running = new HashSet<>();

    /**
     * true = Sicherung gestartet, das Verlassen (then) folgt danach automatisch.
     * false = direkt verlassen (Funktion aus, schon gesichert oder zweiter Aufruf).
     */
    public static boolean backupFirst(int account, long chatId, Runnable then) {
        String key = account + "_" + chatId;
        if (passing.remove(key)) return false;
        if (running.contains(key)) return true; // laeuft schon
        if (!SparkConfig.get(SparkConfig.LEAVE_BACKUP) || !MessagesController.sparkKeepLeftOn()) return false;
        TLRPC.Chat chat = MessagesController.getInstance(account).getChat(chatId);
        if (chat == null) return false;
        running.add(key);
        bulletin("✦ Verlauf wird vor dem Verlassen gesichert …");
        load(account, chatId, 0, 0, () -> {
            running.remove(key);
            passing.add(key);
            AndroidUtilities.runOnUIThread(then);
        });
        return true;
    }

    private static void load(int account, long chatId, int offsetId, int count, Runnable done) {
        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
        req.peer = mc.getInputPeer(-chatId);
        req.offset_id = offsetId;
        req.limit = 100;
        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            try {
                if (res instanceof TLRPC.messages_Messages) {
                    TLRPC.messages_Messages m = (TLRPC.messages_Messages) res;
                    mc.putUsers(m.users, false);
                    mc.putChats(m.chats, false);
                    int minId = 0;
                    for (TLRPC.Message msg : m.messages) {
                        if (minId == 0 || msg.id < minId) minId = msg.id;
                    }
                    if (!m.messages.isEmpty()) {
                        MessagesStorage.getInstance(account).putUsersAndChats(m.users, m.chats, true, true);
                        MessagesStorage.getInstance(account).putMessages(new ArrayList<>(m.messages), true, true, true, 0, 0, 0);
                    }
                    int total = count + m.messages.size();
                    if (!m.messages.isEmpty() && minId > 1 && total < MAX) {
                        final int next = minId;
                        AndroidUtilities.runOnUIThread(() -> load(account, chatId, next, total, done), 300);
                        return;
                    }
                    bulletin("✦ " + total + " Nachrichten gesichert");
                } else if (err != null) {
                    bulletin("✦ Verlauf konnte nicht vollständig gesichert werden (" + err.text + ")");
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
            done.run();
        }));
    }

    private static void bulletin(String text) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                org.telegram.ui.ActionBar.BaseFragment f = LaunchActivity.getLastFragment();
                if (f != null && f.getParentActivity() != null) {
                    org.telegram.ui.Components.BulletinFactory.of(f).createSimpleBulletin(R.raw.ic_download, text).show();
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        });
    }
}

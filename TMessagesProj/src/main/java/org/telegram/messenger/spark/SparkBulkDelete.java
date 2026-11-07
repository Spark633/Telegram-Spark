package org.telegram.messenger.spark;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/**
 * Loescht alle eigenen Nachrichten in einem Chat: sucht sie seitenweise und loescht in
 * 100er-Paketen. Gebremst ueber SparkRateLimit, damit Telegram nicht blockiert.
 */
public class SparkBulkDelete {

    private static final String FEATURE = "bulkDelete";
    private static final int PER_HOUR = 600;
    private static final long GAP = 1200;

    public interface Listener {
        void onProgress(int found, int deleted, boolean searching);
        void onDone(int deleted, String error);

        /** Nur im Auflisten-Modus: alle gefundenen eigenen Nachrichten. */
        default void onCollected(ArrayList<TLRPC.Message> messages) {
        }
    }

    private final int account;
    private final long dialogId;
    private final Listener listener;
    private final ArrayList<Integer> ids = new ArrayList<>();
    private final ArrayList<TLRPC.Message> found = new ArrayList<>();
    private boolean cancelled;
    private boolean collectOnly;
    private int deleted;

    public SparkBulkDelete(int account, long dialogId, Listener listener) {
        this.account = account;
        this.dialogId = dialogId;
        this.listener = listener;
    }

    /** Nur suchen und auflisten, nicht loeschen. */
    public SparkBulkDelete collectOnly() {
        collectOnly = true;
        return this;
    }

    /** Bestimmte Nachrichten loeschen (z. B. Auswahl aus der Liste). */
    public void deleteIds(ArrayList<Integer> selected) {
        ids.clear();
        ids.addAll(selected);
        deleteNext();
    }

    public void cancel() {
        cancelled = true;
    }

    public void start() {
        search(0);
    }

    private boolean waitIfNeeded(Runnable retry) {
        long wait = SparkRateLimit.waitTime(FEATURE, PER_HOUR);
        if (wait > 0) {
            if (wait > 5 * 60_000L) {
                listener.onDone(deleted, "Telegram bremst gerade – bitte später erneut versuchen");
                return true;
            }
            AndroidUtilities.runOnUIThread(retry, wait);
            return true;
        }
        return false;
    }

    private void search(int offsetId) {
        if (cancelled) {
            listener.onDone(deleted, null);
            return;
        }
        if (waitIfNeeded(() -> search(offsetId))) {
            return;
        }
        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
        req.peer = mc.getInputPeer(dialogId);
        req.from_id = mc.getInputPeer(UserConfig.getInstance(account).getClientUserId());
        req.q = "";
        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        req.offset_id = offsetId;
        req.limit = 100;
        SparkRateLimit.use(FEATURE);
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            SparkRateLimit.onError(FEATURE, error);
            if (error != null) {
                if (error.text != null && error.text.startsWith("FLOOD")) {
                    search(offsetId); // wartet automatisch
                } else {
                    listener.onDone(deleted, error.text);
                }
                return;
            }
            TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
            int last = 0;
            for (TLRPC.Message m : res.messages) {
                if (m.id > 0 && !(m instanceof TLRPC.TL_messageService)) {
                    ids.add(m.id);
                    found.add(m);
                }
                last = m.id;
            }
            listener.onProgress(ids.size(), deleted, true);
            if (res.messages.size() >= 100 && last > 0) {
                AndroidUtilities.runOnUIThread(() -> search(last), GAP);
            } else if (collectOnly) {
                listener.onCollected(found);
            } else {
                deleteNext();
            }
        }));
    }

    private void deleteNext() {
        if (cancelled || ids.isEmpty()) {
            listener.onDone(deleted, null);
            return;
        }
        if (waitIfNeeded(this::deleteNext)) {
            return;
        }
        int n = Math.min(100, ids.size());
        ArrayList<Integer> chunk = new ArrayList<>(ids.subList(0, n));
        ids.subList(0, n).clear();
        SparkRateLimit.use(FEATURE);
        MessagesController.getInstance(account).deleteMessages(chunk, null, null, dialogId, 0, true, 0);
        deleted += chunk.size();
        listener.onProgress(deleted + ids.size(), deleted, false);
        AndroidUtilities.runOnUIThread(this::deleteNext, GAP);
    }
}

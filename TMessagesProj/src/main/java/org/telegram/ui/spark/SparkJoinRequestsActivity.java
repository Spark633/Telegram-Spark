package org.telegram.ui.spark;

import android.os.Bundle;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.R;
import org.telegram.messenger.spark.SparkJoinRequests;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/** Liste der gestellten Beitrittsanfragen mit aktuellem Stand (offen / angenommen / Link abgelaufen). */
public class SparkJoinRequestsActivity extends UniversalFragment {

    private static final int ID_BASE = 1000;
    private ArrayList<SparkJoinRequests.Item> list = new ArrayList<>();
    private boolean checked;

    @Override
    protected CharSequence getTitle() {
        return "Beitrittsanfragen";
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!checked) {
            checked = true;
            refreshAll();
        }
        if (listView != null) listView.adapter.update(true);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        list = SparkJoinRequests.list(currentAccount);
        if (list.isEmpty()) {
            items.add(UItem.asShadow("Noch keine Beitrittsanfragen. Sobald du in Sparkgram eine Beitrittsanfrage stellst, erscheint sie hier mit ihrem Stand."));
            return;
        }
        SimpleDateFormat f = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.GERMANY);
        int open = 0;
        for (SparkJoinRequests.Item it : list) if (it.status == null || it.status.startsWith("⏳")) open++;
        items.add(UItem.asHeader(open + " offen · " + list.size() + " insgesamt"));
        for (int i = 0; i < list.size(); i++) {
            SparkJoinRequests.Item it = list.get(i);
            String st = it.status != null ? it.status : "⏳ offen";
            items.add(UItem.asButton(ID_BASE + i, R.drawable.msg_requests, it.title == null || it.title.isEmpty() ? "Gruppe" : it.title,
                st + " · " + f.format(new Date(it.time))));
        }
        items.add(UItem.asShadow("Telegram meldet Ablehnungen nicht – eine abgelehnte Anfrage bleibt hier „offen“, bis du sie lange drückst und entfernst. Gezählt werden nur Anfragen, die du in Sparkgram gestellt hast."));
    }

    /** Stand jeder Anfrage bei Telegram nachfragen (mit kleinen Pausen). */
    private void refreshAll() {
        ArrayList<SparkJoinRequests.Item> items = SparkJoinRequests.list(currentAccount);
        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            final SparkJoinRequests.Item it = items.get(i);
            AndroidUtilities.runOnUIThread(() -> refresh(index, it), i * 400L);
        }
    }

    private void refresh(int index, SparkJoinRequests.Item it) {
        if (it.chatId != 0) {
            TLRPC.Chat chat = getMessagesController().getChat(it.chatId);
            if (chat != null && !ChatObject.isNotInChat(chat)) {
                set(index, "✅ angenommen", it.chatId);
                return;
            }
        }
        if (it.hash == null || it.hash.isEmpty()) {
            if (it.chatId != 0) {
                // ohne Link: Gruppe neu laden, damit ein Beitritt erkannt wird
                getMessagesController().loadFullChat(it.chatId, 0, true);
            }
            return;
        }
        TLRPC.TL_messages_checkChatInvite req = new TLRPC.TL_messages_checkChatInvite();
        req.hash = it.hash;
        getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            if (res instanceof TLRPC.TL_chatInviteAlready) {
                TLRPC.Chat c = ((TLRPC.ChatInvite) res).chat;
                if (c != null) getMessagesController().putChat(c, false);
                set(index, "✅ angenommen", c != null ? c.id : 0);
            } else if (res instanceof TLRPC.TL_chatInvitePeek) {
                set(index, "⏳ offen", 0);
            } else if (res instanceof TLRPC.ChatInvite) {
                set(index, ((TLRPC.ChatInvite) res).request_needed ? "⏳ offen" : "🔓 Beitritt jetzt ohne Anfrage möglich", 0);
            } else if (err != null) {
                set(index, "INVITE_HASH_EXPIRED".equals(err.text) ? "⌛ Link abgelaufen" : "⚠ " + err.text, 0);
            }
        }));
    }

    private void set(int index, String status, long chatId) {
        SparkJoinRequests.setStatus(currentAccount, index, status, chatId);
        if (listView != null) listView.adapter.update(true);
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        int i = item.id - ID_BASE;
        if (i < 0 || i >= list.size()) return;
        SparkJoinRequests.Item it = list.get(i);
        if (it.chatId != 0) {
            Bundle args = new Bundle();
            args.putLong("chat_id", it.chatId);
            presentFragment(new ChatActivity(args));
        } else if (it.hash != null) {
            org.telegram.messenger.browser.Browser.openUrl(getParentActivity(), "https://t.me/+" + it.hash);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        int i = item.id - ID_BASE;
        if (i < 0 || i >= list.size()) return false;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(SparkUi.mark("Anfrage entfernen"));
        b.setMessage("„" + list.get(i).title + "“ aus der Liste entfernen? (Die Anfrage bei Telegram bleibt bestehen.)");
        b.setPositiveButton("Entfernen", (d, w) -> {
            SparkJoinRequests.remove(currentAccount, i);
            listView.adapter.update(true);
        });
        b.setNegativeButton("Abbrechen", null);
        showDialog(b.create());
        return true;
    }
}

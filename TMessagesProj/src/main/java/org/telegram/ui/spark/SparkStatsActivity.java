package org.telegram.ui.spark;

import android.view.View;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.spark.SparkCollection;
import org.telegram.messenger.spark.SparkPeople;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** Statistiken: Anzahl Chats nach Art, Ungelesenes, Stummgeschaltetes, Sparkgram-Daten. */
public class SparkStatsActivity extends UniversalFragment {

    @Override
    protected CharSequence getTitle() {
        return "Statistiken";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        MessagesController mc = getMessagesController();
        ArrayList<TLRPC.Dialog> all = mc.getAllDialogs();
        int people = 0, bots = 0, groups = 0, supergroups = 0, channels = 0, secret = 0, archived = 0, muted = 0, unreadChats = 0, unreadMsgs = 0, pinned = 0;
        for (TLRPC.Dialog d : all) {
            long id = d.id;
            if (d.folder_id == 1) archived++;
            if (d.pinned) pinned++;
            if (mc.isDialogMuted(id, 0)) muted++;
            if (d.unread_count > 0 || d.unread_mark) {
                unreadChats++;
                unreadMsgs += d.unread_count;
            }
            if (DialogObject.isEncryptedDialog(id)) {
                secret++;
            } else if (DialogObject.isUserDialog(id)) {
                TLRPC.User u = mc.getUser(id);
                if (u != null && u.bot) bots++; else people++;
            } else {
                TLRPC.Chat c = mc.getChat(-id);
                if (ChatObject.isChannel(c) && !c.megagroup) channels++;
                else if (ChatObject.isChannel(c)) supergroups++;
                else groups++;
            }
        }
        items.add(UItem.asHeader("Chats (" + all.size() + ")"));
        items.add(UItem.asButton(0, "👤 Personen", String.valueOf(people)));
        items.add(UItem.asButton(0, "🤖 Bots", String.valueOf(bots)));
        items.add(UItem.asButton(0, "👥 Gruppen", String.valueOf(groups + supergroups)));
        items.add(UItem.asButton(0, "📢 Kanäle", String.valueOf(channels)));
        if (secret > 0) items.add(UItem.asButton(0, "🔒 Geheime Chats", String.valueOf(secret)));
        items.add(UItem.asButton(0, "🗄 Archiviert", String.valueOf(archived)));
        items.add(UItem.asButton(0, "📌 Angeheftet", String.valueOf(pinned)));
        items.add(UItem.asButton(0, "🔕 Stummgeschaltet", String.valueOf(muted)));
        items.add(UItem.asButton(0, "🔵 Ungelesen", unreadChats + " Chats · " + unreadMsgs + " Nachr."));
        items.add(UItem.asShadow("Gezählt wird, was gerade geladen ist – nach dem Start kann die Zahl noch etwas steigen."));

        items.add(UItem.asHeader("Sparkgram"));
        ArrayList<SparkCollection.Item> col = SparkCollection.get(currentAccount).list(null);
        items.add(UItem.asButton(0, "Sammlung", col.size() + " Einträge"));
        items.add(UItem.asButton(0, "Einzeln stummgeschaltet", String.valueOf(SparkPeople.allMuted(currentAccount).size())));
        TLRPC.User me = getUserConfig().getCurrentUser();
        items.add(UItem.asShadow("Konto: " + (me != null ? UserObject.getUserName(me) : "?")));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}

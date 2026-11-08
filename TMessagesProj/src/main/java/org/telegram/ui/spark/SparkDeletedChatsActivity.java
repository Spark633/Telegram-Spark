package org.telegram.ui.spark;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.spark.SparkDeletedChats;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/** Gelöschte bzw. verlassene Chats: antippen = Chat öffnen, lange drücken = aus der Liste entfernen. */
public class SparkDeletedChatsActivity extends UniversalFragment {

    private static final int ID_BASE = 1000;
    private ArrayList<SparkDeletedChats.Item> list = new ArrayList<>();

    @Override
    protected CharSequence getTitle() {
        return "Gelöschte & verlassene Chats";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        list = SparkDeletedChats.list(currentAccount);
        if (list.isEmpty()) {
            items.add(UItem.asShadow("Noch keine gelöschten Chats."));
            return;
        }
        SimpleDateFormat f = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.GERMANY);
        for (int i = 0; i < list.size(); i++) {
            SparkDeletedChats.Item it = list.get(i);
            String title = (it.dialogId > 0 ? "👤 " : "👥 ") + it.name + (TextUtils.isEmpty(it.username) ? "" : " (@" + it.username + ")");
            String why = it.kind == SparkDeletedChats.KIND_KICKED ? "entfernt" : it.kind == SparkDeletedChats.KIND_LEFT ? "verlassen" : "gelöscht";
            items.add(UItem.asButton(ID_BASE + i, title, why + " · " + f.format(new Date(it.time * 1000L))));
            if (!TextUtils.isEmpty(it.last)) items.add(UItem.asShadow("Zuletzt: " + it.last));
        }
        items.add(UItem.asShadow("Antippen öffnet den Chat (bei Gruppen nur, wenn du noch Zugriff hast). Lange drücken entfernt den Eintrag."));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        int i = item.id - ID_BASE;
        if (i < 0 || i >= list.size()) return;
        SparkDeletedChats.Item it = list.get(i);
        Bundle args = new Bundle();
        if (it.dialogId > 0) args.putLong("user_id", it.dialogId); else args.putLong("chat_id", -it.dialogId);
        presentFragment(new ChatActivity(args));
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        int i = item.id - ID_BASE;
        if (i < 0 || i >= list.size()) return false;
        SparkDeletedChats.Item it = list.get(i);
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(it.name);
        b.setMessage("Aus der Liste entfernen?");
        b.setPositiveButton("Entfernen", (d, w) -> {
            SparkDeletedChats.remove(currentAccount, it.dialogId);
            listView.adapter.update(true);
        });
        b.setNegativeButton("Abbrechen", null);
        showDialog(b.create());
        return true;
    }
}

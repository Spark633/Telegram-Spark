package org.telegram.ui.spark;

import android.view.View;

import org.telegram.messenger.UserObject;
import org.telegram.messenger.spark.SparkPeople;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** Einzeln stummgeschaltete Personen: antippen = wieder laut. */
public class SparkMutedActivity extends UniversalFragment {

    private static final int ID_BASE = 1000;
    private ArrayList<Long> ids = new ArrayList<>();

    @Override
    protected CharSequence getTitle() {
        return "Stummgeschaltet";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        ids = SparkPeople.allMuted(currentAccount);
        if (ids.isEmpty()) {
            items.add(UItem.asShadow("Niemand stummgeschaltet."));
            return;
        }
        for (int i = 0; i < ids.size(); i++) {
            TLRPC.User u = getMessagesController().getUser(ids.get(i));
            items.add(UItem.asButton(ID_BASE + i, u != null ? UserObject.getUserName(u) : "ID " + ids.get(i), "🔕"));
        }
        items.add(UItem.asShadow("Antippen = wieder laut."));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        int i = item.id - ID_BASE;
        if (i < 0 || i >= ids.size()) return;
        long id = ids.get(i);
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(item.text);
        b.setMessage("Stummschaltung aufheben?");
        b.setPositiveButton("Aufheben", (d, w) -> {
            SparkPeople.setMuted(currentAccount, id, false);
            listView.adapter.update(true);
        });
        b.setNegativeButton("Abbrechen", null);
        showDialog(b.create());
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}

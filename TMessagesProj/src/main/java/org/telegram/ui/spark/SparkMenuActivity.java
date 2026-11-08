package org.telegram.ui.spark;

import android.view.View;

import org.telegram.messenger.spark.SparkMenu;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** Nachrichten-Menue aufraeumen: Haken = Eintrag wird angezeigt. */
public class SparkMenuActivity extends UniversalFragment {

    private static final int ID_BASE = 1000;

    @Override
    protected CharSequence getTitle() {
        return "Nachrichten-Menü";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asShadow("Haken weg = Eintrag ausblenden."));
        for (int i = 0; i < SparkMenu.ENTRIES.length; i++) {
            int opt = (Integer) SparkMenu.ENTRIES[i][0];
            items.add(UItem.asCheck(ID_BASE + i, (String) SparkMenu.ENTRIES[i][1]).setChecked(!SparkMenu.hidden().contains(opt)));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        int i = item.id - ID_BASE;
        if (i < 0 || i >= SparkMenu.ENTRIES.length) return;
        int opt = (Integer) SparkMenu.ENTRIES[i][0];
        SparkMenu.setHidden(opt, !SparkMenu.hidden().contains(opt));
        listView.adapter.update(true);
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}

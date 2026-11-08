package org.telegram.ui.spark;

import android.view.View;

import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.spark.SparkAccounts;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** Versteckte Konten: Haken = in der Kontoauswahl ausblenden. */
public class SparkHiddenAccountsActivity extends UniversalFragment {

    private static final int ID_REVEAL = 1;
    private static final int ID_BASE = 1000;

    @Override
    protected CharSequence getTitle() {
        return "Versteckte Konten";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asCheck(ID_REVEAL, "Versteckte jetzt anzeigen (bis Neustart)").setChecked(SparkAccounts.revealed));
        items.add(UItem.asShadow("Versteckte Konten erscheinen nicht in der Kontoauswahl und in den Einstellungen. Sie bleiben angemeldet."));
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            UserConfig uc = UserConfig.getInstance(a);
            if (!uc.isClientActivated()) continue;
            TLRPC.User u = uc.getCurrentUser();
            String name = u != null ? UserObject.getUserName(u) : "Konto " + (a + 1);
            items.add(UItem.asCheck(ID_BASE + a, name + (a == currentAccount ? " (aktiv)" : "")).setChecked(SparkAccounts.isHiddenRaw(a)));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_REVEAL) {
            SparkAccounts.revealed = !SparkAccounts.revealed;
        } else if (item.id >= ID_BASE) {
            int a = item.id - ID_BASE;
            SparkAccounts.setHidden(a, !SparkAccounts.isHiddenRaw(a));
        }
        listView.adapter.update(true);
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}

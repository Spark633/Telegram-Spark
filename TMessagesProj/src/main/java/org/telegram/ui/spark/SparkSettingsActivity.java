package org.telegram.ui.spark;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.telegram.messenger.R;
import org.telegram.messenger.spark.SparkConfig;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** Einstellungsseite "Sparkgram-Funktionen": jede Zusatzfunktion einzeln schaltbar. */
public class SparkSettingsActivity extends UniversalFragment {

    private static final int ID_PROFILE_TOOLS = 1;
    private static final int ID_CLONE_NAME = 2;
    private static final int ID_CLONE_BIO = 3;
    private static final int ID_CLONE_PHOTO = 4;
    private static final int ID_PROFILE_LIBRARY = 5;

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.SparkSettings);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(getString(R.string.SparkHeaderProfile)));
        items.add(UItem.asCheck(ID_PROFILE_TOOLS, getString(R.string.SparkProfileTools)).setChecked(SparkConfig.get(SparkConfig.PROFILE_TOOLS)));
        items.add(UItem.asButton(ID_PROFILE_LIBRARY, R.drawable.msg_contacts, getString(R.string.SparkProfileLibrary)));
        items.add(UItem.asShadow(getString(R.string.SparkProfileToolsInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderClone)));
        items.add(UItem.asCheck(ID_CLONE_NAME, getString(R.string.SparkCloneName)).setChecked(SparkConfig.get(SparkConfig.CLONE_NAME)));
        items.add(UItem.asCheck(ID_CLONE_BIO, getString(R.string.SparkCloneBio)).setChecked(SparkConfig.get(SparkConfig.CLONE_BIO)));
        items.add(UItem.asCheck(ID_CLONE_PHOTO, getString(R.string.SparkClonePhoto)).setChecked(SparkConfig.get(SparkConfig.CLONE_PHOTO)));
        items.add(UItem.asShadow(getString(R.string.SparkCloneInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderAccounts)));
        items.add(UItem.asShadow(getString(R.string.SparkAccountsInfo)));

        items.add(UItem.asShadow(getString(R.string.SparkComingSoon)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_PROFILE_TOOLS:
                SparkConfig.toggle(SparkConfig.PROFILE_TOOLS);
                break;
            case ID_CLONE_NAME:
                SparkConfig.toggle(SparkConfig.CLONE_NAME);
                break;
            case ID_CLONE_BIO:
                SparkConfig.toggle(SparkConfig.CLONE_BIO);
                break;
            case ID_CLONE_PHOTO:
                SparkConfig.toggle(SparkConfig.CLONE_PHOTO);
                break;
            case ID_PROFILE_LIBRARY:
                presentFragment(new SparkProfileLibraryActivity());
                return;
            default:
                return;
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}

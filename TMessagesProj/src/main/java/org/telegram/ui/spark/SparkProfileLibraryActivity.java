package org.telegram.ui.spark;

import static org.telegram.messenger.LocaleController.getString;

import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.spark.SparkProfiles;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** Liste aller gesicherten und geklonten Profile; jedes kann wieder angewendet werden. */
public class SparkProfileLibraryActivity extends UniversalFragment {

    private static final int ID_BACKUP = 1;
    private static final int ID_ENTRY_BASE = 1000;

    private ArrayList<SparkProfiles.Entry> entries = new ArrayList<>();

    @Override
    public boolean onFragmentCreate() {
        entries = SparkProfiles.load(currentAccount);
        return super.onFragmentCreate();
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.SparkProfileLibrary);
    }

    private void reload() {
        entries = SparkProfiles.load(currentAccount);
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asButton(ID_BACKUP, R.drawable.msg_saved, getString(R.string.SparkProfileBackupNow)).accent());
        items.add(UItem.asShadow(null));
        if (entries.isEmpty()) {
            items.add(UItem.asShadow(getString(R.string.SparkProfileLibraryEmpty)));
            return;
        }
        items.add(UItem.asHeader(getString(R.string.SparkProfileLibrary)));
        for (int i = entries.size() - 1; i >= 0; i--) {
            SparkProfiles.Entry e = entries.get(i);
            String title = e.displayName();
            if (!TextUtils.isEmpty(e.label)) {
                title = e.label + " · " + title;
            }
            String value = LocaleController.formatDateChat(e.created / 1000);
            items.add(UItem.asButton(ID_ENTRY_BASE + i, e.original ? R.drawable.msg_openprofile : R.drawable.msg_contacts, title, value));
        }
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_BACKUP) {
            final AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
            progress.show();
            SparkProfiles.backupCurrent(currentAccount, ok -> {
                progress.dismiss();
                reload();
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, getString(R.string.SparkProfileSaved)).show();
            });
            return;
        }
        int index = item.id - ID_ENTRY_BASE;
        if (index < 0 || index >= entries.size()) {
            return;
        }
        final SparkProfiles.Entry entry = entries.get(index);
        CharSequence[] actions = new CharSequence[]{
            getString(R.string.SparkProfileApply),
            getString(R.string.SparkProfileRename),
            getString(R.string.SparkProfileDelete)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(entry.displayName());
        builder.setItems(actions, (dialog, which) -> {
            if (which == 0) {
                applyEntry(entry);
            } else if (which == 1) {
                renameEntry(entry);
            } else if (which == 2) {
                SparkProfiles.remove(currentAccount, entry);
                reload();
            }
        });
        showDialog(builder.create());
    }

    private void applyEntry(SparkProfiles.Entry entry) {
        final AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progress.show();
        SparkProfiles.apply(currentAccount, entry, (ok, error) -> {
            progress.dismiss();
            if (ok) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, getString(R.string.SparkProfileApplied)).show();
            } else {
                BulletinFactory.of(this).createErrorBulletin(LocaleController.formatString(R.string.SparkError, String.valueOf(error))).show();
            }
        });
    }

    private void renameEntry(SparkProfiles.Entry entry) {
        final EditText input = new EditText(getParentActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setText(entry.label == null ? "" : entry.label);
        input.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        input.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        input.setHint(entry.displayName());
        FrameLayout container = new FrameLayout(getParentActivity());
        container.addView(input, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 24, 0, 24, 0));
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString(R.string.SparkProfileRename));
        builder.setView(container);
        builder.setPositiveButton(getString(R.string.OK), (dialog, which) -> {
            String label = input.getText().toString().trim();
            SparkProfiles.rename(currentAccount, entry, TextUtils.isEmpty(label) ? null : label);
            reload();
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(builder.create());
        AndroidUtilities.runOnUIThread(() -> {
            input.requestFocus();
            AndroidUtilities.showKeyboard(input);
        }, 200);
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}

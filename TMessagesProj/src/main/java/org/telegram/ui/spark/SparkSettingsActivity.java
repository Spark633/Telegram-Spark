package org.telegram.ui.spark;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.spark.SparkUpdater;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.BulletinFactory;

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
    private static final int ID_MASTER = 6;
    private static final int ID_UPDATE_CHECK = 7;
    private static final int ID_UPDATE_TEST = 8;
    private static final int ID_REPO = 9;
    private static final int ID_NOTES = 10;
    private static final int ID_NOTES_LIST = 11;
    private static final int ID_CRASH = 12;
    private static final int ID_VERSION = 13;

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.SparkSettings);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asRippleCheck(ID_MASTER, getString(R.string.SparkMaster)).setChecked(SparkConfig.getRaw(SparkConfig.MASTER)));
        items.add(UItem.asShadow(getString(R.string.SparkMasterInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderProfile)));
        items.add(UItem.asCheck(ID_PROFILE_TOOLS, getString(R.string.SparkProfileTools)).setChecked(SparkConfig.getRaw(SparkConfig.PROFILE_TOOLS)));
        items.add(UItem.asButton(ID_PROFILE_LIBRARY, R.drawable.msg_contacts, getString(R.string.SparkProfileLibrary)));
        items.add(UItem.asShadow(getString(R.string.SparkProfileToolsInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkNotes)));
        items.add(UItem.asCheck(ID_NOTES, getString(R.string.SparkNotesToggle)).setChecked(SparkConfig.getRaw(SparkConfig.NOTES)));
        items.add(UItem.asButton(ID_NOTES_LIST, R.drawable.msg_stories_caption, getString(R.string.SparkNotesAll)));
        items.add(UItem.asShadow(getString(R.string.SparkNotesInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderClone)));
        items.add(UItem.asCheck(ID_CLONE_NAME, getString(R.string.SparkCloneName)).setChecked(SparkConfig.getRaw(SparkConfig.CLONE_NAME)));
        items.add(UItem.asCheck(ID_CLONE_BIO, getString(R.string.SparkCloneBio)).setChecked(SparkConfig.getRaw(SparkConfig.CLONE_BIO)));
        items.add(UItem.asCheck(ID_CLONE_PHOTO, getString(R.string.SparkClonePhoto)).setChecked(SparkConfig.getRaw(SparkConfig.CLONE_PHOTO)));
        items.add(UItem.asShadow(getString(R.string.SparkCloneInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderAccounts)));
        items.add(UItem.asShadow(getString(R.string.SparkAccountsInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderUpdates)));
        items.add(UItem.asButton(ID_VERSION, R.drawable.msg_info, getString(R.string.SparkVersion), SparkUpdater.buildLabel()));
        items.add(UItem.asButton(ID_UPDATE_CHECK, R.drawable.msg_retry, getString(R.string.SparkUpdateCheck)));
        items.add(UItem.asCheck(ID_UPDATE_TEST, getString(R.string.SparkUpdateTestBuilds)).setChecked(SparkConfig.getRaw(SparkConfig.UPDATE_TEST_BUILDS)));
        items.add(UItem.asButton(ID_REPO, R.drawable.msg_link2, getString(R.string.SparkRepo)));
        items.add(UItem.asShadow(getString(R.string.SparkUpdateInfo)));

        if (org.telegram.messenger.spark.SparkCrashLog.read(getContext()) != null) {
            items.add(UItem.asButton(ID_CRASH, R.drawable.msg_report, getString(R.string.SparkCrashLog)).red());
            items.add(UItem.asShadow(null));
        }

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
            case ID_CRASH: {
                final String log = org.telegram.messenger.spark.SparkCrashLog.read(getContext());
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(getString(R.string.SparkCrashLog));
                builder.setMessage(log == null ? "" : (log.length() > 3000 ? log.substring(0, 3000) + "…" : log));
                builder.setPositiveButton(getString(R.string.Copy), (d, w) -> {
                    AndroidUtilities.addToClipboard(log);
                    BulletinFactory.of(this).createCopyBulletin(getString(R.string.TextCopied)).show();
                });
                builder.setNeutralButton(getString(R.string.Delete), (d, w) -> {
                    org.telegram.messenger.spark.SparkCrashLog.clear(getContext());
                    listView.adapter.update(true);
                });
                builder.setNegativeButton(getString(R.string.Close), null);
                showDialog(builder.create());
                return;
            }
            case ID_VERSION: {
                String text = "Sparkgram " + SparkUpdater.buildLabel() + "\nTelegram-Basis " + org.telegram.messenger.BuildConfig.BUILD_VERSION_STRING + "\nCommit " + org.telegram.messenger.BuildConfig.SPARK_COMMIT;
                AndroidUtilities.addToClipboard(text);
                BulletinFactory.of(this).createCopyBulletin(getString(R.string.TextCopied)).show();
                return;
            }
            case ID_NOTES:
                SparkConfig.toggle(SparkConfig.NOTES);
                break;
            case ID_NOTES_LIST:
                presentFragment(new SparkNotesActivity());
                return;
            case ID_MASTER:
                SparkConfig.toggle(SparkConfig.MASTER);
                break;
            case ID_UPDATE_TEST:
                SparkConfig.toggle(SparkConfig.UPDATE_TEST_BUILDS);
                break;
            case ID_UPDATE_CHECK:
                checkForUpdate();
                return;
            case ID_REPO:
                Browser.openUrl(getParentActivity(), SparkUpdater.REPO_URL);
                return;
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

    private void checkForUpdate() {
        final AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progress.show();
        SparkUpdater.check((release, error) -> {
            progress.dismiss();
            if (getParentActivity() == null) {
                return;
            }
            if (release == null) {
                BulletinFactory.of(this).createErrorBulletin(LocaleController.formatString(R.string.SparkError, String.valueOf(error))).show();
                return;
            }
            if (!release.isNewer()) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, getString(R.string.SparkUpdateNone)).show();
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(getString(R.string.SparkUpdateAvailable));
            builder.setMessage(release.apkName + "\n" + AndroidUtilities.formatFileSize(release.apkSize));
            builder.setPositiveButton(getString(R.string.SparkUpdateInstall), (d, w) -> downloadUpdate(release));
            builder.setNegativeButton(getString(R.string.Cancel), null);
            showDialog(builder.create());
        });
    }

    private void downloadUpdate(SparkUpdater.Release release) {
        final AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progress.setCanCancel(false);
        progress.show();
        SparkUpdater.download(release, null, (file, error) -> {
            progress.dismiss();
            if (file == null) {
                BulletinFactory.of(this).createErrorBulletin(LocaleController.formatString(R.string.SparkError, String.valueOf(error))).show();
                return;
            }
            if (getParentActivity() != null) {
                SparkUpdater.install(getParentActivity(), file);
            }
        });
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}

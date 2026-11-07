package org.telegram.ui.spark;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.spark.SparkConfig;
import org.telegram.messenger.spark.SparkCrashLog;
import org.telegram.messenger.spark.SparkUpdater;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * Einstellungsseite "Sparkgram-Funktionen": jede Zusatzfunktion hat einen eigenen Schalter.
 * Ist der Hauptschalter aus, wird alles darunter ausgegraut und ist nicht aenderbar.
 */
public class SparkSettingsActivity extends UniversalFragment {

    private static final int ID_MASTER = 1;
    private static final int ID_COPY_ID = 2;
    private static final int ID_INFO = 3;
    private static final int ID_CLONE = 4;
    private static final int ID_CLONE_NAME = 5;
    private static final int ID_CLONE_BIO = 6;
    private static final int ID_CLONE_PHOTO = 7;
    private static final int ID_PROFILE_LIBRARY = 8;
    private static final int ID_PHOTO_MANAGER = 9;
    private static final int ID_PHOTO_MANAGER_OPEN = 10;
    private static final int ID_NOTES = 11;
    private static final int ID_NOTES_LIST = 12;
    private static final int ID_MARKER = 13;
    private static final int ID_VERSION = 14;
    private static final int ID_UPDATE_CHECK = 15;
    private static final int ID_UPDATE_TEST = 16;
    private static final int ID_REPO = 17;
    private static final int ID_CRASH = 18;
    private static final int ID_CHAT_MENU = 19;
    private static final int ID_PROFILE_MENU_GROUPED = 21;
    private static final int ID_CHAT_MENU_GROUPED = 22;
    private static final int ID_MEMBER_CHECK = 23;
    private static final int ID_MEMBER_HINT = 24;
    private static final int ID_MEMBER_INTERVAL = 25;

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.SparkSettings);
    }

    private static UItem check(int id, int text, String key) {
        return UItem.asCheck(id, SparkUi.mark(getString(text))).setChecked(SparkConfig.getRaw(key));
    }

    private static UItem subCheck(int id, int text, String key) {
        return UItem.asCheck(id, "      " + getString(text)).setChecked(SparkConfig.getRaw(key));
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        final boolean on = SparkConfig.getRaw(SparkConfig.MASTER);
        items.add(UItem.asRippleCheck(ID_MASTER, getString(R.string.SparkMaster)).setChecked(on));
        items.add(UItem.asShadow(getString(on ? R.string.SparkMasterInfo : R.string.SparkMasterOffInfo)));

        final int start = items.size();

        items.add(UItem.asHeader(getString(R.string.SparkHeaderProfile)));
        items.add(check(ID_COPY_ID, R.string.SparkCopyIdToggle, SparkConfig.PROFILE_COPY_ID));
        items.add(check(ID_INFO, R.string.SparkInfoToggle, SparkConfig.PROFILE_INFO));
        items.add(check(ID_CLONE, R.string.SparkCloneToggle, SparkConfig.PROFILE_CLONE));
        if (SparkConfig.getRaw(SparkConfig.PROFILE_CLONE)) {
            items.add(subCheck(ID_CLONE_NAME, R.string.SparkCloneName, SparkConfig.CLONE_NAME));
            items.add(subCheck(ID_CLONE_BIO, R.string.SparkCloneBio, SparkConfig.CLONE_BIO));
            items.add(subCheck(ID_CLONE_PHOTO, R.string.SparkClonePhoto, SparkConfig.CLONE_PHOTO));
            items.add(UItem.asButton(ID_PROFILE_LIBRARY, R.drawable.msg_contacts, getString(R.string.SparkProfileLibrary)));
        }
        items.add(check(ID_PHOTO_MANAGER, R.string.SparkPhotoManagerToggle, SparkConfig.PHOTO_MANAGER));
        if (SparkConfig.getRaw(SparkConfig.PHOTO_MANAGER)) {
            items.add(UItem.asButton(ID_PHOTO_MANAGER_OPEN, R.drawable.msg_photos, getString(R.string.SparkPhotoManager)));
        }
        items.add(UItem.asShadow(getString(R.string.SparkProfileToolsInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderGroups)));
        items.add(check(ID_MEMBER_CHECK, R.string.SparkMemberCheckToggle, SparkConfig.MEMBER_CHECK));
        if (SparkConfig.getRaw(SparkConfig.MEMBER_CHECK)) {
            items.add(subCheck(ID_MEMBER_HINT, R.string.SparkMemberHintToggle, SparkConfig.MEMBER_CHECK_HINT));
            items.add(UItem.asButton(ID_MEMBER_INTERVAL, "      " + getString(R.string.SparkMemberInterval), (org.telegram.messenger.spark.SparkMembership.intervalMs() / 1000) + " s"));
        }
        items.add(UItem.asShadow(getString(R.string.SparkMemberInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkNotes)));
        items.add(check(ID_NOTES, R.string.SparkNotesToggle, SparkConfig.NOTES));
        if (SparkConfig.getRaw(SparkConfig.NOTES)) {
            items.add(UItem.asButton(ID_NOTES_LIST, R.drawable.msg_stories_caption, getString(R.string.SparkNotesAll)));
        }
        items.add(UItem.asShadow(getString(R.string.SparkNotesInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderLook)));
        items.add(check(ID_PROFILE_MENU_GROUPED, R.string.SparkProfileMenuGroupedToggle, SparkConfig.PROFILE_MENU_GROUPED));
        items.add(check(ID_CHAT_MENU, R.string.SparkChatMenuToggle, SparkConfig.CHAT_MENU));
        if (SparkConfig.getRaw(SparkConfig.CHAT_MENU)) {
            items.add(subCheck(ID_CHAT_MENU_GROUPED, R.string.SparkMenuGroupedToggle, SparkConfig.CHAT_MENU_GROUPED));
        }
        items.add(check(ID_MARKER, R.string.SparkMarkerToggle, SparkConfig.SPARK_MARKER));
        items.add(UItem.asShadow(getString(R.string.SparkLookInfo)));

        items.add(UItem.asHeader(getString(R.string.SparkHeaderAccounts)));
        items.add(UItem.asShadow(getString(R.string.SparkAccountsInfo)));

        // Ohne Hauptschalter: alles bis hier ausgrauen
        if (!on) {
            for (int i = start; i < items.size(); i++) {
                items.get(i).setEnabled(false);
            }
        }

        items.add(UItem.asHeader(getString(R.string.SparkHeaderUpdates)));
        items.add(UItem.asButton(ID_VERSION, R.drawable.msg_info, getString(R.string.SparkVersion), SparkUpdater.buildLabel()));
        items.add(UItem.asButton(ID_UPDATE_CHECK, R.drawable.msg_retry, getString(R.string.SparkUpdateCheck)));
        items.add(UItem.asCheck(ID_UPDATE_TEST, getString(R.string.SparkUpdateTestBuilds)).setChecked(SparkConfig.getRaw(SparkConfig.UPDATE_TEST_BUILDS)));
        items.add(UItem.asButton(ID_REPO, R.drawable.msg_link2, getString(R.string.SparkRepo)));
        items.add(UItem.asShadow(getString(R.string.SparkUpdateInfo)));

        if (SparkCrashLog.read(getContext()) != null) {
            items.add(UItem.asButton(ID_CRASH, R.drawable.msg_report, getString(R.string.SparkCrashLog)).red());
            items.add(UItem.asShadow(null));
        }
    }

    private void toggle(String key) {
        SparkConfig.toggle(key);
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (!item.enabled) {
            return;
        }
        switch (item.id) {
            case ID_MASTER: toggle(SparkConfig.MASTER); break;
            case ID_COPY_ID: toggle(SparkConfig.PROFILE_COPY_ID); break;
            case ID_INFO: toggle(SparkConfig.PROFILE_INFO); break;
            case ID_CLONE: toggle(SparkConfig.PROFILE_CLONE); break;
            case ID_CLONE_NAME: toggle(SparkConfig.CLONE_NAME); break;
            case ID_CLONE_BIO: toggle(SparkConfig.CLONE_BIO); break;
            case ID_CLONE_PHOTO: toggle(SparkConfig.CLONE_PHOTO); break;
            case ID_PHOTO_MANAGER: toggle(SparkConfig.PHOTO_MANAGER); break;
            case ID_NOTES: toggle(SparkConfig.NOTES); break;
            case ID_MARKER: toggle(SparkConfig.SPARK_MARKER); break;
            case ID_CHAT_MENU: toggle(SparkConfig.CHAT_MENU); break;
            case ID_PROFILE_MENU_GROUPED: toggle(SparkConfig.PROFILE_MENU_GROUPED); break;
            case ID_CHAT_MENU_GROUPED: toggle(SparkConfig.CHAT_MENU_GROUPED); break;
            case ID_MEMBER_CHECK: toggle(SparkConfig.MEMBER_CHECK); break;
            case ID_MEMBER_HINT: toggle(SparkConfig.MEMBER_CHECK_HINT); break;
            case ID_MEMBER_INTERVAL: {
                int index = SparkConfig.prefs().getInt("memberCheckInterval", 0);
                index = (index + 1) % org.telegram.messenger.spark.SparkMembership.INTERVALS.length;
                SparkConfig.prefs().edit().putInt("memberCheckInterval", index).apply();
                listView.adapter.update(true);
                break;
            }
            case ID_UPDATE_TEST: toggle(SparkConfig.UPDATE_TEST_BUILDS); break;
            case ID_PROFILE_LIBRARY: presentFragment(new SparkProfileLibraryActivity()); break;
            case ID_PHOTO_MANAGER_OPEN: presentFragment(new SparkPhotoManagerActivity()); break;
            case ID_NOTES_LIST: presentFragment(new SparkNotesActivity()); break;
            case ID_UPDATE_CHECK: checkForUpdate(); break;
            case ID_REPO: Browser.openUrl(getParentActivity(), SparkUpdater.REPO_URL); break;
            case ID_VERSION: {
                String text = "Sparkgram " + SparkUpdater.buildLabel() + "\nTelegram-Basis " + org.telegram.messenger.BuildConfig.BUILD_VERSION_STRING + "\nCommit " + org.telegram.messenger.BuildConfig.SPARK_COMMIT;
                AndroidUtilities.addToClipboard(text);
                BulletinFactory.of(this).createCopyBulletin(getString(R.string.TextCopied)).show();
                break;
            }
            case ID_CRASH: {
                final String log = SparkCrashLog.read(getContext());
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(getString(R.string.SparkCrashLog));
                builder.setMessage(log == null ? "" : (log.length() > 3000 ? log.substring(0, 3000) + "…" : log));
                builder.setPositiveButton(getString(R.string.Copy), (d, w) -> {
                    AndroidUtilities.addToClipboard(log);
                    BulletinFactory.of(this).createCopyBulletin(getString(R.string.TextCopied)).show();
                });
                builder.setNeutralButton(getString(R.string.Delete), (d, w) -> {
                    SparkCrashLog.clear(getContext());
                    listView.adapter.update(true);
                });
                builder.setNegativeButton(getString(R.string.Close), null);
                showDialog(builder.create());
                break;
            }
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

package org.telegram.ui.spark;

import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.R;
import org.telegram.messenger.spark.SparkBulkDelete;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

/** Listet alle eigenen Nachrichten eines Chats auf; Auswahl loeschen. */
public class SparkOwnMessagesActivity extends UniversalFragment {

    private static final int MENU_ALL = 1;
    private static final int MENU_DELETE = 2;
    private static final int ID_BASE = 10000;

    private final long dialogId;
    private final ArrayList<TLRPC.Message> messages = new ArrayList<>();
    private final HashSet<Integer> selected = new HashSet<>();
    private boolean loading = true;
    private int foundCount;
    private SparkBulkDelete job;
    private final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.GERMANY);

    public SparkOwnMessagesActivity(long dialogId) {
        this.dialogId = dialogId;
    }

    @Override
    public View createView(android.content.Context context) {
        View v = super.createView(context);
        actionBar.createMenu().addItem(MENU_ALL, R.drawable.msg_select);
        actionBar.createMenu().addItem(MENU_DELETE, R.drawable.msg_delete);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_ALL) {
                    if (selected.size() == messages.size()) {
                        selected.clear();
                    } else {
                        for (TLRPC.Message m : messages) selected.add(m.id);
                    }
                    update();
                } else if (id == MENU_DELETE) {
                    confirmDelete();
                }
            }
        });
        job = new SparkBulkDelete(currentAccount, dialogId, new SparkBulkDelete.Listener() {
            @Override
            public void onProgress(int found, int deleted, boolean searching) {
                foundCount = found;
                update();
            }

            @Override
            public void onDone(int deleted, String error) {
                loading = false;
                update();
            }

            @Override
            public void onCollected(ArrayList<TLRPC.Message> list) {
                messages.clear();
                messages.addAll(list);
                loading = false;
                update();
            }
        }).collectOnly();
        job.start();
        return v;
    }

    @Override
    public void onFragmentDestroy() {
        if (job != null) job.cancel();
        super.onFragmentDestroy();
    }

    private void update() {
        if (actionBar != null) {
            actionBar.setTitle(selected.isEmpty() ? getTitle() : selected.size() + " von " + messages.size() + " ausgewählt");
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    @Override
    protected CharSequence getTitle() {
        return "Eigene Nachrichten";
    }

    private static String preview(TLRPC.Message m) {
        String t = m.message;
        if (TextUtils.isEmpty(t)) {
            if (m.media instanceof TLRPC.TL_messageMediaPhoto) t = "[Foto]";
            else if (m.media instanceof TLRPC.TL_messageMediaDocument) t = "[Datei/Sticker/Sprachnachricht]";
            else if (m.media != null) t = "[Medien]";
            else t = "(leer)";
        }
        t = t.replace('\n', ' ');
        return t.length() > 60 ? t.substring(0, 60) + "…" : t;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (loading) {
            items.add(UItem.asShadow("Suche läuft … " + foundCount + " gefunden"));
            return;
        }
        if (messages.isEmpty()) {
            items.add(UItem.asShadow("Keine eigenen Nachrichten in diesem Chat gefunden."));
            return;
        }
        items.add(UItem.asHeader(messages.size() + " eigene Nachrichten · antippen zum Auswählen"));
        for (int i = 0; i < messages.size(); i++) {
            TLRPC.Message m = messages.get(i);
            items.add(UItem.asCheck(ID_BASE + i, format.format(new Date(m.date * 1000L)) + " · " + preview(m)).setChecked(selected.contains(m.id)));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        int i = item.id - ID_BASE;
        if (i < 0 || i >= messages.size()) return;
        int mid = messages.get(i).id;
        if (!selected.remove(mid)) selected.add(mid);
        update();
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private void confirmDelete() {
        if (selected.isEmpty()) {
            BulletinFactory.of(this).createErrorBulletin("Tippe Nachrichten an, um sie auszuwählen").show();
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Für alle löschen?");
        b.setMessage(selected.size() + " Nachricht(en) werden für alle gelöscht. Das läuft gebremst im Hintergrund.");
        b.setPositiveButton(org.telegram.messenger.LocaleController.getString(R.string.Delete), (d, w) -> {
            final AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
            progress.setCanCancel(true);
            final ArrayList<Integer> chosen = new ArrayList<>(selected);
            final SparkBulkDelete del = new SparkBulkDelete(currentAccount, dialogId, new SparkBulkDelete.Listener() {
                @Override
                public void onProgress(int found, int deleted, boolean searching) {
                    try { progress.setMessage("Lösche … " + deleted + " / " + chosen.size()); } catch (Exception ignore) {}
                }

                @Override
                public void onDone(int deleted, String error) {
                    try { progress.dismiss(); } catch (Exception ignore) {}
                    HashSet<Integer> gone = new HashSet<>(chosen);
                    for (int k = messages.size() - 1; k >= 0; k--) {
                        if (gone.contains(messages.get(k).id)) messages.remove(k);
                    }
                    selected.clear();
                    update();
                    BulletinFactory.of(SparkOwnMessagesActivity.this).createSimpleBulletin(R.raw.contact_check, deleted + " Nachrichten gelöscht").show();
                }
            });
            progress.setOnCancelListener(di -> del.cancel());
            progress.show();
            del.deleteIds(chosen);
        });
        b.setNegativeButton(org.telegram.messenger.LocaleController.getString(R.string.Cancel), null);
        showDialog(b.create());
    }
}

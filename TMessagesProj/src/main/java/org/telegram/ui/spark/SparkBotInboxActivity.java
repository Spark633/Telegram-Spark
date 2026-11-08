package org.telegram.ui.spark;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.spark.SparkBot;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/** Bot-Postfach: Nachrichten, die Leute an deinen Bot schicken – antippen = über den Bot antworten. */
public class SparkBotInboxActivity extends UniversalFragment {

    private static final int ID_REFRESH = 1;
    private static final int ID_CLEAR = 2;
    private static final int ID_BASE = 1000;

    private ArrayList<SparkBot.Incoming> list = new ArrayList<>();
    private String error;
    private boolean loading;
    private final Runnable autoRefresh = this::refresh;

    @Override
    protected CharSequence getTitle() {
        return "Bot-Postfach" + (SparkBot.botName() != null ? " · " + SparkBot.botName() : "");
    }

    @Override
    public void onResume() {
        super.onResume();
        list = SparkBot.inbox();
        refresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        AndroidUtilities.cancelRunOnUIThread(autoRefresh);
    }

    private void refresh() {
        AndroidUtilities.cancelRunOnUIThread(autoRefresh);
        if (loading) return;
        loading = true;
        Utilities.globalQueue.postRunnable(() -> {
            String err = SparkBot.fetch();
            AndroidUtilities.runOnUIThread(() -> {
                loading = false;
                error = err;
                list = SparkBot.inbox();
                if (listView != null) listView.adapter.update(true);
                if (!isPaused) AndroidUtilities.runOnUIThread(autoRefresh, 10000);
            });
        });
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asButton(ID_REFRESH, R.drawable.msg_retry, loading ? "Lädt …" : "Aktualisieren"));
        if (error != null) items.add(UItem.asShadow(error));
        SimpleDateFormat f = new SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY);
        for (int i = 0; i < list.size(); i++) {
            SparkBot.Incoming in = list.get(i);
            String who = in.fromName.isEmpty() ? in.chatTitle : in.fromName + (in.fromUsername.isEmpty() ? "" : " @" + in.fromUsername);
            String where = in.chatTitle.isEmpty() ? "privat" : in.chatTitle;
            items.add(UItem.asButton(ID_BASE + i, who + " · " + where, f.format(new Date(in.date * 1000L))));
            items.add(UItem.asShadow(in.text.length() > 300 ? in.text.substring(0, 300) + " …" : in.text));
        }
        if (list.isEmpty() && error == null) items.add(UItem.asShadow("Noch keine Nachrichten an den Bot."));
        if (!list.isEmpty()) items.add(UItem.asButton(ID_CLEAR, R.drawable.msg_delete, "Postfach leeren").red());
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_REFRESH) {
            refresh();
            listView.adapter.update(true);
        } else if (item.id == ID_CLEAR) {
            SparkBot.clearInbox();
            list = SparkBot.inbox();
            listView.adapter.update(true);
        } else if (item.id >= ID_BASE && item.id - ID_BASE < list.size()) {
            reply(list.get(item.id - ID_BASE));
        }
    }

    private void reply(SparkBot.Incoming in) {
        final EditText edit = new EditText(getParentActivity());
        edit.setHint("Antwort");
        edit.setMinLines(2);
        edit.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        edit.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        FrameLayout box = new FrameLayout(getParentActivity());
        box.addView(edit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 24, 0, 24, 0));
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(SparkUi.mark("Als Bot antworten"));
        b.setMessage(in.text.length() > 200 ? in.text.substring(0, 200) + " …" : in.text);
        b.setView(box);
        b.setPositiveButton(getString(R.string.Send), (d, w) -> {
            String text = edit.getText().toString().trim();
            if (text.isEmpty()) return;
            Utilities.globalQueue.postRunnable(() -> {
                SparkBot.Result r = SparkBot.sendRaw(in.chatId, text, in.messageId);
                AndroidUtilities.runOnUIThread(() -> {
                    if (r.ok) BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "✦ Gesendet").show();
                    else BulletinFactory.of(this).createErrorBulletin(r.error == null ? "Fehler" : r.error).show();
                });
            });
        });
        b.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(b.create());
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}

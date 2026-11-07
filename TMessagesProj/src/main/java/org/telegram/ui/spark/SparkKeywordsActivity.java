package org.telegram.ui.spark;

import static org.telegram.messenger.LocaleController.getString;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.spark.SparkKeywords;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/** Schlagwoerter verwalten und Treffer ansehen (Tippen springt zur Nachricht). */
public class SparkKeywordsActivity extends UniversalFragment {

    private static final int ID_ADD = 1;
    private static final int ID_CLEAR = 2;
    private static final int ID_WORD = 1000;
    private static final int ID_HIT = 100000;

    private ArrayList<String> words = new ArrayList<>();
    private ArrayList<SparkKeywords.Hit> hits = new ArrayList<>();
    private final SimpleDateFormat df = new SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY);

    @Override
    public boolean onFragmentCreate() {
        reload();
        return super.onFragmentCreate();
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
        if (listView != null && listView.adapter != null) listView.adapter.update(true);
    }

    private void reload() {
        words = SparkKeywords.keywords(currentAccount);
        hits = SparkKeywords.hits(currentAccount);
    }

    @Override
    protected CharSequence getTitle() {
        return "Schlagwort-Alarm";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader("Schlagwörter"));
        for (int i = 0; i < words.size(); i++) {
            items.add(UItem.asButton(ID_WORD + i, R.drawable.msg_delete, words.get(i)));
        }
        items.add(UItem.asButton(ID_ADD, R.drawable.msg_add, "Schlagwort hinzufügen").accent());
        items.add(UItem.asShadow("Enthält eine eingehende Nachricht eines der Wörter, bekommst du eine Benachrichtigung und der Treffer landet unten. Groß-/Kleinschreibung egal. Antippen eines Worts entfernt es."));
        items.add(UItem.asHeader("Treffer (" + hits.size() + ")"));
        if (hits.isEmpty()) {
            items.add(UItem.asShadow("Noch keine Treffer."));
            return;
        }
        for (int i = 0; i < hits.size(); i++) {
            SparkKeywords.Hit h = hits.get(i);
            String chat = SparkNotesActivity.dialogName(currentAccount, h.dialogId);
            String sender = h.senderId != h.dialogId ? SparkNotesActivity.dialogName(currentAccount, h.senderId) + ": " : "";
            String text = h.text.replace('\n', ' ');
            if (text.length() > 50) text = text.substring(0, 50) + "…";
            items.add(UItem.asButton(ID_HIT + i, "„" + h.keyword + "“ · " + chat, df.format(new Date(h.date * 1000L)) + " · " + sender + text));
        }
        items.add(UItem.asButton(ID_CLEAR, R.drawable.msg_delete, "Treffer leeren").red());
        items.add(UItem.asShadow("Antippen springt zur Nachricht im Chat."));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_ADD) {
            addWord();
        } else if (item.id == ID_CLEAR) {
            SparkKeywords.clearHits(currentAccount);
            reload();
            listView.adapter.update(true);
        } else if (item.id >= ID_HIT) {
            int i = item.id - ID_HIT;
            if (i < hits.size()) {
                SparkKeywords.Hit h = hits.get(i);
                Bundle args = new Bundle();
                if (h.dialogId > 0) args.putLong("user_id", h.dialogId); else args.putLong("chat_id", -h.dialogId);
                args.putInt("message_id", h.messageId);
                presentFragment(new ChatActivity(args));
            }
        } else if (item.id >= ID_WORD) {
            int i = item.id - ID_WORD;
            if (i < words.size()) {
                words.remove(i);
                SparkKeywords.setKeywords(currentAccount, words);
                listView.adapter.update(true);
            }
        }
    }

    private void addWord() {
        final EditText input = new EditText(getParentActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("z. B. Skywalker");
        input.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        input.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        FrameLayout box = new FrameLayout(getParentActivity());
        box.addView(input, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 24, 0, 24, 0));
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Schlagwort hinzufügen");
        b.setView(box);
        b.setPositiveButton(getString(R.string.Save), (d, w) -> {
            String t = input.getText().toString().trim();
            if (!TextUtils.isEmpty(t) && !words.contains(t)) {
                words.add(t);
                SparkKeywords.setKeywords(currentAccount, words);
                listView.adapter.update(true);
            }
        });
        b.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(b.create());
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

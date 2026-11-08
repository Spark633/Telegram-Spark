package org.telegram.ui.spark;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.telegram.messenger.R;
import org.telegram.messenger.spark.SparkBookmarks;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
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

/** Lesezeichen: antippen = zur Nachricht springen, lange druecken = Schlagwoerter / loeschen. */
public class SparkBookmarksActivity extends UniversalFragment {

    private static final int ID_FILTER = 1;
    private static final int ID_BASE = 1000;

    private String filter;
    private ArrayList<SparkBookmarks.Item> shown = new ArrayList<>();

    @Override
    protected CharSequence getTitle() {
        return "Lesezeichen";
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null) listView.adapter.update(true);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        shown.clear();
        for (SparkBookmarks.Item it : SparkBookmarks.list(currentAccount)) {
            if (filter == null || it.tags.contains(filter)) shown.add(it);
        }
        items.add(UItem.asButton(ID_FILTER, R.drawable.msg_folders, "Schlagwort", filter == null ? "alle" : label(filter)));
        if (shown.isEmpty()) {
            items.add(UItem.asShadow("Noch keine Lesezeichen. Nachricht antippen → „✦ Lesezeichen“."));
            return;
        }
        SimpleDateFormat f = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.GERMANY);
        for (int i = 0; i < shown.size(); i++) {
            SparkBookmarks.Item it = shown.get(i);
            StringBuilder tags = new StringBuilder();
            for (String t : it.tags) tags.append(label(t)).append(' ');
            String chat = SparkNotesActivity.dialogName(currentAccount, it.dialogId);
            items.add(UItem.asButton(ID_BASE + i, (tags.length() > 0 ? tags.toString() + "· " : "") + chat, f.format(new Date(it.date * 1000L))));
            String text = TextUtils.isEmpty(it.text) ? "[Medium]" : it.text;
            items.add(UItem.asShadow(text.length() > 200 ? text.substring(0, 200) + " …" : text));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_FILTER) {
            ArrayList<String> tags = SparkBookmarks.allTags(currentAccount);
            String[] names = new String[tags.size() + 1];
            names[0] = "Alle";
            for (int i = 0; i < tags.size(); i++) names[i + 1] = label(tags.get(i));
            AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
            b.setTitle(SparkUi.mark("Schlagwort"));
            b.setItems(names, (d, w) -> {
                filter = w == 0 ? null : tags.get(w - 1);
                listView.adapter.update(true);
            });
            showDialog(b.create());
            return;
        }
        int i = item.id - ID_BASE;
        if (i < 0 || i >= shown.size()) return;
        SparkBookmarks.Item it = shown.get(i);
        Bundle args = new Bundle();
        if (it.dialogId > 0) args.putLong("user_id", it.dialogId); else args.putLong("chat_id", -it.dialogId);
        args.putInt("message_id", it.messageId);
        presentFragment(new ChatActivity(args));
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        int i = item.id - ID_BASE;
        if (i < 0 || i >= shown.size()) return false;
        SparkBookmarks.Item it = shown.get(i);
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(SparkUi.mark("Lesezeichen"));
        b.setItems(new CharSequence[]{"Schlagwörter ändern", "Löschen"}, (d, w) -> {
            if (w == 0) {
                editTags(this, it.tags, tags -> {
                    SparkBookmarks.setTags(currentAccount, it.dialogId, it.messageId, tags);
                    listView.adapter.update(true);
                });
            } else {
                SparkBookmarks.remove(currentAccount, it.dialogId, it.messageId);
                listView.adapter.update(true);
            }
        });
        showDialog(b.create());
        return true;
    }

    /** Schlagwort zur Anzeige: Woerter mit "#", Emojis ohne. */
    public static String label(String tag) {
        for (int i = 0; i < tag.length(); i++) {
            if (Character.isLetterOrDigit(tag.charAt(i))) return "#" + tag;
        }
        return tag;
    }

    /** Eingabe der Schlagwoerter (mit Vorschlaegen). */
    public static void editTags(BaseFragment fragment, ArrayList<String> current, org.telegram.messenger.Utilities.Callback<ArrayList<String>> done) {
        final EditText edit = new EditText(fragment.getParentActivity());
        StringBuilder sb = new StringBuilder();
        if (current != null) for (String t : current) sb.append(label(t)).append(' ');
        edit.setText(sb.toString().trim());
        edit.setSelection(edit.length());
        edit.setHint("#rp #wichtig ⭐ …");
        edit.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        edit.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        FrameLayout box = new FrameLayout(fragment.getParentActivity());
        box.addView(edit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 24, 0, 24, 0));
        ArrayList<String> known = SparkBookmarks.allTags(fragment.getCurrentAccount());
        AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity());
        b.setTitle(SparkUi.mark("Schlagwörter"));
        if (!known.isEmpty()) {
            StringBuilder k = new StringBuilder("Bisher: ");
            for (int i = 0; i < Math.min(10, known.size()); i++) k.append(label(known.get(i))).append(' ');
            b.setMessage(k.toString().trim());
        }
        b.setView(box);
        b.setPositiveButton("Speichern", (d, w) -> done.run(SparkBookmarks.parseTags(edit.getText().toString())));
        b.setNegativeButton("Abbrechen", null);
        fragment.showDialog(b.create());
    }
}

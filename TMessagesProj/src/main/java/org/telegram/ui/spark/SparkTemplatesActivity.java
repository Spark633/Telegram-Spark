package org.telegram.ui.spark;

import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.telegram.messenger.R;
import org.telegram.messenger.spark.SparkTemplates;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** Textvorlagen verwalten: antippen = bearbeiten, lange druecken = loeschen. */
public class SparkTemplatesActivity extends UniversalFragment {

    private static final int ID_ADD = 1;
    private static final int ID_BASE = 1000;

    @Override
    protected CharSequence getTitle() {
        return "Textvorlagen";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        ArrayList<String> list = SparkTemplates.list();
        items.add(UItem.asButton(ID_ADD, R.drawable.msg_add, "Neue Vorlage").accent());
        for (int i = 0; i < list.size(); i++) {
            String t = list.get(i);
            items.add(UItem.asButton(ID_BASE + i, t.length() > 60 ? t.substring(0, 60) + " …" : t.replace('\n', ' ')));
        }
        items.add(UItem.asShadow("Im Chat-Menü „✦ Textvorlagen“ fügst du eine Vorlage ins Eingabefeld ein. Antippen = bearbeiten, lange drücken = löschen. Gilt für alle Konten."));
    }

    /** Eingabedialog fuer eine Vorlage (index -1 = neu). */
    public static void edit(BaseFragment fragment, int index, Runnable done) {
        ArrayList<String> list = SparkTemplates.list();
        final EditText edit = new EditText(fragment.getParentActivity());
        edit.setHint("Text der Vorlage");
        edit.setMinLines(3);
        if (index >= 0 && index < list.size()) edit.setText(list.get(index));
        edit.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        edit.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        FrameLayout box = new FrameLayout(fragment.getParentActivity());
        box.addView(edit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 24, 0, 24, 0));
        AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity());
        b.setTitle(SparkUi.mark(index < 0 ? "Neue Vorlage" : "Vorlage bearbeiten"));
        b.setView(box);
        b.setPositiveButton("Speichern", (d, w) -> {
            String text = edit.getText().toString();
            if (text.trim().isEmpty()) return;
            ArrayList<String> l = SparkTemplates.list();
            if (index >= 0 && index < l.size()) l.set(index, text); else l.add(text);
            SparkTemplates.save(l);
            if (done != null) done.run();
        });
        b.setNegativeButton("Abbrechen", null);
        fragment.showDialog(b.create());
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_ADD) {
            edit(this, -1, () -> listView.adapter.update(true));
        } else if (item.id >= ID_BASE) {
            edit(this, item.id - ID_BASE, () -> listView.adapter.update(true));
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item.id < ID_BASE) return false;
        final int i = item.id - ID_BASE;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Vorlage löschen?");
        b.setPositiveButton("Löschen", (d, w) -> {
            ArrayList<String> l = SparkTemplates.list();
            if (i < l.size()) l.remove(i);
            SparkTemplates.save(l);
            listView.adapter.update(true);
        });
        b.setNegativeButton("Abbrechen", null);
        showDialog(b.create());
        return true;
    }
}

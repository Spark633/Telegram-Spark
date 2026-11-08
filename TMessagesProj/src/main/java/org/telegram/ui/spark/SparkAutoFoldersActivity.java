package org.telegram.ui.spark;

import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.spark.SparkAutoFolders;
import org.telegram.messenger.spark.SparkConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** Auto-Ordner: Regeln "Name enthaelt … -> Ordner …" verwalten und anwenden. */
public class SparkAutoFoldersActivity extends UniversalFragment {

    private static final int ID_SWITCH = 1;
    private static final int ID_ADD = 2;
    private static final int ID_APPLY = 3;
    private static final int ID_BASE = 1000;

    @Override
    protected CharSequence getTitle() {
        return "Auto-Ordner";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asCheck(ID_SWITCH, SparkUi.mark("Auto-Ordner aktiv")).setChecked(SparkConfig.getRaw(SparkConfig.AUTO_FOLDERS)));
        items.add(UItem.asShadow("Chats, deren Name ein bestimmtes Wort enthält, landen automatisch in einem deiner Telegram-Ordner – auch neue Chats. Geprüft wird beim Öffnen der Chatliste (höchstens alle 5 Minuten) oder mit „Jetzt anwenden“. Pro Ordner erlaubt Telegram höchstens " + SparkAutoFolders.MAX_PER_FOLDER + " fest eingetragene Chats."));
        items.add(UItem.asHeader("Regeln"));
        MessagesController mc = getMessagesController();
        ArrayList<SparkAutoFolders.Rule> rules = SparkAutoFolders.rules(currentAccount);
        for (int i = 0; i < rules.size(); i++) {
            SparkAutoFolders.Rule r = rules.get(i);
            MessagesController.DialogFilter f = SparkAutoFolders.folder(mc, r.folderId);
            int count = 0;
            for (TLRPC.Dialog d : mc.getAllDialogs()) {
                if (SparkAutoFolders.matches(r, SparkAutoFolders.dialogName(mc, d.id))) count++;
            }
            items.add(UItem.asButton(ID_BASE + i, R.drawable.msg_folders, "„" + r.word + "“ → " + (f != null ? f.name : "(Ordner gelöscht)"), count + " Chats"));
        }
        items.add(UItem.asButton(ID_ADD, R.drawable.msg_add, "Regel hinzufügen").accent());
        if (!rules.isEmpty()) {
            items.add(UItem.asButton(ID_APPLY, R.drawable.msg_retry, "Jetzt anwenden"));
        }
        items.add(UItem.asShadow("Lange drücken auf eine Regel = löschen. Bereits einsortierte Chats bleiben im Ordner."));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_SWITCH) {
            SparkConfig.toggle(SparkConfig.AUTO_FOLDERS);
            listView.adapter.update(true);
        } else if (item.id == ID_ADD) {
            addRule();
        } else if (item.id == ID_APPLY) {
            if (!SparkConfig.get(SparkConfig.AUTO_FOLDERS)) {
                BulletinFactory.of(this).createErrorBulletin("Auto-Ordner ist ausgeschaltet").show();
                return;
            }
            int n = SparkAutoFolders.apply(this);
            BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, n == 0 ? "✦ Alles schon einsortiert" : "✦ " + n + " Chat(s) einsortiert").show();
            listView.adapter.update(true);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        int i = item.id - ID_BASE;
        ArrayList<SparkAutoFolders.Rule> rules = SparkAutoFolders.rules(currentAccount);
        if (i < 0 || i >= rules.size()) return false;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(SparkUi.mark("Regel löschen"));
        b.setMessage("Regel „" + rules.get(i).word + "“ löschen?");
        b.setPositiveButton("Löschen", (d, w) -> {
            SparkAutoFolders.remove(currentAccount, i);
            listView.adapter.update(true);
        });
        b.setNegativeButton("Abbrechen", null);
        showDialog(b.create());
        return true;
    }

    private void addRule() {
        final ArrayList<MessagesController.DialogFilter> folders = new ArrayList<>();
        for (MessagesController.DialogFilter f : getMessagesController().getDialogFilters()) {
            if (!f.isDefault() && !f.isChatlist() && f.id != MessagesController.SPARK_LEFT_FILTER_ID) folders.add(f);
        }
        if (folders.isEmpty()) {
            BulletinFactory.of(this).createErrorBulletin("Lege zuerst einen Ordner in Telegram an (Einstellungen → Chat-Ordner)").show();
            return;
        }
        final EditText edit = new EditText(getParentActivity());
        edit.setHint("Wort im Chatnamen, z. B. RP");
        edit.setSingleLine(true);
        edit.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        edit.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        FrameLayout box = new FrameLayout(getParentActivity());
        box.addView(edit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 24, 0, 24, 0));
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(SparkUi.mark("Neue Regel"));
        b.setMessage("Chats, deren Name dieses Wort enthält (Groß-/Kleinschreibung egal):");
        b.setView(box);
        b.setPositiveButton("Weiter", (d, w) -> {
            String word = edit.getText().toString().trim();
            if (TextUtils.isEmpty(word)) return;
            String[] names = new String[folders.size()];
            for (int i = 0; i < folders.size(); i++) names[i] = folders.get(i).name;
            AlertDialog.Builder c = new AlertDialog.Builder(getParentActivity());
            c.setTitle(SparkUi.mark("In welchen Ordner?"));
            c.setItems(names, (d2, w2) -> {
                SparkAutoFolders.add(currentAccount, word, folders.get(w2).id);
                listView.adapter.update(true);
            });
            showDialog(c.create());
        });
        b.setNegativeButton("Abbrechen", null);
        showDialog(b.create());
    }
}

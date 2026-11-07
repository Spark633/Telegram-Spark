package org.telegram.ui.spark;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.spark.SparkCollection;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

/**
 * Sammlung: unbegrenzt Sticker, GIFs und Sprachnachrichten mit eigenen Ordnern.
 * Aus einem Chat geoeffnet: antippen = in diesen Chat senden (als Kopie ohne Absender).
 */
public class SparkCollectionActivity extends BaseFragment {

    private static final int MENU_NEW_FOLDER = 1;

    private final long sendTo;
    private String folder = null; // null = alle
    private int kindFilter = 0;   // 0 = alle Arten
    private LinearLayout chips;
    private LinearLayout kindChips;
    private GridLayout grid;
    private TextView emptyView;

    public SparkCollectionActivity(long sendToDialogId) {
        this.sendTo = sendToDialogId;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Sammlung");
        if (sendTo != 0) {
            actionBar.setSubtitle("Antippen = senden");
        }
        actionBar.createMenu().addItem(MENU_NEW_FOLDER, R.drawable.msg_folders);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_NEW_FOLDER) {
                    newFolder();
                }
            }
        });

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        HorizontalScrollView hs = new HorizontalScrollView(context);
        hs.setHorizontalScrollBarEnabled(false);
        chips = new LinearLayout(context);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(2));
        hs.addView(chips);
        root.addView(hs, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        HorizontalScrollView hs2 = new HorizontalScrollView(context);
        hs2.setHorizontalScrollBarEnabled(false);
        kindChips = new LinearLayout(context);
        kindChips.setOrientation(LinearLayout.HORIZONTAL);
        kindChips.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(2), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
        hs2.addView(kindChips);
        root.addView(hs2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        FrameLayout content = new FrameLayout(context);
        ScrollView scroll = new ScrollView(context);
        grid = new GridLayout(context);
        grid.setColumnCount(4);
        grid.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        scroll.addView(grid, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        emptyView = new TextView(context);
        emptyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        emptyView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setText("Noch leer.\n\nIm Chat eine Nachricht mit Sticker, GIF oder Sprachnachricht antippen → „✦ Medien …“ → „Zur Sammlung hinzufügen“.");
        content.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER, 24, 0, 24, 0));
        root.addView(content, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f));

        fragmentView = root;
        rebuild();
        return fragmentView;
    }

    private TextView chip(Context context, String text, boolean active, View.OnClickListener click, View.OnLongClickListener longClick) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        t.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
        t.setTextColor(active ? Theme.getColor(Theme.key_featuredStickers_buttonText) : Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        t.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), active ? Theme.getColor(Theme.key_featuredStickers_addButton) : Theme.getColor(Theme.key_windowBackgroundGray)));
        t.setOnClickListener(click);
        if (longClick != null) t.setOnLongClickListener(longClick);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = AndroidUtilities.dp(6);
        t.setLayoutParams(lp);
        return t;
    }

    private void rebuild() {
        if (grid == null) return;
        Context context = getParentActivity();
        if (context == null) return;
        SparkCollection c = SparkCollection.get(currentAccount);
        chips.removeAllViews();
        chips.addView(chip(context, "Alle", folder == null, v -> { folder = null; rebuild(); }, null));
        chips.addView(chip(context, "Ohne Ordner", "".equals(folder), v -> { folder = ""; rebuild(); }, null));
        for (String f : c.folders()) {
            chips.addView(chip(context, "📁 " + f, f.equals(folder), v -> { folder = f; rebuild(); }, v -> { folderMenu(f); return true; }));
        }
        chips.addView(chip(context, "＋", false, v -> newFolder(), null));
        kindChips.removeAllViews();
        int[] kinds = {0, SparkCollection.KIND_STICKER, SparkCollection.KIND_GIF, SparkCollection.KIND_VOICE, SparkCollection.KIND_OTHER};
        for (int k : kinds) {
            kindChips.addView(chip(context, k == 0 ? "Alles" : SparkCollection.kindName(k), kindFilter == k, v -> { kindFilter = k; rebuild(); }, null));
        }

        grid.removeAllViews();
        ArrayList<SparkCollection.Item> items = c.list(folder);
        int size = (AndroidUtilities.displaySize.x - AndroidUtilities.dp(8)) / 4;
        int shown = 0;
        for (SparkCollection.Item it : items) {
            if (kindFilter != 0 && it.kind != kindFilter) continue;
            shown++;
            FrameLayout cell = new FrameLayout(context);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = size;
            lp.height = size;
            cell.setLayoutParams(lp);
            cell.setPadding(AndroidUtilities.dp(3), AndroidUtilities.dp(3), AndroidUtilities.dp(3), AndroidUtilities.dp(3));
            TLRPC.Document doc = it.document();
            if (it.kind == SparkCollection.KIND_VOICE || doc == null) {
                TextView tv = new TextView(context);
                int dur = 0;
                if (doc != null) {
                    for (TLRPC.DocumentAttribute a : doc.attributes) {
                        if (a instanceof TLRPC.TL_documentAttributeAudio) dur = (int) a.duration;
                    }
                }
                tv.setText(it.kind == SparkCollection.KIND_VOICE ? "🎤\n" + (dur / 60) + ":" + String.format("%02d", dur % 60) : "📎\n" + (doc != null ? FileLoader.getDocumentFileName(doc) : "Medium"));
                tv.setGravity(Gravity.CENTER);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                tv.setMaxLines(3);
                tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                tv.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10), Theme.getColor(Theme.key_windowBackgroundGray)));
                cell.addView(tv, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            } else {
                BackupImageView img = new BackupImageView(context);
                img.setAspectFit(true);
                TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(doc.thumbs, 320);
                if (it.kind == SparkCollection.KIND_STICKER) {
                    img.setImage(ImageLocation.getForDocument(doc), "80_80", ImageLocation.getForDocument(thumb, doc), "80_80", 0, it.message);
                } else {
                    img.setImage(ImageLocation.getForDocument(thumb, doc), "80_80", null, null, 0, it.message);
                }
                cell.addView(img, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            }
            cell.setOnClickListener(v -> {
                if (sendTo != 0) {
                    send(it, sendTo);
                } else {
                    itemMenu(it);
                }
            });
            cell.setOnLongClickListener(v -> {
                itemMenu(it);
                return true;
            });
            grid.addView(cell);
        }
        emptyView.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
    }

    private void send(SparkCollection.Item it, long dialogId) {
        ArrayList<MessageObject> list = new ArrayList<>();
        list.add(new MessageObject(currentAccount, it.message, false, false));
        SendMessagesHelper.getInstance(currentAccount).sendMessage(list, dialogId, true, false, true, 0, 0);
        finishFragment();
    }

    private void itemMenu(SparkCollection.Item it) {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Runnable> actions = new ArrayList<>();
        if (sendTo != 0) {
            names.add("Hier senden");
            actions.add(() -> send(it, sendTo));
        }
        names.add("An … senden");
        actions.add(() -> SparkMedia.sendCopyTo(this, new MessageObject(currentAccount, it.message, false, false)));
        names.add("In Ordner verschieben");
        actions.add(() -> moveDialog(it));
        names.add("Aus der Sammlung entfernen");
        actions.add(() -> {
            SparkCollection.get(currentAccount).remove(it.id);
            rebuild();
        });
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(SparkUi.mark(SparkCollection.kindName(it.kind)));
        b.setItems(names.toArray(new CharSequence[0]), (d, w) -> actions.get(w).run());
        showDialog(b.create());
    }

    private void moveDialog(SparkCollection.Item it) {
        ArrayList<String> folders = SparkCollection.get(currentAccount).folders();
        ArrayList<String> names = new ArrayList<>();
        names.add("Ohne Ordner");
        names.addAll(folders);
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(SparkUi.mark("Verschieben nach"));
        b.setItems(names.toArray(new CharSequence[0]), (d, w) -> {
            SparkCollection.get(currentAccount).move(it.id, w == 0 ? "" : folders.get(w - 1));
            rebuild();
        });
        showDialog(b.create());
    }

    private void folderMenu(String f) {
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(SparkUi.mark("Ordner „" + f + "“"));
        b.setMessage("Ordner löschen? Die Einträge bleiben erhalten (ohne Ordner).");
        b.setPositiveButton("Löschen", (d, w) -> {
            SparkCollection.get(currentAccount).removeFolder(f);
            if (f.equals(folder)) folder = null;
            rebuild();
        });
        b.setNegativeButton("Abbrechen", null);
        showDialog(b.create());
    }

    private void newFolder() {
        final EditText edit = new EditText(getParentActivity());
        edit.setHint("Name");
        edit.setSingleLine(true);
        edit.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        edit.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        FrameLayout box = new FrameLayout(getParentActivity());
        box.addView(edit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 24, 0, 24, 0));
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(SparkUi.mark("Neuer Ordner"));
        b.setView(box);
        b.setPositiveButton("Anlegen", (d, w) -> {
            String name = edit.getText().toString().trim();
            if (TextUtils.isEmpty(name)) return;
            SparkCollection.get(currentAccount).addFolder(name);
            folder = name;
            rebuild();
        });
        b.setNegativeButton("Abbrechen", null);
        showDialog(b.create());
    }

    @Override
    public void onResume() {
        super.onResume();
        rebuild();
    }
}

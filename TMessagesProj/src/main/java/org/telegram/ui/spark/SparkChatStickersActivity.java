package org.telegram.ui.spark;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.spark.SparkCollection;
import org.telegram.messenger.spark.SparkConfig;
import org.telegram.messenger.spark.SparkRateLimit;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.StickersAlert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

/** Alle Sticker, die in einem Chat verwendet wurden – als Raster (aus dem Verlauf, gebremst). */
public class SparkChatStickersActivity extends BaseFragment {

    private static final int PAGES_PER_STEP = 10;
    private static final String FEATURE = "historyScan";
    private static final int PER_HOUR = 400;

    private static class Sticker {
        TLRPC.Document doc;
        TLRPC.Message message;
        TLRPC.InputStickerSet set;
        int count;
    }

    private final long dialogId;
    private final LinkedHashMap<Long, Sticker> stickers = new LinkedHashMap<>();
    private int offsetId;
    private int scanned;
    private boolean loading, endReached;
    private String error;

    private TextView statusView;
    private TextView moreButton;
    private GridLayout grid;
    private int shownCount;

    public SparkChatStickersActivity(long dialogId) {
        this.dialogId = dialogId;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Sticker in diesem Chat");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        statusView = new TextView(context);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        statusView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(4));
        root.addView(statusView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        moreButton = new TextView(context);
        moreButton.setText("Weiter durchsuchen (+1000 Nachrichten)");
        moreButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        moreButton.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
        moreButton.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        moreButton.setOnClickListener(v -> {
            error = null;
            scanStep(PAGES_PER_STEP);
            updateStatus();
        });
        root.addView(moreButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(context);
        grid = new GridLayout(context);
        grid.setColumnCount(5);
        grid.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        scroll.addView(grid, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f));

        fragmentView = root;
        updateStatus();
        rebuildGrid();
        if (scanned == 0 && !loading) {
            scanStep(PAGES_PER_STEP);
        }
        return fragmentView;
    }

    private void updateStatus() {
        if (statusView == null) return;
        String s = scanned + " Nachrichten durchsucht · " + stickers.size() + " verschiedene Sticker" + (loading ? " · suche …" : endReached ? " · Anfang erreicht" : " · pausiert");
        if (error != null) s += "\n" + error;
        s += "\nAntippen = Pack öffnen · lange drücken = mehr";
        statusView.setText(s);
        moreButton.setVisibility(!endReached && !loading ? View.VISIBLE : View.GONE);
    }

    private void rebuildGrid() {
        if (grid == null || getParentActivity() == null) return;
        ArrayList<Sticker> list = new ArrayList<>(stickers.values());
        Collections.sort(list, (a, b) -> b.count - a.count);
        grid.removeAllViews();
        int size = (AndroidUtilities.displaySize.x - AndroidUtilities.dp(8)) / 5;
        Context context = getParentActivity();
        for (Sticker st : list) {
            FrameLayout cell = new FrameLayout(context);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = size;
            lp.height = size;
            cell.setLayoutParams(lp);
            cell.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
            BackupImageView img = new BackupImageView(context);
            img.setAspectFit(true);
            TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(st.doc.thumbs, 90);
            img.setImage(ImageLocation.getForDocument(st.doc), "66_66", ImageLocation.getForDocument(thumb, st.doc), "66_66", 0, st.message);
            cell.addView(img, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            if (st.count > 1) {
                TextView badge = new TextView(context);
                badge.setText(st.count + "×");
                badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
                badge.setTextColor(0xffffffff);
                badge.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
                badge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(6), 0x99000000));
                cell.addView(badge, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.BOTTOM));
            }
            cell.setOnClickListener(v -> {
                if (st.set != null) {
                    showDialog(new StickersAlert(getParentActivity(), this, st.set, null, null, false));
                }
            });
            cell.setOnLongClickListener(v -> {
                stickerMenu(st);
                return true;
            });
            grid.addView(cell);
        }
        shownCount = list.size();
    }

    private void stickerMenu(Sticker st) {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Runnable> actions = new ArrayList<>();
        if (st.set != null) {
            names.add("Pack öffnen");
            actions.add(() -> showDialog(new StickersAlert(getParentActivity(), this, st.set, null, null, false)));
            names.add("Ganzes Pack herunterladen");
            actions.add(() -> SparkMedia.downloadPack(this, st.set));
        }
        if (SparkConfig.get(SparkConfig.COLLECTION)) {
            names.add("Zur Sammlung hinzufügen");
            actions.add(() -> {
                boolean ok = SparkCollection.get(currentAccount).add(new MessageObject(currentAccount, st.message, false, false), "");
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, ok ? "✦ In der Sammlung" : "Ist schon in der Sammlung").show();
            });
        }
        names.add("In eigenes Sticker-Pack übernehmen");
        actions.add(() -> SparkMedia.addToOwnPack(this, st.doc));
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(SparkUi.mark("Sticker · " + st.count + "× benutzt"));
        b.setItems(names.toArray(new CharSequence[0]), (d, w) -> actions.get(w).run());
        showDialog(b.create());
    }

    private void scanStep(int pages) {
        if (endReached || pages <= 0) {
            loading = false;
            updateStatus();
            return;
        }
        long wait = SparkRateLimit.waitTime(FEATURE, PER_HOUR);
        if (wait > 0) {
            loading = false;
            error = "Telegram bremst gerade – bitte in " + Math.max(1, wait / 60000) + " Min. weiter durchsuchen.";
            updateStatus();
            return;
        }
        loading = true;
        updateStatus();
        TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
        req.peer = getMessagesController().getInputPeer(dialogId);
        req.offset_id = offsetId;
        req.limit = 100;
        SparkRateLimit.use(FEATURE);
        getConnectionsManager().sendRequest(req, (response, err) -> AndroidUtilities.runOnUIThread(() -> {
            SparkRateLimit.onError(FEATURE, err);
            if (!(response instanceof TLRPC.messages_Messages)) {
                loading = false;
                error = err != null ? err.text : "Fehler beim Laden";
                updateStatus();
                return;
            }
            TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
            int last = 0;
            boolean changed = false;
            for (TLRPC.Message m : res.messages) {
                last = m.id;
                scanned++;
                TLRPC.Document doc = m.media != null ? m.media.document : null;
                if (doc == null) continue;
                TLRPC.InputStickerSet set = null;
                boolean isSticker = false;
                for (TLRPC.DocumentAttribute a : doc.attributes) {
                    if (a instanceof TLRPC.TL_documentAttributeSticker) {
                        isSticker = true;
                        set = a.stickerset instanceof TLRPC.TL_inputStickerSetEmpty ? null : a.stickerset;
                    }
                }
                if (!isSticker) continue;
                Sticker st = stickers.get(doc.id);
                if (st == null) {
                    st = new Sticker();
                    st.doc = doc;
                    st.message = m;
                    st.set = set;
                    stickers.put(doc.id, st);
                }
                st.count++;
                changed = true;
            }
            if (res.messages.size() < 100 || last <= 0) {
                endReached = true;
            }
            offsetId = last;
            if (changed) rebuildGrid();
            updateStatus();
            AndroidUtilities.runOnUIThread(() -> scanStep(pages - 1), 700);
        }));
    }
}

package org.telegram.ui.spark;

import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.spark.SparkRateLimit;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.StickersAlert;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

/** Alle Sticker-Packs, die in einem Chat verwendet wurden (aus dem Verlauf, gebremst). */
public class SparkChatStickersActivity extends UniversalFragment {

    private static final int ID_MORE = 1;
    private static final int ID_BASE = 1000;
    private static final int PAGES_PER_STEP = 10;
    private static final String FEATURE = "historyScan";
    private static final int PER_HOUR = 400;

    private static class Pack {
        TLRPC.InputStickerSet input;
        long setId;
        String title;
        int count;
        String emoji;
    }

    private final long dialogId;
    private final LinkedHashMap<Long, Pack> packs = new LinkedHashMap<>();
    private final ArrayList<Pack> sorted = new ArrayList<>();
    private int offsetId;
    private int scanned;
    private boolean loading, endReached;
    private String error;

    public SparkChatStickersActivity(long dialogId) {
        this.dialogId = dialogId;
    }

    @Override
    protected CharSequence getTitle() {
        return "Sticker in diesem Chat";
    }

    @Override
    public boolean onFragmentCreate() {
        scanStep(PAGES_PER_STEP);
        return super.onFragmentCreate();
    }

    private void update() {
        sorted.clear();
        sorted.addAll(packs.values());
        Collections.sort(sorted, (a, b) -> b.count - a.count);
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private void scanStep(int pages) {
        if (endReached || pages <= 0) {
            loading = false;
            update();
            return;
        }
        long wait = SparkRateLimit.waitTime(FEATURE, PER_HOUR);
        if (wait > 0) {
            loading = false;
            error = "Telegram bremst gerade – bitte in " + Math.max(1, wait / 60000) + " Min. weiter durchsuchen.";
            update();
            return;
        }
        loading = true;
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
                update();
                return;
            }
            TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
            int last = 0;
            for (TLRPC.Message m : res.messages) {
                last = m.id;
                scanned++;
                TLRPC.Document doc = m.media != null ? m.media.document : null;
                if (doc == null) continue;
                TLRPC.InputStickerSet input = null;
                String emoji = null;
                for (TLRPC.DocumentAttribute a : doc.attributes) {
                    if (a instanceof TLRPC.TL_documentAttributeSticker || a instanceof TLRPC.TL_documentAttributeCustomEmoji) {
                        input = a.stickerset;
                        emoji = a.alt;
                    }
                }
                if (input == null || input instanceof TLRPC.TL_inputStickerSetEmpty) continue;
                long key = input.id != 0 ? input.id : (input.short_name != null ? input.short_name.hashCode() : 0);
                Pack p = packs.get(key);
                if (p == null) {
                    p = new Pack();
                    p.input = input;
                    p.setId = input.id;
                    p.emoji = emoji;
                    TLRPC.TL_messages_stickerSet cached = MediaDataController.getInstance(currentAccount).getStickerSet(input, true);
                    if (cached != null && cached.set != null) p.title = cached.set.title;
                    packs.put(key, p);
                    if (p.title == null) loadTitle(p);
                }
                p.count++;
            }
            if (res.messages.size() < 100 || last <= 0) {
                endReached = true;
            }
            offsetId = last;
            update();
            AndroidUtilities.runOnUIThread(() -> scanStep(pages - 1), 700);
        }));
    }

    private void loadTitle(Pack p) {
        MediaDataController.getInstance(currentAccount).getStickerSet(p.input, null, false, set -> AndroidUtilities.runOnUIThread(() -> {
            if (set != null && set.set != null) {
                p.title = set.set.title;
                update();
            }
        }));
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asShadow(scanned + " Nachrichten durchsucht · " + packs.size() + " Packs" + (loading ? " · suche …" : endReached ? " · Anfang erreicht" : "")));
        if (error != null) {
            items.add(UItem.asShadow(error));
        }
        for (int i = 0; i < sorted.size(); i++) {
            Pack p = sorted.get(i);
            String title = (p.emoji != null ? p.emoji + "  " : "") + (p.title != null ? p.title : "Pack wird geladen …");
            items.add(UItem.asButton(ID_BASE + i, title, p.count + "×"));
        }
        if (!endReached && !loading) {
            items.add(UItem.asButton(ID_MORE, R.drawable.msg_retry, "Weiter durchsuchen (+1000 Nachrichten)").accent());
        }
        items.add(UItem.asShadow("Antippen öffnet das Pack – dort kannst du es hinzufügen, einzelne Sticker senden oder als Favorit merken."));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_MORE) {
            error = null;
            scanStep(PAGES_PER_STEP);
            update();
            return;
        }
        int i = item.id - ID_BASE;
        if (i >= 0 && i < sorted.size()) {
            showDialog(new StickersAlert(getParentActivity(), this, sorted.get(i).input, null, null, false));
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        int i = item.id - ID_BASE;
        if (i >= 0 && i < sorted.size()) {
            SparkMedia.downloadPack(this, sorted.get(i).input);
            return true;
        }
        return false;
    }
}

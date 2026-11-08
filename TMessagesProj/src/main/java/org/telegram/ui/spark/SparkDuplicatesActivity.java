package org.telegram.ui.spark;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

/** Doppelte Nachrichten in "Gespeicherte Nachrichten" finden und aufraeumen (die aelteste bleibt). */
public class SparkDuplicatesActivity extends UniversalFragment {

    private static final int ID_SCAN = 1;
    private static final int ID_DELETE_ALL = 2;
    private static final int ID_BASE = 1000;
    private static final int MAX_MESSAGES = 10000;

    private static class Group {
        String key;
        String preview;
        ArrayList<Integer> ids = new ArrayList<>(); // aelteste zuerst
        ArrayList<Integer> dates = new ArrayList<>();
    }

    private boolean scanning;
    private int scanned;
    private boolean done;
    private final ArrayList<Group> groups = new ArrayList<>();
    private int reqToken;

    @Override
    protected CharSequence getTitle() {
        return "Duplikate in Gespeichertes";
    }

    @Override
    public void onFragmentDestroy() {
        reqToken++;
        super.onFragmentDestroy();
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (scanning) {
            items.add(UItem.asShadow("Durchsuche Gespeicherte Nachrichten … " + scanned + " geprüft"));
            return;
        }
        items.add(UItem.asButton(ID_SCAN, org.telegram.messenger.R.drawable.msg_search, done ? "Erneut durchsuchen" : "Jetzt durchsuchen"));
        if (!done) {
            items.add(UItem.asShadow("Findet gleiche Fotos, Videos, Dateien, Sticker und gleiche Texte in deinen Gespeicherten Nachrichten. Beim Aufräumen bleibt jeweils die älteste erhalten."));
            return;
        }
        int extra = 0;
        for (Group g : groups) extra += g.ids.size() - 1;
        if (groups.isEmpty()) {
            items.add(UItem.asShadow(scanned + " Nachrichten geprüft – keine Duplikate gefunden. 🎉"));
            return;
        }
        items.add(UItem.asButton(ID_DELETE_ALL, org.telegram.messenger.R.drawable.msg_delete, "Alle Duplikate löschen (" + extra + ")").red());
        items.add(UItem.asShadow(scanned + " Nachrichten geprüft · " + groups.size() + " mehrfach vorhanden. Antippen = alle Vorkommen zeigen, lange drücken = nur diese aufräumen."));
        SimpleDateFormat f = new SimpleDateFormat("dd.MM.yy", Locale.GERMANY);
        for (int i = 0; i < groups.size(); i++) {
            Group g = groups.get(i);
            items.add(UItem.asButton(ID_BASE + i, g.preview, g.ids.size() + "× · seit " + f.format(new Date(g.dates.get(0) * 1000L))));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_SCAN) {
            startScan();
            return;
        }
        if (item.id == ID_DELETE_ALL) {
            ArrayList<Integer> del = new ArrayList<>();
            for (Group g : groups) del.addAll(g.ids.subList(1, g.ids.size()));
            confirmDelete(del, null);
            return;
        }
        int i = item.id - ID_BASE;
        if (i < 0 || i >= groups.size()) return;
        Group g = groups.get(i);
        if (g.key.startsWith("t")) {
            // Text: in Gespeicherte mit Suche oeffnen, dann zeigt Telegram alle Treffer
            String q = g.key.substring(1);
            if (q.length() > 60) q = q.substring(0, 60);
            final String query = q;
            Bundle args = new Bundle();
            args.putLong("user_id", getUserConfig().getClientUserId());
            ChatActivity chat = new ChatActivity(args);
            presentFragment(chat);
            AndroidUtilities.runOnUIThread(() -> chat.openSearchWithText(query), 400);
            return;
        }
        // Medien: Liste aller Vorkommen zum Hinspringen
        SimpleDateFormat f = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.GERMANY);
        CharSequence[] texts = new CharSequence[g.ids.size()];
        for (int k = 0; k < g.ids.size(); k++) {
            texts[k] = (k + 1) + ". " + f.format(new Date(g.dates.get(k) * 1000L)) + (k == 0 ? " (älteste, bleibt)" : "");
        }
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(SparkUi.mark(g.ids.size() + "× " + g.preview));
        b.setItems(texts, (d, w) -> {
            Bundle args = new Bundle();
            args.putLong("user_id", getUserConfig().getClientUserId());
            args.putInt("message_id", g.ids.get(w));
            presentFragment(new ChatActivity(args));
        });
        showDialog(b.create());
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        int i = item.id - ID_BASE;
        if (i < 0 || i >= groups.size()) return false;
        Group g = groups.get(i);
        confirmDelete(new ArrayList<>(g.ids.subList(1, g.ids.size())), g);
        return true;
    }

    private void confirmDelete(ArrayList<Integer> ids, Group only) {
        if (ids.isEmpty()) return;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(SparkUi.mark("Duplikate löschen"));
        b.setMessage(ids.size() + " doppelte Nachricht(en) aus Gespeichertes löschen? Die jeweils älteste bleibt erhalten.");
        b.setPositiveButton("Löschen", (d, w) -> {
            long self = getUserConfig().getClientUserId();
            for (int k = 0; k < ids.size(); k += 100) {
                ArrayList<Integer> chunk = new ArrayList<>(ids.subList(k, Math.min(ids.size(), k + 100)));
                getMessagesController().deleteMessages(chunk, null, null, self, 0, true, 0);
            }
            if (only != null) {
                groups.remove(only);
            } else {
                groups.clear();
            }
            listView.adapter.update(true);
            BulletinFactory.of(this).createSimpleBulletin(org.telegram.messenger.R.raw.ic_delete, "✦ " + ids.size() + " Duplikat(e) gelöscht").show();
        });
        b.setNegativeButton("Abbrechen", null);
        AlertDialog dlg = b.create();
        showDialog(dlg);
        android.widget.TextView btn = (android.widget.TextView) dlg.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        if (btn != null) btn.setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_text_RedBold));
    }

    // ------------------------------------------------------------------ Suche

    private final LinkedHashMap<String, Group> map = new LinkedHashMap<>();

    private void startScan() {
        scanning = true;
        done = false;
        scanned = 0;
        map.clear();
        groups.clear();
        listView.adapter.update(true);
        load(0, ++reqToken);
    }

    private void load(int offsetId, int token) {
        TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
        req.peer = getMessagesController().getInputPeer(getUserConfig().getClientUserId());
        req.offset_id = offsetId;
        req.limit = 100;
        getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            if (token != reqToken) return;
            int minId = 0;
            int count = 0;
            if (res instanceof TLRPC.messages_Messages) {
                TLRPC.messages_Messages m = (TLRPC.messages_Messages) res;
                for (TLRPC.Message msg : m.messages) {
                    count++;
                    if (minId == 0 || msg.id < minId) minId = msg.id;
                    add(msg);
                }
            }
            scanned += count;
            if (err != null && scanned == 0) {
                scanning = false;
                listView.adapter.update(true);
                BulletinFactory.of(this).createErrorBulletin("Fehler: " + err.text).show();
                return;
            }
            if (count > 0 && minId > 1 && scanned < MAX_MESSAGES) {
                listView.adapter.update(true);
                final int next = minId;
                AndroidUtilities.runOnUIThread(() -> load(next, token), 150); // Telegram nicht ueberlasten
                return;
            }
            finish();
        }));
    }

    private void add(TLRPC.Message msg) {
        if (msg == null || msg instanceof TLRPC.TL_messageService) return;
        String key = null;
        String preview = null;
        String text = msg.message == null ? "" : msg.message.trim();
        if (msg.media instanceof TLRPC.TL_messageMediaPhoto && msg.media.photo != null) {
            key = "p" + msg.media.photo.id;
            preview = "🖼 Foto";
        } else if (msg.media instanceof TLRPC.TL_messageMediaDocument && msg.media.document != null) {
            key = "d" + msg.media.document.id;
            preview = "📎 " + docKind(msg.media.document);
        } else if (msg.media != null && !(msg.media instanceof TLRPC.TL_messageMediaEmpty) && !(msg.media instanceof TLRPC.TL_messageMediaWebPage)) {
            return; // Umfragen, Standorte usw. nicht vergleichen
        }
        if (key == null) {
            if (TextUtils.isEmpty(text)) return;
            key = "t" + text;
        } else if (!TextUtils.isEmpty(text)) {
            key += "|" + text;
        }
        if (!TextUtils.isEmpty(text)) {
            String t = text.replace('\n', ' ');
            if (t.length() > 80) t = t.substring(0, 80) + " …";
            preview = preview == null ? t : preview + " · " + t;
        }
        Group g = map.get(key);
        if (g == null) {
            g = new Group();
            g.key = key;
            g.preview = preview;
            map.put(key, g);
        }
        // Nachrichten kommen neueste zuerst -> vorne einfuegen = aelteste zuerst
        g.ids.add(0, msg.id);
        g.dates.add(0, msg.date);
    }

    private static String docKind(TLRPC.Document doc) {
        for (TLRPC.DocumentAttribute a : doc.attributes) {
            if (a instanceof TLRPC.TL_documentAttributeSticker) return "Sticker";
            if (a instanceof TLRPC.TL_documentAttributeAnimated) return "GIF";
            if (a instanceof TLRPC.TL_documentAttributeVideo) return a.round_message ? "Videonachricht" : "Video";
            if (a instanceof TLRPC.TL_documentAttributeAudio) return a.voice ? "Sprachnachricht" : "Audio";
        }
        for (TLRPC.DocumentAttribute a : doc.attributes) {
            if (a instanceof TLRPC.TL_documentAttributeFilename && !TextUtils.isEmpty(a.file_name)) return a.file_name;
        }
        return "Datei";
    }

    private void finish() {
        scanning = false;
        done = true;
        groups.clear();
        for (Group g : map.values()) {
            if (g.ids.size() > 1) groups.add(g);
        }
        java.util.Collections.sort(groups, (a, b) -> b.ids.size() - a.ids.size());
        map.clear();
        if (listView != null) listView.adapter.update(true);
    }
}

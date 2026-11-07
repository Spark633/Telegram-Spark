package org.telegram.ui.spark;

import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.spark.SparkRateLimit;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

/** Chat-Export als HTML-Datei in „Downloads“ und "jetzt als gelesen melden" (Ghost-Modus). */
public class SparkExport {

    private static final String FEATURE = "historyScan";
    private static final int PER_HOUR = 400;

    /** Liest den Chat bis zur letzten Nachricht als gelesen (auch im Ghost-Modus). */
    public static void markReadNow(BaseFragment fragment, long dialogId) {
        int account = fragment.getCurrentAccount();
        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.InputPeer peer = mc.getInputPeer(dialogId);
        org.telegram.tgnet.TLObject req;
        if (peer instanceof TLRPC.TL_inputPeerChannel) {
            TLRPC.TL_channels_readHistory r = new TLRPC.TL_channels_readHistory();
            r.channel = mc.getInputChannel(-dialogId);
            r.max_id = Integer.MAX_VALUE;
            req = r;
        } else {
            TLRPC.TL_messages_readHistory r = new TLRPC.TL_messages_readHistory();
            r.peer = peer;
            r.max_id = Integer.MAX_VALUE;
            req = r;
        }
        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            if (err == null) {
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, "✦ Als gelesen gemeldet").show();
            } else {
                BulletinFactory.of(fragment).createErrorBulletin("Fehler: " + err.text).show();
            }
        }));
    }

    /** Fragt, wie viele Nachrichten exportiert werden sollen, und startet den Export. */
    public static void exportChat(BaseFragment fragment, long dialogId) {
        String[] names = {"Letzte 1.000 Nachrichten", "Letzte 5.000 Nachrichten", "Letzte 20.000 Nachrichten", "Ganzer Chat"};
        int[] limits = {1000, 5000, 20000, Integer.MAX_VALUE};
        AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity());
        b.setTitle(SparkUi.mark("Chat als HTML exportieren"));
        b.setItems(names, (d, w) -> new Job(fragment, dialogId, limits[w]).start());
        fragment.showDialog(b.create());
    }

    private static class Job {
        final BaseFragment fragment;
        final long dialogId;
        final int limit;
        final int account;
        final ArrayList<TLRPC.Message> messages = new ArrayList<>();
        int offsetId;
        boolean cancelled;
        AlertDialog progress;

        Job(BaseFragment fragment, long dialogId, int limit) {
            this.fragment = fragment;
            this.dialogId = dialogId;
            this.limit = limit;
            this.account = fragment.getCurrentAccount();
        }

        void start() {
            progress = new AlertDialog(fragment.getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
            progress.setCanCancel(true);
            progress.setOnCancelListener(d -> cancelled = true);
            progress.show();
            next();
        }

        void next() {
            if (cancelled) return;
            long wait = SparkRateLimit.waitTime(FEATURE, PER_HOUR);
            if (wait > 0) {
                finish("Telegram bremst – exportiert bis hier.");
                return;
            }
            TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
            req.peer = MessagesController.getInstance(account).getInputPeer(dialogId);
            req.offset_id = offsetId;
            req.limit = 100;
            SparkRateLimit.use(FEATURE);
            ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                SparkRateLimit.onError(FEATURE, err);
                if (cancelled) return;
                if (!(res instanceof TLRPC.messages_Messages)) {
                    finish(err != null ? "Abbruch: " + err.text : null);
                    return;
                }
                TLRPC.messages_Messages m = (TLRPC.messages_Messages) res;
                MessagesController.getInstance(account).putUsers(m.users, false);
                MessagesController.getInstance(account).putChats(m.chats, false);
                int last = 0;
                for (TLRPC.Message msg : m.messages) {
                    last = msg.id;
                    messages.add(msg);
                }
                if (m.messages.size() < 100 || last <= 0 || messages.size() >= limit) {
                    finish(null);
                } else {
                    offsetId = last;
                    AndroidUtilities.runOnUIThread(this::next, 400);
                }
            }));
        }

        void finish(String note) {
            final String title = SparkNotesActivity.dialogName(account, dialogId);
            final ArrayList<TLRPC.Message> list = new ArrayList<>(messages);
            org.telegram.messenger.Utilities.globalQueue.postRunnable(() -> {
                String error = null;
                File out = null;
                try {
                    Collections.reverse(list);
                    out = new File(ApplicationLoader.applicationContext.getCacheDir(), "sparkgram_export.html");
                    try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)) {
                        writeHtml(w, title, list, note);
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                    error = e.getMessage();
                }
                final File file = out;
                final String err = error;
                AndroidUtilities.runOnUIThread(() -> {
                    try { progress.dismiss(); } catch (Throwable ignore) {}
                    if (err != null || file == null) {
                        BulletinFactory.of(fragment).createErrorBulletin("Export fehlgeschlagen: " + err).show();
                        return;
                    }
                    String name = "Chat_" + title.replaceAll("[^A-Za-z0-9ÄÖÜäöüß_-]+", "_") + "_" + new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(new Date()) + ".html";
                    MediaController.saveFile(file.getAbsolutePath(), fragment.getParentActivity(), 2, name, "text/html", uri ->
                        BulletinFactory.of(fragment).createSimpleBulletin(R.raw.ic_download, "✦ " + list.size() + " Nachrichten exportiert → Downloads/" + name).show());
                });
            });
        }

        private String esc(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
        }

        private String sender(TLRPC.Message m) {
            MessagesController mc = MessagesController.getInstance(account);
            long id = m.from_id != null ? DialogObject.getPeerDialogId(m.from_id) : dialogId;
            if (m.from_id == null && !DialogObject.isUserDialog(dialogId)) id = dialogId;
            if (id > 0) {
                TLRPC.User u = mc.getUser(id);
                return u != null ? UserObject.getUserName(u) : "ID " + id;
            }
            TLRPC.Chat c = mc.getChat(-id);
            return c != null ? c.title : "ID " + id;
        }

        private String media(TLRPC.Message m) {
            if (m.media == null || m.media instanceof TLRPC.TL_messageMediaEmpty) return null;
            if (m.media instanceof TLRPC.TL_messageMediaPhoto) return "[Foto]";
            if (m.media instanceof TLRPC.TL_messageMediaGeo || m.media instanceof TLRPC.TL_messageMediaVenue) return "[Standort]";
            if (m.media instanceof TLRPC.TL_messageMediaContact) return "[Kontakt]";
            if (m.media instanceof TLRPC.TL_messageMediaPoll) return "[Umfrage]";
            if (m.media instanceof TLRPC.TL_messageMediaWebPage) return null;
            TLRPC.Document d = m.media.document;
            if (d != null) {
                for (TLRPC.DocumentAttribute a : d.attributes) {
                    if (a instanceof TLRPC.TL_documentAttributeSticker) return "[Sticker " + (a.alt != null ? a.alt : "") + "]";
                    if (a instanceof TLRPC.TL_documentAttributeAudio) return a.voice ? "[Sprachnachricht " + (int) a.duration + " s]" : "[Audio]";
                    if (a instanceof TLRPC.TL_documentAttributeVideo) return a.round_message ? "[Videonachricht]" : "[Video]";
                    if (a instanceof TLRPC.TL_documentAttributeAnimated) return "[GIF]";
                }
                return "[Datei" + (d.mime_type != null ? " " + d.mime_type : "") + "]";
            }
            return "[Medium]";
        }

        private void writeHtml(OutputStreamWriter w, String title, ArrayList<TLRPC.Message> list, String note) throws Exception {
            SimpleDateFormat day = new SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.GERMANY);
            SimpleDateFormat time = new SimpleDateFormat("HH:mm", Locale.GERMANY);
            w.write("<!doctype html><html lang=\"de\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
            w.write("<title>" + esc(title) + "</title><style>body{font-family:sans-serif;background:#f0f2f5;margin:0;padding:12px;color:#111}"
                + ".d{text-align:center;color:#666;font-size:13px;margin:16px 0 8px}.m{background:#fff;border-radius:12px;padding:8px 12px;margin:6px 0;max-width:720px}"
                + ".me{background:#e3f6d6;margin-left:auto}.s{font-weight:bold;font-size:13px;color:#2a6fb0}.t{color:#888;font-size:11px;float:right;margin-left:8px}"
                + ".x{color:#555;font-style:italic}.f{color:#888;font-size:12px}h1{font-size:18px}@media(prefers-color-scheme:dark){body{background:#17212b;color:#eee}.m{background:#232e3c}.me{background:#2b5278}}</style></head><body>");
            w.write("<h1>" + esc(title) + "</h1><div class=\"f\">Exportiert mit Sparkgram am " + new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(new Date()) + " · " + list.size() + " Nachrichten" + (note != null ? " · " + esc(note) : "") + "</div>");
            String lastDay = null;
            for (TLRPC.Message m : list) {
                String d = day.format(new Date(m.date * 1000L));
                if (!d.equals(lastDay)) {
                    w.write("<div class=\"d\">" + d + "</div>");
                    lastDay = d;
                }
                if (m instanceof TLRPC.TL_messageService) {
                    w.write("<div class=\"d\">" + time.format(new Date(m.date * 1000L)) + " · Service-Nachricht</div>");
                    continue;
                }
                w.write("<div class=\"m" + (m.out ? " me" : "") + "\"><span class=\"t\">" + time.format(new Date(m.date * 1000L)) + (m.edit_date != 0 ? " ✏️" : "") + "</span>");
                w.write("<div class=\"s\">" + esc(sender(m)) + "</div>");
                if (m.fwd_from != null) {
                    w.write("<div class=\"f\">↪ weitergeleitet" + (m.fwd_from.from_name != null ? " von " + esc(m.fwd_from.from_name) : "") + "</div>");
                }
                String med = media(m);
                if (med != null) w.write("<div class=\"x\">" + esc(med) + "</div>");
                if (!TextUtils.isEmpty(m.message)) w.write("<div>" + esc(m.message) + "</div>");
                w.write("</div>");
            }
            w.write("</body></html>");
        }
    }
}

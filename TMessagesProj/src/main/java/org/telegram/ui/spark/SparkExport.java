package org.telegram.ui.spark;

import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.spark.SparkRateLimit;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
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

    /** Fragt Format und Umfang ab und startet den Export. */
    public static void exportChat(BaseFragment fragment, long dialogId) {
        AlertDialog.Builder f = new AlertDialog.Builder(fragment.getParentActivity());
        f.setTitle(SparkUi.mark("Chat exportieren"));
        f.setItems(new CharSequence[]{"PDF (zum Lesen/Drucken)", "HTML (im Browser öffnen)"}, (d0, which) -> {
            final boolean pdf = which == 0;
            String[] names = {"Letzte 1.000 Nachrichten", "Letzte 5.000 Nachrichten", "Letzte 20.000 Nachrichten", "Ganzer Chat"};
            int[] limits = {1000, 5000, 20000, Integer.MAX_VALUE};
            AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity());
            b.setTitle(SparkUi.mark(pdf ? "Als PDF exportieren" : "Als HTML exportieren"));
            b.setItems(names, (d, w) -> new Job(fragment, dialogId, limits[w], pdf).start());
            fragment.showDialog(b.create());
        });
        fragment.showDialog(f.create());
    }

    private static class Job {
        final BaseFragment fragment;
        final long dialogId;
        final int limit;
        final int account;
        final ArrayList<TLRPC.Message> messages = new ArrayList<>();
        final boolean pdf;
        int offsetId;
        boolean cancelled;
        AlertDialog progress;

        Job(BaseFragment fragment, long dialogId, int limit, boolean pdf) {
            this.fragment = fragment;
            this.dialogId = dialogId;
            this.limit = limit;
            this.pdf = pdf;
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
            if (pdf) {
                int n = 0;
                for (TLRPC.Message m : list) {
                    if (n++ > 600) break; // nicht unbegrenzt viele Bilder laden
                    try { prepareImage(m); } catch (Throwable e) { FileLog.e(e); }
                }
            }
            org.telegram.messenger.Utilities.globalQueue.postRunnable(() -> {
                String error = null;
                File out = null;
                try {
                    if (pdf) waitForImages();
                    Collections.reverse(list);
                    out = new File(ApplicationLoader.applicationContext.getCacheDir(), pdf ? "sparkgram_export.pdf" : "sparkgram_export.html");
                    if (pdf) {
                        writePdf(out, title, list, note);
                    } else {
                        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)) {
                            writeHtml(w, title, list, note);
                        }
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
                    String name = "Chat_" + title.replaceAll("[^A-Za-z0-9ÄÖÜäöüß_-]+", "_") + "_" + new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(new Date()) + (pdf ? ".pdf" : ".html");
                    SparkUi.saveToDownloads(file, name, pdf ? "application/pdf" : "text/html", path -> {
                        if (path != null) {
                            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.ic_download, "✦ " + list.size() + " Nachrichten exportiert: " + path).show();
                        } else {
                            BulletinFactory.of(fragment).createErrorBulletin("Speichern fehlgeschlagen").show();
                        }
                    });
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

        /** Text einer Service-Nachricht ("X ist beigetreten" usw.) wie im Chat. */
        private String serviceText(TLRPC.Message m) {
            try {
                MessageObject mo = new MessageObject(account, m, false, false);
                if (mo.messageText != null && mo.messageText.length() > 0) return mo.messageText.toString();
            } catch (Throwable e) {
                FileLog.e(e);
            }
            return "Service-Nachricht";
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

        // ---------------------------------------------------------------- Bilder fuer das PDF

        private final java.util.HashMap<Integer, File> imageFiles = new java.util.HashMap<>();

        /** Bildquelle einer Nachricht (Foto, statischer Sticker, sonst Vorschaubild) – startet ggf. den Download. */
        private void prepareImage(TLRPC.Message m) {
            if (m.media == null) return;
            FileLoader fl = FileLoader.getInstance(account);
            org.telegram.messenger.ImageLocation loc = null;
            TLObject attach = null;
            String ext = "jpg";
            if (m.media instanceof TLRPC.TL_messageMediaPhoto && m.media.photo != null) {
                TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(m.media.photo.sizes, 800);
                if (size != null) {
                    attach = size;
                    loc = org.telegram.messenger.ImageLocation.getForPhoto(size, m.media.photo);
                }
            } else if (m.media.document != null) {
                TLRPC.Document d = m.media.document;
                boolean staticSticker = "image/webp".equals(d.mime_type);
                if (staticSticker) {
                    attach = d;
                    ext = "webp";
                    loc = org.telegram.messenger.ImageLocation.getForDocument(d);
                } else {
                    TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(d.thumbs, 320);
                    if (thumb != null && !(thumb instanceof TLRPC.TL_photoStrippedSize) && !(thumb instanceof TLRPC.TL_photoPathSize)) {
                        attach = thumb;
                        loc = org.telegram.messenger.ImageLocation.getForDocument(thumb, d);
                    }
                }
            }
            if (attach == null || loc == null) return;
            File f = fl.getPathToAttach(attach, true);
            if (f == null) return;
            imageFiles.put(m.id, f);
            if (!f.exists()) {
                fl.loadFile(loc, new MessageObject(account, m, false, false), ext, FileLoader.PRIORITY_NORMAL, 1);
            }
        }

        /** Wartet (begrenzt), bis die Bilder geladen sind. Hintergrund-Thread! */
        private void waitForImages() {
            long deadline = System.currentTimeMillis() + Math.min(120_000, 10_000 + imageFiles.size() * 1500L);
            while (System.currentTimeMillis() < deadline) {
                boolean all = true;
                for (File f : imageFiles.values()) {
                    if (!f.exists()) { all = false; break; }
                }
                if (all) return;
                try { Thread.sleep(300); } catch (InterruptedException ignore) { return; }
            }
        }

        // ---------------------------------------------------------------- PDF im Chat-Look

        private static final int PAGE_W = 595, PAGE_H = 842, MARGIN = 28, FOOTER = 26;

        private android.graphics.pdf.PdfDocument pdfDoc;
        private android.graphics.pdf.PdfDocument.Page pdfPage;
        private int pageNo;
        private float py;
        private String pdfTitle;
        private android.text.TextPaint pBody, pName, pSmall, pTitle, pCenter;
        private android.graphics.Paint pBubbleIn, pBubbleOut, pBg;

        private void newPage() {
            if (pdfPage != null) pdfDoc.finishPage(pdfPage);
            pageNo++;
            pdfPage = pdfDoc.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create());
            android.graphics.Canvas c = pdfPage.getCanvas();
            c.drawRect(0, 0, PAGE_W, PAGE_H, pBg);
            String foot = pdfTitle + " · Seite " + pageNo;
            c.drawText(foot, (PAGE_W - pSmall.measureText(foot)) / 2f, PAGE_H - 12, pSmall);
            py = MARGIN;
        }

        private float spaceLeft() {
            return PAGE_H - FOOTER - py;
        }

        private android.text.StaticLayout layout(CharSequence text, android.text.TextPaint paint, int width) {
            return new android.text.StaticLayout(text, paint, Math.max(10, width), android.text.Layout.Alignment.ALIGN_NORMAL, 1.15f, 0, false);
        }

        private static float maxLineWidth(android.text.StaticLayout l) {
            float w = 0;
            for (int i = 0; i < l.getLineCount(); i++) w = Math.max(w, l.getLineWidth(i));
            return w;
        }

        /** Zentrierte Zeile (Datum, Service-Nachricht) als kleine Pille. */
        private void drawCenter(String text) {
            // mehrzeilig, zentriert (lange Service-Nachrichten werden umgebrochen)
            int maxW = (int) ((PAGE_W - 2 * MARGIN) * 0.8f);
            android.text.StaticLayout l = new android.text.StaticLayout(text, pCenter, maxW, android.text.Layout.Alignment.ALIGN_CENTER, 1.1f, 0, false);
            float tw = maxLineWidth(l);
            float h = l.getHeight() + 7;
            if (spaceLeft() < h + 6) newPage();
            android.graphics.Canvas c = pdfPage.getCanvas();
            float w = tw + 16;
            float x = (PAGE_W - w) / 2f;
            android.graphics.Paint pill = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            pill.setColor(0x55000000);
            c.drawRoundRect(new android.graphics.RectF(x, py, x + w, py + h), 9, 9, pill);
            c.save();
            c.translate((PAGE_W - maxW) / 2f, py + 3.5f);
            l.draw(c);
            c.restore();
            py += h + 8;
        }

        private void drawBubble(TLRPC.Message m, boolean group, SimpleDateFormat time) {
            final int maxW = (int) ((PAGE_W - 2 * MARGIN) * 0.75f);
            final int pad = 8;
            final int inner = maxW - 2 * pad;
            final boolean out = m.out;

            String name = group && !out ? sender(m) : null;
            String fwd = m.fwd_from != null ? "↪ Weitergeleitet" + (m.fwd_from.from_name != null ? " von " + m.fwd_from.from_name : "") : null;
            String med = media(m);
            File imgFile = imageFiles.get(m.id);
            android.graphics.Bitmap bmp = null;
            if (imgFile != null && imgFile.exists()) {
                try {
                    bmp = org.telegram.messenger.ImageLoader.loadBitmap(imgFile.getAbsolutePath(), null, 800, 800, false);
                } catch (Throwable ignore) {}
            }
            boolean sticker = med != null && med.startsWith("[Sticker");
            String timeText = time.format(new Date(m.date * 1000L)) + (m.edit_date != 0 ? " · bearbeitet" : "");

            android.text.StaticLayout nameL = name != null ? layout(name, pName, inner) : null;
            android.text.StaticLayout fwdL = fwd != null ? layout(fwd, pSmall, inner) : null;
            // Platzhalter nur, wenn kein Bild da ist (oder bei Sprachnachricht/Datei)
            String medText = med != null && (bmp == null || med.startsWith("[Sprach") || med.startsWith("[Datei") || med.startsWith("[Audio")) ? med : null;
            if (med != null && bmp != null && (med.startsWith("[Video") || med.startsWith("[GIF"))) medText = med;
            android.text.StaticLayout medL = medText != null ? layout(medText, pSmall, inner) : null;
            android.text.StaticLayout textL = !TextUtils.isEmpty(m.message) ? layout(m.message, pBody, inner) : null;

            float imgW = 0, imgH = 0;
            if (bmp != null) {
                float maxImgW = sticker ? 120 : inner;
                float maxImgH = sticker ? 120 : 260;
                float scale = Math.min(maxImgW / bmp.getWidth(), maxImgH / bmp.getHeight());
                imgW = bmp.getWidth() * scale;
                imgH = bmp.getHeight() * scale;
            }
            float timeW = pSmall.measureText(timeText);

            float contentW = timeW;
            if (nameL != null) contentW = Math.max(contentW, maxLineWidth(nameL));
            if (fwdL != null) contentW = Math.max(contentW, maxLineWidth(fwdL));
            if (medL != null) contentW = Math.max(contentW, maxLineWidth(medL));
            if (textL != null) contentW = Math.max(contentW, maxLineWidth(textL));
            contentW = Math.max(contentW, imgW);
            contentW = Math.min(contentW, inner);
            float bubbleW = contentW + 2 * pad;

            float headH = (nameL != null ? nameL.getHeight() + 2 : 0) + (fwdL != null ? fwdL.getHeight() + 2 : 0) + (imgH > 0 ? imgH + 4 : 0) + (medL != null ? medL.getHeight() + 2 : 0);
            float timeH = 12;
            float usable = PAGE_H - FOOTER - MARGIN - 2 * pad - 4;

            // Text ggf. ueber mehrere Seiten aufteilen
            int line = 0;
            int lines = textL != null ? textL.getLineCount() : 0;
            boolean first = true;
            do {
                float avail = spaceLeft() - 2 * pad - 4;
                float needHead = first ? headH : 0;
                // wie viele Textzeilen passen noch (mit Zeit-Zeile, falls es die letzten sind)?
                int last = line;
                if (textL != null) {
                    while (last < lines && needHead + (textL.getLineBottom(last) - textL.getLineTop(line)) + timeH <= avail) last++;
                }
                boolean fitsAll = textL == null ? needHead + timeH <= avail : last == lines;
                if (!fitsAll && (last == line || first && needHead + timeH > avail)) {
                    if (py > MARGIN + 1) {
                        newPage();
                        continue;
                    }
                    // passt auf keine leere Seite: so viel wie moeglich
                    if (textL != null && last == line) last = Math.min(lines, line + 1);
                }
                float textH = textL != null && last > line ? textL.getLineTop(last) - textL.getLineTop(line) : 0;
                boolean isLast = textL == null || last >= lines;
                float h = needHead + textH + (isLast ? timeH : 0) + 2 * pad;
                float x = out ? PAGE_W - MARGIN - bubbleW : MARGIN;
                android.graphics.Canvas c = pdfPage.getCanvas();
                boolean noBubble = sticker && bmp != null && textL == null && name == null && fwd == null;
                if (!noBubble) {
                    c.drawRoundRect(new android.graphics.RectF(x, py, x + bubbleW, py + h), 10, 10, out ? pBubbleOut : pBubbleIn);
                }
                float cy = py + pad;
                if (first) {
                    if (nameL != null) { c.save(); c.translate(x + pad, cy); nameL.draw(c); c.restore(); cy += nameL.getHeight() + 2; }
                    if (fwdL != null) { c.save(); c.translate(x + pad, cy); fwdL.draw(c); c.restore(); cy += fwdL.getHeight() + 2; }
                    if (bmp != null) {
                        float ix = out && sticker ? x + bubbleW - pad - imgW : x + pad;
                        android.graphics.RectF dst = new android.graphics.RectF(ix, cy, ix + imgW, cy + imgH);
                        c.save();
                        if (!sticker) {
                            android.graphics.Path clip = new android.graphics.Path();
                            clip.addRoundRect(dst, 6, 6, android.graphics.Path.Direction.CW);
                            c.clipPath(clip);
                        }
                        c.drawBitmap(bmp, null, dst, new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG));
                        c.restore();
                        cy += imgH + 4;
                    }
                    if (medL != null) { c.save(); c.translate(x + pad, cy); medL.draw(c); c.restore(); cy += medL.getHeight() + 2; }
                }
                if (textL != null && last > line) {
                    c.save();
                    c.translate(x + pad, cy - textL.getLineTop(line));
                    c.clipRect(0, textL.getLineTop(line), inner, textL.getLineTop(last));
                    textL.draw(c);
                    c.restore();
                    cy += textH;
                }
                if (isLast) {
                    c.drawText(timeText, x + bubbleW - pad - timeW, cy + 9, pSmall);
                }
                py += h + 5;
                line = last;
                first = false;
                if (!isLast) newPage();
                if (isLast) break;
            } while (true);
            if (bmp != null) bmp.recycle();
        }

        /** PDF (A4) im Chat-Look: eigene Nachrichten rechts, Bilder eingebettet. */
        private void writePdf(File out, String title, ArrayList<TLRPC.Message> list, String note) throws Exception {
            pdfDoc = new android.graphics.pdf.PdfDocument();
            pdfTitle = title;
            pBody = new android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            pBody.setTextSize(10.5f);
            pBody.setColor(0xff111111);
            pName = new android.text.TextPaint(pBody);
            pName.setFakeBoldText(true);
            pName.setTextSize(10f);
            pName.setColor(0xff2a6fb0);
            pSmall = new android.text.TextPaint(pBody);
            pSmall.setTextSize(8f);
            pSmall.setColor(0xff7a8691);
            pTitle = new android.text.TextPaint(pBody);
            pTitle.setTextSize(16f);
            pTitle.setFakeBoldText(true);
            pCenter = new android.text.TextPaint(pBody);
            pCenter.setTextSize(8.5f);
            pCenter.setColor(0xffffffff);
            pBubbleIn = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            pBubbleIn.setColor(0xffffffff);
            pBubbleOut = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            pBubbleOut.setColor(0xffdcf8c6);
            pBg = new android.graphics.Paint();
            pBg.setColor(0xffc9d8e4);

            boolean group = !DialogObject.isUserDialog(dialogId);
            SimpleDateFormat day = new SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.GERMANY);
            SimpleDateFormat time = new SimpleDateFormat("HH:mm", Locale.GERMANY);

            newPage();
            android.text.StaticLayout t = layout(title, pTitle, PAGE_W - 2 * MARGIN);
            android.graphics.Canvas c = pdfPage.getCanvas();
            c.save(); c.translate(MARGIN, py); t.draw(c); c.restore();
            py += t.getHeight() + 2;
            String sub = "Exportiert mit Sparkgram am " + new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(new Date()) + " · " + list.size() + " Nachrichten" + (note != null ? " · " + note : "");
            c.drawText(sub, MARGIN, py + 9, pSmall);
            py += 20;

            String lastDay = null;
            for (TLRPC.Message m : list) {
                if (cancelled) break;
                String d = day.format(new Date(m.date * 1000L));
                if (!d.equals(lastDay)) {
                    drawCenter(d);
                    lastDay = d;
                }
                if (m instanceof TLRPC.TL_messageService) {
                    drawCenter(time.format(new Date(m.date * 1000L)) + " · " + serviceText(m));
                    continue;
                }
                drawBubble(m, group, time);
            }
            pdfDoc.finishPage(pdfPage);
            try (FileOutputStream fos = new FileOutputStream(out)) {
                pdfDoc.writeTo(fos);
            }
            pdfDoc.close();
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
                    w.write("<div class=\"d\">" + time.format(new Date(m.date * 1000L)) + " · " + esc(serviceText(m)) + "</div>");
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

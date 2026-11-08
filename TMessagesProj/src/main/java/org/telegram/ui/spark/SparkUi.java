package org.telegram.ui.spark;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.spark.SparkConfig;

/** Gemeinsame Oberflaechen-Helfer fuer Sparkgram. */
public class SparkUi {

    public static final int MARKER_COLOR = 0xFFFF8A3D;
    public static final String MARKER = "✦";

    /** Haengt die Sparkgram-Markierung an einen Text an (falls aktiviert). */
    public static CharSequence mark(CharSequence text) {
        if (!SparkConfig.get(SparkConfig.SPARK_MARKER)) {
            return text;
        }
        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        sb.append("  ");
        int start = sb.length();
        sb.append(MARKER);
        sb.setSpan(new ForegroundColorSpan(MARKER_COLOR), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    /** Zeigt alle frueheren Fassungen einer bearbeiteten Nachricht. */
    public static void showEditHistory(org.telegram.ui.ActionBar.BaseFragment fragment, org.telegram.messenger.MessageObject message) {
        if (fragment == null || message == null || fragment.getParentActivity() == null) {
            return;
        }
        java.util.ArrayList<org.telegram.messenger.spark.SparkMessageStore.Version> versions =
            org.telegram.messenger.spark.SparkMessageStore.get(fragment.getCurrentAccount()).history(message.getDialogId(), message.getId());
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.GERMANY);
        StringBuilder sb = new StringBuilder();
        int n = 1;
        for (org.telegram.messenger.spark.SparkMessageStore.Version v : versions) {
            long when = v.editDate > 0 ? v.editDate * 1000L : (n == 1 && message.messageOwner != null ? message.messageOwner.date * 1000L : 0);
            sb.append("Fassung ").append(n++);
            if (when > 0) {
                sb.append(" (").append(f.format(new java.util.Date(when))).append(")");
            }
            sb.append(":\n").append(android.text.TextUtils.isEmpty(v.text) ? "(ohne Text)" : v.text).append("\n\n");
        }
        sb.append("Aktuell");
        if (message.messageOwner != null && message.messageOwner.edit_date > 0) {
            sb.append(" (").append(f.format(new java.util.Date(message.messageOwner.edit_date * 1000L))).append(")");
        }
        sb.append(":\n").append(message.messageOwner == null || android.text.TextUtils.isEmpty(message.messageOwner.message) ? "(ohne Text)" : message.messageOwner.message);
        final String text = sb.toString();
        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(mark(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.SparkEditHistory)));
        builder.setMessage(text);
        builder.setPositiveButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.OK), null);
        builder.setNeutralButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.Copy), (d, w) -> {
            AndroidUtilities.addToClipboard(text);
            org.telegram.ui.Components.BulletinFactory.of(fragment).createCopyBulletin(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.TextCopied)).show();
        });
        fragment.showDialog(builder.create());
    }

    /** Rueckfrage nach einer Sprachaufnahme: anhoeren, zuschneiden, senden oder verwerfen. */
    public static void confirmVoice(String path, org.telegram.tgnet.TLRPC.Document doc, Runnable onSend, Runnable onDiscard) {
        org.telegram.ui.ActionBar.BaseFragment f = org.telegram.ui.LaunchActivity.getSafeLastFragment();
        if (f == null || f.getParentActivity() == null) {
            onSend.run();
            return;
        }
        final android.app.Activity activity = f.getParentActivity();
        final android.media.MediaPlayer[] player = new android.media.MediaPlayer[1];
        final boolean[] decided = new boolean[1];
        // Laenge der Aufnahme bestimmen
        int durMs = 0;
        try {
            android.media.MediaPlayer probe = new android.media.MediaPlayer();
            probe.setDataSource(path);
            probe.prepare();
            durMs = probe.getDuration();
            probe.release();
        } catch (Throwable ignore) {}
        if (durMs <= 0 && doc != null) {
            for (org.telegram.tgnet.TLRPC.DocumentAttribute a : doc.attributes) {
                if (a instanceof org.telegram.tgnet.TLRPC.TL_documentAttributeAudio) durMs = (int) (a.duration * 1000);
            }
        }
        final int total = Math.max(durMs, 1);
        final int[] range = {0, total};
        final Runnable[] watcher = new Runnable[1];
        final Runnable stop = () -> {
            if (watcher[0] != null) {
                AndroidUtilities.cancelRunOnUIThread(watcher[0]);
                watcher[0] = null;
            }
            if (player[0] != null) {
                try { player[0].stop(); } catch (Throwable ignore) {}
                try { player[0].release(); } catch (Throwable ignore) {}
                player[0] = null;
            }
        };
        android.widget.LinearLayout box = new android.widget.LinearLayout(activity);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.TextView play = new android.widget.TextView(activity);
        play.setText("▶  Anhören");
        play.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 17);
        play.setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhiteBlueText4));
        play.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(4));
        box.addView(play);
        final android.widget.TextView rangeText = new android.widget.TextView(activity);
        rangeText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 14);
        rangeText.setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhiteGrayText));
        rangeText.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), 0);
        box.addView(rangeText);
        final Runnable updateText = () -> rangeText.setText(String.format(java.util.Locale.GERMANY, "Zuschneiden: %.1f s – %.1f s  (%.1f s)",
            range[0] / 1000f, range[1] / 1000f, (range[1] - range[0]) / 1000f));
        updateText.run();
        android.widget.SeekBar startBar = new android.widget.SeekBar(activity);
        android.widget.SeekBar endBar = new android.widget.SeekBar(activity);
        startBar.setMax(total);
        endBar.setMax(total);
        endBar.setProgress(total);
        startBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar sb, int p, boolean user) {
                if (p > range[1] - 500) { p = Math.max(0, range[1] - 500); sb.setProgress(p); }
                range[0] = p;
                updateText.run();
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar sb) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar sb) {}
        });
        endBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar sb, int p, boolean user) {
                if (p < range[0] + 500) { p = Math.min(total, range[0] + 500); sb.setProgress(p); }
                range[1] = p;
                updateText.run();
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar sb) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar sb) {}
        });
        android.widget.TextView l1 = new android.widget.TextView(activity);
        l1.setText("Anfang");
        android.widget.TextView l2 = new android.widget.TextView(activity);
        l2.setText("Ende");
        for (android.widget.TextView l : new android.widget.TextView[]{l1, l2}) {
            l.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 13);
            l.setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhiteBlackText));
            l.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), 0);
        }
        box.addView(l1);
        box.addView(startBar, org.telegram.ui.Components.LayoutHelper.createLinear(org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT, 8, 0, 8, 0));
        box.addView(l2);
        box.addView(endBar, org.telegram.ui.Components.LayoutHelper.createLinear(org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT, 8, 0, 8, 8));
        play.setOnClickListener(v -> {
            if (player[0] != null) {
                stop.run();
                play.setText("▶  Anhören");
                return;
            }
            try {
                android.media.MediaPlayer mp = new android.media.MediaPlayer();
                mp.setDataSource(path);
                mp.setOnCompletionListener(m -> {
                    stop.run();
                    play.setText("▶  Anhören");
                });
                mp.prepare();
                mp.seekTo(range[0]);
                mp.start();
                player[0] = mp;
                play.setText("■  Stopp");
                // am gewaehlten Ende anhalten
                watcher[0] = new Runnable() {
                    @Override public void run() {
                        try {
                            if (player[0] != null && player[0].getCurrentPosition() >= range[1]) {
                                stop.run();
                                play.setText("▶  Anhören");
                                return;
                            }
                        } catch (Throwable ignore) {}
                        if (player[0] != null) AndroidUtilities.runOnUIThread(this, 100);
                    }
                };
                AndroidUtilities.runOnUIThread(watcher[0], 100);
            } catch (Throwable e) {
                stop.run();
                play.setText("Abspielen nicht möglich");
            }
        });
        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(activity);
        builder.setTitle(mark("Sprachnachricht senden?"));
        builder.setView(box);
        builder.setPositiveButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.Send), (d, w) -> {
            decided[0] = true;
            stop.run();
            final boolean cut = range[0] > 100 || range[1] < total - 100;
            if (!cut) {
                onSend.run();
                return;
            }
            final long from = range[0], to = range[1];
            org.telegram.messenger.Utilities.globalQueue.postRunnable(() -> {
                String tmp = path + ".cut.ogg";
                boolean ok = false;
                try {
                    ok = org.telegram.messenger.MediaController.cropOpusFile(path, tmp, from, to);
                    if (ok) {
                        java.io.File src = new java.io.File(path);
                        src.delete();
                        ok = new java.io.File(tmp).renameTo(src);
                    }
                    if (ok && doc != null) {
                        for (org.telegram.tgnet.TLRPC.DocumentAttribute a : doc.attributes) {
                            if (a instanceof org.telegram.tgnet.TLRPC.TL_documentAttributeAudio) {
                                a.waveform = org.telegram.messenger.MediaController.getWaveform(path);
                                a.duration = (to - from) / 1000.0;
                            }
                        }
                        doc.size = new java.io.File(path).length();
                    }
                } catch (Throwable e) {
                    org.telegram.messenger.FileLog.e(e);
                }
                AndroidUtilities.runOnUIThread(onSend);
            });
        });
        builder.setNegativeButton("Verwerfen", (d, w) -> {
            decided[0] = true;
            stop.run();
            onDiscard.run();
        });
        org.telegram.ui.ActionBar.AlertDialog dialog = builder.create();
        f.showDialog(dialog, d -> {
            stop.run();
            if (!decided[0]) {
                // Fenster weggewischt: nichts senden, Aufnahme verwerfen
                decided[0] = true;
                onDiscard.run();
            }
        });
    }

    /** Rueckfrage vor dem Senden eines Stickers/GIFs, mit kleiner Vorschau. */
    public static void confirmSend(android.app.Activity activity, String title, org.telegram.tgnet.TLRPC.Document doc, Runnable onConfirm) {
        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(activity);
        builder.setTitle(mark(title));
        if (doc != null) {
            org.telegram.ui.Components.BackupImageView image = new org.telegram.ui.Components.BackupImageView(activity);
            org.telegram.tgnet.TLRPC.PhotoSize thumb = org.telegram.messenger.FileLoader.getClosestPhotoSizeWithSize(doc.thumbs, 320);
            boolean isSticker = org.telegram.messenger.MessageObject.isStickerDocument(doc) || org.telegram.messenger.MessageObject.isAnimatedStickerDocument(doc, true);
            image.setImage(isSticker ? org.telegram.messenger.ImageLocation.getForDocument(doc) : org.telegram.messenger.ImageLocation.getForDocument(thumb, doc), "160_160", org.telegram.messenger.ImageLocation.getForDocument(thumb, doc), "160_160", 0, doc);
            android.widget.FrameLayout box = new android.widget.FrameLayout(activity);
            box.addView(image, org.telegram.ui.Components.LayoutHelper.createFrame(140, 140, android.view.Gravity.CENTER, 0, 8, 0, 0));
            builder.setView(box);
        }
        builder.setPositiveButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.Send), (d, w) -> onConfirm.run());
        builder.setNegativeButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.Cancel), null);
        builder.show();
    }

    /** Nachrichten-Details: IDs, genaue Zeiten, Weiterleitungsherkunft, Datei-Infos. */
    public static void showMessageDetails(org.telegram.ui.ActionBar.BaseFragment fragment, org.telegram.messenger.MessageObject mo) {
        if (fragment == null || mo == null || mo.messageOwner == null || fragment.getParentActivity() == null) {
            return;
        }
        final int account = fragment.getCurrentAccount();
        org.telegram.messenger.MessagesController mc = org.telegram.messenger.MessagesController.getInstance(account);
        org.telegram.tgnet.TLRPC.Message m = mo.messageOwner;
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", java.util.Locale.GERMANY);
        StringBuilder sb = new StringBuilder();
        sb.append("Nachrichten-ID: ").append(m.id);
        sb.append("\nChat-ID: ").append(mo.getDialogId());
        long sender = mo.getSenderId();
        if (sender != 0) {
            sb.append("\nAbsender: ").append(org.telegram.ui.spark.SparkNotesActivity.dialogName(account, sender)).append(" (ID ").append(sender).append(")");
        }
        sb.append("\nGesendet: ").append(f.format(new java.util.Date(m.date * 1000L)));
        if (m.edit_date > 0) {
            sb.append("\nBearbeitet: ").append(f.format(new java.util.Date(m.edit_date * 1000L)));
        }
        if (m.fwd_from != null) {
            String from = m.fwd_from.from_name;
            long fromId = m.fwd_from.from_id != null ? org.telegram.messenger.DialogObject.getPeerDialogId(m.fwd_from.from_id) : 0;
            if (fromId != 0) {
                from = org.telegram.ui.spark.SparkNotesActivity.dialogName(account, fromId) + " (ID " + fromId + ")";
            }
            sb.append("\nWeitergeleitet von: ").append(from == null ? "verborgen" : from);
            if (m.fwd_from.date > 0) {
                sb.append("\nUrsprünglich gesendet: ").append(f.format(new java.util.Date(m.fwd_from.date * 1000L)));
            }
            if (m.fwd_from.channel_post != 0) {
                sb.append("\nOriginal-Beitrag: #").append(m.fwd_from.channel_post);
            }
        }
        if (m.reply_to != null && m.reply_to.reply_to_msg_id != 0) {
            sb.append("\nAntwort auf: #").append(m.reply_to.reply_to_msg_id);
        }
        if (m.views > 0) {
            sb.append("\nAufrufe: ").append(m.views);
        }
        if (m.forwards > 0) {
            sb.append("\nWeitergeleitet: ").append(m.forwards).append("×");
        }
        org.telegram.tgnet.TLRPC.Document doc = mo.getDocument();
        if (doc != null) {
            String name = org.telegram.messenger.FileLoader.getDocumentFileName(doc);
            sb.append("\nDatei: ").append(android.text.TextUtils.isEmpty(name) ? "(ohne Name)" : name);
            sb.append("\nGröße: ").append(AndroidUtilities.formatFileSize(doc.size));
            if (!android.text.TextUtils.isEmpty(doc.mime_type)) {
                sb.append("\nTyp: ").append(doc.mime_type);
            }
            sb.append("\nDatei-DC: DC").append(doc.dc_id);
        } else if (m.media instanceof org.telegram.tgnet.TLRPC.TL_messageMediaPhoto && m.media.photo != null) {
            org.telegram.tgnet.TLRPC.PhotoSize big = org.telegram.messenger.FileLoader.getClosestPhotoSizeWithSize(m.media.photo.sizes, 4096);
            if (big != null) {
                sb.append("\nBild: ").append(big.w).append("×").append(big.h).append(", ").append(AndroidUtilities.formatFileSize(big.size));
            }
            sb.append("\nDatei-DC: DC").append(m.media.photo.dc_id);
        }
        final String text = sb.toString();
        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(mark("Nachrichten-Details"));
        builder.setMessage(text);
        builder.setPositiveButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.OK), null);
        builder.setNeutralButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.Copy), (d, w) -> {
            AndroidUtilities.addToClipboard(text);
            org.telegram.ui.Components.BulletinFactory.of(fragment).createCopyBulletin(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.TextCopied)).show();
        });
        fragment.showDialog(builder.create());
    }

    /** "Profilbild vom 07.11.2026, 14:32" */
    public static String photoDate(int unixTime) {
        return "Profilbild vom " + new java.text.SimpleDateFormat("dd.MM.yyyy, HH:mm", java.util.Locale.GERMANY).format(new java.util.Date(unixTime * 1000L));
    }

    /** Laedt ein Bild verkleinert (laengste Seite etwa maxSize Pixel). */
    public static Bitmap decode(String path, int maxSize) {
        if (path == null) {
            return null;
        }
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            int sample = 1;
            while (Math.max(o.outWidth, o.outHeight) / (sample * 2) >= maxSize) {
                sample *= 2;
            }
            o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            return BitmapFactory.decodeFile(path, o);
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    /** Runde, quadratisch zugeschnittene Miniatur in Groesse sizeDp. */
    public static Drawable roundThumb(String path, int sizeDp) {
        int size = AndroidUtilities.dp(sizeDp);
        Bitmap b = decode(path, size * 2);
        if (b == null) {
            return null;
        }
        int side = Math.min(b.getWidth(), b.getHeight());
        Bitmap square = Bitmap.createBitmap(b, (b.getWidth() - side) / 2, (b.getHeight() - side) / 2, side, side);
        Bitmap scaled = Bitmap.createScaledBitmap(square, size, size, true);
        return new CircleBitmapDrawable(scaled);
    }

    /** Rundes Bild, das Einfaerbungen (ColorFilter) der Listen ignoriert. */
    public static class CircleBitmapDrawable extends Drawable {
        private final Bitmap bitmap;
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG | android.graphics.Paint.FILTER_BITMAP_FLAG);

        CircleBitmapDrawable(Bitmap bitmap) {
            this.bitmap = bitmap;
            paint.setShader(new android.graphics.BitmapShader(bitmap, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP));
            setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        }

        @Override
        public void draw(android.graphics.Canvas canvas) {
            android.graphics.Rect b = getBounds();
            canvas.save();
            canvas.translate(b.left, b.top);
            float r = Math.min(bitmap.getWidth(), bitmap.getHeight()) / 2f;
            canvas.drawCircle(bitmap.getWidth() / 2f, bitmap.getHeight() / 2f, r, paint);
            canvas.restore();
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            // bewusst ignoriert: Profilbilder sollen nicht grau eingefaerbt werden
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return bitmap.getWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            return bitmap.getHeight();
        }
    }

    /** Zeigt ein Bild bildschirmfuellend; Tippen schliesst. */
    public static void showFullscreen(Context context, String path) {
        Bitmap b = decode(path, 2048);
        if (b == null || context == null) {
            return;
        }
        final Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setBackgroundColor(Color.BLACK);
        image.setImageBitmap(b);
        image.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(image, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }
        dialog.show();
    }

    /**
     * Kopiert eine Datei nach „Download/Sparkgram“ (Android 10+: MediaStore, sonst direkt).
     * onDone bekommt den angezeigten Pfad oder null bei Fehler (auf dem UI-Thread).
     */
    public static void saveToDownloads(java.io.File src, String name, String mime, org.telegram.messenger.Utilities.Callback<String> onDone) {
        org.telegram.messenger.Utilities.globalQueue.postRunnable(() -> {
            String shown = null;
            try {
                android.content.Context ctx = org.telegram.messenger.ApplicationLoader.applicationContext;
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    android.content.ContentValues v = new android.content.ContentValues();
                    v.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name);
                    if (mime != null) v.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mime);
                    v.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/Sparkgram");
                    android.net.Uri uri = ctx.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    if (uri == null) throw new java.io.IOException("MediaStore insert fehlgeschlagen");
                    try (java.io.InputStream in = new java.io.FileInputStream(src); java.io.OutputStream out = ctx.getContentResolver().openOutputStream(uri)) {
                        byte[] buf = new byte[64 * 1024];
                        int n;
                        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    }
                    org.telegram.messenger.spark.SparkFiles.record(name, uri.toString(), mime);
                } else {
                    java.io.File dir = new java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "Sparkgram");
                    dir.mkdirs();
                    java.io.File dest = new java.io.File(dir, name);
                    try (java.io.InputStream in = new java.io.FileInputStream(src); java.io.OutputStream out = new java.io.FileOutputStream(dest)) {
                        byte[] buf = new byte[64 * 1024];
                        int n;
                        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    }
                    try {
                        android.net.Uri fu = androidx.core.content.FileProvider.getUriForFile(ctx, org.telegram.messenger.ApplicationLoader.getApplicationId() + ".provider", dest);
                        org.telegram.messenger.spark.SparkFiles.record(name, fu.toString(), mime);
                    } catch (Throwable ignore) {
                    }
                }
                shown = "Download/Sparkgram/" + name;
            } catch (Throwable e) {
                org.telegram.messenger.FileLog.e(e);
            }
            final String result = shown;
            AndroidUtilities.runOnUIThread(() -> {
                if (onDone != null) onDone.run(result);
            });
        });
    }

    /** Wie saveToDownloads, mit Hinweis unten im Bildschirm. */
    public static void saveToDownloads(org.telegram.ui.ActionBar.BaseFragment fragment, java.io.File src, String name, String mime) {
        saveToDownloads(src, name, mime, path -> {
            if (path != null) {
                org.telegram.ui.Components.BulletinFactory.of(fragment).createSimpleBulletin(org.telegram.messenger.R.raw.ic_download, "✦ Gespeichert: " + path).show();
            } else {
                org.telegram.ui.Components.BulletinFactory.of(fragment).createErrorBulletin("Speichern fehlgeschlagen").show();
            }
        });
    }
}

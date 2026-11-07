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
        RoundedBitmapDrawable d = RoundedBitmapDrawableFactory.create(ApplicationLoader.applicationContext.getResources(), scaled);
        d.setCircular(true);
        d.setBounds(0, 0, size, size);
        return d;
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
}

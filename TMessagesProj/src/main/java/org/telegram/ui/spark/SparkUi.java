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
}

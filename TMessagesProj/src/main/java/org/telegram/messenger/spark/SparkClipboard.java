package org.telegram.messenger.spark;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

/** Zwischenablage-Schutz: aus Sparkgram Kopiertes nach einer Weile wieder loeschen (falls unveraendert). */
public class SparkClipboard {

    public static final int[] SECONDS = {30, 60, 120, 300};

    public static int delaySeconds() {
        return SparkConfig.prefs().getInt("clipboardSeconds", 60);
    }

    public static void setDelaySeconds(int s) {
        SparkConfig.prefs().edit().putInt("clipboardSeconds", s).apply();
    }

    private static Runnable pending;

    public static void onCopied(CharSequence text) {
        if (text == null || !SparkConfig.get(SparkConfig.CLIPBOARD_CLEAR)) return;
        final String copied = text.toString();
        AndroidUtilities.runOnUIThread(() -> {
            if (pending != null) AndroidUtilities.cancelRunOnUIThread(pending);
            pending = () -> {
                pending = null;
                try {
                    ClipboardManager cm = (ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = cm.getPrimaryClip();
                    // nur loeschen, wenn noch unser Text drin ist
                    if (clip != null && clip.getItemCount() > 0) {
                        CharSequence now = clip.getItemAt(0).coerceToText(ApplicationLoader.applicationContext);
                        if (now != null && !copied.equals(now.toString())) return;
                    }
                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                        cm.clearPrimaryClip();
                    } else {
                        cm.setPrimaryClip(ClipData.newPlainText("", ""));
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            };
            AndroidUtilities.runOnUIThread(pending, delaySeconds() * 1000L);
        });
    }
}

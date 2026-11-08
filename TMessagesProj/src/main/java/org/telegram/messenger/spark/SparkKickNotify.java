package org.telegram.messenger.spark;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.ui.LaunchActivity;

/** Meldung, wenn man aus einer Gruppe entfernt wurde: Einblendung in der App und/oder Android-Benachrichtigung. */
public class SparkKickNotify {

    private static final String CHANNEL_ID = "sparkgram_kicked";

    /** Pro Gruppe hoechstens einmal pro Stunde melden (Telegram schickt die Aenderung manchmal mehrfach). */
    public static boolean wasNotified(int account, long dialogId) {
        long t = SparkConfig.prefs().getLong("kickNotifiedAt_" + account + "_" + dialogId, 0);
        return System.currentTimeMillis() - t < 3600_000L;
    }

    public static void onKicked(int account, long dialogId) {
        if (!SparkConfig.get(SparkConfig.KICK_NOTIFY)) return;
        SparkConfig.prefs().edit().putLong("kickNotifiedAt_" + account + "_" + dialogId, System.currentTimeMillis()).apply();
        final String name = org.telegram.ui.spark.SparkNotesActivity.dialogName(account, dialogId);
        // Einblendung in der App
        if (SparkConfig.prefs().getBoolean("kickNotifyInApp", true)) {
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    org.telegram.ui.ActionBar.BaseFragment f = LaunchActivity.getLastFragment();
                    if (f != null && f.getParentActivity() != null) {
                        org.telegram.ui.Components.BulletinFactory.of(f).createSimpleBulletin(R.raw.ic_ban, "🔨 Du wurdest aus „" + name + "“ entfernt", "Öffnen", () -> {
                            android.os.Bundle args = new android.os.Bundle();
                            args.putLong("chat_id", -dialogId);
                            f.presentFragment(new org.telegram.ui.ChatActivity(args));
                        }).show();
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            });
        }
        // Android-Benachrichtigung
        if (SparkConfig.prefs().getBoolean("kickNotifySystem", true)) {
            try {
                Context ctx = ApplicationLoader.applicationContext;
                NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL_ID) == null) {
                    NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Sparkgram: aus Gruppe entfernt", NotificationManager.IMPORTANCE_HIGH);
                    nm.createNotificationChannel(ch);
                }
                Intent intent = new Intent(ctx, LaunchActivity.class);
                intent.setAction("com.tmessages.openchat" + Math.random() + Integer.MAX_VALUE);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("chatId", -dialogId);
                intent.putExtra("currentAccount", account);
                PendingIntent pi = PendingIntent.getActivity(ctx, (int) (System.currentTimeMillis() & 0xffff), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
                NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle("✦ " + name)
                    .setContentText("🔨 Du wurdest aus der Gruppe entfernt")
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentIntent(pi);
                nm.notify(CHANNEL_ID.hashCode() + (int) (dialogId % 100000), b.build());
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
    }
}

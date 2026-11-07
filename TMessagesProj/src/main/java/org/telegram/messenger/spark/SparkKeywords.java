package org.telegram.messenger.spark;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Schlagwort-Alarm: meldet eingehende Nachrichten, die ein Schlagwort enthalten,
 * und sammelt die Treffer (pro Konto) mit Chat und Nachricht zum Hinspringen.
 */
public class SparkKeywords {

    private static final String CHANNEL_ID = "sparkgram_keywords";
    private static final int MAX_HITS = 300;

    public static class Hit {
        public long dialogId;
        public int messageId;
        public long senderId;
        public String keyword;
        public String text;
        public int date;
    }

    private static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_keywords_" + account, Context.MODE_PRIVATE);
    }

    // ---------------------------------------------------------------- Schlagwoerter

    public static ArrayList<String> keywords(int account) {
        ArrayList<String> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(account).getString("words", "[]"));
            for (int i = 0; i < a.length(); i++) list.add(a.getString(i));
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public static void setKeywords(int account, ArrayList<String> words) {
        JSONArray a = new JSONArray();
        for (String w : words) a.put(w);
        prefs(account).edit().putString("words", a.toString()).apply();
        cache[account] = null;
    }

    private static final String[][] cache = new String[UserConfig.MAX_ACCOUNT_COUNT][];

    private static String[] lowerWords(int account) {
        String[] c = cache[account];
        if (c == null) {
            ArrayList<String> w = keywords(account);
            c = new String[w.size()];
            for (int i = 0; i < c.length; i++) c[i] = w.get(i).toLowerCase(Locale.ROOT);
            cache[account] = c;
        }
        return c;
    }

    // ---------------------------------------------------------------- Treffer

    public static ArrayList<Hit> hits(int account) {
        ArrayList<Hit> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(account).getString("hits", "[]"));
            for (int i = a.length() - 1; i >= 0; i--) {
                JSONObject o = a.getJSONObject(i);
                Hit h = new Hit();
                h.dialogId = o.optLong("d");
                h.messageId = o.optInt("m");
                h.senderId = o.optLong("s");
                h.keyword = o.optString("k");
                h.text = o.optString("t");
                h.date = o.optInt("time");
                list.add(h);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public static void clearHits(int account) {
        prefs(account).edit().remove("hits").apply();
    }

    private static synchronized void addHit(int account, Hit h) {
        try {
            JSONArray a = new JSONArray(prefs(account).getString("hits", "[]"));
            JSONObject o = new JSONObject();
            o.put("d", h.dialogId);
            o.put("m", h.messageId);
            o.put("s", h.senderId);
            o.put("k", h.keyword);
            o.put("t", h.text);
            o.put("time", h.date);
            a.put(o);
            JSONArray trimmed = a;
            if (a.length() > MAX_HITS) {
                trimmed = new JSONArray();
                for (int i = a.length() - MAX_HITS; i < a.length(); i++) trimmed.put(a.get(i));
            }
            prefs(account).edit().putString("hits", trimmed.toString()).apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /** Aus der Update-Verarbeitung aufgerufen (Hintergrund-Thread). */
    public static void check(int account, TLRPC.Message m) {
        if (m == null || m.out || m.action != null || TextUtils.isEmpty(m.message) || !SparkConfig.get(SparkConfig.KEYWORDS)) {
            return;
        }
        String[] words = lowerWords(account);
        if (words.length == 0) {
            return;
        }
        String lower = m.message.toLowerCase(Locale.ROOT);
        String found = null;
        for (String w : words) {
            if (!w.isEmpty() && lower.contains(w)) {
                found = w;
                break;
            }
        }
        if (found == null) {
            return;
        }
        long did = MessageObject.getDialogId(m);
        // gleiche Nachricht kann ueber mehrere Wege kommen (Update, Kurz-Update, Nachladen)
        synchronized (SEEN) {
            if (!SEEN.add(account + "_" + did + "_" + m.id)) {
                return;
            }
            if (SEEN.size() > 2000) {
                SEEN.clear();
            }
        }
        Hit h = new Hit();
        h.dialogId = did;
        h.messageId = m.id;
        h.senderId = m.from_id != null ? org.telegram.messenger.DialogObject.getPeerDialogId(m.from_id) : h.dialogId;
        h.keyword = found;
        h.text = m.message.length() > 300 ? m.message.substring(0, 300) : m.message;
        h.date = m.date;
        addHit(account, h);
        notify(account, h);
    }

    private static final java.util.HashSet<String> SEEN = new java.util.HashSet<>();

    private static void notify(int account, Hit h) {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Sparkgram Schlagwort-Alarm", NotificationManager.IMPORTANCE_HIGH);
                ch.enableVibration(true);
                ch.enableLights(true);
                nm.createNotificationChannel(ch);
            }
            Intent intent = new Intent(ctx, LaunchActivity.class);
            intent.setAction("com.tmessages.openchat" + Math.random() + Integer.MAX_VALUE);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (h.dialogId > 0) {
                intent.putExtra("userId", h.dialogId);
            } else {
                intent.putExtra("chatId", -h.dialogId);
            }
            intent.putExtra("currentAccount", account);
            PendingIntent pi = PendingIntent.getActivity(ctx, (int) (System.currentTimeMillis() & 0xffff), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
            String chatName = org.telegram.ui.spark.SparkNotesActivity.dialogName(account, h.dialogId);
            String sender = h.senderId != h.dialogId ? org.telegram.ui.spark.SparkNotesActivity.dialogName(account, h.senderId) + ": " : "";
            NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("✦ Schlagwort: „" + h.keyword + "“ in " + chatName)
                .setContentText(sender + h.text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(sender + h.text))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setWhen(h.date * 1000L)
                .setShowWhen(true)
                .setContentIntent(pi);
            nm.notify(CHANNEL_ID.hashCode() + (int) (h.dialogId % 1000) * 1000 + (h.messageId % 1000), b.build());
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}

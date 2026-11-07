package org.telegram.messenger.spark;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Online-Protokoll: merkt sich Status-Wechsel von Personen, sobald Telegram sie meldet.
 * Echte Zeiten (online / zuletzt online um …) und verborgene Angaben („kürzlich“, „letzte Woche“ …)
 * werden getrennt gespeichert – verborgene zaehlen nie als „zuletzt echt gesehen“.
 */
public class SparkOnlineLog {

    public static final int KIND_ONLINE = 1;   // value = bis wann online (expires, Sekunden)
    public static final int KIND_OFFLINE = 2;  // time  = zuletzt online (was_online, Sekunden)
    public static final int KIND_RECENTLY = 3; // time  = wann wir das gesehen haben
    public static final int KIND_LAST_WEEK = 4;
    public static final int KIND_LAST_MONTH = 5;

    private static final long KEEP_DAYS = 30;

    public static class Event {
        public int kind;
        public long time;  // Sekunden
        public long value;
    }

    private static final DispatchQueue queue = new DispatchQueue("sparkOnlineLog");
    private static final HashMap<Integer, SparkOnlineLog> instances = new HashMap<>();

    public static synchronized SparkOnlineLog get(int account) {
        SparkOnlineLog l = instances.get(account);
        if (l == null) {
            instances.put(account, l = new SparkOnlineLog(account));
        }
        return l;
    }

    private final Helper helper;
    // userId -> "kind|time" des letzten Eintrags (verhindert doppelte Eintraege)
    private final HashMap<Long, String> last = new HashMap<>();
    private boolean cleaned;

    private static class Helper extends SQLiteOpenHelper {
        Helper(int account) {
            super(ApplicationLoader.applicationContext, "sparkgram_online_" + account + ".db", null, 2);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE ev(user_id INTEGER, time INTEGER, kind INTEGER, value INTEGER)");
            db.execSQL("CREATE INDEX ev_user ON ev(user_id, time)");
            onUpgrade(db, 1, 2);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int o, int n) {
            if (o < 2) {
                // letzte Aktivitaet in gemeinsamen Chats (Nachricht/Tippen)
                db.execSQL("CREATE TABLE IF NOT EXISTS active(user_id INTEGER PRIMARY KEY, time INTEGER, dialog_id INTEGER, what INTEGER)");
            }
        }
    }

    private SparkOnlineLog(int account) {
        helper = new Helper(account);
    }

    /** Aus MessagesController (Status-Update bzw. putUser) aufgerufen. */
    public void onStatus(long userId, TLRPC.UserStatus status) {
        if (status == null || userId <= 0 || !SparkConfig.get(SparkConfig.ONLINE_LOG)) {
            return;
        }
        final long now = System.currentTimeMillis() / 1000;
        final int kind;
        final long time;
        long value = 0;
        if (status instanceof TLRPC.TL_userStatusOnline) {
            kind = KIND_ONLINE;
            time = now;
            value = status.expires;
        } else if (status instanceof TLRPC.TL_userStatusOffline) {
            kind = KIND_OFFLINE;
            time = status.expires; // bei Offline steht hier "zuletzt online"
            if (time <= 0) return;
        } else if (status instanceof TLRPC.TL_userStatusRecently) {
            kind = KIND_RECENTLY;
            time = now;
        } else if (status instanceof TLRPC.TL_userStatusLastWeek) {
            kind = KIND_LAST_WEEK;
            time = now;
        } else if (status instanceof TLRPC.TL_userStatusLastMonth) {
            kind = KIND_LAST_MONTH;
            time = now;
        } else {
            return;
        }
        // gleiche Lage nicht doppelt speichern (online: nur Wechsel; offline: nur neue Zeit)
        final String key = kind == KIND_OFFLINE ? kind + "|" + time : String.valueOf(kind);
        synchronized (last) {
            if (key.equals(last.get(userId))) {
                return;
            }
            last.put(userId, key);
        }
        final long v = value;
        queue.postRunnable(() -> {
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                if (!cleaned) {
                    cleaned = true;
                    db.delete("ev", "time < ?", new String[]{String.valueOf(now - KEEP_DAYS * 86400)});
                }
                // beim ersten Mal nach dem Start: letzten Stand aus der Datenbank pruefen
                try (Cursor c = db.rawQuery("SELECT kind, time FROM ev WHERE user_id = ? ORDER BY rowid DESC LIMIT 1", new String[]{String.valueOf(userId)})) {
                    if (c.moveToFirst()) {
                        int k = c.getInt(0);
                        long t = c.getLong(1);
                        if (k == kind && (kind != KIND_OFFLINE || t == time)) {
                            return;
                        }
                    }
                }
                ContentValues cv = new ContentValues();
                cv.put("user_id", userId);
                cv.put("time", time);
                cv.put("kind", kind);
                cv.put("value", v);
                db.insert("ev", null, cv);
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    /** Alle Eintraege einer Person, aelteste zuerst. */
    public ArrayList<Event> events(long userId) {
        ArrayList<Event> list = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT kind, time, value FROM ev WHERE user_id = ? ORDER BY rowid ASC",
                new String[]{String.valueOf(userId)})) {
            while (c.moveToNext()) {
                Event e = new Event();
                e.kind = c.getInt(0);
                e.time = c.getLong(1);
                e.value = c.getLong(2);
                list.add(e);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public static final int ACT_MESSAGE = 1;
    public static final int ACT_TYPING = 2;

    public static class Activity {
        public long time;
        public long dialogId;
        public int what;
    }

    // userId -> zuletzt gespeicherte Aktivitaetszeit (spart Schreibzugriffe)
    private final HashMap<Long, Long> lastActive = new HashMap<>();

    /** Neue Nachricht einer Person (aus der Update-Verarbeitung). */
    public static void onMessage(int account, TLRPC.Message m) {
        if (m == null || m.out || !(m.from_id instanceof TLRPC.TL_peerUser) || m.from_id.user_id <= 0) {
            return;
        }
        get(account).onActivity(m.from_id.user_id, m.date, org.telegram.messenger.MessageObject.getDialogId(m), ACT_MESSAGE);
    }

    /** Merkt sich die letzte Aktivitaet einer Person (max. alle 30 s ein Schreibzugriff). */
    public void onActivity(long userId, long time, long dialogId, int what) {
        if (userId <= 0 || time <= 0 || !SparkConfig.get(SparkConfig.ONLINE_LOG)) {
            return;
        }
        synchronized (lastActive) {
            Long prev = lastActive.get(userId);
            if (prev != null && time < prev + 30) {
                return;
            }
            lastActive.put(userId, time);
        }
        queue.postRunnable(() -> {
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                db.execSQL("INSERT OR REPLACE INTO active(user_id, time, dialog_id, what) SELECT ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM active WHERE user_id = ? AND time > ?)",
                    new Object[]{userId, time, dialogId, what, userId, time});
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public Activity lastActivity(long userId) {
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT time, dialog_id, what FROM active WHERE user_id = ?", new String[]{String.valueOf(userId)})) {
            if (c.moveToFirst()) {
                Activity a = new Activity();
                a.time = c.getLong(0);
                a.dialogId = c.getLong(1);
                a.what = c.getInt(2);
                return a;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    /**
     * Zeile "zuletzt gesehen/aktiv" fuer Profil und Chat: neueste Angabe aus Online-Protokoll und Aktivitaet.
     * Liefert null, wenn nichts bekannt ist oder Telegram gerade "online" bzw. eine genauere Zeit zeigt.
     */
    public static String seenLine(int account, TLRPC.User user, boolean compact) {
        if (user == null || user.bot || user.self || !SparkConfig.get(SparkConfig.ONLINE_LOG)) {
            return null;
        }
        TLRPC.UserStatus st = user.status;
        if (st instanceof TLRPC.TL_userStatusOnline && st.expires > System.currentTimeMillis() / 1000) {
            return null;
        }
        SparkOnlineLog log = get(account);
        long online = log.lastReal(user.id);
        Activity act = log.lastActivity(user.id);
        long actTime = act != null ? act.time : 0;
        long best = Math.max(online, actTime);
        if (best <= 0) {
            return null;
        }
        // Telegram zeigt schon eine genaue, neuere Zeit -> nichts doppelt anzeigen
        if (st instanceof TLRPC.TL_userStatusOffline && st.expires >= best) {
            return null;
        }
        String when = relative(best);
        if (actTime >= online && act != null) {
            String chat = org.telegram.ui.spark.SparkNotesActivity.dialogName(account, act.dialogId);
            boolean dm = act.dialogId == user.id;
            String what = act.what == ACT_TYPING ? "tippte" : "schrieb";
            if (compact) {
                return "✦ aktiv " + when;
            }
            return "✦ zuletzt aktiv: " + when + " · " + what + (dm ? " hier im Chat" : " in „" + chat + "“");
        }
        return compact ? "✦ online " + when : "✦ zuletzt online gesehen: " + when;
    }

    /** Letzte echte Zeit (online gesehen bzw. „zuletzt online um“), 0 wenn keine. */
    public long lastReal(long userId) {
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT MAX(time) FROM ev WHERE user_id = ? AND kind IN (1, 2)",
                new String[]{String.valueOf(userId)})) {
            if (c.moveToFirst()) {
                return c.getLong(0);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return 0;
    }

    /** "heute 14:20", "gestern 09:12" oder "05.11. 22:10". */
    public static String relative(long sec) {
        java.util.Calendar now = java.util.Calendar.getInstance();
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(sec * 1000);
        String time = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.GERMANY).format(c.getTime());
        int dDays = now.get(java.util.Calendar.DAY_OF_YEAR) - c.get(java.util.Calendar.DAY_OF_YEAR);
        if (now.get(java.util.Calendar.YEAR) == c.get(java.util.Calendar.YEAR)) {
            if (dDays == 0) return "heute " + time;
            if (dDays == 1) return "gestern " + time;
        }
        return new java.text.SimpleDateFormat("dd.MM.", java.util.Locale.GERMANY).format(c.getTime()) + " " + time;
    }

    public void clear(long userId) {
        synchronized (last) {
            last.remove(userId);
        }
        queue.postRunnable(() -> {
            try {
                helper.getWritableDatabase().delete("ev", "user_id = ?", new String[]{String.valueOf(userId)});
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }
}

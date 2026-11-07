package org.telegram.messenger.spark;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Merkt sich Benutzernamen (@handle) und Anzeigenamen von Personen, sobald Sparkgram sie sieht.
 * Aendert jemand seinen Namen, bleibt der alte Wert mit Zeitraum erhalten.
 * Telegram selbst liefert keinen Verlauf – erfasst wird alles ab Installation.
 */
public class SparkNameHistory {

    public static final int KIND_USERNAME = 1;
    public static final int KIND_NAME = 2;

    public static class Item {
        public int kind;
        public String value;
        public long firstSeen;
        public long lastSeen;
    }

    private static final DispatchQueue queue = new DispatchQueue("sparkNameHistory");
    private static final HashMap<Integer, SparkNameHistory> instances = new HashMap<>();

    public static synchronized SparkNameHistory get(int account) {
        SparkNameHistory h = instances.get(account);
        if (h == null) {
            instances.put(account, h = new SparkNameHistory(account));
        }
        return h;
    }

    private final Helper helper;
    // userId -> zuletzt gespeicherter Stand "username|name" (spart Datenbankzugriffe)
    private final HashMap<Long, String> lastState = new HashMap<>();

    private static class Helper extends SQLiteOpenHelper {
        Helper(int account) {
            super(ApplicationLoader.applicationContext, "sparkgram_names_" + account + ".db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE names(user_id INTEGER, kind INTEGER, value TEXT, first_seen INTEGER, last_seen INTEGER, PRIMARY KEY(user_id, kind, value))");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int o, int n) {
        }
    }

    private SparkNameHistory(int account) {
        helper = new Helper(account);
    }

    /** Wird bei jedem bekannten Nutzer-Update aufgerufen (aus MessagesController.putUser). */
    public void onUser(TLRPC.User user) {
        if (user == null || user.min || user.id <= 0 || !SparkConfig.get(SparkConfig.NAME_HISTORY)) {
            return;
        }
        final String username = UserObject.getPublicUsername(user);
        final String name = UserObject.getUserName(user);
        final String state = (username == null ? "" : username) + "|" + (name == null ? "" : name);
        synchronized (lastState) {
            if (state.equals(lastState.get(user.id))) {
                return;
            }
            lastState.put(user.id, state);
        }
        final long userId = user.id;
        queue.postRunnable(() -> {
            long now = System.currentTimeMillis();
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                if (!TextUtils.isEmpty(username)) {
                    touch(db, userId, KIND_USERNAME, username.toLowerCase(), username, now);
                }
                if (!TextUtils.isEmpty(name)) {
                    touch(db, userId, KIND_NAME, name, name, now);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    private void touch(SQLiteDatabase db, long userId, int kind, String key, String value, long now) {
        ContentValues v = new ContentValues();
        v.put("last_seen", now);
        int rows = db.update("names", v, "user_id = ? AND kind = ? AND value = ?", new String[]{String.valueOf(userId), String.valueOf(kind), value});
        if (rows == 0) {
            v.put("user_id", userId);
            v.put("kind", kind);
            v.put("value", value);
            v.put("first_seen", now);
            db.insertWithOnConflict("names", null, v, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    /** Alle bekannten Werte einer Person, neueste zuerst. */
    public ArrayList<Item> history(long userId, int kind) {
        ArrayList<Item> list = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT value, first_seen, last_seen FROM names WHERE user_id = ? AND kind = ? ORDER BY last_seen DESC",
                new String[]{String.valueOf(userId), String.valueOf(kind)})) {
            while (c.moveToNext()) {
                Item i = new Item();
                i.kind = kind;
                i.value = c.getString(0);
                i.firstSeen = c.getLong(1);
                i.lastSeen = c.getLong(2);
                list.add(i);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }
}

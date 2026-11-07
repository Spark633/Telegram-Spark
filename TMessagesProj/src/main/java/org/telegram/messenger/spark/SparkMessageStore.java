package org.telegram.messenger.spark;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Eigener Speicher fuer gelöschte Nachrichten und Bearbeitungsverlaeufe (pro Konto).
 * Geloeschte Nachrichten bleiben in Telegrams Datenbank stehen und werden hier nur als
 * "geloescht" markiert. Fuer den Verlauf merkt sich Sparkgram den Text jeder gesehenen
 * Nachricht; kommt eine Bearbeitung, wird die alte Fassung gesichert.
 */
public class SparkMessageStore {

    private static final DispatchQueue queue = new DispatchQueue("sparkMessageStore");
    private static final HashMap<Integer, SparkMessageStore> instances = new HashMap<>();

    public static synchronized SparkMessageStore get(int account) {
        SparkMessageStore s = instances.get(account);
        if (s == null) {
            instances.put(account, s = new SparkMessageStore(account));
        }
        return s;
    }

    public static class Version {
        public String text;
        public int editDate;
        public long savedAt;
    }

    private final int account;
    private final Helper helper;
    // dialogId -> geloeschte Nachrichten-IDs (Cache fuer die Anzeige)
    private final HashMap<Long, HashSet<Integer>> deleted = new HashMap<>();
    // dialogId -> selbst geloeschte Nachrichten-IDs
    private final HashMap<Long, HashSet<Integer>> deletedSelf = new HashMap<>();
    // dialogId -> IDs mit Verlauf
    private final HashMap<Long, HashSet<Integer>> withHistory = new HashMap<>();

    private static class Helper extends SQLiteOpenHelper {
        Helper(int account) {
            super(ApplicationLoader.applicationContext, "sparkgram_messages_" + account + ".db", null, 2);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE deleted(dialog_id INTEGER, mid INTEGER, deleted_at INTEGER, by_self INTEGER DEFAULT 0, PRIMARY KEY(dialog_id, mid))");
            db.execSQL("CREATE TABLE known(dialog_id INTEGER, mid INTEGER, text TEXT, edit_date INTEGER, PRIMARY KEY(dialog_id, mid))");
            db.execSQL("CREATE TABLE history(dialog_id INTEGER, mid INTEGER, text TEXT, edit_date INTEGER, saved_at INTEGER)");
            db.execSQL("CREATE INDEX history_idx ON history(dialog_id, mid)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE deleted ADD COLUMN by_self INTEGER DEFAULT 0");
            }
        }
    }

    private SparkMessageStore(int account) {
        this.account = account;
        this.helper = new Helper(account);
    }

    // ---------------------------------------------------------------- Abfragen fuer die Anzeige

    private HashSet<Integer> loadSet(HashMap<Long, HashSet<Integer>> cache, String table, long dialogId) {
        synchronized (cache) {
            HashSet<Integer> set = cache.get(dialogId);
            if (set != null) {
                return set;
            }
            set = new HashSet<>();
            try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT DISTINCT mid FROM " + table + " WHERE dialog_id = ?", new String[]{String.valueOf(dialogId)})) {
                while (c.moveToNext()) {
                    set.add(c.getInt(0));
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
            cache.put(dialogId, set);
            return set;
        }
    }

    public static final int NOT_DELETED = 0;
    public static final int DELETED_BY_OTHERS = 1;
    public static final int DELETED_BY_SELF = 2;

    /** 0 = nicht geloescht, 1 = von anderen geloescht, 2 = von dir geloescht. */
    public int deletedState(long dialogId, int mid) {
        if (!isDeleted(dialogId, mid)) {
            return NOT_DELETED;
        }
        HashSet<Integer> self;
        synchronized (deletedSelf) {
            self = deletedSelf.get(dialogId);
            if (self == null) {
                self = new HashSet<>();
                try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT mid FROM deleted WHERE dialog_id = ? AND by_self = 1", new String[]{String.valueOf(dialogId)})) {
                    while (c.moveToNext()) {
                        self.add(c.getInt(0));
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
                deletedSelf.put(dialogId, self);
            }
            return self.contains(mid) ? DELETED_BY_SELF : DELETED_BY_OTHERS;
        }
    }

    public boolean isDeleted(long dialogId, int mid) {
        if (!SparkConfig.get(SparkConfig.KEEP_DELETED) || mid <= 0) {
            return false;
        }
        HashSet<Integer> set = loadSet(deleted, "deleted", dialogId);
        synchronized (deleted) {
            return set.contains(mid);
        }
    }

    public boolean hasHistory(long dialogId, int mid) {
        if (!SparkConfig.get(SparkConfig.EDIT_HISTORY) || mid <= 0) {
            return false;
        }
        HashSet<Integer> set = loadSet(withHistory, "history", dialogId);
        synchronized (withHistory) {
            return set.contains(mid);
        }
    }

    public ArrayList<Version> history(long dialogId, int mid) {
        ArrayList<Version> list = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT text, edit_date, saved_at FROM history WHERE dialog_id = ? AND mid = ? ORDER BY saved_at", new String[]{String.valueOf(dialogId), String.valueOf(mid)})) {
            while (c.moveToNext()) {
                Version v = new Version();
                v.text = c.getString(0);
                v.editDate = c.getInt(1);
                v.savedAt = c.getLong(2);
                list.add(v);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    // ---------------------------------------------------------------- Ereignisse

    private final HashSet<String> remembered = new HashSet<>();

    /** Nachricht wurde angezeigt: Text fuer spaeteren Verlauf merken. */
    public void remember(MessageObject message) {
        if (message != null) {
            rememberRaw(message.messageOwner);
        }
    }

    /** Nachricht empfangen/angezeigt: Text fuer spaeteren Verlauf merken. */
    public void rememberRaw(TLRPC.Message message) {
        if (!SparkConfig.get(SparkConfig.EDIT_HISTORY) || message == null || message.id <= 0 || message.action != null) {
            return;
        }
        final long dialogId = MessageObject.getDialogId(message);
        final int mid = message.id;
        synchronized (remembered) {
            if (!remembered.add(dialogId + "_" + mid)) {
                return;
            }
        }
        final String text = message.message;
        final int editDate = message.edit_date;
        queue.postRunnable(() -> {
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                String known = null;
                try (Cursor c = db.rawQuery("SELECT text FROM known WHERE dialog_id = ? AND mid = ?", new String[]{String.valueOf(dialogId), String.valueOf(mid)})) {
                    if (c.moveToNext()) {
                        known = c.getString(0);
                    }
                }
                if (known == null) {
                    ContentValues v = new ContentValues();
                    v.put("dialog_id", dialogId);
                    v.put("mid", mid);
                    v.put("text", text == null ? "" : text);
                    v.put("edit_date", editDate);
                    db.insertWithOnConflict("known", null, v, SQLiteDatabase.CONFLICT_IGNORE);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    /** Bearbeitung empfangen: vorherige Fassung sichern, falls sich der Text geaendert hat. */
    public void onEdited(TLRPC.Message message) {
        if (!SparkConfig.get(SparkConfig.EDIT_HISTORY) || message == null || message.id <= 0) {
            return;
        }
        final long dialogId = MessageObject.getDialogId(message);
        final int mid = message.id;
        final String newText = message.message == null ? "" : message.message;
        final int editDate = message.edit_date;
        queue.postRunnable(() -> {
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                String known = null;
                int knownEdit = 0;
                try (Cursor c = db.rawQuery("SELECT text, edit_date FROM known WHERE dialog_id = ? AND mid = ?", new String[]{String.valueOf(dialogId), String.valueOf(mid)})) {
                    if (c.moveToNext()) {
                        known = c.getString(0);
                        knownEdit = c.getInt(1);
                    }
                }
                ContentValues k = new ContentValues();
                k.put("dialog_id", dialogId);
                k.put("mid", mid);
                k.put("text", newText);
                k.put("edit_date", editDate);
                db.insertWithOnConflict("known", null, k, SQLiteDatabase.CONFLICT_REPLACE);
                if (known != null && !TextUtils.equals(known, newText)) {
                    ContentValues h = new ContentValues();
                    h.put("dialog_id", dialogId);
                    h.put("mid", mid);
                    h.put("text", known);
                    h.put("edit_date", knownEdit);
                    h.put("saved_at", System.currentTimeMillis());
                    db.insert("history", null, h);
                    synchronized (withHistory) {
                        HashSet<Integer> set = withHistory.get(dialogId);
                        if (set != null) {
                            set.add(mid);
                        }
                    }
                    notifyChanged();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    /**
     * Loeschung empfangen. Rueckgabe true = Sparkgram behaelt die Nachrichten (Telegram soll sie
     * nicht entfernen). channelId = 0 bei privaten Chats/kleinen Gruppen (Dialog wird nachgeschlagen).
     */
    public boolean onDeleted(long channelId, ArrayList<Integer> mids) {
        if (!SparkConfig.get(SparkConfig.KEEP_DELETED) || mids == null || mids.isEmpty()) {
            return false;
        }
        final ArrayList<Integer> copy = new ArrayList<>(mids);
        if (channelId != 0) {
            store(-channelId, copy);
            return true;
        }
        // Dialog der Nachrichten in Telegrams Datenbank nachschlagen
        MessagesStorage storage = MessagesStorage.getInstance(account);
        storage.getStorageQueue().postRunnable(() -> {
            HashMap<Long, ArrayList<Integer>> byDialog = new HashMap<>();
            SQLiteCursor cursor = null;
            try {
                cursor = storage.getDatabase().queryFinalized(String.format(java.util.Locale.US,
                    "SELECT uid, mid FROM messages_v2 WHERE mid IN(%s) AND is_channel = 0", TextUtils.join(",", copy)));
                while (cursor.next()) {
                    long uid = cursor.longValue(0);
                    int mid = cursor.intValue(1);
                    ArrayList<Integer> list = byDialog.get(uid);
                    if (list == null) {
                        byDialog.put(uid, list = new ArrayList<>());
                    }
                    list.add(mid);
                }
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
            for (HashMap.Entry<Long, ArrayList<Integer>> e : byDialog.entrySet()) {
                store(e.getKey(), e.getValue());
            }
        });
        return true;
    }

    /**
     * Eigene Loeschung in Sparkgram. Rueckgabe true = lokal behalten (Telegram loescht nur auf dem Server).
     */
    public boolean onOwnDelete(long dialogId, ArrayList<Integer> mids) {
        if (!SparkConfig.get(SparkConfig.KEEP_DELETED) || !SparkConfig.get(SparkConfig.KEEP_OWN_DELETED) || mids == null || mids.isEmpty() || dialogId == 0) {
            return false;
        }
        for (Integer mid : mids) {
            if (mid == null || mid <= 0) {
                return false; // noch nicht gesendete Nachrichten wirklich loeschen
            }
        }
        // sofort im Cache markieren, damit die Anzeige direkt stimmt
        synchronized (deleted) {
            HashSet<Integer> set = deleted.get(dialogId);
            if (set != null) set.addAll(mids);
        }
        synchronized (deletedSelf) {
            HashSet<Integer> set = deletedSelf.get(dialogId);
            if (set != null) set.addAll(mids);
        }
        store(dialogId, new ArrayList<>(mids), true);
        return true;
    }

    private void store(long dialogId, ArrayList<Integer> mids) {
        store(dialogId, mids, false);
    }

    private void store(long dialogId, ArrayList<Integer> mids, boolean bySelf) {
        queue.postRunnable(() -> {
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                long now = System.currentTimeMillis();
                db.beginTransaction();
                try {
                    for (int mid : mids) {
                        ContentValues v = new ContentValues();
                        v.put("dialog_id", dialogId);
                        v.put("mid", mid);
                        v.put("deleted_at", now);
                        v.put("by_self", bySelf ? 1 : 0);
                        db.insertWithOnConflict("deleted", null, v, SQLiteDatabase.CONFLICT_IGNORE);
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                synchronized (deleted) {
                    HashSet<Integer> set = deleted.get(dialogId);
                    if (set != null) {
                        set.addAll(mids);
                    }
                }
                notifyChanged();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    /** Wirklich entfernen (z. B. wenn der Nutzer eine als geloescht markierte Nachricht selbst loescht). */
    public void forget(long dialogId, ArrayList<Integer> mids) {
        queue.postRunnable(() -> {
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                for (int mid : mids) {
                    db.delete("deleted", "dialog_id = ? AND mid = ?", new String[]{String.valueOf(dialogId), String.valueOf(mid)});
                }
                synchronized (deleted) {
                    HashSet<Integer> set = deleted.get(dialogId);
                    if (set != null) {
                        set.removeAll(mids);
                    }
                }
                synchronized (deletedSelf) {
                    HashSet<Integer> set = deletedSelf.get(dialogId);
                    if (set != null) {
                        set.removeAll(mids);
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    private void notifyChanged() {
        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_NAME));
    }
}

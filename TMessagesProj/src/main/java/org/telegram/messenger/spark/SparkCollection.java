package org.telegram.messenger.spark;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Eigene Sammlung (unbegrenzt, mit Ordnern): Sticker, GIFs, Sprachnachrichten und andere Medien.
 * Gespeichert wird die ganze Nachricht; gesendet wird spaeter als Kopie ohne Absender.
 */
public class SparkCollection {

    public static final int KIND_STICKER = 1;
    public static final int KIND_GIF = 2;
    public static final int KIND_VOICE = 3;
    public static final int KIND_OTHER = 4;

    public static class Item {
        public long id;
        public String folder;
        public int kind;
        public long dialogId;
        public int msgId;
        public long added;
        public TLRPC.Message message;

        public TLRPC.Document document() {
            return message != null && message.media != null ? message.media.document : null;
        }
    }

    private static final HashMap<Integer, SparkCollection> instances = new HashMap<>();

    public static synchronized SparkCollection get(int account) {
        SparkCollection c = instances.get(account);
        if (c == null) {
            instances.put(account, c = new SparkCollection(account));
        }
        return c;
    }

    private final Helper helper;

    private static class Helper extends SQLiteOpenHelper {
        Helper(int account) {
            super(ApplicationLoader.applicationContext, "sparkgram_collection_" + account + ".db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY AUTOINCREMENT, folder TEXT, kind INTEGER, dialog_id INTEGER, msg_id INTEGER, added INTEGER, data BLOB)");
            db.execSQL("CREATE TABLE folders(name TEXT PRIMARY KEY, pos INTEGER)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int o, int n) {
        }
    }

    private SparkCollection(int account) {
        helper = new Helper(account);
    }

    public static int kindOf(MessageObject mo) {
        if (mo == null) return 0;
        if (mo.isSticker() || mo.isAnimatedSticker() || mo.isVideoSticker()) return KIND_STICKER;
        if (mo.isGif()) return KIND_GIF;
        if (mo.isVoice()) return KIND_VOICE;
        if (mo.getDocument() != null) return KIND_OTHER;
        return 0;
    }

    public static String kindName(int kind) {
        switch (kind) {
            case KIND_STICKER: return "Sticker";
            case KIND_GIF: return "GIFs";
            case KIND_VOICE: return "Sprachnachrichten";
            default: return "Medien";
        }
    }

    /** Fuegt eine Nachricht hinzu. Liefert false, wenn sie schon drin ist oder nichts Passendes enthaelt. */
    public boolean add(MessageObject mo, String folder) {
        int kind = kindOf(mo);
        if (kind == 0 || mo.messageOwner == null) return false;
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            TLRPC.Document doc = mo.getDocument();
            if (doc != null) {
                // dasselbe Dokument nicht doppelt
                for (Item it : list(null)) {
                    TLRPC.Document d = it.document();
                    if (d != null && d.id == doc.id) return false;
                }
            }
            SerializedData data = new SerializedData(mo.messageOwner.getObjectSize());
            mo.messageOwner.serializeToStream(data);
            ContentValues v = new ContentValues();
            v.put("folder", folder == null ? "" : folder);
            v.put("kind", kind);
            v.put("dialog_id", mo.getDialogId());
            v.put("msg_id", mo.getId());
            v.put("added", System.currentTimeMillis() / 1000);
            v.put("data", data.toByteArray());
            data.cleanup();
            db.insert("items", null, v);
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    /** Alle Eintraege (neueste zuerst); folder null = alle, "" = ohne Ordner. */
    public ArrayList<Item> list(String folder) {
        ArrayList<Item> list = new ArrayList<>();
        String where = folder == null ? null : "folder = ?";
        String[] args = folder == null ? null : new String[]{folder};
        try (Cursor c = helper.getReadableDatabase().query("items", new String[]{"id", "folder", "kind", "dialog_id", "msg_id", "added", "data"}, where, args, null, null, "id DESC")) {
            while (c.moveToNext()) {
                Item it = new Item();
                it.id = c.getLong(0);
                it.folder = c.getString(1);
                it.kind = c.getInt(2);
                it.dialogId = c.getLong(3);
                it.msgId = c.getInt(4);
                it.added = c.getLong(5);
                byte[] b = c.getBlob(6);
                if (b != null) {
                    SerializedData d = new SerializedData(b);
                    it.message = TLRPC.Message.TLdeserialize(d, d.readInt32(false), false);
                    d.cleanup();
                }
                if (it.message != null) {
                    list.add(it);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public ArrayList<String> folders() {
        ArrayList<String> list = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT name FROM folders ORDER BY pos, name", null)) {
            while (c.moveToNext()) list.add(c.getString(0));
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public void addFolder(String name) {
        try {
            ContentValues v = new ContentValues();
            v.put("name", name);
            v.put("pos", folders().size());
            helper.getWritableDatabase().insertWithOnConflict("folders", null, v, SQLiteDatabase.CONFLICT_IGNORE);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void removeFolder(String name) {
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            db.delete("folders", "name = ?", new String[]{name});
            ContentValues v = new ContentValues();
            v.put("folder", "");
            db.update("items", v, "folder = ?", new String[]{name});
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void move(long id, String folder) {
        try {
            ContentValues v = new ContentValues();
            v.put("folder", folder == null ? "" : folder);
            helper.getWritableDatabase().update("items", v, "id = ?", new String[]{String.valueOf(id)});
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void remove(long id) {
        try {
            helper.getWritableDatabase().delete("items", "id = ?", new String[]{String.valueOf(id)});
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
}

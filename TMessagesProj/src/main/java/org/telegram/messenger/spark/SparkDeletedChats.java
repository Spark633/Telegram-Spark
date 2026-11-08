package org.telegram.messenger.spark;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/** Gelöschte bzw. verlassene Chats (pro Konto): Name, Benutzername, letzte Nachricht, Zeitpunkt. */
public class SparkDeletedChats {

    public static final int KIND_DELETED = 0;
    public static final int KIND_LEFT = 1;
    public static final int KIND_KICKED = 2;

    public static class Item {
        public int kind;
        public long dialogId;
        public String name;
        public String username;
        public String last;
        public long time;
    }

    // Schneller Zugriff fuer die Chatliste: dialogId -> Art (pro Konto)
    private static final java.util.HashMap<Integer, java.util.HashMap<Long, Integer>> kindCache = new java.util.HashMap<>();

    /** Art (KIND_*) eines erfassten Chats oder -1. */
    public static synchronized int kindOf(int account, long dialogId) {
        java.util.HashMap<Long, Integer> m = kindCache.get(account);
        if (m == null) {
            m = new java.util.HashMap<>();
            for (Item it : list(account)) if (!m.containsKey(it.dialogId)) m.put(it.dialogId, it.kind);
            kindCache.put(account, m);
        }
        Integer k = m.get(dialogId);
        return k == null ? -1 : k;
    }

    /** Geloeschte Privatchats, die lokal behalten werden (im Ordner "Verlassen"). */
    public static synchronized boolean isKept(int account, long dialogId) {
        return prefs(account).getStringSet("kept", new java.util.HashSet<>()).contains(String.valueOf(dialogId));
    }

    public static synchronized void setKept(int account, long dialogId, boolean kept) {
        java.util.HashSet<String> set = new java.util.HashSet<>(prefs(account).getStringSet("kept", new java.util.HashSet<>()));
        if (kept) set.add(String.valueOf(dialogId)); else set.remove(String.valueOf(dialogId));
        prefs(account).edit().putStringSet("kept", set).apply();
    }

    /** Einzelne Chats nur im Ordner "Verlassen" zeigen. */
    public static synchronized boolean isOnlyInFolder(int account, long dialogId) {
        return prefs(account).getStringSet("onlyFolder", new java.util.HashSet<>()).contains(String.valueOf(dialogId));
    }

    public static synchronized void setOnlyInFolder(int account, long dialogId, boolean only) {
        java.util.HashSet<String> set = new java.util.HashSet<>(prefs(account).getStringSet("onlyFolder", new java.util.HashSet<>()));
        if (only) set.add(String.valueOf(dialogId)); else set.remove(String.valueOf(dialogId));
        prefs(account).edit().putStringSet("onlyFolder", set).apply();
    }

    /** Symbol je Art: 🚪 verlassen, 🔨 entfernt, 🗑 geloescht. */
    public static String icon(int kind) {
        return kind == KIND_KICKED ? "🔨" : kind == KIND_LEFT ? "🚪" : "🗑";
    }

    private static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_deleted_chats_" + account, Context.MODE_PRIVATE);
    }

    public static void remember(int account, long dialogId) {
        remember(account, dialogId, KIND_DELETED);
    }

    public static void remember(int account, long dialogId, int kind) {
        if (!SparkConfig.get(SparkConfig.DELETED_CHATS) || dialogId == 0) return;
        try {
            MessagesController mc = MessagesController.getInstance(account);
            Item it = new Item();
            it.dialogId = dialogId;
            it.kind = kind;
            if (dialogId > 0) {
                TLRPC.User u = mc.getUser(dialogId);
                it.name = u != null ? UserObject.getUserName(u) : String.valueOf(dialogId);
                it.username = u != null ? UserObject.getPublicUsername(u) : null;
            } else {
                TLRPC.Chat c = mc.getChat(-dialogId);
                it.name = c != null ? c.title : String.valueOf(dialogId);
                it.username = c != null ? c.username : null;
            }
            ArrayList<MessageObject> last = mc.dialogMessage.get(dialogId);
            if (last != null && !last.isEmpty() && last.get(0) != null && last.get(0).messageText != null) {
                it.last = last.get(0).messageText.toString();
                if (it.last.length() > 200) it.last = it.last.substring(0, 200);
            }
            it.time = System.currentTimeMillis() / 1000;
            ArrayList<Item> list = list(account);
            for (int i = list.size() - 1; i >= 0; i--) if (list.get(i).dialogId == dialogId) list.remove(i);
            list.add(0, it);
            while (list.size() > 200) list.remove(list.size() - 1);
            save(account, list);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static ArrayList<Item> list(int account) {
        ArrayList<Item> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(account).getString("items", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Item it = new Item();
                it.dialogId = o.optLong("d");
                it.name = o.optString("n");
                it.username = o.has("u") ? o.optString("u") : null;
                it.last = o.has("l") ? o.optString("l") : null;
                it.time = o.optLong("t");
                it.kind = o.optInt("k");
                list.add(it);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    private static void save(int account, ArrayList<Item> list) throws Exception {
        synchronized (SparkDeletedChats.class) {
            kindCache.remove(account);
        }
        JSONArray a = new JSONArray();
        for (Item it : list) {
            JSONObject o = new JSONObject();
            o.put("d", it.dialogId);
            o.put("n", it.name);
            if (!TextUtils.isEmpty(it.username)) o.put("u", it.username);
            if (!TextUtils.isEmpty(it.last)) o.put("l", it.last);
            o.put("t", it.time);
            o.put("k", it.kind);
            a.put(o);
        }
        prefs(account).edit().putString("items", a.toString()).apply();
    }

    public static void remove(int account, long dialogId) {
        try {
            ArrayList<Item> list = list(account);
            for (int i = list.size() - 1; i >= 0; i--) if (list.get(i).dialogId == dialogId) list.remove(i);
            save(account, list);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
}

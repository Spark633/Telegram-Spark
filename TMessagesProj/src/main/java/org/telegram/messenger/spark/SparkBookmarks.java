package org.telegram.messenger.spark;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;

/** Lesezeichen mit Schlagwoertern (pro Konto): Nachricht merken und spaeter direkt hinspringen. */
public class SparkBookmarks {

    public static class Item {
        public long dialogId;
        public int messageId;
        public long senderId;
        public String text;
        public int date;
        public long added;
        public ArrayList<String> tags = new ArrayList<>();
    }

    private static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_bookmarks_" + account, Context.MODE_PRIVATE);
    }

    /** "#rp, wichtig  idee" -> [rp, wichtig, idee] */
    public static ArrayList<String> parseTags(String input) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (input != null) {
            for (String t : input.split("[,;\\s]+")) {
                t = t.trim();
                while (t.startsWith("#")) t = t.substring(1);
                if (!t.isEmpty()) set.add(t.toLowerCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(set);
    }

    public static ArrayList<Item> list(int account) {
        ArrayList<Item> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(account).getString("items", "[]"));
            for (int i = a.length() - 1; i >= 0; i--) {
                JSONObject o = a.getJSONObject(i);
                Item it = new Item();
                it.dialogId = o.optLong("d");
                it.messageId = o.optInt("m");
                it.senderId = o.optLong("s");
                it.text = o.optString("t");
                it.date = o.optInt("dt");
                it.added = o.optLong("a");
                JSONArray tags = o.optJSONArray("g");
                if (tags != null) for (int k = 0; k < tags.length(); k++) it.tags.add(tags.getString(k));
                list.add(it);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    private static void save(int account, ArrayList<Item> newestFirst) {
        JSONArray a = new JSONArray();
        try {
            for (int i = newestFirst.size() - 1; i >= 0; i--) {
                Item it = newestFirst.get(i);
                JSONObject o = new JSONObject();
                o.put("d", it.dialogId);
                o.put("m", it.messageId);
                o.put("s", it.senderId);
                o.put("t", it.text);
                o.put("dt", it.date);
                o.put("a", it.added);
                JSONArray g = new JSONArray();
                for (String t : it.tags) g.put(t);
                o.put("g", g);
                a.put(o);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        prefs(account).edit().putString("items", a.toString()).apply();
    }

    public static Item find(int account, long dialogId, int messageId) {
        for (Item it : list(account)) {
            if (it.dialogId == dialogId && it.messageId == messageId) return it;
        }
        return null;
    }

    /** Legt ein Lesezeichen an oder aendert die Schlagwoerter eines vorhandenen. */
    public static void put(int account, MessageObject mo, ArrayList<String> tags) {
        ArrayList<Item> all = list(account);
        Item it = null;
        for (Item x : all) {
            if (x.dialogId == mo.getDialogId() && x.messageId == mo.getId()) { it = x; break; }
        }
        if (it == null) {
            it = new Item();
            it.dialogId = mo.getDialogId();
            it.messageId = mo.getId();
            it.senderId = mo.getFromChatId();
            CharSequence t = mo.messageOwner != null && !TextUtils.isEmpty(mo.messageOwner.message) ? mo.messageOwner.message : mo.messageText;
            it.text = t == null ? "" : t.toString();
            if (it.text.length() > 500) it.text = it.text.substring(0, 500);
            it.date = mo.messageOwner != null ? mo.messageOwner.date : 0;
            it.added = System.currentTimeMillis() / 1000;
            all.add(0, it);
        }
        it.tags = tags;
        save(account, all);
    }

    public static void setTags(int account, long dialogId, int messageId, ArrayList<String> tags) {
        ArrayList<Item> all = list(account);
        for (Item x : all) {
            if (x.dialogId == dialogId && x.messageId == messageId) x.tags = tags;
        }
        save(account, all);
    }

    public static void remove(int account, long dialogId, int messageId) {
        ArrayList<Item> all = list(account);
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).dialogId == dialogId && all.get(i).messageId == messageId) all.remove(i);
        }
        save(account, all);
    }

    /** Alle benutzten Schlagwoerter, haeufigste zuerst. */
    public static ArrayList<String> allTags(int account) {
        java.util.HashMap<String, Integer> count = new java.util.HashMap<>();
        for (Item it : list(account)) for (String t : it.tags) count.put(t, count.containsKey(t) ? count.get(t) + 1 : 1);
        ArrayList<String> tags = new ArrayList<>(count.keySet());
        java.util.Collections.sort(tags, (a, b) -> count.get(b) - count.get(a));
        return tags;
    }
}

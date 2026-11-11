package org.telegram.messenger.spark;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.util.ArrayList;

/**
 * Gestellte Beitrittsanfragen (pro Konto). Telegram bietet keine Liste der eigenen Anfragen –
 * deshalb merkt sich Sparkgram jede Anfrage, die in der App gestellt wird.
 */
public class SparkJoinRequests {

    public static class Item {
        public long chatId;      // 0, wenn nur ueber Einladungslink bekannt
        public String title;
        public String hash;      // Einladungslink-Kennung (falls ueber Link)
        public long time;
        public String status;    // zuletzt gepruefter Stand (Text)
    }

    private static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_joinrequests_" + account, Context.MODE_PRIVATE);
    }

    public static ArrayList<Item> list(int account) {
        ArrayList<Item> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(account).getString("items", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Item it = new Item();
                it.chatId = o.optLong("c");
                it.title = o.optString("t");
                it.hash = o.has("h") ? o.optString("h") : null;
                it.time = o.optLong("d");
                it.status = o.has("s") ? o.optString("s") : null;
                list.add(it);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    private static void save(int account, ArrayList<Item> list) {
        JSONArray a = new JSONArray();
        try {
            for (Item it : list) {
                JSONObject o = new JSONObject();
                o.put("c", it.chatId);
                o.put("t", it.title == null ? "" : it.title);
                if (!TextUtils.isEmpty(it.hash)) o.put("h", it.hash);
                o.put("d", it.time);
                if (it.status != null) o.put("s", it.status);
                a.put(o);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        prefs(account).edit().putString("items", a.toString()).apply();
    }

    /** Einladungslink einer Gruppe, ueber den schon einmal eine Anfrage gestellt wurde (bleibt auch nach Entfernen aus der Liste). */
    public static String hashFor(int account, long chatId) {
        return chatId == 0 ? null : prefs(account).getString("hash_" + chatId, null);
    }

    public static void rememberHash(int account, long chatId, String hash) {
        if (chatId != 0 && !TextUtils.isEmpty(hash)) prefs(account).edit().putString("hash_" + chatId, hash).apply();
    }

    /** Gruppe ueber den Namen finden (Einladungslinks verraten die Gruppe oft nicht): verlassene Gruppen bevorzugt. */
    private static long guessChatId(int account, String title) {
        if (TextUtils.isEmpty(title)) return 0;
        org.telegram.messenger.MessagesController mc = org.telegram.messenger.MessagesController.getInstance(account);
        long found = 0;
        for (org.telegram.tgnet.TLRPC.Dialog d : mc.getAllDialogs()) {
            if (d.id >= 0) continue;
            org.telegram.tgnet.TLRPC.Chat c = mc.getChat(-d.id);
            if (c != null && title.equals(c.title)) {
                if (org.telegram.messenger.ChatObject.isNotInChat(c)) return c.id;
                if (found == 0) found = c.id;
            }
        }
        return found;
    }

    /** Neue Anfrage merken (doppelte werden ersetzt). */
    public static void add(int account, long chatId, String title, String hash) {
        if (chatId == 0) chatId = guessChatId(account, title);
        rememberHash(account, chatId, hash);
        ArrayList<Item> list = list(account);
        for (int i = list.size() - 1; i >= 0; i--) {
            Item it = list.get(i);
            if (chatId != 0 && it.chatId == chatId || hash != null && hash.equals(it.hash)) list.remove(i);
        }
        Item it = new Item();
        it.chatId = chatId;
        it.title = title;
        it.hash = hash;
        it.time = System.currentTimeMillis();
        list.add(0, it);
        while (list.size() > 100) list.remove(list.size() - 1);
        save(account, list);
        SparkLog.log("Beitrittsanfrage gestellt: " + (title != null ? title : chatId));
    }

    /** Wird aufgerufen, wenn man (wieder) Mitglied einer Gruppe ist: offene Anfrage -> angenommen + Meldung. */
    public static void checkAccepted(int account, org.telegram.tgnet.TLRPC.Chat chat) {
        if (chat == null || org.telegram.messenger.ChatObject.isNotInChat(chat)) return;
        ArrayList<Item> list = list(account);
        for (Item it : list) {
            boolean match = it.chatId == chat.id || it.chatId == 0 && it.title != null && it.title.equals(chat.title);
            if (match && (it.status == null || it.status.startsWith("⏳"))) {
                it.status = "✅ angenommen";
                it.chatId = chat.id;
                save(account, list);
                SparkKickNotify.onJoinAccepted(account, -chat.id, chat.title);
                return;
            }
        }
    }

    public static void setStatus(int account, int index, String status, long chatId) {
        ArrayList<Item> list = list(account);
        if (index < 0 || index >= list.size()) return;
        list.get(index).status = status;
        if (chatId != 0) list.get(index).chatId = chatId;
        save(account, list);
    }

    public static void remove(int account, int index) {
        ArrayList<Item> list = list(account);
        if (index >= 0 && index < list.size()) list.remove(index);
        save(account, list);
    }
}

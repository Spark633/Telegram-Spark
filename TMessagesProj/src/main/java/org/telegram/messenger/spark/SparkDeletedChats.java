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

    public static class Item {
        public long dialogId;
        public String name;
        public String username;
        public String last;
        public long time;
    }

    private static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_deleted_chats_" + account, Context.MODE_PRIVATE);
    }

    public static void remember(int account, long dialogId) {
        if (!SparkConfig.get(SparkConfig.DELETED_CHATS) || dialogId == 0) return;
        try {
            MessagesController mc = MessagesController.getInstance(account);
            Item it = new Item();
            it.dialogId = dialogId;
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
                list.add(it);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    private static void save(int account, ArrayList<Item> list) throws Exception {
        JSONArray a = new JSONArray();
        for (Item it : list) {
            JSONObject o = new JSONObject();
            o.put("d", it.dialogId);
            o.put("n", it.name);
            if (!TextUtils.isEmpty(it.username)) o.put("u", it.username);
            if (!TextUtils.isEmpty(it.last)) o.put("l", it.last);
            o.put("t", it.time);
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

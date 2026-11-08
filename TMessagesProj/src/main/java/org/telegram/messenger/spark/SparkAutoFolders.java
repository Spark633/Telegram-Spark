package org.telegram.messenger.spark;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.FilterCreateActivity;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Auto-Ordner (pro Konto): Regeln "Chatname enthaelt WORT -> in Telegram-Ordner X".
 * Passende Chats werden dem Ordner als "immer anzeigen" hinzugefuegt (echter Telegram-Ordner, auf allen Geraeten).
 */
public class SparkAutoFolders {

    /** Telegram erlaubt hoechstens 100 fest eingetragene Chats pro Ordner. */
    public static final int MAX_PER_FOLDER = 100;

    public static class Rule {
        public String word;
        public int folderId;
    }

    private static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_autofolders_" + account, Context.MODE_PRIVATE);
    }

    public static ArrayList<Rule> rules(int account) {
        ArrayList<Rule> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(account).getString("rules", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Rule r = new Rule();
                r.word = o.optString("w");
                r.folderId = o.optInt("f");
                if (!TextUtils.isEmpty(r.word)) list.add(r);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public static void save(int account, ArrayList<Rule> list) {
        JSONArray a = new JSONArray();
        try {
            for (Rule r : list) {
                JSONObject o = new JSONObject();
                o.put("w", r.word);
                o.put("f", r.folderId);
                a.put(o);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        prefs(account).edit().putString("rules", a.toString()).apply();
    }

    public static void add(int account, String word, int folderId) {
        ArrayList<Rule> list = rules(account);
        Rule r = new Rule();
        r.word = word.trim();
        r.folderId = folderId;
        list.add(r);
        save(account, list);
    }

    public static void remove(int account, int index) {
        ArrayList<Rule> list = rules(account);
        if (index >= 0 && index < list.size()) list.remove(index);
        save(account, list);
    }

    public static String dialogName(MessagesController mc, long did) {
        if (DialogObject.isUserDialog(did)) {
            TLRPC.User u = mc.getUser(did);
            return u != null ? UserObject.getUserName(u) : "";
        }
        TLRPC.Chat c = mc.getChat(-did);
        return c != null && c.title != null ? c.title : "";
    }

    public static boolean matches(Rule r, String name) {
        return name != null && name.toLowerCase(Locale.ROOT).contains(r.word.toLowerCase(Locale.ROOT));
    }

    /** Ordner, den es (noch) gibt, oder null. */
    public static MessagesController.DialogFilter folder(MessagesController mc, int id) {
        for (MessagesController.DialogFilter f : mc.getDialogFilters()) {
            if (f.id == id && !f.isDefault() && f.id != MessagesController.SPARK_LEFT_FILTER_ID) return f;
        }
        return null;
    }

    /**
     * Regeln anwenden: fehlende passende Chats in die Ordner eintragen. Liefert die Zahl neu einsortierter Chats.
     * Braucht ein Fragment mit Activity (zum Speichern bei Telegram).
     */
    public static int apply(BaseFragment fragment) {
        if (!SparkConfig.get(SparkConfig.AUTO_FOLDERS) || fragment == null || fragment.getParentActivity() == null) return 0;
        int account = fragment.getCurrentAccount();
        ArrayList<Rule> rules = rules(account);
        if (rules.isEmpty()) return 0;
        MessagesController mc = MessagesController.getInstance(account);
        int added = 0;
        java.util.HashSet<Integer> changed = new java.util.HashSet<>();
        for (Rule r : rules) {
            MessagesController.DialogFilter f = folder(mc, r.folderId);
            if (f == null || f.isChatlist()) continue;
            for (TLRPC.Dialog d : mc.getAllDialogs()) {
                if (f.alwaysShow.size() >= MAX_PER_FOLDER) break;
                if (DialogObject.isFolderDialogId(d.id) || f.alwaysShow.contains(d.id)) continue;
                if (matches(r, dialogName(mc, d.id))) {
                    f.alwaysShow.add(d.id);
                    f.neverShow.remove(d.id);
                    added++;
                    changed.add(f.id);
                }
            }
        }
        for (int id : changed) {
            MessagesController.DialogFilter f = folder(mc, id);
            if (f == null) continue;
            FilterCreateActivity.saveFilterToServer(f, f.flags, f.name, f.entities, f.title_noanimate, f.color, f.alwaysShow, f.neverShow, f.pinnedDialogs, false, false, true, true, false, fragment, null);
        }
        return added;
    }

    private static long lastAuto;

    /** Automatisch beim Oeffnen der Chatliste, hoechstens alle 5 Minuten. */
    public static void applyThrottled(BaseFragment fragment) {
        long now = System.currentTimeMillis();
        if (now - lastAuto < 5 * 60_000L) return;
        lastAuto = now;
        try {
            apply(fragment);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
}

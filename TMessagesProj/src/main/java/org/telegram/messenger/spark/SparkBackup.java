package org.telegram.messenger.spark;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;

import java.util.Map;
import java.util.Set;

/**
 * Sicherung: Liste der angemeldeten Konten (Nummer, Name) + Sparkgram-Einstellungen/Notizen/Schlagwoerter/
 * lokale Namen/Vorlagen. KEINE Sitzungsschluessel – auf dem neuen Geraet meldet man sich mit Code (+ Passwort) an.
 * Kontobezogene Daten werden beim Import nach Nutzer-ID zugeordnet, sobald das Konto angemeldet ist.
 */
public class SparkBackup {

    private static final String[] PER_ACCOUNT = {"sparkgram_config_", "sparkgram_notes_", "sparkgram_keywords_", "sparkgram_people_", "sparkgram_local_names_", "sparkgram_admin_counts_", "sparkgram_persona_", "sparkgram_autofolders_"};
    private static final String[] GLOBAL = {"sparkgram_config", "sparkgram_local_names", "sparkgram_admin"};

    public static class Account {
        public long userId;
        public String phone;
        public String name;
        public String username;
    }

    private static SharedPreferences prefs(String name) {
        return ApplicationLoader.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    private static JSONObject dump(SharedPreferences p) throws Exception {
        JSONObject o = new JSONObject();
        for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
            Object v = e.getValue();
            JSONObject t = new JSONObject();
            if (v instanceof Boolean) t.put("b", v);
            else if (v instanceof Integer) t.put("i", v);
            else if (v instanceof Long) t.put("l", v);
            else if (v instanceof Float) t.put("f", (double) (Float) v);
            else if (v instanceof String) t.put("s", v);
            else if (v instanceof Set) {
                JSONArray a = new JSONArray();
                for (Object x : (Set<?>) v) a.put(String.valueOf(x));
                t.put("set", a);
            } else continue;
            o.put(e.getKey(), t);
        }
        return o;
    }

    private static void restore(SharedPreferences p, JSONObject o, boolean overwrite) throws Exception {
        SharedPreferences.Editor ed = p.edit();
        java.util.Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            if (!overwrite && p.contains(k)) continue;
            JSONObject t = o.getJSONObject(k);
            if (t.has("b")) ed.putBoolean(k, t.getBoolean("b"));
            else if (t.has("i")) ed.putInt(k, t.getInt("i"));
            else if (t.has("l")) ed.putLong(k, t.getLong("l"));
            else if (t.has("f")) ed.putFloat(k, (float) t.getDouble("f"));
            else if (t.has("s")) ed.putString(k, t.getString("s"));
            else if (t.has("set")) {
                JSONArray a = t.getJSONArray("set");
                java.util.HashSet<String> set = new java.util.HashSet<>();
                for (int i = 0; i < a.length(); i++) set.add(a.getString(i));
                ed.putStringSet(k, set);
            }
        }
        ed.apply();
    }

    /** Erstellt die Sicherung als JSON-Text. */
    public static String export() throws Exception {
        JSONObject root = new JSONObject();
        root.put("sparkgram_backup", 1);
        root.put("created", System.currentTimeMillis() / 1000);
        JSONArray accounts = new JSONArray();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            UserConfig uc = UserConfig.getInstance(a);
            if (!uc.isClientActivated()) continue;
            TLRPC.User u = uc.getCurrentUser();
            if (u == null) continue;
            JSONObject acc = new JSONObject();
            acc.put("user_id", u.id);
            acc.put("phone", u.phone == null ? "" : u.phone);
            acc.put("name", UserObject.getUserName(u));
            String un = UserObject.getPublicUsername(u);
            if (un != null) acc.put("username", un);
            JSONObject data = new JSONObject();
            for (String prefix : PER_ACCOUNT) {
                data.put(prefix, dump(prefs(prefix + a)));
            }
            acc.put("data", data);
            accounts.put(acc);
        }
        root.put("accounts", accounts);
        JSONObject global = new JSONObject();
        for (String name : GLOBAL) {
            JSONObject d = dump(prefs(name));
            // Schluessel/Tokens nicht mitsichern
            d.remove("deeplKey");
            d.remove("botToken");
            d.remove("backupPending");
            d.remove("backupAccounts");
            global.put(name, d);
        }
        root.put("global", global);
        return root.toString(1);
    }

    /** Liest eine Sicherung: globale Daten sofort, Konto-Daten vormerken. Liefert die Konten der Datei. */
    public static java.util.ArrayList<Account> importBackup(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        if (!root.has("sparkgram_backup")) throw new IllegalArgumentException("Keine Sparkgram-Sicherung");
        JSONObject global = root.optJSONObject("global");
        if (global != null) {
            for (String name : GLOBAL) {
                JSONObject d = global.optJSONObject(name);
                if (d != null) restore(prefs(name), d, false);
            }
        }
        java.util.ArrayList<Account> list = new java.util.ArrayList<>();
        JSONArray accounts = root.optJSONArray("accounts");
        JSONObject pending = new JSONObject(SparkConfig.prefs().getString("backupPending", "{}"));
        if (accounts != null) {
            for (int i = 0; i < accounts.length(); i++) {
                JSONObject acc = accounts.getJSONObject(i);
                Account a = new Account();
                a.userId = acc.getLong("user_id");
                a.phone = acc.optString("phone");
                a.name = acc.optString("name");
                a.username = acc.optString("username", null);
                list.add(a);
                if (acc.has("data")) pending.put(String.valueOf(a.userId), acc.getJSONObject("data"));
            }
        }
        SparkConfig.prefs().edit().putString("backupPending", pending.toString()).putString("backupAccounts", accounts != null ? accounts.toString() : "[]").apply();
        applyPending();
        return list;
    }

    /** Konten aus der zuletzt importierten Sicherung (fuer die Anmelde-Liste). */
    public static java.util.ArrayList<Account> lastImported() {
        java.util.ArrayList<Account> list = new java.util.ArrayList<>();
        try {
            JSONArray accounts = new JSONArray(SparkConfig.prefs().getString("backupAccounts", "[]"));
            for (int i = 0; i < accounts.length(); i++) {
                JSONObject acc = accounts.getJSONObject(i);
                Account a = new Account();
                a.userId = acc.getLong("user_id");
                a.phone = acc.optString("phone");
                a.name = acc.optString("name");
                a.username = acc.optString("username", null);
                list.add(a);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    /** Ordnet vorgemerkte Konto-Daten angemeldeten Konten zu (beim Start und nach dem Import). */
    public static void applyPending() {
        try {
            String raw = SparkConfig.prefs().getString("backupPending", null);
            if (raw == null || raw.equals("{}")) return;
            JSONObject pending = new JSONObject(raw);
            boolean changed = false;
            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                UserConfig uc = UserConfig.getInstance(a);
                if (!uc.isClientActivated()) continue;
                String key = String.valueOf(uc.getClientUserId());
                JSONObject data = pending.optJSONObject(key);
                if (data == null) continue;
                for (String prefix : PER_ACCOUNT) {
                    JSONObject d = data.optJSONObject(prefix);
                    if (d != null) restore(prefs(prefix + a), d, false);
                }
                pending.remove(key);
                changed = true;
            }
            if (changed) {
                SparkConfig.prefs().edit().putString("backupPending", pending.toString()).apply();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static boolean isLoggedIn(long userId) {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            UserConfig uc = UserConfig.getInstance(a);
            if (uc.isClientActivated() && uc.getClientUserId() == userId) return true;
        }
        return false;
    }

    public static int freeAccount() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (!UserConfig.getInstance(a).isClientActivated()) return a;
        }
        return -1;
    }
}

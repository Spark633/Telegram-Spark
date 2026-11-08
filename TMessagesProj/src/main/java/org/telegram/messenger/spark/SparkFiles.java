package org.telegram.messenger.spark;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.util.ArrayList;

/** Von Sparkgram gespeicherte Dateien (Exporte, Sicherungen, Medien) fuer die Download-Liste. */
public class SparkFiles {

    public static class Item {
        public String name;
        public String uri;
        public String mime;
        public long time;
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_files", Context.MODE_PRIVATE);
    }

    public static synchronized void record(String name, String uri, String mime) {
        try {
            JSONArray a = new JSONArray(prefs().getString("items", "[]"));
            JSONArray out = new JSONArray();
            JSONObject o = new JSONObject();
            o.put("n", name);
            o.put("u", uri);
            if (mime != null) o.put("m", mime);
            o.put("t", System.currentTimeMillis());
            out.put(o);
            for (int i = 0; i < a.length() && out.length() < 50; i++) {
                if (!uri.equals(a.getJSONObject(i).optString("u"))) out.put(a.get(i));
            }
            prefs().edit().putString("items", out.toString()).apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /** Neueste zuerst. */
    public static synchronized ArrayList<Item> list() {
        ArrayList<Item> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs().getString("items", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Item it = new Item();
                it.name = o.optString("n");
                it.uri = o.optString("u");
                it.mime = o.has("m") ? o.optString("m") : null;
                it.time = o.optLong("t");
                list.add(it);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public static synchronized void remove(String uri) {
        try {
            JSONArray a = new JSONArray(prefs().getString("items", "[]"));
            JSONArray out = new JSONArray();
            for (int i = 0; i < a.length(); i++) {
                if (!uri.equals(a.getJSONObject(i).optString("u"))) out.put(a.get(i));
            }
            prefs().edit().putString("items", out.toString()).apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /** Datei mit passender App oeffnen. Liefert false, wenn das nicht ging (z. B. geloescht). */
    public static boolean open(Context context, Item it) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.parse(it.uri), it.mime != null ? it.mime : "*/*");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(i, it.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }
}

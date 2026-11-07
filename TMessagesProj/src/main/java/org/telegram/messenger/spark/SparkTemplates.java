package org.telegram.messenger.spark;

import org.json.JSONArray;
import org.telegram.messenger.FileLog;

import java.util.ArrayList;

/** Textvorlagen (global, fuer alle Konten): im Chat-Menue einfuegen. */
public class SparkTemplates {

    public static ArrayList<String> list() {
        ArrayList<String> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(SparkConfig.prefs().getString("templates", "[]"));
            for (int i = 0; i < a.length(); i++) list.add(a.getString(i));
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public static void save(ArrayList<String> list) {
        JSONArray a = new JSONArray();
        for (String s : list) a.put(s);
        SparkConfig.prefs().edit().putString("templates", a.toString()).apply();
    }
}

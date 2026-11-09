package org.telegram.messenger.spark;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/** Kleines Diagnose-Protokoll (letzte 200 Ereignisse) fuer die Fehlersuche, z. B. Verlassen/Entfernen. */
public class SparkLog {

    private static final int MAX = 200;
    private static final ArrayList<String> lines = new ArrayList<>();
    private static boolean loaded;

    public static synchronized void log(String text) {
        load();
        String line = new SimpleDateFormat("HH:mm:ss", Locale.GERMANY).format(new Date()) + " " + text;
        lines.add(line);
        while (lines.size() > MAX) lines.remove(0);
        SparkConfig.prefs().edit().putString("diagLog", android.text.TextUtils.join("\n", lines)).apply();
    }

    public static synchronized String all() {
        load();
        return lines.isEmpty() ? "(noch leer)" : android.text.TextUtils.join("\n", lines);
    }

    public static synchronized void clear() {
        lines.clear();
        SparkConfig.prefs().edit().remove("diagLog").apply();
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        String s = SparkConfig.prefs().getString("diagLog", null);
        if (s != null && !s.isEmpty()) {
            for (String l : s.split("\n")) lines.add(l);
        }
    }
}

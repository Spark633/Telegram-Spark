package org.telegram.messenger.spark;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Zeitzone pro Person (manuell hinterlegt) und Anzeige der aktuellen Ortszeit. */
public class SparkTimezones {

    /** Auswahl gaengiger Zeitzonen: {Anzeigename, Zeitzonen-ID}. */
    public static final String[][] ZONES = {
        {"Deutschland / Österreich / Schweiz", "Europe/Berlin"},
        {"Großbritannien / Irland", "Europe/London"},
        {"Portugal", "Europe/Lisbon"},
        {"Finnland / Baltikum / Griechenland", "Europe/Helsinki"},
        {"Türkei", "Europe/Istanbul"},
        {"Russland (Moskau)", "Europe/Moscow"},
        {"Ukraine", "Europe/Kyiv"},
        {"USA Ostküste (New York)", "America/New_York"},
        {"USA Mitte (Chicago)", "America/Chicago"},
        {"USA Berge (Denver)", "America/Denver"},
        {"USA Westküste (Los Angeles)", "America/Los_Angeles"},
        {"Kanada (Toronto)", "America/Toronto"},
        {"Mexiko", "America/Mexico_City"},
        {"Brasilien (São Paulo)", "America/Sao_Paulo"},
        {"Argentinien", "America/Argentina/Buenos_Aires"},
        {"Indien", "Asia/Kolkata"},
        {"VAE / Dubai", "Asia/Dubai"},
        {"China", "Asia/Shanghai"},
        {"Japan", "Asia/Tokyo"},
        {"Südkorea", "Asia/Seoul"},
        {"Indonesien (Jakarta)", "Asia/Jakarta"},
        {"Philippinen", "Asia/Manila"},
        {"Australien Ost (Sydney)", "Australia/Sydney"},
        {"Australien West (Perth)", "Australia/Perth"},
        {"Neuseeland", "Pacific/Auckland"},
        {"Südafrika", "Africa/Johannesburg"},
        {"Ägypten", "Africa/Cairo"},
        {"Nigeria", "Africa/Lagos"},
    };

    private static SharedPreferences prefs;

    private static SharedPreferences prefs() {
        if (prefs == null) {
            prefs = ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_timezones", Context.MODE_PRIVATE);
        }
        return prefs;
    }

    public static String get(long dialogId) {
        return prefs().getString(String.valueOf(dialogId), null);
    }

    public static void set(long dialogId, String zoneId) {
        if (zoneId == null) {
            prefs().edit().remove(String.valueOf(dialogId)).apply();
        } else {
            prefs().edit().putString(String.valueOf(dialogId), zoneId).apply();
        }
    }

    public static String label(String zoneId) {
        for (String[] z : ZONES) {
            if (z[1].equals(zoneId)) return z[0];
        }
        return zoneId;
    }

    /** Kurzform fuer die Info-Zeile, z. B. "05:32 (UTC−8)". */
    public static String shortTime(long dialogId) {
        String id = get(dialogId);
        if (id == null) {
            return null;
        }
        TimeZone tz = TimeZone.getTimeZone(id);
        SimpleDateFormat f = new SimpleDateFormat("HH:mm", Locale.GERMANY);
        f.setTimeZone(tz);
        long now = System.currentTimeMillis();
        int off = tz.getOffset(now) / 60000;
        int h = Math.abs(off) / 60, m = Math.abs(off) % 60;
        String utc = "UTC" + (off == 0 ? "" : (off > 0 ? "+" : "−") + h + (m != 0 ? ":" + (m < 10 ? "0" : "") + m : ""));
        return f.format(new Date(now)) + " (" + utc + ")";
    }

    /** z. B. "14:32 (Japan, +8 h zu dir)" oder null. */
    public static String localTime(long dialogId) {
        String id = get(dialogId);
        if (id == null) {
            return null;
        }
        TimeZone tz = TimeZone.getTimeZone(id);
        SimpleDateFormat f = new SimpleDateFormat("HH:mm", Locale.GERMANY);
        f.setTimeZone(tz);
        long now = System.currentTimeMillis();
        int diffMin = (tz.getOffset(now) - TimeZone.getDefault().getOffset(now)) / 60000;
        String diff;
        if (diffMin == 0) {
            diff = "gleiche Zeit wie du";
        } else {
            int h = Math.abs(diffMin) / 60, m = Math.abs(diffMin) % 60;
            diff = (diffMin > 0 ? "+" : "−") + h + (m != 0 ? ":" + (m < 10 ? "0" : "") + m : "") + " h zu dir";
        }
        return f.format(new Date(now)) + " (" + label(id) + ", " + diff + ")";
    }
}

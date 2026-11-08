package org.telegram.messenger.spark;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Pro Konto: einzeln stummgeschaltete Personen (keine Benachrichtigungen von ihnen, in allen Chats). */
public class SparkPeople {

    private static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_people_" + account, Context.MODE_PRIVATE);
    }

    private static final Object lock = new Object();
    private static final java.util.HashMap<Integer, HashSet<Long>> cache = new java.util.HashMap<>();

    private static HashSet<Long> muted(int account) {
        synchronized (lock) {
            HashSet<Long> set = cache.get(account);
            if (set == null) {
                set = new HashSet<>();
                for (String s : prefs(account).getStringSet("muted", new HashSet<>())) {
                    try { set.add(Long.parseLong(s)); } catch (Exception ignore) {}
                }
                cache.put(account, set);
            }
            return set;
        }
    }

    public static boolean isMuted(int account, long userId) {
        if (userId == 0) return false;
        synchronized (lock) {
            return muted(account).contains(userId);
        }
    }

    public static void setMuted(int account, long userId, boolean mute) {
        synchronized (lock) {
            HashSet<Long> set = muted(account);
            if (mute) set.add(userId); else set.remove(userId);
            Set<String> out = new HashSet<>();
            for (Long l : set) out.add(String.valueOf(l));
            prefs(account).edit().putStringSet("muted", out).apply();
        }
    }

    public static ArrayList<Long> allMuted(int account) {
        synchronized (lock) {
            return new ArrayList<>(muted(account));
        }
    }

    // pro Person: Nachrichten in Gruppen zusaetzlich einklappen
    public static boolean isHidden(int account, long userId) {
        return userId != 0 && prefs(account).getStringSet("hidden", new HashSet<>()).contains(String.valueOf(userId)) && isMuted(account, userId);
    }

    public static void setHidden(int account, long userId, boolean hide) {
        HashSet<String> set = new HashSet<>(prefs(account).getStringSet("hidden", new HashSet<>()));
        if (hide) set.add(String.valueOf(userId)); else set.remove(String.valueOf(userId));
        prefs(account).edit().putStringSet("hidden", set).apply();
    }
}

package org.telegram.messenger.spark;

import org.telegram.messenger.UserConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Versteckte Konten: tauchen in der Kontoauswahl nicht auf (bis "jetzt anzeigen"). Global, nach Nutzer-ID. */
public class SparkAccounts {

    /** Nur bis zum App-Neustart: versteckte Konten trotzdem zeigen. */
    public static boolean revealed;

    public static boolean isHidden(int account) {
        if (revealed || !SparkConfig.get(SparkConfig.MASTER)) return false;
        UserConfig uc = UserConfig.getInstance(account);
        if (!uc.isClientActivated()) return false;
        return SparkConfig.prefs().getStringSet("hiddenAccounts", new HashSet<>()).contains(String.valueOf(uc.getClientUserId()));
    }

    public static boolean isHiddenRaw(int account) {
        UserConfig uc = UserConfig.getInstance(account);
        return SparkConfig.prefs().getStringSet("hiddenAccounts", new HashSet<>()).contains(String.valueOf(uc.getClientUserId()));
    }

    public static void setHidden(int account, boolean hide) {
        Set<String> set = new HashSet<>(SparkConfig.prefs().getStringSet("hiddenAccounts", new HashSet<>()));
        String id = String.valueOf(UserConfig.getInstance(account).getClientUserId());
        if (hide) set.add(id); else set.remove(id);
        SparkConfig.prefs().edit().putStringSet("hiddenAccounts", set).apply();
    }

    /** Versteckte Konten aus einer Liste entfernen (das aktive bleibt immer). */
    public static void filter(ArrayList<Integer> accounts, int current) {
        for (int i = accounts.size() - 1; i >= 0; i--) {
            int a = accounts.get(i);
            if (a != current && isHidden(a)) accounts.remove(i);
        }
    }
}

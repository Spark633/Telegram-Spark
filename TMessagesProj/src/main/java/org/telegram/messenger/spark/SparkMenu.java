package org.telegram.messenger.spark;

import java.util.HashSet;
import java.util.Set;

/** Menues aufraeumen: welche Eintraege im Nachrichten-Menue ausgeblendet werden (global). */
public class SparkMenu {

    /** {Option-ID, Name}; Werte entsprechen ChatActivity.OPTION_* */
    public static final Object[][] ENTRIES = {
        {8, "Antworten"}, {3, "Kopieren"}, {22, "Link kopieren"}, {2, "Weiterleiten"}, {12, "Bearbeiten"},
        {13, "Anheften"}, {14, "Lösen"}, {1, "Löschen"}, {29, "Übersetzen"}, {30, "Transkribieren"},
        {4, "In Galerie speichern"}, {7, "In Galerie speichern (2)"}, {10, "In Downloads / Musik speichern"}, {6, "Teilen"},
        {11, "Zu GIFs hinzufügen"}, {20, "Sticker zu Favoriten"}, {21, "Sticker aus Favoriten"},
        {28, "Statistik"}, {115, "Statistik anzeigen"}, {23, "Melden"}, {104, "Profil öffnen"},
        {106, "Faktencheck"}, {108, "Geschenk"}, {117, "Geld senden"}, {103, "Schneller laden (Premium)"},
        {902, "✦ Details"}, {903, "✦ Wiederholen"}, {904, "✦ Medien …"}, {905, "✦ Stummschalten"},
        {906, "✦ Lesezeichen"}, {907, "✦ Alle Nachrichten dieser Person"}, {908, "✦ Erinnern"}, {900, "✦ Bearbeitungsverlauf"}
    };

    private static Set<Integer> cache;

    public static synchronized Set<Integer> hidden() {
        if (cache == null) {
            cache = new HashSet<>();
            for (String s : SparkConfig.prefs().getStringSet("menuHidden", new HashSet<>())) {
                try { cache.add(Integer.parseInt(s)); } catch (Exception ignore) {}
            }
        }
        return cache;
    }

    public static synchronized void setHidden(int option, boolean hide) {
        Set<Integer> set = hidden();
        if (hide) set.add(option); else set.remove(option);
        Set<String> out = new HashSet<>();
        for (Integer i : set) out.add(String.valueOf(i));
        SparkConfig.prefs().edit().putStringSet("menuHidden", out).apply();
    }
}

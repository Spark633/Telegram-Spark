package org.telegram.messenger.spark;

import java.util.Calendar;

/**
 * Schaetzt das Registrierungsdatum eines Kontos anhand der Nutzer-ID.
 * Telegram vergibt IDs grob aufsteigend; die Stuetzpunkte sind Naeherungswerte,
 * das Ergebnis ist daher nur eine Schaetzung (meist auf einige Monate genau).
 */
public class SparkRegDate {

    // {ID, Jahr, Monat}
    private static final long[][] POINTS = {
        {0L, 2013, 8},
        {1_000_000L, 2013, 10},
        {10_000_000L, 2014, 3},
        {50_000_000L, 2015, 2},
        {100_000_000L, 2015, 9},
        {150_000_000L, 2016, 3},
        {200_000_000L, 2016, 6},
        {300_000_000L, 2016, 12},
        {400_000_000L, 2017, 6},
        {500_000_000L, 2017, 12},
        {600_000_000L, 2018, 5},
        {700_000_000L, 2018, 9},
        {800_000_000L, 2019, 1},
        {900_000_000L, 2019, 5},
        {1_000_000_000L, 2019, 9},
        {1_100_000_000L, 2020, 2},
        {1_300_000_000L, 2020, 6},
        {1_500_000_000L, 2020, 12},
        {1_800_000_000L, 2021, 4},
        {2_147_483_647L, 2021, 9},
        {5_000_000_000L, 2021, 12},
        {5_500_000_000L, 2022, 6},
        {6_000_000_000L, 2022, 12},
        {6_500_000_000L, 2023, 8},
        {7_000_000_000L, 2024, 2},
        {7_500_000_000L, 2024, 9},
        {8_000_000_000L, 2025, 3},
    };

    private static final String[] MONTHS = {"Jan", "Feb", "März", "Apr", "Mai", "Juni", "Juli", "Aug", "Sep", "Okt", "Nov", "Dez"};

    /** {Jahr, Monat 1-12}; null wenn ausserhalb der bekannten Daten. */
    public static int[] estimate(long userId) {
        if (userId <= 0) {
            return null;
        }
        // Luecke zwischen 32-Bit- und 64-Bit-IDs: kleine 64-Bit-IDs gibt es nicht
        if (userId > 2_147_483_647L && userId < 5_000_000_000L) {
            return new int[]{2021, 10};
        }
        for (int i = 1; i < POINTS.length; i++) {
            if (userId <= POINTS[i][0]) {
                long[] a = POINTS[i - 1];
                long[] b = POINTS[i];
                double f = (userId - a[0]) / (double) (b[0] - a[0]);
                int monthsA = (int) (a[1] * 12 + a[2] - 1);
                int monthsB = (int) (b[1] * 12 + b[2] - 1);
                int m = (int) Math.round(monthsA + f * (monthsB - monthsA));
                return new int[]{m / 12, m % 12 + 1};
            }
        }
        return null;
    }

    /** z. B. "ca. 2019" fuer die kurze Info-Zeile. */
    public static String shortText(long userId) {
        int[] e = estimate(userId);
        if (e == null) {
            long[] last = POINTS[POINTS.length - 1];
            return "nach " + last[1];
        }
        return "ca. " + e[0];
    }

    /** z. B. "ca. Mai 2019 (geschätzt aus der ID)" fuer die ausfuehrliche Ansicht. */
    public static String longText(long userId) {
        int[] e = estimate(userId);
        if (e == null) {
            long[] last = POINTS[POINTS.length - 1];
            return "nach " + MONTHS[(int) last[2] - 1] + " " + last[1] + " (geschätzt aus der ID)";
        }
        int year = Calendar.getInstance().get(Calendar.YEAR);
        String y = e[0] > year ? String.valueOf(year) : String.valueOf(e[0]);
        return "ca. " + MONTHS[e[1] - 1] + " " + y + " (geschätzt aus der ID)";
    }
}

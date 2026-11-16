package org.telegram.messenger.spark;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Waehrungen umrechnen: erkennt Betraege in Nachrichten ("20 $", "$20", "15 GBP", "5k ₹") und rechnet sie mit den
 * Tageskursen der Europaeischen Zentralbank in die Zielwaehrung (Standard: EUR) um. Nur die Kursliste wird geladen,
 * Nachrichten verlassen das Geraet nicht.
 */
public class SparkCurrency {

    /** Markierung, damit die automatische Zeile nicht doppelt angehaengt wird. */
    public static class Mark extends RelativeSizeSpan {
        public Mark() {
            super(0.8f);
        }
    }

    public static class Hit {
        public double amount;
        public String code;
        public String text;   // so wie in der Nachricht
    }

    private static final String ECB = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";
    private static HashMap<String, Double> rates; // 1 EUR = x Waehrung
    private static String ratesDate;
    private static boolean loading;

    private static final String[][] SYMBOLS = {
        {"US$", "USD"}, {"$", "USD"}, {"€", "EUR"}, {"£", "GBP"}, {"¥", "JPY"}, {"₹", "INR"}, {"₩", "KRW"}, {"₺", "TRY"},
        {"zł", "PLN"}, {"Kč", "CZK"}, {"Fr.", "CHF"}, {"R$", "BRL"}, {"A$", "AUD"}, {"C$", "CAD"}, {"HK$", "HKD"},
        {"NZ$", "NZD"}, {"S$", "SGD"}, {"₪", "ILS"}, {"฿", "THB"}, {"₱", "PHP"}, {"Ft", "HUF"},
    };
    private static final String CODES = "USD|EUR|GBP|JPY|CHF|PLN|CZK|DKK|SEK|NOK|HUF|RON|BGN|ISK|TRY|AUD|BRL|CAD|CNY|HKD|IDR|ILS|INR|KRW|MXN|MYR|NZD|PHP|SGD|THB|ZAR";

    private static Pattern pattern;

    private static Pattern pattern() {
        if (pattern != null) return pattern;
        StringBuilder sym = new StringBuilder();
        for (String[] s : SYMBOLS) {
            if (sym.length() > 0) sym.append('|');
            sym.append(Pattern.quote(s[0]));
        }
        String num = "(\\d{1,3}(?:[.,\\u00a0 ]\\d{3})+(?:[.,]\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)";
        String suf = "([kKmM](?![a-zA-Z]))?";
        // "kr" ist mehrdeutig (SEK/NOK/DKK/ISK) – wird erkannt, aber erst nach Rueckfrage umgerechnet
        String cur = "(" + sym + "|(?<![A-Za-z])kr\\.?(?![A-Za-z])|\\b(?:" + CODES + ")\\b)";
        // Waehrung vor der Zahl (Gruppen 1-3) oder nach der Zahl (Gruppen 4-6)
        pattern = Pattern.compile(cur + "\\s?" + num + "\\s?" + suf + "|(?<![\\w.,])" + num + "\\s?" + suf + "\\s?" + cur);
        return pattern;
    }

    /** Mehrdeutige Waehrungszeichen und ihre moeglichen Waehrungen. */
    public static final String AMBIGUOUS_KR = "?KR";
    public static final String[] KR_CHOICES = {"SEK", "NOK", "DKK", "ISK"};

    private static String codeFor(String token) {
        if (token == null) return null;
        if (token.equals("kr") || token.equals("kr.")) return AMBIGUOUS_KR;
        for (String[] s : SYMBOLS) if (s[0].equals(token)) return s[1];
        String up = token.toUpperCase(Locale.ROOT);
        return CODES.contains(up) ? up : null;
    }

    private static double parseNumber(String raw, String suffix) {
        String s = raw.replace(" ", "").replace(" ", "");
        int lastDot = s.lastIndexOf('.'), lastComma = s.lastIndexOf(',');
        int dec = Math.max(lastDot, lastComma);
        if (dec >= 0 && s.length() - dec - 1 <= 2 && s.length() - dec - 1 > 0) {
            // letztes Trennzeichen mit 1-2 Ziffern dahinter = Dezimaltrenner
            s = s.substring(0, dec).replace(".", "").replace(",", "") + "." + s.substring(dec + 1);
        } else {
            s = s.replace(".", "").replace(",", "");
        }
        double v = Double.parseDouble(s);
        if (suffix != null) {
            if (suffix.equalsIgnoreCase("k")) v *= 1000;
            else if (suffix.equalsIgnoreCase("m")) v *= 1_000_000;
        }
        return v;
    }

    /** Alle Betraege in einem Text. */
    public static ArrayList<Hit> find(CharSequence text) {
        ArrayList<Hit> hits = new ArrayList<>();
        if (TextUtils.isEmpty(text)) return hits;
        try {
            Matcher m = pattern().matcher(text);
            while (m.find() && hits.size() < 5) {
                String curTok, numTok, suf;
                if (m.group(1) != null) {
                    curTok = m.group(1); numTok = m.group(2); suf = m.group(3);
                } else {
                    numTok = m.group(4); suf = m.group(5); curTok = m.group(6);
                }
                String code = codeFor(curTok != null ? curTok.trim() : null);
                if (code == null || numTok == null) continue;
                Hit h = new Hit();
                h.code = code;
                h.amount = parseNumber(numTok, suf);
                h.text = m.group().trim();
                if (h.amount > 0) hits.add(h);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return hits;
    }

    // ---------------------------------------------------------------- Kurse

    public static String target() {
        return SparkConfig.prefs().getString("currencyTarget", "EUR");
    }

    public static void setTarget(String code) {
        SparkConfig.prefs().edit().putString("currencyTarget", code).apply();
    }

    /** Enthaelt der Text einen Betrag, dessen Waehrung erst gewaehlt werden muss (z. B. "kr")? */
    public static boolean needsChoice(CharSequence text) {
        for (Hit h : find(text)) if (h.code.startsWith("?")) return true;
        return false;
    }

    public static String[] allCodes() {
        return CODES.split("\\|");
    }

    private static synchronized void loadCached() {
        if (rates != null) return;
        String raw = SparkConfig.prefs().getString("currencyRates", null);
        ratesDate = SparkConfig.prefs().getString("currencyRatesDate", null);
        if (raw == null) return;
        rates = new HashMap<>();
        for (String part : raw.split(";")) {
            String[] kv = part.split("=");
            if (kv.length == 2) {
                try {
                    rates.put(kv[0], Double.parseDouble(kv[1]));
                } catch (NumberFormatException ignore) {
                }
            }
        }
        rates.put("EUR", 1.0);
    }

    public static synchronized String ratesDate() {
        loadCached();
        return ratesDate;
    }

    /** Kurse hoechstens einmal pro 6 Stunden neu laden (Hintergrund). */
    public static void refreshIfNeeded() {
        long last = SparkConfig.prefs().getLong("currencyRatesFetched", 0);
        if (loading || System.currentTimeMillis() - last < 6 * 3600_000L && ratesReady()) return;
        loading = true;
        Utilities.globalQueue.postRunnable(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(ECB).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(15000);
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }
                Matcher d = Pattern.compile("time=['\"]([0-9-]+)['\"]").matcher(sb);
                Matcher m = Pattern.compile("currency=['\"]([A-Z]{3})['\"]\\s+rate=['\"]([0-9.]+)['\"]").matcher(sb);
                StringBuilder out = new StringBuilder();
                while (m.find()) {
                    if (out.length() > 0) out.append(';');
                    out.append(m.group(1)).append('=').append(m.group(2));
                }
                if (out.length() > 0) {
                    SparkConfig.prefs().edit()
                        .putString("currencyRates", out.toString())
                        .putString("currencyRatesDate", d.find() ? d.group(1) : null)
                        .putLong("currencyRatesFetched", System.currentTimeMillis()).apply();
                    synchronized (SparkCurrency.class) {
                        rates = null;
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                loading = false;
            }
        });
    }

    public static synchronized boolean ratesReady() {
        loadCached();
        return rates != null && rates.size() > 1;
    }

    /** Betrag in die Zielwaehrung umrechnen, NaN wenn nicht moeglich. */
    public static synchronized double convert(double amount, String from, String to) {
        loadCached();
        if (rates == null) return Double.NaN;
        Double rf = rates.get(from), rt = rates.get(to);
        if (rf == null || rt == null) return Double.NaN;
        return amount / rf * rt;
    }

    public static String format(double v, String code) {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.GERMANY);
        f.setMaximumFractionDigits(v >= 100 ? 0 : 2);
        f.setMinimumFractionDigits(v >= 100 ? 0 : 2);
        String sym = code.equals("EUR") ? "€" : code.equals("USD") ? "$" : code.equals("GBP") ? "£" : code;
        return f.format(v) + " " + sym;
    }

    /** Kurze Umrechnung aller Betraege, z. B. "≈ 18,40 € · ≈ 91,10 €" (oder null). */
    public static String summary(CharSequence text) {
        String to = target();
        ArrayList<Hit> hits = find(text);
        StringBuilder sb = new StringBuilder();
        for (Hit h : hits) {
            if (h.code.equals(to) || h.code.startsWith("?")) continue;
            double v = convert(h.amount, h.code, to);
            if (Double.isNaN(v)) continue;
            if (sb.length() > 0) sb.append(" · ");
            sb.append("≈ ").append(format(v, to));
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /** Ausfuehrliche Umrechnung fuer das Fenster aus dem Nachrichten-Menue. */
    public static String details(CharSequence text) {
        return details(text, null);
    }

    /** Wie details, aber alle Betraege als Waehrung "from" behandeln (z. B. nach Auswahl bei "kr" oder "$"). */
    public static String details(CharSequence text, String from) {
        String to = target();
        ArrayList<Hit> hits = find(text);
        if (hits.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (Hit h : hits) {
            if (from != null) h.code = from;
            if (h.code.equals(to) || h.code.startsWith("?")) continue;
            double v = convert(h.amount, h.code, to);
            if (Double.isNaN(v)) continue;
            double one = convert(1, h.code, to);
            sb.append(h.text).append("  ≈  ").append(format(v, to)).append("\n")
              .append("   (1 ").append(h.code).append(" = ").append(format(one, to)).append(")\n");
        }
        if (sb.length() == 0) return null;
        String d = ratesDate();
        sb.append("\nKurse: Europäische Zentralbank").append(d != null ? ", Stand " + d : "");
        return sb.toString();
    }

    /** Automatische Anzeige: graue Zeile unter dem Text anhaengen (nur einmal). */
    public static CharSequence appendAuto(CharSequence text, int color) {
        try {
            if (!SparkConfig.get(SparkConfig.CURRENCY_AUTO) || TextUtils.isEmpty(text)) return text;
            if (text instanceof Spanned && ((Spanned) text).getSpans(0, text.length(), Mark.class).length > 0) return text;
            if (!ratesReady()) {
                refreshIfNeeded();
                return text;
            }
            refreshIfNeeded();
            String sum = summary(text);
            if (sum == null) return text;
            SpannableStringBuilder sb = new SpannableStringBuilder(text);
            int start = sb.length();
            sb.append("\n").append(sum);
            sb.setSpan(new Mark(), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(color), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return sb;
        } catch (Exception e) {
            FileLog.e(e);
            return text;
        }
    }
}

package org.telegram.messenger.spark;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Uebersetzer-Auswahl fuer Sparkgram: Google (ohne Schluessel, Telegrams eingebauter Ausweichweg)
 * oder DeepL (eigener API-Schluessel, z. B. "DeepL API Free").
 */
public class SparkTranslate {

    public static final int PROVIDER_GOOGLE = 0;
    public static final int PROVIDER_DEEPL = 1;

    public static int provider() {
        return SparkConfig.prefs().getInt("translateProvider", PROVIDER_GOOGLE);
    }

    public static void setProvider(int p) {
        SparkConfig.prefs().edit().putInt("translateProvider", p).apply();
    }

    public static String deeplKey() {
        return SparkConfig.prefs().getString("deeplKey", null);
    }

    public static void setDeeplKey(String key) {
        if (TextUtils.isEmpty(key == null ? null : key.trim())) {
            SparkConfig.prefs().edit().remove("deeplKey").apply();
        } else {
            SparkConfig.prefs().edit().putString("deeplKey", key.trim()).apply();
        }
    }

    public static boolean useDeepl() {
        return provider() == PROVIDER_DEEPL && !TextUtils.isEmpty(deeplKey());
    }

    /** Synchron (Hintergrund-Thread!). Liefert die Uebersetzung oder null bei Fehler. */
    public static String deepl(String text, String toLang) {
        String key = deeplKey();
        if (TextUtils.isEmpty(key) || text == null) {
            return null;
        }
        HttpURLConnection c = null;
        try {
            // Free-Schluessel enden auf ":fx" und nutzen api-free
            String host = key.endsWith(":fx") ? "https://api-free.deepl.com" : "https://api.deepl.com";
            c = (HttpURLConnection) new URL(host + "/v2/translate").openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setRequestProperty("Authorization", "DeepL-Auth-Key " + key);
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String target = toLang == null ? "DE" : toLang.toUpperCase();
            if (target.startsWith("EN")) target = "EN-GB";
            if (target.startsWith("PT")) target = "PT-PT";
            String body = "text=" + URLEncoder.encode(text, "UTF-8") + "&target_lang=" + URLEncoder.encode(target, "UTF-8");
            try (OutputStream out = c.getOutputStream()) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }
            if (c.getResponseCode() != 200) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            JSONArray arr = new JSONObject(sb.toString()).getJSONArray("translations");
            StringBuilder res = new StringBuilder();
            for (int i = 0; i < arr.length(); i++) res.append(arr.getJSONObject(i).optString("text"));
            return res.toString();
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }
}

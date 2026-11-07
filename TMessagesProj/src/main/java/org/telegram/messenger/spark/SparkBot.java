package org.telegram.messenger.spark;

import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Senden ueber den eigenen Bot (Telegram Bot API). Der Token bleibt nur auf dem Geraet.
 * Der Bot muss im Ziel-Chat Mitglied sein (Gruppen/Kanaele) bzw. von der Person gestartet worden sein.
 */
public class SparkBot {

    public static class Result {
        public boolean ok;
        public String error;
        public String botName;
    }

    public static String token() {
        return SparkConfig.prefs().getString("botToken", null);
    }

    public static String botName() {
        return SparkConfig.prefs().getString("botName", null);
    }

    public static void setToken(String token, String name) {
        if (TextUtils.isEmpty(token == null ? null : token.trim())) {
            SparkConfig.prefs().edit().remove("botToken").remove("botName").apply();
        } else {
            SparkConfig.prefs().edit().putString("botToken", token.trim()).putString("botName", name).apply();
        }
    }

    /** Bot-API-Chat-ID aus einer Telegram-Dialog-ID. */
    public static String chatId(int account, long dialogId) {
        if (dialogId > 0) {
            return String.valueOf(dialogId);
        }
        TLRPC.Chat chat = MessagesController.getInstance(account).getChat(-dialogId);
        if (chat != null && org.telegram.messenger.ChatObject.isChannel(chat)) {
            return "-100" + (-dialogId);
        }
        return String.valueOf(dialogId);
    }

    private static JSONObject call(String token, String method, String body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("https://api.telegram.org/bot" + token + "/" + method).openConnection();
        try {
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            if (body != null) {
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
                try (OutputStream os = c.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int code = c.getResponseCode();
            InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
            StringBuilder sb = new StringBuilder();
            if (in != null) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }
            }
            return new JSONObject(sb.length() > 0 ? sb.toString() : "{\"ok\":false,\"description\":\"HTTP " + code + "\"}");
        } finally {
            c.disconnect();
        }
    }

    /** Prueft einen Token (Hintergrund-Thread!). */
    public static Result check(String token) {
        Result r = new Result();
        try {
            JSONObject o = call(token, "getMe", null);
            r.ok = o.optBoolean("ok");
            if (r.ok) {
                JSONObject u = o.getJSONObject("result");
                r.botName = "@" + u.optString("username");
            } else {
                r.error = o.optString("description", "unbekannter Fehler");
            }
        } catch (Exception e) {
            FileLog.e(e);
            r.error = e.getMessage();
        }
        return r;
    }

    /** Sendet Text an einen Chat (Hintergrund-Thread!). replyToMsgId 0 = keine Antwort. */
    public static Result send(int account, long dialogId, String text, int replyToMsgId) {
        Result r = new Result();
        String token = token();
        if (TextUtils.isEmpty(token)) {
            r.error = "Kein Bot-Token hinterlegt (Sparkgram-Einstellungen → Bot).";
            return r;
        }
        try {
            String body = "chat_id=" + URLEncoder.encode(chatId(account, dialogId), "UTF-8")
                + "&text=" + URLEncoder.encode(text, "UTF-8");
            if (replyToMsgId > 0) {
                body += "&reply_parameters=" + URLEncoder.encode("{\"message_id\":" + replyToMsgId + ",\"allow_sending_without_reply\":true}", "UTF-8");
            }
            JSONObject o = call(token, "sendMessage", body);
            r.ok = o.optBoolean("ok");
            if (!r.ok) {
                r.error = explain(o.optString("description", "unbekannter Fehler"));
            }
        } catch (Exception e) {
            FileLog.e(e);
            r.error = e.getMessage();
        }
        return r;
    }

    private static String explain(String d) {
        String l = d.toLowerCase();
        if (l.contains("chat not found")) return "Chat nicht gefunden – der Bot ist hier kein Mitglied bzw. wurde von der Person nicht gestartet.";
        if (l.contains("not enough rights") || l.contains("have no rights")) return "Der Bot darf hier nicht schreiben (Rechte fehlen).";
        if (l.contains("bot was kicked") || l.contains("not a member")) return "Der Bot ist nicht (mehr) in diesem Chat.";
        if (l.contains("blocked by the user")) return "Die Person hat den Bot blockiert.";
        if (l.contains("unauthorized")) return "Der Bot-Token ist ungültig.";
        return d;
    }
}

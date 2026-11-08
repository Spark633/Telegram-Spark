package org.telegram.messenger.spark;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;

import java.util.ArrayList;

/**
 * Admin-Vorlagen: Nachricht antippen -> "✦ Admin" -> Vorlage. Pro Vorlage: Nachricht loeschen, Stufen mit
 * eigener Aktion/Dauer/Text. Zaehler lokal pro Konto, Gruppe, Person und Vorlage.
 */
public class SparkAdmin {

    public static final int ACT_NONE = 0;
    public static final int ACT_MUTE = 1;
    public static final int ACT_NO_MEDIA = 2;
    public static final int ACT_NO_STICKERS = 3;
    public static final int ACT_NO_LINKS = 4;
    public static final int ACT_KICK = 5;
    public static final int ACT_BAN = 6;

    public static final String[] ACTION_NAMES = {"Keine Einschränkung", "Stumm (gar nichts senden)", "Keine Medien (Fotos, Videos, Dateien, Sprache)", "Keine Sticker & GIFs", "Keine Links", "Entfernen (kann wieder beitreten)", "Bannen"};

    // Einschraenkungen (kombinierbar) als Bits
    public static final int R_MUTE = 1;
    public static final int R_MEDIA = 2;
    public static final int R_STICKERS = 4;
    public static final int R_LINKS = 8;
    public static final int R_POLLS = 16;
    public static final int R_INVITE = 32;
    public static final int[] R_BITS = {R_MUTE, R_MEDIA, R_STICKERS, R_LINKS, R_POLLS, R_INVITE};
    public static final String[] R_NAMES = {"Stumm (gar nichts senden)", "Keine Medien (Fotos, Videos, Dateien, Sprache)", "Keine Sticker & GIFs", "Keine Links", "Keine Umfragen", "Niemanden einladen"};
    public static final String[] R_SHORT = {"stumm", "keine Medien", "keine Sticker/GIFs", "keine Links", "keine Umfragen", "nicht einladen"};

    public static final int REMOVE_NONE = 0;
    public static final int REMOVE_KICK = 1;
    public static final int REMOVE_BAN = 2;
    public static final int[] DURATIONS = {3600, 86400, 7 * 86400, 30 * 86400, 90 * 86400, 0};
    public static final String[] DURATION_NAMES = {"1 Stunde", "1 Tag", "1 Woche", "1 Monat", "3 Monate", "dauerhaft"};

    public static class Stage {
        public int restrictions; // R_* Bits
        public int remove;       // REMOVE_*
        public int duration;     // Sekunden, 0 = dauerhaft
        public String text = "";
    }

    /** Alte Einzel-Aktion (bis 0.17) in Bits umwandeln. */
    private static void fromLegacy(Stage s, int action) {
        switch (action) {
            case ACT_MUTE: s.restrictions = R_MUTE; break;
            case ACT_NO_MEDIA: s.restrictions = R_MEDIA; break;
            case ACT_NO_STICKERS: s.restrictions = R_STICKERS; break;
            case ACT_NO_LINKS: s.restrictions = R_LINKS; break;
            case ACT_KICK: s.remove = REMOVE_KICK; break;
            case ACT_BAN: s.remove = REMOVE_BAN; break;
        }
    }

    public static class Template {
        public long id;
        public String name = "";
        public boolean deleteMessage = true;
        public boolean confirm = true;
        public ArrayList<Stage> stages = new ArrayList<>();
    }

    // ------------------------------------------------------------------ Speichern

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_admin", Context.MODE_PRIVATE);
    }

    private static SharedPreferences counts(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_admin_counts_" + account, Context.MODE_PRIVATE);
    }

    public static ArrayList<Template> templates() {
        ArrayList<Template> list = new ArrayList<>();
        String raw = prefs().getString("templates", null);
        if (raw == null) {
            list.add(defaultTemplate());
            save(list);
            return list;
        }
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Template t = new Template();
                t.id = o.optLong("id");
                t.name = o.optString("n");
                t.deleteMessage = o.optBoolean("del", true);
                t.confirm = o.optBoolean("conf", true);
                JSONArray st = o.optJSONArray("s");
                if (st != null) {
                    for (int k = 0; k < st.length(); k++) {
                        JSONObject so = st.getJSONObject(k);
                        Stage s = new Stage();
                        if (so.has("r") || so.has("x")) {
                            s.restrictions = so.optInt("r");
                            s.remove = so.optInt("x");
                        } else {
                            fromLegacy(s, so.optInt("a"));
                        }
                        s.duration = so.optInt("d");
                        s.text = so.optString("t", "");
                        t.stages.add(s);
                    }
                }
                list.add(t);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public static void save(ArrayList<Template> list) {
        JSONArray a = new JSONArray();
        try {
            for (Template t : list) {
                JSONObject o = new JSONObject();
                o.put("id", t.id);
                o.put("n", t.name);
                o.put("del", t.deleteMessage);
                o.put("conf", t.confirm);
                JSONArray st = new JSONArray();
                for (Stage s : t.stages) {
                    JSONObject so = new JSONObject();
                    so.put("r", s.restrictions);
                    so.put("x", s.remove);
                    so.put("d", s.duration);
                    so.put("t", s.text == null ? "" : s.text);
                    st.put(so);
                }
                o.put("s", st);
                a.put(o);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        prefs().edit().putString("templates", a.toString()).apply();
    }

    public static Template find(long id) {
        for (Template t : templates()) if (t.id == id) return t;
        return null;
    }

    public static void put(Template t) {
        ArrayList<Template> list = templates();
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == t.id) {
                list.set(i, t);
                found = true;
            }
        }
        if (!found) list.add(t);
        save(list);
    }

    public static void remove(long id) {
        ArrayList<Template> list = templates();
        for (int i = list.size() - 1; i >= 0; i--) if (list.get(i).id == id) list.remove(i);
        save(list);
    }

    public static Template newTemplate() {
        Template t = new Template();
        t.id = System.currentTimeMillis();
        t.name = "Neue Vorlage";
        Stage s = new Stage();
        s.restrictions = R_MUTE;
        s.duration = 86400;
        s.text = "{user} …";
        t.stages.add(s);
        return t;
    }

    private static Template defaultTemplate() {
        Template t = new Template();
        t.id = 1;
        t.name = "RL-Bilder";
        t.deleteMessage = true;
        t.confirm = true;
        int[] d = {30 * 86400, 90 * 86400, 0};
        for (int dur : d) {
            Stage s = new Stage();
            s.restrictions = R_MEDIA;
            s.duration = dur;
            s.text = "{user} restricted for sending RL pics.";
            t.stages.add(s);
        }
        return t;
    }

    // ------------------------------------------------------------------ Zaehler

    private static String key(long chatId, long userId, long templateId) {
        return chatId + "_" + userId + "_" + templateId;
    }

    public static int count(int account, long chatId, long userId, long templateId) {
        return counts(account).getInt(key(chatId, userId, templateId), 0);
    }

    public static void setCount(int account, long chatId, long userId, long templateId, int value) {
        if (value <= 0) counts(account).edit().remove(key(chatId, userId, templateId)).apply();
        else counts(account).edit().putInt(key(chatId, userId, templateId), value).apply();
    }

    public static class Count {
        public long userId;
        public long templateId;
        public int count;
    }

    /** Alle Zaehler einer Gruppe. */
    public static ArrayList<Count> countsForChat(int account, long chatId) {
        ArrayList<Count> list = new ArrayList<>();
        String prefix = chatId + "_";
        for (java.util.Map.Entry<String, ?> e : counts(account).getAll().entrySet()) {
            if (!e.getKey().startsWith(prefix) || !(e.getValue() instanceof Integer)) continue;
            String[] parts = e.getKey().split("_");
            if (parts.length != 3) continue;
            try {
                Count c = new Count();
                c.userId = Long.parseLong(parts[1]);
                c.templateId = Long.parseLong(parts[2]);
                c.count = (Integer) e.getValue();
                if (c.count > 0) list.add(c);
            } catch (NumberFormatException ignore) {
            }
        }
        return list;
    }

    /** Stufe (0-basiert), die beim naechsten Anwenden greift. */
    public static int nextStage(int account, long chatId, long userId, Template t) {
        if (t.stages.isEmpty()) return -1;
        return Math.min(count(account, chatId, userId, t.id), t.stages.size() - 1);
    }

    public static String describe(Stage s) {
        if (s.remove == REMOVE_KICK) return "Entfernen (kann wieder beitreten)";
        if (s.remove == REMOVE_BAN) return "Bannen · " + durationName(s.duration);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < R_BITS.length; i++) {
            if ((s.restrictions & R_BITS[i]) != 0) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(R_SHORT[i]);
            }
        }
        if (sb.length() == 0) return "Keine Einschränkung";
        return sb + " · " + durationName(s.duration);
    }

    public static String durationName(int seconds) {
        for (int i = 0; i < DURATIONS.length; i++) if (DURATIONS[i] == seconds) return DURATION_NAMES[i];
        return seconds == 0 ? "dauerhaft" : (seconds / 86400) + " Tage";
    }

    // ------------------------------------------------------------------ Ausfuehren

    public interface Done {
        void run(String error);
    }

    /** Fuehrt die naechste Stufe der Vorlage gegen den Absender von mo aus und erhoeht den Zaehler. */
    public static void execute(BaseFragment fragment, MessageObject mo, Template t, Done done) {
        final int account = fragment.getCurrentAccount();
        final MessagesController mc = MessagesController.getInstance(account);
        final long dialogId = mo.getDialogId();
        final long chatId = -dialogId;
        final long userId = mo.getFromChatId();
        final TLRPC.Chat chat = mc.getChat(chatId);
        final TLRPC.User user = mc.getUser(userId);
        if (chat == null || user == null || dialogId >= 0) {
            done.run("Nur in Gruppen bei Nachrichten von Personen möglich");
            return;
        }
        int stageIndex = nextStage(account, chatId, userId, t);
        if (stageIndex < 0) {
            done.run("Die Vorlage hat keine Stufen");
            return;
        }
        Stage s = t.stages.get(stageIndex);
        boolean channel = ChatObject.isChannel(chat);
        String error = null;

        // 1. Nachricht loeschen
        if (t.deleteMessage) {
            ArrayList<Integer> ids = new ArrayList<>();
            ids.add(mo.getId());
            mc.deleteMessages(ids, null, null, dialogId, 0, true, 0);
        }

        // 2. Entfernen/Bannen oder Einschraenkungen (kombiniert)
        if (s.remove != REMOVE_NONE || s.restrictions != 0) {
            int now = (int) (System.currentTimeMillis() / 1000);
            if (!channel) {
                if (s.remove != REMOVE_NONE) {
                    mc.deleteParticipantFromChat(chatId, user);
                } else {
                    error = "Einschränkungen gehen nur in Supergruppen – Löschen/Text wurden trotzdem ausgeführt";
                }
            } else {
                TLRPC.TL_chatBannedRights r = new TLRPC.TL_chatBannedRights();
                if (s.remove != REMOVE_NONE) {
                    r.view_messages = true;
                    r.until_date = s.remove == REMOVE_KICK ? now + 60 : (s.duration == 0 ? 0 : now + s.duration);
                } else {
                    int m = s.restrictions;
                    if ((m & R_MUTE) != 0) {
                        r.send_messages = r.send_media = r.send_stickers = r.send_gifs = r.send_games = r.send_inline = r.embed_links = r.send_polls = true;
                        r.send_photos = r.send_videos = r.send_roundvideos = r.send_audios = r.send_voices = r.send_docs = r.send_plain = true;
                    }
                    if ((m & R_MEDIA) != 0) {
                        r.send_media = r.send_photos = r.send_videos = r.send_roundvideos = r.send_audios = r.send_voices = r.send_docs = true;
                    }
                    if ((m & R_STICKERS) != 0) {
                        r.send_stickers = r.send_gifs = true;
                    }
                    if ((m & R_LINKS) != 0) {
                        r.embed_links = true;
                    }
                    if ((m & R_POLLS) != 0) {
                        r.send_polls = true;
                    }
                    if ((m & R_INVITE) != 0) {
                        r.invite_users = true;
                    }
                    r.until_date = s.duration == 0 ? 0 : now + s.duration;
                }
                mc.setParticipantBannedRole(chatId, user, null, r, true, fragment);
            }
        }

        // 3. Text in die Gruppe
        if (!TextUtils.isEmpty(s.text == null ? null : s.text.trim())) {
            sendText(account, dialogId, user, s.text.trim());
        }

        setCount(account, chatId, userId, t.id, count(account, chatId, userId, t.id) + 1);
        done.run(error);
    }

    /** {user} wird zu @benutzername bzw. zu einer Erwaehnung mit Namen. */
    private static void sendText(int account, long dialogId, TLRPC.User user, String template) {
        String username = UserObject.getPublicUsername(user);
        String mention = username != null ? "@" + username : UserObject.getFirstName(user);
        ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        while (true) {
            int p = template.indexOf("{user}", idx);
            if (p < 0) {
                sb.append(template.substring(idx));
                break;
            }
            sb.append(template, idx, p);
            if (username == null) {
                TLRPC.TL_inputMessageEntityMentionName e = new TLRPC.TL_inputMessageEntityMentionName();
                e.user_id = MessagesController.getInstance(account).getInputUser(user);
                e.offset = sb.length();
                e.length = mention.length();
                entities.add(e);
            }
            sb.append(mention);
            idx = p + "{user}".length();
        }
        SendMessagesHelper.SendMessageParams p = SendMessagesHelper.SendMessageParams.of(sb.toString(), dialogId);
        p.entities = entities.isEmpty() ? null : entities;
        p.searchLinks = false;
        SparkPersona.suppress = true; // Admin-Text nie mit Persona-Namen
        try {
            SendMessagesHelper.getInstance(account).sendMessage(p);
        } finally {
            SparkPersona.suppress = false;
        }
    }
}

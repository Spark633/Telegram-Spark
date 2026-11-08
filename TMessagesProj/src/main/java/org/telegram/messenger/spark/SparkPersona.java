package org.telegram.messenger.spark;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/**
 * RP-Persona pro Chat (pro Konto): Ist fuer einen Chat eine Persona aktiv, beginnt jede eigene Textnachricht
 * dort mit dem Persona-Namen in Fett, z. B. "**Luna:** Hallo". Nur lokal gespeichert.
 */
public class SparkPersona {

    private static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_persona_" + account, Context.MODE_PRIVATE);
    }

    /** Persona-Name des Chats (auch wenn pausiert) oder null. */
    public static String name(int account, long dialogId) {
        return prefs(account).getString("n" + dialogId, null);
    }

    public static boolean isActive(int account, long dialogId) {
        return SparkConfig.get(SparkConfig.PERSONA) && !TextUtils.isEmpty(name(account, dialogId)) && prefs(account).getBoolean("a" + dialogId, true);
    }

    public static void set(int account, long dialogId, String name) {
        if (TextUtils.isEmpty(name == null ? null : name.trim())) {
            prefs(account).edit().remove("n" + dialogId).remove("a" + dialogId).apply();
        } else {
            prefs(account).edit().putString("n" + dialogId, name.trim()).putBoolean("a" + dialogId, true).apply();
        }
    }

    public static void setActive(int account, long dialogId, boolean active) {
        prefs(account).edit().putBoolean("a" + dialogId, active).apply();
    }

    /** Waehrend true werden keine Persona-Namen gesetzt (z. B. fuer Admin-Texte). */
    public static boolean suppress;

    /** Vor dem Senden: Persona-Namen vor eine reine Textnachricht setzen. */
    public static void apply(int account, SendMessagesHelper.SendMessageParams p) {
        try {
            if (suppress || p == null || p.message == null || p.retryMessageObject != null) return;
            if (p.photo != null || p.document != null || p.location != null || p.poll != null || p.game != null || p.invoice != null || p.user != null) return;
            if (TextUtils.isEmpty(p.message.trim()) || !isActive(account, p.peer)) return;
            String raw = name(account, p.peer);
            // Klammer-Paar in der Persona ("[]", "{}", "()") = Nachricht damit umschliessen
            String open = null, close = null;
            String[][] pairs = {{"[", "]"}, {"{", "}"}, {"(", ")"}};
            for (String[] pr : pairs) {
                if (raw.contains(pr[0] + pr[1])) {
                    open = pr[0];
                    close = pr[1];
                    raw = raw.replace(pr[0] + pr[1], "");
                    break;
                }
            }
            final String n = raw.trim();
            if (p.message.startsWith("/")) return; // Bot-Befehl
            if (!n.isEmpty() && p.message.startsWith(n + ":")) return; // schon mit Persona geschrieben
            if (open != null && p.message.startsWith(open) && p.message.trim().endsWith(close)) open = null; // schon umschlossen
            String prefix = (n.isEmpty() ? "" : n + ": ") + (open != null ? open : "");
            p.message = prefix + p.message + (open != null ? close : "");
            ArrayList<TLRPC.MessageEntity> ents = p.entities != null ? new ArrayList<>(p.entities) : new ArrayList<>();
            for (TLRPC.MessageEntity e : ents) e.offset += prefix.length();
            if (!n.isEmpty()) {
                TLRPC.TL_messageEntityBold bold = new TLRPC.TL_messageEntityBold();
                bold.offset = 0;
                bold.length = n.length() + 1;
                ents.add(0, bold);
            }
            p.entities = ents;
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
        }
    }
}

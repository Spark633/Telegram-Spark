package org.telegram.messenger.spark;

import org.telegram.tgnet.TLRPC;

import java.util.HashMap;

/**
 * Gemeinsame Bremse fuer alle Sparkgram-Funktionen, die viele Anfragen an Telegram schicken.
 * - respektiert FLOOD_WAIT_x: die Funktion pausiert komplett fuer x Sekunden (+ Puffer)
 * - festes Stundenbudget pro Funktion
 */
public class SparkRateLimit {

    private static class State {
        long pausedUntil;
        long windowStart;
        int used;
    }

    private static final HashMap<String, State> states = new HashMap<>();

    private static State state(String feature) {
        State s = states.get(feature);
        if (s == null) {
            states.put(feature, s = new State());
        }
        return s;
    }

    /** Wie lange (ms) die Funktion noch warten muss; 0 = darf jetzt eine Anfrage senden. */
    public static synchronized long waitTime(String feature, int perHour) {
        State s = state(feature);
        long now = System.currentTimeMillis();
        if (now < s.pausedUntil) {
            return s.pausedUntil - now;
        }
        if (now - s.windowStart >= 3600_000L) {
            s.windowStart = now;
            s.used = 0;
        }
        if (s.used >= perHour) {
            return s.windowStart + 3600_000L - now;
        }
        return 0;
    }

    /** Vor dem Senden aufrufen. */
    public static synchronized void use(String feature) {
        state(feature).used++;
    }

    /** Nach einer Antwort aufrufen; erkennt FLOOD_WAIT und pausiert die Funktion. */
    public static synchronized void onError(String feature, TLRPC.TL_error error) {
        if (error == null || error.text == null) {
            return;
        }
        if (error.text.startsWith("FLOOD_WAIT_") || error.text.startsWith("FLOOD_PREMIUM_WAIT_")) {
            int seconds = 60;
            try {
                seconds = Integer.parseInt(error.text.substring(error.text.lastIndexOf('_') + 1));
            } catch (Exception ignore) {
            }
            // grosszuegiger Puffer: doppelte Wartezeit, mindestens 2 Minuten
            long pause = Math.max(120_000L, seconds * 2000L);
            state(feature).pausedUntil = System.currentTimeMillis() + pause;
        }
    }
}

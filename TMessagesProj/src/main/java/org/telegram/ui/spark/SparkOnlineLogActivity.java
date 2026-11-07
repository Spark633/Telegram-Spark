package org.telegram.ui.spark;

import android.view.View;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.spark.SparkOnlineLog;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

/** Online-Protokoll einer Person: echte Online-Zeiten und getrennt davon verborgene Angaben. */
public class SparkOnlineLogActivity extends UniversalFragment {

    private static final int ID_CLEAR = 1;

    private final long userId;

    public SparkOnlineLogActivity(long userId) {
        this.userId = userId;
    }

    @Override
    protected CharSequence getTitle() {
        TLRPC.User u = MessagesController.getInstance(currentAccount).getUser(userId);
        return "Online-Protokoll" + (u != null ? " · " + UserObject.getUserName(u) : "");
    }

    private static String day(long sec) {
        return new SimpleDateFormat("EEEE, dd.MM.yyyy", Locale.GERMANY).format(new Date(sec * 1000));
    }

    private static String time(long sec) {
        return new SimpleDateFormat("HH:mm", Locale.GERMANY).format(new Date(sec * 1000));
    }

    private static String full(long sec) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(new Date(sec * 1000));
    }

    private static String duration(long sec) {
        if (sec < 60) return "< 1 min";
        long m = sec / 60;
        if (m < 60) return m + " min";
        return (m / 60) + " h " + (m % 60) + " min";
    }

    private static class Line {
        long sortTime;
        String text;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        ArrayList<SparkOnlineLog.Event> ev = SparkOnlineLog.get(currentAccount).events(userId);
        ArrayList<Line> lines = new ArrayList<>();
        long lastReal = 0;
        long openStart = 0;
        for (SparkOnlineLog.Event e : ev) {
            switch (e.kind) {
                case SparkOnlineLog.KIND_ONLINE:
                    if (openStart == 0) openStart = e.time;
                    lastReal = Math.max(lastReal, e.time);
                    break;
                case SparkOnlineLog.KIND_OFFLINE: {
                    Line l = new Line();
                    if (openStart != 0 && e.time >= openStart) {
                        l.sortTime = openStart;
                        l.text = "🟢 " + time(openStart) + " – " + time(e.time) + "  (" + duration(e.time - openStart) + ")";
                    } else {
                        l.sortTime = e.time;
                        l.text = "⚪ zuletzt online " + time(e.time);
                    }
                    openStart = 0;
                    lastReal = Math.max(lastReal, e.time);
                    lines.add(l);
                    break;
                }
                default: {
                    if (openStart != 0) {
                        Line l = new Line();
                        l.sortTime = openStart;
                        l.text = "🟢 ab " + time(openStart) + " (Ende unbekannt)";
                        lines.add(l);
                        openStart = 0;
                    }
                    Line l = new Line();
                    l.sortTime = e.time;
                    String what = e.kind == SparkOnlineLog.KIND_RECENTLY ? "„kürzlich“" : e.kind == SparkOnlineLog.KIND_LAST_WEEK ? "„letzte Woche“" : "„letzten Monat“";
                    l.text = "⚠ " + time(e.time) + "  Telegram zeigte " + what + " (genaue Zeit verborgen)";
                    lines.add(l);
                    break;
                }
            }
        }
        if (openStart != 0) {
            Line l = new Line();
            l.sortTime = openStart;
            TLRPC.User u = MessagesController.getInstance(currentAccount).getUser(userId);
            boolean nowOnline = u != null && u.status instanceof TLRPC.TL_userStatusOnline;
            l.text = "🟢 ab " + time(openStart) + (nowOnline ? "  · gerade online" : " (Ende unbekannt)");
            lines.add(l);
        }

        String head = lastReal > 0 ? "Zuletzt echt gesehen: " + full(lastReal) : "Noch keine echte Online-Zeit erfasst.";
        items.add(UItem.asShadow(head + "\n\nErfasst wird, solange Sparkgram verbunden ist und Telegram den Status meldet (meist Kontakte und Leute aus offenen Chats). Verborgene Angaben (⚠) zählen nicht als „gesehen“. Einträge werden 30 Tage aufbewahrt."));

        Collections.sort(lines, (a, b) -> Long.compare(b.sortTime, a.sortTime));
        String currentDay = null;
        StringBuilder sb = new StringBuilder();
        for (Line l : lines) {
            String d = day(l.sortTime);
            if (!d.equals(currentDay)) {
                if (sb.length() > 0) {
                    items.add(UItem.asShadow(sb.toString()));
                    sb.setLength(0);
                }
                currentDay = d;
                items.add(UItem.asHeader(d));
            }
            if (sb.length() > 0) sb.append('\n');
            sb.append(l.text);
        }
        if (sb.length() > 0) {
            items.add(UItem.asShadow(sb.toString()));
        }
        if (!lines.isEmpty()) {
            items.add(UItem.asButton(ID_CLEAR, "Protokoll dieser Person löschen").red());
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_CLEAR) {
            AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
            b.setTitle("Protokoll löschen?");
            b.setPositiveButton("Löschen", (d, w) -> {
                SparkOnlineLog.get(currentAccount).clear(userId);
                org.telegram.messenger.AndroidUtilities.runOnUIThread(() -> listView.adapter.update(true), 300);
            });
            b.setNegativeButton("Abbrechen", null);
            showDialog(b.create());
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}

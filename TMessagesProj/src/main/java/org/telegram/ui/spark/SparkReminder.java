package org.telegram.ui.spark;

import android.text.TextUtils;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * "An Nachricht erinnern": legt eine geplante Nachricht in "Gespeicherte Nachrichten" an
 * (Telegram meldet sie zur gewaehlten Zeit als Erinnerung) – mit Chatname, Textauszug und Link.
 */
public class SparkReminder {

    public static void show(BaseFragment fragment, MessageObject mo) {
        if (fragment.getParentActivity() == null || mo == null) return;
        CharSequence[] items = {"In 30 Minuten", "In 1 Stunde", "In 3 Stunden", "Heute Abend (20:00)", "Morgen früh (9:00)", "Eigene Zeit …"};
        AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity());
        b.setTitle(SparkUi.mark("Erinnern"));
        b.setItems(items, (d, w) -> {
            long now = System.currentTimeMillis();
            Calendar c = Calendar.getInstance();
            switch (w) {
                case 0: schedule(fragment, mo, (int) ((now + 30 * 60_000L) / 1000)); break;
                case 1: schedule(fragment, mo, (int) ((now + 60 * 60_000L) / 1000)); break;
                case 2: schedule(fragment, mo, (int) ((now + 3 * 60 * 60_000L) / 1000)); break;
                case 3:
                    c.set(Calendar.HOUR_OF_DAY, 20); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0);
                    if (c.getTimeInMillis() < now + 60_000L) c.add(Calendar.DAY_OF_YEAR, 1);
                    schedule(fragment, mo, (int) (c.getTimeInMillis() / 1000));
                    break;
                case 4:
                    c.add(Calendar.DAY_OF_YEAR, 1);
                    c.set(Calendar.HOUR_OF_DAY, 9); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0);
                    schedule(fragment, mo, (int) (c.getTimeInMillis() / 1000));
                    break;
                default: {
                    long self = fragment.getUserConfig().getClientUserId();
                    AlertsCreator.createScheduleDatePickerDialog(fragment.getParentActivity(), self,
                        (notify, date, repeat) -> schedule(fragment, mo, date)).show();
                }
            }
        });
        fragment.showDialog(b.create());
    }

    private static void schedule(BaseFragment fragment, MessageObject mo, int date) {
        final int account = fragment.getCurrentAccount();
        final long self = fragment.getUserConfig().getClientUserId();
        String text = "⏰ Erinnerung\n" + chatName(account, mo.getDialogId());
        String body = mo.messageOwner != null && !TextUtils.isEmpty(mo.messageOwner.message) ? mo.messageOwner.message : (mo.messageText != null ? mo.messageText.toString() : "");
        if (!TextUtils.isEmpty(body)) {
            body = body.replace('\n', ' ');
            if (body.length() > 300) body = body.substring(0, 300) + " …";
            text += ": „" + body + "“";
        }
        String link = link(account, mo);
        if (link != null) text += "\n" + link;
        SendMessagesHelper.SendMessageParams p = SendMessagesHelper.SendMessageParams.of(text, self);
        p.scheduleDate = date;
        p.notify = true;
        SendMessagesHelper.getInstance(account).sendMessage(p);
        SimpleDateFormat f = new SimpleDateFormat("dd.MM. 'um' HH:mm", Locale.GERMANY);
        BulletinFactory.of(fragment).createSimpleBulletin(org.telegram.messenger.R.raw.chats_infotip,
            "✦ Erinnerung am " + f.format(new Date(date * 1000L)) + " (in Gespeicherte)").show();
    }

    private static String chatName(int account, long dialogId) {
        MessagesController mc = MessagesController.getInstance(account);
        if (dialogId > 0) {
            TLRPC.User u = mc.getUser(dialogId);
            return u != null ? UserObject.getUserName(u) : "Chat";
        }
        TLRPC.Chat c = mc.getChat(-dialogId);
        return c != null ? c.title : "Chat";
    }

    /** t.me-Link zur Nachricht (oeffentlich oder t.me/c/…); bei Privatchats null. */
    private static String link(int account, MessageObject mo) {
        long did = mo.getDialogId();
        if (did >= 0) {
            TLRPC.User u = MessagesController.getInstance(account).getUser(did);
            String un = u != null ? UserObject.getPublicUsername(u) : null;
            return un != null ? "https://t.me/" + un : null;
        }
        TLRPC.Chat c = MessagesController.getInstance(account).getChat(-did);
        if (c == null) return null;
        String un = ChatObject.getPublicUsername(c);
        if (!TextUtils.isEmpty(un)) return "https://t.me/" + un + "/" + mo.getId();
        if (ChatObject.isChannel(c)) return "https://t.me/c/" + c.id + "/" + mo.getId();
        return null;
    }
}

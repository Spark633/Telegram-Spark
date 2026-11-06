package org.telegram.ui.spark;

import static org.telegram.messenger.LocaleController.getString;

import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.spark.SparkConfig;
import org.telegram.messenger.spark.SparkProfiles;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

/** Sparkgram-Eintraege im Drei-Punkte-Menue von Profilen, Gruppen und Kanaelen. */
public class SparkProfileMenu {

    public static final int ID_COPY_ID = 900;
    public static final int ID_INFO = 901;
    public static final int ID_CLONE = 902;

    public static boolean isSparkItem(int id) {
        return id >= ID_COPY_ID && id <= ID_CLONE;
    }

    public static void addItems(BaseFragment fragment, ActionBarMenuItem menu, long userId, long chatId) {
        if (menu == null || !SparkConfig.get(SparkConfig.PROFILE_TOOLS) || (userId == 0 && chatId == 0)) {
            return;
        }
        menu.addSubItem(ID_COPY_ID, R.drawable.msg_copy, getString(R.string.SparkCopyId));
        menu.addSubItem(ID_INFO, R.drawable.msg_info, getString(R.string.SparkUserInfo));
        if (userId != 0 && userId != UserConfig.getInstance(fragment.getCurrentAccount()).getClientUserId()) {
            menu.addSubItem(ID_CLONE, R.drawable.msg_customize, getString(R.string.SparkCloneProfile));
        }
    }

    public static String peerId(int account, long userId, long chatId) {
        if (userId != 0) {
            return String.valueOf(userId);
        }
        TLRPC.Chat chat = MessagesController.getInstance(account).getChat(chatId);
        return (ChatObject.isChannel(chat) ? "-100" : "-") + chatId;
    }

    public static void handle(BaseFragment fragment, int id, long userId, long chatId) {
        final int account = fragment.getCurrentAccount();
        switch (id) {
            case ID_COPY_ID: {
                String value = peerId(account, userId, chatId);
                AndroidUtilities.addToClipboard(value);
                BulletinFactory.of(fragment).createCopyBulletin(getString(R.string.SparkIdCopied) + ": " + value).show();
                break;
            }
            case ID_INFO:
                showInfo(fragment, userId, chatId);
                break;
            case ID_CLONE:
                confirmClone(fragment, userId);
                break;
        }
    }

    private static void line(StringBuilder sb, String key, Object value) {
        if (value == null || TextUtils.isEmpty(String.valueOf(value))) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(key).append(": ").append(value);
    }

    private static String yes(boolean b) {
        return b ? "ja" : "nein";
    }

    private static String date(int unixTime) {
        if (unixTime <= 0) {
            return null;
        }
        return LocaleController.formatDateTime(unixTime, false);
    }

    private static void showInfo(BaseFragment fragment, long userId, long chatId) {
        final int account = fragment.getCurrentAccount();
        MessagesController mc = MessagesController.getInstance(account);
        StringBuilder sb = new StringBuilder();
        String title;
        if (userId != 0) {
            TLRPC.User user = mc.getUser(userId);
            if (user == null) {
                return;
            }
            TLRPC.UserFull full = mc.getUserFull(userId);
            title = UserObject.getUserName(user);
            line(sb, "ID", user.id);
            String username = UserObject.getPublicUsername(user);
            line(sb, "Benutzername", username != null ? "@" + username : null);
            if (!TextUtils.isEmpty(user.phone)) {
                line(sb, "Telefon", "+" + user.phone);
            }
            if (user.photo != null && user.photo.dc_id != 0) {
                line(sb, "Rechenzentrum", "DC" + user.photo.dc_id);
            }
            line(sb, "Premium", yes(user.premium));
            if (user.bot) {
                line(sb, "Bot", yes(true));
            }
            if (user.verified) {
                line(sb, "Verifiziert", yes(true));
            }
            if (user.scam || user.fake) {
                line(sb, "Warnung", user.scam ? "als Betrug markiert" : "als Fake markiert");
            }
            if (full != null) {
                line(sb, "Gemeinsame Gruppen", full.common_chats_count);
                if (full.profile_photo != null) {
                    line(sb, "Profilbild gesetzt", date(full.profile_photo.date));
                }
            }
        } else {
            TLRPC.Chat chat = mc.getChat(chatId);
            if (chat == null) {
                return;
            }
            TLRPC.ChatFull full = mc.getChatFull(chatId);
            title = chat.title;
            line(sb, "ID", peerId(account, 0, chatId));
            String username = ChatObject.getPublicUsername(chat);
            line(sb, "Benutzername", username != null ? "@" + username : null);
            line(sb, "Typ", ChatObject.isChannel(chat) ? (chat.megagroup ? "Supergruppe" : "Kanal") : "Gruppe");
            if (chat.photo != null && chat.photo.dc_id != 0) {
                line(sb, "Rechenzentrum", "DC" + chat.photo.dc_id);
            }
            int members = full != null && full.participants_count > 0 ? full.participants_count : chat.participants_count;
            if (members > 0) {
                line(sb, "Mitglieder", members);
            }
            line(sb, "Erstellt/beigetreten", date(chat.date));
            if (full != null && full.chat_photo != null) {
                line(sb, "Bild gesetzt", date(full.chat_photo.date));
            }
        }
        final String text = sb.toString();
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(title);
        builder.setMessage(text);
        builder.setPositiveButton(getString(R.string.OK), null);
        builder.setNeutralButton(getString(R.string.Copy), (dialog, which) -> {
            AndroidUtilities.addToClipboard(text);
            BulletinFactory.of(fragment).createCopyBulletin(getString(R.string.TextCopied)).show();
        });
        fragment.showDialog(builder.create());
    }

    private static void confirmClone(BaseFragment fragment, long userId) {
        final int account = fragment.getCurrentAccount();
        final TLRPC.User user = MessagesController.getInstance(account).getUser(userId);
        if (user == null) {
            return;
        }
        final String name = UserObject.getUserName(user);
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(getString(R.string.SparkCloneProfile));
        builder.setMessage(LocaleController.formatString(R.string.SparkCloneConfirm, name));
        builder.setPositiveButton(getString(R.string.SparkCloneProfile), (dialog, which) -> {
            final AlertDialog progress = new AlertDialog(fragment.getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
            progress.show();
            SparkProfiles.cloneFrom(account, user, (ok, error) -> {
                progress.dismiss();
                if (ok) {
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, LocaleController.formatString(R.string.SparkCloneDone, name)).show();
                } else {
                    BulletinFactory.of(fragment).createErrorBulletin(LocaleController.formatString(R.string.SparkError, String.valueOf(error))).show();
                }
            });
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        fragment.showDialog(builder.create());
    }
}

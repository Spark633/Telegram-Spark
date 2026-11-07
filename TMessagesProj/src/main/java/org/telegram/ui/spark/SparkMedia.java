package org.telegram.ui.spark;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.spark.SparkBot;
import org.telegram.messenger.spark.SparkCollection;
import org.telegram.messenger.spark.SparkConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.DialogsActivity;

import java.io.File;
import java.util.ArrayList;

/** Etappe 6: Medien-Werkzeuge (Sticker, GIFs, Sprachnachrichten) und Senden ueber den eigenen Bot. */
public class SparkMedia {

    /** Gibt es fuer diese Nachricht Medien-Aktionen? */
    public static boolean hasActions(MessageObject mo) {
        if (mo == null || mo.messageOwner == null || mo.getId() <= 0) return false;
        int kind = SparkCollection.kindOf(mo);
        if (kind == 0) return false;
        return SparkConfig.get(SparkConfig.COLLECTION)
            || kind == SparkCollection.KIND_STICKER && SparkConfig.get(SparkConfig.STICKER_TOOLS)
            || kind == SparkCollection.KIND_VOICE && SparkConfig.get(SparkConfig.VOICE_TOOLS);
    }

    private interface Action {
        void run();
    }

    /** Auswahl der Medien-Aktionen fuer eine Nachricht. */
    public static void showActions(ChatActivity chat, MessageObject mo) {
        if (chat.getParentActivity() == null) return;
        final int kind = SparkCollection.kindOf(mo);
        final ArrayList<String> names = new ArrayList<>();
        final ArrayList<Action> actions = new ArrayList<>();
        if (SparkConfig.get(SparkConfig.COLLECTION)) {
            names.add("Zur Sammlung hinzufügen");
            actions.add(() -> addToCollection(chat, mo));
        }
        boolean sticker = kind == SparkCollection.KIND_STICKER && SparkConfig.get(SparkConfig.STICKER_TOOLS);
        boolean voice = kind == SparkCollection.KIND_VOICE && SparkConfig.get(SparkConfig.VOICE_TOOLS);
        if (sticker || voice || kind == SparkCollection.KIND_GIF && SparkConfig.get(SparkConfig.COLLECTION)) {
            names.add(voice ? "Sprachnachricht speichern (Downloads)" : "Datei speichern (Downloads)");
            actions.add(() -> saveToDownloads(chat, mo));
        }
        if (voice || sticker) {
            names.add("Ohne Absender an … senden");
            actions.add(() -> sendCopyTo(chat, mo));
        }
        if (sticker) {
            TLRPC.InputStickerSet set = MessageObject.getInputStickerSet(mo.getDocument());
            if (set != null && !(set instanceof TLRPC.TL_inputStickerSetEmpty)) {
                names.add("Pack-Link kopieren");
                actions.add(() -> copyPackLink(chat, set));
                names.add("Ganzes Pack herunterladen");
                actions.add(() -> downloadPack(chat, set));
            }
            names.add("In eigenes Sticker-Pack übernehmen");
            actions.add(() -> addToOwnPack(chat, mo.getDocument()));
        }
        AlertDialog.Builder b = new AlertDialog.Builder(chat.getParentActivity());
        b.setTitle(SparkUi.mark(SparkCollection.kindName(kind)));
        b.setItems(names.toArray(new CharSequence[0]), (d, which) -> actions.get(which).run());
        b.setNegativeButton(org.telegram.messenger.LocaleController.getString(R.string.Cancel), null);
        chat.showDialog(b.create());
    }

    // ------------------------------------------------------------ Sammlung

    private static void addToCollection(ChatActivity chat, MessageObject mo) {
        SparkCollection c = SparkCollection.get(chat.getCurrentAccount());
        ArrayList<String> folders = c.folders();
        if (folders.isEmpty()) {
            doAdd(chat, mo, "");
            return;
        }
        ArrayList<String> names = new ArrayList<>();
        names.add("Ohne Ordner");
        names.addAll(folders);
        AlertDialog.Builder b = new AlertDialog.Builder(chat.getParentActivity());
        b.setTitle(SparkUi.mark("In welchen Ordner?"));
        b.setItems(names.toArray(new CharSequence[0]), (d, which) -> doAdd(chat, mo, which == 0 ? "" : folders.get(which - 1)));
        chat.showDialog(b.create());
    }

    private static void doAdd(ChatActivity chat, MessageObject mo, String folder) {
        boolean ok = SparkCollection.get(chat.getCurrentAccount()).add(mo, folder);
        BulletinFactory.of(chat).createSimpleBulletin(R.raw.contact_check, ok ? "✦ In der Sammlung" + (TextUtils.isEmpty(folder) ? "" : " · " + folder) : "Ist schon in der Sammlung").show();
    }

    // ------------------------------------------------------------ Speichern

    private static String extension(MessageObject mo) {
        TLRPC.Document doc = mo.getDocument();
        String name = doc != null ? FileLoader.getDocumentFileName(doc) : null;
        if (!TextUtils.isEmpty(name) && name.contains(".")) return name.substring(name.lastIndexOf('.'));
        String mime = doc != null ? doc.mime_type : null;
        if (mime == null) return "";
        if (mime.contains("ogg")) return ".ogg";
        if (mime.contains("webp")) return ".webp";
        if (mime.contains("webm")) return ".webm";
        if (mime.contains("x-tgsticker")) return ".tgs";
        if (mime.contains("mp4")) return ".mp4";
        return "";
    }

    private static void saveToDownloads(BaseFragment fragment, MessageObject mo) {
        File f = FileLoader.getInstance(fragment.getCurrentAccount()).getPathToMessage(mo.messageOwner);
        if (f == null || !f.exists()) {
            f = FileLoader.getInstance(fragment.getCurrentAccount()).getPathToAttach(mo.getDocument(), true);
        }
        if (f == null || !f.exists()) {
            BulletinFactory.of(fragment).createErrorBulletin("Datei ist noch nicht geladen – erst einmal abspielen bzw. anzeigen lassen.").show();
            return;
        }
        String prefix = mo.isVoice() ? "Sprachnachricht_" : "Sparkgram_";
        String name = prefix + mo.getDialogId() + "_" + mo.getId() + extension(mo);
        String mime = mo.getDocument() != null ? mo.getDocument().mime_type : null;
        MediaController.saveFile(f.getAbsolutePath(), fragment.getParentActivity(), 2, name, mime, uri ->
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.ic_download, "✦ In „Downloads“ gespeichert").show());
    }

    // ------------------------------------------------------------ Kopie senden

    /** Waehlt einen Chat und sendet die Nachricht als Kopie ohne Absender dorthin. */
    public static void sendCopyTo(BaseFragment fragment, MessageObject mo) {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_FORWARD);
        DialogsActivity picker = new DialogsActivity(args);
        picker.setDelegate((f, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
            ArrayList<MessageObject> list = new ArrayList<>();
            list.add(mo);
            for (MessagesStorage.TopicKey key : dids) {
                org.telegram.messenger.SendMessagesHelper.getInstance(fragment.getCurrentAccount()).sendMessage(list, key.dialogId, true, false, true, 0, 0);
            }
            f.finishFragment();
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.forward, "✦ Ohne Absender gesendet").show();
            return true;
        });
        fragment.presentFragment(picker);
    }

    // ------------------------------------------------------------ Sticker-Packs

    private static void withSet(BaseFragment fragment, TLRPC.InputStickerSet input, Utilities.Callback<TLRPC.TL_messages_stickerSet> cb) {
        MediaDataController mdc = MediaDataController.getInstance(fragment.getCurrentAccount());
        TLRPC.TL_messages_stickerSet cached = mdc.getStickerSet(input, true);
        if (cached != null && cached.set != null) {
            cb.run(cached);
            return;
        }
        mdc.getStickerSet(input, null, false, set -> AndroidUtilities.runOnUIThread(() -> {
            if (set == null || set.set == null) {
                BulletinFactory.of(fragment).createErrorBulletin("Pack konnte nicht geladen werden.").show();
            } else {
                cb.run(set);
            }
        }));
    }

    private static void copyPackLink(BaseFragment fragment, TLRPC.InputStickerSet input) {
        withSet(fragment, input, set -> {
            AndroidUtilities.addToClipboard("https://t.me/addstickers/" + set.set.short_name);
            BulletinFactory.of(fragment).createCopyLinkBulletin().show();
        });
    }

    /** Laedt alle Sticker eines Packs und legt sie in „Downloads“ ab. */
    public static void downloadPack(BaseFragment fragment, TLRPC.InputStickerSet input) {
        withSet(fragment, input, set -> {
            final int account = fragment.getCurrentAccount();
            final ArrayList<TLRPC.Document> docs = new ArrayList<>(set.documents);
            final String shortName = set.set.short_name;
            final AlertDialog progress = new AlertDialog(fragment.getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
            progress.setCanCancel(false);
            progress.show();
            for (TLRPC.Document d : docs) {
                File f = FileLoader.getInstance(account).getPathToAttach(d, true);
                if (f == null || !f.exists()) {
                    FileLoader.getInstance(account).loadFile(d, set, FileLoader.PRIORITY_NORMAL, 1);
                }
            }
            Utilities.globalQueue.postRunnable(() -> {
                long deadline = System.currentTimeMillis() + 120_000;
                int saved = 0;
                for (int i = 0; i < docs.size(); i++) {
                    TLRPC.Document d = docs.get(i);
                    File f = FileLoader.getInstance(account).getPathToAttach(d, true);
                    while ((f == null || !f.exists()) && System.currentTimeMillis() < deadline) {
                        try { Thread.sleep(250); } catch (InterruptedException ignore) {}
                        f = FileLoader.getInstance(account).getPathToAttach(d, true);
                    }
                    if (f != null && f.exists()) {
                        String ext = d.mime_type != null && d.mime_type.contains("x-tgsticker") ? ".tgs" : d.mime_type != null && d.mime_type.contains("webm") ? ".webm" : ".webp";
                        MediaController.saveFile(f.getAbsolutePath(), ApplicationLoader.applicationContext, 2, shortName + "_" + (i + 1) + ext, d.mime_type, null, false);
                        saved++;
                    }
                }
                final int savedF = saved;
                AndroidUtilities.runOnUIThread(() -> {
                    try { progress.dismiss(); } catch (Throwable ignore) {}
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.ic_download, "✦ " + savedF + " von " + docs.size() + " Stickern in „Downloads“ gespeichert").show();
                });
            });
        });
    }

    /** Fremden Sticker in ein eigenes Pack uebernehmen. */
    public static void addToOwnPack(BaseFragment fragment, TLRPC.Document doc) {
        final int account = fragment.getCurrentAccount();
        TLRPC.TL_messages_getMyStickers req = new TLRPC.TL_messages_getMyStickers();
        req.limit = 100;
        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            if (!(res instanceof TLRPC.TL_messages_myStickers) || ((TLRPC.TL_messages_myStickers) res).sets.isEmpty()) {
                BulletinFactory.of(fragment).createErrorBulletin("Du hast noch kein eigenes Sticker-Pack. Leg eins an (Sticker-Ansicht → „Neues Pack“), dann geht's.").show();
                return;
            }
            ArrayList<TLRPC.StickerSetCovered> sets = ((TLRPC.TL_messages_myStickers) res).sets;
            String[] names = new String[sets.size()];
            for (int i = 0; i < sets.size(); i++) names[i] = sets.get(i).set.title;
            AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity());
            b.setTitle(SparkUi.mark("In welches Pack?"));
            b.setItems(names, (d, which) -> {
                String emoji = null;
                for (TLRPC.DocumentAttribute a : doc.attributes) {
                    if (a instanceof TLRPC.TL_documentAttributeSticker && !TextUtils.isEmpty(a.alt)) emoji = a.alt;
                }
                if (TextUtils.isEmpty(emoji)) emoji = "😀";
                TLRPC.TL_stickers_addStickerToSet add = new TLRPC.TL_stickers_addStickerToSet();
                add.stickerset = MediaDataController.getInputStickerSet(sets.get(which).set);
                add.sticker = MediaDataController.getInputStickerSetItem(doc, emoji);
                ConnectionsManager.getInstance(account).sendRequest(add, (r2, e2) -> AndroidUtilities.runOnUIThread(() -> {
                    if (r2 instanceof TLRPC.TL_messages_stickerSet) {
                        MediaDataController.getInstance(account).putStickerSet((TLRPC.TL_messages_stickerSet) r2);
                        BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, "✦ Zu „" + names[which] + "“ hinzugefügt").show();
                    } else {
                        BulletinFactory.of(fragment).createErrorBulletin("Hat nicht geklappt: " + (e2 != null ? e2.text : "unbekannt")).show();
                    }
                }));
            });
            fragment.showDialog(b.create());
        }));
    }

    // ------------------------------------------------------------ Bot

    /** Text aus dem Eingabefeld (oder neu eingetippt) ueber den eigenen Bot senden. */
    public static void sendViaBot(ChatActivity chat, long dialogId) {
        if (chat.getParentActivity() == null) return;
        if (TextUtils.isEmpty(SparkBot.token())) {
            BulletinFactory.of(chat).createErrorBulletin("Erst den Bot-Token eintragen: Sparkgram-Einstellungen → Bot.").show();
            return;
        }
        org.telegram.ui.Components.ChatActivityEnterView input = chat.getChatActivityEnterView();
        CharSequence typed = input != null ? input.getFieldText() : null;
        final EditText edit = new EditText(chat.getParentActivity());
        edit.setText(typed == null ? "" : typed.toString());
        edit.setHint("Nachricht");
        edit.setMinLines(2);
        edit.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        edit.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        FrameLayout box = new FrameLayout(chat.getParentActivity());
        box.addView(edit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 24, 0, 24, 0));
        final MessageObject reply = input != null ? input.getReplyingMessageObject() : null;
        final boolean canReply = reply != null && dialogId < 0 && org.telegram.messenger.ChatObject.isChannel(chat.getMessagesController().getChat(-dialogId));
        AlertDialog.Builder b = new AlertDialog.Builder(chat.getParentActivity());
        b.setTitle(SparkUi.mark("Als " + (SparkBot.botName() != null ? SparkBot.botName() : "Bot") + " senden"));
        if (canReply) {
            b.setMessage("Als Antwort auf die gewählte Nachricht.");
        }
        b.setView(box);
        b.setPositiveButton(org.telegram.messenger.LocaleController.getString(R.string.Send), (d, w) -> {
            final String text = edit.getText().toString().trim();
            if (text.isEmpty()) return;
            final int account = chat.getCurrentAccount();
            Utilities.globalQueue.postRunnable(() -> {
                SparkBot.Result r = SparkBot.send(account, dialogId, text, canReply ? reply.getId() : 0);
                AndroidUtilities.runOnUIThread(() -> {
                    if (r.ok) {
                        if (input != null && typed != null && text.equals(typed.toString().trim())) {
                            input.setFieldText("");
                        }
                        BulletinFactory.of(chat).createSimpleBulletin(R.raw.contact_check, "✦ Über den Bot gesendet").show();
                    } else {
                        BulletinFactory.of(chat).createErrorBulletin(r.error == null ? "Senden fehlgeschlagen" : r.error).show();
                    }
                });
            });
        });
        b.setNegativeButton(org.telegram.messenger.LocaleController.getString(R.string.Cancel), null);
        chat.showDialog(b.create());
    }

    /** Dialog zum Eintragen/Pruefen des Bot-Tokens (Einstellungen). */
    public static void editBotToken(BaseFragment fragment, Runnable onChanged) {
        final EditText edit = new EditText(fragment.getParentActivity());
        edit.setHint("123456789:ABC…");
        edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edit.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        edit.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        FrameLayout box = new FrameLayout(fragment.getParentActivity());
        box.addView(edit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 24, 0, 24, 0));
        AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity());
        b.setTitle(SparkUi.mark("Bot-Token"));
        b.setMessage("Den Token bekommst du von @BotFather (/newbot bzw. /token). Er wird nur auf diesem Gerät gespeichert. Der Bot muss in der Gruppe sein bzw. von der Person gestartet worden sein.");
        b.setView(box);
        b.setPositiveButton(org.telegram.messenger.LocaleController.getString(R.string.Save), (d, w) -> {
            final String token = edit.getText().toString().trim();
            if (token.isEmpty()) return;
            final AlertDialog progress = new AlertDialog(fragment.getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
            progress.show();
            Utilities.globalQueue.postRunnable(() -> {
                SparkBot.Result r = SparkBot.check(token);
                AndroidUtilities.runOnUIThread(() -> {
                    try { progress.dismiss(); } catch (Throwable ignore) {}
                    if (r.ok) {
                        SparkBot.setToken(token, r.botName);
                        BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, "✦ Verbunden mit " + r.botName).show();
                        if (onChanged != null) onChanged.run();
                    } else {
                        BulletinFactory.of(fragment).createErrorBulletin("Token ungültig: " + r.error).show();
                    }
                });
            });
        });
        b.setNeutralButton("Entfernen", (d, w) -> {
            SparkBot.setToken(null, null);
            if (onChanged != null) onChanged.run();
        });
        b.setNegativeButton(org.telegram.messenger.LocaleController.getString(R.string.Cancel), null);
        fragment.showDialog(b.create());
    }
}

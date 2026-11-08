package org.telegram.ui.spark;

import android.os.Bundle;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;
import org.telegram.ui.ProfileActivity;

import java.util.ArrayList;

/** Admin: Mitglieder durchsuchen nach geloeschten Konten, Fake/Scam-Markierung und Konten ohne Bild & Benutzernamen. */
public class SparkSuspiciousActivity extends UniversalFragment {

    private static final int ID_REMOVE_DELETED = 1;
    private static final int ID_BASE = 1000;
    private static final int MAX_MEMBERS = 10000;

    private final long chatId;
    private boolean loading = true;
    private int scanned;
    private int total;
    private int offset;
    private boolean hidden;
    private final ArrayList<TLRPC.User> deleted = new ArrayList<>();
    private final ArrayList<TLRPC.User> flagged = new ArrayList<>();   // fake / scam
    private final ArrayList<TLRPC.User> faceless = new ArrayList<>();  // ohne Bild und Benutzername
    private final ArrayList<TLRPC.User> shown = new ArrayList<>();

    public SparkSuspiciousActivity(long chatId) {
        this.chatId = chatId;
    }

    @Override
    protected CharSequence getTitle() {
        return "Verdächtige Konten";
    }

    @Override
    public boolean onFragmentCreate() {
        load();
        return super.onFragmentCreate();
    }

    private void load() {
        TLRPC.Chat chat = getMessagesController().getChat(chatId);
        if (chat == null) {
            loading = false;
            return;
        }
        TLRPC.TL_channels_getParticipants req = new TLRPC.TL_channels_getParticipants();
        req.channel = MessagesController.getInputChannel(chat);
        req.filter = new TLRPC.TL_channelParticipantsRecent();
        req.offset = offset;
        req.limit = 200;
        getConnectionsManager().sendRequest(req, (response, err) -> AndroidUtilities.runOnUIThread(() -> {
            if (response instanceof TLRPC.channels_ChannelParticipants) {
                TLRPC.channels_ChannelParticipants res = (TLRPC.channels_ChannelParticipants) response;
                getMessagesController().putUsers(res.users, false);
                total = res.count;
                for (TLRPC.ChannelParticipant p : res.participants) {
                    long uid = p.peer != null ? DialogObject.getPeerDialogId(p.peer) : 0;
                    if (uid <= 0) continue;
                    TLRPC.User u = getMessagesController().getUser(uid);
                    if (u == null || u.bot || UserObject.isUserSelf(u)) continue;
                    scanned++;
                    if (u.deleted) deleted.add(u);
                    else if (u.fake || u.scam) flagged.add(u);
                    else if (u.photo == null && UserObject.getPublicUsername(u) == null) faceless.add(u);
                }
                offset += res.participants.size();
                if (!res.participants.isEmpty() && offset < Math.min(MAX_MEMBERS, res.count)) {
                    if (listView != null) listView.adapter.update(true);
                    AndroidUtilities.runOnUIThread(this::load, 700);
                    return;
                }
                if (offset == 0) hidden = true;
            } else {
                hidden = true;
            }
            loading = false;
            if (listView != null) listView.adapter.update(true);
        }));
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        shown.clear();
        if (loading) {
            items.add(UItem.asShadow("Prüfe Mitglieder … " + scanned + (total > 0 ? " von " + total : "")));
        } else if (hidden) {
            items.add(UItem.asShadow("Die Mitgliederliste konnte nicht geladen werden (verborgen oder keine Rechte)."));
            return;
        } else {
            items.add(UItem.asShadow(scanned + " Mitglieder geprüft" + (total > MAX_MEMBERS ? " (Telegram liefert höchstens " + MAX_MEMBERS + ")" : "") + "."));
        }
        if (!deleted.isEmpty() && !loading) {
            items.add(UItem.asButton(ID_REMOVE_DELETED, R.drawable.msg_delete, "Gelöschte Konten entfernen (" + deleted.size() + ")").red());
        }
        addSection(items, "Gelöschte Konten", deleted);
        addSection(items, "Als Fake/Scam markiert", flagged);
        addSection(items, "Ohne Profilbild und Benutzername", faceless);
        if (!loading && deleted.isEmpty() && flagged.isEmpty() && faceless.isEmpty()) {
            items.add(UItem.asShadow("Nichts Auffälliges gefunden. 🎉"));
        }
    }

    private void addSection(ArrayList<UItem> items, String title, ArrayList<TLRPC.User> users) {
        if (users.isEmpty()) return;
        items.add(UItem.asHeader(title + " (" + users.size() + ")"));
        for (TLRPC.User u : users) {
            items.add(UItem.asButton(ID_BASE + shown.size(), UserObject.getUserName(u), "ID " + u.id));
            shown.add(u);
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_REMOVE_DELETED) {
            AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
            b.setTitle(SparkUi.mark("Gelöschte Konten entfernen"));
            b.setMessage(deleted.size() + " gelöschte Konten aus der Gruppe entfernen?");
            b.setPositiveButton("Entfernen", (d, w) -> removeDeleted());
            b.setNegativeButton("Abbrechen", null);
            showDialog(b.create());
            return;
        }
        int i = item.id - ID_BASE;
        if (i < 0 || i >= shown.size()) return;
        Bundle args = new Bundle();
        args.putLong("user_id", shown.get(i).id);
        presentFragment(new ProfileActivity(args));
    }

    private void removeDeleted() {
        final ArrayList<TLRPC.User> list = new ArrayList<>(deleted);
        final int now = (int) (System.currentTimeMillis() / 1000);
        for (int i = 0; i < list.size(); i++) {
            final TLRPC.User u = list.get(i);
            // nacheinander mit kleiner Pause, damit Telegram nicht bremst
            AndroidUtilities.runOnUIThread(() -> {
                TLRPC.TL_chatBannedRights r = new TLRPC.TL_chatBannedRights();
                r.view_messages = true;
                r.until_date = now + 60;
                getMessagesController().setParticipantBannedRole(chatId, u, null, r, true, this);
            }, i * 400L);
        }
        deleted.clear();
        listView.adapter.update(true);
        BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "✦ " + list.size() + " gelöschte Konten werden entfernt").show();
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}

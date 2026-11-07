package org.telegram.ui.spark;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.spark.SparkLocalNames;
import org.telegram.messenger.spark.SparkNameHistory;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;
import org.telegram.ui.ProfileActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Suche nach Name, @Benutzername oder ID – in allem, was Sparkgram kennt
 * (Kontakte, Chats, Gruppenmitglieder, lokale Namen, fruehere Namen) und auf Wunsch bei Telegram.
 */
public class SparkSearchActivity extends UniversalFragment {

    private static final int ID_ONLINE_LOOKUP = 1;
    private static final int MAX_RESULTS = 100;

    private String query = "";
    private final LinkedHashMap<Long, String> results = new LinkedHashMap<>(); // dialogId -> Hinweis
    private String remoteState;
    private boolean searched;
    private org.telegram.ui.Components.EditTextBoldCursor searchField;
    private final Runnable searchRunnable = this::runLocalSearch;

    @Override
    protected CharSequence getTitle() {
        return "Suche nach Name / ID";
    }

    @Override
    public View createView(Context context) {
        View v = super.createView(context);
        // Suchfeld oben fest angeheftet
        android.widget.FrameLayout box = new android.widget.FrameLayout(context);
        box.setBackgroundColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhite));
        box.setElevation(AndroidUtilities.dp(2));
        searchField = new org.telegram.ui.Components.EditTextBoldCursor(context);
        searchField.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 17);
        searchField.setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhiteBlackText));
        searchField.setHintTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhiteHintText));
        searchField.setCursorColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhiteBlackText));
        searchField.setHint("Name, @Benutzername oder ID");
        searchField.setBackground(null);
        searchField.setSingleLine(true);
        searchField.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        searchField.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        searchField.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void afterTextChanged(android.text.Editable e) {
                query = e.toString().trim();
                remoteState = null;
                AndroidUtilities.cancelRunOnUIThread(searchRunnable);
                AndroidUtilities.runOnUIThread(searchRunnable, 250);
            }
        });
        searchField.setOnEditorActionListener((tv, actionId, ev) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                query = searchField.getText().toString().trim();
                runLocalSearch();
                lookupOnline();
                return true;
            }
            return false;
        });
        box.addView(searchField, org.telegram.ui.Components.LayoutHelper.createFrame(org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, android.view.Gravity.CENTER_VERTICAL, 20, 0, 20, 0));
        ((android.widget.FrameLayout) v).addView(box, org.telegram.ui.Components.LayoutHelper.createFrame(org.telegram.ui.Components.LayoutHelper.MATCH_PARENT, 52, android.view.Gravity.TOP));
        listView.setPadding(0, AndroidUtilities.dp(52), 0, 0);
        listView.setClipToPadding(false);
        return v;
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        super.onTransitionAnimationEnd(isOpen, backward);
        if (isOpen && searchField != null) {
            searchField.requestFocus();
            AndroidUtilities.showKeyboard(searchField);
        }
    }

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim();
    }

    private static Long parseId(String q) {
        String s = q.trim();
        if (s.startsWith("-100")) s = s.substring(4);
        else if (s.startsWith("-")) s = s.substring(1);
        if (s.isEmpty() || s.length() > 19) return null;
        for (int i = 0; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return null;
        try { return Long.parseLong(s); } catch (Exception e) { return null; }
    }

    private void add(long dialogId, String hint) {
        if (results.size() >= MAX_RESULTS || results.containsKey(dialogId)) return;
        results.put(dialogId, hint);
    }

    private void runLocalSearch() {
        results.clear();
        searched = !TextUtils.isEmpty(query);
        if (!searched) {
            if (listView != null) listView.adapter.update(true);
            return;
        }
        MessagesController mc = getMessagesController();
        Long id = parseId(query);
        if (id != null) {
            if (mc.getUser(id) != null) add(id, "ID-Treffer");
            if (mc.getChat(id) != null) add(-id, "ID-Treffer");
        }
        String q = norm(query.startsWith("@") ? query.substring(1) : query);
        if (q.length() >= 2 || id != null) {
            for (Map.Entry<Long, TLRPC.User> e : mc.getUsers().entrySet()) {
                TLRPC.User u = e.getValue();
                if (u == null || u.id <= 0) continue;
                String name = norm(UserObject.getUserName(u));
                String real = norm(org.telegram.messenger.ContactsController.formatName(u.first_name, u.last_name));
                String un = norm(UserObject.getPublicUsername(u));
                String local = norm(SparkLocalNames.get(u.id));
                if (name.contains(q) || real.contains(q) || un.contains(q)) {
                    add(u.id, null);
                } else if (!local.isEmpty() && local.contains(q)) {
                    add(u.id, "lokaler Name");
                }
            }
            for (Map.Entry<Long, TLRPC.Chat> e : mc.getChats().entrySet()) {
                TLRPC.Chat c = e.getValue();
                if (c == null || c.id <= 0) continue;
                if (norm(c.title).contains(q) || norm(c.username).contains(q)) add(-c.id, null);
            }
            // fruehere Namen / Benutzernamen
            HashMap<Long, String> old = SparkNameHistory.get(currentAccount).search(q, MAX_RESULTS);
            for (Map.Entry<Long, String> e : old.entrySet()) {
                if (mc.getUser(e.getKey()) != null) add(e.getKey(), "früher: " + e.getValue());
            }
        }
        if (listView != null) listView.adapter.update(true);
    }

    /** Eine einzelne Anfrage bei Telegram: @Benutzername aufloesen bzw. ID nachschlagen. */
    private void lookupOnline() {
        if (TextUtils.isEmpty(query)) return;
        Long id = parseId(query);
        remoteState = "Frage Telegram …";
        if (listView != null) listView.adapter.update(true);
        if (id != null) {
            TLRPC.TL_users_getUsers req = new TLRPC.TL_users_getUsers();
            TLRPC.TL_inputUser in = new TLRPC.TL_inputUser();
            in.user_id = id;
            in.access_hash = 0;
            req.id.add(in);
            getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                boolean found = false;
                if (res instanceof org.telegram.tgnet.Vector) {
                    for (Object o : ((org.telegram.tgnet.Vector<?>) res).objects) {
                        if (o instanceof TLRPC.User && !(o instanceof TLRPC.TL_userEmpty)) {
                            getMessagesController().putUser((TLRPC.User) o, false);
                            found = true;
                        }
                    }
                }
                remoteState = found ? null : "Telegram kennt diese ID für dich nicht. Eine ID lässt sich nur öffnen, wenn du die Person schon irgendwo gesehen hast (gemeinsame Gruppe, Weiterleitung …).";
                runLocalSearch();
            }));
        } else {
            String un = query.startsWith("@") ? query.substring(1) : query;
            if (un.contains(" ") || un.length() < 4) {
                remoteState = "Bei Telegram lässt sich nur ein @Benutzername oder eine ID nachschlagen.";
                if (listView != null) listView.adapter.update(true);
                return;
            }
            TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
            req.username = un;
            getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                if (res instanceof TLRPC.TL_contacts_resolvedPeer) {
                    TLRPC.TL_contacts_resolvedPeer r = (TLRPC.TL_contacts_resolvedPeer) res;
                    getMessagesController().putUsers(r.users, false);
                    getMessagesController().putChats(r.chats, false);
                    long did = org.telegram.messenger.DialogObject.getPeerDialogId(r.peer);
                    remoteState = null;
                    runLocalSearch();
                    if (!results.containsKey(did)) {
                        LinkedHashMap<Long, String> copy = new LinkedHashMap<>();
                        copy.put(did, "@" + un);
                        copy.putAll(results);
                        results.clear();
                        results.putAll(copy);
                        listView.adapter.update(true);
                    }
                } else {
                    remoteState = err != null && err.text != null && err.text.startsWith("FLOOD_WAIT") ? "Telegram bremst gerade – bitte später nochmal." : "@" + un + " wurde nicht gefunden.";
                    listView.adapter.update(true);
                }
            }), ConnectionsManager.RequestFlagFailOnServerErrors);
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (!searched) {
            items.add(UItem.asShadow("Sucht in allen Personen und Chats, die Sparkgram kennt – auch nach lokalen und früheren Namen. Mit der Such-Taste der Tastatur wird zusätzlich bei Telegram nach @Benutzername bzw. ID gefragt."));
            return;
        }
        if (remoteState != null) {
            items.add(UItem.asShadow(remoteState));
        }
        if (results.isEmpty()) {
            items.add(UItem.asShadow("Nichts gefunden."));
        } else {
            items.add(UItem.asHeader(results.size() + (results.size() >= MAX_RESULTS ? "+" : "") + " Treffer"));
            MessagesController mc = getMessagesController();
            for (Map.Entry<Long, String> e : results.entrySet()) {
                TLObject o = e.getKey() > 0 ? mc.getUser(e.getKey()) : mc.getChat(-e.getKey());
                if (o == null) continue;
                UItem it = UItem.asProfileCell(o);
                it.id = 1000;
                items.add(it);
                long rawId = e.getKey() > 0 ? e.getKey() : -e.getKey();
                items.add(UItem.asShadow("ID " + rawId + (e.getValue() != null ? " · " + e.getValue() : "")));
            }
        }
        items.add(UItem.asButton(ID_ONLINE_LOOKUP, R.drawable.msg_search, "Bei Telegram nachschlagen"));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_ONLINE_LOOKUP) {
            lookupOnline();
            return;
        }
        Bundle args = new Bundle();
        if (item.object instanceof TLRPC.User) {
            args.putLong("user_id", ((TLRPC.User) item.object).id);
        } else if (item.object instanceof TLRPC.Chat) {
            args.putLong("chat_id", ((TLRPC.Chat) item.object).id);
        } else {
            return;
        }
        presentFragment(new ProfileActivity(args));
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        AndroidUtilities.cancelRunOnUIThread(searchRunnable);
    }
}

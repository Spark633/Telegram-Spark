package org.telegram.ui.spark;

import static org.telegram.messenger.LocaleController.getString;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.spark.SparkNotes;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** Uebersicht aller Notizen; Tippen bearbeitet, langes Druecken oeffnet den Chat. */
public class SparkNotesActivity extends UniversalFragment {

    private static final int ID_BASE = 1000;
    private ArrayList<Long> ids = new ArrayList<>();

    @Override
    public boolean onFragmentCreate() {
        ids = SparkNotes.all(currentAccount);
        return super.onFragmentCreate();
    }

    @Override
    public void onResume() {
        super.onResume();
        ids = SparkNotes.all(currentAccount);
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.SparkNotes);
    }

    public static String dialogName(int account, long dialogId) {
        MessagesController mc = MessagesController.getInstance(account);
        if (dialogId > 0) {
            TLRPC.User user = mc.getUser(dialogId);
            return user != null ? UserObject.getUserName(user) : String.valueOf(dialogId);
        }
        TLRPC.Chat chat = mc.getChat(-dialogId);
        return chat != null ? chat.title : String.valueOf(dialogId);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (ids.isEmpty()) {
            items.add(UItem.asShadow(getString(R.string.SparkNotesEmpty)));
            return;
        }
        items.add(UItem.asHeader(getString(R.string.SparkNotes)));
        for (int i = 0; i < ids.size(); i++) {
            long did = ids.get(i);
            String note = SparkNotes.get(currentAccount, did);
            String preview = note == null ? "" : note.replace('\n', ' ');
            if (preview.length() > 40) {
                preview = preview.substring(0, 40) + "…";
            }
            items.add(UItem.asButton(ID_BASE + i, dialogName(currentAccount, did), preview));
        }
        items.add(UItem.asShadow(getString(R.string.SparkNotesHint)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        int index = item.id - ID_BASE;
        if (index >= 0 && index < ids.size()) {
            edit(this, ids.get(index), () -> {
                ids = SparkNotes.all(currentAccount);
                listView.adapter.update(true);
            });
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        int index = item.id - ID_BASE;
        if (index >= 0 && index < ids.size()) {
            long did = ids.get(index);
            Bundle args = new Bundle();
            if (did > 0) {
                args.putLong("user_id", did);
            } else {
                args.putLong("chat_id", -did);
            }
            presentFragment(new ChatActivity(args));
            return true;
        }
        return false;
    }

    /** Editor-Dialog fuer die Notiz eines Dialogs. */
    public static void edit(BaseFragment fragment, long dialogId, Runnable onSaved) {
        final int account = fragment.getCurrentAccount();
        final EditText input = new EditText(fragment.getParentActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setMinLines(3);
        input.setMaxLines(10);
        input.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        input.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        input.setHint(getString(R.string.SparkNoteHint));
        String existing = SparkNotes.get(account, dialogId);
        if (existing != null) {
            input.setText(existing);
            input.setSelection(existing.length());
        }
        FrameLayout container = new FrameLayout(fragment.getParentActivity());
        container.addView(input, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 24, 0, 24, 0));
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(getString(R.string.SparkNote) + " · " + dialogName(account, dialogId));
        builder.setView(container);
        builder.setPositiveButton(getString(R.string.Save), (d, w) -> {
            SparkNotes.set(account, dialogId, input.getText().toString());
            if (onSaved != null) {
                onSaved.run();
            }
        });
        if (!TextUtils.isEmpty(existing)) {
            builder.setNeutralButton(getString(R.string.Delete), (d, w) -> {
                SparkNotes.set(account, dialogId, null);
                if (onSaved != null) {
                    onSaved.run();
                }
            });
        }
        builder.setNegativeButton(getString(R.string.Cancel), null);
        fragment.showDialog(builder.create());
        AndroidUtilities.runOnUIThread(() -> {
            input.requestFocus();
            AndroidUtilities.showKeyboard(input);
        }, 200);
    }
}

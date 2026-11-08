package org.telegram.ui.spark;

import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.telegram.messenger.R;
import org.telegram.messenger.spark.SparkAdmin;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * Admin-Vorlagen: ohne templateId die Liste aller Vorlagen, mit templateId der Editor einer Vorlage.
 */
public class SparkAdminActivity extends UniversalFragment {

    private static final int ID_ADD = 1;
    private static final int ID_NAME = 2;
    private static final int ID_DELETE_MSG = 3;
    private static final int ID_CONFIRM = 4;
    private static final int ID_ADD_STAGE = 5;
    private static final int ID_REMOVE = 6;
    private static final int ID_TEMPLATE_BASE = 1000;
    private static final int ID_STAGE_BASE = 2000;

    private final long templateId;
    private SparkAdmin.Template template;
    private ArrayList<SparkAdmin.Template> list = new ArrayList<>();

    /** Liste aller Vorlagen. */
    public SparkAdminActivity() {
        this(0);
    }

    /** Editor einer Vorlage. */
    public SparkAdminActivity(long templateId) {
        this.templateId = templateId;
    }

    private boolean isEditor() {
        return templateId != 0;
    }

    @Override
    protected CharSequence getTitle() {
        if (isEditor()) {
            SparkAdmin.Template t = SparkAdmin.find(templateId);
            return t != null ? t.name : "Vorlage";
        }
        return "Admin-Vorlagen";
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null) listView.adapter.update(true);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (!isEditor()) {
            list = SparkAdmin.templates();
            items.add(UItem.asShadow("In Gruppen, in denen du Admin bist: Nachricht antippen → „✦ Admin“ → Vorlage. Je nachdem, wie oft du die Vorlage bei dieser Person schon benutzt hast, greift die nächste Stufe. Der Zähler wird nur auf deinem Gerät gespeichert."));
            items.add(UItem.asHeader("Vorlagen"));
            for (int i = 0; i < list.size(); i++) {
                SparkAdmin.Template t = list.get(i);
                items.add(UItem.asButton(ID_TEMPLATE_BASE + i, R.drawable.msg_admins, t.name, t.stages.size() + (t.stages.size() == 1 ? " Stufe" : " Stufen")));
            }
            items.add(UItem.asButton(ID_ADD, R.drawable.msg_add, "Vorlage hinzufügen").accent());
            return;
        }
        template = SparkAdmin.find(templateId);
        if (template == null) {
            items.add(UItem.asShadow("Vorlage nicht gefunden."));
            return;
        }
        items.add(UItem.asButton(ID_NAME, R.drawable.msg_edit, "Name", template.name));
        items.add(UItem.asCheck(ID_DELETE_MSG, "Nachricht löschen").setChecked(template.deleteMessage));
        items.add(UItem.asCheck(ID_CONFIRM, "Vor dem Ausführen fragen").setChecked(template.confirm));
        items.add(UItem.asHeader("Stufen"));
        for (int i = 0; i < template.stages.size(); i++) {
            SparkAdmin.Stage s = template.stages.get(i);
            String last = i == template.stages.size() - 1 && template.stages.size() > 1 ? " (und jedes weitere Mal)" : "";
            items.add(UItem.asButton(ID_STAGE_BASE + i, (i + 1) + ". Mal" + last, SparkAdmin.describe(s)));
            items.add(UItem.asShadow(TextUtils.isEmpty(s.text) ? "Kein Text in die Gruppe" : "Text: " + s.text));
        }
        items.add(UItem.asButton(ID_ADD_STAGE, R.drawable.msg_add, "Stufe hinzufügen").accent());
        items.add(UItem.asShadow("Antippen = Stufe bearbeiten, lange drücken = Stufe löschen. Im Text wird {user} durch die Person ersetzt (als Erwähnung)."));
        items.add(UItem.asButton(ID_REMOVE, R.drawable.msg_delete, "Vorlage löschen").red());
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (!isEditor()) {
            if (item.id == ID_ADD) {
                SparkAdmin.Template t = SparkAdmin.newTemplate();
                SparkAdmin.put(t);
                presentFragment(new SparkAdminActivity(t.id));
                return;
            }
            int i = item.id - ID_TEMPLATE_BASE;
            if (i >= 0 && i < list.size()) presentFragment(new SparkAdminActivity(list.get(i).id));
            return;
        }
        if (template == null) return;
        if (item.id == ID_NAME) {
            editText(this, "Name der Vorlage", template.name, "z. B. RL-Bilder", false, text -> {
                if (TextUtils.isEmpty(text)) return;
                template.name = text;
                saveAndUpdate();
            });
        } else if (item.id == ID_DELETE_MSG) {
            template.deleteMessage = !template.deleteMessage;
            saveAndUpdate();
        } else if (item.id == ID_CONFIRM) {
            template.confirm = !template.confirm;
            saveAndUpdate();
        } else if (item.id == ID_ADD_STAGE) {
            SparkAdmin.Stage s = new SparkAdmin.Stage();
            if (!template.stages.isEmpty()) {
                SparkAdmin.Stage prev = template.stages.get(template.stages.size() - 1);
                s.action = prev.action;
                s.duration = prev.duration;
                s.text = prev.text;
            } else {
                s.action = SparkAdmin.ACT_MUTE;
                s.duration = 86400;
            }
            template.stages.add(s);
            saveAndUpdate();
            editStage(template.stages.size() - 1);
        } else if (item.id == ID_REMOVE) {
            AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
            b.setTitle(SparkUi.mark("Vorlage löschen"));
            b.setMessage("„" + template.name + "“ wirklich löschen?");
            b.setPositiveButton("Löschen", (d, w) -> {
                SparkAdmin.remove(template.id);
                finishFragment();
            });
            b.setNegativeButton("Abbrechen", null);
            showDialog(b.create());
        } else {
            int i = item.id - ID_STAGE_BASE;
            if (i >= 0 && i < template.stages.size()) editStage(i);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (!isEditor() || template == null) return false;
        int i = item.id - ID_STAGE_BASE;
        if (i < 0 || i >= template.stages.size()) return false;
        if (template.stages.size() <= 1) return true; // mindestens eine Stufe
        template.stages.remove(i);
        saveAndUpdate();
        return true;
    }

    private void saveAndUpdate() {
        SparkAdmin.put(template);
        if (actionBar != null) actionBar.setTitle(template.name);
        listView.adapter.update(true);
    }

    /** Stufe bearbeiten: Aktion -> Dauer -> Text. */
    private void editStage(int index) {
        SparkAdmin.Stage s = template.stages.get(index);
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(SparkUi.mark((index + 1) + ". Mal: Aktion"));
        b.setItems(SparkAdmin.ACTION_NAMES, (d, action) -> {
            s.action = action;
            if (action == SparkAdmin.ACT_NONE || action == SparkAdmin.ACT_KICK) {
                s.duration = 0;
                saveAndUpdate();
                editStageText(index);
                return;
            }
            AlertDialog.Builder c = new AlertDialog.Builder(getParentActivity());
            c.setTitle(SparkUi.mark((index + 1) + ". Mal: Dauer"));
            c.setItems(SparkAdmin.DURATION_NAMES, (d2, w) -> {
                s.duration = SparkAdmin.DURATIONS[w];
                saveAndUpdate();
                editStageText(index);
            });
            showDialog(c.create());
        });
        showDialog(b.create());
    }

    private void editStageText(int index) {
        SparkAdmin.Stage s = template.stages.get(index);
        editText(this, (index + 1) + ". Mal: Text in die Gruppe", s.text, "{user} restricted for … (leer = kein Text)", true, text -> {
            s.text = text == null ? "" : text;
            saveAndUpdate();
        });
    }

    // ------------------------------------------------------------------ Im Chat: Nachricht -> "✦ Admin"

    /** Darf "✦ Admin" fuer diese Nachricht erscheinen? */
    public static boolean canShow(int account, org.telegram.messenger.MessageObject mo) {
        if (!org.telegram.messenger.spark.SparkConfig.get(org.telegram.messenger.spark.SparkConfig.ADMIN_TOOLS) || mo == null || mo.getDialogId() >= 0) return false;
        long from = mo.getFromChatId();
        if (from <= 0 || mo.isOutOwner() || from == org.telegram.messenger.UserConfig.getInstance(account).getClientUserId()) return false;
        org.telegram.tgnet.TLRPC.Chat chat = org.telegram.messenger.MessagesController.getInstance(account).getChat(-mo.getDialogId());
        if (chat == null || org.telegram.messenger.ChatObject.isChannel(chat) && !chat.megagroup) return false;
        return org.telegram.messenger.ChatObject.canBlockUsers(chat) || org.telegram.messenger.ChatObject.canUserDoAdminAction(chat, org.telegram.messenger.ChatObject.ACTION_DELETE_MESSAGES);
    }

    public static void showForMessage(BaseFragment fragment, org.telegram.messenger.MessageObject mo) {
        final int account = fragment.getCurrentAccount();
        final long chatId = -mo.getDialogId();
        final long userId = mo.getFromChatId();
        final ArrayList<SparkAdmin.Template> templates = SparkAdmin.templates();
        org.telegram.tgnet.TLRPC.User user = org.telegram.messenger.MessagesController.getInstance(account).getUser(userId);
        final String name = user != null ? org.telegram.messenger.UserObject.getUserName(user) : "Person";
        ArrayList<CharSequence> texts = new ArrayList<>();
        ArrayList<Integer> icons = new ArrayList<>();
        for (SparkAdmin.Template t : templates) {
            int st = SparkAdmin.nextStage(account, chatId, userId, t);
            int done = SparkAdmin.count(account, chatId, userId, t.id);
            texts.add(t.name + (st >= 0 ? "  ·  Stufe " + Math.min(done + 1, t.stages.size()) + "/" + t.stages.size() : ""));
            icons.add(R.drawable.msg_admins);
        }
        texts.add("Zähler von " + name);
        icons.add(R.drawable.msg_stats);
        texts.add("Vorlagen verwalten");
        icons.add(R.drawable.msg_settings);
        int[] ic = new int[icons.size()];
        for (int i = 0; i < ic.length; i++) ic[i] = icons.get(i);
        AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity());
        b.setTitle(SparkUi.mark("Admin · " + name));
        b.setItems(texts.toArray(new CharSequence[0]), ic, (d, w) -> {
            if (w < templates.size()) {
                run(fragment, mo, templates.get(w), name);
            } else if (w == templates.size()) {
                showCounters(fragment, chatId, userId, name);
            } else {
                fragment.presentFragment(new SparkAdminActivity());
            }
        });
        fragment.showDialog(b.create());
    }

    private static void run(BaseFragment fragment, org.telegram.messenger.MessageObject mo, SparkAdmin.Template t, String name) {
        final int account = fragment.getCurrentAccount();
        int st = SparkAdmin.nextStage(account, -mo.getDialogId(), mo.getFromChatId(), t);
        if (st < 0) return;
        SparkAdmin.Stage s = t.stages.get(st);
        Runnable go = () -> SparkAdmin.execute(fragment, mo, t, error -> {
            if (error != null) {
                org.telegram.ui.Components.BulletinFactory.of(fragment).createErrorBulletin(error).show();
            } else {
                org.telegram.ui.Components.BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, "✦ " + t.name + ": Stufe " + (st + 1) + " bei " + name + " ausgeführt").show();
            }
        });
        if (!t.confirm) {
            go.run();
            return;
        }
        StringBuilder msg = new StringBuilder();
        msg.append("Stufe ").append(st + 1).append(" von ").append(t.stages.size()).append(" bei ").append(name).append(":\n\n");
        if (t.deleteMessage) msg.append("• Nachricht löschen\n");
        msg.append("• ").append(SparkAdmin.describe(s)).append("\n");
        if (!TextUtils.isEmpty(s.text)) msg.append("• Text: ").append(s.text);
        AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity());
        b.setTitle(SparkUi.mark(t.name));
        b.setMessage(msg.toString().trim());
        b.setPositiveButton("Ausführen", (d, w) -> go.run());
        b.setNegativeButton("Abbrechen", null);
        fragment.showDialog(b.create());
    }

    private static void showCounters(BaseFragment fragment, long chatId, long userId, String name) {
        final int account = fragment.getCurrentAccount();
        final ArrayList<SparkAdmin.Template> templates = SparkAdmin.templates();
        StringBuilder sb = new StringBuilder();
        boolean any = false;
        for (SparkAdmin.Template t : templates) {
            int c = SparkAdmin.count(account, chatId, userId, t.id);
            if (c > 0) any = true;
            sb.append(t.name).append(": ").append(c).append("×\n");
        }
        AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity());
        b.setTitle(SparkUi.mark("Zähler von " + name));
        b.setMessage((templates.isEmpty() ? "Keine Vorlagen." : sb.toString().trim()) + "\n\nNur in dieser Gruppe, nur auf diesem Gerät.");
        if (any) {
            b.setNeutralButton("Zurücksetzen", (d, w) -> {
                for (SparkAdmin.Template t : templates) SparkAdmin.setCount(account, chatId, userId, t.id, 0);
                org.telegram.ui.Components.BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, "✦ Zähler von " + name + " zurückgesetzt").show();
            });
        }
        b.setPositiveButton("OK", null);
        fragment.showDialog(b.create());
    }

    public interface TextDone {
        void run(String text);
    }

    static void editText(BaseFragment fragment, String title, String value, String hint, boolean multiline, TextDone done) {
        final EditText edit = new EditText(fragment.getParentActivity());
        if (!multiline) edit.setSingleLine(true);
        edit.setText(value == null ? "" : value);
        edit.setSelection(edit.length());
        edit.setHint(hint);
        edit.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        edit.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        FrameLayout box = new FrameLayout(fragment.getParentActivity());
        box.addView(edit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 24, 0, 24, 0));
        AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity());
        b.setTitle(SparkUi.mark(title));
        b.setView(box);
        b.setPositiveButton("Speichern", (d, w) -> done.run(edit.getText().toString().trim()));
        b.setNegativeButton("Abbrechen", null);
        fragment.showDialog(b.create());
    }
}

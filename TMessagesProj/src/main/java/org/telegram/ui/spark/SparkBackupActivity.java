package org.telegram.ui.spark;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.R;
import org.telegram.messenger.spark.SparkBackup;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;
import org.telegram.ui.LoginActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/** Sicherung: Konten + Sparkgram-Daten exportieren und auf einem anderen Geraet einlesen. */
public class SparkBackupActivity extends UniversalFragment {

    private static final int ID_EXPORT = 1;
    private static final int ID_IMPORT = 2;
    private static final int ID_QR = 3;
    private static final int ID_ACC_BASE = 1000;
    private static final int REQUEST_PICK = 7731;

    private ArrayList<SparkBackup.Account> accounts = new ArrayList<>();

    @Override
    protected CharSequence getTitle() {
        return "Sicherung & Konten";
    }

    @Override
    public void onResume() {
        super.onResume();
        SparkBackup.applyPending();
        accounts = SparkBackup.lastImported();
        if (listView != null) listView.adapter.update(true);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asButton(ID_QR, R.drawable.msg_qrcode, "Konto per QR-Code hinzufügen"));
        items.add(UItem.asShadow("Zeigt einen QR-Code. Auf dem Gerät, auf dem das Konto schon angemeldet ist: Einstellungen → Geräte → „QR-Code scannen“ (bzw. „Desktop-Gerät verbinden“). Kein SMS-/Telegram-Code nötig – nur dein Passwort, falls du eins hast."));
        items.add(UItem.asButton(ID_EXPORT, R.drawable.msg_download, "Sicherung erstellen"));
        items.add(UItem.asButton(ID_IMPORT, R.drawable.msg_filled_data_files, "Sicherung importieren"));
        items.add(UItem.asShadow("Die Sicherung enthält die Telefonnummern deiner angemeldeten Konten und deine Sparkgram-Daten (Einstellungen, Notizen, Schlagwörter, lokale Namen, Vorlagen, stummgeschaltete Personen) – aber keine Sitzungen, Chats oder Schlüssel. Auf dem neuen Gerät meldest du jedes Konto mit dem Telegram-Code (und ggf. Passwort) an; die Nummer ist schon eingetragen. Die Datei enthält deine Nummern – nicht weitergeben."));
        if (!accounts.isEmpty()) {
            items.add(UItem.asHeader("Konten aus der Sicherung"));
            for (int i = 0; i < accounts.size(); i++) {
                SparkBackup.Account a = accounts.get(i);
                boolean in = SparkBackup.isLoggedIn(a.userId);
                String title = (in ? "✅ " : "➕ ") + a.name + (a.username != null ? " (@" + a.username + ")" : "");
                items.add(UItem.asButton(ID_ACC_BASE + i, title, in ? "angemeldet" : "+" + a.phone));
            }
            items.add(UItem.asShadow("Antippen = anmelden (per QR-Code oder mit Code). Die Sparkgram-Daten des Kontos werden danach automatisch übernommen."));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_QR) {
            login(null, true);
        } else if (item.id == ID_EXPORT) {
            doExport();
        } else if (item.id == ID_IMPORT) {
            try {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                getParentActivity().startActivityForResult(i, REQUEST_PICK);
            } catch (Exception e) {
                FileLog.e(e);
            }
        } else if (item.id >= ID_ACC_BASE) {
            int i = item.id - ID_ACC_BASE;
            if (i >= accounts.size()) return;
            SparkBackup.Account a = accounts.get(i);
            if (SparkBackup.isLoggedIn(a.userId)) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Ist schon angemeldet").show();
                return;
            }
            AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
            b.setTitle(SparkUi.mark(a.name));
            b.setItems(new CharSequence[]{"Per QR-Code (ohne Code)", "Mit Telegram-Code"}, (d, w) -> login(a.phone, w == 0));
            showDialog(b.create());
        }
    }

    private void login(String phone, boolean qr) {
        int free = SparkBackup.freeAccount();
        if (free < 0) {
            BulletinFactory.of(this).createErrorBulletin("Keine freien Konto-Plätze mehr").show();
            return;
        }
        LoginActivity.sparkPrefillPhone = phone;
        LoginActivity.sparkQrMode = qr;
        presentFragment(new LoginActivity(free));
    }

    private void doExport() {
        try {
            String json = SparkBackup.export();
            File f = new File(ApplicationLoader.applicationContext.getCacheDir(), "sparkgram_backup.json");
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(json.getBytes(StandardCharsets.UTF_8));
            }
            String name = "Sparkgram-Sicherung_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.GERMANY).format(new Date()) + ".json";
            SparkUi.saveToDownloads(this, f, name, "application/json");
        } catch (Exception e) {
            FileLog.e(e);
            BulletinFactory.of(this).createErrorBulletin("Fehler: " + e.getMessage()).show();
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_PICK || resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;
        try (InputStream in = ApplicationLoader.applicationContext.getContentResolver().openInputStream(data.getData());
             BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            accounts = SparkBackup.importBackup(sb.toString());
            listView.adapter.update(true);
            AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
            b.setTitle(SparkUi.mark("Sicherung eingelesen"));
            b.setMessage(accounts.size() + " Konten in der Sicherung. Tippe jedes Konto an, das noch nicht angemeldet ist – die Nummer ist dann schon eingetragen, du brauchst nur noch den Code (und ggf. dein Passwort).");
            b.setPositiveButton("OK", null);
            showDialog(b.create());
        } catch (Exception e) {
            FileLog.e(e);
            BulletinFactory.of(this).createErrorBulletin("Sicherung konnte nicht gelesen werden: " + e.getMessage()).show();
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}

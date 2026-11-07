package org.telegram.ui.spark;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.spark.SparkProfiles;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** Vorschau eines gespeicherten Profils: Bild, Name, Bio, Aktionen. */
public class SparkProfilePreviewActivity extends UniversalFragment {

    private static final int ID_APPLY = 1;
    private static final int ID_RENAME = 2;
    private static final int ID_DELETE = 3;
    private static final int ID_OPEN_SOURCE = 4;
    private static final int ID_FAVORITE = 5;

    private SparkProfiles.Entry entry;
    private View header;

    public SparkProfilePreviewActivity(SparkProfiles.Entry entry) {
        this.entry = entry;
    }

    @Override
    protected CharSequence getTitle() {
        return TextUtils.isEmpty(entry.label) ? entry.displayName() : entry.label;
    }

    private View createHeader(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(16));
        layout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap b = entry.hasPhoto() ? SparkUi.decode(entry.photoPath, AndroidUtilities.dp(240)) : null;
        if (b != null) {
            RoundedBitmapDrawable d = RoundedBitmapDrawableFactory.create(context.getResources(), b);
            d.setCircular(true);
            image.setImageDrawable(d);
            image.setOnClickListener(v -> SparkUi.showFullscreen(getParentActivity(), entry.photoPath));
        } else {
            image.setImageResource(R.drawable.msg_contacts);
            image.setScaleType(ImageView.ScaleType.CENTER);
        }
        layout.addView(image, LayoutHelper.createLinear(120, 120, Gravity.CENTER_HORIZONTAL));

        TextView name = new TextView(context);
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        name.setTypeface(AndroidUtilities.bold());
        name.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        name.setGravity(Gravity.CENTER);
        name.setText(entry.displayName());
        layout.addView(name, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 12, 0, 0));

        TextView info = new TextView(context);
        info.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        info.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        info.setGravity(Gravity.CENTER);
        String sub = (entry.original ? getString(R.string.SparkProfileOriginal) + " · " : "") + LocaleController.formatDateTime(entry.created / 1000, false);
        if (!entry.original && entry.sourceUserId != 0) {
            sub += "\nGeklont von " + (TextUtils.isEmpty(entry.sourceUsername) ? "" : "@" + entry.sourceUsername + " · ") + "ID " + entry.sourceUserId;
        }
        if (entry.hasPhoto()) {
            sub += "\n" + getString(R.string.SparkProfileTapPhoto);
        }
        info.setText(sub);
        layout.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));

        if (!TextUtils.isEmpty(entry.about)) {
            TextView bio = new TextView(context);
            bio.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            bio.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            bio.setGravity(Gravity.CENTER);
            bio.setText(entry.about);
            bio.setTextIsSelectable(true);
            layout.addView(bio, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 14, 0, 0));
        }
        return layout;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (header == null) {
            header = createHeader(getContext());
        }
        items.add(UItem.asCustom(header));
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(ID_APPLY, R.drawable.msg_openprofile, getString(R.string.SparkProfileApply)).accent());
        if (!entry.original && entry.sourceUserId != 0 && entry.sourceUserId != org.telegram.messenger.UserConfig.getInstance(currentAccount).getClientUserId()) {
            items.add(UItem.asButton(ID_OPEN_SOURCE, R.drawable.msg_contacts, getString(R.string.SparkProfileOpenSource)));
        }
        if (!entry.original) {
            items.add(UItem.asButton(ID_FAVORITE, R.drawable.msg_fave, entry.favorite ? "Favorit entfernen" : "Als Favorit oben anheften ⭐"));
        }
        items.add(UItem.asButton(ID_RENAME, R.drawable.msg_edit, getString(R.string.SparkProfileRename)));
        items.add(UItem.asButton(ID_DELETE, R.drawable.msg_delete, getString(R.string.SparkProfileDelete)).red());
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_APPLY) {
            final AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
            progress.show();
            SparkProfiles.apply(currentAccount, entry, (ok, error) -> {
                progress.dismiss();
                if (ok) {
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, getString(R.string.SparkProfileApplied)).show();
                } else {
                    BulletinFactory.of(this).createErrorBulletin(LocaleController.formatString(R.string.SparkError, String.valueOf(error))).show();
                }
            });
        } else if (item.id == ID_FAVORITE) {
            SparkProfiles.setFavorite(currentAccount, entry, !entry.favorite);
            listView.adapter.update(true);
        } else if (item.id == ID_OPEN_SOURCE) {
            openSource();
        } else if (item.id == ID_RENAME) {
            final EditText input = new EditText(getParentActivity());
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            input.setText(entry.label == null ? "" : entry.label);
            input.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            input.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
            input.setHint(entry.displayName());
            FrameLayout container = new FrameLayout(getParentActivity());
            container.addView(input, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 24, 0, 24, 0));
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(getString(R.string.SparkProfileRename));
            builder.setView(container);
            builder.setPositiveButton(getString(R.string.OK), (d, w) -> {
                String label = input.getText().toString().trim();
                entry.label = TextUtils.isEmpty(label) ? null : label;
                SparkProfiles.rename(currentAccount, entry, entry.label);
                actionBar.setTitle(getTitle());
            });
            builder.setNegativeButton(getString(R.string.Cancel), null);
            showDialog(builder.create());
        } else if (item.id == ID_DELETE) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(getString(R.string.SparkProfileDelete));
            builder.setMessage(entry.original ? "Das ist dein gesichertes Original-Profil. Wirklich löschen? Beim nächsten Klonen wird dann dein dann aktuelles Profil als neues Original gesichert." : entry.displayName());
            builder.setPositiveButton(getString(R.string.Delete), (d, w) -> {
                SparkProfiles.remove(currentAccount, entry);
                finishFragment();
            });
            builder.setNegativeButton(getString(R.string.Cancel), null);
            AlertDialog dialog = builder.create();
            showDialog(dialog);
        }
    }

    /** Oeffnet das geklonte Original per ID; laedt die Person bei Bedarf ueber ID + Zugangsschluessel nach. */
    private void openSource() {
        final org.telegram.messenger.MessagesController mc = org.telegram.messenger.MessagesController.getInstance(currentAccount);
        if (mc.getUser(entry.sourceUserId) != null) {
            presentFragment(org.telegram.ui.ProfileActivity.of(entry.sourceUserId));
            return;
        }
        if (entry.sourceAccessHash == 0) {
            BulletinFactory.of(this).createErrorBulletin(getString(R.string.SparkProfileSourceUnknown)).show();
            return;
        }
        org.telegram.tgnet.TLRPC.TL_inputUser input = new org.telegram.tgnet.TLRPC.TL_inputUser();
        input.user_id = entry.sourceUserId;
        input.access_hash = entry.sourceAccessHash;
        org.telegram.tgnet.TLRPC.TL_users_getUsers req = new org.telegram.tgnet.TLRPC.TL_users_getUsers();
        req.id.add(input);
        org.telegram.tgnet.ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            boolean found = false;
            if (response instanceof org.telegram.tgnet.Vector) {
                for (Object o : ((org.telegram.tgnet.Vector<?>) response).objects) {
                    if (o instanceof org.telegram.tgnet.TLRPC.User && !(o instanceof org.telegram.tgnet.TLRPC.TL_userEmpty)) {
                        mc.putUser((org.telegram.tgnet.TLRPC.User) o, false);
                        found = true;
                    }
                }
            }
            if (found) {
                presentFragment(org.telegram.ui.ProfileActivity.of(entry.sourceUserId));
            } else {
                BulletinFactory.of(this).createErrorBulletin(getString(R.string.SparkProfileSourceUnknown)).show();
            }
        }));
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}

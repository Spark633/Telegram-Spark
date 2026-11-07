package org.telegram.ui.spark;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.HashSet;

/** Eigene Profilbilder als Raster: mehrere auswaehlen und auf einmal loeschen. */
public class SparkPhotoManagerActivity extends BaseFragment {

    private static final int MENU_DELETE = 1;
    private static final int MENU_SELECT_ALL = 2;

    private final ArrayList<TLRPC.Photo> photos = new ArrayList<>();
    private final HashSet<Long> selected = new HashSet<>();
    private GridLayout grid;
    private TextView emptyView;
    private boolean loading;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getString(R.string.SparkPhotoManager));
        actionBar.createMenu().addItem(MENU_SELECT_ALL, R.drawable.msg_select);
        actionBar.createMenu().addItem(MENU_DELETE, R.drawable.msg_delete);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_DELETE) {
                    confirmDelete();
                } else if (id == MENU_SELECT_ALL) {
                    if (selected.size() == photos.size()) {
                        selected.clear();
                    } else {
                        for (TLRPC.Photo p : photos) {
                            selected.add(p.id);
                        }
                    }
                    rebuildGrid();
                }
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        ScrollView scroll = new ScrollView(context);
        grid = new GridLayout(context);
        grid.setColumnCount(3);
        grid.setPadding(AndroidUtilities.dp(2), AndroidUtilities.dp(2), AndroidUtilities.dp(2), AndroidUtilities.dp(2));
        scroll.addView(grid, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        emptyView = new TextView(context);
        emptyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        emptyView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setText(getString(R.string.SparkPhotoManagerLoading));
        root.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER, 24, 0, 24, 0));

        fragmentView = root;
        load();
        return fragmentView;
    }

    private void load() {
        if (loading) {
            return;
        }
        loading = true;
        photos.clear();
        selected.clear();
        loadPage(0);
    }

    private void loadPage(int offset) {
        TLRPC.TL_photos_getUserPhotos req = new TLRPC.TL_photos_getUserPhotos();
        req.user_id = MessagesController.getInstance(currentAccount).getInputUser(UserConfig.getInstance(currentAccount).getClientUserId());
        req.offset = offset;
        req.max_id = 0;
        req.limit = 100;
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (response instanceof TLRPC.photos_Photos) {
                TLRPC.photos_Photos res = (TLRPC.photos_Photos) response;
                MessagesController.getInstance(currentAccount).putUsers(res.users, false);
                for (TLRPC.Photo p : res.photos) {
                    if (p instanceof TLRPC.TL_photo) {
                        photos.add(p);
                    }
                }
                if (res instanceof TLRPC.TL_photos_photosSlice && res.photos.size() > 0 && photos.size() < res.count) {
                    loadPage(photos.size());
                    return;
                }
            }
            loading = false;
            rebuildGrid();
        }));
    }

    private void rebuildGrid() {
        if (grid == null) {
            return;
        }
        grid.removeAllViews();
        emptyView.setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
        if (photos.isEmpty()) {
            emptyView.setText(getString(loading ? R.string.SparkPhotoManagerLoading : R.string.SparkPhotoManagerEmpty));
        }
        int cell = (AndroidUtilities.displaySize.x - AndroidUtilities.dp(4)) / 3;
        for (int i = 0; i < photos.size(); i++) {
            final TLRPC.Photo photo = photos.get(i);
            FrameLayout frame = new FrameLayout(getParentActivity());
            BackupImageView image = new BackupImageView(getParentActivity());
            TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, 320);
            image.setImage(ImageLocation.getForPhoto(size, photo), "200_200", (Drawable) null, photo);
            frame.addView(image, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.NO_GRAVITY, 1, 1, 1, 1));

            TextView date = new TextView(getParentActivity());
            date.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            date.setTextColor(Color.WHITE);
            date.setShadowLayer(AndroidUtilities.dp(2), 0, 0, Color.BLACK);
            date.setText((i == 0 ? getString(R.string.SparkPhotoCurrent) + "\n" : "") + LocaleController.formatDateChat(photo.date));
            frame.addView(date, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 5, 0, 0, 4));

            boolean isSelected = selected.contains(photo.id);
            View overlay = new View(getParentActivity());
            overlay.setBackgroundColor(isSelected ? 0x66FF8A3D : 0);
            frame.addView(overlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            TextView check = new TextView(getParentActivity());
            check.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            check.setTextColor(Color.WHITE);
            check.setGravity(Gravity.CENTER);
            check.setText(isSelected ? "✓" : "");
            check.setBackground(Theme.createCircleDrawable(AndroidUtilities.dp(24), isSelected ? SparkUi.MARKER_COLOR : 0x55000000));
            frame.addView(check, LayoutHelper.createFrame(24, 24, Gravity.TOP | Gravity.RIGHT, 0, 6, 6, 0));

            frame.setOnClickListener(v -> {
                if (selected.contains(photo.id)) {
                    selected.remove(photo.id);
                } else {
                    selected.add(photo.id);
                }
                rebuildGrid();
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cell;
            lp.height = cell;
            grid.addView(frame, lp);
        }
        actionBar.setTitle(selected.isEmpty() ? getString(R.string.SparkPhotoManager) : LocaleController.formatString(R.string.SparkPhotoSelected, selected.size(), photos.size()));
    }

    private void confirmDelete() {
        if (selected.isEmpty()) {
            BulletinFactory.of(this).createErrorBulletin(getString(R.string.SparkPhotoNoneSelected)).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString(R.string.SparkPhotoManager));
        builder.setMessage(LocaleController.formatString(R.string.SparkPhotoDeleteConfirm, selected.size()));
        builder.setPositiveButton(getString(R.string.Delete), (d, w) -> delete());
        builder.setNegativeButton(getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        showDialog(dialog);
    }

    private void delete() {
        final MessagesController mc = MessagesController.getInstance(currentAccount);
        final long selfId = UserConfig.getInstance(currentAccount).getClientUserId();
        TLRPC.User self = mc.getUser(selfId);
        final long currentPhotoId = self != null && self.photo != null ? self.photo.photo_id : 0;
        boolean deleteCurrent = false;
        TLRPC.TL_photos_deletePhotos req = new TLRPC.TL_photos_deletePhotos();
        for (TLRPC.Photo p : photos) {
            if (!selected.contains(p.id)) {
                continue;
            }
            if (p.id == currentPhotoId) {
                deleteCurrent = true;
                continue;
            }
            TLRPC.TL_inputPhoto input = new TLRPC.TL_inputPhoto();
            input.id = p.id;
            input.access_hash = p.access_hash;
            input.file_reference = p.file_reference != null ? p.file_reference : new byte[0];
            req.id.add(input);
            mc.getDialogPhotos(selfId).removePhoto(p.id);
        }
        final boolean finalDeleteCurrent = deleteCurrent;
        final int count = selected.size();
        final AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progress.show();
        Runnable done = () -> {
            if (finalDeleteCurrent) {
                mc.deleteUserPhoto(null);
            }
            if (self != null) {
                mc.loadFullUser(self, classGuid, true);
            }
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_AVATAR);
            progress.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, LocaleController.formatString(R.string.SparkPhotoDeleted, count)).show();
            AndroidUtilities.runOnUIThread(this::load, 800);
        };
        if (req.id.isEmpty()) {
            done.run();
        } else {
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(done));
        }
    }
}

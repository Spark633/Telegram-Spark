package org.telegram.ui.spark;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

/**
 * Hinweis-Leiste oben im Bildschirm (ueber allem): antippen = Aktion, ✕ = schliessen.
 * Bleibt einige Sekunden oder – auf Wunsch – bis man sie wegklickt.
 */
public class SparkTopBanner {

    public static void show(Activity activity, CharSequence text, Runnable onClick, boolean sticky) {
        if (activity == null || activity.isFinishing()) {
            org.telegram.messenger.spark.SparkLog.log("Leiste oben: keine Activity");
            return;
        }
        org.telegram.messenger.spark.SparkLog.log("Leiste oben angezeigt");
        // in den Inhaltsbereich der App legen (liegt sicher ueber Telegrams Oberflaeche)
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)) content = activity.getWindow().getDecorView();
        if (!(content instanceof ViewGroup)) return;
        final ViewGroup root = (ViewGroup) content;

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_dialogBackground));
        bg.setCornerRadius(AndroidUtilities.dp(16));
        box.setBackground(bg);
        box.setElevation(AndroidUtilities.dp(8));
        box.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12));

        TextView t = new TextView(activity);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        t.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        box.addView(t, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        TextView close = new TextView(activity);
        close.setText("✕");
        close.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        close.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        close.setGravity(Gravity.CENTER);
        box.addView(close, LayoutHelper.createLinear(44, 44));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
        lp.leftMargin = lp.rightMargin = AndroidUtilities.dp(12);
        lp.topMargin = AndroidUtilities.statusBarHeight + AndroidUtilities.dp(8);
        root.addView(box, lp);
        box.bringToFront();
        box.setTranslationZ(AndroidUtilities.dp(16));

        final Runnable[] hide = new Runnable[1];
        hide[0] = () -> box.animate().translationY(-AndroidUtilities.dp(160)).alpha(0f).setDuration(220)
            .withEndAction(() -> { try { root.removeView(box); } catch (Throwable ignore) {} }).start();
        close.setOnClickListener(v -> hide[0].run());
        box.setOnClickListener(v -> {
            hide[0].run();
            if (onClick != null) onClick.run();
        });

        box.setTranslationY(-AndroidUtilities.dp(160));
        box.setAlpha(0f);
        box.animate().translationY(0).alpha(1f).setDuration(260).start();
        if (!sticky) {
            AndroidUtilities.runOnUIThread(hide[0], 10_000);
        }
    }
}

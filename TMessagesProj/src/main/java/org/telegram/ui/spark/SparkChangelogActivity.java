package org.telegram.ui.spark;

import android.view.View;

import org.telegram.messenger.spark.SparkChangelog;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** "Was ist neu?": alle Sparkgram-Versionen mit ihren Aenderungen. */
public class SparkChangelogActivity extends UniversalFragment {

    @Override
    protected CharSequence getTitle() {
        return "Was ist neu?";
    }

    public static String text(SparkChangelog.Release r) {
        StringBuilder sb = new StringBuilder();
        for (String c : r.changes) {
            if (sb.length() > 0) sb.append('\n');
            sb.append("• ").append(c);
        }
        return sb.toString();
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        String installed = org.telegram.messenger.BuildConfig.SPARK_VERSION;
        for (SparkChangelog.Release r : SparkChangelog.RELEASES) {
            String head = "Version " + r.version + " · " + r.date + (r.version.equals(installed) ? " · installiert" : "");
            items.add(UItem.asHeader(head));
            items.add(UItem.asShadow(text(r)));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}

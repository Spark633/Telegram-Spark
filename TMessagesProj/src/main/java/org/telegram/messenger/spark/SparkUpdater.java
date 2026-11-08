package org.telegram.messenger.spark;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Prueft die GitHub-Releases des Sparkgram-Repos auf neuere APKs und installiert sie. */
public class SparkUpdater {

    public static final String REPO = "Spark633/Telegram-Spark";
    public static final String REPO_URL = "https://github.com/" + REPO;

    public static class Release {
        public String channel;
        public String commit;
        public String apkName;
        public String apkUrl;
        public long apkSize;
        public String publishedAt;

        /** true, wenn GitHub nicht erreichbar war und nur der feste Link bekannt ist. */
        public boolean unknown;

        public boolean isNewer() {
            return commit != null && !commit.isEmpty() && !commit.equals(BuildConfig.SPARK_COMMIT);
        }
    }

    public interface Progress {
        void onProgress(float progress);
    }

    public static String currentVersion() {
        String c = BuildConfig.SPARK_COMMIT;
        return BuildConfig.BUILD_VERSION_STRING + " · " + BuildConfig.SPARK_CHANNEL + (c != null && c.length() >= 7 ? " · " + c.substring(0, 7) : "");
    }

    /** z. B. "entwicklung · 4993e98 · 06.11.2026 23:10" */
    public static String buildLabel() {
        String c = BuildConfig.SPARK_COMMIT;
        String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.GERMANY).format(new java.util.Date(BuildConfig.SPARK_BUILD_TIME));
        return BuildConfig.SPARK_VERSION + " · " + BuildConfig.SPARK_CHANNEL + (c != null && c.length() >= 7 ? " · " + c.substring(0, 7) : "") + " · " + date;
    }

    private static String channel() {
        return SparkConfig.getRaw(SparkConfig.UPDATE_TEST_BUILDS) ? "entwicklung" : "main";
    }

    /** Callback auf dem UI-Thread: Release oder null, bei Fehler error != null. */
    public static void check(Utilities.Callback2<Release, String> done) {
        final String channel = channel();
        Utilities.globalQueue.postRunnable(() -> {
            Release release = null;
            String error = null;
            try {
                String json = httpGet("https://api.github.com/repos/" + REPO + "/releases/tags/build-" + channel);
                if (json == null) {
                    error = "Noch keine Version für „" + channel + "“ veröffentlicht";
                } else {
                    JSONObject o = new JSONObject(json);
                    release = new Release();
                    release.channel = channel;
                    release.commit = o.optString("target_commitish");
                    release.publishedAt = o.optString("published_at");
                    JSONArray assets = o.optJSONArray("assets");
                    if (assets != null) {
                        for (int i = 0; i < assets.length(); i++) {
                            JSONObject a = assets.getJSONObject(i);
                            if (a.optString("name").endsWith(".apk")) {
                                // feste Datei "Sparkgram-<kanal>.apk" bevorzugen (bleibt bei neuen Baeuen gleich)
                                boolean fixed = a.optString("name").equals("Sparkgram-" + channel + ".apk");
                                if (release.apkUrl == null || fixed) {
                                    release.apkName = a.optString("name");
                                    release.apkUrl = a.optString("browser_download_url");
                                    release.apkSize = a.optLong("size");
                                }
                                if (fixed) break;
                            }
                        }
                    }
                    if (release.apkUrl == null) {
                        error = "Im Release ist keine APK enthalten";
                        release = null;
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
                error = e.getMessage();
                // GitHub-Schnittstelle nicht erreichbar: fester Download-Link als Ausweg
                release = new Release();
                release.channel = channel;
                release.unknown = true;
                release.apkName = "Sparkgram-" + channel + ".apk";
                release.apkUrl = "https://github.com/" + REPO + "/releases/download/build-" + channel + "/Sparkgram-" + channel + ".apk";
            }
            final Release r = release;
            final String err = error;
            AndroidUtilities.runOnUIThread(() -> done.run(r, err));
        });
    }

    /** Mit bis zu 3 Versuchen bei Server-Fehlern (5xx). */
    private static String httpGet(String url) throws Exception {
        Exception last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return httpGetOnce(url);
            } catch (Exception e) {
                last = e;
                if (e.getMessage() == null || !e.getMessage().contains(" 5")) break;
                try { Thread.sleep(1500L * (attempt + 1)); } catch (InterruptedException ignore) {}
            }
        }
        throw last;
    }

    private static String httpGetOnce(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", "Sparkgram-Updater");
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        int code = c.getResponseCode();
        if (code == 404) {
            return null;
        }
        if (code != 200) {
            throw new Exception("GitHub antwortet mit " + code);
        }
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) {
                out.write(buf, 0, r);
            }
            return out.toString("UTF-8");
        } finally {
            c.disconnect();
        }
    }

    /** Laedt die APK herunter; Callback auf dem UI-Thread mit Datei oder null. */
    public static void download(Release release, Progress progress, Utilities.Callback2<File, String> done) {
        Utilities.globalQueue.postRunnable(() -> {
            File dir = new File(ApplicationLoader.getFilesDirFixed(), "cache");
            dir.mkdirs();
            File target = new File(dir, "sparkgram-update.apk");
            String error = null;
            String[] urls = {release.apkUrl, "https://github.com/" + REPO + "/releases/download/build-" + release.channel + "/Sparkgram-" + release.channel + ".apk"};
            for (String url : urls) {
            error = null;
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setRequestProperty("User-Agent", "Sparkgram-Updater");
                c.setInstanceFollowRedirects(true);
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                long total = c.getContentLengthLong() > 0 ? c.getContentLengthLong() : release.apkSize;
                try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(target)) {
                    byte[] buf = new byte[65536];
                    long loaded = 0;
                    int r;
                    long lastPost = 0;
                    while ((r = in.read(buf)) > 0) {
                        out.write(buf, 0, r);
                        loaded += r;
                        long now = System.currentTimeMillis();
                        if (total > 0 && progress != null && now - lastPost > 200) {
                            lastPost = now;
                            final float p = loaded / (float) total;
                            AndroidUtilities.runOnUIThread(() -> progress.onProgress(p));
                        }
                    }
                } finally {
                    c.disconnect();
                }
            } catch (java.io.FileNotFoundException e) {
                FileLog.e(e);
                error = "Datei nicht gefunden (vermutlich wird gerade eine neue Version gebaut)";
                target.delete();
                continue;
            } catch (Exception e) {
                FileLog.e(e);
                error = e.getMessage();
                target.delete();
            }
            break;
            }
            final String err = error;
            AndroidUtilities.runOnUIThread(() -> done.run(err == null ? target : null, err));
        });
    }

    /** Startet die Installation. Liefert eine Fehlermeldung oder null. */
    public static String install(Activity activity, File apk) {
        try {
            // Android 8+: Erlaubnis "Unbekannte Apps installieren" fuer Sparkgram noetig
            if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) {
                Intent s = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ApplicationLoader.getApplicationId()));
                activity.startActivity(s);
                return "Bitte erlaube Sparkgram „Unbekannte Apps installieren“ und tippe danach nochmal auf Aktualisieren.";
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri;
            if (Build.VERSION.SDK_INT >= 24) {
                uri = FileProvider.getUriForFile(activity, ApplicationLoader.getApplicationId() + ".provider", apk);
            } else {
                uri = Uri.fromFile(apk);
            }
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            activity.startActivity(intent);
            return null;
        } catch (Exception e) {
            FileLog.e(e);
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
}

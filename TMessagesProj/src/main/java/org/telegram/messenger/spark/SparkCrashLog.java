package org.telegram.messenger.spark;

import android.content.Context;
import android.os.Build;

import org.telegram.messenger.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Speichert den letzten Java-Absturz, damit er in den Sparkgram-Einstellungen angezeigt werden kann. */
public class SparkCrashLog {

    private static File file(Context context) {
        return new File(context.getFilesDir(), "sparkgram_last_crash.txt");
    }

    public static void install(Context context) {
        if (context == null) {
            return;
        }
        final Context app = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println("Sparkgram " + BuildConfig.BUILD_VERSION_STRING + " " + BuildConfig.SPARK_CHANNEL + " " + BuildConfig.SPARK_COMMIT);
                pw.println("Android " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + "), " + Build.MANUFACTURER + " " + Build.MODEL);
                pw.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + " Thread: " + thread.getName());
                throwable.printStackTrace(pw);
                pw.flush();
                try (FileOutputStream out = new FileOutputStream(file(app))) {
                    out.write(sw.toString().getBytes(StandardCharsets.UTF_8));
                }
            } catch (Throwable ignore) {
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            }
        });
    }

    public static String read(Context context) {
        try {
            File f = file(context);
            if (!f.exists()) {
                return null;
            }
            byte[] data = new byte[(int) Math.min(f.length(), 200000)];
            try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                int off = 0, r;
                while (off < data.length && (r = in.read(data, off, data.length - off)) > 0) {
                    off += r;
                }
            }
            return new String(data, StandardCharsets.UTF_8);
        } catch (Throwable e) {
            return null;
        }
    }

    public static void clear(Context context) {
        file(context).delete();
    }
}

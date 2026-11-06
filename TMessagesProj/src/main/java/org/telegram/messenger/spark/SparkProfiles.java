package org.telegram.messenger.spark;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Profil-Bibliothek: sichert das eigene Profil, klont fremde Profile (Name, Bio, Profilbild)
 * und kann jedes gespeicherte Profil wieder auf das eigene Konto anwenden.
 * Alles wird nur lokal auf dem Geraet gespeichert (pro Konto).
 */
public class SparkProfiles {

    public static class Entry {
        public String id;
        public String label;
        public String firstName;
        public String lastName;
        public String about;
        public String photoPath;
        public long sourceUserId;
        public long created;
        public boolean original;

        public String displayName() {
            String name = (firstName == null ? "" : firstName) + (TextUtils.isEmpty(lastName) ? "" : " " + lastName);
            return name.trim();
        }

        public boolean hasPhoto() {
            return photoPath != null && new File(photoPath).exists();
        }

        JSONObject toJson() throws Exception {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("label", label);
            o.put("first", firstName);
            o.put("last", lastName);
            o.put("about", about);
            o.put("photo", photoPath);
            o.put("source", sourceUserId);
            o.put("created", created);
            o.put("original", original);
            return o;
        }

        static Entry fromJson(JSONObject o) {
            Entry e = new Entry();
            e.id = o.optString("id");
            e.label = o.optString("label", null);
            e.firstName = o.optString("first", "");
            e.lastName = o.optString("last", "");
            e.about = o.optString("about", "");
            e.photoPath = o.has("photo") && !o.isNull("photo") ? o.optString("photo") : null;
            e.sourceUserId = o.optLong("source");
            e.created = o.optLong("created");
            e.original = o.optBoolean("original");
            return e;
        }
    }

    public interface Done {
        void run(boolean ok, String error);
    }

    private static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("sparkgram_profiles_" + account, Context.MODE_PRIVATE);
    }

    private static File dir(int account) {
        File d = new File(ApplicationLoader.getFilesDirFixed(), "sparkgram/profiles/" + account);
        if (!d.exists()) {
            d.mkdirs();
        }
        return d;
    }

    public static ArrayList<Entry> load(int account) {
        ArrayList<Entry> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(account).getString("list", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                list.add(Entry.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public static void save(int account, ArrayList<Entry> list) {
        try {
            JSONArray arr = new JSONArray();
            for (Entry e : list) {
                arr.put(e.toJson());
            }
            prefs(account).edit().putString("list", arr.toString()).apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void add(int account, Entry entry) {
        ArrayList<Entry> list = load(account);
        list.add(entry);
        save(account, list);
    }

    public static void remove(int account, Entry entry) {
        ArrayList<Entry> list = load(account);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(entry.id)) {
                Entry e = list.remove(i);
                if (e.photoPath != null) {
                    new File(e.photoPath).delete();
                }
                break;
            }
        }
        save(account, list);
    }

    public static void rename(int account, Entry entry, String label) {
        ArrayList<Entry> list = load(account);
        for (Entry e : list) {
            if (e.id.equals(entry.id)) {
                e.label = label;
            }
        }
        save(account, list);
    }

    /** Liefert den Eintrag, der dem aktuellen eigenen Profil entspricht (Name + Bio), sonst null. */
    private static Entry findMatching(int account, String first, String last, String about) {
        for (Entry e : load(account)) {
            if (TextUtils.equals(n(e.firstName), n(first)) && TextUtils.equals(n(e.lastName), n(last)) && TextUtils.equals(n(e.about), n(about))) {
                return e;
            }
        }
        return null;
    }

    private static String n(String s) {
        return s == null ? "" : s;
    }

    /**
     * Erstellt einen Schnappschuss eines Profils (Name, Bio, groesstes Profilbild).
     * Der Callback laeuft auf dem UI-Thread; bei Fehlern wird null geliefert.
     */
    public static void snapshot(int account, TLRPC.User user, boolean original, Utilities.Callback<Entry> done) {
        if (user == null) {
            done.run(null);
            return;
        }
        TLRPC.TL_users_getFullUser req = new TLRPC.TL_users_getFullUser();
        req.id = MessagesController.getInstance(account).getInputUser(user);
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            TLRPC.UserFull full = null;
            if (response instanceof TLRPC.TL_users_userFull) {
                TLRPC.TL_users_userFull res = (TLRPC.TL_users_userFull) response;
                MessagesController.getInstance(account).putUsers(res.users, false);
                full = res.full_user;
            } else {
                full = MessagesController.getInstance(account).getUserFull(user.id);
            }
            Entry e = new Entry();
            e.id = UUID.randomUUID().toString();
            e.firstName = n(user.first_name);
            e.lastName = n(user.last_name);
            e.about = full != null ? n(full.about) : "";
            e.sourceUserId = user.id;
            e.created = System.currentTimeMillis();
            e.original = original;
            TLRPC.Photo photo = null;
            if (full != null) {
                photo = full.profile_photo;
                if (photo == null || photo instanceof TLRPC.TL_photoEmpty) {
                    photo = full.fallback_photo;
                }
            }
            if (photo == null || photo instanceof TLRPC.TL_photoEmpty || photo.sizes == null || photo.sizes.isEmpty()) {
                done.run(e);
                return;
            }
            final File target = new File(dir(account), e.id + ".jpg");
            downloadPhoto(account, photo, target, ok -> {
                if (ok) {
                    e.photoPath = target.getAbsolutePath();
                }
                done.run(e);
            });
        }));
    }

    private static void downloadPhoto(int account, TLRPC.Photo photo, File target, Utilities.Callback<Boolean> done) {
        final TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, 1280);
        if (size == null) {
            done.run(false);
            return;
        }
        File existing = findLoaded(account, size);
        if (existing != null) {
            done.run(copy(existing, target));
            return;
        }
        final String name = FileLoader.getAttachFileName(size);
        final NotificationCenter nc = NotificationCenter.getInstance(account);
        final boolean[] finished = new boolean[1];
        final NotificationCenter.NotificationCenterDelegate[] observer = new NotificationCenter.NotificationCenterDelegate[1];
        final Runnable finish = () -> {
            nc.removeObserver(observer[0], NotificationCenter.fileLoaded);
            nc.removeObserver(observer[0], NotificationCenter.fileLoadFailed);
        };
        observer[0] = (id, acc, args) -> {
            if (finished[0] || args.length == 0 || !name.equals(args[0])) {
                return;
            }
            finished[0] = true;
            finish.run();
            if (id == NotificationCenter.fileLoaded) {
                File f = args.length > 1 && args[1] instanceof File ? (File) args[1] : findLoaded(account, size);
                done.run(f != null && copy(f, target));
            } else {
                done.run(false);
            }
        };
        nc.addObserver(observer[0], NotificationCenter.fileLoaded);
        nc.addObserver(observer[0], NotificationCenter.fileLoadFailed);
        FileLoader.getInstance(account).loadFile(ImageLocation.getForPhoto(size, photo), photo, "jpg", FileLoader.PRIORITY_HIGH, 1);
        AndroidUtilities.runOnUIThread(() -> {
            if (!finished[0]) {
                finished[0] = true;
                finish.run();
                File f = findLoaded(account, size);
                done.run(f != null && copy(f, target));
            }
        }, 30000);
    }

    private static File findLoaded(int account, TLRPC.PhotoSize size) {
        File f = FileLoader.getInstance(account).getPathToAttach(size, true);
        if (f != null && f.exists() && f.length() > 0) {
            return f;
        }
        f = FileLoader.getInstance(account).getPathToAttach(size, false);
        if (f != null && f.exists() && f.length() > 0) {
            return f;
        }
        return null;
    }

    private static boolean copy(File src, File dst) {
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[16384];
            int r;
            while ((r = in.read(buf)) > 0) {
                out.write(buf, 0, r);
            }
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    /**
     * Sichert das aktuelle eigene Profil, falls es noch nicht in der Bibliothek liegt.
     * Das allererste gesicherte Profil wird als "Original" markiert.
     */
    public static void backupCurrent(int account, Utilities.Callback<Boolean> done) {
        TLRPC.User self = UserConfig.getInstance(account).getCurrentUser();
        if (self == null) {
            done.run(false);
            return;
        }
        boolean hasOriginal = false;
        for (Entry e : load(account)) {
            if (e.original) {
                hasOriginal = true;
                break;
            }
        }
        final boolean markOriginal = !hasOriginal;
        snapshot(account, self, markOriginal, e -> {
            if (e == null) {
                done.run(false);
                return;
            }
            if (findMatching(account, e.firstName, e.lastName, e.about) != null && !markOriginal) {
                // aktueller Stand ist schon gespeichert
                if (e.photoPath != null) {
                    new File(e.photoPath).delete();
                }
                done.run(true);
                return;
            }
            if (markOriginal) {
                e.label = "Original";
            }
            add(account, e);
            done.run(true);
        });
    }

    /** Klont das Profil von {@code target} auf das eigene Konto (vorher wird gesichert). */
    public static void cloneFrom(int account, TLRPC.User target, Done done) {
        backupCurrent(account, backedUp -> {
            if (!backedUp) {
                done.run(false, "Sicherung des eigenen Profils fehlgeschlagen");
                return;
            }
            snapshot(account, target, false, e -> {
                if (e == null) {
                    done.run(false, "Profil konnte nicht geladen werden");
                    return;
                }
                add(account, e);
                apply(account, e, done);
            });
        });
    }

    /** Wendet einen gespeicherten Eintrag auf das eigene Profil an. */
    public static void apply(int account, Entry e, Done done) {
        final boolean name = SparkConfig.get(SparkConfig.CLONE_NAME);
        final boolean bio = SparkConfig.get(SparkConfig.CLONE_BIO);
        final boolean photo = SparkConfig.get(SparkConfig.CLONE_PHOTO) && e.hasPhoto();
        if (!name && !bio && !photo) {
            done.run(true, null);
            return;
        }
        if (name || bio) {
            updateProfile(account, e, name, bio, true, (ok, err) -> {
                if (!ok) {
                    done.run(false, err);
                } else if (photo) {
                    uploadPhoto(account, e.photoPath, done);
                } else {
                    finishApply(account);
                    done.run(true, null);
                }
            });
        } else {
            uploadPhoto(account, e.photoPath, done);
        }
    }

    private static void updateProfile(int account, Entry e, boolean name, boolean bio, boolean allowRetry, Done done) {
        TL_account.updateProfile req = new TL_account.updateProfile();
        if (name) {
            req.flags |= 1 | 2;
            req.first_name = TextUtils.isEmpty(e.firstName) ? "." : e.firstName;
            req.last_name = n(e.lastName);
        }
        String about = n(e.about);
        if (bio) {
            req.flags |= 4;
            req.about = about;
        }
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error != null) {
                if (allowRetry && bio && error.text != null && error.text.contains("ABOUT_TOO_LONG") && about.length() > 70) {
                    Entry shortened = new Entry();
                    shortened.firstName = e.firstName;
                    shortened.lastName = e.lastName;
                    shortened.about = about.substring(0, 70);
                    updateProfile(account, shortened, name, bio, false, done);
                } else {
                    done.run(false, error.text);
                }
                return;
            }
            if (response instanceof TLRPC.User) {
                TLRPC.User user = (TLRPC.User) response;
                MessagesController.getInstance(account).putUser(user, false);
                UserConfig.getInstance(account).setCurrentUser(user);
                UserConfig.getInstance(account).saveConfig(true);
            }
            if (bio) {
                TLRPC.UserFull full = MessagesController.getInstance(account).getUserFull(UserConfig.getInstance(account).getClientUserId());
                if (full != null) {
                    full.about = req.about;
                    full.flags = TextUtils.isEmpty(full.about) ? (full.flags & ~2) : (full.flags | 2);
                }
            }
            done.run(true, null);
        }));
    }

    private static void uploadPhoto(int account, String path, Done done) {
        FileLoader.getInstance(account).uploadFile(path, inputFile -> {
            if (inputFile == null) {
                AndroidUtilities.runOnUIThread(() -> done.run(false, "Upload des Profilbilds fehlgeschlagen"));
                return;
            }
            TLRPC.TL_photos_uploadProfilePhoto req = new TLRPC.TL_photos_uploadProfilePhoto();
            req.file = inputFile;
            req.flags |= 1;
            ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                if (error != null) {
                    done.run(false, error.text);
                    return;
                }
                if (response instanceof TLRPC.TL_photos_photo) {
                    TLRPC.TL_photos_photo res = (TLRPC.TL_photos_photo) response;
                    MessagesController.getInstance(account).putUsers(res.users, false);
                    long selfId = UserConfig.getInstance(account).getClientUserId();
                    for (TLRPC.User u : res.users) {
                        if (u.id == selfId) {
                            UserConfig.getInstance(account).setCurrentUser(u);
                            UserConfig.getInstance(account).saveConfig(true);
                        }
                    }
                }
                finishApply(account);
                done.run(true, null);
            }));
        });
    }

    private static void finishApply(int account) {
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.mainUserInfoChanged);
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        TLRPC.User self = UserConfig.getInstance(account).getCurrentUser();
        if (self != null) {
            MessagesController.getInstance(account).loadFullUser(self, 0, true);
        }
    }
}

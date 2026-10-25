package de.tcltv.launcher;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/** TCL Home's named app directory adapted to overlapping, installed-app categories. */
public final class AppCategories {
    public enum Category {
        ALL("Alle Apps"), RECOMMENDED("Empfehlungen"), MOVIES("Filme + Serien"),
        ENTERTAINMENT("Unterhaltung"), MUSIC("Musik"), SPORTS("Sport"), GAMES("Spiele"),
        KIDS("Kinder"), MEDIA_LIBRARIES("Mediatheken"), TOOLS("Tools");
        public final String label;
        Category(String label) { this.label = label; }
    }

    private AppCategories() {}

    public static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    public static EnumSet<Category> categories(AppEntry app) {
        String pkg = normalize(app.packageName);
        String name = normalize(app.name);
        String value = pkg + " " + name;
        EnumSet<Category> result = EnumSet.of(Category.ALL);
        boolean movies = any(value, "netflix", "disney", "amazonvideo", "avod", "prime video",
                "appletv", "apple tv", "de.sky.online", "de.sky.bw", "wow", "paramount", "com.cbs.", "plex", "rakuten", "mubi");
        boolean media = any(value, "de.swr.avp", "zdf", "mediathek", "joyn", "seventv", "de.rtli", "rtl+", "arte", "3sat", "phoenix") || name.matches(".*\\bard\\b.*");
        boolean entertainment = movies || media || any(value, "youtube", "twitch", "waipu", "zattoo", "pluto", "tubi", "vimeo", "dailymotion");
        if (movies) result.add(Category.MOVIES);
        if (media) result.add(Category.MEDIA_LIBRARIES);
        if (entertainment) result.add(Category.ENTERTAINMENT);
        if (any(value, "spotify", "music", "musik", "tidal", "deezer", "tunein", "soundcloud", "radio", "qobuz")) result.add(Category.MUSIC);
        if (any(value, "dazn", "sport", "eurosport", "fifa", "nba", "nfl", "f1tv", "redbull", "red bull", "wow", "de.sky.online")) result.add(Category.SPORTS);
        if (any(value, "game", "spiel", "geforcenow", "steamlink", "steam link", "retroarch", "minecraft", "moonlight", "xbox", "playstation")) result.add(Category.GAMES);
        if (any(value, "kids", "kinder", "kika", "toggo", "disney", "sesame", "nickelodeon")) result.add(Category.KIDS);
        if (any(value, "youtube", "netflix", "prime", "amazonvideo", "avod", "disney", "appletv", "apple tv", "wow", "de.sky.online", "de.rtli", "rtl+", "joyn", "seventv", "waipu", "zattoo", "de.swr.avp", "zdf", "spotify")) result.add(Category.RECOMMENDED);
        if (result.size() == 1) result.add(Category.TOOLS);
        return result;
    }

    public static EnumSet<Category> categories(AppEntry app, PackageManager pm) {
        EnumSet<Category> result = categories(app);
        try {
            ApplicationInfo info = pm.getApplicationInfo(app.packageName, 0);
            if (info.category == ApplicationInfo.CATEGORY_GAME) result.add(Category.GAMES);
            if (info.category == ApplicationInfo.CATEGORY_AUDIO) result.add(Category.MUSIC);
            if (info.category == ApplicationInfo.CATEGORY_VIDEO) result.add(Category.ENTERTAINMENT);
            if (result.size() > 2) result.remove(Category.TOOLS);
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) { }
        return result;
    }

    public static List<AppEntry> filter(List<AppEntry> apps, Category category) {
        List<AppEntry> result = new ArrayList<>();
        for (AppEntry app : apps) if (categories(app).contains(category)) result.add(app);
        return result;
    }

    private static boolean any(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }
}

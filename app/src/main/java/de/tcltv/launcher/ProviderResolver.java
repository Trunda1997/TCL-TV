package de.tcltv.launcher;

import java.util.List;

/** Matches the visible provider name to an installed TV application. */
public final class ProviderResolver {
    private ProviderResolver() { }

    public static AppEntry find(List<AppEntry> apps, String provider) {
        if (apps == null || provider == null) return null;
        String[] aliases = aliases(provider);
        AppEntry best = null;
        int bestScore = 0;
        for (AppEntry app : apps) {
            String name = AppCategories.normalize(app.name);
            String pkg = AppCategories.normalize(app.packageName);
            for (String alias : aliases) {
                String key = AppCategories.normalize(alias);
                int score = name.equals(key) ? 100 : name.startsWith(key) ? 80
                        : name.contains(key) ? 65 : pkg.contains(key.replace(" ", "")) ? 55 : 0;
                if (score > bestScore) { best = app; bestScore = score; }
            }
        }
        return best;
    }

    private static String[] aliases(String provider) {
        String key = AppCategories.normalize(provider);
        if (key.contains("prime") || key.contains("amazon")) return new String[]{"Prime Video", "Amazon Video", "amazonvideo", "avod"};
        if (key.contains("disney")) return new String[]{"Disney+", "Disney Plus", "disneyplus"};
        if (key.equals("wow")) return new String[]{"WOW", "Sky", "sky online"};
        if (key.contains("apple")) return new String[]{"Apple TV", "appletv"};
        if (key.contains("rtl")) return new String[]{"RTL+", "RTL Plus", "rtli"};
        if (key.contains("ard")) return new String[]{"ARD Mediathek", "ARD", "swr avp"};
        if (key.contains("zdf")) return new String[]{"ZDFmediathek", "ZDF", "zdf mediathek"};
        if (key.contains("waipu")) return new String[]{"waipu.tv", "waipu", "exaring"};
        if (key.contains("joyn")) return new String[]{"Joyn", "seventv"};
        return new String[]{provider};
    }
}

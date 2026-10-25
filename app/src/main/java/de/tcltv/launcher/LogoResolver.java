package de.tcltv.launcher;

import java.util.Locale;

public final class LogoResolver {
    private LogoResolver() {}

    public static DeviceBrand resolve(String raw) {
        String s = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        if (containsAny(s, "playstation", "ps5", "ps4", "ps 5", "ps 4", "sony interactive")) return DeviceBrand.PLAYSTATION;
        if (containsAny(s, "xbox", "series x", "series s")) return DeviceBrand.XBOX;
        if (containsAny(s, "switch 2", "switch2", "nintendo switch 2")) return DeviceBrand.SWITCH2;
        if (containsAny(s, "nintendo", "switch")) return DeviceBrand.NINTENDO;
        if (containsAny(s, "apple tv", "appletv")) return DeviceBrand.APPLE_TV;
        if (containsAny(s, "fire tv", "firetv", "amazon")) return DeviceBrand.FIRE_TV;
        if (containsAny(s, "chromecast", "google tv", "google streamer")) return DeviceBrand.CHROMECAST;
        if (containsAny(s, "shield", "nvidia")) return DeviceBrand.NVIDIA;
        if (s.contains("sonos")) return DeviceBrand.SONOS;
        if (s.contains("magenta") || s.contains("telekom")) return DeviceBrand.MAGENTA;
        if (s.contains("vodafone")) return DeviceBrand.VODAFONE;
        if (s.contains("sky")) return DeviceBrand.SKY;
        return DeviceBrand.HDMI;
    }

    private static boolean containsAny(String s, String... words) {
        for (String w : words) if (s.contains(w)) return true;
        return false;
    }
}

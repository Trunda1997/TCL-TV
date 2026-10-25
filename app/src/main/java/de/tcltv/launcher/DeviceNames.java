package de.tcltv.launcher;

import java.util.Locale;

/** Logos identify a brand; only explicit model evidence identifies a model. */
public final class DeviceNames {
    private DeviceNames() {}
    public static String displayName(DeviceBrand brand, String labels) {
        String s = labels.toLowerCase(Locale.ROOT);
        if (brand == DeviceBrand.PLAYSTATION) {
            if (s.contains("ps5") || s.contains("ps 5") || s.contains("playstation 5")) return "PlayStation 5";
            if (s.contains("ps4") || s.contains("ps 4") || s.contains("playstation 4")) return "PlayStation 4";
        }
        if (brand == DeviceBrand.XBOX) {
            if (s.contains("series s")) return "Xbox Series S";
            if (s.contains("series x")) return "Xbox Series X";
            if (s.contains("xbox one")) return "Xbox One";
            if (s.contains("xbox 360")) return "Xbox 360";
        }
        return brand.displayName;
    }
}

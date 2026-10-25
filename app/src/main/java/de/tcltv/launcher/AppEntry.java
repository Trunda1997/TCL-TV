package de.tcltv.launcher;

import android.graphics.drawable.Drawable;

public final class AppEntry {
    public final String name;
    public final String packageName;
    public final Drawable icon;

    public AppEntry(String name, String packageName, Drawable icon) {
        this.name = name;
        this.packageName = packageName;
        this.icon = icon;
    }
}

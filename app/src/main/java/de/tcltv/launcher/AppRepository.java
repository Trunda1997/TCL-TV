package de.tcltv.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AppRepository {
    // Explicit visibility also covers TCL firmware that omits launcher query results.
    public static final String[] KNOWN_TV_PACKAGES = {
        "com.google.android.youtube.tv", "com.netflix.ninja", "com.netflix.mediaclient",
        "com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus", "com.apple.atve.androidtv.appletv",
        "de.sky.online", "de.sky.bw", "de.sky.skygo", "de.rtli.tv", "de.rtli.now",
        "de.prosiebensat1digital.seventv", "de.exaring.waipu", "com.zattoo.player",
        "de.swr.avp.ard.tv", "de.swr.avp.ard", "com.zdf.android.mediathek",
        "com.zdf.android.mediathek.tv", "com.spotify.tv.android", "com.spotify.music"
    };
    private final Context context;

    public AppRepository(Context context) {
        this.context = context;
    }

    public List<AppEntry> installedTvApps() {
        PackageManager pm = context.getPackageManager();
        Map<String, AppEntry> unique = new LinkedHashMap<>();

        collect(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, unique);
        collect(pm, Intent.CATEGORY_LAUNCHER, unique);

        for (String pkg : KNOWN_TV_PACKAGES) addLaunchable(pm, pkg, unique);
        try {
            for (ApplicationInfo info : pm.getInstalledApplications(0)) addLaunchable(pm, info.packageName, unique);
        } catch (RuntimeException ignored) { /* Retain launcher-query results on restricted firmware. */ }

        List<AppEntry> out = new ArrayList<>(unique.values());
        out.sort(Comparator.comparing(a -> a.name.toLowerCase(Locale.ROOT)));
        return out;
    }

    private void addLaunchable(PackageManager pm, String pkg, Map<String, AppEntry> out) {
        if (pkg.equals(context.getPackageName()) || out.containsKey(pkg)) return;
        try {
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            if (!info.enabled) return;
            ResolveInfo resolved = null;
            for (boolean tv : new boolean[]{true, false}) {
                try {
                    Intent launch = tv ? pm.getLeanbackLaunchIntentForPackage(pkg) : pm.getLaunchIntentForPackage(pkg);
                    resolved = resolveLaunch(pm, launch, pkg, context.getPackageName());
                } catch (RuntimeException ignored) { }
                if (resolved != null) break;
            }
            if (resolved == null) return;
            android.graphics.drawable.Drawable icon;
            try { icon = resolved.loadIcon(pm); }
            catch (RuntimeException e) { icon = pm.getDefaultActivityIcon(); }
            CharSequence label = null;
            try { label = resolved.loadLabel(pm); } catch (RuntimeException ignored) { }
            out.put(pkg, new AppEntry(label == null || label.length() == 0 ? pkg : label.toString(), pkg, icon));
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) { }
    }

    // Same launch-target checks as TCL Home LaunchableApps.resolveLaunch.
    static ResolveInfo resolveLaunch(PackageManager pm, Intent intent, String pkg, String caller) {
        if (intent == null) return null;
        ResolveInfo resolved = pm.resolveActivity(intent, 0);
        ActivityInfo activity = resolved == null ? null : resolved.activityInfo;
        if (activity == null || !pkg.equals(activity.packageName) || !activity.exported
                || !activity.enabled || activity.applicationInfo == null || !activity.applicationInfo.enabled) return null;
        if (activity.permission != null && pm.checkPermission(activity.permission, caller)
                != PackageManager.PERMISSION_GRANTED) return null;
        return resolved;
    }

    private void collect(PackageManager pm, String category, Map<String, AppEntry> out) {
        Intent q = new Intent(Intent.ACTION_MAIN);
        q.addCategory(category);
        List<ResolveInfo> matches;
        try { matches = pm.queryIntentActivities(q, 0); }
        catch (RuntimeException e) { return; }
        for (ResolveInfo ri : matches) {
            if (ri.activityInfo == null || !ri.activityInfo.enabled || !ri.activityInfo.exported
                    || ri.activityInfo.applicationInfo == null || !ri.activityInfo.applicationInfo.enabled) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg == null || pkg.equals(context.getPackageName()) || out.containsKey(pkg)) continue;
            addLaunchable(pm, pkg, out);
        }
    }
}

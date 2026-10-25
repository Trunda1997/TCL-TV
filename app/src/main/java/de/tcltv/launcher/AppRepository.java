package de.tcltv.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private List<AppEntry> cached;
    private Runnable listener;
    private boolean registered, firstStart = true, loading, reloadRequested, pendingChanged, closed;
    private final BroadcastReceiver packagesChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent intent) {
            if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())
                    && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return;
            requestRefresh();
        }
    };

    public AppRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized List<AppEntry> installedTvApps() {
        if (cached == null) cached = discover();
        return new ArrayList<>(cached);
    }

    // As in TCL Home: background discovery + package notifications; UI reads the snapshot.
    public void start(Runnable changed) {
        listener = changed;
        if (pendingChanged) { pendingChanged = false; listener.run(); }
        if (!registered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            filter.addDataScheme("package");
            if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(packagesChanged, filter, Context.RECEIVER_NOT_EXPORTED);
            else context.registerReceiver(packagesChanged, filter);
            registered = true;
        }
        if (!firstStart || cached == null) requestRefresh();
        firstStart = false;
    }
    public void stop() {
        listener = null;
        if (registered) { context.unregisterReceiver(packagesChanged); registered = false; }
    }
    public void close() { stop(); closed = true; worker.shutdownNow(); main.removeCallbacksAndMessages(null); }
    private void requestRefresh() {
        if (closed) return;
        if (loading) { reloadRequested = true; return; }
        loading = true;
        worker.execute(() -> {
            List<AppEntry> next = discover();
            main.post(() -> {
                if (closed) return;
                boolean changed;
                synchronized (this) {
                    changed = !sameApps(cached, next);
                    cached = next;
                }
                loading = false;
                if (changed) {
                    if (listener != null) listener.run();
                    else pendingChanged = true;
                }
                if (reloadRequested) { reloadRequested = false; requestRefresh(); }
            });
        });
    }
    static boolean sameApps(List<AppEntry> a, List<AppEntry> b) {
        if (a == null || a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).packageName.equals(b.get(i).packageName) || !a.get(i).name.equals(b.get(i).name)) return false;
        }
        return true;
    }

    private List<AppEntry> discover() {
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

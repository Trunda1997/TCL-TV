package de.tcltv.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AppRepository {
    private final Context context;

    public AppRepository(Context context) {
        this.context = context;
    }

    public List<AppEntry> installedTvApps() {
        PackageManager pm = context.getPackageManager();
        Map<String, AppEntry> unique = new LinkedHashMap<>();

        collect(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, unique);
        collect(pm, Intent.CATEGORY_LAUNCHER, unique);

        List<AppEntry> out = new ArrayList<>(unique.values());
        out.sort(Comparator.comparing(a -> a.name.toLowerCase(Locale.ROOT)));
        return out;
    }

    private void collect(PackageManager pm, String category, Map<String, AppEntry> out) {
        Intent q = new Intent(Intent.ACTION_MAIN);
        q.addCategory(category);
        for (ResolveInfo ri : pm.queryIntentActivities(q, PackageManager.MATCH_ALL)) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg == null || pkg.equals(context.getPackageName()) || out.containsKey(pkg)) continue;
            try {
                String label = ri.loadLabel(pm) == null ? pkg : ri.loadLabel(pm).toString();
                out.put(pkg, new AppEntry(label, pkg, ri.loadIcon(pm)));
            } catch (Exception ignored) {}
        }
    }
}

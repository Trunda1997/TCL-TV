package de.tcltv.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AppRepository {
    private final Context context;

    public AppRepository(Context context) { this.context = context; }

    public List<AppEntry> installedTvApps() {
        PackageManager pm = context.getPackageManager();
        Intent q = new Intent(Intent.ACTION_MAIN);
        q.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<AppEntry> out = new ArrayList<>();
        for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
            if (ri.activityInfo == null || context.getPackageName().equals(ri.activityInfo.packageName)) continue;
            try {
                out.add(new AppEntry(ri.loadLabel(pm).toString(), ri.activityInfo.packageName, ri.loadIcon(pm)));
            } catch (Exception ignored) {}
        }
        out.sort(Comparator.comparing(a -> a.name.toLowerCase()));
        return out;
    }
}

package de.tcltv.launcher;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.tv.TvContract;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

public final class LauncherUtils {
    private LauncherUtils() {}

    public static void openSettings(Activity a) {
        try { a.startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        catch (Exception e) { Toast.makeText(a, "Einstellungen konnten nicht geöffnet werden.", Toast.LENGTH_SHORT).show(); }
    }

    public static void openPlayStore(Activity a, String query) {
        try {
            Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + Uri.encode(query == null ? "tv" : query)));
            market.setPackage("com.android.vending");
            a.startActivity(market);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(a, "Google Play Store wurde nicht gefunden.", Toast.LENGTH_SHORT).show();
        }
    }

    public static void openHdmi(Activity a, String inputId) {
        try {
            Uri uri = TvContract.buildChannelUriForPassthroughInput(inputId);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            a.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(a, "Eingang konnte nicht direkt geöffnet werden.", Toast.LENGTH_LONG).show();
        }
    }

    /** Opens an official provider URI only when the intended installed package accepts it. */
    public static boolean launchDeepLink(Activity a, String pkg, String uri) {
        if (pkg == null || pkg.isEmpty() || uri == null || uri.isEmpty()) return false;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(pkg);
            if (AppRepository.resolveLaunch(a.getPackageManager(), intent, pkg, a.getPackageName()) == null)
                return false;
            a.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (RuntimeException ignored) { return false; }
    }

    /** Uses an app's exported Android search activity; providers without one return false. */
    public static boolean searchPackage(Activity a, String pkg, String query) {
        if (pkg == null || pkg.isEmpty() || query == null || query.trim().isEmpty()) return false;
        try {
            Intent intent = new Intent(Intent.ACTION_SEARCH).setPackage(pkg)
                    .putExtra(SearchManager.QUERY, query.trim());
            if (AppRepository.resolveLaunch(a.getPackageManager(), intent, pkg, a.getPackageName()) == null)
                return false;
            a.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (RuntimeException ignored) { return false; }
    }

    public static boolean launchPackage(Activity a, String pkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        try {
            Intent leanback = a.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            if (AppRepository.resolveLaunch(a.getPackageManager(), leanback, pkg, a.getPackageName()) != null) {
                a.startActivity(leanback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
                return true;
            }
        } catch (RuntimeException ignored) { }
        try {
            Intent launcher = a.getPackageManager().getLaunchIntentForPackage(pkg);
            if (AppRepository.resolveLaunch(a.getPackageManager(), launcher, pkg, a.getPackageName()) != null) {
                a.startActivity(launcher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
                return true;
            }
        } catch (RuntimeException ignored) { }
        return false;
    }
}

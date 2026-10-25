package de.tcltv.launcher;

import android.app.Activity;
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

    public static boolean launchPackage(Activity a, String pkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        try {
            Intent i = a.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            if (i == null) i = a.getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) {
                a.startActivity(i);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
}

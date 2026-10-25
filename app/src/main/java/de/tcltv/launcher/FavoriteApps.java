package de.tcltv.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;

/** TCL Home's ordered and explicitly-empty favorites, migrated from TCL TV 0.1. */
public final class FavoriteApps {
    private final SharedPreferences prefs;
    private static final String KEY = "favorite_packages";
    public FavoriteApps(Context context) { prefs = context.getSharedPreferences("tcl_tv", Context.MODE_PRIVATE); }

    public List<String> packages() {
        Set<String> unique = new LinkedHashSet<>();
        try {
            JSONArray saved = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < saved.length(); i++) {
                Object value = saved.opt(i);
                if (value instanceof String && !((String) value).isEmpty()) unique.add((String) value);
            }
        } catch (JSONException ignored) { }
        return new ArrayList<>(unique);
    }
    public boolean contains(String pkg) { return packages().contains(pkg); }
    public void initialize(List<AppEntry> apps) {
        if (prefs.contains(KEY)) return;
        if (prefs.getBoolean("favorites_edited", false)) {
            save(new ArrayList<>(prefs.getStringSet("favorites", new LinkedHashSet<>())));
            return;
        }
        if (apps.isEmpty()) return;
        List<String> defaults = new ArrayList<>();
        for (AppEntry app : apps) {
            String key = (app.name + " " + app.packageName).toLowerCase(Locale.ROOT);
            for (String provider : new String[]{"youtube", "netflix", "amazon", "prime", "disney", "appletv", "apple tv", "sky", "wow", "rtl", "joyn", "seventv", "waipu", "exaring", "zattoo", "ard", "zdf", "spotify"}) {
                if (key.contains(provider)) { defaults.add(app.packageName); break; }
            }
        }
        if (defaults.isEmpty()) {
            for (AppEntry app : apps) {
                if (app.packageName.toLowerCase(Locale.ROOT).contains("settings")) continue;
                defaults.add(app.packageName);
                if (defaults.size() == 4) break;
            }
        }
        save(defaults);
    }
    public boolean toggle(String pkg) {
        List<String> values = packages();
        boolean added = !values.remove(pkg);
        if (added) values.add(pkg);
        save(values);
        return added;
    }
    private void save(List<String> values) {
        prefs.edit().putString(KEY, new JSONArray(new LinkedHashSet<>(values)).toString())
                .putBoolean("favorites_edited", true).apply();
    }
}

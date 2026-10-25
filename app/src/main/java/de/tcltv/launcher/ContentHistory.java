package de.tcltv.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Local history only: records content intentionally opened from TCL TV. */
public final class ContentHistory {
    private static final int LIMIT = 12;
    private final SharedPreferences prefs;

    public ContentHistory(Context context) {
        prefs = context.getSharedPreferences("content_history", Context.MODE_PRIVATE);
    }

    public void record(String section, ContentItem item) {
        if (section == null || item == null) return;
        Map<String, ContentItem> ordered = new LinkedHashMap<>();
        ordered.put(key(item), item);
        for (ContentItem previous : list(section)) ordered.putIfAbsent(key(previous), previous);
        JSONArray array = new JSONArray();
        int count = 0;
        for (ContentItem value : ordered.values()) {
            if (count++ == LIMIT) break;
            JSONObject entry = new JSONObject();
            try {
                entry.put("title", value.title).put("provider", value.provider)
                        .put("progress", value.progress);
                if (value.deepLink != null) entry.put("uri", value.deepLink);
                array.put(entry);
            } catch (JSONException ignored) { }
        }
        prefs.edit().putString(section, array.toString()).apply();
    }

    public List<ContentItem> list(String section) {
        List<ContentItem> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(section, "[]"));
            for (int i = 0; i < array.length() && result.size() < LIMIT; i++) {
                JSONObject entry = array.optJSONObject(i);
                if (entry == null) continue;
                String title = entry.optString("title", "").trim();
                String provider = entry.optString("provider", "").trim();
                if (title.isEmpty() || provider.isEmpty()) continue;
                String uri = entry.optString("uri", null);
                result.add(new ContentItem(title, provider, entry.optInt("progress", 0), uri));
            }
        } catch (JSONException ignored) { }
        return result;
    }

    private String key(ContentItem item) {
        return (item.provider + "\n" + item.title).toLowerCase(java.util.Locale.ROOT);
    }
}

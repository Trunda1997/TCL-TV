package de.tcltv.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;

/** Caches descriptors separately from noisy TCL connection-state callbacks. */
public final class HdmiRepository {
    public interface Listener { void onChanged(); }
    private final Context context;
    private final TvInputManager manager;
    private final SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Integer> states = new HashMap<>();
    private List<HdmiInput> cached = new ArrayList<>();
    private Listener listener;
    private boolean registered;
    private boolean loaded;
    private final Runnable refresh = this::reload;

    private final TvInputManager.TvInputCallback callback = new TvInputManager.TvInputCallback() {
        @Override public void onInputAdded(String id) { scheduleRefresh(); }
        @Override public void onInputRemoved(String id) { states.remove(id); scheduleRefresh(); }
        @Override public void onInputUpdated(String id) { scheduleRefresh(); }
        @Override public void onInputStateChanged(String id, int state) {
            states.put(id, state); // Connection changes never rebuild the device rail.
        }
    };

    public HdmiRepository(Context context) {
        this.context = context.getApplicationContext();
        manager = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
        prefs = context.getSharedPreferences("hdmi_overrides", Context.MODE_PRIVATE);
    }

    public void start(Listener l) {
        listener = l;
        if (manager != null && !registered) {
            try {
                manager.registerCallback(callback, handler);
                registered = true;
            } catch (SecurityException e) { Log.w("TclHdmi", "TV input callbacks unavailable", e); }
        }
        reload(); // Reconcile changes while another app was open, preserving existing views.
    }

    public void stop() {
        handler.removeCallbacks(refresh);
        listener = null;
        if (manager != null && registered) {
            try { manager.unregisterCallback(callback); }
            catch (RuntimeException e) { Log.w("TclHdmi", "Cannot unregister TV inputs", e); }
            registered = false;
        }
    }

    public List<HdmiInput> list() {
        if (!loaded) reload();
        List<HdmiInput> result = new ArrayList<>();
        for (HdmiInput input : cached) result.add(new HdmiInput(input.id, input.rawLabel,
                input.displayName, states.getOrDefault(input.id, input.state), input.brand));
        return result;
    }

    private void reload() {
        List<HdmiInput> next = new ArrayList<>();
        if (manager != null) {
            try {
                List<TvInputInfo> inputs = manager.getTvInputList();
                Set<String> parentsWithDevices = new HashSet<>();
                for (TvInputInfo info : inputs) {
                    if (info.getType() == TvInputInfo.TYPE_HDMI && info.getParentId() != null)
                        parentsWithDevices.add(info.getParentId());
                }
                for (TvInputInfo info : inputs) {
                    if (info.getType() != TvInputInfo.TYPE_HDMI || parentsWithDevices.contains(info.getId())) continue;
                    String custom = label(info, true), normal = label(info, false);
                    String raw = !custom.isEmpty() ? custom : !normal.isEmpty() ? normal : info.getId();
                    // Vendor extras may expose a CEC display name; no hidden APIs or privileged permission.
                    String device = deviceMetadata(info.getExtras());
                    DeviceBrand brand = LogoResolver.resolve(custom + " " + normal + " " + device + " " + info.getId());
                    String saved = prefs.getString("brand:" + info.getId(), null);
                    if (saved != null) {
                        try { brand = DeviceBrand.valueOf(saved); } catch (IllegalArgumentException ignored) { }
                    }
                    String name = prefs.getString("name:" + info.getId(), "").trim();
                    if (name.isEmpty()) name = brand != DeviceBrand.HDMI
                            ? DeviceNames.displayName(brand, custom + " " + normal + " " + device)
                            : safeName(!custom.isEmpty() ? custom : !device.isEmpty() ? device : normal);
                    int state = states.getOrDefault(info.getId(), TvInputManager.INPUT_STATE_DISCONNECTED);
                    try { state = manager.getInputState(info.getId()); } catch (IllegalArgumentException ignored) { }
                    states.put(info.getId(), state);
                    next.add(new HdmiInput(info.getId(), raw, safeName(name), state, brand));
                }
            } catch (RuntimeException e) {
                Log.w("TclHdmi", "Keeping cached TV inputs after query failure", e);
                return;
            }
        }
        next.sort(Comparator.comparing(input -> input.id)); // Labels never reorder focus positions.
        boolean changed = !sameDescriptors(cached, next);
        cached = next;
        loaded = true;
        if (changed && listener != null) listener.onChanged();
    }

    static boolean sameDescriptors(List<HdmiInput> a, List<HdmiInput> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            HdmiInput x = a.get(i), y = b.get(i);
            if (!x.id.equals(y.id) || !x.displayName.equals(y.displayName) || x.brand != y.brand) return false;
        }
        return true;
    }

    private String label(TvInputInfo info, boolean custom) {
        try {
            CharSequence value = custom ? info.loadCustomLabel(context) : info.loadLabel(context);
            return value == null ? "" : value.toString().trim();
        } catch (RuntimeException e) { return ""; }
    }

    private String deviceMetadata(Bundle extras) {
        if (extras == null) return "";
        StringBuilder value = new StringBuilder();
        for (String key : new String[]{"device_name", "display_name", "cec_device_name", "manufacturer", "brand"}) {
            Object item = extras.get(key);
            if (item instanceof String) value.append(item).append(' ');
        }
        return value.toString().trim();
    }

    private String safeName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty() || name.matches("(?i).*hdmi[\\s_-]*[0-9]*.*") || name.contains("/")) return "Externes Gerät";
        return name;
    }

    public void setOverride(String id, DeviceBrand brand) {
        prefs.edit().putString("brand:" + id, brand.name()).apply();
        scheduleRefresh();
    }
    public void setNameOverride(String id, String name) {
        prefs.edit().putString("name:" + id, name).apply();
        scheduleRefresh();
    }
    public void clearOverrides(String id) {
        prefs.edit().remove("brand:" + id).remove("name:" + id).apply();
        scheduleRefresh();
    }
    private void scheduleRefresh() {
        handler.removeCallbacks(refresh);
        if (listener != null) handler.postDelayed(refresh, 250);
        else loaded = false;
    }
}

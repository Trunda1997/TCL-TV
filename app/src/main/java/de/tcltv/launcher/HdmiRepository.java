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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * TvInputManager/custom-label/override foundation shared with TCL Home. Unlike
 * the reference's full rebuild on every callback, signal state is independent
 * from descriptors. Recognized CEC devices survive disappearing standby inputs.
 */
public final class HdmiRepository {
    public interface Listener { void onChanged(); }
    interface InputSource { List<InputDescriptor> list(); }

    /** Public Android input metadata, also an injectable boundary for firmware regression tests. */
    static final class InputDescriptor {
        final String id, parentId, custom, label, metadata;
        final int state;
        InputDescriptor(String id, String parentId, String custom, String label, String metadata, int state) {
            this.id = id;
            this.parentId = parentId == null || parentId.isEmpty() ? null : parentId;
            this.custom = custom == null ? "" : custom.trim();
            this.label = label == null ? "" : label.trim();
            this.metadata = metadata == null ? "" : metadata.trim();
            this.state = state;
        }
        String anchor() { return parentId == null ? id : parentId; }
        String evidence() { return custom + " " + label + " " + metadata; }
        String raw() { return !custom.isEmpty() ? custom : !label.isEmpty() ? label : id; }
        DeviceBrand brand() { return LogoResolver.resolve(evidence() + " " + id); }
        String name() {
            DeviceBrand brand = brand();
            return brand != DeviceBrand.HDMI ? DeviceNames.displayName(brand, evidence())
                    : safeName(!custom.isEmpty() ? custom : !metadata.isEmpty() ? metadata : label);
        }
        boolean recognized() { return brand() != DeviceBrand.HDMI || !"Externes Gerät".equals(name()); }
    }

    private static final class KnownDevice {
        final String id, sourceId, anchorId, raw, name;
        final DeviceBrand brand;
        KnownDevice(String id, String sourceId, String anchorId, String raw, String name, DeviceBrand brand) {
            this.id = id;
            this.sourceId = sourceId;
            this.anchorId = anchorId;
            this.raw = raw;
            this.name = name;
            this.brand = brand;
        }
    }

    private final Context context;
    private final TvInputManager manager;
    private final InputSource source;
    private final SharedPreferences prefs;
    private final SharedPreferences devices;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Integer> states = new HashMap<>();
    private final Map<String, KnownDevice> known = new LinkedHashMap<>();
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
            states.put(id, state); // A video-signal change never removes a remembered device.
        }
    };

    public HdmiRepository(Context context) { this(context, null); }

    HdmiRepository(Context context, InputSource source) {
        this.context = context.getApplicationContext();
        manager = source == null ? (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE) : null;
        this.source = source == null ? this::readSystemInputs : source;
        prefs = context.getSharedPreferences("hdmi_overrides", Context.MODE_PRIVATE);
        devices = context.getSharedPreferences("hdmi_known_devices", Context.MODE_PRIVATE);
        loadKnownDevices();
        for (KnownDevice device : known.values()) cached.add(remembered(device, null));
    }

    public void start(Listener l) {
        listener = l;
        if (manager != null && !registered) {
            try { manager.registerCallback(callback, handler); registered = true; }
            catch (RuntimeException e) { Log.w("TclHdmi", "TV input callbacks unavailable", e); }
        }
        reload();
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
        for (HdmiInput input : cached) result.add(new HdmiInput(input.id, input.openId, input.parentId,
                input.rawLabel, input.displayName, states.getOrDefault(input.openId, input.state),
                input.brand, input.remembered));
        return result;
    }

    private List<InputDescriptor> readSystemInputs() {
        List<InputDescriptor> result = new ArrayList<>();
        if (manager == null) return result;
        for (TvInputInfo info : manager.getTvInputList()) {
            if (info.getType() != TvInputInfo.TYPE_HDMI) continue;
            int state = states.getOrDefault(info.getId(), TvInputManager.INPUT_STATE_DISCONNECTED);
            try { state = manager.getInputState(info.getId()); } catch (IllegalArgumentException ignored) { }
            result.add(new InputDescriptor(info.getId(), info.getParentId(), label(info, true),
                    label(info, false), deviceMetadata(info.getExtras()), state));
        }
        return result;
    }

    private void reload() {
        List<InputDescriptor> inputs;
        try { inputs = source.list(); }
        catch (RuntimeException e) {
            Log.w("TclHdmi", "Keeping remembered TV devices after query failure", e);
            return;
        }
        List<HdmiInput> next = reconcile(inputs);
        next.sort(Comparator.comparing(input -> input.id));
        boolean changed = !sameDescriptors(cached, next);
        cached = next;
        loaded = true;
        saveKnownDevices();
        if (changed && listener != null) listener.onChanged();
    }

    private List<HdmiInput> reconcile(List<InputDescriptor> inputs) {
        Map<String, InputDescriptor> byId = new HashMap<>();
        Set<String> parentsWithChildren = new HashSet<>();
        for (InputDescriptor input : inputs) {
            byId.put(input.id, input);
            states.put(input.id, input.state);
            if (input.parentId != null) parentsWithChildren.add(input.parentId);
        }
        List<HdmiInput> result = new ArrayList<>();
        Set<String> usedKnown = new HashSet<>();
        Set<String> replacedAnchors = new HashSet<>();
        for (InputDescriptor input : inputs) {
            if (parentsWithChildren.contains(input.id)) continue;
            List<KnownDevice> onPort = onPort(input.anchor());
            KnownDevice match = findKnown(input, onPort);
            // A bare physical input after CEC disappears is standby evidence, not a new device.
            if (input.parentId == null && !input.recognized() && !onPort.isEmpty()) {
                for (KnownDevice previous : onPort) {
                    result.add(withOverrides(remembered(previous, input), previous.id));
                    usedKnown.add(previous.id);
                }
                continue;
            }
            String stableId = match == null ? input.id : match.id;
            String name = input.recognized() || match == null ? input.name() : match.name;
            DeviceBrand brand = input.recognized() || match == null ? input.brand() : match.brand;
            HdmiInput current = new HdmiInput(stableId, input.id, input.parentId, input.raw(),
                    name, input.state, brand, match != null && !input.recognized());
            current = withOverrides(current, stableId);
            result.add(current);
            if (match != null) usedKnown.add(match.id);
            boolean trustworthy = current.brand != DeviceBrand.HDMI || !"Externes Gerät".equals(current.displayName);
            if (trustworthy) {
                if (match == null && input.recognized() && !onPort.isEmpty()) replacedAnchors.add(input.anchor());
                // Keep all non-privileged label/CEC evidence so that clearing a
                // manual override can reconstruct the automatic name after standby.
                String evidence = input.evidence().trim();
                known.put(stableId, new KnownDevice(stableId, input.id, input.anchor(),
                        evidence.isEmpty() ? input.raw() : evidence, current.displayName, current.brand));
                usedKnown.add(stableId);
            }
        }
        List<String> remove = new ArrayList<>();
        for (KnownDevice previous : known.values()) {
            if (usedKnown.contains(previous.id)) continue;
            // A different positively identified device on this port supersedes stale remembered hardware.
            if (replacedAnchors.contains(previous.anchorId)) { remove.add(previous.id); continue; }
            InputDescriptor parent = byId.get(previous.anchorId);
            result.add(withOverrides(remembered(previous, parent), previous.id));
        }
        for (String id : remove) known.remove(id);
        return result;
    }

    private List<KnownDevice> onPort(String anchor) {
        List<KnownDevice> result = new ArrayList<>();
        for (KnownDevice value : known.values()) if (value.anchorId.equals(anchor)) result.add(value);
        return result;
    }

    private KnownDevice findKnown(InputDescriptor input, List<KnownDevice> candidates) {
        for (KnownDevice value : known.values())
            if (value.id.equals(input.id) || value.sourceId.equals(input.id)) return value;
        for (KnownDevice value : candidates)
            if (value.brand == input.brand() && value.name.equalsIgnoreCase(input.name())) return value;
        if (!input.recognized() && candidates.size() == 1) return candidates.get(0);
        return null;
    }

    private HdmiInput remembered(KnownDevice value, InputDescriptor parent) {
        int state = parent == null ? TvInputManager.INPUT_STATE_DISCONNECTED : parent.state;
        return new HdmiInput(value.id, value.anchorId, value.anchorId.equals(value.id) ? null : value.anchorId,
                value.raw, value.name, state, value.brand, true);
    }

    private HdmiInput withOverrides(HdmiInput input, String stableId) {
        DeviceBrand brand = input.brand;
        String savedBrand = prefs.getString("brand:" + stableId,
                prefs.getString("brand:" + input.openId, null));
        if (savedBrand != null) {
            try { brand = DeviceBrand.valueOf(savedBrand); } catch (IllegalArgumentException ignored) { }
        }
        String name = prefs.getString("name:" + stableId,
                prefs.getString("name:" + input.openId, "")).trim();
        if (name.isEmpty()) name = savedBrand == null || brand == input.brand
                ? input.displayName : DeviceNames.displayName(brand, input.rawLabel);
        return new HdmiInput(input.id, input.openId, input.parentId, input.rawLabel,
                safeName(name), input.state, brand, input.remembered);
    }

    static boolean sameDescriptors(List<HdmiInput> a, List<HdmiInput> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            HdmiInput x = a.get(i), y = b.get(i);
            if (!x.id.equals(y.id) || !x.openId.equals(y.openId)
                    || !x.displayName.equals(y.displayName) || x.brand != y.brand) return false;
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
        try {
            for (String key : new String[]{"device_name", "display_name", "cec_device_name", "manufacturer", "brand"}) {
                Object item = extras.get(key);
                if (item instanceof String) value.append(item).append(' ');
            }
        } catch (RuntimeException ignored) { }
        return value.toString().trim();
    }

    static String safeName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty() || name.matches("(?i).*hdmi[\\s_-]*[0-9]*.*")
                || name.matches("(?i)(input|source|port|eingang)[\\s_-]*[0-9]*") || name.contains("/")) return "Externes Gerät";
        return name;
    }

    private void loadKnownDevices() {
        try {
            JSONArray entries = new JSONArray(devices.getString("entries", "[]"));
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                String id = entry.getString("id"), anchor = entry.getString("anchor");
                if (id.isEmpty() || anchor.isEmpty()) continue;
                DeviceBrand brand = DeviceBrand.valueOf(entry.getString("brand"));
                String name = safeName(entry.getString("name"));
                if (brand == DeviceBrand.HDMI && name.equals("Externes Gerät")) continue;
                known.put(id, new KnownDevice(id, entry.optString("source", id), anchor,
                        entry.optString("raw", name), name, brand));
            }
        } catch (JSONException | IllegalArgumentException e) { Log.w("TclHdmi", "Ignoring invalid saved device data", e); }
    }

    private void saveKnownDevices() {
        JSONArray entries = new JSONArray();
        try {
            for (KnownDevice value : known.values()) {
                JSONObject entry = new JSONObject();
                entry.put("id", value.id).put("source", value.sourceId).put("anchor", value.anchorId)
                        .put("raw", value.raw).put("name", value.name).put("brand", value.brand.name());
                entries.put(entry);
            }
            String encoded = entries.toString();
            if (!encoded.equals(devices.getString("entries", "[]"))) devices.edit().putString("entries", encoded).apply();
        } catch (JSONException e) { Log.w("TclHdmi", "Cannot save known devices", e); }
    }

    public void setOverride(String id, DeviceBrand brand) {
        prefs.edit().putString("brand:" + id, brand.name()).apply();
        scheduleRefresh();
    }
    public void setNameOverride(String id, String name) {
        prefs.edit().putString("name:" + id, name).apply();
        scheduleRefresh();
    }
    /** Restore automatic label/brand detection without deleting the standby cache. */
    public void clearOverrides(String id) {
        KnownDevice device = known.get(id);
        SharedPreferences.Editor edit = prefs.edit().remove("brand:" + id).remove("name:" + id);
        if (device != null && !device.sourceId.equals(id))
            edit.remove("brand:" + device.sourceId).remove("name:" + device.sourceId);
        edit.apply();
        if (device != null) {
            DeviceBrand automaticBrand = LogoResolver.resolve(device.raw + " " + device.sourceId + " " + device.anchorId);
            String automaticName = automaticBrand == DeviceBrand.HDMI
                    ? safeName(device.raw) : DeviceNames.displayName(automaticBrand, device.raw);
            if (automaticBrand == DeviceBrand.HDMI && "Externes Gerät".equals(automaticName)) {
                known.remove(id);
            } else {
                known.put(id, new KnownDevice(id, device.sourceId, device.anchorId,
                        device.raw, automaticName, automaticBrand));
            }
        }
        saveKnownDevices();
        scheduleRefresh();
    }

    /** Forget a stale standby mapping; active, positively identified hardware can be rediscovered. */
    public void forgetKnownDevice(String id) {
        KnownDevice device = known.remove(id);
        SharedPreferences.Editor edit = prefs.edit().remove("brand:" + id).remove("name:" + id);
        if (device != null && !device.sourceId.equals(id))
            edit.remove("brand:" + device.sourceId).remove("name:" + device.sourceId);
        edit.apply();
        saveKnownDevices();
        scheduleRefresh();
    }
    private void scheduleRefresh() {
        handler.removeCallbacks(refresh);
        if (listener != null) handler.postDelayed(refresh, 250);
        else loaded = false;
    }
}

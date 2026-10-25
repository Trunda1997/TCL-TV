package de.tcltv.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HdmiRepository {
    public interface Listener { void onChanged(); }

    private final Context context;
    private final TvInputManager manager;
    private final SharedPreferences prefs;
    private final PackageManager pm;
    private Listener listener;

    private final TvInputManager.TvInputCallback callback = new TvInputManager.TvInputCallback() {
        @Override public void onInputAdded(String inputId) { notifyChanged(); }
        @Override public void onInputRemoved(String inputId) { notifyChanged(); }
        @Override public void onInputUpdated(String inputId) { notifyChanged(); }
        @Override public void onInputStateChanged(String inputId, int state) { notifyChanged(); }
    };

    public HdmiRepository(Context context) {
        this.context = context;
        manager = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
        prefs = context.getSharedPreferences("hdmi_overrides", Context.MODE_PRIVATE);
        pm = context.getPackageManager();
    }

    public void start(Listener l) {
        listener = l;
        if (manager != null) manager.registerCallback(callback, new Handler(Looper.getMainLooper()));
    }

    public void stop() {
        if (manager != null) manager.unregisterCallback(callback);
        listener = null;
    }

    public List<HdmiInput> list() {
        List<HdmiInput> out = new ArrayList<>();
        if (manager == null) return out;
        for (TvInputInfo info : manager.getTvInputList()) {
            CharSequence custom = null, normal = null;
            try { custom = info.loadCustomLabel(context); } catch (Exception ignored) {}
            try { normal = info.loadLabel(pm); } catch (Exception ignored) {}
            String raw = custom != null ? custom.toString() : normal != null ? normal.toString() : info.getId();
            boolean hdmi = info.getType() == TvInputInfo.TYPE_HDMI;
            boolean looksHdmi = raw.toLowerCase().contains("hdmi") || info.getId().toLowerCase().contains("hdmi");
            if (!hdmi && !looksHdmi) continue;

            DeviceBrand brand = LogoResolver.resolve(raw + " " + info.getId());
            String savedBrand = prefs.getString("brand:" + info.getId(), null);
            if (savedBrand != null) {
                try { brand = DeviceBrand.valueOf(savedBrand); } catch (Exception ignored) {}
            }
            String customName = prefs.getString("name:" + info.getId(), null);
            String displayName = customName != null && !customName.trim().isEmpty()
                    ? customName.trim()
                    : brand != DeviceBrand.HDMI ? brand.displayName : sanitizeUnknown(raw);
            out.add(new HdmiInput(info.getId(), raw, displayName, manager.getInputState(info.getId()), brand));
        }
        out.sort(Comparator.comparing(a -> a.rawLabel));
        return out;
    }

    private String sanitizeUnknown(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.matches("(?i).*hdmi\\s*\\d+.*") || s.equalsIgnoreCase("HDMI")) return "Externes Gerät";
        return s.isEmpty() ? "Externes Gerät" : s;
    }

    public void setOverride(String inputId, DeviceBrand brand) {
        prefs.edit().putString("brand:" + inputId, brand.name()).apply();
        notifyChanged();
    }

    public void setNameOverride(String inputId, String name) {
        prefs.edit().putString("name:" + inputId, name).apply();
        notifyChanged();
    }

    public void clearOverrides(String inputId) {
        prefs.edit().remove("brand:" + inputId).remove("name:" + inputId).apply();
        notifyChanged();
    }

    private void notifyChanged() {
        if (listener != null) listener.onChanged();
    }
}

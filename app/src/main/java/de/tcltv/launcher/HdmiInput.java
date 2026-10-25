package de.tcltv.launcher;

/** Stable card identity and currently usable passthrough input are deliberately separate. */
public final class HdmiInput {
    public final String id;
    public final String openId;
    public final String parentId;
    public final String rawLabel;
    public final String displayName;
    public final int state;
    public final DeviceBrand brand;
    public final boolean remembered;

    public HdmiInput(String id, String rawLabel, String displayName, int state, DeviceBrand brand) {
        this(id, id, null, rawLabel, displayName, state, brand, false);
    }

    public HdmiInput(String id, String openId, String parentId, String rawLabel,
            String displayName, int state, DeviceBrand brand, boolean remembered) {
        this.id = id;
        this.openId = openId;
        this.parentId = parentId;
        this.rawLabel = rawLabel;
        this.displayName = displayName;
        this.state = state;
        this.brand = brand;
        this.remembered = remembered;
    }
}

package de.tcltv.launcher;

public final class HdmiInput {
    public final String id;
    public final String rawLabel;
    public final String displayName;
    public final int state;
    public final DeviceBrand brand;

    public HdmiInput(String id, String rawLabel, String displayName, int state, DeviceBrand brand) {
        this.id = id;
        this.rawLabel = rawLabel;
        this.displayName = displayName;
        this.state = state;
        this.brand = brand;
    }
}

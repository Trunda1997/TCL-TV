package de.tcltv.launcher;

public enum DeviceBrand {
    HDMI("Externes Gerät", "HDMI"),
    PLAYSTATION("PlayStation 5", "PS"),
    XBOX("Xbox Series X", "XBOX"),
    SWITCH2("Nintendo Switch 2", "SWITCH"),
    NINTENDO("Nintendo Switch", "SWITCH"),
    APPLE_TV("Apple TV", "tv"),
    FIRE_TV("Amazon Fire TV", "fire tv"),
    CHROMECAST("Google TV", "Google TV"),
    NVIDIA("NVIDIA Shield", "NVIDIA"),
    SONOS("Sonos", "SONOS"),
    SKY("Sky", "sky"),
    MAGENTA("MagentaTV", "MAGENTA"),
    VODAFONE("Vodafone TV", "vodafone");

    public final String displayName;
    public final String logoText;

    DeviceBrand(String displayName, String logoText) {
        this.displayName = displayName;
        this.logoText = logoText;
    }
}

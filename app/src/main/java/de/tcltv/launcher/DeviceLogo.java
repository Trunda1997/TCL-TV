package de.tcltv.launcher;

/** Monochrome device logos independent of the fonts installed on TCL firmware. */
public final class DeviceLogo {
    private DeviceLogo() {}
    public static int resource(DeviceBrand brand) {
        switch (brand) {
            case PLAYSTATION: return R.drawable.logo_playstation;
            case SWITCH2:
            case NINTENDO: return R.drawable.logo_nintendoswitch;
            case XBOX: return R.drawable.logo_xbox;
            case APPLE_TV: return R.drawable.logo_appletv;
            case SONOS: return R.drawable.logo_sonos;
            default: return R.drawable.logo_device;
        }
    }
}

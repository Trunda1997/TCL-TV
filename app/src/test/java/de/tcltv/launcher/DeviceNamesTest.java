package de.tcltv.launcher;

import org.junit.Test;
import static org.junit.Assert.*;

public class DeviceNamesTest {
    @Test public void modelsRequireEvidence() {
        assertEquals("Xbox Series S", DeviceNames.displayName(LogoResolver.resolve("Xbox Series S"), "Xbox Series S"));
        assertEquals("Xbox", DeviceNames.displayName(LogoResolver.resolve("Xbox"), "Xbox"));
        assertEquals("PlayStation", DeviceNames.displayName(LogoResolver.resolve("PlayStation"), "PlayStation"));
        assertEquals("PlayStation 5", DeviceNames.displayName(LogoResolver.resolve("PS5"), "PS5"));
        assertEquals(DeviceBrand.SWITCH2, LogoResolver.resolve("Nintendo Switch 2"));
        assertEquals(DeviceBrand.SONOS, LogoResolver.resolve("Sonos Arc"));
    }
}

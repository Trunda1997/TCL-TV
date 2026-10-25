package de.tcltv.launcher;

import android.content.Context;
import android.media.tv.TvInputManager;
import android.os.Looper;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class HdmiStandbyTest {
    private Context context;
    private final List<HdmiRepository.InputDescriptor> reported = new ArrayList<>();
    private final AtomicInteger reads = new AtomicInteger();

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("hdmi_overrides", Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences("hdmi_known_devices", Context.MODE_PRIVATE).edit().clear().commit();
    }

    private HdmiRepository repository() {
        return new HdmiRepository(context, () -> {
            reads.incrementAndGet();
            return new ArrayList<>(reported);
        });
    }

    private HdmiRepository.InputDescriptor parent(int state) {
        return new HdmiRepository.InputDescriptor("physical-port", null, "", "HDMI 2", "", state);
    }

    private HdmiRepository.InputDescriptor device(String id, String label) {
        return new HdmiRepository.InputDescriptor(id, "physical-port", "", label, "", TvInputManager.INPUT_STATE_CONNECTED);
    }

    private TvInputManager.TvInputCallback callback(HdmiRepository repository) throws Exception {
        Field field = HdmiRepository.class.getDeclaredField("callback");
        field.setAccessible(true);
        return (TvInputManager.TvInputCallback) field.get(repository);
    }

    @Test public void ps5SurvivesStandbyAndProcessRestartWithParentPassthrough() {
        reported.add(parent(TvInputManager.INPUT_STATE_CONNECTED));
        reported.add(device("cec-ps5", "PlayStation 5"));
        HdmiRepository repository = repository();
        repository.start(() -> {});
        List<HdmiInput> active = repository.list();
        assertEquals(1, active.size());
        assertEquals("cec-ps5", active.get(0).id);
        assertEquals("cec-ps5", active.get(0).openId);
        assertEquals("PlayStation 5", active.get(0).displayName);
        repository.stop();

        reported.clear();
        reported.add(parent(TvInputManager.INPUT_STATE_CONNECTED_STANDBY));
        HdmiRepository restarted = repository();
        restarted.start(() -> {});
        HdmiInput standby = restarted.list().get(0);
        assertEquals(1, restarted.list().size());
        assertEquals("Stable card/focus identity survives CEC disappearance", "cec-ps5", standby.id);
        assertEquals("physical-port", standby.openId);
        assertEquals(DeviceBrand.PLAYSTATION, standby.brand);
        assertEquals("PlayStation 5", standby.displayName);
        assertTrue(standby.remembered);
        assertEquals(TvInputManager.INPUT_STATE_CONNECTED_STANDBY, standby.state);
        restarted.stop();
    }

    @Test public void wakeWithChangedCecIdKeepsStableCardAndUsesCurrentInput() {
        reported.add(parent(0));
        reported.add(device("cec-old", "PS5"));
        HdmiRepository repository = repository();
        repository.start(() -> {});
        reported.set(1, device("cec-new", "PlayStation 5"));
        repository.start(() -> {});
        assertEquals(1, repository.list().size());
        assertEquals("cec-old", repository.list().get(0).id);
        assertEquals("cec-new", repository.list().get(0).openId);
        assertFalse(repository.list().get(0).remembered);
        repository.stop();
    }

    @Test public void positivelyIdentifiedReplacementDoesNotKeepOldDeviceAsGhost() {
        reported.add(parent(0));
        reported.add(device("cec-ps5", "PS5"));
        HdmiRepository repository = repository();
        repository.start(() -> {});
        reported.set(1, device("cec-xbox", "Xbox Series S"));
        repository.start(() -> {});
        List<HdmiInput> replaced = repository.list();
        assertEquals(1, replaced.size());
        assertEquals("cec-xbox", replaced.get(0).id);
        assertEquals("Xbox Series S", replaced.get(0).displayName);
        repository.stop();
        reported.clear();
        reported.add(parent(2));
        HdmiRepository restarted = repository();
        restarted.start(() -> {});
        assertEquals("Xbox Series S", restarted.list().get(0).displayName);
        assertEquals(1, restarted.list().size());
        restarted.stop();
    }

    @Test public void stateStormDoesNotReadMetadataPersistOrNotifyRows() throws Exception {
        reported.add(parent(0));
        reported.add(device("cec-ps5", "PS5"));
        AtomicInteger renders = new AtomicInteger();
        HdmiRepository repository = repository();
        repository.start(renders::incrementAndGet);
        int initialReads = reads.get(), initialRenders = renders.get();
        String persisted = context.getSharedPreferences("hdmi_known_devices", 0).getString("entries", "");
        TvInputManager.TvInputCallback callback = callback(repository);
        for (int i = 0; i < 100; i++) callback.onInputStateChanged("cec-ps5", i % 3);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        assertEquals(initialReads, reads.get());
        assertEquals(initialRenders, renders.get());
        assertEquals(persisted, context.getSharedPreferences("hdmi_known_devices", 0).getString("entries", ""));
        assertEquals(1, repository.list().size());
        repository.stop();
    }

    @Test public void structuralStormIsDebouncedAndUnchangedDescriptorsDoNotRender() throws Exception {
        reported.add(parent(0));
        reported.add(device("cec-ps5", "PS5"));
        AtomicInteger renders = new AtomicInteger();
        HdmiRepository repository = repository();
        repository.start(renders::incrementAndGet);
        int initialRenders = renders.get();
        TvInputManager.TvInputCallback callback = callback(repository);
        for (int i = 0; i < 30; i++) callback.onInputUpdated("cec-ps5");
        assertEquals(1, reads.get());
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(251));
        assertEquals(2, reads.get());
        assertEquals(initialRenders, renders.get());
        repository.stop();
    }

    @Test public void forgetRemovesStandbyAssignmentButNotPhysicalInput() {
        reported.add(parent(0));
        reported.add(device("cec-ps5", "PS5"));
        HdmiRepository repository = repository();
        repository.start(() -> {});
        reported.clear();
        reported.add(parent(2));
        repository.start(() -> {});
        repository.forgetKnownDevice("cec-ps5");
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(251));
        assertEquals(1, repository.list().size());
        assertEquals("physical-port", repository.list().get(0).id);
        assertEquals("HDMI 2", repository.list().get(0).displayName);
        assertFalse(repository.list().get(0).remembered);
        repository.stop();
    }

    @Test public void manualNamesAndBrandsPersistWhenOnlyGenericPortRemains() {
        reported.add(parent(0));
        HdmiRepository repository = repository();
        repository.start(() -> {});
        repository.setOverride("physical-port", DeviceBrand.PLAYSTATION);
        repository.setNameOverride("physical-port", "PlayStation 5");
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(251));
        assertEquals("PlayStation 5", repository.list().get(0).displayName);
        repository.stop();
        HdmiRepository restarted = repository();
        restarted.start(() -> {});
        assertEquals("PlayStation 5", restarted.list().get(0).displayName);
        assertEquals(DeviceBrand.PLAYSTATION, restarted.list().get(0).brand);
        assertEquals(1, restarted.list().size());
        restarted.stop();
    }

    @Test public void emptyFirmwareSnapshotAndPermissionFailureKeepRememberedIdentity() {
        reported.add(parent(0));
        reported.add(device("cec-ps5", "PS5"));
        HdmiRepository repository = repository();
        repository.start(() -> {});
        repository.stop();
        HdmiRepository denied = new HdmiRepository(context, () -> { throw new SecurityException("Firmware restriction"); });
        denied.start(() -> {});
        assertEquals("cec-ps5", denied.list().get(0).id);
        assertEquals("PlayStation 5", denied.list().get(0).displayName);
        denied.stop();
        reported.clear();
        HdmiRepository empty = repository();
        empty.start(() -> {});
        assertEquals(1, empty.list().size());
        assertEquals("cec-ps5", empty.list().get(0).id);
        assertEquals("physical-port", empty.list().get(0).openId);
        empty.stop();
    }

    @Test public void genericPortNamesAreNeverPersistedAsRecognizedDevices() {
        reported.add(parent(2));
        HdmiRepository repository = repository();
        repository.start(() -> {});
        assertEquals("HDMI 2", repository.list().get(0).displayName);
        assertEquals("[]", context.getSharedPreferences("hdmi_known_devices", 0).getString("entries", "[]"));
        repository.stop();
    }

    @Test public void genericPlaybackChildUsesPortNumberAndIsNotRememberedAsADevice() {
        reported.add(parent(2));
        reported.add(device("playback_1", "playback_1"));
        HdmiRepository repository = repository();
        repository.start(() -> {});
        assertEquals(1, repository.list().size());
        assertEquals("HDMI 2", repository.list().get(0).displayName);
        assertEquals("playback_1", repository.list().get(0).openId);
        assertEquals("[]", context.getSharedPreferences("hdmi_known_devices", 0).getString("entries", "[]"));
        repository.stop();
    }

    @Test public void targetTclHardwareIdsMapToPhysicalHdmiNumbers() {
        assertEquals("HDMI 1", HdmiRepository.safeName(
                "com.tcl.tvinput/.TvPassThroughService/HW15"));
        assertEquals("HDMI 4", HdmiRepository.safeName(
                "com.tcl.tvinput/.TvPassThroughService/HW18"));
        reported.add(new HdmiRepository.InputDescriptor(
                "com.tcl.tvinput/.TvPassThroughService/HW17", null, "", "", "", 2));
        HdmiRepository repository = repository();
        repository.start(() -> {});
        assertEquals("HDMI 3", repository.list().get(0).displayName);
        repository.stop();
    }

    @Test public void clearingManualOverrideKeepsRememberedStandbyDevice() {
        reported.add(parent(0));
        reported.add(device("cec-ps5", "PS5"));
        HdmiRepository repository = repository();
        repository.start(() -> {});
        repository.setNameOverride("cec-ps5", "Wohnzimmer-Konsole");
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(251));
        reported.clear();
        reported.add(parent(TvInputManager.INPUT_STATE_CONNECTED_STANDBY));
        repository.start(() -> {});
        repository.clearOverrides("cec-ps5");
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(251));
        assertEquals("PlayStation 5", repository.list().get(0).displayName);
        assertTrue(repository.list().get(0).remembered);
        repository.stop();
    }
}

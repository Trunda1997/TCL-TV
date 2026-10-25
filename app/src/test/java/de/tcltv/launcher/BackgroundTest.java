package de.tcltv.launcher;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class BackgroundTest {
    @Test public void presetCatalogContainsAllRequestedGroups() {
        assertTrue(BackgroundPresets.ALL.stream().filter(p -> p.group.equals("Farben")).count() >= 14);
        assertTrue(BackgroundPresets.ALL.stream().filter(p -> p.group.equals("Verläufe")).count() >= 7);
        assertTrue(BackgroundPresets.ALL.stream().filter(p -> p.live).count() >= 7);
        assertEquals(BackgroundPresets.ALL.size(), BackgroundPresets.ALL.stream().map(p -> p.id).distinct().count());
    }

    @Test public void remoteSliderClampsAndKeepsUpForRowNavigation() {
        try (ActivityController<Activity> activity = Robolectric.buildActivity(Activity.class).setup()) {
            AtomicInteger value = new AtomicInteger(95), ups = new AtomicInteger();
            BackgroundScreen.Adjustment slider = new BackgroundScreen.Adjustment(activity.get(), "Blur", 95, 0, 100, 5, "%", value::set);
            slider.setOnKeyListener((view, key, event) -> { if (key == KeyEvent.KEYCODE_DPAD_UP) { ups.incrementAndGet(); return true; } return false; });
            slider.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
            assertEquals(100, value.get());
            slider.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
            assertEquals(100, value.get());
            slider.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
            assertEquals(95, value.get());
            slider.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
            assertEquals(1, ups.get());
        }
    }

    @Test public void selectionsAndSettingsPersistAcrossControllerRecreation() {
        try (ActivityController<Activity> activity = Robolectric.buildActivity(Activity.class).setup()) {
            BackgroundController first = new BackgroundController(activity.get(), new FrameLayout(activity.get()));
            first.choosePreset("gradient_petrol"); first.setValue("dim", 65); first.setValue("blur", 30);
            first.setEnabled("animation", false); first.setOption("scale", "fit"); first.destroy();
            BackgroundController second = new BackgroundController(activity.get(), new FrameLayout(activity.get()));
            assertEquals("gradient_petrol", second.mode()); assertEquals(65, second.value("dim", 0));
            assertEquals(30, second.value("blur", 0)); assertFalse(second.enabled("animation", true));
            assertEquals("fit", second.option("scale", "crop")); second.destroy();
        }
    }

    @Test public void backgroundAnimationStopsWithLifecycle() {
        try (ActivityController<Activity> activity = Robolectric.buildActivity(Activity.class).setup()) {
            FrameLayout root = new FrameLayout(activity.get());
            BackgroundController background = new BackgroundController(activity.get(), root);
            background.choosePreset("water"); background.start();
            AnimatedBackgroundView scene = (AnimatedBackgroundView) root.getChildAt(0);
            assertTrue(scene.isAnimating()); background.stop(); assertFalse(scene.isAnimating());
            background.start(); assertTrue(scene.isAnimating()); background.setEnabled("animation", false);
            assertFalse(scene.isAnimating()); background.destroy();
        }
    }

    @Test public void startupRotationRunsOncePerControllerNotAfterEveryExternalApp() {
        try (ActivityController<Activity> activity = Robolectric.buildActivity(Activity.class).setup()) {
            BackgroundController first = new BackgroundController(activity.get(), new FrameLayout(activity.get()));
            first.setEnabled("auto", true); first.setOption("rotation", "startup"); first.start();
            String initial = first.mode(); first.stop(); first.start(); assertEquals(initial, first.mode()); first.destroy();
            BackgroundController second = new BackgroundController(activity.get(), new FrameLayout(activity.get()));
            second.start(); assertNotEquals(initial, second.mode()); second.destroy();
        }
    }

    @Test public void blurPreservesSourceAndSpreadsImageEdges() {
        Bitmap bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.BLACK); bitmap.setPixel(10, 10, Color.WHITE);
        Bitmap blurred = BackgroundController.blur(bitmap, 40);
        assertEquals(Color.WHITE, bitmap.getPixel(10, 10));
        assertTrue(Color.red(blurred.getPixel(10, 10)) < 255);
        assertTrue(Color.red(blurred.getPixel(10, 9)) > 0);
    }
}

package de.tcltv.launcher;

import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "w960dp-h540dp-land-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class TvNavigationTest {
    @SuppressWarnings("unchecked")
    private <T> T field(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(target);
    }

    private void key(MainActivity activity, int code) {
        activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private void invoke(MainActivity activity, String name) throws Exception {
        Method method = MainActivity.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(activity);
    }

    private void layout(MainActivity activity) {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false);
        View decor = activity.getWindow().getDecorView();
        int width = activity.getResources().getDisplayMetrics().widthPixels;
        int height = activity.getResources().getDisplayMetrics().heightPixels;
        decor.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, width, height);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400));
    }

    private void settleScroll(HorizontalScrollView viewport) {
        // Drive real framework scrolling and focus animations under the paused test clock.
        for (int frame = 0; frame < 40; frame++) {
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20));
            viewport.computeScroll();
        }
    }

    private void assertFullyVisible(HorizontalScrollView viewport, View card) {
        int[] viewportLocation = new int[2], cardLocation = new int[2];
        viewport.getLocationOnScreen(viewportLocation);
        card.getLocationOnScreen(cardLocation);
        assertTrue("Focused card must fit inside its viewport", viewport.getWidth() > card.getWidth());
        assertTrue("Focused card's left edge including focus scale is visible",
                cardLocation[0] >= viewportLocation[0] - 1);
        assertTrue("Focused card's right edge including focus scale is visible",
                cardLocation[0] + Math.round(card.getWidth() * card.getScaleX())
                        <= viewportLocation[0] + viewport.getWidth() + 1);
    }

    private void fillAppRail(MainActivity activity, LinearLayout row, int count) throws Exception {
        row.removeAllViews();
        Method makeTile = MainActivity.class.getDeclaredMethod("appTile", AppEntry.class, boolean.class);
        makeTile.setAccessible(true);
        for (int i = 0; i < count; i++) {
            row.addView((View) makeTile.invoke(activity,
                    new AppEntry("App " + i, "test.tv.app" + i, null), false));
        }
        invoke(activity, "wireNavigation");
        layout(activity);
    }

    @Test public void longAppRailScrollsInBothDirectionsAndKeepsFocusedCardFullyVisible() throws Exception {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup().visible().windowFocusChanged(true)) {
            MainActivity activity = controller.get();
            List<LinearLayout> rows = field(activity, "rows");
            LinearLayout favorites = rows.get(1);
            fillAppRail(activity, favorites, 12);
            HorizontalScrollView viewport = (HorizontalScrollView) favorites.getParent();
            rows.get(0).getChildAt(0).requestFocus();
            key(activity, KeyEvent.KEYCODE_DPAD_DOWN);
            assertSame(favorites.getChildAt(0), activity.getWindow().getDecorView().findFocus());
            for (int i = 1; i < favorites.getChildCount(); i++) {
                key(activity, KeyEvent.KEYCODE_DPAD_RIGHT);
                assertSame(favorites.getChildAt(i), activity.getWindow().getDecorView().findFocus());
                settleScroll(viewport);
                assertFullyVisible(viewport, activity.getWindow().getDecorView().findFocus());
            }
            assertTrue("Navigating beyond the first screen must scroll the rail", viewport.getScrollX() > 0);
            for (int i = favorites.getChildCount() - 2; i >= 0; i--) {
                key(activity, KeyEvent.KEYCODE_DPAD_LEFT);
                assertSame(favorites.getChildAt(i), activity.getWindow().getDecorView().findFocus());
                settleScroll(viewport);
                assertFullyVisible(viewport, activity.getWindow().getDecorView().findFocus());
            }
            key(activity, KeyEvent.KEYCODE_DPAD_LEFT);
            assertSame(favorites.getChildAt(0), activity.getWindow().getDecorView().findFocus());
        }
    }

    @Test public void upAndDownRestoreEachRowsLastCardInsteadOfUsingOtherRowsColumn() throws Exception {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup().visible().windowFocusChanged(true)) {
            MainActivity activity = controller.get();
            List<LinearLayout> rows = field(activity, "rows");
            LinearLayout favorites = rows.get(1), continueWatching = rows.get(2);
            fillAppRail(activity, favorites, 6);
            favorites.getChildAt(0).requestFocus();
            for (int i = 0; i < 4; i++) key(activity, KeyEvent.KEYCODE_DPAD_RIGHT);
            View rememberedFavorite = favorites.getChildAt(4);
            assertSame(rememberedFavorite, activity.getWindow().getDecorView().findFocus());
            key(activity, KeyEvent.KEYCODE_DPAD_DOWN);
            assertSame(continueWatching.getChildAt(3), activity.getWindow().getDecorView().findFocus());
            key(activity, KeyEvent.KEYCODE_DPAD_LEFT);
            View rememberedContent = continueWatching.getChildAt(2);
            assertSame(rememberedContent, activity.getWindow().getDecorView().findFocus());
            key(activity, KeyEvent.KEYCODE_DPAD_UP);
            assertSame(rememberedFavorite, activity.getWindow().getDecorView().findFocus());
            key(activity, KeyEvent.KEYCODE_DPAD_DOWN);
            assertSame(rememberedContent, activity.getWindow().getDecorView().findFocus());
            key(activity, KeyEvent.KEYCODE_DPAD_UP);
            assertSame(rememberedFavorite, activity.getWindow().getDecorView().findFocus());
        }
    }

    @Test public void hdmiDescriptorUpdatesPreserveCardIdentityFocusAndSearchPosition() throws Exception {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup().visible().windowFocusChanged(true)) {
            MainActivity activity = controller.get();
            HdmiRepository repository = field(activity, "hdmiRepository");
            repository.stop();
            setField(repository, "loaded", true);
            setField(repository, "cached", Arrays.asList(
                    new HdmiInput("input-a", "Sonos", "Sonos", 0, DeviceBrand.SONOS),
                    new HdmiInput("input-b", "Apple TV", "Apple TV", 0, DeviceBrand.APPLE_TV)));
            invoke(activity, "refreshHomeHdmi");
            layout(activity);
            LinearLayout row = field(activity, "hdmiRow");
            View search = row.getChildAt(0);
            View sonos = row.findViewWithTag("input-a");
            View appleTv = row.findViewWithTag("input-b");
            sonos.requestFocus();
            setField(repository, "cached", Arrays.asList(
                    new HdmiInput("input-a", "Sonos", "Wohnzimmer", 2, DeviceBrand.SONOS),
                    new HdmiInput("input-b", "Apple TV", "Apple TV", 0, DeviceBrand.APPLE_TV),
                    new HdmiInput("input-c", "Nintendo", "Nintendo Switch 2", 0, DeviceBrand.SWITCH2)));
            invoke(activity, "refreshHomeHdmi");
            assertSame(sonos, row.findViewWithTag("input-a"));
            assertSame(appleTv, row.findViewWithTag("input-b"));
            assertSame(sonos, activity.getWindow().getDecorView().findFocus());
            assertSame(search, row.getChildAt(0));
            assertEquals("Wohnzimmer", ((TextView) ((LinearLayout) sonos).getChildAt(1)).getText().toString());
            assertEquals(4, row.getChildCount());
            setField(repository, "cached", Arrays.asList(
                    new HdmiInput("input-a", "Sonos", "Wohnzimmer", 2, DeviceBrand.SONOS)));
            invoke(activity, "refreshHomeHdmi");
            assertSame(sonos, activity.getWindow().getDecorView().findFocus());
            assertSame(sonos, row.getChildAt(1));
            assertSame(search, row.getChildAt(0));
            setField(repository, "cached", java.util.Collections.emptyList());
            invoke(activity, "refreshHomeHdmi");
            assertSame("Removing the focused input falls back to an existing card", search, activity.getWindow().getDecorView().findFocus());
        }
    }

    @Test public void hdmiFocusDoesNotReorderSearchAndSonos() throws Exception {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup().visible().windowFocusChanged(true)) {
            MainActivity activity = controller.get();
            LinearLayout row = field(activity, "hdmiRow");
            View search = row.getChildAt(0);
            Method makeTile = MainActivity.class.getDeclaredMethod("hdmiTile", HdmiInput.class);
            makeTile.setAccessible(true);
            View sonos = (View) makeTile.invoke(activity, new HdmiInput("sonos", "Sonos", "Sonos", 0, DeviceBrand.SONOS));
            row.addView(sonos);
            Method wire = MainActivity.class.getDeclaredMethod("wireNavigation");
            wire.setAccessible(true);
            wire.invoke(activity);
            layout(activity);
            assertTrue(search.requestFocus());
            for (int i = 0; i < 12; i++) {
                key(activity, KeyEvent.KEYCODE_DPAD_RIGHT);
                assertSame(sonos, activity.getWindow().getDecorView().findFocus());
                assertSame(search, row.getChildAt(0));
                key(activity, KeyEvent.KEYCODE_DPAD_RIGHT);
                assertSame(sonos, activity.getWindow().getDecorView().findFocus());
                key(activity, KeyEvent.KEYCODE_DPAD_LEFT);
                assertSame(search, activity.getWindow().getDecorView().findFocus());
            }
        }
    }

    @Test public void everyTabHasDeterministicRailsAndBackReturnsHome() throws Exception {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup().visible().windowFocusChanged(true)) {
            MainActivity activity = controller.get();
            List<TextView> nav = field(activity, "navViews");
            for (TextView tab : nav) {
                tab.requestFocus();
                tab.performClick();
                layout(activity);
                assertTrue(tab.requestFocus());
                List<LinearLayout> rows = field(activity, "rows");
                key(activity, KeyEvent.KEYCODE_DPAD_DOWN);
                for (LinearLayout row : rows) {
                    if (row.getChildCount() == 0) continue;
                    assertSame(row, activity.getWindow().getDecorView().findFocus().getParent());
                    // Entering a new row keeps/clamps the previous column. Start at its left edge.
                    for (int i = 0; i < row.getChildCount(); i++) key(activity, KeyEvent.KEYCODE_DPAD_LEFT);
                    assertSame(row.getChildAt(0), activity.getWindow().getDecorView().findFocus());
                    for (int i = 1; i < row.getChildCount(); i++) {
                        key(activity, KeyEvent.KEYCODE_DPAD_RIGHT);
                        assertSame(row.getChildAt(i), activity.getWindow().getDecorView().findFocus());
                    }
                    View last = activity.getWindow().getDecorView().findFocus();
                    key(activity, KeyEvent.KEYCODE_DPAD_RIGHT);
                    assertSame(last, activity.getWindow().getDecorView().findFocus());
                    assertFalse(((HorizontalScrollView) row.getParent()).isFocusable());
                    key(activity, KeyEvent.KEYCODE_DPAD_DOWN);
                }
                activity.onBackPressed();
                assertEquals("Home", field(activity, "currentTab"));
                assertSame(nav.get(0), activity.getWindow().getDecorView().findFocus());
            }
            assertEquals(3, ((List<?>) field(activity, "rows")).size());
        }
    }

    @Test public void connectionStateDoesNotCountAsStructuralHdmiChange() {
        HdmiInput old = new HdmiInput("port", "HDMI 1", "Sonos", 0, DeviceBrand.SONOS);
        HdmiInput state = new HdmiInput("port", "HDMI 1", "Sonos", 2, DeviceBrand.SONOS);
        HdmiInput renamed = new HdmiInput("port", "HDMI 1", "Wohnzimmer", 2, DeviceBrand.SONOS);
        assertTrue(HdmiRepository.sameDescriptors(Arrays.asList(old), Arrays.asList(state)));
        assertFalse(HdmiRepository.sameDescriptors(Arrays.asList(old), Arrays.asList(renamed)));
    }
}

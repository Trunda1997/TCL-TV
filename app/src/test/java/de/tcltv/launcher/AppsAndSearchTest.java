package de.tcltv.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowPackageManager;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "w960dp-h540dp-land-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class AppsAndSearchTest {
    private void install(String pkg, String label) {
        Context context = RuntimeEnvironment.getApplication();
        ShadowPackageManager pm = Shadows.shadowOf(context.getPackageManager());
        PackageInfo info = new PackageInfo();
        info.packageName = pkg;
        info.applicationInfo = new ApplicationInfo();
        info.applicationInfo.packageName = pkg;
        info.applicationInfo.enabled = true;
        info.applicationInfo.nonLocalizedLabel = label;
        pm.installPackage(info);
        ComponentName component = new ComponentName(pkg, pkg + ".TvLauncher");
        ActivityInfo activity = pm.addActivityIfNotPresent(component);
        activity.enabled = true;
        activity.exported = true;
        activity.nonLocalizedLabel = label;
        activity.applicationInfo.enabled = true;
        pm.addOrUpdateActivity(activity);
        IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        pm.addIntentFilterForActivity(component, filter);
        pm.addActivityIcon(component, new ColorDrawable(Color.WHITE));
        pm.setApplicationIcon(pkg, new ColorDrawable(Color.WHITE));
    }

    private void layout(View decor) {
        decor.measure(View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, 1920, 1080);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400));
    }

    private void key(TvSearchDialog dialog, int key) {
        dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, key));
        dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, key));
    }

    @Test public void categorySelectionFiltersAndBackReturnsToSelectedCategory() {
        install("com.spotify.tv.android", "Spotify");
        install("com.netflix.ninja", "Netflix");
        try (ActivityController<RecordingActivity> controller = Robolectric.buildActivity(RecordingActivity.class).setup().visible().windowFocusChanged(true)) {
            RecordingActivity activity = controller.get();
            AppsScreen screen = new AppsScreen(activity, new AppRepository(activity), app -> {
                TextView tile = new TextView(activity);
                tile.setTag("app:" + app.packageName);
                tile.setText(app.name);
                tile.setFocusableInTouchMode(true);
                tile.setLayoutParams(new LinearLayout.LayoutParams(300, 180));
                return tile;
            }, () -> {}, null);
            activity.setContentView(screen.root());
            layout(activity.getWindow().getDecorView());
            View music = screen.root().findViewWithTag("app-category:MUSIC");
            assertNotNull(music);
            music.requestFocus();
            music.performClick();
            layout(activity.getWindow().getDecorView());
            LinearLayout results = screen.rows().get(2);
            assertEquals(1, results.getChildCount());
            assertEquals("app:com.spotify.tv.android", results.getChildAt(0).getTag());
            assertTrue(music.isSelected());
            assertSame(results.getChildAt(0), activity.getWindow().getDecorView().findFocus());
            assertTrue(screen.onBack());
            assertSame(music, activity.getWindow().getDecorView().findFocus());
            assertFalse(screen.onBack());
            LinearLayout suggestions = screen.rows().get(3);
            assertTrue("Apps page has installed recommendations", suggestions.getChildCount() > 0);
        }
    }

    @Test public void searchHasReachableSubmitCancelAndStartsInstalledApp() {
        install("com.netflix.ninja", "Netflix");
        try (ActivityController<RecordingActivity> controller = Robolectric.buildActivity(RecordingActivity.class).setup().visible().windowFocusChanged(true)) {
            RecordingActivity activity = controller.get();
            TextView opener = new TextView(activity);
            opener.setFocusableInTouchMode(true);
            activity.setContentView(opener);
            layout(activity.getWindow().getDecorView());
            opener.requestFocus();
            TvSearchDialog dialog = new TvSearchDialog(activity, new AppRepository(activity));
            dialog.show();
            View decor = dialog.getWindow().getDecorView();
            layout(decor);
            EditText input = decor.findViewWithTag("tv-search-input");
            View search = decor.findViewWithTag("tv-search-submit");
            View cancel = decor.findViewWithTag("tv-search-cancel");
            input.setText("ne");
            layout(decor);
            input.requestFocus();
            key(dialog, KeyEvent.KEYCODE_DPAD_DOWN);
            View suggestion = decor.findViewWithTag("tv-search-suggestion:com.netflix.ninja");
            assertNotNull(suggestion);
            assertSame(suggestion, decor.findFocus());
            for (int i = 0; i < 5 && decor.findFocus() != search; i++)
                key(dialog, KeyEvent.KEYCODE_DPAD_DOWN);
            assertSame(search, decor.findFocus());
            key(dialog, KeyEvent.KEYCODE_DPAD_RIGHT);
            assertSame(cancel, decor.findFocus());
            key(dialog, KeyEvent.KEYCODE_DPAD_LEFT);
            assertSame(search, decor.findFocus());
            key(dialog, KeyEvent.KEYCODE_DPAD_CENTER);
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400));
            assertNotNull(activity.started);
            assertEquals("com.netflix.ninja", activity.started.getComponent().getPackageName());
            assertFalse(dialog.isShowing());
            assertSame(opener, activity.getWindow().getDecorView().findFocus());
        }
    }

    @Test public void imeSearchUsesStoreFallbackAndBackFirstLeavesEditing() {
        try (ActivityController<RecordingActivity> controller = Robolectric.buildActivity(RecordingActivity.class).setup().visible().windowFocusChanged(true)) {
            RecordingActivity activity = controller.get();
            activity.setContentView(new LinearLayout(activity));
            TvSearchDialog dialog = new TvSearchDialog(activity, new AppRepository(activity));
            dialog.show();
            layout(dialog.getWindow().getDecorView());
            EditText input = dialog.getWindow().getDecorView().findViewWithTag("tv-search-input");
            input.performClick();
            assertFalse("The virtual keyboard must keep DOWN while editing",
                    input.onKeyPreIme(KeyEvent.KEYCODE_DPAD_DOWN,
                            new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN)));
            dialog.onBackPressed();
            assertTrue("First BACK hides editing instead of closing search", dialog.isShowing());
            input.setText("Nicht installierte App");
            input.onEditorAction(EditorInfo.IME_ACTION_SEARCH);
            assertNotNull(activity.started);
            assertEquals("market", activity.started.getData().getScheme());
            assertTrue(activity.started.getData().toString().contains("Nicht%20installierte%20App"));
            assertFalse(dialog.isShowing());
            TvSearchDialog again = new TvSearchDialog(activity, new AppRepository(activity));
            again.show();
            again.onBackPressed();
            assertFalse(again.isShowing());
        }
    }

    public static class RecordingActivity extends Activity {
        Intent started;
        @Override public void startActivity(Intent intent) { started = new Intent(intent); }
    }
}

package de.tcltv.launcher;

import android.content.Context;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class FavoriteAppsTest {
    @Test public void initialStreamingDefaultsCanBeRemovedAndEmptyStaysEmpty() {
        FavoriteApps store = new FavoriteApps(RuntimeEnvironment.getApplication());
        store.initialize(Arrays.asList(new AppEntry("Netflix", "com.netflix.ninja", null)));
        assertTrue(store.contains("com.netflix.ninja"));
        assertFalse(store.toggle("com.netflix.ninja"));
        store.initialize(Arrays.asList(new AppEntry("Netflix", "com.netflix.ninja", null)));
        assertTrue(store.packages().isEmpty());
        assertTrue(store.toggle("test.extra"));
        assertEquals(Collections.singletonList("test.extra"), new FavoriteApps(RuntimeEnvironment.getApplication()).packages());
    }
    @Test public void oldExplicitFavoritesMigrateWithoutAddingDefaults() {
        Context context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("tcl_tv", Context.MODE_PRIVATE).edit()
                .putBoolean("favorites_edited", true)
                .putStringSet("favorites", Collections.singleton("old.package")).apply();
        FavoriteApps store = new FavoriteApps(context);
        store.initialize(Arrays.asList(new AppEntry("Netflix", "com.netflix.ninja", null)));
        assertEquals(Collections.singletonList("old.package"), store.packages());
    }
    @Test public void emptyDiscoveryDoesNotLockInEmptyDefaults() {
        FavoriteApps store = new FavoriteApps(RuntimeEnvironment.getApplication());
        store.initialize(Collections.emptyList());
        store.initialize(Arrays.asList(new AppEntry("YouTube", "com.google.android.youtube.tv", null)));
        assertTrue(store.contains("com.google.android.youtube.tv"));
    }
}

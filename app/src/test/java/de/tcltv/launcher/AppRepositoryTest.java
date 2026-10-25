package de.tcltv.launcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AppRepositoryTest {
    private ComponentName installLauncher(String pkg, String activityName, String label,
            String category, boolean enabled) {
        Context context = RuntimeEnvironment.getApplication();
        PackageManager pm = context.getPackageManager();
        ShadowPackageManager shadow = Shadows.shadowOf(pm);
        try {
            pm.getApplicationInfo(pkg, 0);
        } catch (PackageManager.NameNotFoundException missing) {
            PackageInfo info = new PackageInfo();
            info.packageName = pkg;
            info.applicationInfo = new ApplicationInfo();
            info.applicationInfo.packageName = pkg;
            info.applicationInfo.nonLocalizedLabel = label;
            info.applicationInfo.enabled = true;
            shadow.installPackage(info);
        }
        ComponentName component = new ComponentName(pkg, pkg + "." + activityName);
        ActivityInfo activity = shadow.addActivityIfNotPresent(component);
        activity.enabled = enabled;
        activity.exported = true;
        activity.nonLocalizedLabel = label;
        activity.applicationInfo.enabled = true;
        shadow.addOrUpdateActivity(activity);
        IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(category);
        shadow.addIntentFilterForActivity(component, filter);
        shadow.addActivityIcon(component, new ColorDrawable(Color.WHITE));
        shadow.setApplicationIcon(pkg, new ColorDrawable(Color.WHITE));
        return component;
    }

    @Test public void combinesTvAndStandardLaunchersWithoutDuplicatesOrSelf() {
        Context context = RuntimeEnvironment.getApplication();
        installLauncher("com.netflix.ninja", "TvLauncher", "Netflix", Intent.CATEGORY_LEANBACK_LAUNCHER, true);
        installLauncher("com.netflix.ninja", "StandardLauncher", "Netflix", Intent.CATEGORY_LAUNCHER, true);
        installLauncher("tv.example.standard", "StandardLauncher", "Standard TV App", Intent.CATEGORY_LAUNCHER, true);
        installLauncher(context.getPackageName(), "SelfLauncher", "TCL TV", Intent.CATEGORY_LEANBACK_LAUNCHER, true);
        installLauncher("tv.example.disabled", "DisabledLauncher", "Disabled App", Intent.CATEGORY_LAUNCHER, false);

        List<AppEntry> apps = new AppRepository(context).installedTvApps();
        List<String> packages = apps.stream().map(app -> app.packageName).collect(Collectors.toList());
        assertTrue("TV launcher must be discovered", packages.contains("com.netflix.ninja"));
        assertTrue("Standard launcher fallback must be discovered", packages.contains("tv.example.standard"));
        assertEquals("Multiple launcher activities represent one app", 1,
                packages.stream().filter("com.netflix.ninja"::equals).count());
        assertFalse("Launcher must exclude itself", packages.contains(context.getPackageName()));
        assertFalse("Disabled activities must not become launch targets", packages.contains("tv.example.disabled"));
    }

    @Test public void appWithoutLaunchableActivityIsNotListed() {
        PackageInfo info = new PackageInfo();
        info.packageName = "tv.example.serviceonly";
        info.applicationInfo = new ApplicationInfo();
        info.applicationInfo.packageName = info.packageName;
        info.applicationInfo.enabled = true;
        Shadows.shadowOf(RuntimeEnvironment.getApplication().getPackageManager()).installPackage(info);
        assertFalse(new AppRepository(RuntimeEnvironment.getApplication()).installedTvApps().stream()
                .anyMatch(app -> app.packageName.equals(info.packageName)));
    }

    @Test public void launchPrefersLeanbackActivity() {
        ComponentName tv = installLauncher("tv.example.both", "TvLauncher", "Both", Intent.CATEGORY_LEANBACK_LAUNCHER, true);
        installLauncher("tv.example.both", "StandardLauncher", "Both", Intent.CATEGORY_LAUNCHER, true);
        try (ActivityController<RecordingActivity> controller = Robolectric.buildActivity(RecordingActivity.class).setup()) {
            RecordingActivity activity = controller.get();
            assertTrue(LauncherUtils.launchPackage(activity, "tv.example.both"));
            assertEquals(1, activity.attempts.size());
            assertEquals(tv, activity.attempts.get(0).getComponent());
        }
    }

    @Test public void launchUsesStandardActivityWhenLeanbackIsMissing() {
        ComponentName standard = installLauncher("tv.example.standard", "StandardLauncher", "Standard", Intent.CATEGORY_LAUNCHER, true);
        try (ActivityController<RecordingActivity> controller = Robolectric.buildActivity(RecordingActivity.class).setup()) {
            RecordingActivity activity = controller.get();
            assertTrue(LauncherUtils.launchPackage(activity, "tv.example.standard"));
            assertEquals(standard, activity.attempts.get(0).getComponent());
        }
    }

    @Test public void launchRetriesStandardActivityWhenFirmwareRejectsLeanbackStart() {
        ComponentName tv = installLauncher("tv.example.both", "TvLauncher", "Both", Intent.CATEGORY_LEANBACK_LAUNCHER, true);
        ComponentName standard = installLauncher("tv.example.both", "StandardLauncher", "Both", Intent.CATEGORY_LAUNCHER, true);
        try (ActivityController<RecordingActivity> controller = Robolectric.buildActivity(RecordingActivity.class).setup()) {
            RecordingActivity activity = controller.get();
            activity.reject = tv;
            assertTrue(LauncherUtils.launchPackage(activity, "tv.example.both"));
            assertEquals(2, activity.attempts.size());
            assertEquals(standard, activity.attempts.get(1).getComponent());
        }
    }

    @Test public void missingAppReturnsFalseWithoutStartingAnything() {
        try (ActivityController<RecordingActivity> controller = Robolectric.buildActivity(RecordingActivity.class).setup()) {
            RecordingActivity activity = controller.get();
            assertFalse(LauncherUtils.launchPackage(activity, "tv.example.notinstalled"));
            assertFalse(LauncherUtils.launchPackage(activity, ""));
            assertFalse(LauncherUtils.launchPackage(activity, null));
            assertTrue(activity.attempts.isEmpty());
        }
    }

    public static class RecordingActivity extends Activity {
        final List<Intent> attempts = new ArrayList<>();
        ComponentName reject;
        @Override public void startActivity(Intent intent) {
            attempts.add(new Intent(intent));
            if (intent.getComponent().equals(reject)) throw new ActivityNotFoundException("Firmware rejected leanback start");
        }
    }
}

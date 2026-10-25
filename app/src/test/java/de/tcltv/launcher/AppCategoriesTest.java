package de.tcltv.launcher;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class AppCategoriesTest {
    @Test public void appsCanBelongToSeveralCategories() {
        AppEntry disney = new AppEntry("Disney+", "com.disney.disneyplus", null);
        assertTrue(AppCategories.categories(disney).contains(AppCategories.Category.MOVIES));
        assertTrue(AppCategories.categories(disney).contains(AppCategories.Category.KIDS));
        assertTrue(AppCategories.categories(disney).contains(AppCategories.Category.RECOMMENDED));
        AppEntry ard = new AppEntry("ARD Mediathek", "de.swr.avp.ard.tv", null);
        assertTrue(AppCategories.categories(ard).contains(AppCategories.Category.MEDIA_LIBRARIES));
        assertTrue(AppCategories.categories(ard).contains(AppCategories.Category.ENTERTAINMENT));
    }

    @Test public void categoriesActuallyFilterTheInstalledList() {
        AppEntry spotify = new AppEntry("Spotify", "com.spotify.tv.android", null);
        AppEntry netflix = new AppEntry("Netflix", "com.netflix.ninja", null);
        AppEntry tools = new AppEntry("Dateimanager", "tv.files.browser", null);
        List<AppEntry> apps = Arrays.asList(spotify, netflix, tools);
        assertEquals(Arrays.asList(spotify), AppCategories.filter(apps, AppCategories.Category.MUSIC));
        assertEquals(Arrays.asList(netflix), AppCategories.filter(apps, AppCategories.Category.MOVIES));
        assertEquals(Arrays.asList(tools), AppCategories.filter(apps, AppCategories.Category.TOOLS));
        assertEquals(apps, AppCategories.filter(apps, AppCategories.Category.ALL));
    }
}

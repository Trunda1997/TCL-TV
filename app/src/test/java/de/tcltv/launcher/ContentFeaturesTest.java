package de.tcltv.launcher;

import android.content.Context;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ContentFeaturesTest {
    private Context context;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("content_history", 0).edit().clear().commit();
    }

    @Test public void localHistoryIsSeparatedBySectionAndMovesReopenedItemFirst() {
        ContentHistory history = new ContentHistory(context);
        ContentItem dune = new ContentItem("Dune: Part Two", "Prime Video", 0);
        ContentItem batman = new ContentItem("The Batman", "Netflix", 0);
        history.record("movies", dune);
        history.record("movies", batman);
        history.record("movies", dune);
        history.record("series", new ContentItem("Wednesday", "Netflix", 0));
        List<ContentItem> movies = history.list("movies");
        assertEquals(2, movies.size());
        assertEquals("Dune: Part Two", movies.get(0).title);
        assertEquals("The Batman", movies.get(1).title);
        assertEquals("Wednesday", history.list("series").get(0).title);
    }

    @Test public void providerNamesResolveToInstalledTvApps() {
        List<AppEntry> apps = Arrays.asList(
                new AppEntry("Amazon Prime Video", "com.amazon.amazonvideo.livingroom", null),
                new AppEntry("ARD Mediathek", "de.swr.avp.ard.tv", null),
                new AppEntry("Netflix", "com.netflix.ninja", null));
        assertEquals("com.amazon.amazonvideo.livingroom",
                ProviderResolver.find(apps, "Prime Video").packageName);
        assertEquals("de.swr.avp.ard.tv", ProviderResolver.find(apps, "ARD").packageName);
        assertNull(ProviderResolver.find(apps, "waipu.tv"));
    }
}

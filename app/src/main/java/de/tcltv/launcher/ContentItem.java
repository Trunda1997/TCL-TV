package de.tcltv.launcher;

public final class ContentItem {
    public final String title;
    public final String provider;
    public final int progress;
    /** Official provider URI when one is known; demo entries deliberately keep this null. */
    public final String deepLink;

    public ContentItem(String title, String provider, int progress) {
        this(title, provider, progress, null);
    }

    public ContentItem(String title, String provider, int progress, String deepLink) {
        this.title = title;
        this.provider = provider;
        this.progress = progress;
        this.deepLink = deepLink;
    }
}

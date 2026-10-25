package de.tcltv.launcher;

public final class ContentItem {
    public final String title;
    public final String provider;
    public final int progress;

    public ContentItem(String title, String provider, int progress) {
        this.title = title;
        this.provider = provider;
        this.progress = progress;
    }
}

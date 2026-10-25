package de.tcltv.launcher;

/** Offline artwork keeps demo and local-history cards visually complete on every TV. */
public final class ContentArtwork {
    private ContentArtwork() { }

    public static int resource(ContentItem item) {
        String title = AppCategories.normalize(item == null ? "" : item.title);
        if (title.contains("avatar")) return R.drawable.preview_ocean;
        if (title.contains("dune")) return R.drawable.preview_sand;
        if (title.contains("mandalorian") || title.contains("ahsoka") || title.contains("fallout"))
            return R.drawable.preview_space;
        if (title.contains("last of us") || title.contains("batman") || title.contains("penguin"))
            return R.drawable.preview_city;
        if (title.contains("rings") || title.contains("jack ryan") || title.contains("boys"))
            return R.drawable.preview_forest;
        return R.drawable.preview_neon;
    }
}

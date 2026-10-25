package de.tcltv.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int PICK_BACKGROUND = 4001;
    private static final int BG = Color.rgb(14, 17, 21);
    private static final int CARD = Color.argb(224, 42, 47, 55);
    private static final int CARD_FOCUS = Color.argb(248, 62, 68, 78);

    private final Handler handler = new Handler();
    private final List<TextView> navViews = new ArrayList<>();
    private LinearLayout content;
    private LinearLayout hdmiRow;
    private final List<LinearLayout> rows = new ArrayList<>();
    private TextView settings;
    private TextView clock;
    private AnimatedBackgroundView background;
    private ImageView customBackground;
    private HdmiRepository hdmiRepository;
    private AppRepository appRepository;
    private SharedPreferences prefs;
    private String appSnapshot;
    private String currentTab = "Home";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hdmiRepository = new HdmiRepository(this);
        appRepository = new AppRepository(this);
        prefs = getSharedPreferences("tcl_tv", MODE_PRIVATE);
        buildShell();
        applySavedBackground();
        showTab(savedInstanceState == null ? "Home" : savedInstanceState.getString("tab", "Home"));
        activeNav().requestFocus();
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
        }
    }

    @Override protected void onStart() {
        super.onStart();
        hdmiRepository.start(() -> {
            if ("Home".equals(currentTab)) refreshHomeHdmi();
        });
        handler.post(clockTick);
    }

    @Override protected void onResume() {
        super.onResume();
        StringBuilder signature = new StringBuilder();
        for (AppEntry app : appRepository.installedTvApps())
            signature.append(app.packageName).append(':').append(app.name).append(';');
        String next = signature.toString();
        if (appSnapshot != null && !appSnapshot.equals(next)
                && ("Home".equals(currentTab) || "Apps".equals(currentTab))) {
            View focused = getCurrentFocus();
            Object key = focused == null ? null : focused.getTag();
            boolean wasInContent = content.hasFocus();
            showTab(currentTab);
            if (wasInContent) {
                View target = key == null ? null : content.findViewWithTag(key);
                if (target != null) target.requestFocus();
                else if (!rows.isEmpty()) TvFocus.enterRow(rows.get(0), 0);
            }
        }
        appSnapshot = next;
        if (getCurrentFocus() == null) activeNav().requestFocus();
    }

    @Override protected void onStop() {
        hdmiRepository.stop();
        handler.removeCallbacks(clockTick);
        super.onStop();
    }

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (clock != null) {
                clock.setText(new SimpleDateFormat("HH:mm", Locale.GERMANY).format(new Date()));
            }
            handler.postDelayed(this, 30000);
        }
    };

    private void buildShell() {
        FrameLayout root = new FrameLayout(this);
        background = new AnimatedBackgroundView(this);
        root.setClipChildren(false);
        root.setClipToPadding(false);
        root.addView(background, match());

        customBackground = new ImageView(this);
        customBackground.setScaleType(ImageView.ScaleType.CENTER_CROP);
        customBackground.setAlpha(0.52f);
        customBackground.setVisibility(View.GONE);
        root.addView(customBackground, match());

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(48), dp(28), dp(48), dp(28));
        shell.setClipChildren(false);
        shell.setClipToPadding(false);
        root.addView(shell, match());

        shell.addView(buildTopBar(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(88)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setFocusable(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        content.setPadding(0, dp(22), 0, dp(40));
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        shell.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private View buildTopBar() {
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setClipChildren(false);
        top.setClipToPadding(false);

        TextView logo = text("TCL TV", 27, true);
        top.addView(logo, new LinearLayout.LayoutParams(dp(100), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setClipChildren(false);
        nav.setClipToPadding(false);
        nav.setPadding(dp(8), dp(12), dp(8), dp(12));
        nav.setBackground(rounded(Color.argb(90, 19, 22, 27), 28));
        String[] tabs = {"Home", "Filme", "Serien", "Apps", "Live TV", "Hintergrund"};
        for (String tab : tabs) {
            TextView v = text(tab, 14, false);
            v.setSingleLine(true);
            v.setId(View.generateViewId());
            v.setGravity(Gravity.CENTER);
            v.setFocusable(true);
            v.setOnClickListener(x -> showTab(tab));
            applyNavFocus(v);
            navViews.add(v);
            nav.addView(v, new LinearLayout.LayoutParams(0, dp(48), tab.equals("Hintergrund") ? 1.5f : tab.equals("Live TV") ? 1.15f : 1f));
        }
        top.addView(nav, new LinearLayout.LayoutParams(0, dp(76), 1f));

        TextView gear = text("Einstellungen", 12, false);
        settings = gear;
        gear.setId(View.generateViewId());
        gear.setGravity(Gravity.CENTER);
        gear.setFocusable(true);
        gear.setOnClickListener(v -> LauncherUtils.openSettings(this));
        applyFocus(gear);
        top.addView(gear, new LinearLayout.LayoutParams(dp(100), dp(52)));

        clock = text("", 16, false);
        clock.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        top.addView(clock, new LinearLayout.LayoutParams(dp(60), ViewGroup.LayoutParams.MATCH_PARENT));
        return top;
    }

    private void showTab(String tab) {
        boolean contentHadFocus = content.hasFocus();
        currentTab = tab;
        updateNav();
        hdmiRow = null;
        rows.clear();
        content.removeAllViews();
        if ("Home".equals(tab)) buildHome();
        else if ("Filme".equals(tab)) buildMovies(false);
        else if ("Serien".equals(tab)) buildMovies(true);
        else if ("Apps".equals(tab)) buildApps();
        else if ("Live TV".equals(tab)) buildLiveTv();
        else buildBackgrounds();
        wireNavigation();
        if (contentHadFocus) activeNav().requestFocus();
    }

    private void updateNav() {
        for (TextView v : navViews) {
            boolean active = v.getText().toString().equals(currentTab);
            v.setTextColor(active ? Color.BLACK : Color.WHITE);
            v.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            v.setBackground(active ? rounded(Color.WHITE, 26) : null);
        }
    }

    private void buildHome() {
        hdmiRow = addRow();
        View search = iconTile("⌕", "Suche", v -> showSearchDialog());
        search.setTag("search");
        hdmiRow.addView(search);
        refreshHomeHdmi();
        spacer(28);
        section("Favoriten-Apps");
        buildFavorites(addRow());
        spacer(26);
        section("Weiterschauen");
        LinearLayout row = addRow();
        for (ContentItem item : demoContinue()) row.addView(contentCard(item, true));
    }

    private void refreshHomeHdmi() {
        if (hdmiRow == null) return;
        View focused = hdmiRow.findFocus();
        String focusedId = focused != null && focused.getTag() instanceof String
                ? (String) focused.getTag() : null;
        int previousIndex = focused == null ? 0 : hdmiRow.indexOfChild(focused);
        List<HdmiInput> inputs = hdmiRepository.list();
        Set<String> ids = new HashSet<>();
        for (HdmiInput input : inputs) ids.add(input.id);
        for (int i = hdmiRow.getChildCount() - 1; i >= 1; i--) {
            if (!ids.contains(hdmiRow.getChildAt(i).getTag())) hdmiRow.removeViewAt(i);
        }
        for (int i = 0; i < inputs.size(); i++) {
            HdmiInput input = inputs.get(i);
            View existing = null;
            for (int j = 1; j < hdmiRow.getChildCount(); j++) {
                if (input.id.equals(hdmiRow.getChildAt(j).getTag())) existing = hdmiRow.getChildAt(j);
            }
            if (existing == null) {
                existing = hdmiTile(input);
                hdmiRow.addView(existing, Math.min(i + 1, hdmiRow.getChildCount()));
            } else {
                bindHdmiTile((LinearLayout) existing, input);
            }
        }
        wireNavigation();
        if (focused != null && !focused.isAttachedToWindow()) {
            View target = focusedId == null ? null : hdmiRow.findViewWithTag(focusedId);
            if (target == null) target = hdmiRow.getChildAt(Math.min(previousIndex, hdmiRow.getChildCount() - 1));
            target.requestFocus();
        }
    }

    private void bindHdmiTile(LinearLayout tile, HdmiInput input) {
        ImageView mark = (ImageView) tile.getChildAt(0);
        mark.setImageResource(DeviceLogo.resource(input.brand));
        mark.setContentDescription(input.brand.displayName);
        ((TextView) tile.getChildAt(1)).setText(input.displayName);
        tile.setOnClickListener(v -> LauncherUtils.openHdmi(this, input.id));
        tile.setOnLongClickListener(v -> { showHdmiEditor(input); return true; });
    }

    private View hdmiTile(HdmiInput input) {
        LinearLayout tile = cardLayout(dp(245), dp(150));
        tile.setTag(input.id);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        ImageView mark = new ImageView(this);
        mark.setImageResource(DeviceLogo.resource(input.brand));
        mark.setContentDescription(input.brand.displayName);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mark.setPadding(dp(8), dp(8), dp(8), dp(8));
        tile.addView(mark, new LinearLayout.LayoutParams(dp(130), dp(68)));
        TextView name = text(input.displayName, 16, false);
        name.setSingleLine(true);
        name.setGravity(Gravity.CENTER);
        tile.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(36)));
        tile.setOnClickListener(v -> LauncherUtils.openHdmi(this, input.id));
        tile.setOnLongClickListener(v -> {
            showHdmiEditor(input);
            return true;
        });
        applyFocus(tile);
        return margin(tile, 16);
    }

    private void showHdmiEditor(HdmiInput input) {
        String[] names = new String[DeviceBrand.values().length + 2];
        names[0] = "Automatisch erkennen";
        names[1] = "Gerätename manuell ändern";
        for (int i = 0; i < DeviceBrand.values().length; i++) names[i + 2] = DeviceBrand.values()[i].displayName;
        new AlertDialog.Builder(this).setTitle(input.displayName).setItems(names, (d, which) -> {
            if (which == 0) hdmiRepository.clearOverrides(input.id);
            else if (which == 1) {
                EditText e = new EditText(this);
                e.setSingleLine(true);
                e.setText(input.displayName);
                new AlertDialog.Builder(this)
                        .setTitle("Gerätename")
                        .setView(e)
                        .setPositiveButton("Speichern", (x, w) -> hdmiRepository.setNameOverride(input.id, e.getText().toString()))
                        .setNegativeButton("Abbrechen", null)
                        .show();
            } else {
                hdmiRepository.setOverride(input.id, DeviceBrand.values()[which - 2]);
            }
        }).show();
    }

    private void buildFavorites(LinearLayout row) {
        List<AppEntry> apps = appRepository.installedTvApps();
        Set<String> fav = new HashSet<>(prefs.getStringSet("favorites", new HashSet<>()));
        if (!prefs.getBoolean("favorites_edited", false)) {
            String[] preferred = {"youtube", "netflix", "prime", "amazon", "disney", "apple", "wow", "rtl", "joyn", "waipu", "zattoo", "ard", "zdf", "spotify"};
            for (String key : preferred) {
                for (AppEntry app : apps) {
                    if (app.name.toLowerCase(Locale.ROOT).contains(key)) fav.add(app.packageName);
                }
            }
            if (fav.isEmpty()) {
                for (int i = 0; i < Math.min(7, apps.size()); i++) fav.add(apps.get(i).packageName);
            }
            // Recalculate defaults until edited, and persist the effective set for the editor.
            prefs.edit().putStringSet("favorites", fav).apply();
        }
        int count = 0;
        for (AppEntry app : apps) {
            if (fav.contains(app.packageName)) {
                row.addView(appTile(app, false));
                count++;
            }
        }
        if (count == 0) row.addView(iconTile("+", "Apps auswählen", v -> showTab("Apps")));
    }

    private void buildMovies(boolean series) {
        section("Kategorien");
        LinearLayout genres = addRow();
        String[] labels = series
                ? new String[]{"Drama", "Comedy", "Action", "Krimi", "Sci-Fi", "Doku", "Animation"}
                : new String[]{"Action", "Komödie", "Drama", "Sci-Fi", "Thriller", "Familie", "Animation"};
        for (String g : labels) genres.addView(genreTile(g));

        spacer(26);
        section(series ? "Empfohlene Serien" : "Empfohlene Filme");
        LinearLayout row = addRow();
        for (ContentItem item : series ? demoSeries() : demoMovies()) row.addView(contentCard(item, false));

        spacer(24);
        section("Neu & beliebt");
        LinearLayout row2 = addRow();
        for (ContentItem item : series ? demoSeries2() : demoMovies2()) row2.addView(contentCard(item, false));
    }

    private void buildApps() {
        addSearchBar("Apps suchen …");
        spacer(24);
        section("App-Kategorien");
        LinearLayout cats = addRow();
        for (String c : new String[]{"Streaming", "Musik", "Sport", "Mediatheken", "Spiele", "Tools"}) cats.addView(genreTile(c));

        spacer(24);
        section("Installierte Apps");
        LinearLayout apps = addRow();
        for (AppEntry app : appRepository.installedTvApps()) apps.addView(appTile(app, true));

        spacer(24);
        section("Empfohlen");
        LinearLayout providers = addRow();
        for (String p : new String[]{"Netflix", "Prime Video", "Disney+", "Apple TV", "WOW", "RTL+", "YouTube"}) {
            providers.addView(providerTile(p));
        }
    }

    private View appTile(AppEntry app, boolean editable) {
        LinearLayout tile = cardLayout(dp(185), dp(112));
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(app.icon);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(58), dp(58)));
        TextView name = text(app.name, 13, false);
        name.setSingleLine(true);
        name.setGravity(Gravity.CENTER);
        tile.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(32)));
        tile.setTag("app:" + app.packageName);
        tile.setOnClickListener(v -> {
            if (!LauncherUtils.launchPackage(this, app.packageName))
                Toast.makeText(this, "App konnte nicht gestartet werden.", Toast.LENGTH_SHORT).show();
        });
        if (editable) {
            tile.setOnLongClickListener(v -> {
                toggleFavorite(app.packageName);
                return true;
            });
        }
        applyFocus(tile);
        return margin(tile, 14);
    }

    private void toggleFavorite(String pkg) {
        Set<String> fav = new HashSet<>(prefs.getStringSet("favorites", new HashSet<>()));
        boolean added;
        if (fav.contains(pkg)) {
            fav.remove(pkg);
            added = false;
        } else {
            fav.add(pkg);
            added = true;
        }
        prefs.edit().putStringSet("favorites", fav).putBoolean("favorites_edited", true).apply();
        Toast.makeText(this, added ? "Zu Favoriten hinzugefügt" : "Aus Favoriten entfernt", Toast.LENGTH_SHORT).show();
    }

    private void buildLiveTv() {
        section("Live-TV-Apps");
        LinearLayout providers = addRow();
        for (String p : new String[]{"waipu.tv", "Zattoo", "Joyn", "RTL+", "ARD", "ZDF"}) providers.addView(providerTile(p));
        spacer(26);
        section("Jetzt im TV");
        LinearLayout now = addRow();
        for (ContentItem item : Arrays.asList(
                new ContentItem("Tagesschau", "ARD", 45),
                new ContentItem("heute journal", "ZDF", 18),
                new ContentItem("Live Programm", "RTL+", 62),
                new ContentItem("Sport", "waipu.tv", 30))) {
            now.addView(contentCard(item, true));
        }
    }

    private void buildBackgrounds() {
        section("Hintergrund");
        LinearLayout modes = addRow();
        modes.addView(iconTile("▧", "Empfohlen", v -> setBackground("neutral")));
        modes.addView(iconTile("▶", "Live-Hintergründe", v -> setBackground("live")));
        modes.addView(iconTile("▣", "Eigene Bilder", v -> pickBackgroundImage()));

        spacer(28);
        section("Farben & neutrale Hintergründe");
        LinearLayout colors = addRow();
        colors.addView(colorTile("Dunkelgrau", Color.rgb(18,22,28), Color.rgb(46,52,62), false, "neutral"));
        colors.addView(colorTile("Graphit", Color.rgb(15,17,20), Color.rgb(36,40,46), false, "graphite"));
        colors.addView(colorTile("Nachtblau", Color.rgb(11,18,28), Color.rgb(30,47,65), false, "midnight"));
        colors.addView(colorTile("Live – dezent", Color.rgb(12,18,27), Color.rgb(47,60,76), true, "live"));
    }

    private View colorTile(String title, int a, int b, boolean live, String key) {
        LinearLayout tile = cardLayout(dp(210), dp(130));
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        View swatch = new View(this);
        GradientDrawable grad = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{a, b});
        grad.setCornerRadius(dp(20));
        swatch.setBackground(grad);
        tile.addView(swatch, new LinearLayout.LayoutParams(dp(120), dp(62)));
        TextView t = text(title, 14, false);
        t.setGravity(Gravity.CENTER);
        tile.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));
        tile.setOnClickListener(v -> {
            prefs.edit().putString("background_mode", key).remove("background_uri").apply();
            customBackground.setVisibility(View.GONE);
            background.setMode(a, b, live);
        });
        applyFocus(tile);
        return margin(tile, 14);
    }

    private void setBackground(String key) {
        prefs.edit().putString("background_mode", key).remove("background_uri").apply();
        customBackground.setVisibility(View.GONE);
        applySavedBackground();
    }

    private void pickBackgroundImage() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(i, PICK_BACKGROUND); }
        catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Auf diesem Fernseher ist keine Bildauswahl verfügbar.", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_BACKGROUND && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            prefs.edit().putString("background_uri", uri.toString()).putString("background_mode", "custom").apply();
            applySavedBackground();
        }
    }

    private void applySavedBackground() {
        String mode = prefs.getString("background_mode", "neutral");
        String uri = prefs.getString("background_uri", null);
        if ("custom".equals(mode) && uri != null) {
            try {
                customBackground.setImageURI(Uri.parse(uri));
                customBackground.setVisibility(View.VISIBLE);
                background.setMode(Color.rgb(10,12,16), Color.rgb(22,26,32), false);
                return;
            } catch (Exception ignored) {}
        }
        customBackground.setVisibility(View.GONE);
        if ("graphite".equals(mode)) background.setMode(Color.rgb(15,17,20), Color.rgb(36,40,46), false);
        else if ("midnight".equals(mode)) background.setMode(Color.rgb(11,18,28), Color.rgb(30,47,65), false);
        else if ("live".equals(mode)) background.setMode(Color.rgb(12,18,27), Color.rgb(47,60,76), true);
        else background.setMode(Color.rgb(18,22,28), Color.rgb(46,52,62), false);
    }

    private void showSearchDialog() {
        EditText input = new EditText(this);
        input.setHint("Filme, Serien oder Apps suchen");
        input.setSingleLine(true);
        new AlertDialog.Builder(this)
                .setTitle("Suche")
                .setView(input)
                .setPositiveButton("Suchen", (d, w) -> performSearch(input.getText().toString()))
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void addSearchBar(String hint) {
        LinearLayout row = addRow();
        row.addView(iconTile("⌕", hint, v -> showSearchDialog()));
    }

    private void performSearch(String q) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) return;
        for (AppEntry app : appRepository.installedTvApps()) {
            if (app.name.toLowerCase(Locale.ROOT).contains(query)) {
                LauncherUtils.launchPackage(this, app.packageName);
                return;
            }
        }
        Toast.makeText(this, "Keine installierte App gefunden – Store-Suche wird geöffnet.", Toast.LENGTH_SHORT).show();
        LauncherUtils.openPlayStore(this, q);
    }

    private View providerTile(String name) {
        LinearLayout tile = cardLayout(dp(175), dp(98));
        tile.setGravity(Gravity.CENTER);
        TextView t = text(name, 18, true);
        t.setGravity(Gravity.CENTER);
        tile.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tile.setOnClickListener(v -> launchProvider(name));
        applyFocus(tile);
        return margin(tile, 14);
    }

    private void launchProvider(String provider) {
        String key = provider.toLowerCase(Locale.ROOT).replace("+", "").replace(" video", "").replace(".tv", "");
        for (AppEntry app : appRepository.installedTvApps()) {
            String n = app.name.toLowerCase(Locale.ROOT);
            if (n.contains(key) || (key.equals("prime") && n.contains("amazon"))) {
                LauncherUtils.launchPackage(this, app.packageName);
                return;
            }
        }
        LauncherUtils.openPlayStore(this, provider);
    }

    private View contentCard(ContentItem item, boolean showProgress) {
        LinearLayout tile = cardLayout(dp(295), dp(showProgress ? 158 : 145));
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(16), dp(12), dp(16), dp(12));

        TextView provider = text(item.provider, 12, true);
        provider.setGravity(Gravity.RIGHT);
        provider.setTextColor(Color.rgb(222, 226, 232));
        tile.addView(provider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));

        TextView title = text(item.title, 16, true);
        title.setGravity(Gravity.BOTTOM);
        tile.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        if (showProgress) {
            FrameLayout bar = new FrameLayout(this);
            View track = new View(this);
            track.setBackgroundColor(Color.argb(90, 255, 255, 255));
            bar.addView(track, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)));
            View fill = new View(this);
            fill.setBackgroundColor(Color.WHITE);
            bar.addView(fill, new FrameLayout.LayoutParams(dp((255 * item.progress) / 100), dp(4)));
            tile.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(7)));
        }

        tile.setOnClickListener(v -> launchProvider(item.provider));
        applyFocus(tile);
        return margin(tile, 16);
    }

    private View genreTile(String title) {
        LinearLayout tile = cardLayout(dp(180), dp(84));
        tile.setGravity(Gravity.CENTER);
        TextView t = text(title, 14, true);
        t.setGravity(Gravity.CENTER);
        tile.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        applyFocus(tile);
        return margin(tile, 12);
    }

    private View iconTile(String icon, String title, View.OnClickListener click) {
        LinearLayout tile = cardLayout(dp(245), dp(150));
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        TextView i = text(icon, 37, false);
        i.setGravity(Gravity.CENTER);
        tile.addView(i, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(70)));
        TextView t = text(title, 16, false);
        t.setGravity(Gravity.CENTER);
        tile.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));
        tile.setOnClickListener(click);
        applyFocus(tile);
        return margin(tile, 16);
    }

    private LinearLayout cardLayout(int width, int height) {
        LinearLayout v = new LinearLayout(this);
        v.setFocusable(true);
        v.setId(View.generateViewId());
        v.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        v.setBackground(rounded(CARD, 28));
        v.setElevation(dp(5));
        v.setLayoutParams(new LinearLayout.LayoutParams(width, height));
        return v;
    }

    private void applyFocus(View v) {
        v.setFocusableInTouchMode(true);
        v.setOnFocusChangeListener((view, hasFocus) -> {
            view.animate()
                    .scaleX(hasFocus ? 1.055f : 1f)
                    .scaleY(hasFocus ? 1.055f : 1f)
                    .translationZ(hasFocus ? dp(12) : 0)
                    .setDuration(140)
                    .start();
            if (view instanceof LinearLayout) view.setBackground(rounded(hasFocus ? CARD_FOCUS : CARD, 28));
            if (hasFocus) {
                TvFocus.ensureVisible(view);
            }
        });
    }

    private void applyNavFocus(TextView v) {
        v.setFocusableInTouchMode(true);
        v.setOnFocusChangeListener((view, hasFocus) ->
                view.animate().scaleX(hasFocus ? 1.06f : 1f).scaleY(hasFocus ? 1.06f : 1f).setDuration(120).start());
    }

    private LinearLayout addRow() {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setClipToPadding(false);
        hsv.setClipChildren(false);
        hsv.setFocusable(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(16), dp(16), dp(16), dp(16));
        row.setClipChildren(false);
        row.setClipToPadding(false);
        rows.add(row);
        hsv.addView(row, new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(hsv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    // Explicit row navigation prevents Android's spatial search from escaping a rail.
    private void wireNavigation() {
        List<LinearLayout> nonEmpty = new ArrayList<>();
        for (LinearLayout row : rows) if (row.getChildCount() > 0) nonEmpty.add(row);
        for (int r = 0; r < nonEmpty.size(); r++) {
            LinearLayout row = nonEmpty.get(r);
            View up = r == 0 ? activeNav() : nonEmpty.get(r - 1);
            View down = r + 1 < nonEmpty.size() ? nonEmpty.get(r + 1) : null;
            TvFocus.bindRow(row, up, down);
        }
        List<View> top = new ArrayList<>(navViews);
        top.add(settings);
        for (int i = 0; i < top.size(); i++) {
            View item = top.get(i);
            View left = top.get(Math.max(0, i - 1));
            View right = top.get(Math.min(top.size() - 1, i + 1));
            item.setOnKeyListener((v, code, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (code == KeyEvent.KEYCODE_DPAD_LEFT) { left.requestFocus(); return true; }
                if (code == KeyEvent.KEYCODE_DPAD_RIGHT) { right.requestFocus(); return true; }
                if (code == KeyEvent.KEYCODE_DPAD_UP) return true;
                if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (!nonEmpty.isEmpty()) TvFocus.enterRow(nonEmpty.get(0), 0);
                    return true;
                }
                return false;
            });
        }
    }

    private View activeNav() {
        for (TextView nav : navViews) if (nav.getText().toString().equals(currentTab)) return nav;
        return navViews.get(0);
    }

    // Legacy TV remotes/API 26–32; API 33+ uses the registered OnBackInvokedCallback.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed() { handleBack(); }

    private void handleBack() {
        if (!"Home".equals(currentTab)) showTab("Home");
        activeNav().requestFocus();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("tab", currentTab);
        super.onSaveInstanceState(state);
    }

    private View margin(View v, int rightDp) {
        ViewGroup.LayoutParams raw = v.getLayoutParams();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(raw.width, raw.height);
        lp.setMargins(0, 0, dp(rightDp), 0);
        v.setLayoutParams(lp);
        return v;
    }

    private void section(String title) {
        TextView t = text(title, 19, false);
        content.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));
    }

    private void spacer(int size) {
        content.addView(new View(this), new LinearLayout.LayoutParams(1, dp(size)));
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(Color.WHITE);
        t.setTextSize(sp);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(1), Color.argb(65, 255, 255, 255));
        return g;
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private List<ContentItem> demoContinue() {
        return Arrays.asList(
                new ContentItem("The Mandalorian", "Disney+", 38),
                new ContentItem("The Last of Us", "WOW", 31),
                new ContentItem("Dune: Part Two", "Prime Video", 49),
                new ContentItem("Avatar: The Way of Water", "Disney+", 57));
    }

    private List<ContentItem> demoMovies() {
        return Arrays.asList(
                new ContentItem("Dune: Part Two", "Prime Video", 0),
                new ContentItem("Deadpool & Wolverine", "Disney+", 0),
                new ContentItem("Oppenheimer", "Prime Video", 0),
                new ContentItem("The Batman", "Netflix", 0),
                new ContentItem("Avatar: The Way of Water", "Disney+", 0));
    }

    private List<ContentItem> demoMovies2() {
        return Arrays.asList(
                new ContentItem("Alles steht Kopf 2", "Disney+", 0),
                new ContentItem("Challengers", "Prime Video", 0),
                new ContentItem("Civil War", "Prime Video", 0),
                new ContentItem("Godzilla x Kong", "WOW", 0),
                new ContentItem("The Fall Guy", "Prime Video", 0));
    }

    private List<ContentItem> demoSeries() {
        return Arrays.asList(
                new ContentItem("The Last of Us", "WOW", 0),
                new ContentItem("House of the Dragon", "WOW", 0),
                new ContentItem("Fallout", "Prime Video", 0),
                new ContentItem("Wednesday", "Netflix", 0),
                new ContentItem("The Boys", "Prime Video", 0));
    }

    private List<ContentItem> demoSeries2() {
        return Arrays.asList(
                new ContentItem("The Penguin", "WOW", 0),
                new ContentItem("Rings of Power", "Prime Video", 0),
                new ContentItem("Jack Ryan", "Prime Video", 0),
                new ContentItem("Ahsoka", "Disney+", 0),
                new ContentItem("Shrinking", "Apple TV", 0));
    }
}

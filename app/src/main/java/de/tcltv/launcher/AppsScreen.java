package de.tcltv.launcher;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Installed-app catalogue: category controls stay stable while only results change. */
public final class AppsScreen {
    public interface TileFactory { View create(AppEntry app); }
    private final Activity activity;
    private final AppRepository repository;
    private final TileFactory tileFactory;
    private final Runnable rowsChanged;
    private final LinearLayout root;
    private final List<LinearLayout> rows = new ArrayList<>();
    private final LinearLayout categories;
    private final LinearLayout results;
    private final LinearLayout suggestions;
    private final TextView resultHeading;
    private final EnumMap<AppCategories.Category, TextView> categoryViews = new EnumMap<>(AppCategories.Category.class);
    private final Map<String, EnumSet<AppCategories.Category>> classification = new HashMap<>();
    private List<AppEntry> apps;
    private AppCategories.Category selected = AppCategories.Category.ALL;

    public AppsScreen(Activity activity, AppRepository repository, TileFactory tileFactory,
            Runnable search, Runnable rowsChanged) {
        this.activity = activity;
        this.repository = repository;
        this.tileFactory = tileFactory;
        this.rowsChanged = rowsChanged;
        try {
            selected = AppCategories.Category.valueOf(activity.getSharedPreferences("tcl_tv", Context.MODE_PRIVATE)
                    .getString("apps_category", AppCategories.Category.ALL.name()));
        } catch (IllegalArgumentException ignored) { }
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setClipChildren(false);
        root.setClipToPadding(false);
        LinearLayout searchRow = row();
        searchRow.addView(button("⌕   Apps suchen", 230, search));
        heading("App-Kategorien");
        categories = row();
        for (AppCategories.Category category : AppCategories.Category.values()) {
            TextView chip = button(category.label, category == AppCategories.Category.MOVIES ? 200 : 175,
                    () -> select(category));
            chip.setTag("app-category:" + category.name());
            categoryViews.put(category, chip);
            categories.addView(chip);
        }
        resultHeading = heading("Installierte Apps");
        results = row();
        heading("Vorschläge");
        suggestions = row();
        refresh();
    }

    public LinearLayout root() { return root; }
    public List<LinearLayout> rows() { return new ArrayList<>(rows); }
    public AppCategories.Category selectedCategory() { return selected; }

    public void select(AppCategories.Category category) {
        selected = category;
        activity.getSharedPreferences("tcl_tv", Context.MODE_PRIVATE).edit()
                .putString("apps_category", category.name()).apply();
        render(false);
        TextView chosen = categoryViews.get(category);
        results.post(() -> {
            if (selected == category && chosen.hasFocus() && results.getChildCount() > 0)
                TvFocus.reveal(results.getChildAt(0), View.FOCUS_DOWN);
        });
    }

    /** Called on package-list changes; focus is retained by package when possible. */
    public void refresh() {
        apps = repository.installedTvApps();
        classification.clear();
        for (AppEntry app : apps) classification.put(app.packageName,
                AppCategories.categories(app, activity.getPackageManager()));
        render(true);
    }

    private void render(boolean preserveFocus) {
        View previous = results.findFocus();
        Object focusedTag = previous == null ? null : previous.getTag();
        for (Map.Entry<AppCategories.Category, TextView> entry : categoryViews.entrySet()) {
            boolean active = entry.getKey() == selected;
            entry.getValue().setTextColor(active ? Color.BLACK : Color.WHITE);
            entry.getValue().setSelected(active);
            entry.getValue().setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
            decorate(entry.getValue(), entry.getValue().hasFocus());
        }
        results.removeAllViews();
        for (AppEntry app : apps) {
            if (classification.get(app.packageName).contains(selected)) results.addView(tileFactory.create(app));
        }
        int count = results.getChildCount();
        resultHeading.setText(selected.label + " · " + count + (count == 1 ? " App" : " Apps"));
        if (count == 0) {
            TextView empty = button("Keine installierten Apps · Kategorie ändern", 370,
                    () -> TvFocus.reveal(categoryViews.get(selected), View.FOCUS_UP));
            empty.setTag("apps-empty");
            results.addView(empty);
        }
        renderSuggestions();
        if (rowsChanged != null) rowsChanged.run();
        if (preserveFocus && previous != null) {
            View target = focusedTag == null ? null : results.findViewWithTag(focusedTag);
            TvFocus.reveal(target == null ? results.getChildAt(0) : target, View.FOCUS_DOWN);
        }
    }

    private void renderSuggestions() {
        suggestions.removeAllViews();
        int count = 0;
        for (AppEntry app : apps) {
            EnumSet<AppCategories.Category> groups = classification.get(app.packageName);
            if (groups == null || !groups.contains(AppCategories.Category.RECOMMENDED)) continue;
            suggestions.addView(tileFactory.create(app));
            if (++count == 12) break;
        }
        if (count == 0) {
            for (AppEntry app : apps) {
                suggestions.addView(tileFactory.create(app));
                if (++count == 8) break;
            }
        }
    }

    /** BACK first returns from results to their active category. */
    public boolean onBack() {
        if (!results.hasFocus()) return false;
        TvFocus.reveal(categoryViews.get(selected), View.FOCUS_UP);
        return true;
    }

    private LinearLayout row() {
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setFocusable(false);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        LinearLayout row = new LinearLayout(activity);
        row.setPadding(dp(16), dp(16), dp(16), dp(16));
        row.setClipChildren(false);
        row.setClipToPadding(false);
        scroll.addView(row, new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        rows.add(row);
        return row;
    }

    private TextView heading(String value) {
        TextView heading = new TextView(activity);
        heading.setText(value);
        heading.setTextSize(19);
        heading.setTextColor(Color.WHITE);
        heading.setPadding(0, dp(18), 0, dp(4));
        root.addView(heading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        return heading;
    }

    private TextView button(String label, int width, Runnable action) {
        TextView button = new TextView(activity);
        button.setText(label);
        button.setTextSize(15);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setMaxLines(2);
        button.setPadding(dp(12), dp(8), dp(12), dp(8));
        button.setId(View.generateViewId());
        button.setFocusableInTouchMode(true);
        button.setOnClickListener(view -> action.run());
        button.setElevation(dp(4));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(width), dp(76));
        params.setMargins(0, 0, dp(14), 0);
        button.setLayoutParams(params);
        button.setOnFocusChangeListener((view, focused) -> {
            decorate(button, focused);
            view.animate().scaleX(focused ? 1.055f : 1f).scaleY(focused ? 1.055f : 1f).setDuration(140).start();
            if (focused) TvFocus.ensureVisible(view);
        });
        decorate(button, false);
        return button;
    }

    private void decorate(TextView view, boolean focused) {
        GradientDrawable surface = new GradientDrawable();
        surface.setColor(view.isSelected() ? Color.WHITE : focused ? Color.rgb(62, 66, 72) : Color.rgb(42, 45, 50));
        surface.setCornerRadius(dp(24));
        surface.setStroke(dp(1), focused ? Color.WHITE : Color.argb(65, 255, 255, 255));
        view.setBackground(surface);
    }

    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
}

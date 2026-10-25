package de.tcltv.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Remote-first background editor: passive rails, neutral cards, D-pad value controls. */
public final class BackgroundScreen {
    public final LinearLayout root;
    public final List<LinearLayout> rows = new ArrayList<>();
    private final Activity activity;
    private final BackgroundController controller;
    private final Runnable pickImage;
    private final List<View> modeCards = new ArrayList<>();

    public BackgroundScreen(Activity activity, BackgroundController controller, Runnable pickImage) {
        this.activity = activity; this.controller = controller; this.pickImage = pickImage;
        root = new LinearLayout(activity); root.setOrientation(LinearLayout.VERTICAL); passive(root);
        LinearLayout categories = row("Hintergrund");
        String[] groups = {"Empfohlen", "Farben", "Verläufe", "Eigene Bilder", "Live", "Einstellungen"};
        List<View> groupButtons = new ArrayList<>();
        for (String group : groups) {
            View button = card(group, null, () -> {}); categories.addView(button); groupButtons.add(button);
        }
        for (int groupIndex = 0; groupIndex < groups.length; groupIndex++) {
            String group = groups[groupIndex];
            if ("Eigene Bilder".equals(group)) {
                LinearLayout images = row("Eigene Bilder");
                images.addView(card("Bild auswählen / austauschen", null, pickImage));
                images.addView(card("Gespeichertes Bild verwenden", null, () -> { controller.useImage(); updateSelection(); }));
                images.addView(card("Bild entfernen", null, () -> { controller.clearImage(); updateSelection(); }));
                TextView scale = valueCard("Bildskalierung", () -> "fit".equals(controller.option("scale", "crop")) ? "Einpassen" : "Bild füllen",
                        () -> controller.setOption("scale", "fit".equals(controller.option("scale", "crop")) ? "crop" : "fit"));
                images.addView(scale);
                bindCategory(groupButtons.get(groupIndex), images);
            } else if ("Einstellungen".equals(group)) {
                LinearLayout brightness = row("Bild und Darstellung");
                Adjustment dim = adjustment("Abdunklung", controller.value("dim", 20), 0, 100, 5, "%", v -> controller.setValue("dim", v));
                brightness.addView(dim);
                LinearLayout blur = row(null);
                blur.addView(adjustment("Bild weichzeichnen", controller.value("blur", 0), 0, 100, 5, "%", v -> controller.setValue("blur", v)));
                LinearLayout animation = row("Animation");
                animation.addView(valueCard("Animation", () -> controller.enabled("animation", true) ? "Ein" : "Aus",
                        () -> controller.setEnabled("animation", !controller.enabled("animation", true))));
                animation.addView(valueCard("Geschwindigkeit", () -> new String[]{"Langsam", "Normal", "Schnell"}[bounded(controller.value("speed", 0), 0, 2)],
                        () -> controller.setValue("speed", (controller.value("speed", 0) + 1) % 3)));
                animation.addView(valueCard("Verlaufsrichtung", () -> new String[]{"Diagonal", "Horizontal", "Vertikal"}[bounded(controller.value("direction", 0), 0, 2)],
                        () -> controller.setValue("direction", (controller.value("direction", 0) + 1) % 3)));
                LinearLayout rotate = row("Automatischer Wechsel");
                rotate.addView(valueCard("Automatischer Wechsel", () -> controller.enabled("auto", false) ? "Ein" : "Aus",
                        () -> controller.setEnabled("auto", !controller.enabled("auto", false))));
                rotate.addView(valueCard("Zeitpunkt", () -> rotationLabel(controller.option("rotation", "startup")), () -> {
                    String current = controller.option("rotation", "startup");
                    controller.setOption("rotation", "startup".equals(current) ? "daily" : "daily".equals(current) ? "hours" : "startup");
                }));
                LinearLayout hours = row(null);
                hours.addView(adjustment("Intervall", controller.value("hours", 6), 1, 24, 1, " Stunden", v -> controller.setValue("hours", v)));
                bindCategory(groupButtons.get(groupIndex), brightness);
            } else {
                LinearLayout presets = row("Live".equals(group) ? "Ruhige Live-Hintergründe" : group);
                for (BackgroundPresets.Preset preset : BackgroundPresets.ALL) if (group.equals(preset.group)) {
                    View tile = card(preset.name, new int[]{preset.start, preset.end}, () -> {
                        controller.choosePreset(preset.id); updateSelection();
                    });
                    tile.setTag("background:" + preset.id); presets.addView(tile); modeCards.add(tile);
                }
                if ("Farben".equals(group)) presets.addView(card("Eigene Farbe", null, () -> colorPicker("Eigene Farbe",
                        controller.value("custom_color", Color.rgb(35, 38, 42)), controller::setCustomColor)));
                if ("Verläufe".equals(group)) presets.addView(card("Eigener Verlauf", null, () -> colorPicker("Startfarbe",
                        controller.value("gradient_start", Color.rgb(31, 40, 53)), start -> colorPicker("Endfarbe",
                                controller.value("gradient_end", Color.rgb(10, 12, 16)), end -> controller.setCustomGradient(start, end)))));
                bindCategory(groupButtons.get(groupIndex), presets);
            }
        }
        updateSelection();
    }

    private void bindCategory(View category, LinearLayout target) {
        category.setOnClickListener(v -> {
            for (int i = 0; i < ((ViewGroup)category.getParent()).getChildCount(); i++)
                ((ViewGroup)category.getParent()).getChildAt(i).setSelected(false);
            category.setSelected(true); style(category, category.hasFocus());
            if (target.getChildCount() > 0) TvFocus.reveal(target.getChildAt(0), View.FOCUS_DOWN);
        });
    }
    private void updateSelection() {
        for (View card : modeCards) {
            card.setSelected(("background:" + controller.mode()).equals(card.getTag()));
            style(card, card.hasFocus());
        }
    }
    private String rotationLabel(String option) {
        return "daily".equals(option) ? "Täglich" : "hours".equals(option) ? "Alle X Stunden" : "Bei App-Start";
    }

    private LinearLayout row(String title) {
        if (title != null) {
            TextView label = text(title, 19); label.setPadding(dp(4), dp(20), 0, dp(8));
            root.addView(label, new LinearLayout.LayoutParams(-1, -2));
        }
        HorizontalScrollView scroll = new HorizontalScrollView(activity); passive(scroll);
        scroll.setHorizontalScrollBarEnabled(false); scroll.setClipChildren(false); scroll.setClipToPadding(false);
        LinearLayout row = new LinearLayout(activity); row.setOrientation(LinearLayout.HORIZONTAL); passive(row);
        row.setPadding(dp(16), dp(16), dp(16), dp(16));
        scroll.addView(row, new HorizontalScrollView.LayoutParams(-2, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, -2)); rows.add(row);
        return row;
    }
    private View card(String title, int[] preview, Runnable action) {
        LinearLayout tile = new LinearLayout(activity); tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER); tile.setPadding(dp(12), dp(12), dp(12), dp(10));
        tile.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        tile.setId(View.generateViewId()); tile.setFocusable(true); tile.setFocusableInTouchMode(true);
        tile.setClickable(true);
        if (preview != null) {
            View swatch = new View(activity);
            GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR, preview); gradient.setCornerRadius(dp(12));
            swatch.setBackground(gradient); tile.addView(swatch, new LinearLayout.LayoutParams(dp(105), dp(46)));
        }
        TextView label = text(title, 14); label.setGravity(Gravity.CENTER); label.setMaxLines(2);
        tile.addView(label, new LinearLayout.LayoutParams(-1, 0, 1));
        tile.setOnClickListener(v -> action.run()); focus(tile);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(206), dp(118)); lp.rightMargin = dp(14); tile.setLayoutParams(lp);
        return tile;
    }
    private TextView valueCard(String title, java.util.function.Supplier<String> value, Runnable change) {
        TextView button = text(title + "\n" + value.get(), 15); button.setGravity(Gravity.CENTER); button.setMaxLines(3);
        button.setId(View.generateViewId()); button.setFocusable(true); button.setFocusableInTouchMode(true); button.setClickable(true);
        button.setOnClickListener(v -> { change.run(); button.setText(title + "\n" + value.get()); }); focus(button);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(240), dp(96)); lp.rightMargin = dp(14); button.setLayoutParams(lp);
        return button;
    }
    private Adjustment adjustment(String label, int value, int min, int max, int step, String suffix, IntConsumer onChange) {
        Adjustment control = new Adjustment(activity, label, value, min, max, step, suffix, onChange);
        focus(control); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(480), dp(80)); lp.rightMargin = dp(14); control.setLayoutParams(lp);
        return control;
    }

    /** Owns LEFT/RIGHT before a rail listener sees them; UP/DOWN retain shared row navigation. */
    public static final class Adjustment extends TextView {
        private final String label, suffix;
        private final int min, max, step;
        private final IntConsumer callback;
        private int value;
        public Adjustment(Activity activity, String label, int value, int min, int max, int step, String suffix, IntConsumer callback) {
            super(activity); this.label = label; this.min = min; this.max = max; this.step = step;
            this.suffix = suffix; this.callback = callback; this.value = bounded(value, min, max);
            setId(View.generateViewId()); setFocusable(true); setFocusableInTouchMode(true);
            setTextColor(Color.WHITE); setTextSize(17); setGravity(Gravity.CENTER); updateLabel();
        }
        public int value() { return value; }
        private void updateLabel() { setText(label + "   ‹  " + value + suffix + "  ›"); setContentDescription(label + ": " + value + suffix); }
        @Override public boolean dispatchKeyEvent(KeyEvent event) {
            int code = event.getKeyCode();
            if (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    int next = bounded(value + (code == KeyEvent.KEYCODE_DPAD_RIGHT ? step : -step), min, max);
                    if (next != value) { value = next; updateLabel(); callback.accept(value); }
                }
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
    }

    private void colorPicker(String title, int initial, IntConsumer save) {
        Dialog dialog = new Dialog(activity); dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(activity); panel.setOrientation(LinearLayout.VERTICAL); passive(panel);
        panel.setPadding(dp(28), dp(20), dp(28), dp(24)); panel.setBackground(surface(Color.rgb(24, 27, 31), 24));
        TextView heading = text(title, 22); panel.addView(heading, new LinearLayout.LayoutParams(-1, dp(38)));
        View preview = new View(activity); panel.addView(preview, new LinearLayout.LayoutParams(-1, dp(42)));
        int[] rgb = {Color.red(initial), Color.green(initial), Color.blue(initial)};
        preview.setBackground(surface(initial, 12));
        List<LinearLayout> dialogRows = new ArrayList<>();
        String[] labels = {"Rot", "Grün", "Blau"};
        for (int i = 0; i < 3; i++) {
            int channel = i;
            LinearLayout row = new LinearLayout(activity); passive(row);
            Adjustment slider = adjustment(labels[i], rgb[i], 0, 255, 5, "", value -> {
                rgb[channel] = value; preview.setBackground(surface(Color.rgb(rgb[0], rgb[1], rgb[2]), 12));
            });
            slider.getLayoutParams().height = dp(64);
            row.addView(slider); panel.addView(row, new LinearLayout.LayoutParams(-1, dp(72))); dialogRows.add(row);
        }
        LinearLayout actions = new LinearLayout(activity); passive(actions);
        TextView apply = valueCard("Übernehmen", () -> "", () -> { dialog.dismiss(); save.accept(Color.rgb(rgb[0], rgb[1], rgb[2])); updateSelection(); });
        TextView cancel = valueCard("Abbrechen", () -> "", dialog::dismiss);
        apply.getLayoutParams().height = cancel.getLayoutParams().height = dp(80);
        actions.addView(apply); actions.addView(cancel); panel.addView(actions); dialogRows.add(actions);
        for (int i = 0; i < dialogRows.size(); i++) TvFocus.bindRow(dialogRows.get(i), i == 0 ? dialogRows.get(0) : dialogRows.get(i - 1),
                i + 1 == dialogRows.size() ? null : dialogRows.get(i + 1));
        dialog.setContentView(panel);
        Window window = dialog.getWindow();
        if (window != null) { window.setBackgroundDrawableResource(android.R.color.transparent); window.setLayout(dp(620), -2); }
        dialog.show();
        if (window != null) window.setLayout(dp(620), -2);
        dialogRows.get(0).getChildAt(0).requestFocus();
    }
    private void passive(ViewGroup parent) {
        parent.setFocusable(false); parent.setFocusableInTouchMode(false);
        parent.setClipChildren(false); parent.setClipToPadding(false); parent.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
    }
    private TextView text(String value, int size) {
        TextView text = new TextView(activity); text.setText(value); text.setTextSize(size); text.setTextColor(Color.WHITE);
        text.setTypeface(null, Typeface.NORMAL); return text;
    }
    private void focus(View view) {
        view.setOnFocusChangeListener((v, hasFocus) -> {
            style(v, hasFocus); v.animate().scaleX(hasFocus ? 1.035f : 1).scaleY(hasFocus ? 1.035f : 1).setDuration(130).start();
            if (hasFocus) TvFocus.ensureVisible(v);
        }); style(view, false);
    }
    private void style(View view, boolean focused) {
        GradientDrawable background = surface(focused ? Color.rgb(58, 62, 68) : Color.rgb(36, 39, 44), 24);
        background.setStroke(dp(1), focused || view.isSelected() ? Color.WHITE : Color.argb(55, 255, 255, 255));
        view.setBackground(background); view.setElevation(dp(focused ? 7 : 3));
    }
    private GradientDrawable surface(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius)); return drawable;
    }
    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    private static int bounded(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}

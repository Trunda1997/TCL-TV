package de.tcltv.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A TV search with explicit edit/submit/cancel focus targets, independent of AlertDialog buttons. */
public final class TvSearchDialog extends Dialog {
    private final Activity activity;
    private final AppRepository apps;
    private final View opener;
    private SearchInput input;
    private LinearLayout suggestions;
    private TextView submit;
    private TextView cancel;
    private List<AppEntry> installed;
    private boolean editing;

    public TvSearchDialog(Activity activity, AppRepository apps) {
        super(activity);
        this.activity = activity;
        this.apps = apps;
        opener = activity.getWindow().getDecorView().findFocus();
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(28), dp(26), dp(28), dp(26));
        panel.setClipChildren(false);
        panel.setClipToPadding(false);
        panel.setBackground(surface(Color.rgb(28, 31, 35), false));
        TextView title = new TextView(activity);
        title.setText("Apps suchen");
        title.setTextSize(25);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        panel.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        input = new SearchInput(activity);
        input.setTag("tv-search-input");
        input.setId(View.generateViewId());
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.LTGRAY);
        input.setHint("Name einer installierten App");
        input.setTextSize(20);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        input.setFocusableInTouchMode(true);
        input.setShowSoftInputOnFocus(false);
        input.setSelectAllOnFocus(false);
        input.setPadding(dp(20), dp(10), dp(20), dp(10));
        input.setBackground(surface(Color.rgb(44, 48, 54), false));
        panel.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72)));
        suggestions = new LinearLayout(activity);
        suggestions.setOrientation(LinearLayout.VERTICAL);
        suggestions.setClipChildren(false);
        suggestions.setClipToPadding(false);
        panel.addView(suggestions, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView hint = new TextView(activity);
        hint.setText("Tippen zeigt Treffer · ↓: auswählen · Enter: ersten Treffer öffnen");
        hint.setTextColor(Color.LTGRAY);
        hint.setTextSize(13);
        hint.setPadding(0, dp(12), 0, dp(8));
        panel.addView(hint, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
        LinearLayout actions = new LinearLayout(activity);
        actions.setClipChildren(false);
        actions.setClipToPadding(false);
        actions.setPadding(dp(8), dp(10), dp(8), dp(10));
        submit = action("Suchen", "tv-search-submit", this::submit);
        cancel = action("Abbrechen", "tv-search-cancel", this::dismiss);
        LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, dp(62), 1);
        first.setMargins(0, 0, dp(16), 0);
        actions.addView(submit, first);
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(62), 1));
        panel.addView(actions);
        setContentView(panel);
        installed = apps.installedTvApps();
        wireFocus();
        input.setOnClickListener(view -> edit());
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) {
                updateSuggestions(value == null ? "" : value.toString());
            }
            @Override public void afterTextChanged(Editable value) { }
        });
        input.setOnEditorActionListener((view, action, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (action == EditorInfo.IME_ACTION_SEARCH || enter) {
                if (!enter || event.getAction() == KeyEvent.ACTION_UP) submit();
                return true;
            }
            return false;
        });
        input.setOnFocusChangeListener((view, focused) -> input.setBackground(surface(Color.rgb(44, 48, 54), focused)));
        setOnDismissListener(dialog -> {
            hideKeyboard();
            if (opener != null) opener.post(() -> {
                if (opener.isAttachedToWindow() && opener.isShown()) TvFocus.reveal(opener, View.FOCUS_DOWN);
            });
        });
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.7f);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            if (Build.VERSION.SDK_INT >= 33)
                window.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                        android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::back);
        }
    }

    @Override public void show() {
        super.show();
        Window window = getWindow();
        if (window != null) window.setLayout(Math.min(dp(760),
                activity.getResources().getDisplayMetrics().widthPixels - dp(96)), ViewGroup.LayoutParams.WRAP_CONTENT);
        input.requestFocus();
    }

    private void wireFocus() {
        input.setNextFocusDownId(submit.getId());
        submit.setNextFocusUpId(input.getId());
        cancel.setNextFocusUpId(input.getId());
        submit.setNextFocusRightId(cancel.getId());
        cancel.setNextFocusLeftId(submit.getId());
        input.setOnKeyListener((view, key, event) -> {
            if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) focusFirstResult();
                return true;
            }
            if (key == KeyEvent.KEYCODE_DPAD_UP) return true;
            if (key == KeyEvent.KEYCODE_DPAD_CENTER) {
                if (event.getAction() == KeyEvent.ACTION_UP) edit();
                return true;
            }
            // LEFT/RIGHT remain native cursor movement while editing, as in TCL Home.
            return false;
        });
        for (TextView button : new TextView[]{submit, cancel}) {
            button.setOnKeyListener((view, key, event) -> {
                if (key == KeyEvent.KEYCODE_DPAD_UP) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (suggestions.getChildCount() > 0)
                            suggestions.getChildAt(suggestions.getChildCount() - 1).requestFocus();
                        else input.requestFocus();
                    }
                    return true;
                }
                if (key == KeyEvent.KEYCODE_DPAD_DOWN) return true;
                if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN)
                        (key == KeyEvent.KEYCODE_DPAD_LEFT ? submit : cancel).requestFocus();
                    return true;
                }
                return false;
            });
        }
    }

    private void focusFirstResult() {
        hideKeyboard();
        if (suggestions.getChildCount() > 0) suggestions.getChildAt(0).requestFocus();
        else submit.requestFocus();
    }

    private void updateSuggestions(String query) {
        suggestions.removeAllViews();
        List<AppEntry> matches = rankedMatches(installed, query, 4);
        for (int i = 0; i < matches.size(); i++) {
            AppEntry app = matches.get(i);
            TextView result = action(app.name, "tv-search-suggestion:" + app.packageName,
                    () -> launch(app));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
            params.setMargins(0, dp(6), 0, 0);
            suggestions.addView(result, params);
        }
        wireSuggestionFocus();
    }

    private void wireSuggestionFocus() {
        for (int i = 0; i < suggestions.getChildCount(); i++) {
            View result = suggestions.getChildAt(i);
            final int index = i;
            result.setOnKeyListener((view, key, event) -> {
                if (key != KeyEvent.KEYCODE_DPAD_UP && key != KeyEvent.KEYCODE_DPAD_DOWN) return false;
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (key == KeyEvent.KEYCODE_DPAD_UP) {
                        if (index == 0) input.requestFocus();
                        else suggestions.getChildAt(index - 1).requestFocus();
                    } else if (index + 1 < suggestions.getChildCount()) {
                        suggestions.getChildAt(index + 1).requestFocus();
                    } else submit.requestFocus();
                }
                return true;
            });
        }
    }

    private void edit() {
        input.setShowSoftInputOnFocus(true);
        input.requestFocus();
        editing = true;
        InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        editing = false;
        if (input == null) return;
        input.setShowSoftInputOnFocus(false);
        InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
    }

    private void submit() {
        String query = input.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(activity, "Bitte einen App-Namen eingeben.", Toast.LENGTH_SHORT).show();
            input.requestFocus();
            return;
        }
        hideKeyboard();
        List<AppEntry> matches = rankedMatches(installed, query, 1);
        if (!matches.isEmpty()) launch(matches.get(0));
        else { LauncherUtils.openPlayStore(activity, query); dismiss(); }
    }

    private void launch(AppEntry app) {
        hideKeyboard();
        if (!LauncherUtils.launchPackage(activity, app.packageName)) {
            Toast.makeText(activity, "App konnte nicht gestartet werden.", Toast.LENGTH_SHORT).show();
            submit.requestFocus();
            return;
        }
        dismiss();
    }

    static List<AppEntry> rankedMatches(List<AppEntry> entries, String query, int limit) {
        List<AppEntry> result = new ArrayList<>();
        if (entries == null || limit <= 0) return result;
        String needle = AppCategories.normalize(query);
        if (needle.isEmpty()) return result;
        Map<String, AppEntry> exact = new LinkedHashMap<>(), prefix = new LinkedHashMap<>(), contains = new LinkedHashMap<>();
        for (AppEntry app : entries) {
            String name = AppCategories.normalize(app.name);
            String pkg = AppCategories.normalize(app.packageName);
            if (name.equals(needle) || pkg.equals(needle)) exact.put(app.packageName, app);
            else if (name.startsWith(needle)) prefix.put(app.packageName, app);
            else if (name.contains(needle) || pkg.contains(needle)) contains.put(app.packageName, app);
        }
        for (Map<String, AppEntry> group : Arrays.asList(exact, prefix, contains)) {
            for (AppEntry app : group.values()) {
                result.add(app);
                if (result.size() == limit) return result;
            }
        }
        return result;
    }

    private void back() {
        if (editing) { hideKeyboard(); input.requestFocus(); }
        else dismiss();
    }

    // Modern Android uses the registered native callback; old TCL firmware calls this method.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed() { back(); }

    private TextView action(String label, String tag, Runnable clicked) {
        TextView button = new TextView(activity);
        button.setId(View.generateViewId());
        button.setTag(tag);
        button.setText(label);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(Color.WHITE);
        button.setTextSize(18);
        button.setTypeface(null, Typeface.BOLD);
        button.setFocusableInTouchMode(true);
        button.setOnClickListener(view -> clicked.run());
        button.setBackground(surface(Color.rgb(48, 52, 58), false));
        button.setOnFocusChangeListener((view, focused) -> {
            view.setBackground(surface(focused ? Color.rgb(70, 75, 82) : Color.rgb(48, 52, 58), focused));
            view.animate().scaleX(focused ? 1.035f : 1f).scaleY(focused ? 1.035f : 1f).setDuration(130).start();
        });
        return button;
    }

    private GradientDrawable surface(int color, boolean focus) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(focus ? 2 : 1), focus ? Color.WHITE : Color.argb(70, 255, 255, 255));
        return drawable;
    }

    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }

    private final class SearchInput extends EditText {
        SearchInput(Context context) { super(context); }
        @Override public boolean onKeyPreIme(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK && editing) {
                if (event.getAction() == KeyEvent.ACTION_UP) { hideKeyboard(); requestFocus(); }
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && !editing) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) focusFirstResult();
                return true;
            }
            return super.onKeyPreIme(keyCode, event);
        }
    }
}

package de.tcltv.launcher;

import android.view.KeyEvent;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import java.util.Map;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** Shared deterministic D-pad navigation; never changes child order for drawing. */
public final class TvFocus {
    private static final Map<LinearLayout, WeakReference<View>> lastFocused = new WeakHashMap<>();
    private TvFocus() {}

    public static void enterRow(LinearLayout row, int fallbackIndex) {
        WeakReference<View> reference = lastFocused.get(row);
        View remembered = reference == null ? null : reference.get();
        if (remembered != null && remembered.getParent() == row) reveal(remembered, View.FOCUS_DOWN);
        else if (row.getChildCount() > 0)
            reveal(row.getChildAt(Math.min(fallbackIndex, row.getChildCount() - 1)), View.FOCUS_DOWN);
    }

    // Adapted from TCL Home FocusNavigator.reveal: wait for layout and ignore stale requests.
    public static void reveal(View target, int direction) {
        if (target != null && target.requestFocus(direction)) ensureVisible(target);
    }

    public static void ensureVisible(View target) {
        target.post(() -> {
            if (!target.hasFocus()) return;
            int margin = Math.round(14 * target.getResources().getDisplayMetrics().density);
            target.requestRectangleOnScreen(new Rect(-margin, -margin,
                    target.getWidth() + margin, target.getHeight() + margin), false);
        });
    }

    public static void bindRow(LinearLayout row, View up, View down) {
        row.setFocusable(false);
        row.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        for (int i = 0; i < row.getChildCount(); i++) {
            View card = row.getChildAt(i);
            card.setFocusableInTouchMode(true);
            card.setNextFocusLeftId(row.getChildAt(Math.max(0, i - 1)).getId());
            card.setNextFocusRightId(row.getChildAt(Math.min(row.getChildCount() - 1, i + 1)).getId());
            card.setOnKeyListener((view, key, event) -> {
                boolean direction = key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT
                        || key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN;
                if (!direction) return false;
                if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
                int index = row.indexOfChild(view);
                lastFocused.put(row, new WeakReference<>(view));
                if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    int next = index + (key == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
                    if (next >= 0 && next < row.getChildCount()) {
                        View target = row.getChildAt(next);
                        lastFocused.put(row, new WeakReference<>(target));
                        reveal(target, key == KeyEvent.KEYCODE_DPAD_RIGHT ? View.FOCUS_RIGHT : View.FOCUS_LEFT);
                    }
                    return true; // At rail edges stay on the current card.
                }
                if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN) {
                    View target = key == KeyEvent.KEYCODE_DPAD_UP ? up : down;
                    if (target instanceof LinearLayout) enterRow((LinearLayout) target, index);
                    else if (target != null) reveal(target, key == KeyEvent.KEYCODE_DPAD_UP ? View.FOCUS_UP : View.FOCUS_DOWN);
                    return true;
                }
                return false;
            });
        }
    }
}

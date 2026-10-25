package de.tcltv.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Owns persistent background state, asynchronous image processing and lifecycle scheduling. */
public final class BackgroundController {
    private final Activity activity;
    private final SharedPreferences prefs;
    private final AnimatedBackgroundView animated;
    private final ImageView image;
    private final View dim;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private boolean running, firstStart = true, destroyed;
    private int generation;
    private String renderedUri;
    private int renderedBlur = -1;
    private Bitmap sourceBitmap; // Accessed only on the image worker.
    private String sourceUri;
    private final Runnable rotate = () -> { if (running) { rotateIfDue(false); scheduleRotation(); } };
    private Runnable loadImage;

    public BackgroundController(Activity activity, FrameLayout root) {
        this.activity = activity;
        prefs = activity.getSharedPreferences("tcl_tv", Activity.MODE_PRIVATE);
        animated = new AnimatedBackgroundView(activity);
        image = new ImageView(activity);
        image.setFocusable(false);
        dim = new View(activity);
        dim.setBackgroundColor(Color.BLACK);
        dim.setFocusable(false);
        root.addView(animated, new FrameLayout.LayoutParams(-1, -1));
        root.addView(image, new FrameLayout.LayoutParams(-1, -1));
        root.addView(dim, new FrameLayout.LayoutParams(-1, -1));
        apply();
    }

    public void start() {
        if (destroyed) return;
        running = true;
        rotateIfDue(firstStart);
        firstStart = false;
        apply();
        scheduleRotation();
    }
    public void stop() {
        running = false;
        animated.setRunning(false);
        handler.removeCallbacks(rotate);
    }
    public void destroy() {
        destroyed = true; stop(); generation++;
        if (loadImage != null) handler.removeCallbacks(loadImage);
        worker.shutdownNow(); image.setImageDrawable(null);
    }
    public String mode() { return prefs.getString("background_mode", "neutral"); }
    public String imageUri() { return prefs.getString("background_uri", null); }
    public int value(String key, int fallback) { return prefs.getInt("background_" + key, fallback); }
    public boolean enabled(String key, boolean fallback) { return prefs.getBoolean("background_" + key, fallback); }
    public String option(String key, String fallback) { return prefs.getString("background_" + key, fallback); }

    public void setValue(String key, int value) {
        prefs.edit().putInt("background_" + key, value).apply(); apply();
    }
    public void setEnabled(String key, boolean value) {
        prefs.edit().putBoolean("background_" + key, value).apply(); apply(); scheduleRotation();
    }
    public void setOption(String key, String value) {
        prefs.edit().putString("background_" + key, value).apply(); apply(); scheduleRotation();
    }
    public void choosePreset(String id) {
        prefs.edit().putString("background_mode", id).apply(); apply();
    }
    public void setCustomColor(int color) {
        prefs.edit().putInt("background_custom_color", color).putString("background_mode", "custom_color").apply(); apply();
    }
    public void setCustomGradient(int start, int end) {
        prefs.edit().putInt("background_gradient_start", start).putInt("background_gradient_end", end)
                .putString("background_mode", "custom_gradient").apply(); apply();
    }
    public void useImage() {
        if (imageUri() != null) { prefs.edit().putString("background_mode", "custom").apply(); apply(); }
        else Toast.makeText(activity, "Bitte zuerst ein Bild auswählen.", Toast.LENGTH_SHORT).show();
    }
    public void onImagePicked(Intent data) {
        if (data == null || data.getData() == null) return;
        Uri uri = data.getData();
        int flags = data.getFlags();
        try {
            // Pass only the permission constants accepted by ContentResolver.  The
            // document picker requests read access, while a few OEM providers also
            // return write access.
            if ((flags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0
                    && (flags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0)
                activity.getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            else if ((flags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
                activity.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            else if ((flags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0)
                activity.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (RuntimeException ignored) { /* Some local providers already expose a stable URI. */ }
        String previous = imageUri();
        prefs.edit().putString("background_uri", uri.toString()).putString("background_mode", "custom").apply();
        if (previous != null && !previous.equals(uri.toString())) release(previous);
        apply();
    }
    public void clearImage() {
        String previous = imageUri();
        SharedPreferences.Editor editor = prefs.edit().remove("background_uri");
        if ("custom".equals(mode())) editor.putString("background_mode", "neutral");
        editor.apply();
        if (previous != null) release(previous);
        generation++; renderedUri = null;
        image.setImageDrawable(null); apply();
    }
    private void release(String uri) {
        try { activity.getContentResolver().releasePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch (RuntimeException ignored) { }
    }

    public void apply() {
        if (destroyed) return;
        BackgroundPresets.Preset preset = BackgroundPresets.find(mode());
        int a = preset.start, b = preset.end;
        if ("custom_color".equals(mode())) a = b = value("custom_color", Color.rgb(35, 38, 42));
        if ("custom_gradient".equals(mode())) {
            a = value("gradient_start", Color.rgb(31, 40, 53));
            b = value("gradient_end", Color.rgb(10, 12, 16));
        }
        animated.configure(a, b, preset.live && enabled("animation", true), preset.id,
                value("speed", 0), value("direction", 0));
        animated.setRunning(running);
        dim.setAlpha(clamp(value("dim", 20)) / 100f);
        String uri = imageUri();
        boolean useImage = "custom".equals(mode()) && uri != null;
        image.setVisibility(useImage ? View.VISIBLE : View.GONE);
        image.setScaleType("fit".equals(option("scale", "crop")) ? ImageView.ScaleType.FIT_CENTER : ImageView.ScaleType.CENTER_CROP);
        if (useImage) {
            int blur = clamp(value("blur", 0));
            if (!uri.equals(renderedUri) || blur != renderedBlur) scheduleImage(uri, blur);
        }
    }

    private void scheduleImage(String uri, int blur) {
        renderedUri = uri; renderedBlur = blur;
        int request = ++generation;
        if (loadImage != null) handler.removeCallbacks(loadImage);
        loadImage = () -> {
            if (destroyed) return;
            worker.submit(() -> {
                try {
                    if (!uri.equals(sourceUri) || sourceBitmap == null) {
                        sourceBitmap = decode(Uri.parse(uri)); sourceUri = uri;
                    }
                    Bitmap bitmap = blur == 0 ? sourceBitmap : blur(sourceBitmap, blur);
                    handler.post(() -> {
                        if (!destroyed && request == generation) image.setImageBitmap(bitmap);
                    });
                } catch (Exception | OutOfMemoryError e) {
                    handler.post(() -> {
                        if (destroyed || request != generation) return;
                        image.setVisibility(View.GONE); renderedUri = null;
                        Toast.makeText(activity, "Bild ist nicht mehr verfügbar. Bitte erneut auswählen.", Toast.LENGTH_LONG).show();
                    });
                }
            });
        };
        handler.postDelayed(loadImage, 160); // Coalesce repeated D-pad blur adjustments.
    }

    private Bitmap decode(Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
        try (InputStream input = activity.getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(input, null, bounds); }
        if (bounds.outWidth < 1 || bounds.outHeight < 1) throw new IllegalArgumentException("Invalid image");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 1600) options.inSampleSize *= 2;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try (InputStream input = activity.getContentResolver().openInputStream(uri)) {
            Bitmap decoded = BitmapFactory.decodeStream(input, null, options);
            if (decoded == null) throw new IllegalArgumentException("Unsupported image");
            return decoded;
        }
    }

    /** CPU fallback works on Android 8+; bounded image size and linear box passes keep work off UI. */
    static Bitmap blur(Bitmap original, int amount) {
        if (amount <= 0) return original;
        float scale = Math.min(1f, 640f / Math.max(original.getWidth(), original.getHeight()));
        int w = Math.max(1, Math.round(original.getWidth() * scale)), h = Math.max(1, Math.round(original.getHeight() * scale));
        Bitmap small = Bitmap.createScaledBitmap(original, w, h, true);
        int[] a = new int[w * h], b = new int[a.length];
        small.getPixels(a, 0, w, 0, 0, w, h);
        int radius = Math.max(1, Math.round(clamp(amount) * .22f));
        for (int pass = 0; pass < 3; pass++) { box(a, b, w, h, radius, true); box(b, a, w, h, radius, false); }
        return Bitmap.createBitmap(a, w, h, Bitmap.Config.ARGB_8888);
    }
    private static void box(int[] in, int[] out, int w, int h, int radius, boolean horizontal) {
        int lines = horizontal ? h : w, length = horizontal ? w : h;
        int[] alpha = new int[length + 1];
        int[] red = new int[length + 1];
        int[] green = new int[length + 1];
        int[] blue = new int[length + 1];
        for (int line = 0; line < lines; line++) {
            alpha[0] = red[0] = green[0] = blue[0] = 0;
            for (int position = 0; position < length; position++) {
                int pixel = in[horizontal ? line * w + position : position * w + line];
                alpha[position + 1] = alpha[position] + Color.alpha(pixel);
                red[position + 1] = red[position] + Color.red(pixel);
                green[position + 1] = green[position] + Color.green(pixel);
                blue[position + 1] = blue[position] + Color.blue(pixel);
            }
            for (int position = 0; position < length; position++) {
                int first = Math.max(0, position - radius);
                int last = Math.min(length - 1, position + radius);
                int count = last - first + 1;
                int index = horizontal ? line * w + position : position * w + line;
                out[index] = Color.argb(
                        (alpha[last + 1] - alpha[first] + count / 2) / count,
                        (red[last + 1] - red[first] + count / 2) / count,
                        (green[last + 1] - green[first] + count / 2) / count,
                        (blue[last + 1] - blue[first] + count / 2) / count);
            }
        }
    }
    private static int clamp(int value) { return Math.max(0, Math.min(100, value)); }

    private void rotateIfDue(boolean startup) {
        if (!enabled("auto", false)) return;
        String policy = option("rotation", "startup");
        long now = System.currentTimeMillis(), last = prefs.getLong("background_last_change", 0);
        boolean due = "startup".equals(policy) ? startup
                : "daily".equals(policy) ? now / 86400000L != last / 86400000L
                : now - last >= Math.max(1, value("hours", 6)) * 3600000L;
        if (due) {
            List<BackgroundPresets.Preset> candidates = new ArrayList<>();
            for (BackgroundPresets.Preset preset : BackgroundPresets.ALL)
                if (!"Farben".equals(preset.group)) candidates.add(preset);
            int index = (prefs.getInt("background_rotation_index", -1) + 1) % candidates.size();
            prefs.edit().putInt("background_rotation_index", index).putLong("background_last_change", now)
                    .putString("background_mode", candidates.get(index).id).apply();
            apply();
        }
    }
    private void scheduleRotation() {
        handler.removeCallbacks(rotate);
        if (running && enabled("auto", false) && !"startup".equals(option("rotation", "startup")))
            handler.postDelayed(rotate, 60000); // Check only while visible; persisted time handles standby.
    }
}

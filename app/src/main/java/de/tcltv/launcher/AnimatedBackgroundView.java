package de.tcltv.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

/** Quiet procedural scenes; drawing pauses with the Activity and allocates no per-frame paths. */
public final class AnimatedBackgroundView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scenePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private int baseA = Color.rgb(17, 21, 28), baseB = Color.rgb(40, 46, 56);
    private String scene = "neutral";
    private boolean live, running;
    private int speed = 1, direction;
    private long start = SystemClock.uptimeMillis();
    private LinearGradient gradient;
    private RadialGradient glow;

    public AnimatedBackgroundView(Context context) { super(context); setFocusable(false); }

    public void setMode(int a, int b, boolean live) { configure(a, b, live, "live", 1, 0); }

    public void configure(int a, int b, boolean animate, String scene, int speed, int direction) {
        boolean changed = a != baseA || b != baseB || !this.scene.equals(scene) || this.direction != direction;
        baseA = a; baseB = b; live = animate; this.scene = scene;
        this.speed = Math.max(0, Math.min(2, speed)); this.direction = direction;
        if (changed) { rebuildShaders(); start = SystemClock.uptimeMillis(); }
        invalidate();
    }
    public void setRunning(boolean running) { this.running = running; if (running) invalidate(); }
    public boolean isAnimating() { return running && live; }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) { rebuildShaders(); }

    private void rebuildShaders() {
        float w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
        float x = direction == 1 ? w : direction == 2 ? 0 : w;
        float y = direction == 1 ? 0 : h;
        gradient = new LinearGradient(0, 0, x, y, baseA, baseB, Shader.TileMode.MIRROR);
        glow = new RadialGradient(0, 0, w * .48f,
                new int[]{Color.argb(26, 230, 235, 245), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (gradient == null) rebuildShaders();
        float time = live && running ? (SystemClock.uptimeMillis() - start) / (speed == 0 ? 30000f : speed == 2 ? 9000f : 18000f) : 0;
        float shift = (float) Math.sin(time) * getWidth() * .055f;
        canvas.save();
        canvas.translate(shift, 0);
        paint.setShader(gradient);
        canvas.drawRect(-getWidth() * .1f, 0, getWidth() * 1.1f, getHeight(), paint);
        canvas.restore();
        drawScene(canvas, time);
        if (live && running && getWindowVisibility() == VISIBLE) postInvalidateDelayed(66);
    }

    private void drawScene(Canvas canvas, float time) {
        float w = getWidth(), h = getHeight();
        scenePaint.setShader(null);
        if ("landscape".equals(scene)) {
            scenePaint.setColor(Color.argb(85, 11, 25, 19));
            path.rewind(); path.moveTo(0, h);
            path.lineTo(0, h * .76f); path.lineTo(w * .22f, h * .48f);
            path.lineTo(w * .42f, h * .72f); path.lineTo(w * .67f, h * .38f);
            path.lineTo(w, h * .77f); path.lineTo(w, h); path.close(); canvas.drawPath(path, scenePaint);
        } else if ("city".equals(scene)) {
            scenePaint.setColor(Color.argb(95, 9, 12, 19));
            for (int i = 0; i < 18; i++) {
                float x = i * w / 18f, top = h * (.56f + ((i * 7) % 9) * .025f);
                canvas.drawRect(x, top, x + w / 19f, h, scenePaint);
            }
        } else if ("water".equals(scene)) {
            scenePaint.setColor(Color.argb(12, 210, 235, 240));
            for (int i = 0; i < 7; i++) {
                float y = h * (.4f + i * .09f) + (float)Math.sin(time + i) * h * .012f;
                canvas.drawRect(0, y, w, y + h * .015f, scenePaint);
            }
        } else if ("clouds".equals(scene)) {
            scenePaint.setColor(Color.argb(15, 230, 235, 240));
            for (int i = 0; i < 4; i++) {
                float cx = w * (.08f + i * .31f) + (float)Math.sin(time * .4f) * w * .025f;
                canvas.drawOval(cx - w * .25f, h * (.18f + i * .08f), cx + w * .28f,
                        h * (.52f + i * .08f), scenePaint);
            }
        } else if ("light".equals(scene) || "abstract".equals(scene)) {
            canvas.save();
            canvas.translate(w * (.48f + (float)Math.sin(time) * .05f), h * .42f);
            scenePaint.setShader(glow);
            canvas.drawCircle(0, 0, w * .5f, scenePaint);
            canvas.restore();
        }
    }
}

package de.tcltv.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

public final class AnimatedBackgroundView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long start = System.currentTimeMillis();
    private boolean live;
    private int baseA = Color.rgb(17, 21, 28);
    private int baseB = Color.rgb(40, 46, 56);

    public AnimatedBackgroundView(Context c) { super(c); }

    public void setMode(int a, int b, boolean live) {
        this.baseA = a;
        this.baseB = b;
        this.live = live;
        this.start = System.currentTimeMillis();
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float shift = 0f;
        if (live) shift = (float)((Math.sin((System.currentTimeMillis() - start) / 5000.0) + 1.0) * 0.08);
        int b = blend(baseB, Color.rgb(72, 85, 102), shift);
        paint.setShader(new LinearGradient(0, 0, getWidth(), getHeight(), baseA, b, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        if (live) postInvalidateDelayed(33);
    }

    private int blend(int a, int b, float t) {
        return Color.rgb(
                (int)(Color.red(a) * (1-t) + Color.red(b) * t),
                (int)(Color.green(a) * (1-t) + Color.green(b) * t),
                (int)(Color.blue(a) * (1-t) + Color.blue(b) * t));
    }
}

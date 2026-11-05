package com.fonte.carnet.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** Anneau de progression avec une valeur au centre (calories, eau…). */
public class RingView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private float progress;
    private String value = "";
    private String label = "";

    public RingView(Context c) {
        super(c);
        float stroke = Ui.dp(11);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(stroke);
        track.setColor(Ui.SURFACE2);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(stroke);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setColor(Ui.FLUO);
        big.setColor(Ui.TEXT);
        big.setTypeface(Typeface.DEFAULT_BOLD);
        big.setTextAlign(Paint.Align.CENTER);
        small.setColor(Ui.MUTED);
        small.setTextAlign(Paint.Align.CENTER);
    }

    public void set(float progress, String value, String label, int color) {
        this.progress = Math.max(0f, progress);
        this.value = value;
        this.label = label;
        arc.setColor(this.progress > 1.0001f ? Ui.WARN : color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float size = Math.min(getWidth(), getHeight());
        float half = arc.getStrokeWidth() / 2f + 1;
        float left = (getWidth() - size) / 2f;
        float top = (getHeight() - size) / 2f;
        box.set(left + half, top + half, left + size - half, top + size - half);
        c.drawArc(box, 0, 360, false, track);
        float sweep = 360f * Math.min(1f, progress);
        if (sweep > 0.5f) c.drawArc(box, -90, sweep, false, arc);
        big.setTextSize(size * 0.2f);
        small.setTextSize(size * 0.095f);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        c.drawText(value, cx, cy + big.getTextSize() * 0.25f, big);
        c.drawText(label, cx, cy + big.getTextSize() * 0.25f + small.getTextSize() * 1.6f, small);
    }
}

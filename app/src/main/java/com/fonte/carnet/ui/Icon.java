package com.fonte.carnet.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Icônes au trait dessinées sur une grille de 24 × 24 (aucune ressource image nécessaire). */
public class Icon extends Drawable {
    public static final int BARBELL = 0;
    public static final int HISTORY = 1;
    public static final int PROGRAM = 2;
    public static final int TROPHY = 3;
    public static final int GEAR = 4;
    public static final int BACK = 5;
    public static final int MORE = 6;
    public static final int PLUS = 7;
    public static final int CHECK = 8;
    public static final int CHEVRON = 9;
    public static final int TIMER = 10;
    public static final int KETTLEBELL = 11;
    public static final int PULSE = 12;
    public static final int HOME = 13;
    public static final int NUTRITION = 14;
    public static final int BODY = 15;
    public static final int DROP = 16;
    public static final int FLAME = 17;
    public static final int CAMERA = 18;
    public static final int BARCODE = 19;
    public static final int RULER = 20;
    public static final int SPARK = 21;
    public static final int EDIT = 22;

    private final int type;
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private float u;
    private float ox;
    private float oy;

    public Icon(int type, int color) {
        this.type = type;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        fill.setStyle(Paint.Style.FILL);
        setColor(color);
    }

    public void setColor(int color) {
        stroke.setColor(color);
        fill.setColor(color);
        invalidateSelf();
    }

    private float x(float v) {
        return ox + v * u;
    }

    private float y(float v) {
        return oy + v * u;
    }

    private void line(float x1, float y1, float x2, float y2) {
        stroke.setStrokeWidth(2f * u);
        lineRaw(x1, y1, x2, y2);
    }

    private Canvas c;

    private void lineRaw(float x1, float y1, float x2, float y2) {
        c.drawLine(x(x1), y(y1), x(x2), y(y2), stroke);
    }

    private void box(float l, float t, float r, float b, float rad, boolean filled) {
        stroke.setStrokeWidth(2f * u);
        c.drawRoundRect(x(l), y(t), x(r), y(b), rad * u, rad * u, filled ? fill : stroke);
    }

    private void circle(float cx, float cy, float r, boolean filled) {
        stroke.setStrokeWidth(2f * u);
        c.drawCircle(x(cx), y(cy), r * u, filled ? fill : stroke);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        float s = Math.min(b.width(), b.height());
        if (s <= 0) return;
        u = s / 24f;
        ox = b.left + (b.width() - s) / 2f;
        oy = b.top + (b.height() - s) / 2f;
        c = canvas;
        switch (type) {
            case BARBELL:
                line(1.5f, 12, 22.5f, 12);
                box(5, 6, 8.2f, 18, 1.2f, true);
                box(15.8f, 6, 19, 18, 1.2f, true);
                box(2.8f, 8.5f, 5, 15.5f, 1, true);
                box(19, 8.5f, 21.2f, 15.5f, 1, true);
                break;
            case HISTORY:
                circle(12, 12, 9, false);
                line(12, 12, 12, 7);
                line(12, 12, 15.5f, 14);
                break;
            case PROGRAM:
                box(3.5f, 5, 20.5f, 20.5f, 2.5f, false);
                line(3.5f, 10, 20.5f, 10);
                line(8, 3, 8, 6.5f);
                line(16, 3, 16, 6.5f);
                circle(8, 14.8f, 1.3f, true);
                circle(12, 14.8f, 1.3f, true);
                circle(16, 14.8f, 1.3f, true);
                break;
            case TROPHY:
                stroke.setStrokeWidth(2f * u);
                path.reset();
                path.moveTo(x(7), y(4));
                path.lineTo(x(17), y(4));
                path.lineTo(x(17), y(9));
                path.cubicTo(x(17), y(14.5f), x(7), y(14.5f), x(7), y(9));
                path.close();
                c.drawPath(path, stroke);
                path.reset();
                path.moveTo(x(7), y(6));
                path.lineTo(x(4), y(6));
                path.quadTo(x(4), y(11), x(8), y(11.5f));
                path.moveTo(x(17), y(6));
                path.lineTo(x(20), y(6));
                path.quadTo(x(20), y(11), x(16), y(11.5f));
                c.drawPath(path, stroke);
                line(12, 13, 12, 17);
                box(8, 17, 16, 20.5f, 1.2f, false);
                break;
            case GEAR:
                circle(12, 12, 3, false);
                circle(12, 12, 6.8f, false);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i;
                    float cx = (float) Math.cos(a);
                    float cy = (float) Math.sin(a);
                    line(12 + cx * 7.2f, 12 + cy * 7.2f, 12 + cx * 9.8f, 12 + cy * 9.8f);
                }
                break;
            case BACK:
                line(15, 5, 8, 12);
                line(8, 12, 15, 19);
                break;
            case CHEVRON:
                line(9, 5, 16, 12);
                line(16, 12, 9, 19);
                break;
            case MORE:
                circle(5, 12, 1.9f, true);
                circle(12, 12, 1.9f, true);
                circle(19, 12, 1.9f, true);
                break;
            case PLUS:
                line(12, 5, 12, 19);
                line(5, 12, 19, 12);
                break;
            case CHECK:
                stroke.setStrokeWidth(2.6f * u);
                lineRaw(5, 12.5f, 10, 17.5f);
                lineRaw(10, 17.5f, 19, 7);
                break;
            case TIMER:
                circle(12, 13.5f, 8, false);
                line(12, 13.5f, 12, 9);
                line(10, 2.5f, 14, 2.5f);
                break;
            case KETTLEBELL:
                stroke.setStrokeWidth(2f * u);
                path.reset();
                path.moveTo(x(8), y(10));
                path.lineTo(x(8), y(7));
                path.quadTo(x(8), y(3.5f), x(12), y(3.5f));
                path.quadTo(x(16), y(3.5f), x(16), y(7));
                path.lineTo(x(16), y(10));
                c.drawPath(path, stroke);
                circle(12, 14.5f, 6.5f, false);
                line(9, 20.5f, 15, 20.5f);
                break;
            case PULSE:
                stroke.setStrokeWidth(2f * u);
                path.reset();
                path.moveTo(x(2), y(12.5f));
                path.lineTo(x(7), y(12.5f));
                path.lineTo(x(9.5f), y(6));
                path.lineTo(x(13.5f), y(19));
                path.lineTo(x(16), y(11));
                path.lineTo(x(17.5f), y(12.5f));
                path.lineTo(x(22), y(12.5f));
                c.drawPath(path, stroke);
                break;
            case HOME:
                stroke.setStrokeWidth(2f * u);
                path.reset();
                path.moveTo(x(3.5f), y(11));
                path.lineTo(x(12), y(4));
                path.lineTo(x(20.5f), y(11));
                c.drawPath(path, stroke);
                path.reset();
                path.moveTo(x(6), y(9.5f));
                path.lineTo(x(6), y(20));
                path.lineTo(x(18), y(20));
                path.lineTo(x(18), y(9.5f));
                c.drawPath(path, stroke);
                box(10, 14, 14, 20, 0.8f, false);
                break;
            case NUTRITION:
                // Fourchette et couteau.
                line(7, 3, 7, 21);
                line(4.5f, 3, 4.5f, 8);
                line(9.5f, 3, 9.5f, 8);
                stroke.setStrokeWidth(2f * u);
                path.reset();
                path.moveTo(x(4.5f), y(8));
                path.quadTo(x(4.5f), y(10.5f), x(7), y(10.5f));
                path.quadTo(x(9.5f), y(10.5f), x(9.5f), y(8));
                c.drawPath(path, stroke);
                path.reset();
                path.moveTo(x(17), y(21));
                path.lineTo(x(17), y(3));
                path.quadTo(x(20), y(5), x(20), y(11));
                path.lineTo(x(17), y(13));
                c.drawPath(path, stroke);
                break;
            case BODY:
                // Pèse-personne.
                box(3.5f, 3.5f, 20.5f, 20.5f, 4, false);
                stroke.setStrokeWidth(2f * u);
                path.reset();
                path.moveTo(x(7.5f), y(10));
                path.quadTo(x(12), y(5.5f), x(16.5f), y(10));
                c.drawPath(path, stroke);
                line(12, 10.5f, 13.6f, 7.8f);
                break;
            case DROP:
                stroke.setStrokeWidth(2f * u);
                path.reset();
                path.moveTo(x(12), y(3));
                path.cubicTo(x(9.5f), y(7), x(5.5f), y(11), x(5.5f), y(14.5f));
                path.cubicTo(x(5.5f), y(18.5f), x(8.5f), y(21), x(12), y(21));
                path.cubicTo(x(15.5f), y(21), x(18.5f), y(18.5f), x(18.5f), y(14.5f));
                path.cubicTo(x(18.5f), y(11), x(14.5f), y(7), x(12), y(3));
                path.close();
                c.drawPath(path, stroke);
                break;
            case FLAME:
                stroke.setStrokeWidth(2f * u);
                path.reset();
                path.moveTo(x(12), y(2.5f));
                path.cubicTo(x(13), y(7), x(18.5f), y(9), x(18.5f), y(14.5f));
                path.cubicTo(x(18.5f), y(18.5f), x(15.5f), y(21.5f), x(12), y(21.5f));
                path.cubicTo(x(8.5f), y(21.5f), x(5.5f), y(18.5f), x(5.5f), y(15));
                path.cubicTo(x(5.5f), y(12), x(7.5f), y(10.5f), x(8.7f), y(8.5f));
                path.cubicTo(x(9.3f), y(10.5f), x(10.3f), y(11.5f), x(11.3f), y(12));
                path.cubicTo(x(12.3f), y(9), x(12.5f), y(6), x(12), y(2.5f));
                path.close();
                c.drawPath(path, stroke);
                break;
            case CAMERA:
                box(3, 7, 21, 19.5f, 3, false);
                circle(12, 13.2f, 3.6f, false);
                stroke.setStrokeWidth(2f * u);
                path.reset();
                path.moveTo(x(8.5f), y(7));
                path.lineTo(x(10), y(4.5f));
                path.lineTo(x(14), y(4.5f));
                path.lineTo(x(15.5f), y(7));
                c.drawPath(path, stroke);
                break;
            case BARCODE:
                line(4, 5, 4, 19);
                line(7.5f, 5, 7.5f, 19);
                line(10, 5, 10, 19);
                line(14, 5, 14, 19);
                line(16.5f, 5, 16.5f, 19);
                line(20, 5, 20, 19);
                break;
            case RULER:
                stroke.setStrokeWidth(2f * u);
                path.reset();
                path.moveTo(x(3), y(16));
                path.lineTo(x(16), y(3));
                path.lineTo(x(21), y(8));
                path.lineTo(x(8), y(21));
                path.close();
                c.drawPath(path, stroke);
                line(7, 12, 9, 14);
                line(10, 9, 12, 11);
                line(13, 6, 15, 8);
                break;
            case SPARK:
                stroke.setStrokeWidth(2f * u);
                path.reset();
                path.moveTo(x(12), y(3));
                path.lineTo(x(14), y(10));
                path.lineTo(x(21), y(12));
                path.lineTo(x(14), y(14));
                path.lineTo(x(12), y(21));
                path.lineTo(x(10), y(14));
                path.lineTo(x(3), y(12));
                path.lineTo(x(10), y(10));
                path.close();
                c.drawPath(path, stroke);
                break;
            case EDIT:
                stroke.setStrokeWidth(2f * u);
                path.reset();
                path.moveTo(x(4), y(20));
                path.lineTo(x(5), y(15.5f));
                path.lineTo(x(15.5f), y(5));
                path.lineTo(x(19), y(8.5f));
                path.lineTo(x(8.5f), y(19));
                path.close();
                c.drawPath(path, stroke);
                break;
            default:
                break;
        }
        c = null;
    }

    @Override
    public int getIntrinsicWidth() {
        return Ui.dp(24);
    }

    @Override
    public int getIntrinsicHeight() {
        return Ui.dp(24);
    }

    @Override
    public void setAlpha(int alpha) {
        stroke.setAlpha(alpha);
        fill.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        stroke.setColorFilter(cf);
        fill.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}

package com.fonte.carnet.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

import com.fonte.carnet.core.Fmt;

import java.util.ArrayList;
import java.util.List;

/** Courbe de progression (une valeur par séance, espacement régulier). */
public class ChartView extends View {
    private List<double[]> points = new ArrayList<>();
    private String unit = "";
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint area = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path fillPath = new Path();

    public ChartView(Context c) {
        super(c);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Ui.dp(2.5f));
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setColor(Ui.ACCENT);
        dot.setColor(Ui.ACCENT);
        grid.setColor(Ui.LINE);
        grid.setStrokeWidth(Ui.dp(1));
        label.setColor(Ui.MUTED);
        label.setTextSize(Ui.dp(11));
        area.setStyle(Paint.Style.FILL);
    }

    /** points : {date, valeur}, triés par date. */
    public void setData(List<double[]> pts, String unit) {
        // On garde les 40 dernières séances pour rester lisible.
        int from = Math.max(0, pts.size() - 40);
        this.points = new ArrayList<>(pts.subList(from, pts.size()));
        this.unit = unit;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth();
        float h = getHeight();
        if (points.size() < 2) {
            label.setTextAlign(Paint.Align.CENTER);
            c.drawText("La courbe apparaît dès la 2e séance.", w / 2, h / 2, label);
            return;
        }
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double[] p : points) {
            min = Math.min(min, p[1]);
            max = Math.max(max, p[1]);
        }
        double span = max - min;
        if (span < 1e-6) span = Math.max(1, Math.abs(max) * 0.1);
        double lo = min - span * 0.15;
        double hi = max + span * 0.15;
        if (lo < 0 && min >= 0) lo = 0;

        float left = Ui.dp(44);
        float right = w - Ui.dp(10);
        float top = Ui.dp(12);
        float bottom = h - Ui.dp(24);

        label.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 2; i++) {
            double v = lo + (hi - lo) * i / 2.0;
            float y = (float) (bottom - (v - lo) / (hi - lo) * (bottom - top));
            c.drawLine(left, y, right, y, grid);
            c.drawText(Fmt.kg(Math.round(v * 10) / 10.0), left - Ui.dp(6), y + Ui.dp(4), label);
        }

        int n = points.size();
        path.reset();
        fillPath.reset();
        float lastX = 0;
        float lastY = 0;
        for (int i = 0; i < n; i++) {
            float x = left + (right - left) * i / (float) (n - 1);
            float y = (float) (bottom - (points.get(i)[1] - lo) / (hi - lo) * (bottom - top));
            if (i == 0) {
                path.moveTo(x, y);
                fillPath.moveTo(x, bottom);
            } else {
                path.lineTo(x, y);
            }
            fillPath.lineTo(x, y);
            lastX = x;
            lastY = y;
        }
        fillPath.lineTo(lastX, bottom);
        fillPath.close();
        area.setShader(new LinearGradient(0, top, 0, bottom, 0x55FF5A1F, 0x00FF5A1F, Shader.TileMode.CLAMP));
        c.drawPath(fillPath, area);
        c.drawPath(path, line);
        if (n <= 20) {
            for (int i = 0; i < n; i++) {
                float x = left + (right - left) * i / (float) (n - 1);
                float y = (float) (bottom - (points.get(i)[1] - lo) / (hi - lo) * (bottom - top));
                c.drawCircle(x, y, Ui.dp(3), dot);
            }
        }
        c.drawCircle(lastX, lastY, Ui.dp(5), dot);

        label.setTextAlign(Paint.Align.LEFT);
        c.drawText(Fmt.shortDay((long) points.get(0)[0]), left, h - Ui.dp(6), label);
        label.setTextAlign(Paint.Align.RIGHT);
        String last = Fmt.kg(points.get(n - 1)[1]) + " " + unit + " · " + Fmt.shortDay((long) points.get(n - 1)[0]);
        c.drawText(last, right, h - Ui.dp(6), label);
    }
}

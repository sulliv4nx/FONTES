package com.fonte.carnet.ui;

import android.view.View;
import android.widget.LinearLayout;

import com.fonte.carnet.core.BodyLog;
import com.fonte.carnet.core.Fmt;

import java.util.ArrayList;
import java.util.List;

/** Évolution d'une mensuration. */
public class MeasureHistoryScreen extends Screen {
    private final String key;

    public MeasureHistoryScreen(String key) {
        this.key = key;
    }

    @Override
    public String title() {
        return key;
    }

    @Override
    public int actionIcon() {
        return Icon.PLUS;
    }

    @Override
    public void onAction() {
        a.show(new MeasureEditScreen());
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        List<double[]> pts = new ArrayList<>();
        List<BodyLog.Measures> withKey = new ArrayList<>();
        for (BodyLog.Measures m : st.measures) {
            Double v = m.values.get(key);
            if (v == null || v <= 0) continue;
            withKey.add(m);
            pts.add(0, new double[]{m.time, v});
        }
        String unit = BodyLog.unit(key);
        if (!withKey.isEmpty()) {
            LinearLayout card = Ui.card(a);
            card.addView(Ui.bigValue(a, Fmt.kg(withKey.get(0).values.get(key)) + " " + unit, "le " + Fmt.shortDay(withKey.get(0).time)));
            ChartView chart = new ChartView(a);
            chart.setData(pts, unit);
            card.addView(chart, Ui.mlp(-1, Ui.dp(180), 0, 12, 0, 0));
            c.addView(card);
        }
        c.addView(Ui.section(a, "Historique"));
        LinearLayout list = Ui.card(a);
        int n = 0;
        for (BodyLog.Measures m : withKey) {
            if (n++ > 0) list.addView(Ui.divider(a));
            LinearLayout row = Ui.row(a, Fmt.day(m.time), Fmt.kg(m.values.get(key)) + " " + unit);
            row.setOnLongClickListener(v -> {
                Ui.confirm(a, "Supprimer cette mesure ?", key + " · " + Fmt.shortDay(m.time), "Supprimer", () -> {
                    m.values.remove(key);
                    if (m.values.isEmpty()) st.deleteMeasures(m);
                    else st.save();
                    a.refresh();
                });
                return true;
            });
            list.addView(row);
        }
        if (withKey.isEmpty()) list.addView(Ui.text(a, "Aucune mesure.", 14, Ui.MUTED));
        c.addView(list);
        c.addView(Ui.hint(a, "Appui long sur une ligne pour la supprimer."));
        return Ui.page(a, c);
    }
}

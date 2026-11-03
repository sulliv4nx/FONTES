package com.fonte.carnet.ui;

import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Programs;
import com.fonte.carnet.core.Stats;

/** Calculateur de 1RM et tableau des charges en pourcentage. */
public class CalculatorScreen extends Screen {
    private static final int[] PERCENTS = {100, 95, 90, 85, 80, 75, 70, 65, 60, 50};
    private TextView result;
    private LinearLayout table;
    private String weightText = "";
    private String repsText = "";

    @Override
    public String title() {
        return "Calculateur de max";
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        TextView intro = Ui.text(a, "Entre une série réalisée pour estimer ton max sur une répétition.", 14, Ui.MUTED);
        intro.setPadding(Ui.dp(2), 0, 0, Ui.dp(8));
        c.addView(intro);

        LinearLayout inputs = Ui.hbox(a);
        EditText w = Ui.field(a, "100", Ui.DECIMAL);
        w.setText(weightText);
        EditText r = Ui.field(a, "5", Ui.INTEGER);
        r.setText(repsText);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, -2, 1);
        p1.rightMargin = Ui.dp(10);
        inputs.addView(Ui.labelled(a, "Charge (kg)", w), p1);
        inputs.addView(Ui.labelled(a, "Répétitions", r), new LinearLayout.LayoutParams(0, -2, 1));
        c.addView(inputs);

        LinearLayout card = Ui.card(a);
        card.addView(Ui.text(a, "1RM estimé", 13, Ui.MUTED));
        result = Ui.medium(a, "—", 34, Ui.ACCENT);
        Ui.tabular(result);
        card.addView(result);
        c.addView(card);

        c.addView(Ui.section(a, "Charges par pourcentage"));
        table = Ui.card(a);
        c.addView(table);
        TextView note = Ui.text(a, "Formule d'Epley, fiable jusqu'à une dizaine de répétitions. Charges arrondies à "
                + Fmt.kg(st.settings.rounding) + " kg (modifiable dans Réglages). Les reps sont indicatives.", 12, Ui.MUTED);
        note.setPadding(0, Ui.dp(4), 0, 0);
        c.addView(note);

        Ui.onText(w, t -> {
            weightText = t;
            update();
        });
        Ui.onText(r, t -> {
            repsText = t;
            update();
        });
        update();
        return Ui.page(a, c);
    }

    private void update() {
        double w = Fmt.parse(weightText);
        int reps = Fmt.parseInt(repsText);
        table.removeAllViews();
        if (Double.isNaN(w) || w <= 0 || reps <= 0) {
            result.setText("—");
            table.addView(Ui.text(a, "Renseigne la charge et les répétitions.", 14, Ui.MUTED));
            return;
        }
        double max = Stats.e1rm(w, reps);
        result.setText(Fmt.kg(Stats.round1(max)) + " kg");
        for (int pct : PERCENTS) {
            LinearLayout row = Ui.hbox(a);
            row.setPadding(0, Ui.dp(5), 0, Ui.dp(5));
            TextView p = Ui.text(a, pct + " %", 15, Ui.MUTED);
            Ui.tabular(p);
            row.addView(p, Ui.lp(Ui.dp(64), -2));
            TextView kg = Ui.medium(a, Fmt.kg(Programs.round(max * pct / 100.0, st.settings.rounding)) + " kg", 16, Ui.TEXT);
            Ui.tabular(kg);
            row.addView(kg, Ui.weight(1));
            double estReps = pct >= 100 ? 1 : 30.0 * (100.0 / pct - 1);
            int rr = (int) Math.max(1, Math.round(estReps));
            row.addView(Ui.text(a, "≈ " + rr + " rép.", 13, Ui.MUTED));
            table.addView(row);
        }
    }
}

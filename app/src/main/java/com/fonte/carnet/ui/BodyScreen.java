package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.BodyLog;
import com.fonte.carnet.core.Days;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Nutrition;
import com.fonte.carnet.core.Stats;

import java.util.ArrayList;
import java.util.List;

/** Onglet Corps : poids, IMC, composition, mensurations. */
public class BodyScreen extends Screen {

    @Override
    public String title() {
        return "Corps";
    }

    @Override
    public int actionIcon() {
        return Icon.GEAR;
    }

    @Override
    public void onAction() {
        a.show(new ProfileScreen());
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        c.addView(weightCard());
        View comp = compositionCard();
        if (comp != null) c.addView(comp);

        c.addView(Ui.section(a, "Mensurations"));
        List<View> tiles = new ArrayList<>();
        for (String key : BodyLog.MEASURES) {
            double v = st.measure(key, 0);
            double prev = st.measure(key, 1);
            String value = v > 0 ? Fmt.kg(v) + " " + BodyLog.unit(key) : "—";
            String delta = "";
            if (v > 0 && prev > 0) {
                double d = Stats.round1(v - prev);
                delta = d == 0 ? "stable" : (d > 0 ? "+" : "") + Fmt.kg(d) + " " + BodyLog.unit(key);
            } else if (v <= 0) {
                delta = "pas encore mesuré";
            }
            LinearLayout t = Ui.vbox(a);
            t.setBackground(Ui.pressable(Ui.outline(Ui.SURFACE, Ui.LINE, 18), 18));
            t.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(14), Ui.dp(12));
            t.setOnClickListener(x -> {
                if (v > 0) a.show(new MeasureHistoryScreen(key));
                else a.show(new MeasureEditScreen());
            });
            t.addView(Ui.text(a, key, 13, Ui.MUTED));
            TextView val = Ui.medium(a, value, 18, Ui.TEXT);
            Ui.tabular(val);
            t.addView(val);
            t.addView(Ui.text(a, delta, 12, Ui.FAINT));
            tiles.add(t);
        }
        c.addView(Ui.grid(a, 2, tiles.toArray(new View[0])));
        c.addView(Ui.button(a, "+ Nouvelles mensurations", Ui.SECONDARY, v -> a.show(new MeasureEditScreen())));

        if (!st.weights.isEmpty()) {
            c.addView(Ui.section(a, "Pesées"));
            LinearLayout list = Ui.card(a);
            int n = 0;
            for (BodyLog.Weight w : st.weights) {
                if (n++ >= 12) break;
                if (n > 1) list.addView(Ui.divider(a));
                LinearLayout row = Ui.row(a, Fmt.day(w.time), Fmt.kg(w.kg) + " kg");
                row.setOnLongClickListener(v -> {
                    Ui.confirm(a, "Supprimer cette pesée ?", Fmt.kg(w.kg) + " kg · " + Fmt.shortDay(w.time), "Supprimer", () -> {
                        st.deleteWeight(w);
                        a.refresh();
                    });
                    return true;
                });
                list.addView(row);
            }
            c.addView(list);
            c.addView(Ui.hint(a, "Appui long sur une pesée pour la supprimer. Pèse-toi idéalement le matin, à jeun."));
        }
        return Ui.page(a, c);
    }

    private View weightCard() {
        LinearLayout card = Ui.card(a);
        card.addView(Ui.cardHeader(a, Icon.BODY, Ui.ACCENT, "Poids", "+ Pesée", v -> weightDialog(a, a::refresh)));
        double w = st.weight();
        if (w <= 0) {
            card.addView(Ui.hint(a, "Aucune pesée. Ajoute ton poids pour suivre ta courbe et calculer tes besoins."));
            card.addView(Ui.button(a, "Ajouter ma première pesée", Ui.PRIMARY, v -> weightDialog(a, a::refresh)));
            return card;
        }
        LinearLayout row = Ui.hbox(a);
        row.addView(Ui.bigValue(a, Fmt.kg(w) + " kg", "le " + Fmt.shortDay(st.weights.get(0).time)), Ui.weight(1));
        LinearLayout deltas = Ui.vbox(a);
        deltas.setGravity(Gravity.END);
        double w7 = st.weightDaysAgo(7);
        double w30 = st.weightDaysAgo(30);
        if (w7 > 0) deltas.addView(Ui.text(a, signed(w - w7) + " kg en 7 j", 13, Ui.MUTED));
        if (w30 > 0) deltas.addView(Ui.text(a, signed(w - w30) + " kg en 30 j", 13, Ui.MUTED));
        row.addView(deltas);
        card.addView(row);
        if (st.weights.size() >= 2) {
            List<double[]> pts = new ArrayList<>();
            for (int i = Math.min(st.weights.size(), 60) - 1; i >= 0; i--) {
                BodyLog.Weight x = st.weights.get(i);
                pts.add(new double[]{x.time, x.kg});
            }
            ChartView chart = new ChartView(a);
            chart.setData(pts, "kg");
            card.addView(chart, Ui.mlp(-1, Ui.dp(170), 0, 12, 0, 4));
        }
        double bmi = Nutrition.bmi(w, st.profile.heightCm);
        if (bmi > 0) {
            card.addView(Ui.divider(a));
            card.addView(Ui.row(a, "IMC", Fmt.kg(Stats.round1(bmi)) + " · " + Nutrition.bmiLabel(bmi)));
        } else {
            TextView t = Ui.text(a, "Renseigne ta taille dans le profil pour calculer ton IMC.", 12, Ui.MUTED);
            t.setPadding(0, Ui.dp(8), 0, 0);
            card.addView(t);
        }
        return card;
    }

    private View compositionCard() {
        double bf = st.bodyFat();
        double w = st.weight();
        if (bf <= 0) return null;
        boolean measured = st.measure(BodyLog.BODY_FAT, 0) > 0;
        LinearLayout card = Ui.card(a);
        card.addView(Ui.cardHeader(a, Icon.RULER, Ui.ACCENT, "Composition", null, null));
        card.addView(Ui.row(a, measured ? "Masse grasse (mesurée)" : "Masse grasse (estimée)", Fmt.kg(Stats.round1(bf)) + " %"));
        if (w > 0) {
            card.addView(Ui.row(a, "Masse grasse", Fmt.kg(Stats.round1(w * bf / 100)) + " kg"));
            card.addView(Ui.row(a, "Masse maigre", Fmt.kg(Stats.round1(w * (1 - bf / 100))) + " kg"));
        }
        if (!measured) {
            TextView t = Ui.text(a, "Estimation par la méthode de l'US Navy (taille, cou, tour de taille"
                    + (com.fonte.carnet.core.Profile.FEMALE.equals(st.profile.sex) ? ", hanches" : "") + "), à ± 3 à 4 points près.", 12, Ui.MUTED);
            t.setPadding(0, Ui.dp(6), 0, 0);
            card.addView(t);
        }
        return card;
    }

    private static String signed(double d) {
        double r = Stats.round1(d);
        return (r > 0 ? "+" : "") + Fmt.kg(r);
    }

    /** Saisie d'une pesée du jour. */
    static void weightDialog(MainActivity a, Runnable onDone) {
        LinearLayout box = Ui.vbox(a);
        box.setPadding(Ui.dp(20), Ui.dp(4), Ui.dp(20), 0);
        EditText kg = Ui.field(a, "ex. 78,5", Ui.DECIMAL);
        double last = a.store.weight();
        if (last > 0) kg.setText(Fmt.kg(last));
        box.addView(Ui.labelled(a, "Poids (kg)", kg));
        AlertDialog d = new AlertDialog.Builder(a)
                .setTitle("Pesée du " + Fmt.shortDay(System.currentTimeMillis()))
                .setView(box)
                .setPositiveButton("Enregistrer", null)
                .setNegativeButton("Annuler", null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            double w = Fmt.parse(kg.getText().toString());
            if (Double.isNaN(w) || w < 25 || w > 350) {
                kg.setError("Poids en kg");
                return;
            }
            a.store.addWeight(Stats.round1(w), Days.today());
            Ui.hideKeyboard(kg);
            d.dismiss();
            if (onDone != null) onDone.run();
        }));
        d.show();
    }
}

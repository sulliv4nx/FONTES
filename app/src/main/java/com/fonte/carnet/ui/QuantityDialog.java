package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Food;
import com.fonte.carnet.core.FoodEntry;
import com.fonte.carnet.core.Stats;

/** Choix de la quantité (et du repas) pour un aliment, avec aperçu des calories et macros. */
final class QuantityDialog {
    private QuantityDialog() {
    }

    private static String preview(double kcal100, double p100, double c100, double f100, double grams) {
        double k = grams / 100.0;
        return Fmt.kg(Math.round(kcal100 * k)) + " kcal · P " + Fmt.kg(Stats.round1(p100 * k)) + " g · G "
                + Fmt.kg(Stats.round1(c100 * k)) + " g · L " + Fmt.kg(Stats.round1(f100 * k)) + " g";
    }

    /** Ajout d'un aliment au journal. */
    static void add(MainActivity a, Food f, String meal, int day, String photo, Runnable onDone) {
        final String[] chosenMeal = {meal};
        LinearLayout box = Ui.vbox(a);
        box.setPadding(Ui.dp(20), Ui.dp(4), Ui.dp(20), Ui.dp(4));
        box.addView(Ui.text(a, f.per100(), 13, Ui.MUTED));
        EditText grams = Ui.field(a, "g", Ui.DECIMAL);
        double initial = f.portion > 0 ? f.portion : 100;
        grams.setText(Fmt.kg(initial));
        TextView live = Ui.medium(a, preview(f.kcal, f.protein, f.carbs, f.fat, initial), 15, Ui.ACCENT);
        Ui.tabular(live);
        Ui.onText(grams, t -> {
            double g = Fmt.parse(t);
            live.setText(preview(f.kcal, f.protein, f.carbs, f.fat, Double.isNaN(g) ? 0 : g));
        });
        box.addView(Ui.labelled(a, "Quantité (g ou ml)", grams));
        if (f.portion > 0) {
            String label = f.portionLabel == null ? "1 portion" : f.portionLabel;
            View[] chips = {
                    Ui.chip(a, label + " · " + Fmt.kg(f.portion) + " g", false, v -> grams.setText(Fmt.kg(f.portion))),
                    Ui.chip(a, "× 2", false, v -> grams.setText(Fmt.kg(f.portion * 2))),
                    Ui.chip(a, "100 g", false, v -> grams.setText("100"))};
            box.addView(Ui.chipRow(a, chips));
        }
        box.addView(live, Ui.mlp(-1, -2, 0, 4, 0, 10));
        box.addView(mealChips(a, chosenMeal));
        ScrollView sv = new ScrollView(a);
        sv.addView(box);
        AlertDialog d = new AlertDialog.Builder(a)
                .setTitle(f.name)
                .setView(sv)
                .setPositiveButton("Ajouter", null)
                .setNegativeButton("Annuler", null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            double g = Fmt.parse(grams.getText().toString());
            if (Double.isNaN(g) || g <= 0 || g > 5000) {
                grams.setError("Quantité en grammes");
                return;
            }
            FoodEntry e = FoodEntry.of(f, g);
            e.day = day;
            e.meal = chosenMeal[0];
            e.photo = photo;
            a.store.addFood(e);
            Ui.hideKeyboard(grams);
            d.dismiss();
            Ui.toast(a, "Ajouté : " + f.name + " · " + Fmt.kg(Math.round(e.kcal)) + " kcal");
            if (onDone != null) onDone.run();
        }));
        d.show();
    }

    /** Modification d'une entrée du journal (quantité et repas). */
    static void edit(MainActivity a, FoodEntry e, Runnable onDone) {
        final String[] chosenMeal = {e.meal};
        double grams0 = e.grams > 0 ? e.grams : 100;
        double k100 = e.kcal * 100 / grams0;
        double p100 = e.protein * 100 / grams0;
        double c100 = e.carbs * 100 / grams0;
        double f100 = e.fat * 100 / grams0;
        LinearLayout box = Ui.vbox(a);
        box.setPadding(Ui.dp(20), Ui.dp(4), Ui.dp(20), Ui.dp(4));
        EditText grams = Ui.field(a, "g", Ui.DECIMAL);
        grams.setText(Fmt.kg(grams0));
        TextView live = Ui.medium(a, preview(k100, p100, c100, f100, grams0), 15, Ui.ACCENT);
        Ui.tabular(live);
        Ui.onText(grams, t -> {
            double g = Fmt.parse(t);
            live.setText(preview(k100, p100, c100, f100, Double.isNaN(g) ? 0 : g));
        });
        box.addView(Ui.labelled(a, "Quantité (g ou ml)", grams));
        box.addView(live, Ui.mlp(-1, -2, 0, 4, 0, 10));
        box.addView(mealChips(a, chosenMeal));
        AlertDialog d = new AlertDialog.Builder(a)
                .setTitle(e.name)
                .setView(box)
                .setPositiveButton("Enregistrer", null)
                .setNeutralButton("Supprimer", (x, w) -> {
                    a.store.deleteFood(e);
                    if (onDone != null) onDone.run();
                })
                .setNegativeButton("Annuler", null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            double g = Fmt.parse(grams.getText().toString());
            if (Double.isNaN(g) || g <= 0 || g > 5000) {
                grams.setError("Quantité en grammes");
                return;
            }
            double k = g / 100.0;
            e.grams = g;
            e.kcal = k100 * k;
            e.protein = p100 * k;
            e.carbs = c100 * k;
            e.fat = f100 * k;
            e.meal = chosenMeal[0];
            a.store.save();
            Ui.hideKeyboard(grams);
            d.dismiss();
            if (onDone != null) onDone.run();
        }));
        d.show();
    }

    private static View mealChips(MainActivity a, String[] chosen) {
        LinearLayout row = Ui.hbox(a);
        android.widget.HorizontalScrollView h = new android.widget.HorizontalScrollView(a);
        h.setHorizontalScrollBarEnabled(false);
        h.addView(row);
        TextView[] chips = new TextView[FoodEntry.MEALS.length];
        for (int i = 0; i < FoodEntry.MEALS.length; i++) {
            String m = FoodEntry.MEALS[i];
            final int idx = i;
            chips[i] = Ui.chip(a, m, m.equals(chosen[0]), null);
            chips[i].setOnClickListener(v -> {
                chosen[0] = m;
                for (int k = 0; k < chips.length; k++) {
                    boolean sel = k == idx;
                    chips[k].setTextColor(sel ? Ui.BG : Ui.TEXT);
                    chips[k].setBackground(Ui.pressable(sel ? Ui.rect(Ui.ACCENT, 20) : Ui.outline(Ui.SURFACE, Ui.LINE, 20), 20));
                }
            });
            row.addView(chips[i]);
        }
        h.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 8));
        return h;
    }
}

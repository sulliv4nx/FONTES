package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
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
        add(a, f, null, meal, day, photo, null, onDone);
    }

    /**
     * Feuille « Quantité » : portions toutes prêtes, réglage au gramme, calories et macros en direct.
     *
     * @param source    origine des valeurs affichée sous le nom (ex. « Ciqual 2025 · valeurs moyennes »), ou null
     * @param beforeAdd transforme l'aliment juste avant l'ajout (garder un produit trouvé en ligne), ou null
     */
    static void add(MainActivity a, Food f, String source, String meal, int day, String photo,
                    java.util.function.UnaryOperator<Food> beforeAdd, Runnable onDone) {
        final String[] chosenMeal = {meal};
        boolean liquid = com.fonte.carnet.core.Portions.liquid(f);
        String unit = liquid ? " ml" : " g";
        LinearLayout box = Ui.vbox(a);

        LinearLayout head = Ui.hbox(a);
        head.setGravity(Gravity.TOP);
        LinearLayout titles = Ui.vbox(a);
        TextView title = Ui.text(a, f.name, 19, Ui.TEXT);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titles.addView(title);
        TextView src = Ui.text(a, (source == null ? "" : source + " · ") + f.per100(), 12, Ui.MUTED);
        src.setPadding(0, Ui.dp(4), 0, 0);
        titles.addView(src);
        head.addView(titles, Ui.weight(1));
        box.addView(head);

        EditText grams = Ui.field(a, liquid ? "ml" : "g", Ui.DECIMAL);
        java.util.List<com.fonte.carnet.core.Portions.Portion> portions =
                com.fonte.carnet.core.Portions.forFood(f, a.store.lastGrams(f.id));
        double initial = portions.size() > 1 ? portions.get(portions.size() - 1).grams : (f.portion > 0 ? f.portion : 100);
        for (com.fonte.carnet.core.Portions.Portion p : portions) if ("Ma portion".equals(p.label)) initial = p.grams;
        grams.setText(Fmt.kg(initial));
        grams.setGravity(Gravity.CENTER);
        grams.setTextSize(22);

        java.util.List<View> chips = new java.util.ArrayList<>();
        java.util.List<TextView> chipViews = new java.util.ArrayList<>();
        for (com.fonte.carnet.core.Portions.Portion p : portions) {
            TextView chip = Ui.chip(a, p.text(liquid), Math.abs(p.grams - initial) < 0.5, v -> grams.setText(Fmt.kg(p.grams)));
            chip.setLayoutParams(new android.view.ViewGroup.LayoutParams(-2, -2));
            chips.add(chip);
            chipViews.add(chip);
        }
        View flow = Ui.flow(a, chips.toArray(new View[0]));
        flow.setPadding(0, Ui.dp(12), 0, Ui.dp(4));
        box.addView(flow);

        LinearLayout qty = Ui.hbox(a);
        qty.setGravity(Gravity.CENTER_VERTICAL);
        qty.setPadding(0, Ui.dp(8), 0, Ui.dp(8));
        TextView minus = stepButton(a, "−", "Moins 10" + unit, () -> step(grams, -10));
        TextView plus = stepButton(a, "+", "Plus 10" + unit, () -> step(grams, 10));
        qty.addView(minus);
        LinearLayout.LayoutParams fl = new LinearLayout.LayoutParams(0, -2, 1);
        fl.leftMargin = Ui.dp(10);
        fl.rightMargin = Ui.dp(10);
        qty.addView(Ui.labelled(a, liquid ? "Quantité en millilitres" : "Quantité en grammes", grams), fl);
        qty.addView(plus);
        box.addView(qty);

        com.fonte.carnet.core.Nutrition.Macros goal = a.store.targetMacros(day);
        LinearLayout live = Ui.vbox(a);
        live.setBackground(Ui.rect(Ui.BG, 16));
        live.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(14), Ui.dp(14));
        LinearLayout kRow = Ui.hbox(a);
        kRow.setGravity(Gravity.BOTTOM);
        TextView kcal = Ui.text(a, "", 30, Ui.TEXT);
        kcal.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        Ui.tabular(kcal);
        kRow.addView(kcal);
        TextView kUnit = Ui.text(a, " kcal", 15, Ui.MUTED);
        kUnit.setPadding(0, 0, 0, Ui.dp(5));
        kRow.addView(kUnit);
        live.addView(kRow);
        LinearLayout cols = Ui.hbox(a);
        cols.setPadding(0, Ui.dp(10), 0, 0);
        MacroCol pc = new MacroCol(a, "Protéines", Ui.PROTEIN);
        MacroCol cc = new MacroCol(a, "Glucides", Ui.CARBS);
        MacroCol fc = new MacroCol(a, "Lipides", Ui.FAT);
        LinearLayout.LayoutParams mid = Ui.weight(1);
        mid.leftMargin = Ui.dp(12);
        mid.rightMargin = Ui.dp(12);
        cols.addView(pc.box, Ui.weight(1));
        cols.addView(cc.box, mid);
        cols.addView(fc.box, Ui.weight(1));
        live.addView(cols);
        TextView share = Ui.text(a, "Les barres montrent la part de ton objectif du jour.", 12, Ui.FAINT);
        share.setPadding(0, Ui.dp(10), 0, 0);
        live.addView(share);
        box.addView(live);

        Runnable update = () -> {
            double g = Fmt.parse(grams.getText().toString());
            if (Double.isNaN(g) || g < 0) g = 0;
            double k = g / 100.0;
            kcal.setText(Fmt.kg(Math.round(f.kcal * k)));
            pc.set(f.protein * k, goal.protein);
            cc.set(f.carbs * k, goal.carbs);
            fc.set(f.fat * k, goal.fat);
            for (int i = 0; i < chipViews.size(); i++) {
                boolean sel = Math.abs(portions.get(i).grams - g) < 0.5;
                chipViews.get(i).setTextColor(sel ? Ui.BG : Ui.TEXT);
                chipViews.get(i).setBackground(Ui.pressable(sel ? Ui.rect(Ui.ACCENT, 20) : Ui.outline(Ui.SURFACE, Ui.LINE, 20), 20));
            }
        };
        Ui.onText(grams, t -> update.run());
        update.run();

        View meals = mealChips(a, chosenMeal);
        meals.setPadding(0, Ui.dp(12), 0, 0);
        box.addView(meals);

        android.app.Dialog d = Ui.sheet(a, box);
        TextView edit = Ui.button(a, "Modifier", Ui.GHOST, v -> {
            d.dismiss();
            Food copy = new Food();
            copy.name = f.name;
            copy.barcode = f.barcode;
            copy.kcal = f.kcal;
            copy.protein = f.protein;
            copy.carbs = f.carbs;
            copy.fat = f.fat;
            copy.portion = f.portion;
            copy.portionLabel = f.portionLabel;
            FoodFormScreen form = new FoodFormScreen(copy, chosenMeal[0], day, photo);
            form.withNote("Change les valeurs si ton produit diffère : une copie est gardée dans tes aliments, l'original ne change pas.");
            a.show(form);
        });
        TextView addBtn = Ui.button(a, "Ajouter", Ui.PRIMARY, null);
        Runnable label = () -> addBtn.setText(addLabel(chosenMeal[0]));
        label.run();
        mealListener = label;
        addBtn.setOnClickListener(v -> {
            double g = Fmt.parse(grams.getText().toString());
            if (Double.isNaN(g) || g <= 0 || g > 5000) {
                grams.setError(liquid ? "Quantité en millilitres" : "Quantité en grammes");
                return;
            }
            Food food = beforeAdd == null ? f : beforeAdd.apply(f);
            FoodEntry e = FoodEntry.of(food, g);
            e.day = day;
            e.meal = chosenMeal[0];
            e.photo = photo;
            a.store.addFood(e);
            Ui.hideKeyboard(grams);
            d.dismiss();
            Ui.toast(a, "Ajouté : " + food.name + " · " + Fmt.kg(Math.round(e.kcal)) + " kcal");
            if (onDone != null) onDone.run();
        });
        LinearLayout buttons = Ui.hbox(a);
        buttons.setPadding(0, Ui.dp(14), 0, 0);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, -2, 1);
        l1.rightMargin = Ui.dp(8);
        buttons.addView(edit, l1);
        buttons.addView(addBtn, new LinearLayout.LayoutParams(0, -2, 2));
        box.addView(buttons);
        boolean mine = false;
        for (com.fonte.carnet.core.Portions.Portion p : portions) if ("Ma portion".equals(p.label)) mine = true;
        if (!mine) {
            TextView hint = Ui.text(a, "« Ma portion » apparaîtra ici : Fonte retient la quantité que tu prends d'habitude.", 12, Ui.FAINT);
            hint.setGravity(Gravity.CENTER);
            hint.setPadding(0, Ui.dp(10), 0, 0);
            box.addView(hint);
        }
        d.setOnDismissListener(x -> mealListener = null);
        d.show();
    }

    /** Appelé quand le repas choisi change (libellé du bouton). */
    private static Runnable mealListener;

    static String addLabel(String meal) {
        if (FoodEntry.BREAKFAST.equals(meal)) return "Ajouter au petit-déjeuner";
        if (FoodEntry.LUNCH.equals(meal)) return "Ajouter au déjeuner";
        if (FoodEntry.DINNER.equals(meal)) return "Ajouter au dîner";
        return "Ajouter en collation";
    }

    private static void step(EditText grams, double delta) {
        double g = Fmt.parse(grams.getText().toString());
        if (Double.isNaN(g)) g = 0;
        g = Math.max(0, Math.round((g + delta) / 5.0) * 5);
        grams.setText(Fmt.kg(g));
    }

    private static TextView stepButton(MainActivity a, String label, String description, Runnable action) {
        TextView t = Ui.medium(a, label, 24, Ui.TEXT);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.pressable(Ui.rect(Ui.SURFACE2, 14), 14));
        t.setContentDescription(description);
        t.setOnClickListener(v -> action.run());
        t.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(52), Ui.dp(52)));
        return t;
    }

    /** Une colonne de macro : nom, grammes et barre de la part de l'objectif du jour. */
    private static final class MacroCol {
        final LinearLayout box;
        final TextView value;
        final View fill;
        final LinearLayout bar;

        MacroCol(MainActivity a, String label, int color) {
            box = Ui.vbox(a);
            box.addView(Ui.text(a, label, 12, Ui.MUTED));
            value = Ui.medium(a, "", 15, Ui.TEXT);
            Ui.tabular(value);
            box.addView(value);
            bar = Ui.hbox(a);
            bar.setBackground(Ui.rect(Ui.SURFACE2, 3));
            bar.setWeightSum(1f);
            fill = new View(a);
            fill.setBackground(Ui.rect(color, 3));
            bar.addView(fill, new LinearLayout.LayoutParams(0, -1, 0f));
            box.addView(bar, Ui.mlp(-1, Ui.dp(6), 0, 6, 0, 0));
        }

        void set(double grams, double target) {
            value.setText(Fmt.kg(Stats.round1(grams)) + " g");
            float w = target > 0 ? (float) Math.min(1, grams / target) : 0f;
            fill.setLayoutParams(new LinearLayout.LayoutParams(0, -1, w));
        }
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
                if (mealListener != null) mealListener.run();
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

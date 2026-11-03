package com.fonte.carnet.ui;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Days;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Groceries;
import com.fonte.carnet.core.MealPlan;
import com.fonte.carnet.core.MealPlanner;
import com.fonte.carnet.core.Nutrition;
import com.fonte.carnet.core.Recipe;
import com.fonte.carnet.core.Recipes;


/** Détail d'un plat du plan : quantités, macros et préparation. */
public class RecipeScreen extends ScrollScreen {
    private MealPlan.Meal meal;
    /** Afficher les quantités à cuisiner pour les deux jours (cuisine en lot). */
    private boolean both = true;

    public RecipeScreen(MealPlan.Meal meal) {
        this.meal = meal;
    }

    @Override
    public String title() {
        return MealPlan.slotLabel(meal.slot) + " · " + MealPlanner.weekdayLong(meal.day) + " " + (meal.day % 100);
    }

    @Override
    public View build() {
        MealPlan plan = st.mealPlan;
        if (plan != null && !plan.meals.contains(meal)) {
            MealPlan.Meal same = plan.find(meal.day, meal.slot);
            if (same != null) meal = same;
        }
        Recipe r = Recipes.find(meal.recipeId);
        MealPlan.Meal partner = plan == null ? null : plan.batchPartner(meal);
        boolean twoDays = partner != null && both;
        Nutrition.Totals t = meal.totals();

        LinearLayout c = Ui.vbox(a);
        TextView h = Ui.heading(a, meal.name);
        c.addView(h);
        LinearLayout badges = Ui.hbox(a);
        badges.setLayoutParams(Ui.mlp(-1, -2, 0, 10, 0, 14));
        if (r != null) badges.addView(Ui.badge(a, r.minutes + " min", Ui.MUTED, Ui.SURFACE2));
        if (partner != null) {
            int first = Math.min(meal.day, partner.day);
            int second = Math.max(meal.day, partner.day);
            TextView b = Ui.badge(a, "Cuisine en lot · " + MealPlanner.weekdayLong(first) + " + " + MealPlanner.weekdayLong(second),
                    Ui.ACCENT, Ui.ACCENT_SOFT);
            LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
            lp.leftMargin = Ui.dp(8);
            badges.addView(b, lp);
        }
        c.addView(badges);

        c.addView(macroCard(t));
        c.addView(ingredientsCard(partner, twoDays));
        if (r != null) c.addView(stepsCard(r, partner));

        boolean future = !meal.eaten && meal.day > Days.today();
        TextView eat = Ui.smallButton(a, meal.eaten ? "✓ Mangé" : "Au journal", meal.eaten ? Ui.SECONDARY : Ui.PRIMARY, v -> {
            if (future) {
                Ui.toast(a, "Tu pourras le cocher le jour même.");
                return;
            }
            boolean now = !meal.eaten;
            st.setMealEaten(meal, now);
            if (now) Ui.toast(a, "Ajouté au journal · " + Fmt.kg(Math.round(meal.totals().kcal)) + " kcal");
            a.refresh();
        });
        eat.setMinHeight(Ui.dp(52));
        if (meal.eaten) eat.setTextColor(Ui.OK);
        TextView swap = Ui.smallButton(a, "Remplacer", Ui.GHOST, v -> SwapSheet.show(a, meal, fresh -> {
            meal = fresh;
            a.refresh();
        }));
        swap.setMinHeight(Ui.dp(52));
        c.addView(Ui.buttonRow(a, swap, eat));
        return page(c);
    }

    private View macroCard(Nutrition.Totals t) {
        LinearLayout card = Ui.card(a);
        LinearLayout kcalRow = Ui.hbox(a);
        kcalRow.setGravity(Gravity.BOTTOM);
        TextView big = Ui.text(a, Fmt.kg(Math.round(t.kcal)) + " ", 30, Ui.TEXT);
        big.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        Ui.tabular(big);
        kcalRow.addView(big);
        TextView unit = Ui.text(a, "kcal / portion", 15, Ui.MUTED);
        unit.setPadding(0, 0, 0, Ui.dp(5));
        kcalRow.addView(unit);
        card.addView(kcalRow);
        // Barre empilée : part de chaque macro dans les calories.
        double kp = t.protein * 4, kc = t.carbs * 4, kf = t.fat * 9;
        LinearLayout bar = Ui.hbox(a);
        bar.setLayoutParams(Ui.mlp(-1, Ui.dp(8), 0, 12, 0, 12));
        int[] colors = {Ui.PROTEIN, Ui.CARBS, Ui.FAT};
        double[] parts = {kp, kc, kf};
        for (int i = 0; i < 3; i++) {
            View seg = new View(a);
            seg.setBackground(Ui.rect(colors[i], 4));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, (float) Math.max(0.01, parts[i]));
            if (i > 0) lp.leftMargin = Ui.dp(2);
            bar.addView(seg, lp);
        }
        card.addView(bar);
        LinearLayout row = Ui.hbox(a);
        row.setGravity(Gravity.TOP);
        String[] names = {"Protéines", "Glucides", "Lipides"};
        double[] grams = {t.protein, t.carbs, t.fat};
        for (int i = 0; i < 3; i++) {
            LinearLayout box = Ui.vbox(a);
            TextView v = Ui.medium(a, Fmt.kg(Math.round(grams[i])) + " g", 18, Ui.TEXT);
            Ui.tabular(v);
            box.addView(v);
            LinearLayout lab = Ui.hbox(a);
            View dot = new View(a);
            dot.setBackground(Ui.rect(colors[i], 4));
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Ui.dp(8), Ui.dp(8));
            dlp.rightMargin = Ui.dp(5);
            lab.addView(dot, dlp);
            lab.addView(Ui.text(a, names[i], 13, Ui.MUTED));
            box.addView(lab);
            row.addView(box, Ui.weight(1));
        }
        card.addView(row);
        return card;
    }

    private View ingredientsCard(MealPlan.Meal partner, boolean twoDays) {
        LinearLayout card = Ui.card(a);
        LinearLayout head = Ui.hbox(a);
        head.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 4));
        TextView label = Ui.medium(a, "INGRÉDIENTS", 11, Ui.MUTED);
        label.setLetterSpacing(0.06f);
        head.addView(label, Ui.weight(1));
        if (partner != null) {
            LinearLayout seg = Ui.segmented(a, new String[]{"1 portion", "2 jours"}, both ? 1 : 0, i -> {
                both = i == 1;
                a.refresh();
            });
            head.addView(seg, Ui.lp(Ui.dp(190), -2));
        }
        card.addView(head);
        boolean first = true;
        for (MealPlan.Item i : meal.items) {
            double grams = i.grams;
            if (twoDays) {
                for (MealPlan.Item j : partner.items) if (j.foodId.equals(i.foodId)) grams += j.grams;
            }
            if (!first) {
                View line = new View(a);
                line.setBackgroundColor(Ui.SURFACE2);
                card.addView(line, Ui.lp(-1, 1));
            }
            first = false;
            LinearLayout row = Ui.hbox(a);
            row.setMinimumHeight(Ui.dp(54));
            row.setPadding(0, Ui.dp(8), 0, Ui.dp(8));
            LinearLayout txt = Ui.vbox(a);
            Groceries.Info info = Groceries.info(i.foodId, i.name);
            txt.addView(Ui.text(a, info.label, 15, Ui.TEXT));
            String per = Groceries.amount(i.foodId, i.grams) + (Groceries.cooked(i.foodId) ? " cuit" : "");
            String raw = Groceries.rawHint(i.foodId, i.grams);
            String sub = (twoDays ? per + " par portion" : per) + (raw != null ? " · " + raw : "");
            TextView s = Ui.text(a, sub, 12, Ui.FAINT);
            s.setPadding(0, Ui.dp(2), 0, 0);
            txt.addView(s);
            row.addView(txt, Ui.weight(1));
            LinearLayout right = Ui.vbox(a);
            right.setGravity(Gravity.END);
            String qty = Groceries.amount(i.foodId, grams);
            String rawTotal = Groceries.rawHint(i.foodId, grams);
            if (twoDays && rawTotal != null) qty = rawTotal.replace("≈ ", "");
            TextView q = Ui.medium(a, qty, 15, Ui.TEXT);
            Ui.tabular(q);
            q.setGravity(Gravity.END);
            right.addView(q);
            com.fonte.carnet.core.Food f = com.fonte.carnet.core.Foods.find(i.foodId);
            if (f != null) {
                TextView k = Ui.text(a, Fmt.kg(Math.round(f.kcal * grams / 100.0)) + " kcal", 12, Ui.FAINT);
                k.setGravity(Gravity.END);
                right.addView(k);
            }
            row.addView(right);
            card.addView(row);
        }
        if (twoDays) {
            TextView t = Ui.text(a, "Quantités à préparer pour les deux jours (crues ou sèches pour les viandes et féculents).", 12, Ui.FAINT);
            t.setPadding(0, Ui.dp(6), 0, 0);
            card.addView(t);
        }
        return card;
    }

    private View stepsCard(Recipe r, MealPlan.Meal partner) {
        LinearLayout card = Ui.card(a);
        TextView label = Ui.medium(a, "PRÉPARATION", 11, Ui.MUTED);
        label.setLetterSpacing(0.06f);
        label.setPadding(0, 0, 0, Ui.dp(4));
        card.addView(label);
        int n = 0;
        for (String s : r.steps) card.addView(step(++n, s));
        if (partner != null && partner.day > meal.day) {
            card.addView(step(++n, "Répartir dans 2 boîtes : une pour aujourd'hui, une au frigo pour "
                    + MealPlanner.weekdayLong(partner.day) + " " + (MealPlan.LUNCH.equals(meal.slot) ? "midi" : "soir") + "."));
        } else if (partner != null) {
            card.addView(step(++n, "Préparé " + MealPlanner.weekdayLong(partner.day) + " : il suffit de réchauffer ta boîte."));
        }
        return card;
    }

    private View step(int n, String text) {
        LinearLayout row = Ui.hbox(a);
        row.setGravity(Gravity.TOP);
        row.setPadding(0, Ui.dp(10), 0, 0);
        TextView num = Ui.medium(a, String.valueOf(n), 13, Ui.TEXT);
        num.setGravity(Gravity.CENTER);
        num.setBackground(Ui.rect(Ui.SURFACE2, 14));
        row.addView(num, Ui.lp(Ui.dp(28), Ui.dp(28)));
        TextView t = Ui.text(a, text, 14, Ui.TEXT);
        t.setPadding(Ui.dp(12), Ui.dp(3), 0, 0);
        row.addView(t, Ui.weight(1));
        return row;
    }
}

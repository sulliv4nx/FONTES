package com.fonte.carnet.ui;

import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Foods;
import com.fonte.carnet.core.MealPlan;
import com.fonte.carnet.core.MealPlanner;
import com.fonte.carnet.core.Nutrition;

import java.util.List;
import java.util.function.Consumer;

/** Choisir soi-même le plat d'un repas : toutes les recettes compatibles, déjà ajustées à l'objectif. */
public class RecipePickerScreen extends ScrollScreen {
    private final MealPlan.Meal meal;
    private final Consumer<MealPlan.Meal> onReplaced;
    private List<MealPlanner.Option> options;
    private String query = "";

    RecipePickerScreen(MealPlan.Meal meal, Consumer<MealPlan.Meal> onReplaced) {
        this.meal = meal;
        this.onReplaced = onReplaced;
    }

    @Override
    public String title() {
        return "Choisir " + SwapSheet.article(meal.slot) + SwapSheet.lower(MealPlan.slotLabel(meal.slot));
    }

    @Override
    public View build() {
        if (options == null) options = MealPlanner.alternatives(st.mealPrefs(), meal, 0, true);
        LinearLayout c = Ui.vbox(a);
        c.addView(Ui.hint(a, "Quantités déjà ajustées pour viser ≈ " + Fmt.kg(Math.round(target())) + " kcal sur ce repas."));
        EditText search = Ui.field(a, "Rechercher une recette", Ui.TEXT_FIELD);
        search.setText(query);
        search.setSelectAllOnFocus(false);
        search.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 12));
        c.addView(search);
        LinearLayout list = Ui.vbox(a);
        c.addView(list);
        fill(list);
        Ui.onText(search, s -> {
            query = s;
            fill(list);
        });
        return page(c);
    }

    private double target() {
        return meal.targetKcal > 0 ? meal.targetKcal : meal.totals().kcal;
    }

    private void fill(LinearLayout list) {
        list.removeAllViews();
        String q = Foods.norm(query);
        int shown = 0;
        for (MealPlanner.Option o : options) {
            if (!q.isEmpty() && !Foods.norm(o.recipe.name).contains(q)) continue;
            shown++;
            Nutrition.Totals t = o.meal.totals();
            LinearLayout card = Ui.clickCard(a, v -> choose(o));
            card.addView(Ui.medium(a, o.recipe.name, 16, Ui.TEXT));
            TextView sub = Ui.text(a, Fmt.kg(Math.round(t.kcal)) + " kcal · " + o.recipe.minutes + " min", 13, Ui.MUTED);
            sub.setPadding(0, Ui.dp(4), 0, 0);
            card.addView(sub);
            card.addView(Ui.macroDots(a, t.protein, t.carbs, t.fat));
            list.addView(card);
        }
        if (shown == 0) list.addView(Ui.hint(a, "Aucune recette ne correspond."));
    }

    private void choose(MealPlanner.Option o) {
        MealPlan plan = st.mealPlan;
        MealPlan.Meal partner = plan == null ? null : plan.batchPartner(meal);
        Runnable go = () -> {
            MealPlan.Meal fresh = SwapSheet.apply(a, meal, o.recipe, null, o.meal);
            a.back();
            Ui.toast(a, "Remplacé par « " + o.recipe.name + " »");
            onReplaced.accept(fresh);
        };
        if (partner == null) {
            go.run();
            return;
        }
        new android.app.AlertDialog.Builder(a)
                .setTitle("Plat cuisiné en lot")
                .setMessage("Remplacer aussi le " + SwapSheet.lower(MealPlan.slotLabel(partner.slot)) + " de " + MealPlanner.weekdayLong(partner.day) + " ?")
                .setPositiveButton("Les deux", (d, w) -> {
                    MealPlan.Meal fresh = SwapSheet.apply(a, meal, o.recipe, partner, o.meal);
                    a.back();
                    Ui.toast(a, "Remplacé par « " + o.recipe.name + " »");
                    onReplaced.accept(fresh);
                })
                .setNegativeButton("Seulement celui-ci", (d, w) -> go.run())
                .show();
    }
}

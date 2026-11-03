package com.fonte.carnet.ui;

import android.app.Dialog;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.MealPlan;
import com.fonte.carnet.core.MealPlanner;
import com.fonte.carnet.core.Nutrition;
import com.fonte.carnet.core.Recipe;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Feuille « Remplacer le déjeuner » : 3 plats aux mêmes calories et protéines, ou choix libre. */
final class SwapSheet {
    private SwapSheet() {
    }

    static void show(MainActivity a, MealPlan.Meal meal, Consumer<MealPlan.Meal> onReplaced) {
        MealPlan plan = a.store.mealPlan;
        if (plan == null) return;
        List<MealPlanner.Option> options = MealPlanner.alternatives(a.store.mealPrefs(), meal, 3, false);
        MealPlan.Meal partner = plan.batchPartner(meal);
        final int[] chosen = {options.isEmpty() ? -1 : 0};
        final boolean[] alsoPartner = {partner != null};

        LinearLayout box = Ui.vbox(a);
        LinearLayout head = Ui.hbox(a);
        head.setGravity(Gravity.TOP);
        LinearLayout titles = Ui.vbox(a);
        TextView title = Ui.text(a, "Remplacer " + article(meal.slot) + lower(MealPlan.slotLabel(meal.slot)), 20, Ui.TEXT);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titles.addView(title);
        TextView sub = Ui.text(a, "Mêmes calories et protéines, à ±10 %", 13, Ui.MUTED);
        sub.setPadding(0, Ui.dp(4), 0, 0);
        titles.addView(sub);
        head.addView(titles, Ui.weight(1));
        box.addView(head);

        Nutrition.Totals now = meal.totals();
        LinearLayout current = Ui.hbox(a);
        current.setBackground(Ui.rect(Ui.BG, 12));
        current.setPadding(Ui.dp(12), Ui.dp(10), Ui.dp(12), Ui.dp(10));
        current.setLayoutParams(Ui.mlp(-1, -2, 0, 12, 0, 4));
        TextView tag = Ui.medium(a, "ACTUEL", 11, Ui.MUTED);
        tag.setLetterSpacing(0.06f);
        current.addView(tag);
        TextView cn = Ui.text(a, meal.name, 14, Ui.TEXT);
        cn.setSingleLine(true);
        cn.setEllipsize(TextUtils.TruncateAt.END);
        cn.setPadding(Ui.dp(10), 0, Ui.dp(8), 0);
        current.addView(cn, Ui.weight(1));
        current.addView(Ui.text(a, Fmt.kg(Math.round(now.kcal)) + " kcal · P " + Math.round(now.protein), 13, Ui.MUTED));
        box.addView(current);

        LinearLayout list = Ui.vbox(a);
        box.addView(list);
        Runnable[] render = new Runnable[1];
        render[0] = () -> {
            list.removeAllViews();
            if (options.isEmpty()) {
                list.addView(Ui.hint(a, "Aucun autre plat ne correspond à tes préférences pour ce repas."));
            }
            for (int i = 0; i < options.size(); i++) {
                final int idx = i;
                list.addView(optionView(a, options.get(i), i == chosen[0], v -> {
                    chosen[0] = idx;
                    render[0].run();
                }));
            }
        };
        render[0].run();

        if (partner != null) {
            LinearLayout sw = Ui.switchRow(a, "Aussi " + MealPlanner.weekdayLong(partner.day), "Le plat est cuisiné en lot pour 2 jours",
                    true, on -> alsoPartner[0] = on);
            sw.setLayoutParams(Ui.mlp(-1, -2, 0, 4, 0, 0));
            box.addView(sw);
        }

        Dialog d = Ui.sheet(a, box);
        TextView pick = Ui.medium(a, "Choisir moi-même dans les recettes", 14, Ui.ACCENT);
        pick.setGravity(Gravity.CENTER);
        pick.setMinHeight(Ui.dp(44));
        pick.setOnClickListener(v -> {
            d.dismiss();
            a.show(new RecipePickerScreen(meal, onReplaced));
        });
        box.addView(pick, Ui.lp(-1, -2));

        TextView keep = Ui.smallButton(a, "Garder l'actuel", Ui.GHOST, v -> d.dismiss());
        keep.setMinHeight(Ui.dp(52));
        keep.setTextColor(Ui.TEXT);
        TextView replace = Ui.smallButton(a, "Remplacer", Ui.PRIMARY, v -> {
            if (chosen[0] < 0) return;
            MealPlanner.Option o = options.get(chosen[0]);
            MealPlan.Meal fresh = apply(a, meal, o.recipe, alsoPartner[0] ? partner : null, o.meal);
            d.dismiss();
            Ui.toast(a, "Remplacé par « " + o.recipe.name + " »");
            onReplaced.accept(fresh);
        });
        replace.setMinHeight(Ui.dp(52));
        box.addView(Ui.buttonRow(a, keep, replace));
        TextView note = Ui.text(a, "La liste de courses se met à jour toute seule.", 12, Ui.FAINT);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, Ui.dp(10), 0, 0);
        box.addView(note, Ui.lp(-1, -2));
        d.show();
    }

    /** Remplace le repas (et son jumeau cuisiné en lot si demandé). */
    static MealPlan.Meal apply(MainActivity a, MealPlan.Meal meal, Recipe r, MealPlan.Meal partner, MealPlan.Meal built) {
        a.store.replaceMeal(meal, built);
        if (partner != null) {
            MealPlan.Meal twin = MealPlanner.build(r, partner.day, partner.slot, target(partner.targetKcal, partner.totals().kcal),
                    target(partner.targetProtein, partner.totals().protein), target(partner.targetFat, partner.totals().fat));
            a.store.replaceMeal(partner, twin);
        }
        return built;
    }

    private static double target(double t, double fallback) {
        return t > 0 ? t : fallback;
    }

    private static View optionView(MainActivity a, MealPlanner.Option o, boolean selected, View.OnClickListener l) {
        Nutrition.Totals t = o.meal.totals();
        LinearLayout card = Ui.vbox(a);
        card.setBackground(Ui.pressable(selected ? Ui.outline(0x1469E6D2, Ui.ACCENT, 16) : Ui.outline(Ui.BG, Ui.LINE, 16), 16));
        card.setPadding(Ui.dp(14), Ui.dp(14), Ui.dp(14), Ui.dp(14));
        card.setLayoutParams(Ui.mlp(-1, -2, 0, 10, 0, 0));
        card.setOnClickListener(l);
        LinearLayout head = Ui.hbox(a);
        head.addView(Ui.medium(a, o.recipe.name, 16, Ui.TEXT), Ui.weight(1));
        if (selected) {
            android.widget.ImageView ok = new android.widget.ImageView(a);
            ok.setImageDrawable(new Icon(Icon.CHECK, Ui.BG));
            ok.setPadding(Ui.dp(4), Ui.dp(4), Ui.dp(4), Ui.dp(4));
            ok.setBackground(Ui.rect(Ui.ACCENT, 12));
            LinearLayout.LayoutParams lp = Ui.lp(Ui.dp(24), Ui.dp(24));
            lp.leftMargin = Ui.dp(8);
            head.addView(ok, lp);
        }
        card.addView(head);
        LinearLayout macros = Ui.hbox(a);
        macros.setPadding(0, Ui.dp(6), 0, 0);
        TextView k = Ui.medium(a, Fmt.kg(Math.round(t.kcal)) + " kcal", 14, Ui.TEXT);
        Ui.tabular(k);
        macros.addView(k);
        LinearLayout dots = Ui.macroDots(a, t.protein, t.carbs, t.fat);
        dots.setLayoutParams(Ui.mlp(-2, -2, 12, 0, 0, 0));
        macros.addView(dots);
        card.addView(macros);
        LinearLayout tags = Ui.hbox(a);
        tags.setPadding(0, Ui.dp(8), 0, 0);
        tags.addView(Ui.badge(a, o.recipe.minutes + " min", Ui.MUTED, Ui.SURFACE2));
        String extra = o.recipe.has(Recipe.CREOLE) ? "Créole" : o.recipe.vegetarian() ? "Végétarien"
                : o.recipe.has(Recipe.FISH) ? "Poisson" : o.recipe.has(Recipe.BUDGET) ? "Petit budget" : null;
        if (extra != null) {
            TextView b = Ui.badge(a, extra, Ui.OK, Ui.OK_SOFT);
            LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
            lp.leftMargin = Ui.dp(6);
            tags.addView(b, lp);
        }
        if (!o.close) {
            TextView b = Ui.badge(a, "Écart > 10 %", Ui.WARN, 0x26FFB347);
            LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
            lp.leftMargin = Ui.dp(6);
            tags.addView(b, lp);
        }
        card.addView(tags);
        return card;
    }

    static String article(String slot) {
        return MealPlan.SNACK_AM.equals(slot) || MealPlan.SNACK_PM.equals(slot) ? "la " : "le ";
    }

    static String lower(String s) {
        return s.toLowerCase(Locale.FRENCH);
    }
}

package com.fonte.carnet.ui;

import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.BodyLog;
import com.fonte.carnet.core.Days;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.FoodEntry;
import com.fonte.carnet.core.Nutrition;
import com.fonte.carnet.core.Stats;

import java.util.List;

/** Journal alimentaire du jour : bilan calories et macros, eau, dépense, repas. */
public class NutritionScreen extends Screen {
    private int day = Days.today();

    @Override
    public String title() {
        return "Nutrition";
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
        c.addView(dayNavigator());
        if (!st.profile.complete()) {
            LinearLayout card = Ui.clickCard(a, v -> a.show(new ProfileScreen()));
            card.setBackground(Ui.pressable(Ui.outline(Ui.ACCENT_SOFT, Ui.ACCENT, 18), 18));
            card.addView(Ui.cardHeader(a, Icon.SPARK, Ui.ACCENT, "Calcule tes besoins", "Profil", v -> a.show(new ProfileScreen())));
            card.addView(Ui.text(a, "Renseigne ta taille, ton âge et ton objectif : les objectifs ci-dessous sont pour l'instant des valeurs par défaut.", 13, Ui.MUTED));
            c.addView(card);
        }
        c.addView(summaryCard());
        c.addView(mealsCard());
        c.addView(waterCard());
        c.addView(burnCard());
        for (String meal : FoodEntry.MEALS) c.addView(mealCard(meal));
        return Ui.page(a, c);
    }

    private View dayNavigator() {
        LinearLayout row = Ui.hbox(a);
        row.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 8));
        row.addView(Ui.iconButton(a, Icon.BACK, Ui.TEXT, v -> {
            day = Days.add(day, -1);
            a.refresh();
        }));
        TextView label = Ui.medium(a, Days.label(day), 17, Ui.TEXT);
        label.setGravity(Gravity.CENTER);
        row.addView(label, Ui.weight(1));
        boolean canGoNext = day < Days.today();
        android.widget.ImageView next = Ui.iconButton(a, Icon.CHEVRON, canGoNext ? Ui.TEXT : Ui.SURFACE2, v -> {
            if (day < Days.today()) {
                day = Days.add(day, 1);
                a.refresh();
            }
        });
        row.addView(next);
        return row;
    }

    private View summaryCard() {
        Nutrition.Totals t = Nutrition.totals(st.foodEntries, day, null);
        int burned = Nutrition.total(st.burned(day));
        int target = st.targetKcal();
        int goal = target + burned;
        Nutrition.Macros m = st.targetMacros(day);
        LinearLayout card = Ui.card(a);
        LinearLayout row = Ui.hbox(a);
        RingView ring = new RingView(a);
        long left = Math.round(goal - t.kcal);
        ring.set(goal > 0 ? (float) (t.kcal / goal) : 0f, Fmt.kg(Math.abs(left)), left >= 0 ? "kcal restantes" : "kcal en trop", Ui.ACCENT);
        row.addView(ring, Ui.lp(Ui.dp(128), Ui.dp(128)));
        LinearLayout macros = Ui.vbox(a);
        macros.setPadding(Ui.dp(16), 0, 0, 0);
        macros.addView(Ui.meter(a, "Protéines", t.protein, m.protein, "g", Ui.PROTEIN));
        macros.addView(Ui.meter(a, "Glucides", t.carbs, m.carbs, "g", Ui.CARBS));
        macros.addView(Ui.meter(a, "Lipides", t.fat, m.fat, "g", Ui.FAT));
        row.addView(macros, Ui.weight(1));
        card.addView(row);
        card.addView(Ui.divider(a));
        card.addView(Ui.row(a, "Objectif (" + st.profile.goalLabel().toLowerCase(java.util.Locale.FRENCH) + ")", Fmt.kg(target) + " kcal"));
        card.addView(Ui.row(a, "+ Activité du jour", Fmt.kg(burned) + " kcal"));
        if (burned > 0) {
            TextView extra = Ui.text(a, "Ces " + Fmt.kg(burned) + " kcal de séance sont ajoutées en glucides : +"
                    + Math.round(burned / 4.0) + " g aujourd'hui.", 12, Ui.MUTED);
            extra.setPadding(0, 0, 0, Ui.dp(4));
            card.addView(extra);
        }
        card.addView(Ui.row(a, "− Consommé", Fmt.kg(Math.round(t.kcal)) + " kcal"));
        LinearLayout total = Ui.row(a, left >= 0 ? "Reste" : "Dépassement", Fmt.kg(Math.abs(left)) + " kcal");
        ((TextView) total.getChildAt(1)).setTextColor(left >= 0 ? Ui.ACCENT : Ui.WARN);
        card.addView(total);
        return card;
    }

    /** Mes repas : ce qui est prévu ce jour-là, ou l'invitation à créer un plan. */
    private View mealsCard() {
        com.fonte.carnet.core.MealPlan plan = st.mealPlan;
        LinearLayout card = Ui.clickCard(a, v -> a.show(new MealPlanScreen(day)));
        card.addView(Ui.cardHeader(a, Icon.SPARK, Ui.ACCENT, "Mes repas", plan == null ? null : "Courses",
                v -> a.show(new ShoppingScreen())));
        if (plan == null || !plan.covers(day)) {
            card.addView(Ui.text(a, plan == null
                    ? "Fonte te propose quoi manger sur 7 jours (plats, collations, shakers), ajusté à ton objectif, avec la liste de courses."
                    : "Aucun plan pour ce jour.", 13, Ui.MUTED));
            card.addView(Ui.button(a, plan == null ? "Créer mon plan" : "Voir mes repas", Ui.SECONDARY,
                    v -> a.show(plan == null ? new MealSetupScreen() : new MealPlanScreen())));
            return card;
        }
        java.util.List<com.fonte.carnet.core.MealPlan.Meal> meals = plan.mealsOf(day);
        int eaten = 0;
        for (com.fonte.carnet.core.MealPlan.Meal m : meals) if (m.eaten) eaten++;
        card.addView(Ui.text(a, meals.size() + " repas prévus · " + Fmt.kg(Math.round(plan.totals(day).kcal)) + " kcal · "
                + eaten + " coché" + (eaten > 1 ? "s" : ""), 13, Ui.MUTED));
        com.fonte.carnet.core.MealPlan.Meal next = MealPlanScreen.nextMeal(plan, day);
        if (next != null) {
            LinearLayout row = Ui.hbox(a);
            row.setPadding(0, Ui.dp(10), 0, 0);
            LinearLayout txt = Ui.vbox(a);
            txt.addView(Ui.text(a, (day == Days.today() ? "À suivre · " : "") + com.fonte.carnet.core.MealPlan.slotLabel(next.slot), 12, Ui.FAINT));
            txt.addView(Ui.medium(a, next.name, 15, Ui.TEXT));
            row.addView(txt, Ui.weight(1));
            TextView k = Ui.medium(a, Fmt.kg(Math.round(next.totals().kcal)) + " kcal", 14, Ui.TEXT);
            Ui.tabular(k);
            row.addView(k);
            card.addView(row);
        } else {
            TextView done = Ui.text(a, "Tous les repas du jour sont cochés.", 13, Ui.OK);
            done.setPadding(0, Ui.dp(8), 0, 0);
            card.addView(done);
        }
        return card;
    }

    private View waterCard() {
        int ml = Nutrition.waterOf(st.water, day);
        int goal = st.waterTarget(day);
        LinearLayout card = Ui.card(a);
        card.addView(Ui.cardHeader(a, Icon.DROP, Ui.WATER, "Hydratation", ml > 0 ? "Annuler" : null, v -> {
            st.undoWater(day);
            a.refresh();
        }));
        card.addView(Ui.meter(a, "Bu aujourd'hui", ml, goal, "ml", Ui.WATER));
        LinearLayout row = Ui.hbox(a);
        int[] amounts = {250, 330, 500, 750};
        String[] labels = {"Verre", "Canette", "Bouteille", "Gourde"};
        for (int i = 0; i < amounts.length; i++) {
            int amount = amounts[i];
            LinearLayout b = Ui.vbox(a);
            b.setGravity(Gravity.CENTER);
            b.setPadding(0, Ui.dp(8), 0, Ui.dp(8));
            b.setBackground(Ui.pressable(Ui.rect(Ui.SURFACE2, 12), 12));
            b.setOnClickListener(v -> {
                st.addWater(amount, day);
                a.refresh();
            });
            TextView t1 = Ui.medium(a, "+" + amount, 15, Ui.TEXT);
            t1.setGravity(Gravity.CENTER);
            b.addView(t1, Ui.lp(-1, -2));
            TextView t2 = Ui.text(a, labels[i], 11, Ui.MUTED);
            t2.setGravity(Gravity.CENTER);
            b.addView(t2, Ui.lp(-1, -2));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            if (i < amounts.length - 1) lp.rightMargin = Ui.dp(8);
            row.addView(b, lp);
        }
        card.addView(row);
        TextView hint = Ui.text(a, "Objectif : 35 ml par kg de poids" + (st.trained(day) ? ", +500 ml car tu t'es entraîné" : "")
                + ". Modifiable dans ton profil.", 12, Ui.MUTED);
        hint.setPadding(0, Ui.dp(10), 0, 0);
        card.addView(hint);
        return card;
    }

    private View burnCard() {
        List<Nutrition.Burn> burns = st.burned(day);
        LinearLayout card = Ui.card(a);
        card.addView(Ui.cardHeader(a, Icon.FLAME, Ui.BURN, "Calories dépensées", "+ Activité",
                v -> ActivityDialog.show(a, day, a::refresh)));
        if (burns.isEmpty()) {
            card.addView(Ui.text(a, "Aucune activité ce jour. Tes séances, WOD et circuits sont comptés automatiquement.", 13, Ui.MUTED));
        }
        for (Nutrition.Burn b : burns) {
            LinearLayout row = Ui.hbox(a);
            row.setPadding(0, Ui.dp(4), 0, Ui.dp(4));
            LinearLayout txt = Ui.vbox(a);
            txt.addView(Ui.text(a, b.label, 15, Ui.TEXT));
            txt.addView(Ui.text(a, b.minutes + " min", 12, Ui.MUTED));
            row.addView(txt, Ui.weight(1));
            TextView k = Ui.medium(a, b.kcal + " kcal", 15, Ui.BURN);
            Ui.tabular(k);
            row.addView(k);
            BodyLog.Activity manual = findActivity(b);
            if (manual != null) {
                row.setOnLongClickListener(v -> {
                    Ui.confirm(a, "Supprimer cette activité ?", manual.name, "Supprimer", () -> {
                        st.deleteActivity(manual);
                        a.refresh();
                    });
                    return true;
                });
            }
            card.addView(row);
        }
        int base = Nutrition.maintenance(st.profile, st.weightForCalc(), st.profile.age(System.currentTimeMillis()));
        TextView t = Ui.text(a, "Métabolisme et vie quotidienne : ≈ " + Fmt.kg(base) + " kcal par jour (déjà compris dans ton objectif).", 12, Ui.MUTED);
        t.setPadding(0, Ui.dp(8), 0, 0);
        card.addView(t);
        return card;
    }

    private BodyLog.Activity findActivity(Nutrition.Burn b) {
        for (BodyLog.Activity x : st.activities) {
            if (x.day == day && x.name.equals(b.label) && x.kcal == b.kcal && x.minutes == b.minutes) return x;
        }
        return null;
    }

    private View mealCard(String meal) {
        List<FoodEntry> items = st.foods(day, meal);
        Nutrition.Totals t = Nutrition.totals(st.foodEntries, day, meal);
        LinearLayout card = Ui.card(a);
        LinearLayout head = Ui.hbox(a);
        head.addView(Ui.medium(a, meal, 17, Ui.TEXT), Ui.weight(1));
        TextView total = Ui.medium(a, Fmt.kg(Math.round(t.kcal)) + " kcal", 15, items.isEmpty() ? Ui.FAINT : Ui.TEXT);
        Ui.tabular(total);
        head.addView(total);
        card.addView(head);
        for (FoodEntry e : items) {
            LinearLayout row = Ui.hbox(a);
            row.setPadding(0, Ui.dp(10), 0, 0);
            row.setBackground(Ui.pressable(Ui.rect(0x00000000, 10), 10));
            row.setOnClickListener(v -> QuantityDialog.edit(a, e, a::refresh));
            row.setOnLongClickListener(v -> {
                Ui.confirm(a, "Retirer « " + e.name + " » ?", meal, "Retirer", () -> {
                    st.deleteFood(e);
                    a.refresh();
                });
                return true;
            });
            if (e.photo != null) {
                Bitmap bmp = Photos.thumb(e.photo, Ui.dp(44));
                if (bmp != null) {
                    ImageView iv = new ImageView(a);
                    iv.setImageBitmap(bmp);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    iv.setClipToOutline(true);
                    iv.setBackground(Ui.rect(Ui.SURFACE2, 10));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(44), Ui.dp(44));
                    lp.rightMargin = Ui.dp(12);
                    row.addView(iv, lp);
                }
            }
            LinearLayout txt = Ui.vbox(a);
            txt.addView(Ui.text(a, e.name, 15, Ui.TEXT));
            txt.addView(Ui.text(a, Fmt.kg(Math.round(e.grams)) + " g · P " + Fmt.kg(Stats.round1(e.protein)) + " · G "
                    + Fmt.kg(Stats.round1(e.carbs)) + " · L " + Fmt.kg(Stats.round1(e.fat)), 12, Ui.MUTED));
            row.addView(txt, Ui.weight(1));
            TextView k = Ui.text(a, Fmt.kg(Math.round(e.kcal)), 15, Ui.TEXT);
            Ui.tabular(k);
            row.addView(k);
            card.addView(row);
        }
        TextView add = Ui.button(a, "+ Ajouter", Ui.GHOST, v -> a.show(new FoodAddScreen(meal, day)));
        add.setMinHeight(Ui.dp(44));
        card.addView(add);
        return card;
    }
}

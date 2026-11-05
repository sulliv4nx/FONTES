package com.fonte.carnet.ui;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Days;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.MealPlan;
import com.fonte.carnet.core.MealPlanner;
import com.fonte.carnet.core.Nutrition;

import java.util.List;
import java.util.Locale;

/** Mes repas : la semaine jour par jour, avec les plats à cocher, changer ou ouvrir. */
public class MealPlanScreen extends ScrollScreen {
    private int day;

    public MealPlanScreen() {
        this(0);
    }

    public MealPlanScreen(int day) {
        this.day = day;
    }

    /** Affiche ce jour au prochain rendu. */
    void select(int day) {
        this.day = day;
        scrollTop();
    }

    @Override
    public String title() {
        return "Mes repas";
    }

    @Override
    public int actionIcon() {
        return st.mealPlan == null ? -1 : Icon.GEAR;
    }

    @Override
    public void onAction() {
        a.show(new MealSetupScreen());
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        c.addView(DayPlanScreen.modeSwitch(a, false, day));
        MealPlan plan = st.mealPlan;
        if (plan == null) {
            c.addView(emptyCard());
            return page(c);
        }
        int today = Days.today();
        if (day == 0 || !plan.covers(day)) day = plan.covers(today) ? today : plan.start;
        if (today > plan.end()) c.addView(finishedCard());
        c.addView(weekStrip(plan, today));
        c.addView(composeCard());
        if (targetsChanged(plan)) c.addView(targetChangedCard(plan));
        c.addView(summaryCard(plan));
        for (MealPlan.Meal m : plan.mealsOf(day)) c.addView(mealCard(plan, m, today));
        TextView shop = Ui.button(a, "Liste de courses de la semaine", Ui.PRIMARY, v -> a.show(new ShoppingScreen()));
        c.addView(shop);
        TextView note = Ui.text(a, "Plan calculé pour " + describe(plan) + ". Les valeurs sont des moyennes indicatives.", 12, Ui.FAINT);
        note.setPadding(0, Ui.dp(10), 0, 0);
        c.addView(note);
        return page(c);
    }

    // ---------------------------------------------------------------- cartes

    private View emptyCard() {
        LinearLayout card = Ui.card(a);
        card.setBackground(Ui.outline(Ui.ACCENT_SOFT, Ui.ACCENT, 18));
        card.addView(Ui.cardHeader(a, Icon.SPARK, Ui.ACCENT, "Je te dis quoi manger", null, null));
        card.addView(Ui.hint(a, "Un plan de 7 jours avec tes plats, collations et shakers, ajusté à ton objectif en calories et en protéines. "
                + "La liste de courses de la semaine est calculée avec."));
        card.addView(Ui.button(a, "Créer mon plan", Ui.PRIMARY, v -> a.show(new MealSetupScreen())));
        card.addView(Ui.button(a, "Composer avec mes ingrédients", Ui.SECONDARY, v -> a.show(new ComposeScreen())));
        TextView or = Ui.text(a, "Ou une journée type à heures fixes, avec des équivalences pour chaque aliment, comme une diète de préparateur.",
                13, Ui.MUTED);
        or.setPadding(0, Ui.dp(12), 0, 0);
        card.addView(or);
        card.addView(Ui.button(a, "Créer ma journée type", Ui.SECONDARY, v -> a.show(new DaySetupScreen(true))));
        return card;
    }

    /** Accès à « Composer mes repas ». */
    private View composeCard() {
        LinearLayout card = Ui.clickCard(a, v -> a.show(new ComposeScreen()));
        card.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(10), Ui.dp(12));
        LinearLayout row = Ui.hbox(a);
        row.addView(Ui.iconBadge(a, Icon.NUTRITION, Ui.ACCENT));
        LinearLayout txt = Ui.vbox(a);
        txt.setPadding(Ui.dp(12), 0, Ui.dp(8), 0);
        txt.addView(Ui.medium(a, "Composer avec mes ingrédients", 15, Ui.TEXT));
        java.util.List<String> mine = st.mealPrefs().pantry;
        txt.addView(Ui.text(a, mine.isEmpty() ? "Choisis ce que tu as, l'appli propose les repas · plats réunionnais inclus"
                : mine.size() + " ingrédient" + (mine.size() > 1 ? "s" : "") + " choisi" + (mine.size() > 1 ? "s" : "")
                + " · voir les repas possibles", 12, Ui.MUTED));
        row.addView(txt, Ui.weight(1));
        android.widget.ImageView chev = new android.widget.ImageView(a);
        chev.setImageDrawable(new Icon(Icon.CHEVRON, Ui.MUTED));
        row.addView(chev, Ui.lp(Ui.dp(20), Ui.dp(20)));
        card.addView(row);
        return card;
    }

    private View finishedCard() {
        LinearLayout card = Ui.card(a);
        card.setBackground(Ui.outline(Ui.ACCENT_SOFT, Ui.ACCENT, 18));
        card.addView(Ui.medium(a, "Ta semaine est terminée", 16, Ui.TEXT));
        TextView t = Ui.text(a, "Génère la suivante avec les mêmes réglages, ou modifie-les avant.", 13, Ui.MUTED);
        t.setPadding(0, Ui.dp(4), 0, 0);
        card.addView(t);
        card.addView(Ui.buttonRow(a,
                Ui.smallButton(a, "Régler", Ui.GHOST, v -> a.show(new MealSetupScreen())),
                Ui.smallButton(a, "Semaine suivante", Ui.PRIMARY, v -> {
                    st.generateMealPlan(st.mealPrefs(), Days.today());
                    day = Days.today();
                    scrollTop();
                    a.refresh();
                })));
        return card;
    }

    private View targetChangedCard(MealPlan plan) {
        LinearLayout card = Ui.card(a);
        card.addView(Ui.medium(a, "Tes objectifs ont changé", 16, Ui.TEXT));
        MealPlanner.Targets rest = st.mealTargets();
        MealPlanner.Targets train = st.mealTrainingTargets();
        TextView t = Ui.text(a, "Le plan vise " + describe(plan) + ". Ton objectif est maintenant " + Fmt.kg(rest.kcal)
                + " kcal sans séance et " + Fmt.kg(train.kcal) + " kcal les jours d'entraînement (P " + rest.protein + " · L " + rest.fat
                + "). Régénère la semaine pour l'ajuster.", 13, Ui.MUTED);
        t.setPadding(0, Ui.dp(4), 0, 0);
        card.addView(t);
        card.addView(Ui.buttonRow(a, Ui.smallButton(a, "Régénérer", Ui.SECONDARY, v -> a.show(new MealSetupScreen()))));
        return card;
    }

    private View weekStrip(MealPlan plan, int today) {
        LinearLayout row = Ui.hbox(a);
        row.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 12));
        for (int i = 0; i < plan.days; i++) {
            int d = Days.add(plan.start, i);
            boolean sel = d == day;
            LinearLayout cell = Ui.vbox(a);
            cell.setGravity(Gravity.CENTER);
            cell.setMinimumHeight(Ui.dp(58));
            cell.setBackground(Ui.pressable(sel ? Ui.rect(Ui.ACCENT, 12) : Ui.outline(Ui.SURFACE, Ui.LINE, 12), 12));
            TextView w = Ui.text(a, MealPlanner.weekdayShort(d), 11, sel ? Ui.BG : Ui.MUTED);
            w.setGravity(Gravity.CENTER);
            cell.addView(w, Ui.lp(-1, -2));
            TextView n = Ui.medium(a, String.valueOf(d % 100), 16, sel ? Ui.BG : Ui.TEXT);
            n.setGravity(Gravity.CENTER);
            Ui.tabular(n);
            cell.addView(n, Ui.lp(-1, -2));
            View dot = new View(a);
            boolean allEaten = allEaten(plan, d);
            dot.setBackground(Ui.rect(allEaten ? (sel ? Ui.BG : Ui.OK) : (d == today && !sel ? Ui.ACCENT : 0x00000000), 2));
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(Ui.dp(4), Ui.dp(4));
            dp.gravity = Gravity.CENTER_HORIZONTAL;
            dp.topMargin = Ui.dp(3);
            cell.addView(dot, dp);
            cell.setContentDescription(MealPlanner.weekdayLong(d) + " " + (d % 100) + (d == today ? ", aujourd'hui" : ""));
            cell.setOnClickListener(v -> {
                day = d;
                scrollTop();
                a.refresh();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) lp.leftMargin = Ui.dp(5);
            row.addView(cell, lp);
        }
        return row;
    }

    private static boolean allEaten(MealPlan plan, int d) {
        List<MealPlan.Meal> meals = plan.mealsOf(d);
        if (meals.isEmpty()) return false;
        for (MealPlan.Meal m : meals) if (!m.eaten) return false;
        return true;
    }

    /** Objectifs du profil différents de ceux du plan (ou jours d'entraînement changés). */
    private boolean targetsChanged(MealPlan plan) {
        MealPlanner.Targets rest = st.mealTargets();
        MealPlanner.Targets train = st.mealTrainingTargets();
        int planTrain = plan.trainKcal > 0 ? plan.trainKcal : plan.kcal;
        int nowTrain = train.kcal > rest.kcal ? train.kcal : rest.kcal;
        boolean maskChanged = plan.trainKcal > 0 && plan.trainingMask != Math.max(0, st.mealPrefs().trainingMask);
        return Math.abs(rest.kcal - plan.kcal) >= 50 || Math.abs(rest.protein - plan.protein) >= 10
                || Math.abs(rest.fat - plan.fat) >= 8 || Math.abs(nowTrain - planTrain) >= 50 || maskChanged;
    }

    private static String describe(MealPlan plan) {
        String s = Fmt.kg(plan.kcal) + " kcal · P " + plan.protein + " g · G " + plan.carbs + " g · L " + plan.fat + " g";
        if (plan.trainKcal > 0) {
            return s + " les jours sans séance, et " + Fmt.kg(plan.trainKcal) + " kcal · G " + plan.trainCarbs + " g les jours d'entraînement";
        }
        return s + " par jour";
    }

    private View summaryCard(MealPlan plan) {
        Nutrition.Totals t = plan.totals(day);
        int[] goal = plan.targetsOn(day);
        List<MealPlan.Meal> meals = plan.mealsOf(day);
        int eaten = 0;
        double eatenKcal = 0;
        for (MealPlan.Meal m : meals) {
            if (!m.eaten) continue;
            eaten++;
            eatenKcal += m.totals().kcal;
        }
        LinearLayout card = Ui.card(a);
        LinearLayout head = Ui.hbox(a);
        LinearLayout left = Ui.vbox(a);
        TextView label = Ui.medium(a, ("Plan du " + MealPlanner.weekdayLong(day) + " " + dayMonth(day)
                + (plan.trainingDay(day) ? " · jour de séance" : "")).toUpperCase(Locale.FRENCH), 11, Ui.MUTED);
        label.setLetterSpacing(0.06f);
        left.addView(label);
        TextView big = Ui.text(a, Fmt.kg(Math.round(t.kcal)) + " ", 30, Ui.TEXT);
        big.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        Ui.tabular(big);
        LinearLayout kcalRow = Ui.hbox(a);
        kcalRow.setGravity(Gravity.BOTTOM);
        kcalRow.addView(big);
        TextView of = Ui.text(a, "/ " + Fmt.kg(goal[0]) + " kcal", 15, Ui.MUTED);
        of.setPadding(0, 0, 0, Ui.dp(5));
        kcalRow.addView(of);
        left.addView(kcalRow);
        head.addView(left, Ui.weight(1));
        double gap = goal[0] > 0 ? Math.abs(t.kcal - goal[0]) / goal[0] : 1;
        if (gap <= 0.05) head.addView(Ui.badge(a, "Objectif tenu", Ui.BG, Ui.OK));
        card.addView(head);
        LinearLayout macros = Ui.hbox(a);
        macros.setGravity(Gravity.TOP);
        macros.setPadding(0, Ui.dp(12), 0, 0);
        macros.addView(mini("Protéines", t.protein, goal[1], Ui.PROTEIN), Ui.weight(1));
        LinearLayout.LayoutParams mid = Ui.weight(1);
        mid.leftMargin = Ui.dp(10);
        mid.rightMargin = Ui.dp(10);
        macros.addView(mini("Glucides", t.carbs, goal[2], Ui.CARBS), mid);
        macros.addView(mini("Lipides", t.fat, goal[3], Ui.FAT), Ui.weight(1));
        card.addView(macros);
        TextView done = Ui.text(a, eaten + " repas sur " + meals.size() + " coché" + (eaten > 1 ? "s" : "")
                + (eaten > 0 ? " · " + Fmt.kg(Math.round(eatenKcal)) + " kcal mangées" : ""), 12, Ui.FAINT);
        done.setPadding(0, Ui.dp(12), 0, 0);
        card.addView(done);
        return card;
    }

    private LinearLayout mini(String label, double value, double target, int color) {
        LinearLayout box = Ui.vbox(a);
        LinearLayout row = Ui.hbox(a);
        row.addView(Ui.text(a, label, 12, Ui.MUTED), Ui.weight(1));
        TextView v = Ui.text(a, Fmt.kg(Math.round(value)) + " g", 12, Ui.TEXT);
        Ui.tabular(v);
        row.addView(v);
        box.addView(row);
        LinearLayout bar = Ui.hbox(a);
        bar.setBackground(Ui.rect(Ui.SURFACE2, 3));
        bar.setWeightSum(1f);
        View fill = new View(a);
        fill.setBackground(Ui.rect(color, 3));
        bar.addView(fill, new LinearLayout.LayoutParams(0, -1, target > 0 ? (float) Math.min(1, value / target) : 0f));
        box.addView(bar, Ui.mlp(-1, Ui.dp(6), 0, 6, 0, 0));
        return box;
    }

    static String dayMonth(int day) {
        return new java.text.SimpleDateFormat("d MMMM", Locale.FRENCH).format(new java.util.Date(Days.start(day)));
    }

    private View mealCard(MealPlan plan, MealPlan.Meal m, int today) {
        Nutrition.Totals t = m.totals();
        LinearLayout card = Ui.clickCard(a, v -> a.show(new RecipeScreen(m)));
        if (m.eaten) card.setBackground(Ui.pressable(Ui.outline(Ui.SURFACE, 0x7385DBA1, 18), 18));
        LinearLayout head = Ui.hbox(a);
        TextView slot = Ui.medium(a, (MealPlan.slotLabel(m.slot) + " · " + MealPlan.slotTime(m.slot)).toUpperCase(Locale.FRENCH), 11, Ui.MUTED);
        slot.setLetterSpacing(0.06f);
        head.addView(slot, Ui.weight(1));
        TextView k = Ui.medium(a, Fmt.kg(Math.round(t.kcal)) + " kcal", 14, Ui.TEXT);
        Ui.tabular(k);
        head.addView(k);
        card.addView(head);

        LinearLayout nameRow = Ui.hbox(a);
        nameRow.setPadding(0, Ui.dp(8), 0, 0);
        if (MealPlan.SHAKER.equals(m.slot)) {
            View b = Ui.iconBadge(a, Icon.SHAKER, Ui.PROTEIN);
            b.setLayoutParams(Ui.mlp(Ui.dp(36), Ui.dp(36), 0, 0, 10, 0));
            nameRow.addView(b);
        }
        TextView name = Ui.medium(a, m.name, 17, Ui.TEXT);
        nameRow.addView(name, Ui.weight(1));
        int batch = plan.batchIndex(m);
        if (batch > 0) {
            TextView bd = Ui.badge(a, "En lot · J" + batch + "/2", Ui.ACCENT, Ui.ACCENT_SOFT);
            LinearLayout.LayoutParams bp = Ui.lp(-2, -2);
            bp.leftMargin = Ui.dp(8);
            nameRow.addView(bd, bp);
        }
        card.addView(nameRow);
        TextView sum = Ui.text(a, m.summary(), 13, Ui.MUTED);
        sum.setPadding(0, Ui.dp(4), 0, 0);
        card.addView(sum);
        card.addView(Ui.macroDots(a, t.protein, t.carbs, t.fat));

        TextView eat = Ui.smallButton(a, m.eaten ? "✓ Mangé" : "Mangé", Ui.SECONDARY, v -> toggleEaten(m, today));
        if (m.eaten) {
            eat.setTextColor(Ui.OK);
            eat.setBackground(Ui.pressable(Ui.outline(Ui.OK_SOFT, 0x7385DBA1, 12), 12));
        }
        eat.setContentDescription(m.eaten ? "Mangé, appuyer pour décocher" : "Marquer comme mangé");
        card.addView(Ui.buttonRow(a, eat,
                Ui.smallButton(a, "Changer", Ui.GHOST, v -> SwapSheet.show(a, m, fresh -> a.refresh())),
                Ui.smallButton(a, "Recette", Ui.GHOST, v -> a.show(new RecipeScreen(m)))));
        return card;
    }

    private void toggleEaten(MealPlan.Meal m, int today) {
        if (!m.eaten && m.day > today) {
            Ui.toast(a, "Tu pourras le cocher le jour même.");
            return;
        }
        boolean now = !m.eaten;
        st.setMealEaten(m, now);
        if (now) Ui.toast(a, "Ajouté au journal · " + Fmt.kg(Math.round(m.totals().kcal)) + " kcal");
        a.refresh();
    }

    /** Accès rapide pour l'accueil et la nutrition : prochain repas non coché aujourd'hui, ou null. */
    static MealPlan.Meal nextMeal(MealPlan plan, int day) {
        if (plan == null || !plan.covers(day)) return null;
        for (MealPlan.Meal m : plan.mealsOf(day)) if (!m.eaten) return m;
        return null;
    }
}

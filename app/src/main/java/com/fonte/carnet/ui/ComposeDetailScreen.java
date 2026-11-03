package com.fonte.carnet.ui;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Composer;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Food;
import com.fonte.carnet.core.Foods;
import com.fonte.carnet.core.Groceries;
import com.fonte.carnet.core.MealPlan;
import com.fonte.carnet.core.Nutrition;

import java.util.Locale;

/** Composer mes repas, étape 3 : le détail d'un repas proposé. */
public class ComposeDetailScreen extends ScrollScreen {
    private final ComposeSession session;
    private final Composer.Proposal p;

    ComposeDetailScreen(ComposeSession session, Composer.Proposal p) {
        this.session = session;
        this.p = p;
    }

    @Override
    public String title() {
        return "Repas proposé · " + MealPlan.slotLabel(session.slot).toLowerCase(Locale.FRENCH);
    }

    @Override
    public View build() {
        Nutrition.Totals t = p.meal.totals();
        LinearLayout c = Ui.vbox(a);
        c.addView(Ui.heading(a, p.recipe.name));
        LinearLayout badges = Ui.hbox(a);
        badges.setLayoutParams(Ui.mlp(-1, -2, 0, 10, 0, 14));
        if (p.reunion) {
            TextView b = Ui.badge(a, "Réunionnais", ComposeSession.AMBER, 0x24FFC66B);
            LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
            lp.rightMargin = Ui.dp(8);
            badges.addView(b, lp);
        }
        badges.addView(Ui.badge(a, p.recipe.minutes + " min", Ui.MUTED, Ui.SURFACE2));
        c.addView(badges);

        LinearLayout kc = Ui.card(a);
        LinearLayout kcalRow = Ui.hbox(a);
        kcalRow.setGravity(Gravity.BOTTOM);
        TextView big = Ui.text(a, Fmt.kg(Math.round(t.kcal)) + " ", 30, Ui.TEXT);
        big.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        Ui.tabular(big);
        kcalRow.addView(big);
        TextView unit = Ui.text(a, "kcal / portion", 15, Ui.MUTED);
        unit.setPadding(0, 0, 0, Ui.dp(5));
        kcalRow.addView(unit);
        kc.addView(kcalRow);
        kc.addView(Ui.macroDots(a, t.protein, t.carbs, t.fat));
        double[] target = session.target();
        if (p.lowProtein) {
            TextView low = Ui.text(a, "Protéines un peu justes pour " + session.slotWord() + " (cible " + Math.round(target[1])
                    + " g) : le reste de la journée compense.", 12, Ui.FAINT);
            low.setPadding(0, Ui.dp(10), 0, 0);
            kc.addView(low);
        }
        c.addView(kc);

        LinearLayout ing = Ui.card(a);
        ing.setPadding(Ui.dp(16), Ui.dp(14), Ui.dp(16), Ui.dp(8));
        addSection(ing, "Tes ingrédients", true);
        boolean firstHave = true;
        for (MealPlan.Item i : p.meal.items) {
            if (!p.have.contains(i.foodId)) continue;
            ing.addView(row(i, 0, firstHave));
            firstHave = false;
        }
        boolean anyNeed = false;
        for (MealPlan.Item i : p.meal.items) if (p.need.contains(i.foodId)) anyNeed = true;
        if (anyNeed) {
            addSection(ing, "À prévoir · ajouté à ta liste de courses", false);
            boolean first = true;
            for (MealPlan.Item i : p.meal.items) {
                if (!p.need.contains(i.foodId)) continue;
                ing.addView(row(i, 1, first));
                first = false;
            }
        }
        boolean firstPantry = true;
        for (MealPlan.Item i : p.meal.items) {
            if (p.have.contains(i.foodId) || p.need.contains(i.foodId)) continue;
            if (firstPantry) addSection(ing, "Au placard", false);
            ing.addView(row(i, 2, firstPantry));
            firstPantry = false;
        }
        TextView spices = Ui.text(a, "Oignon, ail, épices et piment : à ton goût.", 12, Ui.FAINT);
        spices.setPadding(0, Ui.dp(8), 0, Ui.dp(4));
        ing.addView(spices);
        c.addView(ing);

        LinearLayout steps = Ui.card(a);
        TextView sl = Ui.medium(a, "PRÉPARATION", 11, Ui.MUTED);
        sl.setLetterSpacing(0.06f);
        steps.addView(sl);
        int n = 0;
        for (String s : p.recipe.steps) {
            LinearLayout row = Ui.hbox(a);
            row.setGravity(Gravity.TOP);
            row.setPadding(0, Ui.dp(10), 0, 0);
            TextView num = Ui.medium(a, String.valueOf(++n), 13, Ui.TEXT);
            num.setGravity(Gravity.CENTER);
            num.setBackground(Ui.rect(Ui.SURFACE2, 14));
            row.addView(num, Ui.lp(Ui.dp(28), Ui.dp(28)));
            TextView tx = Ui.text(a, s, 14, Ui.TEXT);
            tx.setPadding(Ui.dp(12), Ui.dp(3), 0, 0);
            row.addView(tx, Ui.weight(1));
            steps.addView(row);
        }
        c.addView(steps);

        boolean on = session.isValidated(p);
        TextView back = Ui.smallButton(a, "Autres repas", Ui.GHOST, v -> a.back());
        back.setMinHeight(Ui.dp(52));
        TextView valid = Ui.smallButton(a, on ? "✓ Validé" : "Valider ce repas", Ui.PRIMARY, v -> {
            session.toggleValidated(p);
            if (session.isValidated(p)) {
                Ui.toast(a, "Validé · " + session.validated.size() + " repas");
                a.back();
            } else {
                a.refresh();
            }
        });
        valid.setMinHeight(Ui.dp(52));
        c.addView(Ui.buttonRow(a, back, valid));
        return page(c);
    }

    private void addSection(LinearLayout card, String title, boolean first) {
        TextView l = Ui.medium(a, title.toUpperCase(Locale.FRENCH), 11, Ui.MUTED);
        l.setLetterSpacing(0.06f);
        l.setPadding(0, first ? 0 : Ui.dp(14), 0, Ui.dp(4));
        card.addView(l);
    }

    /** kind : 0 = à moi, 1 = à prévoir, 2 = placard. */
    private View row(MealPlan.Item i, int kind, boolean first) {
        LinearLayout wrap = Ui.vbox(a);
        if (!first) {
            View line = new View(a);
            line.setBackgroundColor(Ui.SURFACE2);
            wrap.addView(line, Ui.lp(-1, 1));
        }
        LinearLayout r = Ui.hbox(a);
        r.setMinimumHeight(Ui.dp(52));
        r.setPadding(0, Ui.dp(6), 0, Ui.dp(6));
        TextView ico = Ui.medium(a, kind == 0 ? "✓" : kind == 1 ? "+" : "–", 13, kind == 0 ? Ui.OK : kind == 1 ? ComposeSession.AMBER : Ui.MUTED);
        ico.setGravity(Gravity.CENTER);
        ico.setBackground(Ui.rect(kind == 0 ? Ui.OK_SOFT : kind == 1 ? 0x29FFC66B : Ui.SURFACE2, 12));
        r.addView(ico, Ui.lp(Ui.dp(24), Ui.dp(24)));
        LinearLayout txt = Ui.vbox(a);
        txt.setPadding(Ui.dp(12), 0, Ui.dp(8), 0);
        Groceries.Info info = Groceries.info(i.foodId, i.name);
        txt.addView(Ui.text(a, info.label, 15, Ui.TEXT));
        String raw = Groceries.rawHint(i.foodId, i.grams);
        String sub = kind == 2 ? "souvent déjà au placard"
                : raw != null ? Groceries.amount(i.foodId, i.grams) + " cuit · " + raw : null;
        if (sub != null) txt.addView(Ui.text(a, sub, 12, Ui.FAINT));
        r.addView(txt, Ui.weight(1));
        LinearLayout right = Ui.vbox(a);
        right.setGravity(Gravity.END);
        TextView q = Ui.medium(a, Groceries.amount(i.foodId, i.grams), 15, Ui.TEXT);
        Ui.tabular(q);
        right.addView(q);
        Food f = Foods.find(i.foodId);
        if (f != null) {
            TextView k = Ui.text(a, Fmt.kg(Math.round(f.kcal * i.grams / 100.0)) + " kcal", 12, Ui.FAINT);
            k.setGravity(Gravity.END);
            right.addView(k);
        }
        r.addView(right);
        wrap.addView(r);
        return wrap;
    }
}

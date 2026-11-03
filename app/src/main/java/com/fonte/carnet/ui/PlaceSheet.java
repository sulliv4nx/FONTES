package com.fonte.carnet.ui;

import android.app.Dialog;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Composer;
import com.fonte.carnet.core.Days;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.MealPlan;
import com.fonte.carnet.core.MealPlanner;
import com.fonte.carnet.core.MealPrefs;
import com.fonte.carnet.core.Recipe;
import com.fonte.carnet.data.Store;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Composer mes repas, étape 4 : placer les plats validés (toute la semaine, ou un repas précis). */
final class PlaceSheet {
    private PlaceSheet() {
    }

    private static final int WEEK = 0;
    private static final int ONE = 1;

    static void show(MainActivity a, ComposeSession s) {
        Store st = a.store;
        List<Recipe> dishes = new ArrayList<>();
        for (Composer.Proposal p : s.validated.values()) dishes.add(p.recipe);
        if (dishes.isEmpty()) return;
        int today = Days.today();
        MealPlan plan = st.mealPlan != null && today <= st.mealPlan.end() ? st.mealPlan : null;
        List<Integer> days = new ArrayList<>();
        if (plan != null) {
            for (int d = Math.max(plan.start, today); d <= plan.end(); d = Days.add(d, 1)) days.add(d);
        } else {
            for (int i = 0; i < 7; i++) days.add(Days.add(today, i));
        }
        final int[] mode = {WEEK};
        final int[] day = {days.get(0)};
        final String[] slot = {Recipe.MAIN.equals(MealPlan.kind(s.slot)) ? s.slot : MealPlan.LUNCH};
        final int[] dish = {0};

        LinearLayout box = Ui.vbox(a);
        int n = dishes.size();
        TextView title = Ui.text(a, "Placer " + (n > 1 ? "mes " + n + " repas" : "mon repas"), 20, Ui.TEXT);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        box.addView(title);
        TextView sub = Ui.text(a, "Les quantités s'ajustent au moment choisi.", 13, Ui.MUTED);
        sub.setPadding(0, Ui.dp(4), 0, Ui.dp(8));
        box.addView(sub);
        LinearLayout body = Ui.vbox(a);
        box.addView(body);

        List<String> need = s.needed();
        StringBuilder foot = new StringBuilder();
        if (!need.isEmpty()) foot.append(ComposeSession.names(need)).append(need.size() > 1 ? " s'ajoutent" : " s'ajoute")
                .append(" à ta liste de courses. ");
        foot.append("Tes ingrédients y sont notés comme déjà là.");
        TextView note = Ui.text(a, foot.toString(), 12, Ui.FAINT);
        note.setPadding(0, Ui.dp(12), 0, 0);
        box.addView(note);

        Dialog d = Ui.sheet(a, box);
        TextView cancel = Ui.smallButton(a, "Annuler", Ui.GHOST, v -> d.dismiss());
        cancel.setTextColor(Ui.TEXT);
        cancel.setMinHeight(Ui.dp(52));
        TextView go = Ui.smallButton(a, "Remplir ma semaine", Ui.PRIMARY, null);
        go.setMinHeight(Ui.dp(52));
        box.addView(Ui.buttonRow(a, cancel, go));

        Runnable[] render = new Runnable[1];
        render[0] = () -> {
            body.removeAllViews();
            body.addView(weekOption(a, st, plan, dishes, mode[0] == WEEK, v -> {
                mode[0] = WEEK;
                render[0].run();
            }));
            body.addView(oneOption(a, st, plan, dishes, days, day, slot, dish, mode[0] == ONE, v -> {
                mode[0] = ONE;
                render[0].run();
            }, render[0]));
            go.setText(mode[0] == WEEK ? "Remplir ma semaine" : "Placer ce repas");
        };
        render[0].run();

        go.setOnClickListener(v -> {
            if (mode[0] == WEEK) {
                int count = st.placeWeek(dishes, s.selected);
                s.validated.clear();
                d.dismiss();
                Ui.toast(a, count > 0 ? count + " repas placés dans ta semaine" : "Aucun repas à remplacer (tous déjà cochés)");
                openPlan(a, Days.today());
            } else {
                MealPlan p = st.ensureMealPlan();
                MealPlan.Meal target = p.find(day[0], slot[0]);
                if (target == null) {
                    Ui.toast(a, "Ce moment n'existe pas ce jour-là.");
                    return;
                }
                if (target.eaten) {
                    Ui.toast(a, "Ce repas est déjà coché comme mangé.");
                    return;
                }
                Recipe r = dishes.get(Math.min(dish[0], dishes.size() - 1));
                st.placeOne(r, target, s.selected);
                s.validated.remove(r.id);
                d.dismiss();
                Ui.toast(a, "Placé : " + MealPlanner.weekdayLong(day[0]) + " · " + MealPlan.slotLabel(slot[0]).toLowerCase(Locale.FRENCH));
                if (s.validated.isEmpty()) openPlan(a, day[0]);
                else a.refresh();
            }
        });
        d.show();
    }

    /** Retour à « Mes repas » sur le jour indiqué. */
    private static void openPlan(MainActivity a, int day) {
        Screen s = a.backTo(MealPlanScreen.class);
        if (s instanceof MealPlanScreen) {
            ((MealPlanScreen) s).select(day);
            a.refresh();
        } else {
            a.show(new MealPlanScreen(day));
        }
    }

    private static LinearLayout optionCard(MainActivity a, String title, String sub, boolean on, View.OnClickListener l) {
        LinearLayout card = Ui.vbox(a);
        card.setBackground(Ui.pressable(on ? Ui.outline(0x1469E6D2, Ui.ACCENT, 16) : Ui.outline(Ui.BG, Ui.LINE, 16), 16));
        card.setPadding(Ui.dp(14), Ui.dp(14), Ui.dp(14), Ui.dp(14));
        card.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 10));
        card.setOnClickListener(l);
        LinearLayout head = Ui.hbox(a);
        View radio = new View(a);
        radio.setBackground(on ? Ui.outline(Ui.ACCENT, Ui.ACCENT, 11) : Ui.outline(0x00000000, 0xFF4A5A6D, 11));
        head.addView(radio, Ui.lp(Ui.dp(22), Ui.dp(22)));
        TextView t = Ui.medium(a, title, 16, Ui.TEXT);
        t.setPadding(Ui.dp(12), 0, 0, 0);
        head.addView(t, Ui.weight(1));
        head.setContentDescription(title + (on ? ", choisi" : ""));
        card.addView(head);
        if (sub != null) {
            TextView s = Ui.text(a, sub, 13, Ui.MUTED);
            s.setPadding(Ui.dp(34), Ui.dp(6), 0, 0);
            card.addView(s);
        }
        return card;
    }

    private static View weekOption(MainActivity a, Store st, MealPlan plan, List<Recipe> dishes, boolean on, View.OnClickListener l) {
        String sub = plan == null
                ? "Un plan de 7 jours est créé à partir d'aujourd'hui avec tes réglages, puis tes plats remplacent les repas correspondants."
                : "Tes plats sont répartis " + (plan.start < Days.today() ? "d'aujourd'hui au " + MealPlanScreen.dayMonth(plan.end())
                : plan.rangeLabel()) + ". Les autres repas ne changent pas.";
        LinearLayout card = optionCard(a, "Remplir ma semaine", sub, on, l);
        if (!on || plan == null) return card;
        List<Composer.Placement> list = Composer.fillWeek(plan, dishes, Days.today(), st.mealPrefs().batch);
        LinearLayout prev = Ui.vbox(a);
        prev.setBackground(Ui.rect(Ui.SURFACE, 12));
        prev.setPadding(Ui.dp(12), Ui.dp(4), Ui.dp(12), Ui.dp(4));
        LinearLayout.LayoutParams lp = Ui.mlp(-1, -2, 34, 10, 0, 0);
        int shown = 0;
        for (Composer.Placement p : list) {
            if (shown == 4) break;
            LinearLayout row = Ui.hbox(a);
            row.setGravity(Gravity.TOP);
            row.setPadding(0, Ui.dp(7), 0, Ui.dp(7));
            TextView when = Ui.text(a, MealPlanner.weekdayShort(p.old.day) + " · " + short_(p.old.slot), 13, Ui.FAINT);
            row.addView(when, Ui.lp(Ui.dp(92), -2));
            String name = p.fresh.name;
            if (plan.find(Days.add(p.old.day, -1), p.old.slot) != null) {
                MealPlan.Meal prevDay = null;
                for (Composer.Placement q : list) if (q.old.day == Days.add(p.old.day, -1) && q.old.slot.equals(p.old.slot)) prevDay = q.fresh;
                if (prevDay != null && prevDay.recipeId.equals(p.fresh.recipeId)) name += " · en lot";
            }
            row.addView(Ui.text(a, name, 13, Ui.TEXT), Ui.weight(1));
            prev.addView(row);
            shown++;
        }
        if (list.size() > shown) {
            TextView more = Ui.text(a, "+ " + (list.size() - shown) + " autres repas jusqu'à " + MealPlanner.weekdayLong(plan.end()), 13, Ui.FAINT);
            more.setPadding(0, Ui.dp(7), 0, Ui.dp(7));
            prev.addView(more);
        }
        if (list.isEmpty()) prev.addView(Ui.text(a, "Tous les repas concernés sont déjà cochés.", 13, Ui.FAINT));
        card.addView(prev, lp);
        return card;
    }

    private static String short_(String slot) {
        switch (slot) {
            case MealPlan.LUNCH:
                return "midi";
            case MealPlan.DINNER:
                return "soir";
            case MealPlan.BREAKFAST:
                return "matin";
            case MealPlan.SHAKER:
                return "shaker";
            default:
                return "collation";
        }
    }

    private static View oneOption(MainActivity a, Store st, MealPlan plan, List<Recipe> dishes, List<Integer> days,
                                  int[] day, String[] slot, int[] dish, boolean on, View.OnClickListener l, Runnable rerender) {
        LinearLayout card = optionCard(a, "Un repas précis", on ? null : "Tu choisis le jour et le moment.", on, l);
        if (!on) return card;
        LinearLayout inner = Ui.vbox(a);
        LinearLayout.LayoutParams ilp = Ui.mlp(-1, -2, 0, 12, 0, 0);

        // Jours
        LinearLayout strip = Ui.hbox(a);
        for (int i = 0; i < days.size(); i++) {
            int d = days.get(i);
            boolean sel = d == day[0];
            LinearLayout cell = Ui.vbox(a);
            cell.setGravity(Gravity.CENTER);
            cell.setMinimumHeight(Ui.dp(52));
            cell.setBackground(Ui.pressable(sel ? Ui.rect(Ui.ACCENT, 12) : Ui.outline(Ui.BG, Ui.LINE, 12), 12));
            TextView w = Ui.text(a, MealPlanner.weekdayShort(d), 11, sel ? Ui.BG : Ui.MUTED);
            w.setGravity(Gravity.CENTER);
            cell.addView(w, Ui.lp(-1, -2));
            TextView num = Ui.medium(a, String.valueOf(d % 100), 15, sel ? Ui.BG : Ui.TEXT);
            num.setGravity(Gravity.CENTER);
            cell.addView(num, Ui.lp(-1, -2));
            cell.setContentDescription(MealPlanner.weekdayLong(d) + " " + (d % 100));
            cell.setOnClickListener(v -> {
                day[0] = d;
                rerender.run();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) lp.leftMargin = Ui.dp(4);
            strip.addView(cell, lp);
        }
        inner.addView(strip);

        // Moments du jour
        List<String> slots = new ArrayList<>();
        if (plan != null && plan.covers(day[0])) {
            for (MealPlan.Meal m : plan.mealsOf(day[0])) slots.add(m.slot);
        } else {
            MealPrefs prefs = st.mealPrefs();
            slots.addAll(MealPlanner.slots(prefs.meals, MealPlanner.shakerDay(prefs, day[0])));
        }
        if (!slots.contains(slot[0])) slot[0] = slots.contains(MealPlan.LUNCH) ? MealPlan.LUNCH : slots.get(0);
        List<View> chips = new ArrayList<>();
        for (String sl : slots) {
            MealPlan.Meal m = plan != null ? plan.find(day[0], sl) : null;
            boolean eaten = m != null && m.eaten;
            String label = slotChip(sl) + (eaten ? " ✓" : "");
            TextView c = Ui.chip(a, label, sl.equals(slot[0]), v -> {
                if (eaten) {
                    Ui.toast(a, "Déjà mangé : choisis un autre moment.");
                    return;
                }
                slot[0] = sl;
                rerender.run();
            });
            c.setTextSize(13);
            c.setMinHeight(Ui.dp(40));
            if (eaten) c.setAlpha(0.5f);
            c.setLayoutParams(new android.view.ViewGroup.LayoutParams(-2, -2));
            chips.add(c);
        }
        View flow = Ui.flow(a, chips.toArray(new View[0]));
        inner.addView(flow, Ui.mlp(-1, -2, 0, 10, 0, 0));

        // Plat
        TextView pl = Ui.medium(a, "PLAT", 11, Ui.MUTED);
        pl.setLetterSpacing(0.06f);
        pl.setPadding(0, Ui.dp(12), 0, Ui.dp(2));
        inner.addView(pl);
        MealPlan.Meal target = plan != null ? plan.find(day[0], slot[0]) : null;
        for (int i = 0; i < dishes.size(); i++) {
            Recipe r = dishes.get(i);
            boolean sel = i == Math.min(dish[0], dishes.size() - 1);
            final int idx = i;
            LinearLayout row = Ui.hbox(a);
            row.setMinimumHeight(Ui.dp(48));
            row.setBackground(Ui.pressable(Ui.rect(0x00000000, 8), 8));
            View radio = new View(a);
            radio.setBackground(sel ? Ui.outline(Ui.ACCENT, Ui.ACCENT, 11) : Ui.outline(0x00000000, 0xFF4A5A6D, 11));
            row.addView(radio, Ui.lp(Ui.dp(22), Ui.dp(22)));
            TextView name = Ui.text(a, r.name, 15, Ui.TEXT);
            name.setPadding(Ui.dp(12), 0, Ui.dp(8), 0);
            row.addView(name, Ui.weight(1));
            double kcal = target != null ? Composer.fit(r, target).totals().kcal
                    : MealPlanner.build(r, day[0], slot[0], st.slotTarget(slot[0])[0], st.slotTarget(slot[0])[1], st.slotTarget(slot[0])[2]).totals().kcal;
            row.addView(Ui.text(a, Fmt.kg(Math.round(kcal)) + " kcal", 13, Ui.MUTED));
            row.setContentDescription(r.name + (sel ? ", choisi" : ""));
            row.setOnClickListener(v -> {
                dish[0] = idx;
                rerender.run();
            });
            inner.addView(row);
        }
        if (target != null) {
            Recipe r = dishes.get(Math.min(dish[0], dishes.size() - 1));
            LinearLayout swap = Ui.vbox(a);
            swap.setBackground(Ui.rect(Ui.SURFACE, 12));
            swap.setPadding(Ui.dp(12), Ui.dp(10), Ui.dp(12), Ui.dp(10));
            swap.addView(Ui.text(a, "Remplace : " + target.name + " · " + Fmt.kg(Math.round(target.totals().kcal)) + " kcal", 13, Ui.FAINT));
            TextView by = Ui.text(a, "Par : " + r.name + " · " + Fmt.kg(Math.round(Composer.fit(r, target).totals().kcal)) + " kcal", 13, Ui.TEXT);
            by.setPadding(0, Ui.dp(3), 0, 0);
            swap.addView(by);
            inner.addView(swap, Ui.mlp(-1, -2, 0, 8, 0, 0));
        }
        if (dishes.size() > 1) {
            TextView rest = Ui.text(a, "Les autres plats restent dans tes repas validés.", 12, Ui.FAINT);
            rest.setPadding(0, Ui.dp(8), 0, 0);
            inner.addView(rest);
        }
        card.addView(inner, ilp);
        return card;
    }

    private static String slotChip(String slot) {
        switch (slot) {
            case MealPlan.BREAKFAST:
                return "Petit-déjeuner";
            case MealPlan.SNACK_AM:
                return "Collation 10 h";
            case MealPlan.LUNCH:
                return "Déjeuner";
            case MealPlan.SNACK_PM:
                return "Collation 16 h";
            case MealPlan.SHAKER:
                return "Shaker";
            default:
                return "Dîner";
        }
    }
}

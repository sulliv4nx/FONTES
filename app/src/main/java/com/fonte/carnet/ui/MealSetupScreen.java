package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Days;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.MealPlanner;
import com.fonte.carnet.core.MealPrefs;
import com.fonte.carnet.core.Nutrition;

import java.util.Locale;

/** Créer (ou régler) mon plan de repas. */
public class MealSetupScreen extends ScrollScreen {
    private MealPrefs p;
    private int start;

    @Override
    public String title() {
        return st.mealPlan == null ? "Créer mon plan" : "Régler mon plan";
    }

    @Override
    public View build() {
        if (p == null) {
            p = st.mealPrefs().copy();
            start = Days.today();
        }
        LinearLayout c = Ui.vbox(a);
        c.addView(goalCard());
        c.addView(mealsCard());
        c.addView(shakerCard());
        c.addView(prefsCard());
        c.addView(batchCard());
        c.addView(startCard());
        c.addView(Ui.space(a, 4));
        TextView go = Ui.button(a, "Générer ma semaine", Ui.PRIMARY, v -> generate());
        c.addView(go);
        TextView hint = Ui.text(a, "Tu pourras ensuite changer n'importe quel plat. Les quantités sont recalculées pour garder tes objectifs.", 12, Ui.FAINT);
        hint.setPadding(0, Ui.dp(8), 0, 0);
        c.addView(hint);
        return page(c);
    }

    private View goalCard() {
        Nutrition.Macros m = st.targetMacros();
        LinearLayout card = Ui.card(a);
        LinearLayout row = Ui.hbox(a);
        row.addView(Ui.iconBadge(a, Icon.SPARK, Ui.ACCENT));
        LinearLayout txt = Ui.vbox(a);
        txt.setPadding(Ui.dp(12), 0, Ui.dp(8), 0);
        TextView label = Ui.medium(a, "MON OBJECTIF", 11, Ui.MUTED);
        label.setLetterSpacing(0.06f);
        txt.addView(label);
        txt.addView(Ui.medium(a, st.profile.goalLabel() + " · " + Fmt.kg(m.kcal) + " kcal", 16, Ui.TEXT));
        txt.addView(Ui.text(a, "P " + m.protein + " g · G " + m.carbs + " g · L " + m.fat + " g", 13, Ui.MUTED));
        row.addView(txt, Ui.weight(1));
        TextView edit = Ui.medium(a, "Modifier", 14, Ui.ACCENT);
        edit.setPadding(Ui.dp(8), Ui.dp(12), 0, Ui.dp(12));
        edit.setOnClickListener(v -> a.show(new ProfileScreen()));
        row.addView(edit);
        card.addView(row);
        if (!st.profile.complete()) {
            TextView t = Ui.text(a, "Profil incomplet : ces objectifs sont des valeurs par défaut. Renseigne ta taille et ton âge pour un plan juste.", 12, Ui.WARN);
            t.setPadding(0, Ui.dp(10), 0, 0);
            card.addView(t);
        }
        return card;
    }

    private TextView label(String s) {
        TextView t = Ui.medium(a, s.toUpperCase(Locale.FRENCH), 11, Ui.MUTED);
        t.setLetterSpacing(0.06f);
        t.setPadding(0, 0, 0, Ui.dp(10));
        return t;
    }

    private View mealsCard() {
        LinearLayout card = Ui.card(a);
        card.addView(label("Repas par jour"));
        int sel = p.meals - 3;
        card.addView(Ui.segmented(a, new String[]{"3", "4", "5"}, sel, i -> {
            p.meals = i + 3;
            a.refresh();
        }));
        TextView t = Ui.text(a, p.mealsLabel(), 13, Ui.MUTED);
        t.setPadding(0, Ui.dp(8), 0, 0);
        card.addView(t);
        return card;
    }

    private View shakerCard() {
        LinearLayout card = Ui.card(a);
        card.addView(Ui.switchRow(a, "Shaker protéiné", p.meals >= 4 ? "Remplace la collation de l'après-midi" : "S'ajoute après la séance",
                p.shaker, on -> {
                    p.shaker = on;
                    a.refresh();
                }));
        if (!p.shaker) return card;
        LinearLayout seg = Ui.segmented(a, new String[]{"Jours d'entraînement", "Tous les jours"}, p.shakerEveryDay ? 1 : 0, i -> {
            p.shakerEveryDay = i == 1;
            a.refresh();
        });
        seg.setLayoutParams(Ui.mlp(-1, -2, 0, 8, 0, 0));
        card.addView(seg);
        if (!p.shakerEveryDay) {
            LinearLayout days = Ui.hbox(a);
            days.setLayoutParams(Ui.mlp(-1, -2, 0, 10, 0, 0));
            String[] l = {"L", "M", "M", "J", "V", "S", "D"};
            String[] names = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"};
            int mask = Math.max(0, p.trainingMask);
            for (int i = 0; i < 7; i++) {
                final int bit = 1 << i;
                boolean on = (mask & bit) != 0;
                TextView d = Ui.medium(a, l[i], 14, on ? Ui.BG : Ui.MUTED);
                d.setGravity(Gravity.CENTER);
                d.setMinHeight(Ui.dp(40));
                d.setBackground(Ui.pressable(on ? Ui.rect(Ui.ACCENT, 10) : Ui.outline(0x00000000, Ui.LINE, 10), 10));
                d.setContentDescription(names[i] + (on ? ", jour d'entraînement" : ""));
                d.setOnClickListener(v -> {
                    p.trainingMask = Math.max(0, p.trainingMask) ^ bit;
                    a.refresh();
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
                if (i > 0) lp.leftMargin = Ui.dp(5);
                days.addView(d, lp);
            }
            card.addView(days);
            TextView t = Ui.text(a, "Jours repérés d'après tes séances des 4 dernières semaines. Touche un jour pour le changer.", 12, Ui.FAINT);
            t.setPadding(0, Ui.dp(8), 0, 0);
            card.addView(t);
        }
        return card;
    }

    private View prefsCard() {
        LinearLayout card = Ui.card(a);
        card.addView(label("Préférences"));
        card.addView(Ui.flow(a,
                Ui.toggleChip(a, "Sans porc", p.noPork, v -> {
                    p.noPork = !p.noPork;
                    a.refresh();
                }),
                Ui.toggleChip(a, "Végétarien", p.vegetarian, v -> {
                    p.vegetarian = !p.vegetarian;
                    a.refresh();
                }),
                Ui.toggleChip(a, "Sans lactose", p.lactoseFree, v -> {
                    p.lactoseFree = !p.lactoseFree;
                    a.refresh();
                }),
                Ui.toggleChip(a, "Poisson 2×/sem", p.fish, v -> {
                    p.fish = !p.fish;
                    a.refresh();
                }),
                Ui.toggleChip(a, "Cuisine créole", p.creole, v -> {
                    p.creole = !p.creole;
                    a.refresh();
                }),
                Ui.toggleChip(a, "Petit budget", p.budget, v -> {
                    p.budget = !p.budget;
                    a.refresh();
                })));
        EditText avoid = Ui.field(a, "Aliments à éviter : ex. champignons, thon", Ui.TEXT_FIELD);
        avoid.setText(p.avoid);
        avoid.setSelectAllOnFocus(false);
        Ui.onText(avoid, s -> p.avoid = s);
        avoid.setLayoutParams(Ui.mlp(-1, -2, 0, 12, 0, 0));
        card.addView(avoid);
        if (p.vegetarian && p.fish) {
            TextView t = Ui.text(a, "Végétarien et poisson : le poisson ne sera pas proposé.", 12, Ui.WARN);
            t.setPadding(0, Ui.dp(8), 0, 0);
            card.addView(t);
        }
        return card;
    }

    private View batchCard() {
        LinearLayout card = Ui.card(a);
        card.addView(Ui.switchRow(a, "Cuisine en lot", "Un plat cuisiné pour 2 jours", p.batch, on -> {
            p.batch = on;
            a.refresh();
        }));
        TextView l = label("Variété");
        l.setPadding(0, Ui.dp(12), 0, Ui.dp(10));
        card.addView(l);
        card.addView(Ui.segmented(a, MealPrefs.VARIETY, p.variety, i -> {
            p.variety = i;
            a.refresh();
        }));
        String[] sub = {"Peu de recettes différentes, courses plus simples", "Bon compromis entre variété et courses",
                "Un maximum de plats différents dans la semaine"};
        TextView t = Ui.text(a, sub[p.variety], 13, Ui.MUTED);
        t.setPadding(0, Ui.dp(8), 0, 0);
        card.addView(t);
        return card;
    }

    private View startCard() {
        LinearLayout card = Ui.card(a);
        LinearLayout row = Ui.hbox(a);
        android.widget.ImageView cal = new android.widget.ImageView(a);
        cal.setImageDrawable(new Icon(Icon.PROGRAM, Ui.MUTED));
        row.addView(cal, Ui.lp(Ui.dp(24), Ui.dp(24)));
        LinearLayout txt = Ui.vbox(a);
        txt.setPadding(Ui.dp(12), 0, Ui.dp(8), 0);
        txt.addView(Ui.medium(a, "7 jours", 15, Ui.TEXT));
        txt.addView(Ui.text(a, "À partir " + startLabel(start), 13, Ui.MUTED));
        row.addView(txt, Ui.weight(1));
        TextView change = Ui.medium(a, "Changer", 14, Ui.ACCENT);
        change.setPadding(Ui.dp(8), Ui.dp(12), 0, Ui.dp(12));
        change.setOnClickListener(v -> chooseStart());
        row.addView(change);
        card.addView(row);
        return card;
    }

    private static String startLabel(int day) {
        int t = Days.today();
        String date = MealPlanner.weekdayLong(day) + " " + MealPlanScreen.dayMonth(day);
        if (day == t) return "d'aujourd'hui, " + date;
        if (day == Days.add(t, 1)) return "de demain, " + date;
        return "du " + date;
    }

    private void chooseStart() {
        int t = Days.today();
        int monday = Days.monday(t);
        int nextMonday = Days.add(monday, 7);
        java.util.List<Integer> options = new java.util.ArrayList<>();
        options.add(t);
        options.add(Days.add(t, 1));
        if (monday != t) options.add(monday);
        if (nextMonday != Days.add(t, 1)) options.add(nextMonday);
        String[] labels = new String[options.size()];
        for (int i = 0; i < labels.length; i++) {
            int d = options.get(i);
            String w = Fmt.cap(MealPlanner.weekdayLong(d)) + " " + MealPlanScreen.dayMonth(d);
            labels[i] = d == t ? "Aujourd'hui · " + w : d == Days.add(t, 1) ? "Demain · " + w
                    : d == monday ? "Lundi de cette semaine · " + MealPlanScreen.dayMonth(d) : "Lundi prochain · " + MealPlanScreen.dayMonth(d);
        }
        new AlertDialog.Builder(a)
                .setTitle("Début du plan")
                .setItems(labels, (dlg, which) -> {
                    start = options.get(which);
                    a.refresh();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void generate() {
        Ui.hideKeyboard(a.getWindow().getDecorView());
        if (p.shaker && !p.shakerEveryDay && p.trainingMask <= 0) {
            Ui.toast(a, "Choisis au moins un jour d'entraînement, ou « Tous les jours ».");
            return;
        }
        Runnable go = () -> {
            st.generateMealPlan(p.copy(), start);
            a.back();
            if (!(a.currentScreen() instanceof MealPlanScreen)) a.show(new MealPlanScreen(start));
            else a.refresh();
            Ui.toast(a, "Ta semaine est prête");
        };
        if (st.mealPlan != null) {
            Ui.confirm(a, "Remplacer ton plan actuel ?", "Les repas déjà cochés restent dans ton journal.", "Remplacer", go);
        } else {
            go.run();
        }
    }
}

package com.fonte.carnet.ui;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.BodyLog;
import com.fonte.carnet.core.Days;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.FoodEntry;
import com.fonte.carnet.core.Nutrition;
import com.fonte.carnet.core.Program;
import com.fonte.carnet.core.RenfoLog;
import com.fonte.carnet.core.Session;
import com.fonte.carnet.core.Stats;
import com.fonte.carnet.core.WodResult;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Accueil : le récapitulatif du jour (nutrition, eau, dépense, entraînement, corps, semaine). */
public class DashboardScreen extends Screen {

    @Override
    public String title() {
        return "Paradoxe Training";
    }

    @Override
    public int actionIcon() {
        return Icon.GEAR;
    }

    @Override
    public void onAction() {
        a.show(new SettingsScreen());
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        long now = System.currentTimeMillis();
        int day = Days.today();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String hello = hour < 5 ? "Bonsoir" : hour < 12 ? "Bonjour" : hour < 18 ? "Bon après-midi" : "Bonsoir";
        TextView h = Ui.heading(a, hello);
        c.addView(h);
        TextView date = Ui.text(a, Fmt.day(now), 14, Ui.MUTED);
        date.setPadding(0, 0, 0, Ui.dp(16));
        c.addView(date);

        if (!st.profile.complete() || st.weights.isEmpty()) c.addView(setupCard());
        if (st.current != null) c.addView(sessionCard());
        View running = CrossFitScreen.runningCard(a);
        if (running != null) c.addView(running);

        c.addView(nutritionCard(day));
        View meal = nextMealCard(day);
        if (meal != null) c.addView(meal);
        c.addView(Ui.grid(a, 2, waterCard(day), burnCard(day)));
        c.addView(trainingCard());
        c.addView(bodyCard());
        c.addView(weekCard(day));
        View rec = recordsCard();
        if (rec != null) c.addView(rec);
        return Ui.page(a, c);
    }

    // ---------------------------------------------------------------- cartes

    private View setupCard() {
        LinearLayout card = Ui.card(a);
        card.setBackground(Ui.outline(Ui.ACCENT_SOFT, Ui.ACCENT, 18));
        card.addView(Ui.cardHeader(a, Icon.SPARK, Ui.ACCENT, "Configure ton profil", null, null));
        card.addView(Ui.hint(a, "Taille, âge, poids et objectif : l'appli calcule tes besoins en calories, en macros et en eau."));
        card.addView(Ui.button(a, "Configurer", Ui.PRIMARY, v -> a.show(new ProfileScreen())));
        return card;
    }

    private View sessionCard() {
        Session cur = st.current;
        LinearLayout card = Ui.clickCard(a, v -> a.openSession());
        card.setBackground(Ui.pressable(Ui.outline(Ui.ACCENT_SOFT, Ui.ACCENT, 18), 18));
        card.addView(Ui.cardHeader(a, Icon.BARBELL, Ui.ACCENT, "Séance en cours", null, null));
        card.addView(Ui.medium(a, cur.name, 16, Ui.TEXT));
        card.addView(Ui.text(a, Fmt.plural(cur.doneSets(), "série") + " · " + Fmt.duration(cur.durationMs()), 13, Ui.MUTED));
        card.addView(Ui.button(a, "Reprendre", Ui.PRIMARY, v -> a.openSession()));
        return card;
    }

    private View nutritionCard(int day) {
        Nutrition.Totals t = Nutrition.totals(st.foodEntries, day, null);
        Nutrition.Targets target = st.targets(day);
        int goal = target.kcal;
        Nutrition.Macros m = target.macros();
        LinearLayout card = Ui.clickCard(a, v -> a.goTab(MainActivity.TAB_NUTRITION));
        card.addView(Ui.cardHeader(a, Icon.NUTRITION, Ui.ACCENT, "Nutrition du jour", "Journal",
                v -> a.goTab(MainActivity.TAB_NUTRITION)));
        LinearLayout row = Ui.hbox(a);
        RingView ring = new RingView(a);
        long left = Math.round(goal - t.kcal);
        ring.set(goal > 0 ? (float) (t.kcal / goal) : 0f, Fmt.kg(Math.abs(left)),
                left >= 0 ? "kcal restantes" : "kcal en trop", Ui.ACCENT);
        row.addView(ring, Ui.lp(Ui.dp(128), Ui.dp(128)));
        LinearLayout macros = Ui.vbox(a);
        macros.setPadding(Ui.dp(16), 0, 0, 0);
        macros.addView(Ui.meter(a, "Protéines", t.protein, m.protein, "g", Ui.PROTEIN));
        macros.addView(Ui.meter(a, "Glucides", t.carbs, m.carbs, "g", Ui.CARBS));
        macros.addView(Ui.meter(a, "Lipides", t.fat, m.fat, "g", Ui.FAT));
        row.addView(macros, Ui.weight(1));
        card.addView(row);
        TextView sum = Ui.text(a, Fmt.kg(Math.round(t.kcal)) + " kcal consommées sur " + Fmt.kg(goal)
                + " (" + st.profile.goalLabel().toLowerCase(java.util.Locale.FRENCH)
                + (target.sessionKcal > 0 ? (target.planned ? ", séance prévue comprise" : ", séances comprises") : "") + ")", 12, Ui.MUTED);
        sum.setPadding(0, Ui.dp(8), 0, 0);
        card.addView(sum);
        card.addView(Ui.button(a, "Ajouter un repas", Ui.SECONDARY,
                v -> a.show(new FoodAddScreen(FoodEntry.mealFor(System.currentTimeMillis()), day))));
        return card;
    }

    /** Prochain repas prévu dans « Mes repas », s'il y a un plan pour aujourd'hui. */
    private View nextMealCard(int day) {
        if (st.dayPrefs.dayMode) return nextDayMealCard(day);
        com.fonte.carnet.core.MealPlan.Meal m = MealPlanScreen.nextMeal(st.mealPlan, day);
        if (m == null) return null;
        LinearLayout card = Ui.clickCard(a, v -> a.show(new MealPlanScreen(day)));
        LinearLayout row = Ui.hbox(a);
        row.addView(Ui.iconBadge(a, Icon.SPARK, Ui.ACCENT));
        LinearLayout txt = Ui.vbox(a);
        txt.setPadding(Ui.dp(12), 0, Ui.dp(8), 0);
        txt.addView(Ui.text(a, "À manger · " + com.fonte.carnet.core.MealPlan.slotLabel(m.slot), 12, Ui.MUTED));
        TextView n = Ui.medium(a, m.name, 15, Ui.TEXT);
        n.setMaxLines(2);
        txt.addView(n);
        row.addView(txt, Ui.weight(1));
        TextView k = Ui.medium(a, Fmt.kg(Math.round(m.totals().kcal)) + " kcal", 14, Ui.TEXT);
        Ui.tabular(k);
        row.addView(k);
        card.addView(row);
        return card;
    }

    /** Prochain repas de la journée type. */
    private View nextDayMealCard(int day) {
        com.fonte.carnet.core.DayPlanner.Day plan = st.dayPlan(day);
        com.fonte.carnet.core.DayPlanner.Slot s = DayPlanScreen.nextSlot(st, plan, day);
        if (s == null) return null;
        LinearLayout card = Ui.clickCard(a, v -> a.show(new DayPlanScreen(day)));
        LinearLayout row = Ui.hbox(a);
        row.addView(Ui.iconBadge(a, Icon.SPARK, Ui.ACCENT));
        LinearLayout txt = Ui.vbox(a);
        txt.setPadding(Ui.dp(12), 0, Ui.dp(8), 0);
        txt.addView(Ui.text(a, "À manger · " + com.fonte.carnet.core.DayPrefs.clock(s.minutes), 12, Ui.MUTED));
        TextView n = Ui.medium(a, com.fonte.carnet.core.DayPlanner.title(s, plan.training), 15, Ui.TEXT);
        n.setMaxLines(2);
        txt.addView(n);
        row.addView(txt, Ui.weight(1));
        TextView k = Ui.medium(a, Math.round(s.kcal) + " kcal", 14, Ui.TEXT);
        Ui.tabular(k);
        row.addView(k);
        card.addView(row);
        return card;
    }

    private LinearLayout waterCard(int day) {
        int ml = Nutrition.waterOf(st.water, day);
        int goal = st.waterTarget(day);
        LinearLayout card = Ui.card(a);
        card.addView(Ui.cardHeader(a, Icon.DROP, Ui.WATER, "Eau", null, null));
        card.addView(Ui.bigValue(a, Fmt.kg(ml / 1000.0) + " L", "sur " + Fmt.kg(goal / 1000.0) + " L"));
        LinearLayout bar = Ui.hbox(a);
        bar.setBackground(Ui.rect(Ui.SURFACE2, 3));
        bar.setWeightSum(1f);
        View fill = new View(a);
        fill.setBackground(Ui.rect(Ui.WATER, 3));
        bar.addView(fill, new LinearLayout.LayoutParams(0, -1, goal > 0 ? Math.min(1f, ml / (float) goal) : 0f));
        card.addView(bar, Ui.mlp(-1, Ui.dp(6), 0, 10, 0, 4));
        card.addView(new View(a), new LinearLayout.LayoutParams(1, 0, 1));
        TextView add = Ui.button(a, "+ 250 ml", Ui.SECONDARY, v -> {
            st.addWater(250, day);
            a.refresh();
        });
        add.setMinHeight(Ui.dp(44));
        card.addView(add);
        return card;
    }

    private LinearLayout burnCard(int day) {
        List<Nutrition.Burn> burns = st.burned(day);
        int total = Nutrition.total(burns);
        LinearLayout card = Ui.card(a);
        card.addView(Ui.cardHeader(a, Icon.FLAME, Ui.BURN, "Dépense", null, null));
        card.addView(Ui.bigValue(a, total + " kcal", burns.isEmpty() ? "aucune activité" : Fmt.plural(burns.size(), "activité")));
        TextView base = Ui.text(a, "Calories nettes, ajoutées à ton objectif du jour", 12, Ui.MUTED);
        base.setPadding(0, Ui.dp(8), 0, Ui.dp(4));
        card.addView(base);
        card.addView(new View(a), new LinearLayout.LayoutParams(1, 0, 1));
        TextView add = Ui.button(a, "+ Activité", Ui.SECONDARY, v -> ActivityDialog.show(a, day, a::refresh));
        add.setMinHeight(Ui.dp(44));
        card.addView(add);
        return card;
    }

    private View trainingCard() {
        LinearLayout card = Ui.card(a);
        card.addView(Ui.cardHeader(a, Icon.BARBELL, Ui.ACCENT, "Entraînement", "Tout voir",
                v -> a.openTraining(TrainingScreen.segment)));
        Program p = st.program;
        int next = p == null ? -1 : p.nextIndex();
        if (next >= 0) {
            Program.Day d = p.days.get(next);
            card.addView(Ui.text(a, p.title, 13, Ui.MUTED));
            TextView nt = Ui.medium(a, d.group + " · " + d.name, 16, Ui.TEXT);
            nt.setPadding(0, Ui.dp(2), 0, Ui.dp(4));
            card.addView(nt);
            card.addView(Ui.button(a, "Commencer la séance", Ui.PRIMARY, v -> a.startProgramDay(next)));
        } else {
            card.addView(Ui.hint(a, "Choisis ta séance du jour : musculation, WOD ou circuit de renfo."));
        }
        LinearLayout row = Ui.hbox(a);
        String[] labels = {"Muscu", "WOD", "Renfo"};
        int[] segs = {TrainingScreen.MUSCU, TrainingScreen.CROSSFIT, TrainingScreen.RENFO};
        for (int i = 0; i < 3; i++) {
            int seg = segs[i];
            TextView b = Ui.button(a, labels[i], Ui.SECONDARY, v -> a.openTraining(seg));
            b.setMinHeight(Ui.dp(44));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            if (i < 2) lp.rightMargin = Ui.dp(8);
            row.addView(b, lp);
        }
        card.addView(row);
        return card;
    }

    private View bodyCard() {
        LinearLayout card = Ui.clickCard(a, v -> a.goTab(MainActivity.TAB_BODY));
        card.addView(Ui.cardHeader(a, Icon.BODY, Ui.ACCENT, "Corps", "Détail", v -> a.goTab(MainActivity.TAB_BODY)));
        double w = st.weight();
        if (w <= 0) {
            card.addView(Ui.hint(a, "Ajoute ta première pesée pour suivre ton poids et affiner tes besoins."));
        } else {
            LinearLayout row = Ui.hbox(a);
            row.addView(Ui.bigValue(a, Fmt.kg(w) + " kg", "le " + Fmt.shortDay(st.weights.get(0).time)), Ui.weight(1));
            double old = st.weightDaysAgo(30);
            if (old > 0) {
                double diff = Stats.round1(w - old);
                TextView d = Ui.medium(a, (diff > 0 ? "+" : "") + Fmt.kg(diff) + " kg\nen 30 jours", 13, Ui.MUTED);
                d.setGravity(Gravity.END);
                row.addView(d);
            }
            card.addView(row);
            if (st.weights.size() >= 2) {
                List<double[]> pts = new ArrayList<>();
                for (int i = Math.min(st.weights.size(), 30) - 1; i >= 0; i--) {
                    BodyLog.Weight x = st.weights.get(i);
                    pts.add(new double[]{x.time, x.kg});
                }
                ChartView chart = new ChartView(a);
                chart.setData(pts, "kg");
                card.addView(chart, Ui.mlp(-1, Ui.dp(120), 0, 10, 0, 0));
            }
            double bmi = Nutrition.bmi(w, st.profile.heightCm);
            if (bmi > 0) {
                TextView b = Ui.text(a, "IMC " + Fmt.kg(Stats.round1(bmi)) + " · " + Nutrition.bmiLabel(bmi), 12, Ui.MUTED);
                b.setPadding(0, Ui.dp(6), 0, 0);
                card.addView(b);
            }
        }
        card.addView(Ui.button(a, "+ Pesée", Ui.SECONDARY, v -> BodyScreen.weightDialog(a, a::refresh)));
        return card;
    }

    private View weekCard(int today) {
        LinearLayout card = Ui.card(a);
        card.addView(Ui.cardHeader(a, Icon.HISTORY, Ui.ACCENT, "Ma semaine", "Historique", v -> {
            ProgressScreen.segment = 0;
            a.goTab(MainActivity.TAB_PROGRESS);
        }));
        int monday = Days.monday(today);
        LinearLayout strip = Ui.hbox(a);
        int muscu = 0;
        int wods = 0;
        int renfo = 0;
        double tonnage = 0;
        for (int i = 0; i < 7; i++) {
            int d = Days.add(monday, i);
            int count = 0;
            for (Session s : st.sessions) {
                if (Days.key(s.start) == d) {
                    count++;
                    muscu++;
                    tonnage += s.tonnage();
                }
            }
            for (WodResult r : st.wodResults) {
                if (Days.key(r.date) == d) {
                    count++;
                    wods++;
                }
            }
            for (RenfoLog l : st.renfoLogs) {
                if (Days.key(l.date) == d) {
                    count++;
                    renfo++;
                }
            }
            LinearLayout col = Ui.vbox(a);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView ini = Ui.medium(a, Days.initial(d), 12, d == today ? Ui.ACCENT : Ui.MUTED);
            ini.setGravity(Gravity.CENTER);
            col.addView(ini, Ui.lp(-1, -2));
            TextView dot = Ui.medium(a, count > 0 ? String.valueOf(count) : "", 13, Ui.BG);
            dot.setGravity(Gravity.CENTER);
            if (count > 0) dot.setBackground(Ui.rect(Ui.ACCENT, 15));
            else dot.setBackground(Ui.outline(0x00000000, d > today ? Ui.SURFACE2 : Ui.LINE, 15));
            if (d == today && count == 0) dot.setBackground(Ui.outline(0x00000000, Ui.ACCENT, 15));
            col.addView(dot, Ui.mlp(Ui.dp(30), Ui.dp(30), 0, 6, 0, 0));
            strip.addView(col, new LinearLayout.LayoutParams(0, -2, 1));
        }
        card.addView(strip);
        TextView sum = Ui.text(a, Fmt.plural(muscu, "séance") + " de muscu · " + Fmt.plural(wods, "WOD") + " · "
                + renfo + " renfo · " + Fmt.tonnage(tonnage) + " soulevés", 13, Ui.MUTED);
        sum.setPadding(0, Ui.dp(12), 0, 0);
        card.addView(sum);
        return card;
    }

    private View recordsCard() {
        List<String> lines = new ArrayList<>();
        for (Session s : st.sessions) {
            for (String r : s.records) {
                if (lines.size() >= 3) break;
                lines.add(r);
            }
            if (lines.size() >= 3) break;
        }
        if (lines.isEmpty()) return null;
        LinearLayout card = Ui.clickCard(a, v -> {
            ProgressScreen.segment = 1;
            a.goTab(MainActivity.TAB_PROGRESS);
        });
        card.addView(Ui.cardHeader(a, Icon.TROPHY, Ui.ACCENT, "Derniers records", null, null));
        for (String l : lines) {
            TextView t = Ui.text(a, "• " + l, 14, Ui.TEXT);
            t.setPadding(0, Ui.dp(2), 0, Ui.dp(2));
            card.addView(t);
        }
        return card;
    }

    static TextView bold(MainActivity a, String s, float sp) {
        TextView t = Ui.text(a, s, sp, Ui.TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }
}

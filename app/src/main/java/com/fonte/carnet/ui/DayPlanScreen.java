package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Ciqual;
import com.fonte.carnet.core.DayPlanner;
import com.fonte.carnet.core.DayPrefs;
import com.fonte.carnet.core.Days;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Food;
import com.fonte.carnet.core.FoodSearch;
import com.fonte.carnet.core.Foods;
import com.fonte.carnet.core.MealPlanner;
import com.fonte.carnet.core.Nutrition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Journée type : les repas du jour à heures fixes, faits de blocs (glucides, protéines, légumes ou fruit, lipides),
 * chacun avec ses équivalences, sur le principe d'une diète de préparateur.
 */
public class DayPlanScreen extends ScrollScreen {
    static final int SESSION_TEXT = 0xFFFFB38A;
    static final int SESSION_BG = 0x1FFF8F5A;
    private static final int ROLE_CARBS = 0xFFFFB547;
    private static final int ROLE_PROTEIN = 0xFFFF7DBA;
    private static final int ROLE_FAT = 0xFFA9B8FF;
    private static final int ROLE_PLANT = 0xFF85DBA1;

    private int day;

    public DayPlanScreen(int day) {
        this.day = day == 0 ? Days.today() : day;
    }

    @Override
    public String title() {
        return "Mes repas";
    }

    @Override
    public int actionIcon() {
        return Icon.GEAR;
    }

    @Override
    public void onAction() {
        a.show(new DaySetupScreen());
    }

    /** Ouvre « Mes repas » dans le mode choisi (plats de la semaine ou journée type). */
    static void open(MainActivity a, int day) {
        if (a.store.dayPrefs.dayMode) a.show(new DayPlanScreen(day));
        else a.show(new MealPlanScreen(day));
    }

    /** Prochain repas non coché du jour (aujourd'hui : à partir de l'heure qu'il est, moins une heure). */
    static DayPlanner.Slot nextSlot(com.fonte.carnet.data.Store st, DayPlanner.Day plan, int day) {
        int now = -1;
        if (day == Days.today()) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            now = c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE);
        }
        DayPlanner.Slot first = null;
        for (DayPlanner.Slot s : plan.slots) {
            if (!s.main() || st.dayMealEaten(day, s.index)) continue;
            if (first == null) first = s;
            if (s.minutes + 60 >= now) return s;
        }
        return first;
    }

    /** Sélecteur « Plats de la semaine | Journée type », partagé avec l'écran des plats. */
    static View modeSwitch(MainActivity a, boolean dayMode, int day) {
        LinearLayout seg = Ui.segmented(a, new String[]{"Plats de la semaine", "Journée type"}, dayMode ? 1 : 0, i -> {
            boolean wanted = i == 1;
            if (wanted == dayMode) return;
            a.store.dayPrefs.dayMode = wanted;
            a.store.save();
            a.back();
            open(a, day);
        });
        seg.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 10));
        return seg;
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        c.addView(modeSwitch(a, true, day));
        c.addView(weekStrip(a, day, d -> {
            day = d;
            scrollTop();
            a.refresh();
        }));
        DayPlanner.Day plan = st.dayPlan(day);
        c.addView(summary(a, st, plan, day));

        LinearLayout wake = timeRow(DayPrefs.clock(st.dayPrefs.wake), "Au lever · un grand verre d'eau", null, false);
        c.addView(wake);
        boolean sessionShown = false;
        for (DayPlanner.Slot s : plan.slots) {
            if (plan.training && !sessionShown && s.minutes > plan.sessionTime) {
                c.addView(sessionCard(plan));
                sessionShown = true;
            }
            c.addView(s.main() ? mealCard(plan, s) : snackRow(s));
        }
        if (plan.training && !sessionShown) c.addView(sessionCard(plan));

        LinearLayout buttons = Ui.buttonRow(a,
                Ui.smallButton(a, "Résumé du jour", Ui.GHOST, v -> a.show(new DaySummaryScreen(day))),
                Ui.smallButton(a, "Liste de courses", Ui.GHOST, v -> a.show(new ShoppingScreen(true))));
        buttons.setPadding(0, Ui.dp(6), 0, 0);
        c.addView(buttons);
        TextView hint = Ui.text(a, "Touche un aliment pour voir ses équivalences. Poids cuits, avec l'équivalent cru pour la viande et le riz.",
                12, Ui.FAINT);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, Ui.dp(10), 0, 0);
        c.addView(hint);
        return page(c);
    }

    // ---------------------------------------------------------------- en-tête

    interface DayPick {
        void on(int day);
    }

    /** Lundi à dimanche de la semaine du jour affiché ; un point orange marque les jours de séance. */
    static View weekStrip(MainActivity a, int day, DayPick pick) {
        LinearLayout row = Ui.hbox(a);
        row.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 10));
        int monday = Days.monday(day);
        for (int i = 0; i < 7; i++) {
            int d = Days.add(monday, i);
            boolean sel = d == day;
            boolean training = a.store.plannedTraining(d);
            LinearLayout cell = Ui.vbox(a);
            cell.setGravity(Gravity.CENTER);
            cell.setMinimumHeight(Ui.dp(52));
            cell.setBackground(Ui.pressable(sel ? Ui.rect(Ui.ACCENT, 12) : Ui.outline(0x00000000, Ui.LINE, 12), 12));
            TextView t = Ui.medium(a, Days.initial(d), 14, sel ? Ui.BG : Ui.MUTED);
            t.setGravity(Gravity.CENTER);
            cell.addView(t);
            View dot = new View(a);
            dot.setBackground(Ui.rect(training ? (sel ? Ui.BG : SESSION_TEXT) : 0x00000000, 3));
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(Ui.dp(6), Ui.dp(6));
            dl.gravity = Gravity.CENTER_HORIZONTAL;
            dl.topMargin = Ui.dp(3);
            cell.addView(dot, dl);
            cell.setContentDescription(MealPlanner.weekdayLong(d) + (training ? ", jour de séance" : ", jour de repos") + (sel ? ", affiché" : ""));
            cell.setOnClickListener(v -> pick.on(d));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) lp.leftMargin = Ui.dp(5);
            row.addView(cell, lp);
        }
        return row;
    }

    static View summary(MainActivity a, com.fonte.carnet.data.Store st, DayPlanner.Day plan, int day) {
        LinearLayout card = Ui.card(a);
        LinearLayout head = Ui.hbox(a);
        int mains = 0;
        boolean snack = false;
        for (DayPlanner.Slot s : plan.slots) {
            if (s.main()) mains++;
            else snack = true;
        }
        TextView label = Ui.medium(a, (MealPlanner.weekdayLong(day) + " · " + (plan.training ? "jour de séance" : "jour de repos"))
                .toUpperCase(Locale.FRENCH), 11, Ui.MUTED);
        label.setLetterSpacing(0.06f);
        head.addView(label, Ui.weight(1));
        head.addView(Ui.text(a, mains + " repas" + (snack ? " + collation" : ""), 12, Ui.FAINT));
        card.addView(head);
        LinearLayout kRow = Ui.hbox(a);
        kRow.setGravity(Gravity.BOTTOM);
        kRow.setPadding(0, Ui.dp(4), 0, 0);
        TextView big = Ui.text(a, Fmt.kg(Math.round(plan.kcal / 10.0) * 10), 30, Ui.TEXT);
        big.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        Ui.tabular(big);
        kRow.addView(big);
        TextView of = Ui.text(a, " / " + Fmt.kg(plan.targetKcal) + " kcal", 15, Ui.MUTED);
        of.setPadding(0, 0, 0, Ui.dp(5));
        kRow.addView(of);
        card.addView(kRow);
        LinearLayout cols = Ui.hbox(a);
        cols.setPadding(0, Ui.dp(8), 0, 0);
        LinearLayout.LayoutParams mid = Ui.weight(1);
        mid.leftMargin = Ui.dp(12);
        mid.rightMargin = Ui.dp(12);
        cols.addView(col(a, "Protéines", plan.protein, plan.targetProtein, Ui.PROTEIN), Ui.weight(1));
        cols.addView(col(a, "Glucides", plan.carbs, plan.targetCarbs, Ui.CARBS), mid);
        cols.addView(col(a, "Lipides", plan.fat, plan.targetFat, Ui.FAT), Ui.weight(1));
        card.addView(cols);
        card.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 10));
        return card;
    }

    private static View col(MainActivity a, String label, double value, double target, int color) {
        LinearLayout box = Ui.vbox(a);
        box.addView(Ui.text(a, label, 12, Ui.MUTED));
        TextView v = Ui.medium(a, Math.round(value) + " / " + Math.round(target) + " g", 14, Ui.TEXT);
        Ui.tabular(v);
        box.addView(v);
        LinearLayout bar = Ui.hbox(a);
        bar.setBackground(Ui.rect(Ui.SURFACE2, 3));
        bar.setWeightSum(1f);
        View fill = new View(a);
        fill.setBackground(Ui.rect(color, 3));
        bar.addView(fill, new LinearLayout.LayoutParams(0, -1, target > 0 ? (float) Math.min(1, value / target) : 0f));
        box.addView(bar, Ui.mlp(-1, Ui.dp(6), 0, 5, 0, 0));
        return box;
    }

    // ---------------------------------------------------------------- lignes du jour

    private TextView timePill(String time, boolean strong, int color) {
        TextView t = Ui.medium(a, time, 12, strong ? Ui.BG : Ui.TEXT);
        t.setBackground(Ui.rect(strong ? color : Ui.SURFACE2, 8));
        t.setPadding(Ui.dp(8), Ui.dp(3), Ui.dp(8), Ui.dp(3));
        t.setSingleLine(true);
        Ui.tabular(t);
        return t;
    }

    private LinearLayout timeRow(String time, String text, String right, boolean strong) {
        LinearLayout row = Ui.hbox(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(2), Ui.dp(4), Ui.dp(2), Ui.dp(10));
        row.addView(timePill(time, strong, Ui.ACCENT));
        TextView t = Ui.text(a, text, 14, Ui.TEXT);
        t.setPadding(Ui.dp(10), 0, Ui.dp(8), 0);
        row.addView(t, Ui.weight(1));
        if (right != null) row.addView(Ui.text(a, right, 12, Ui.MUTED));
        return row;
    }

    private View snackRow(DayPlanner.Slot s) {
        DayPlanner.Line l = s.lines.get(0);
        String d = DayPlanner.detail(l);
        LinearLayout row = timeRow(DayPrefs.clock(s.minutes), "Collation · " + DayPlanner.text(l) + (d == null ? "" : " (" + d + ")"),
                Math.round(s.kcal) + " kcal", false);
        row.setOnClickListener(v -> swap(s, l));
        row.setBackground(Ui.pressable(Ui.rect(0x00000000, 12), 12));
        return row;
    }

    private View sessionCard(DayPlanner.Day plan) {
        Nutrition.Targets t = st.trainingTargets();
        LinearLayout card = Ui.hbox(a);
        card.setGravity(Gravity.TOP);
        card.setBackground(Ui.rect(SESSION_BG, 14));
        card.setPadding(Ui.dp(12), Ui.dp(12), Ui.dp(12), Ui.dp(12));
        card.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 10));
        card.addView(timePill(DayPrefs.clock(plan.sessionTime), true, SESSION_TEXT));
        LinearLayout txt = Ui.vbox(a);
        txt.setPadding(Ui.dp(10), 0, 0, 0);
        txt.addView(Ui.medium(a, "Séance · ≈ " + t.sessionKcal + " kcal nettes", 14, Ui.TEXT));
        txt.addView(Ui.text(a, "Déjà comptées : elles passent en glucides autour de la séance. Pendant : de l'eau, environ 500 ml par heure.",
                13, Ui.MUTED));
        card.addView(txt, Ui.weight(1));
        return card;
    }

    private View mealCard(DayPlanner.Day plan, DayPlanner.Slot s) {
        LinearLayout card = Ui.card(a);
        card.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 10));
        String title = DayPlanner.title(s, plan.training);
        LinearLayout head = Ui.hbox(a);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(timePill(DayPrefs.clock(s.minutes), true, Ui.ACCENT));
        TextView name = Ui.medium(a, title, 16, Ui.TEXT);
        name.setPadding(Ui.dp(8), 0, Ui.dp(8), 0);
        head.addView(name, Ui.weight(1));
        TextView k = Ui.medium(a, Math.round(s.kcal) + " kcal", 14, Ui.TEXT);
        Ui.tabular(k);
        head.addView(k);
        card.addView(head);
        TextView macros = Ui.text(a, "P " + Math.round(s.protein) + " · G " + Math.round(s.carbs) + " · L " + Math.round(s.fat), 12, Ui.MUTED);
        macros.setPadding(0, Ui.dp(6), 0, Ui.dp(4));
        card.addView(macros);
        for (DayPlanner.Line l : s.lines) {
            card.addView(rule(a));
            card.addView(line(s, l));
        }
        for (DayPrefs.Reminder r : st.dayPrefs.reminders) {
            if (r.slot != s.index) continue;
            TextView t = Ui.text(a, "Rappel · " + r.text, 13, Ui.MUTED);
            t.setPadding(0, Ui.dp(8), 0, 0);
            card.addView(t);
        }
        boolean eaten = st.dayMealEaten(day, s.index);
        TextView done = Ui.button(a, eaten ? "✓ Mangé · dans le journal" : "Mangé", eaten ? Ui.SECONDARY : Ui.GHOST, v -> {
            st.setDayMealEaten(day, s, title, !eaten);
            if (!eaten) Ui.toast(a, title + " ajouté au journal");
            a.refresh();
        });
        if (eaten) done.setTextColor(Ui.OK);
        done.setLayoutParams(Ui.mlp(-1, -2, 0, 10, 0, 0));
        card.addView(done);
        return card;
    }

    private View line(DayPlanner.Slot s, DayPlanner.Line l) {
        LinearLayout row = Ui.hbox(a);
        row.setGravity(Gravity.TOP);
        row.setPadding(0, Ui.dp(9), 0, Ui.dp(9));
        row.setBackground(Ui.pressable(Ui.rect(0x00000000, 10), 10));
        TextView role = Ui.medium(a, roleLabel(l.role).toUpperCase(Locale.FRENCH), 11, roleColor(l.role));
        role.setLetterSpacing(0.04f);
        role.setPadding(0, Ui.dp(2), 0, 0);
        row.addView(role, new LinearLayout.LayoutParams(Ui.dp(70), -2));
        LinearLayout txt = Ui.vbox(a);
        String d = DayPlanner.detail(l);
        TextView main = Ui.text(a, DayPlanner.text(l) + (d == null ? "" : " (" + d + ")"), 15, Ui.TEXT);
        txt.addView(main);
        String alt = DayPlanner.altText(l);
        if (!alt.isEmpty()) {
            TextView at = Ui.text(a, alt, 12, Ui.MUTED);
            at.setPadding(0, Ui.dp(2), 0, 0);
            txt.addView(at);
        }
        row.addView(txt, Ui.weight(1));
        TextView k = Ui.text(a, Math.round(l.kcal) + " kcal", 12, Ui.MUTED);
        k.setPadding(Ui.dp(8), Ui.dp(2), 0, 0);
        row.addView(k);
        row.setContentDescription(roleLabel(l.role) + " : " + DayPlanner.text(l) + ". Toucher pour les équivalences.");
        if (!DayPlanner.VEG.equals(l.role)) row.setOnClickListener(v -> swap(s, l));
        return row;
    }

    static View rule(MainActivity a) {
        View v = new View(a);
        v.setBackgroundColor(Ui.LINE);
        v.setLayoutParams(Ui.mlp(-1, 1, 0, 0, 0, 0));
        return v;
    }

    static String roleLabel(String role) {
        switch (role) {
            case DayPlanner.CARBS:
                return "Glucides";
            case DayPlanner.PROTEIN:
                return "Protéines";
            case DayPlanner.FAT:
                return "Lipides";
            case DayPlanner.FRUIT:
                return "Fruit";
            default:
                return "Légumes";
        }
    }

    private static int roleColor(String role) {
        switch (role) {
            case DayPlanner.CARBS:
                return ROLE_CARBS;
            case DayPlanner.PROTEIN:
                return ROLE_PROTEIN;
            case DayPlanner.FAT:
                return ROLE_FAT;
            default:
                return ROLE_PLANT;
        }
    }

    // ---------------------------------------------------------------- équivalences

    /** Nutriment principal d'une famille pour 100 g. */
    private static double main(Food f, String role) {
        if (DayPlanner.PROTEIN.equals(role)) return f.protein;
        if (DayPlanner.FAT.equals(role)) return f.fat;
        return f.carbs;
    }

    private static double amount(DayPlanner.Line l) {
        if (DayPlanner.PROTEIN.equals(l.role)) return l.protein;
        if (DayPlanner.FAT.equals(l.role)) return l.fat;
        return l.carbs;
    }

    private static String nutrientWord(String role) {
        if (DayPlanner.PROTEIN.equals(role)) return "protéines";
        if (DayPlanner.FAT.equals(role)) return "lipides";
        return "glucides";
    }

    /** Feuille des équivalences d'un aliment : même quantité du nutriment principal. */
    private void swap(DayPlanner.Slot s, DayPlanner.Line l) {
        DayPrefs p = st.dayPrefs;
        List<Food> pool = new ArrayList<>(DayPlanner.selected(p, l.role, st::food));
        for (String[] ch : DayPlanner.choices(l.role)) {
            Food f = DayPlanner.resolve(ch[0], st::food);
            if (f != null && !pool.contains(f)) pool.add(f);
        }
        if (!containsId(pool, l.food.id)) pool.add(0, l.food);
        double need = amount(l);
        boolean[] keep = {true};
        String[] chosen = {l.food.id};
        LinearLayout box = Ui.vbox(a);
        TextView title = Ui.text(a, roleLabel(l.role) + (s.main() ? " du repas " + (s.index + 1) : " de la collation"), 19, Ui.TEXT);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        box.addView(title);
        TextView sub = Ui.text(a, "Même quantité de " + nutrientWord(l.role) + " : " + Math.round(need) + " g. Les calories changent un peu ; "
                + "le reste du repas s'ajuste pour garder " + Math.round(s.kcal) + " kcal.", 13, Ui.MUTED);
        sub.setPadding(0, Ui.dp(4), 0, Ui.dp(10));
        box.addView(sub);
        List<TextView> rows = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        Runnable paint = () -> {
            for (int i = 0; i < rows.size(); i++) {
                boolean sel = ids.get(i).equals(chosen[0]);
                rows.get(i).setBackground(Ui.pressable(sel ? Ui.outline(Ui.ACCENT_SOFT, Ui.ACCENT, 12) : Ui.outline(Ui.BG, Ui.SURFACE2, 12), 12));
            }
        };
        for (Food f : pool) {
            double d = main(f, l.role);
            if (d <= 0) continue;
            double g = Math.round(need * 100 / d / 5) * 5;
            if (g <= 0 || g > 1200) continue;
            String raw = f.id == null ? null : com.fonte.carnet.core.Groceries.rawHint(f.id, g);
            String label = Fmt.kg(g) + " g   " + f.name + (raw == null ? "" : " (" + raw + ")") + "   ·   " + Math.round(f.kcal * g / 100) + " kcal";
            TextView row = Ui.text(a, label, 15, Ui.TEXT);
            row.setMinHeight(Ui.dp(48));
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(12), Ui.dp(8), Ui.dp(12), Ui.dp(8));
            row.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 6));
            String id = f.id;
            row.setOnClickListener(v -> {
                chosen[0] = id;
                paint.run();
            });
            rows.add(row);
            ids.add(id);
            box.addView(row);
        }
        paint.run();
        android.app.Dialog[] holder = new android.app.Dialog[1];
        TextView other = Ui.medium(a, "+ Un autre aliment (Ciqual, tes aliments)", 14, Ui.ACCENT);
        other.setMinHeight(Ui.dp(44));
        other.setGravity(Gravity.CENTER_VERTICAL);
        other.setOnClickListener(v -> pickFood(a, l.role, f -> {
            chosen[0] = f.id;
            apply(s, l.role, f.id, keep[0]);
            if (holder[0] != null) holder[0].dismiss();
        }));
        box.addView(other);
        box.addView(Ui.switchRow(a, "Me proposer celui-ci en premier",
                s.main() ? "Au repas " + (s.index + 1) + " de chaque jour, pas seulement aujourd'hui"
                        : "À chaque collation d'avant la séance", true, on -> keep[0] = on));
        TextView cancel = Ui.button(a, "Annuler", Ui.GHOST, null);
        TextView ok = Ui.button(a, "Choisir", Ui.PRIMARY, null);
        LinearLayout buttons = Ui.hbox(a);
        buttons.setPadding(0, Ui.dp(12), 0, 0);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, -2, 1);
        l1.rightMargin = Ui.dp(8);
        buttons.addView(cancel, l1);
        buttons.addView(ok, new LinearLayout.LayoutParams(0, -2, 2));
        box.addView(buttons);
        android.app.Dialog d = Ui.sheet(a, box);
        holder[0] = d;
        cancel.setOnClickListener(v -> d.dismiss());
        ok.setOnClickListener(v -> {
            d.dismiss();
            apply(s, l.role, chosen[0], keep[0]);
        });
        d.show();
    }

    private static boolean containsId(List<Food> pool, String id) {
        for (Food f : pool) if (f.id != null && f.id.equals(id)) return true;
        return false;
    }

    /** Retient l'aliment choisi : pour toujours à ce repas, ou pour ce jour seulement. */
    private void apply(DayPlanner.Slot s, String role, String id, boolean always) {
        if (id == null) return;
        DayPrefs p = st.dayPrefs;
        String key = DayPrefs.firstKey(s.index, role);
        if (always) {
            p.firsts.put(key, id);
            p.dayFirsts.remove(day + ":" + key);
        } else {
            p.dayFirsts.put(day + ":" + key, id);
        }
        st.save();
        a.refresh();
    }

    interface FoodPick {
        void on(Food f);
    }

    /** Recherche d'un aliment hors de la liste (base de l'appli, Ciqual, tes aliments) qui apporte le nutriment de la famille. */
    static void pickFood(MainActivity a, String role, FoodPick pick) {
        com.fonte.carnet.data.Store st = a.store;
        LinearLayout box = Ui.vbox(a);
        box.setPadding(Ui.dp(20), Ui.dp(4), Ui.dp(20), Ui.dp(4));
        EditText search = Ui.field(a, "Rechercher un aliment…", Ui.TEXT_FIELD);
        box.addView(search);
        LinearLayout list = Ui.vbox(a);
        list.setPadding(0, Ui.dp(8), 0, 0);
        box.addView(list);
        android.widget.ScrollView sv = new android.widget.ScrollView(a);
        sv.addView(box);
        AlertDialog d = new AlertDialog.Builder(a).setTitle(roleLabel(role) + " : un autre aliment").setView(sv)
                .setNegativeButton("Annuler", null).create();
        Runnable fill = () -> {
            list.removeAllViews();
            String q = search.getText().toString().trim();
            if (q.isEmpty()) {
                list.addView(Ui.text(a, "Tape le nom d'un aliment : manioc, fruit à pain, songe…", 13, Ui.MUTED));
                return;
            }
            FoodSearch.Input in = new FoodSearch.Input();
            in.mine = st.customFoods;
            in.fonte = Foods.all();
            in.ciqual = Ciqual.all();
            int n = 0;
            for (FoodSearch.Result r : FoodSearch.search(q, in)) {
                if (r.food == null || main(r.food, role) <= 0) continue;
                if (n++ >= 25) break;
                Food f = r.food;
                LinearLayout row = Ui.vbox(a);
                row.setPadding(Ui.dp(4), Ui.dp(10), Ui.dp(4), Ui.dp(10));
                row.setBackground(Ui.pressable(Ui.rect(0x00000000, 10), 10));
                row.addView(Ui.text(a, f.name, 15, Ui.TEXT));
                row.addView(Ui.text(a, FoodAddScreen.macros(f), 12, Ui.MUTED));
                row.setOnClickListener(v -> {
                    Ui.hideKeyboard(search);
                    d.dismiss();
                    pick.on(f);
                });
                list.addView(row);
            }
            if (n == 0) list.addView(Ui.text(a, "Aucun aliment trouvé.", 13, Ui.MUTED));
        };
        Ui.onText(search, t -> fill.run());
        fill.run();
        d.show();
    }
}

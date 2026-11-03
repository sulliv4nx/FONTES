package com.fonte.carnet.ui;

import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Nutrition;
import com.fonte.carnet.core.Profile;

/** Profil et besoins : métabolisme, objectif calorique, macros et eau. */
public class ProfileScreen extends Screen {

    @Override
    public String title() {
        return "Profil et besoins";
    }

    @Override
    public View build() {
        Profile p = st.profile;
        LinearLayout c = Ui.vbox(a);

        c.addView(Ui.section(a, "Toi"));
        View[] sex = {
                Ui.chip(a, "Homme", Profile.MALE.equals(p.sex), v -> set(() -> p.sex = Profile.MALE)),
                Ui.chip(a, "Femme", Profile.FEMALE.equals(p.sex), v -> set(() -> p.sex = Profile.FEMALE))};
        c.addView(Ui.chipRow(a, sex));
        LinearLayout row = Ui.hbox(a);
        row.setGravity(android.view.Gravity.TOP);
        EditText year = Ui.field(a, "ex. 1992", Ui.INTEGER);
        if (p.birthYear > 0) year.setText(String.valueOf(p.birthYear));
        Ui.onText(year, t -> {
            int y = Fmt.parseInt(t);
            p.birthYear = y > 1900 && y < 2030 ? y : 0;
            st.saveSoon();
        });
        EditText height = Ui.field(a, "ex. 178", Ui.DECIMAL);
        if (p.heightCm > 0) height.setText(Fmt.kg(p.heightCm));
        Ui.onText(height, t -> {
            double h = Fmt.parse(t);
            p.heightCm = !Double.isNaN(h) && h >= 100 && h <= 250 ? h : 0;
            st.saveSoon();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.rightMargin = Ui.dp(10);
        row.addView(Ui.labelled(a, "Année de naissance", year), lp);
        row.addView(Ui.labelled(a, "Taille (cm)", height), new LinearLayout.LayoutParams(0, -2, 1));
        c.addView(row);

        double w = st.weight();
        LinearLayout weight = Ui.clickCard(a, v -> BodyScreen.weightDialog(a, a::refresh));
        LinearLayout wl = Ui.hbox(a);
        wl.addView(Ui.text(a, "Poids", 15, Ui.MUTED), Ui.weight(1));
        wl.addView(Ui.medium(a, w > 0 ? Fmt.kg(w) + " kg" : "Ajouter une pesée", 15, w > 0 ? Ui.TEXT : Ui.ACCENT));
        weight.addView(wl);
        c.addView(weight);

        c.addView(Ui.section(a, "Activité hors entraînement"));
        c.addView(Ui.hint(a, "Ta journée sans le sport : tes séances, WOD et activités sont ajoutés chaque jour à ton objectif."));
        for (int i = 0; i < Profile.ACTIVITY.length; i++) {
            double level = Profile.ACTIVITY[i];
            boolean sel = Math.abs(p.activity - level) < 1e-6;
            LinearLayout opt = Ui.clickCard(a, v -> set(() -> p.activity = level));
            if (sel) opt.setBackground(Ui.pressable(Ui.outline(Ui.ACCENT_SOFT, Ui.ACCENT, 18), 18));
            opt.addView(Ui.medium(a, Profile.ACTIVITY_LABELS[i], 15, sel ? Ui.TEXT : Ui.MUTED));
            c.addView(opt);
        }

        c.addView(Ui.section(a, "Objectif"));
        View[] goals = {
                Ui.chip(a, "Perte de poids", Profile.LOSE.equals(p.goal), v -> set(() -> p.goal = Profile.LOSE)),
                Ui.chip(a, "Maintien", Profile.MAINTAIN.equals(p.goal), v -> set(() -> p.goal = Profile.MAINTAIN)),
                Ui.chip(a, "Prise de masse", Profile.GAIN.equals(p.goal), v -> set(() -> p.goal = Profile.GAIN))};
        c.addView(Ui.chipRow(a, goals));

        // Résultats
        int age = p.age(System.currentTimeMillis());
        double wc = st.weightForCalc();
        int target = st.targetKcal();
        Nutrition.Macros m = st.targetMacros();
        LinearLayout res = Ui.card(a);
        res.addView(Ui.cardHeader(a, Icon.SPARK, Ui.ACCENT, "Tes besoins", null, null));
        if (!p.complete() || w <= 0) {
            res.addView(Ui.hint(a, "Complète l'année de naissance, la taille et une pesée pour un calcul personnalisé."));
        }
        res.addView(Ui.row(a, "Métabolisme de base", Fmt.kg(Math.round(Nutrition.bmr(p, wc, age))) + " kcal"));
        res.addView(Ui.row(a, "Dépense hors sport", Fmt.kg(Nutrition.maintenance(p, wc, age)) + " kcal"));
        LinearLayout tr = Ui.row(a, "Objectif quotidien (" + p.goalLabel().toLowerCase(java.util.Locale.FRENCH) + ")", Fmt.kg(target) + " kcal");
        ((TextView) tr.getChildAt(1)).setTextColor(Ui.ACCENT);
        res.addView(tr);
        res.addView(Ui.divider(a));
        res.addView(Ui.row(a, "Protéines", m.protein + " g · " + Fmt.kg(p.proteinPerKg) + " g/kg"));
        res.addView(Ui.row(a, "Lipides", m.fat + " g · " + Fmt.kg(p.fatPerKg) + " g/kg"));
        res.addView(Ui.row(a, "Glucides", m.carbs + " g (le reste)"));
        res.addView(Ui.row(a, "Eau", Fmt.kg(st.waterTarget(com.fonte.carnet.core.Days.today()) / 1000.0) + " L par jour"));
        TextView how = Ui.text(a, "Formule de Mifflin-St Jeor. Perte : −20 %, prise de masse : +10 %. "
                + "Ce sont des estimations : ajuste selon l'évolution de ton poids sur 2 à 3 semaines.", 12, Ui.MUTED);
        how.setPadding(0, Ui.dp(8), 0, 0);
        res.addView(how);
        c.addView(res);

        c.addView(Ui.section(a, "Réglages fins"));
        c.addView(Ui.text(a, "Protéines par kg de poids", 13, Ui.MUTED));
        double[] prot = {1.6, 1.8, 2.0, 2.2, 2.5};
        View[] pc = new View[prot.length];
        for (int i = 0; i < prot.length; i++) {
            double v = prot[i];
            pc[i] = Ui.chip(a, Fmt.kg(v) + " g", Math.abs(p.proteinPerKg - v) < 1e-6, x -> set(() -> p.proteinPerKg = v));
        }
        c.addView(Ui.chipRow(a, pc));
        c.addView(Ui.text(a, "Lipides par kg de poids", 13, Ui.MUTED));
        double[] fat = {0.7, 0.8, 0.9, 1.0, 1.2};
        View[] fc = new View[fat.length];
        for (int i = 0; i < fat.length; i++) {
            double v = fat[i];
            fc[i] = Ui.chip(a, Fmt.kg(v) + " g", Math.abs(p.fatPerKg - v) < 1e-6, x -> set(() -> p.fatPerKg = v));
        }
        c.addView(Ui.chipRow(a, fc));
        LinearLayout ov = Ui.hbox(a);
        ov.setGravity(android.view.Gravity.TOP);
        EditText kcal = Ui.field(a, "auto", Ui.INTEGER);
        if (p.kcalOverride > 0) kcal.setText(String.valueOf(p.kcalOverride));
        Ui.onText(kcal, t -> {
            int k = Fmt.parseInt(t);
            p.kcalOverride = k >= 1000 && k <= 6000 ? k : 0;
            st.saveSoon();
        });
        EditText water = Ui.field(a, "auto", Ui.INTEGER);
        if (p.waterOverride > 0) water.setText(String.valueOf(p.waterOverride));
        Ui.onText(water, t -> {
            int ml = Fmt.parseInt(t);
            p.waterOverride = ml >= 500 && ml <= 8000 ? ml : 0;
            st.saveSoon();
        });
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, -2, 1);
        lp2.rightMargin = Ui.dp(10);
        ov.addView(Ui.labelled(a, "Objectif imposé (kcal)", kcal), lp2);
        ov.addView(Ui.labelled(a, "Eau imposée (ml)", water), new LinearLayout.LayoutParams(0, -2, 1));
        c.addView(ov);
        c.addView(Ui.button(a, "Recalculer", Ui.SECONDARY, v -> {
            Ui.hideKeyboard(v);
            st.save();
            a.refresh();
        }));
        return Ui.page(a, c);
    }

    private void set(Runnable change) {
        change.run();
        st.save();
        a.refresh();
    }
}

package com.fonte.carnet.ui;

import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.BodyLog;
import com.fonte.carnet.core.Days;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.MealPrefs;
import com.fonte.carnet.core.Nutrition;
import com.fonte.carnet.core.Profile;

import java.util.ArrayList;
import java.util.List;

/**
 * Profil et besoins, méthode 4.3 : Ten Haaf × activité du quotidien, séances en calories nettes, objectif en
 * rythme de poids, protéines en g/kg, lipides en %, glucides en reste, réglage par les pesées.
 */
public class ProfileScreen extends Screen {

    @Override
    public String title() {
        return "Profil et besoins";
    }

    @Override
    public View build() {
        Profile p = st.profile;
        LinearLayout c = Ui.vbox(a);

        // ------------------------------------------------------------ toi
        c.addView(Ui.section(a, "Toi"));
        View[] sex = {
                Ui.chip(a, "Homme", Profile.MALE.equals(p.sex), v -> set(() -> p.sex = Profile.MALE)),
                Ui.chip(a, "Femme", Profile.FEMALE.equals(p.sex), v -> set(() -> p.sex = Profile.FEMALE))};
        c.addView(Ui.chipRow(a, sex));
        LinearLayout row = Ui.hbox(a);
        row.setGravity(Gravity.TOP);
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

        double fat = st.bodyFat();
        boolean measured = st.measure(BodyLog.BODY_FAT, 0) > 0;
        LinearLayout fatCard = Ui.clickCard(a, v -> a.show(new MeasureEditScreen()));
        LinearLayout fl = Ui.hbox(a);
        fl.addView(Ui.text(a, "Masse grasse", 15, Ui.MUTED), Ui.weight(1));
        fl.addView(Ui.medium(a, fat > 0 ? Fmt.kg(fat) + " %" + (measured ? "" : " (estimée)") : "Ajouter", 15,
                fat > 0 ? Ui.TEXT : Ui.ACCENT));
        fatCard.addView(fl);
        if (fat > 0 && !measured) {
            TextView t = Ui.text(a, "Estimée d'après ton tour de cou et de taille (méthode US Navy).", 12, Ui.FAINT);
            t.setPadding(0, Ui.dp(4), 0, 0);
            fatCard.addView(t);
        }
        c.addView(fatCard);

        // ------------------------------------------------------------ métabolisme de base
        c.addView(Ui.section(a, "Métabolisme de base"));
        List<View> bmr = new ArrayList<>();
        for (int i = 0; i < Profile.BMR_IDS.length; i++) {
            String id = Profile.BMR_IDS[i];
            String label = Profile.BMR_LABELS[i] + (Profile.BMR_TENHAAF.equals(id) ? " · conseillée" : "");
            TextView chip = Ui.chip(a, label, id.equals(p.bmrFormula), v -> set(() -> {
                p.bmrFormula = id;
                p.adjustDay = Days.today();
            }));
            chip.setLayoutParams(new android.view.ViewGroup.LayoutParams(-2, -2));
            bmr.add(chip);
        }
        c.addView(Ui.flow(a, bmr.toArray(new View[0])));
        c.addView(note(bmrNote(p, fat)));

        // ------------------------------------------------------------ activité
        c.addView(Ui.section(a, "Activité au quotidien, hors sport"));
        for (int i = 0; i < Profile.DAILY.length; i++) {
            double level = Profile.DAILY[i];
            boolean sel = Math.abs(p.dailyFactor() - level) < 1e-6;
            LinearLayout opt = Ui.clickCard(a, v -> set(() -> {
                p.daily = level;
                p.adjustDay = Days.today();
            }));
            if (sel) opt.setBackground(Ui.pressable(Ui.outline(Ui.ACCENT_SOFT, Ui.ACCENT, 18), 18));
            LinearLayout r = Ui.hbox(a);
            r.addView(Ui.medium(a, Profile.DAILY_LABELS[i], 15, sel ? Ui.TEXT : Ui.MUTED), Ui.weight(1));
            TextView f = Ui.medium(a, "× " + Fmt.kg(level), 14, sel ? Ui.ACCENT : Ui.FAINT);
            Ui.tabular(f);
            f.setPadding(Ui.dp(8), 0, 0, 0);
            r.addView(f);
            opt.addView(r);
            c.addView(opt);
        }
        int[] typical = st.typicalSession();
        c.addView(note("Ne compte pas le sport ici : chaque séance notée dans Fonte s'ajoute le jour même, en calories nettes "
                + "(le repos est déjà dans le métabolisme). Les jours d'entraînement prévus, une séance type est comptée "
                + "tant que rien n'est noté : ≈ " + typical[0] + " kcal pour " + typical[1] + " min."));
        c.addView(trainingDays());

        // ------------------------------------------------------------ objectif
        c.addView(Ui.section(a, "Objectif"));
        View[] goals = {
                Ui.chip(a, "Perte de poids", Profile.LOSE.equals(p.goal), v -> set(() -> setGoal(p, Profile.LOSE))),
                Ui.chip(a, "Maintien", Profile.MAINTAIN.equals(p.goal), v -> set(() -> setGoal(p, Profile.MAINTAIN))),
                Ui.chip(a, "Prise de masse", Profile.GAIN.equals(p.goal), v -> set(() -> setGoal(p, Profile.GAIN)))};
        c.addView(Ui.chipRow(a, goals));
        double wc = st.weightForCalc();
        if (!Profile.MAINTAIN.equals(p.goal)) {
            boolean lose = Profile.LOSE.equals(p.goal);
            double[] rates = lose ? Profile.LOSS_RATES : Profile.GAIN_RATES;
            double current = lose ? p.lossRate : p.gainRate;
            List<View> rc = new ArrayList<>();
            for (double r : rates) {
                TextView chip = Ui.chip(a, (lose ? "−" : "+") + Fmt.kg(r) + " %", Math.abs(current - r) < 1e-6, v -> set(() -> {
                    if (lose) p.lossRate = r;
                    else p.gainRate = r;
                    p.adjustDay = Days.today();
                }));
                rc.add(chip);
            }
            c.addView(Ui.chipRow(a, rc.toArray(new View[0])));
            double kgWeek = wc * p.ratePct() / 100.0;
            int kcalDay = (int) Math.round(kgWeek * Nutrition.KCAL_PER_KG / 7.0);
            c.addView(note("Du poids par semaine, soit ≈ " + signed(Math.round(kgWeek * 100) / 100.0) + " kg et "
                    + signed(kcalDay) + " kcal par jour. "
                    + (lose ? "Repère : 0,5 à 1 % par semaine pour garder muscle et force (Helms 2014)."
                    : "Repère : 0,25 à 0,5 % par semaine ; plutôt 0,25 % si tu t'entraînes depuis longtemps (Iraki 2019).")));
        }

        // ------------------------------------------------------------ macros
        c.addView(Ui.section(a, "Macros"));
        LinearLayout seg = Ui.segmented(a, new String[]{"En grammes · conseillé", "En pourcentages"}, p.percentMode() ? 1 : 0,
                i -> set(() -> p.method = i == 1 ? Profile.METHOD_PERCENT : Profile.METHOD_GRAMS));
        c.addView(seg);
        if (p.percentMode()) percentSettings(c, p);
        else gramSettings(c, p);

        // ------------------------------------------------------------ résultats
        c.addView(results(p, wc, w));
        c.addView(trendCard(p));

        // ------------------------------------------------------------ réglages fins
        c.addView(Ui.section(a, "Réglages fins"));
        LinearLayout ov = Ui.hbox(a);
        ov.setGravity(Gravity.TOP);
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
        c.addView(note("Un objectif imposé vaut pour tous les jours : les séances ne s'y ajoutent plus."));
        c.addView(Ui.button(a, "Recalculer", Ui.SECONDARY, v -> {
            Ui.hideKeyboard(v);
            st.save();
            a.refresh();
        }));
        return Ui.page(a, c);
    }

    // ---------------------------------------------------------------- blocs

    private static String bmrNote(Profile p, double fat) {
        String f = p.bmrFormula;
        if (Profile.needsLeanMass(f)) {
            if (fat <= 0) return "Cette équation part de ta masse maigre : ajoute ta masse grasse. En attendant, Ten Haaf est utilisée.";
            return "Calculée sur ta masse maigre. Fiable si ta masse grasse est bien mesurée (impédancemètre, pince, DEXA).";
        }
        if (Profile.BMR_MIFFLIN.equals(f)) {
            return "La référence pour le grand public. Chez les sportifs, elle sous-estime souvent les besoins.";
        }
        return "La plus précise chez les sportifs : 80 % à moins de 10 % de la valeur mesurée (méta-analyse Sports Medicine 2023).";
    }

    private static void setGoal(Profile p, String goal) {
        if (!goal.equals(p.goal)) p.adjustDay = Days.today();
        p.goal = goal;
    }

    /** Les 7 jours de la semaine, partagés avec « Mes repas ». */
    private View trainingDays() {
        MealPrefs prefs = st.mealPrefs();
        LinearLayout box = Ui.vbox(a);
        TextView label = Ui.text(a, "Jours d'entraînement prévus", 13, Ui.MUTED);
        label.setPadding(0, Ui.dp(4), 0, Ui.dp(6));
        box.addView(label);
        LinearLayout days = Ui.hbox(a);
        String[] l = {"L", "M", "M", "J", "V", "S", "D"};
        String[] names = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"};
        int mask = Math.max(0, prefs.trainingMask);
        for (int i = 0; i < 7; i++) {
            final int bit = 1 << i;
            boolean on = (mask & bit) != 0;
            TextView d = Ui.medium(a, l[i], 14, on ? Ui.BG : Ui.MUTED);
            d.setGravity(Gravity.CENTER);
            d.setMinHeight(Ui.dp(40));
            d.setBackground(Ui.pressable(on ? Ui.rect(Ui.ACCENT, 10) : Ui.outline(0x00000000, Ui.LINE, 10), 10));
            d.setContentDescription(names[i] + (on ? ", jour d'entraînement" : ""));
            d.setOnClickListener(v -> set(() -> prefs.trainingMask = Math.max(0, prefs.trainingMask) ^ bit));
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) dl.leftMargin = Ui.dp(5);
            days.addView(d, dl);
        }
        box.addView(days);
        return box;
    }

    private void gramSettings(LinearLayout c, Profile p) {
        TextView pl = Ui.medium(a, "Protéines", 14, Ui.TEXT);
        pl.setPadding(0, Ui.dp(14), 0, Ui.dp(4));
        c.addView(pl);
        if (Profile.LOSE.equals(p.goal)) {
            c.addView(note("En sèche : " + Fmt.kg(Profile.PROTEIN_LOSS_LEAN) + " g par kg de masse maigre (repère 2,3 à 3,1), "
                    + "ou " + Fmt.kg(Profile.PROTEIN_LOSS_WEIGHT) + " g par kg de poids si ta masse grasse est inconnue."));
        } else {
            List<View> pc = new ArrayList<>();
            for (double v : Profile.PROTEIN_CHOICES) {
                pc.add(Ui.chip(a, Fmt.kg(v) + " g/kg", Math.abs(p.proteinPerKg - v) < 1e-6, x -> set(() -> p.proteinPerKg = v)));
            }
            c.addView(Ui.chipRow(a, pc.toArray(new View[0])));
            c.addView(note("Le gain de muscle plafonne vers 1,6 g/kg ; 2,2 g/kg pour ne rien laisser au hasard (Morton 2018)."));
        }
        TextView fl = Ui.medium(a, "Lipides", 14, Ui.TEXT);
        fl.setPadding(0, Ui.dp(14), 0, Ui.dp(4));
        c.addView(fl);
        List<View> fc = new ArrayList<>();
        for (int v : Profile.FAT_CHOICES) {
            fc.add(Ui.chip(a, v + " %", p.fatPct == v, x -> set(() -> p.fatPct = v)));
        }
        c.addView(Ui.chipRow(a, fc.toArray(new View[0])));
        c.addView(note("Des calories d'un jour sans séance, jamais sous 0,5 g/kg (ACSM : 20 à 35 %). "
                + "Les glucides prennent le reste, plus les calories de la séance les jours d'entraînement."));
    }

    private void percentSettings(LinearLayout c, Profile p) {
        c.addView(note("Répartitions de l'article « Calculer ses macros ». Elles suivent les calories du jour, séances comprises."));
        List<View> sc = new ArrayList<>();
        sc.add(Ui.chip(a, "Selon mon objectif", Profile.SPLIT_AUTO.equals(p.split), v -> set(() -> p.split = Profile.SPLIT_AUTO)));
        for (int i = 0; i < Profile.SPLIT_IDS.length; i++) {
            String id = Profile.SPLIT_IDS[i];
            sc.add(Ui.chip(a, Profile.SPLIT_LABELS[i], id.equals(p.split), v -> set(() -> p.split = id)));
        }
        sc.add(Ui.chip(a, "Personnalisée", Profile.SPLIT_CUSTOM.equals(p.split), v -> set(() -> p.split = Profile.SPLIT_CUSTOM)));
        for (View v : sc) v.setLayoutParams(new android.view.ViewGroup.LayoutParams(-2, -2));
        c.addView(Ui.flow(a, sc.toArray(new View[0])));
        int[] pct = p.percents();
        TextView splitInfo = Ui.text(a, (Profile.SPLIT_AUTO.equals(p.split) ? p.splitLabel() + " · " : "")
                + "protéines " + pct[0] + " % · glucides " + pct[1] + " % · lipides " + pct[2] + " % des calories", 13, Ui.MUTED);
        splitInfo.setPadding(0, Ui.dp(10), 0, Ui.dp(4));
        c.addView(splitInfo);
        if (Profile.SPLIT_CUSTOM.equals(p.split)) c.addView(customSplit(p));
    }

    private View results(Profile p, double wc, double w) {
        Nutrition.Targets rest = st.restTargets();
        Nutrition.Targets train = st.trainingTargets();
        LinearLayout res = Ui.card(a);
        res.setLayoutParams(Ui.mlp(-1, -2, 0, 12, 0, 12));
        res.addView(Ui.cardHeader(a, Icon.SPARK, Ui.ACCENT, "Tes besoins", null, null));
        if (!p.complete() || w <= 0) {
            res.addView(Ui.hint(a, "Complète l'année de naissance, la taille et une pesée pour un calcul personnalisé."));
        }
        res.addView(Ui.row(a, "Métabolisme de base · " + Profile.bmrLabel(rest.bmrFormula), Fmt.kg(rest.bmr) + " kcal"));
        res.addView(Ui.row(a, "Dépense hors sport (× " + Fmt.kg(rest.daily) + ")", Fmt.kg(rest.expenditure) + " kcal"));
        if (rest.goalKcal != 0 && !rest.imposed) {
            res.addView(Ui.row(a, "Objectif (" + signed(p.ratePct()) + " % par semaine)", signed(rest.goalKcal) + " kcal"));
        }
        if (rest.correction != 0 && !rest.imposed) {
            res.addView(Ui.row(a, "Correction d'après tes pesées", signed(rest.correction) + " kcal"));
        }
        LinearLayout base = Ui.row(a, rest.imposed ? "Objectif imposé, tous les jours" : "Jour sans séance", Fmt.kg(rest.kcal) + " kcal");
        ((TextView) base.getChildAt(1)).setTextColor(Ui.ACCENT);
        res.addView(base);
        if (!rest.imposed) {
            LinearLayout tr = Ui.row(a, "Jour de séance (+" + train.sessionKcal + " kcal nettes, " + train.sessionMinutes + " min)",
                    Fmt.kg(train.kcal) + " kcal");
            ((TextView) tr.getChildAt(1)).setTextColor(Ui.ACCENT);
            res.addView(tr);
        }
        res.addView(Ui.divider(a));
        boolean same = rest.imposed || (rest.protein == train.protein && rest.fat == train.fat);
        String prot = rest.protein + " g · " + Fmt.kg(round1(rest.protein / wc)) + " g/kg";
        if (rest.proteinLean) prot = rest.protein + " g · " + Fmt.kg(rest.proteinFactor) + " g/kg de masse maigre";
        if (!same) prot = rest.protein + " / " + train.protein + " g";
        res.addView(Ui.row(a, "Protéines", prot));
        String fatTxt = rest.fat + " g · " + Fmt.kg(round1(rest.fat / wc)) + " g/kg";
        if (!same) fatTxt = rest.fat + " / " + train.fat + " g";
        res.addView(Ui.row(a, "Lipides", fatTxt));
        String carbs = rest.imposed ? rest.carbs + " g · " + Fmt.kg(round1(rest.carbs / wc)) + " g/kg"
                : rest.carbs + " / " + train.carbs + " g · " + Fmt.kg(round1(rest.carbs / wc)) + " à " + Fmt.kg(round1(train.carbs / wc)) + " g/kg";
        res.addView(Ui.row(a, rest.imposed ? "Glucides" : "Glucides (sans / avec séance)", carbs));
        double fatShare = rest.kcal > 0 ? rest.fat * 9.0 / rest.kcal : 0;
        if (fatShare < 0.195) res.addView(warn("Moins de 20 % de lipides : sous le minimum conseillé aux sportifs (ACSM : 20 à 35 %)."));
        if (rest.fat < Math.round(Profile.FAT_FLOOR * wc)) res.addView(warn("Moins de 0,5 g de lipides par kg : à éviter sur la durée."));
        if (wc > 0 && train.protein / wc > 3.1) res.addView(warn("Plus de 3,1 g de protéines par kg : au-delà de ce qui est recommandé, même en sèche (ISSN)."));
        if (wc > 0 && rest.carbs / wc < 3 && !Profile.LOSE.equals(p.goal)) {
            res.addView(warn("Moins de 3 g de glucides par kg les jours sans séance : un peu juste pour la force et le CrossFit (ACSM : 3 à 7 g/kg)."));
        }
        if (rest.floored) res.addView(warn("Objectif remonté à ton métabolisme de base : un déficit plus grand coûterait du muscle."));
        if (rest.leanFallback) res.addView(warn("Masse grasse inconnue : Ten Haaf (poids) est utilisée à la place."));
        res.addView(Ui.row(a, "Eau", Fmt.kg(rest.water / 1000.0) + " L · " + Fmt.kg(train.water / 1000.0) + " L avec la séance"));
        TextView how = Ui.text(a, "Métabolisme × activité du quotidien, ajusté à ton objectif, puis + les calories nettes des séances "
                + "(MET − 1). Protéines et lipides restent les mêmes chaque jour ; la séance s'ajoute en glucides. "
                + "Eau : 35 ml/kg, +500 ml par heure de séance. Ce sont des estimations à ±10 % : tes pesées font le réglage final.",
                12, Ui.MUTED);
        how.setPadding(0, Ui.dp(8), 0, 0);
        res.addView(how);
        return res;
    }

    /** Réglage d'après la pente des pesées. */
    private View trendCard(Profile p) {
        Nutrition.Trend t = st.weightTrend();
        LinearLayout card = Ui.card(a);
        card.addView(Ui.cardHeader(a, Icon.SPARK, Ui.ACCENT, "Réglage par la balance", null, null));
        if (!t.ready()) {
            card.addView(Ui.text(a, "Pèse-toi au moins 4 fois sur 2 semaines (le matin, à jeun) : Fonte comparera ta courbe au rythme visé "
                    + "et te proposera de corriger ton objectif de 100 à 200 kcal. Pesées prises en compte : " + t.count
                    + (t.count > 1 ? " sur " + t.spanDays + " jours." : "."), 13, Ui.MUTED));
        } else {
            card.addView(Ui.text(a, "Sur " + t.count + " pesées (" + t.spanDays + " jours), ton poids évolue de "
                    + signed(round2(t.kgPerWeek)) + " kg par semaine, pour " + signed(round2(t.expected)) + " kg visés.", 13, Ui.MUTED));
            if (t.suggestion == 0) {
                TextView ok = Ui.text(a, "Tu es dans le rythme : rien à changer.", 13, Ui.OK);
                ok.setPadding(0, Ui.dp(6), 0, 0);
                card.addView(ok);
            } else {
                TextView s = Ui.medium(a, "Je te propose " + signed(t.suggestion) + " kcal par jour.", 15, Ui.TEXT);
                s.setPadding(0, Ui.dp(8), 0, 0);
                card.addView(s);
                int delta = t.suggestion;
                card.addView(Ui.buttonRow(a, Ui.smallButton(a, "Appliquer " + signed(delta) + " kcal", Ui.PRIMARY, v -> {
                    st.applyCorrection(delta);
                    Ui.toast(a, "Objectif corrigé de " + signed(delta) + " kcal par jour.");
                    a.refresh();
                })));
            }
        }
        if (p.kcalAdjust != 0) {
            card.addView(Ui.divider(a));
            LinearLayout r = Ui.row(a, "Correction en cours", signed(p.kcalAdjust) + " kcal");
            card.addView(r);
            card.addView(Ui.buttonRow(a, Ui.smallButton(a, "Remettre à zéro", Ui.SECONDARY, v -> {
                st.resetCorrection();
                a.refresh();
            })));
        }
        return card;
    }

    // ---------------------------------------------------------------- outils

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static String signed(double v) {
        return (v > 0 ? "+" : v < 0 ? "−" : "") + Fmt.kg(Math.abs(v));
    }

    private static String signed(int v) {
        return (v > 0 ? "+" : v < 0 ? "−" : "") + Fmt.kg(Math.abs(v));
    }

    private TextView note(String s) {
        TextView t = Ui.text(a, s, 12, Ui.MUTED);
        t.setPadding(0, Ui.dp(6), 0, Ui.dp(4));
        return t;
    }

    private TextView warn(String s) {
        TextView t = Ui.text(a, s, 12, Ui.WARN);
        t.setPadding(0, Ui.dp(2), 0, Ui.dp(6));
        return t;
    }

    /** Trois champs en % (protéines, glucides, lipides) ; le total doit faire 100. */
    private View customSplit(Profile p) {
        LinearLayout box = Ui.vbox(a);
        LinearLayout row = Ui.hbox(a);
        row.setGravity(Gravity.TOP);
        TextView total = Ui.text(a, "", 12, Ui.MUTED);
        Runnable check = () -> {
            int sum = p.pctProtein + p.pctCarbs + p.pctFat;
            total.setText(sum == 100 ? "Total : 100 %" : "Total : " + sum + " % (doit faire 100 %, l'appli ajuste en attendant)");
            total.setTextColor(sum == 100 ? Ui.MUTED : Ui.WARN);
        };
        String[] labels = {"Protéines %", "Glucides %", "Lipides %"};
        int[] vals = {p.pctProtein, p.pctCarbs, p.pctFat};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            EditText e = Ui.field(a, "%", Ui.INTEGER);
            e.setText(String.valueOf(vals[i]));
            Ui.onText(e, t -> {
                int v = Math.max(0, Math.min(100, Fmt.parseInt(t)));
                if (idx == 0) p.pctProtein = v;
                else if (idx == 1) p.pctCarbs = v;
                else p.pctFat = v;
                check.run();
                st.saveSoon();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            if (i < 2) lp.rightMargin = Ui.dp(8);
            row.addView(Ui.labelled(a, labels[i], e), lp);
        }
        box.addView(row);
        check.run();
        box.addView(total);
        return box;
    }

    private void set(Runnable change) {
        change.run();
        st.save();
        a.refresh();
    }
}

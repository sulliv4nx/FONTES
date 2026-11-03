package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.List;

/** Besoins (Mifflin-St Jeor), macros, eau et dépense énergétique (MET). */
public final class Nutrition {
    private Nutrition() {
    }

    /** Poids utilisé tant qu'aucune pesée n'est enregistrée. */
    public static final double DEFAULT_WEIGHT = 75;

    public static final double MET_MUSCU = 5.0;
    public static final double MET_WOD = 8.0;
    public static final double MET_RENFO = 3.8;

    /** Activités hors séances : {nom, MET}. Valeurs moyennes du Compendium des activités physiques. */
    public static final String[] ACTIVITIES = {
            "Course à pied (≈ 10 km/h)", "Course lente (≈ 8 km/h)", "Marche rapide", "Marche", "Vélo (≈ 20 km/h)",
            "Vélo d'appartement", "Natation", "Rameur", "Corde à sauter", "Randonnée", "Football", "Basket",
            "Tennis", "Boxe / sports de combat", "Yoga / étirements", "Danse", "Surf", "Ménage / jardinage"};
    public static final double[] METS = {
            9.8, 8.3, 4.3, 3.0, 8.0,
            6.8, 7.0, 7.0, 11.0, 6.0, 7.0, 6.5,
            7.3, 9.0, 2.5, 5.0, 3.0, 3.5};

    public static class Macros {
        public int kcal;
        public int protein;
        public int carbs;
        public int fat;
    }

    public static class Totals {
        public double kcal;
        public double protein;
        public double carbs;
        public double fat;

        public void add(FoodEntry e) {
            kcal += e.kcal;
            protein += e.protein;
            carbs += e.carbs;
            fat += e.fat;
        }
    }

    public static class Burn {
        public String label;
        public int minutes;
        public int kcal;

        Burn(String label, int minutes, int kcal) {
            this.label = label;
            this.minutes = minutes;
            this.kcal = kcal;
        }
    }

    /** Métabolisme de base (kcal/jour), formule de Mifflin-St Jeor. */
    public static double bmr(Profile p, double weight, int age) {
        double base = 10 * weight + 6.25 * p.heightCm - 5 * age;
        return Profile.FEMALE.equals(p.sex) ? base - 161 : base + 5;
    }

    /** Dépense quotidienne hors entraînement. */
    public static int maintenance(Profile p, double weight, int age) {
        return (int) Math.round(bmr(p, weight, age) * p.activity);
    }

    /** Objectif calorique du jour, hors calories d'entraînement (ajoutées à part). */
    public static int target(Profile p, double weight, int age) {
        if (p.kcalOverride > 0) return p.kcalOverride;
        double m = maintenance(p, weight, age);
        if (Profile.LOSE.equals(p.goal)) m *= 0.8;
        else if (Profile.GAIN.equals(p.goal)) m *= 1.1;
        return (int) (Math.round(m / 10.0) * 10);
    }

    /** Répartition : protéines et lipides en g/kg, glucides pour le reste. */
    public static Macros macros(Profile p, double weight, int kcal) {
        Macros m = new Macros();
        m.kcal = kcal;
        m.protein = (int) Math.round(p.proteinPerKg * weight);
        m.fat = (int) Math.round(p.fatPerKg * weight);
        m.carbs = (int) Math.max(0, Math.round((kcal - 4.0 * m.protein - 9.0 * m.fat) / 4.0));
        return m;
    }

    /** Eau : 35 ml/kg, +500 ml un jour d'entraînement, arrondi à 50 ml. */
    public static int water(Profile p, double weight, boolean trained) {
        if (p.waterOverride > 0) return p.waterOverride;
        double ml = 35 * (weight > 0 ? weight : DEFAULT_WEIGHT) + (trained ? 500 : 0);
        return (int) (Math.round(ml / 50.0) * 50);
    }

    /** kcal = MET × 3,5 × poids / 200 × minutes (formule ACSM). */
    public static int kcal(double met, double weight, double minutes) {
        return (int) Math.round(met * 3.5 * weight / 200.0 * minutes);
    }

    /** Dépense des séances et activités d'un jour. */
    public static List<Burn> burned(int day, List<Session> sessions, List<WodResult> wods, List<RenfoLog> renfo,
                                    List<BodyLog.Activity> activities, double weight) {
        List<Burn> out = new ArrayList<>();
        for (Session s : sessions) {
            if (s.inProgress() || Days.key(s.start) != day) continue;
            int min = (int) Math.max(10, Math.min(180, s.durationMs() / 60000));
            out.add(new Burn(s.name, min, kcal(MET_MUSCU, weight, min)));
        }
        for (WodResult r : wods) {
            if (Days.key(r.date) != day) continue;
            int min = r.minutes > 0 ? r.minutes : 20;
            out.add(new Burn(r.wodName, min, kcal(MET_WOD, weight, min)));
        }
        for (RenfoLog l : renfo) {
            if (Days.key(l.date) != day) continue;
            int min = Math.max(1, l.seconds / 60);
            out.add(new Burn(l.name, min, kcal(MET_RENFO, weight, min)));
        }
        for (BodyLog.Activity a : activities) {
            if (a.day != day) continue;
            out.add(new Burn(a.name, a.minutes, a.kcal));
        }
        return out;
    }

    public static int total(List<Burn> burns) {
        int t = 0;
        for (Burn b : burns) t += b.kcal;
        return t;
    }

    public static Totals totals(List<FoodEntry> entries, int day, String meal) {
        Totals t = new Totals();
        for (FoodEntry e : entries) {
            if (e.day != day) continue;
            if (meal != null && !meal.equals(e.meal)) continue;
            t.add(e);
        }
        return t;
    }

    public static int waterOf(List<BodyLog.Water> water, int day) {
        int ml = 0;
        for (BodyLog.Water w : water) if (w.day == day) ml += w.ml;
        return ml;
    }

    /** Indice de masse corporelle. */
    public static double bmi(double weight, double heightCm) {
        if (weight <= 0 || heightCm <= 0) return 0;
        double m = heightCm / 100.0;
        return weight / (m * m);
    }

    public static String bmiLabel(double bmi) {
        if (bmi <= 0) return "";
        if (bmi < 18.5) return "insuffisance pondérale";
        if (bmi < 25) return "corpulence normale";
        if (bmi < 30) return "surpoids";
        return "obésité";
    }

    /** Masse grasse estimée (méthode de l'US Navy, mesures en cm), ou 0 si mesures insuffisantes. */
    public static double navyBodyFat(String sex, double heightCm, double neck, double waist, double hips) {
        if (heightCm <= 0 || neck <= 0 || waist <= 0) return 0;
        double v;
        if (Profile.FEMALE.equals(sex)) {
            if (hips <= 0 || waist + hips - neck <= 0) return 0;
            v = 495 / (1.29579 - 0.35004 * Math.log10(waist + hips - neck) + 0.22100 * Math.log10(heightCm)) - 450;
        } else {
            if (waist - neck <= 0) return 0;
            v = 495 / (1.0324 - 0.19077 * Math.log10(waist - neck) + 0.15456 * Math.log10(heightCm)) - 450;
        }
        return v > 2 && v < 70 ? Stats.round1(v) : 0;
    }
}

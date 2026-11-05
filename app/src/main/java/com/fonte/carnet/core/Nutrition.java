package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Besoins du jour, méthode 4.3 (voir le document « Meilleures méthodes de calcul des calories et des macros ») :
 * <ol>
 * <li>métabolisme de base de Ten Haaf (2014), ou Cunningham / Ten Haaf masse maigre si la masse grasse est connue ;</li>
 * <li>× coefficient de la vie quotidienne sans le sport (fourchettes FAO/OMS 2004) ;</li>
 * <li>objectif en rythme de poids (% du poids par semaine, 7 700 kcal par kg), plus la correction d'après les pesées ;</li>
 * <li>+ calories nettes des séances du jour (MET − 1, Compendium 2024), ajoutées en glucides ;</li>
 * <li>protéines en g/kg, lipides en % (au moins 0,5 g/kg), glucides = le reste.</li>
 * </ol>
 */
public final class Nutrition {
    private Nutrition() {
    }

    /** Poids utilisé tant qu'aucune pesée n'est enregistrée. */
    public static final double DEFAULT_WEIGHT = 75;
    /** Énergie d'un kg de poids perdu ou pris (approximation classique). */
    public static final double KCAL_PER_KG = 7700;

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

    /** Objectifs d'une journée, avec le détail du calcul. */
    public static class Targets {
        /** Équation réellement utilisée. */
        public String bmrFormula;
        /** Équation à masse maigre demandée mais masse grasse inconnue : Ten Haaf (poids) utilisée à la place. */
        public boolean leanFallback;
        public int bmr;
        public double daily;
        /** Dépense hors sport : métabolisme × coefficient du quotidien. */
        public int expenditure;
        /** Ajustement de l'objectif (kcal/jour, négatif en perte). */
        public int goalKcal;
        /** Correction validée d'après les pesées. */
        public int correction;
        /** Objectif d'un jour sans séance. */
        public int base;
        /** Objectif remonté au minimum (métabolisme de base, 1 200 ou 1 500 kcal). */
        public boolean floored;
        /** Objectif imposé dans le profil. */
        public boolean imposed;
        /** Calories nettes des séances du jour (arrondies à 10). */
        public int sessionKcal;
        public int sessionMinutes;
        /** Séance prévue (pas encore notée). */
        public boolean planned;
        public int kcal;
        public int protein;
        public int carbs;
        public int fat;
        public int water;
        /** Protéines en g par kg (de masse maigre si proteinLean). */
        public double proteinFactor;
        public boolean proteinLean;
        public boolean percentMode;

        public Macros macros() {
            Macros m = new Macros();
            m.kcal = kcal;
            m.protein = protein;
            m.carbs = carbs;
            m.fat = fat;
            return m;
        }
    }

    // ---------------------------------------------------------------- métabolisme de base

    /** Taille pour les calculs : celle du profil, sinon une taille moyenne (175 cm, 162 cm). */
    static double height(Profile p) {
        if (p.heightCm >= 100) return p.heightCm;
        return Profile.FEMALE.equals(p.sex) ? 162 : 175;
    }

    /** Mifflin-St Jeor (1990). */
    public static double mifflin(Profile p, double weight, int age) {
        double base = 10 * weight + 6.25 * height(p) - 5 * age;
        return Profile.FEMALE.equals(p.sex) ? base - 161 : base + 5;
    }

    /** Ten Haaf et Weijs (2014), à partir du poids : la plus précise chez les sportifs (Sports Medicine 2023). */
    public static double tenHaaf(Profile p, double weight, int age) {
        double sex = Profile.FEMALE.equals(p.sex) ? 0 : 1;
        return 11.936 * weight + 587.728 * (height(p) / 100.0) - 8.129 * age + 191.027 * sex + 29.279;
    }

    /** Ten Haaf et Weijs (2014), à partir de la masse maigre (kg). */
    public static double tenHaafLean(double leanKg) {
        return 22.771 * leanKg + 484.264;
    }

    /** Cunningham (1980), à partir de la masse maigre (kg). */
    public static double cunningham(double leanKg) {
        return 500 + 22 * leanKg;
    }

    /** Équation utilisée : celles à masse maigre demandent une masse grasse connue. */
    public static String formula(Profile p, double bodyFat) {
        String f = p.bmrFormula;
        boolean known = false;
        for (String id : Profile.BMR_IDS) if (id.equals(f)) known = true;
        if (!known) return Profile.BMR_TENHAAF;
        if (Profile.needsLeanMass(f) && !(bodyFat > 2 && bodyFat < 70)) return Profile.BMR_TENHAAF;
        return f;
    }

    public static double leanMass(double weight, double bodyFat) {
        return bodyFat > 2 && bodyFat < 70 ? weight * (1 - bodyFat / 100.0) : 0;
    }

    /** Métabolisme de base (kcal/jour) avec l'équation du profil. */
    public static double bmr(Profile p, double weight, int age, double bodyFat) {
        String f = formula(p, bodyFat);
        double lean = leanMass(weight, bodyFat);
        switch (f) {
            case Profile.BMR_TENHAAF_LEAN:
                return tenHaafLean(lean);
            case Profile.BMR_CUNNINGHAM:
                return cunningham(lean);
            case Profile.BMR_MIFFLIN:
                return mifflin(p, weight, age);
            default:
                return tenHaaf(p, weight, age);
        }
    }

    // ---------------------------------------------------------------- objectifs du jour

    private static int round10(double v) {
        return (int) (Math.round(v / 10.0) * 10);
    }

    /**
     * Objectifs d'un jour.
     *
     * @param bodyFat        masse grasse en % (0 si inconnue)
     * @param sessionKcal    calories nettes des séances du jour (notées ou prévues)
     * @param sessionMinutes durée des séances du jour (pour l'eau)
     * @param planned        séance prévue mais pas encore notée
     */
    public static Targets targets(Profile p, double weight, int age, double bodyFat, int sessionKcal, int sessionMinutes,
                                  boolean planned) {
        Targets t = new Targets();
        double w = weight > 0 ? weight : DEFAULT_WEIGHT;
        t.bmrFormula = formula(p, bodyFat);
        t.leanFallback = Profile.needsLeanMass(p.bmrFormula) && !Profile.needsLeanMass(t.bmrFormula);
        double bmr = bmr(p, w, age, bodyFat);
        t.bmr = (int) Math.round(bmr);
        t.daily = p.dailyFactor();
        double expenditure = bmr * t.daily;
        t.expenditure = (int) Math.round(expenditure);
        t.goalKcal = (int) Math.round(w * p.ratePct() / 100.0 * KCAL_PER_KG / 7.0);
        t.correction = Math.max(-600, Math.min(600, p.kcalAdjust));
        double base = expenditure + t.goalKcal + t.correction;
        double floor = Math.max(bmr, Profile.FEMALE.equals(p.sex) ? 1200 : 1500);
        if (base < floor) {
            base = floor;
            t.floored = true;
        }
        t.sessionMinutes = Math.max(0, Math.min(240, sessionMinutes));
        t.planned = planned;
        if (p.kcalOverride > 0) {
            t.imposed = true;
            t.base = p.kcalOverride;
            t.sessionKcal = 0;
        } else {
            t.base = round10(base);
            t.sessionKcal = round10(Math.max(0, sessionKcal));
        }
        t.kcal = t.base + t.sessionKcal;

        t.percentMode = p.percentMode();
        if (t.percentMode) {
            int[] pct = p.percents();
            t.protein = (int) Math.round(t.kcal * pct[0] / 100.0 / 4.0);
            t.carbs = (int) Math.round(t.kcal * pct[1] / 100.0 / 4.0);
            t.fat = (int) Math.round(t.kcal * pct[2] / 100.0 / 9.0);
            t.proteinFactor = t.protein / w;
        } else {
            double lean = leanMass(w, bodyFat);
            if (Profile.LOSE.equals(p.goal)) {
                if (lean > 0) {
                    t.proteinLean = true;
                    t.proteinFactor = Profile.PROTEIN_LOSS_LEAN;
                    t.protein = (int) Math.round(Profile.PROTEIN_LOSS_LEAN * lean);
                } else {
                    t.proteinFactor = Profile.PROTEIN_LOSS_WEIGHT;
                    t.protein = (int) Math.round(Profile.PROTEIN_LOSS_WEIGHT * w);
                }
            } else {
                t.proteinFactor = Math.max(1.2, Math.min(3.1, p.proteinPerKg));
                t.protein = (int) Math.round(t.proteinFactor * w);
            }
            int fatPct = Math.max(15, Math.min(40, p.fatPct));
            int floorFat = (int) Math.round(Profile.FAT_FLOOR * w);
            int fat = Math.max(floorFat, (int) Math.round(t.base * fatPct / 100.0 / 9.0));
            if (t.base - 4 * t.protein - 9 * fat < 0) fat = Math.max(floorFat, (int) Math.floor((t.base - 4 * t.protein) / 9.0));
            t.fat = fat;
            t.carbs = (int) Math.max(0, Math.round((t.kcal - 4 * t.protein - 9 * t.fat) / 4.0));
        }
        t.water = water(p, w, t.sessionMinutes);
        return t;
    }

    /** Eau : 35 ml/kg, +500 ml par heure de séance, arrondi à 50 ml. */
    public static int water(Profile p, double weight, int sessionMinutes) {
        if (p.waterOverride > 0) return p.waterOverride;
        double ml = 35 * (weight > 0 ? weight : DEFAULT_WEIGHT) + 500 * Math.max(0, Math.min(240, sessionMinutes)) / 60.0;
        return (int) (Math.round(ml / 50.0) * 50);
    }

    // ---------------------------------------------------------------- réglage par les pesées

    /** Tendance du poids et correction proposée. */
    public static class Trend {
        /** Pente des pesées, kg par semaine (NaN si pas assez de pesées). */
        public double kgPerWeek = Double.NaN;
        /** Rythme visé, kg par semaine. */
        public double expected;
        public int count;
        public int spanDays;
        /** Correction proposée, kcal/jour (0 = rien à corriger ou pas assez de données). */
        public int suggestion;

        public boolean ready() {
            return !Double.isNaN(kgPerWeek);
        }
    }

    /**
     * Compare la pente des pesées des 4 dernières semaines (depuis la dernière correction) au rythme visé.
     * Il faut au moins 4 pesées sur 14 jours. Écart ≥ 100 kcal/jour → correction de 100 à 200 kcal.
     */
    public static Trend trend(Profile p, List<BodyLog.Weight> weights, double currentWeight, int today) {
        Trend t = new Trend();
        double w = currentWeight > 0 ? currentWeight : DEFAULT_WEIGHT;
        t.expected = w * p.ratePct() / 100.0;
        int from = Days.add(today, -28);
        if (p.adjustDay > from) from = p.adjustDay;
        long ref = Days.start(today);
        List<double[]> pts = new ArrayList<>();
        double minX = 1e9, maxX = -1e9;
        for (BodyLog.Weight x : weights) {
            if (x.kg <= 0 || x.day <= from || x.day > today) continue;
            double d = Math.round((Days.start(x.day) - ref) / 86_400_000.0);
            pts.add(new double[]{d, x.kg});
            minX = Math.min(minX, d);
            maxX = Math.max(maxX, d);
        }
        t.count = pts.size();
        t.spanDays = pts.isEmpty() ? 0 : (int) Math.round(maxX - minX);
        if (t.count < 4 || t.spanDays < 14) return t;
        double mx = 0, my = 0;
        for (double[] q : pts) {
            mx += q[0];
            my += q[1];
        }
        mx /= t.count;
        my /= t.count;
        double sxy = 0, sxx = 0;
        for (double[] q : pts) {
            sxy += (q[0] - mx) * (q[1] - my);
            sxx += (q[0] - mx) * (q[0] - mx);
        }
        if (sxx <= 0) return t;
        t.kgPerWeek = sxy / sxx * 7;
        double gapKcal = (t.kgPerWeek - t.expected) * KCAL_PER_KG / 7.0;
        if (Math.abs(gapKcal) >= 100) {
            int step = (int) (Math.round(Math.abs(gapKcal) / 50.0) * 50);
            step = Math.max(100, Math.min(200, step));
            t.suggestion = gapKcal > 0 ? -step : step;
        }
        return t;
    }

    // ---------------------------------------------------------------- séances

    /** kcal brutes = MET × 3,5 × poids / 200 × minutes (formule ACSM). */
    public static int kcal(double met, double weight, double minutes) {
        return (int) Math.round(met * 3.5 * weight / 200.0 * minutes);
    }

    /** kcal nettes : le premier MET (le repos) est déjà compté dans le métabolisme de base. */
    public static int netKcal(double met, double weight, double minutes) {
        return kcal(Math.max(0, met - 1), weight, minutes);
    }

    /** Dépense nette des séances et activités d'un jour. */
    public static List<Burn> burned(int day, List<Session> sessions, List<WodResult> wods, List<RenfoLog> renfo,
                                    List<BodyLog.Activity> activities, double weight) {
        List<Burn> out = new ArrayList<>();
        for (Session s : sessions) {
            if (s.inProgress() || Days.key(s.start) != day) continue;
            // Le cardio noté dans la séance compte à part : la muscu prend le reste du temps.
            int cardio = 0;
            for (BodyLog.Activity a : activities) if (a.sessionId == s.id) cardio += a.minutes;
            if (s.exercises.isEmpty() && cardio > 0) continue;
            int min = (int) Math.max(cardio > 0 ? 5 : 10, Math.min(180, s.durationMs() / 60000 - cardio));
            out.add(new Burn(s.name, min, netKcal(MET_MUSCU, weight, min)));
        }
        for (WodResult r : wods) {
            if (Days.key(r.date) != day) continue;
            int min = r.minutes > 0 ? r.minutes : 20;
            out.add(new Burn(r.wodName, min, netKcal(MET_WOD, weight, min)));
        }
        for (RenfoLog l : renfo) {
            if (Days.key(l.date) != day) continue;
            int min = Math.max(1, l.seconds / 60);
            out.add(new Burn(l.name, min, netKcal(MET_RENFO, weight, min)));
        }
        for (BodyLog.Activity a : activities) {
            if (a.day != day) continue;
            out.add(new Burn(a.name, a.minutes, a.kcal));
        }
        return out;
    }

    public static int minutes(List<Burn> burns) {
        int t = 0;
        for (Burn b : burns) t += b.minutes;
        return t;
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

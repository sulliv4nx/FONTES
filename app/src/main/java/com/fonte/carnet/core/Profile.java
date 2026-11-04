package com.fonte.carnet.core;

import java.util.Calendar;

/**
 * Profil utilisé pour les besoins nutritionnels.
 * Méthode 4.3 : métabolisme de Ten Haaf, coefficient du quotidien hors sport + séances en calories nettes,
 * objectif en rythme de poids, protéines en g/kg, lipides en %, glucides en reste.
 */
public class Profile {
    public static final String MALE = "H";
    public static final String FEMALE = "F";

    public static final String LOSE = "perte";
    public static final String MAINTAIN = "maintien";
    public static final String GAIN = "prise";

    // ---------------------------------------------------------------- métabolisme de base

    public static final String BMR_TENHAAF = "tenhaaf";
    public static final String BMR_TENHAAF_LEAN = "tenhaaf_mm";
    public static final String BMR_CUNNINGHAM = "cunningham";
    public static final String BMR_MIFFLIN = "mifflin";
    public static final String[] BMR_IDS = {BMR_TENHAAF, BMR_TENHAAF_LEAN, BMR_CUNNINGHAM, BMR_MIFFLIN};
    public static final String[] BMR_LABELS = {"Ten Haaf", "Ten Haaf · masse maigre", "Cunningham", "Mifflin-St Jeor"};

    // ---------------------------------------------------------------- activité hors sport (fourchettes FAO/OMS)

    public static final double[] DAILY = {1.4, 1.55, 1.75};
    public static final String[] DAILY_LABELS = {
            "Assis la plupart du temps · bureau, voiture",
            "Debout ou en mouvement · commerce, enseignement, service",
            "Travail physique · chantier, manutention, agriculture"};

    // ---------------------------------------------------------------- objectif en rythme de poids

    /** Rythmes proposés, en % du poids par semaine. */
    public static final double[] LOSS_RATES = {0.5, 0.75, 1.0};
    public static final double[] GAIN_RATES = {0.25, 0.35, 0.5};

    // ---------------------------------------------------------------- macros

    public static final String METHOD_GRAMS = "grammes";
    public static final String METHOD_PERCENT = "pourcentages";
    public static final double[] PROTEIN_CHOICES = {1.6, 1.8, 2.0, 2.2};
    /** Protéines en sèche, en g par kg de masse maigre (Helms 2014, ISSN 2017 : 2,3 à 3,1). */
    public static final double PROTEIN_LOSS_LEAN = 2.6;
    /** Protéines en sèche quand la masse grasse est inconnue, en g par kg de poids. */
    public static final double PROTEIN_LOSS_WEIGHT = 2.2;
    public static final int[] FAT_CHOICES = {20, 25, 30, 35};
    /** Minimum de lipides, en g par kg. */
    public static final double FAT_FLOOR = 0.5;

    /** Anciens niveaux « sport compris » (Fonte 4.2), gardés pour relire les sauvegardes. */
    public static final double[] ACTIVITY = {1.2, 1.375, 1.55, 1.725, 1.9};

    /** Répartitions en % de l'article « Calculer ses macros » (mode pourcentages). */
    public static final String SPLIT_AUTO = "auto";
    public static final String SPLIT_CUSTOM = "perso";
    public static final String[] SPLIT_IDS = {"perte", "maintien", "muscle", "athlete", "hyper", "keto"};
    public static final String[] SPLIT_LABELS = {"Perte de poids", "Maintien", "Prise de muscle", "Sportif de haut niveau",
            "Hyperprotéiné", "Cétogène"};
    /** {protéines, glucides, lipides} en % des calories. */
    public static final int[][] SPLITS = {{35, 35, 30}, {25, 50, 25}, {40, 50, 10}, {30, 50, 20}, {65, 10, 25}, {40, 10, 50}};

    public String sex = MALE;
    public int birthYear;
    public double heightCm;
    /** Équation du métabolisme de base. */
    public String bmrFormula = BMR_TENHAAF;
    /** Coefficient de la vie quotidienne, sans le sport. */
    public double daily = 1.4;
    /** Ancien niveau d'activité sport compris (4.2), lu pour la compatibilité seulement. */
    public double activity = 1.375;
    public String goal = MAINTAIN;
    public double lossRate = 0.75;
    public double gainRate = 0.25;
    /** Objectif calorique imposé (0 = calculé). */
    public int kcalOverride;
    /** Correction validée d'après les pesées (kcal par jour). */
    public int kcalAdjust;
    /** Jour de la dernière correction : les pesées d'avant ne comptent plus. */
    public int adjustDay;
    /** Méthode des macros : en grammes (conseillée) ou en pourcentages. */
    public String method = METHOD_GRAMS;
    /** Protéines en maintien ou en prise, g/kg. */
    public double proteinPerKg = 2.0;
    /** Lipides en % des calories du jour sans séance. */
    public int fatPct = 25;
    /** Ancien réglage (v3), gardé pour la compatibilité. */
    public double fatPerKg = 0.9;
    /** Répartition choisie en mode pourcentages : « auto », un des SPLIT_IDS, ou « perso ». */
    public String split = SPLIT_AUTO;
    public int pctProtein = 30;
    public int pctCarbs = 50;
    public int pctFat = 20;
    /** Objectif d'eau imposé en ml (0 = calculé). */
    public int waterOverride;

    public boolean complete() {
        return birthYear > 1900 && heightCm >= 100;
    }

    public int age(long now) {
        if (birthYear <= 0) return 30;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        return Math.max(10, c.get(Calendar.YEAR) - birthYear);
    }

    public boolean percentMode() {
        return METHOD_PERCENT.equals(method);
    }

    public static String bmrLabel(String id) {
        for (int i = 0; i < BMR_IDS.length; i++) if (BMR_IDS[i].equals(id)) return BMR_LABELS[i];
        return BMR_LABELS[0];
    }

    public static boolean needsLeanMass(String id) {
        return BMR_TENHAAF_LEAN.equals(id) || BMR_CUNNINGHAM.equals(id);
    }

    /** Rythme visé en % du poids par semaine : négatif en perte, positif en prise, 0 en maintien. */
    public double ratePct() {
        if (LOSE.equals(goal)) return -Math.max(0.25, Math.min(1.0, lossRate));
        if (GAIN.equals(goal)) return Math.max(0.1, Math.min(0.5, gainRate));
        return 0;
    }

    /** Coefficient du quotidien borné aux valeurs FAO plausibles. */
    public double dailyFactor() {
        return daily >= 1.2 && daily <= 2.0 ? daily : 1.4;
    }

    /** Identifiant de la répartition réellement utilisée (mode pourcentages). */
    public String splitId() {
        if (SPLIT_CUSTOM.equals(split)) return SPLIT_CUSTOM;
        for (String id : SPLIT_IDS) if (id.equals(split)) return id;
        if (LOSE.equals(goal)) return "perte";
        if (GAIN.equals(goal)) return "muscle";
        return "maintien";
    }

    /** {protéines, glucides, lipides} en % des calories, total ramené à 100. */
    public int[] percents() {
        String id = splitId();
        int[] v;
        if (SPLIT_CUSTOM.equals(id)) {
            v = new int[]{Math.max(0, pctProtein), Math.max(0, pctCarbs), Math.max(0, pctFat)};
        } else {
            v = SPLITS[0];
            for (int i = 0; i < SPLIT_IDS.length; i++) if (SPLIT_IDS[i].equals(id)) v = SPLITS[i];
        }
        int sum = v[0] + v[1] + v[2];
        if (sum <= 0) return new int[]{30, 50, 20};
        if (sum == 100) return v.clone();
        int p = Math.round(v[0] * 100f / sum);
        int c = Math.round(v[1] * 100f / sum);
        return new int[]{p, c, 100 - p - c};
    }

    public String splitLabel() {
        String id = splitId();
        if (SPLIT_CUSTOM.equals(id)) return "Personnalisée";
        for (int i = 0; i < SPLIT_IDS.length; i++) if (SPLIT_IDS[i].equals(id)) return SPLIT_LABELS[i];
        return "";
    }

    public String goalLabel() {
        if (LOSE.equals(goal)) return "Perte de poids";
        if (GAIN.equals(goal)) return "Prise de masse";
        return "Maintien";
    }
}

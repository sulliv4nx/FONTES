package com.fonte.carnet.core;

import java.util.Calendar;

/** Profil utilisé pour les besoins nutritionnels. */
public class Profile {
    public static final String MALE = "H";
    public static final String FEMALE = "F";

    public static final String LOSE = "perte";
    public static final String MAINTAIN = "maintien";
    public static final String GAIN = "prise";

    /** Niveaux d'activité HORS entraînement (l'entraînement est compté chaque jour à part). */
    public static final double[] ACTIVITY = {1.2, 1.375, 1.55, 1.725};
    public static final String[] ACTIVITY_LABELS = {
            "Sédentaire · bureau, peu de marche",
            "Légèrement actif · debout ou marche une partie de la journée",
            "Actif · métier physique ou beaucoup de marche",
            "Très actif · métier très physique"};

    public String sex = MALE;
    public int birthYear;
    public double heightCm;
    public double activity = 1.375;
    public String goal = MAINTAIN;
    /** Objectif calorique imposé (0 = calculé). */
    public int kcalOverride;
    public double proteinPerKg = 2.0;
    public double fatPerKg = 0.9;
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

    public String goalLabel() {
        if (LOSE.equals(goal)) return "Perte de poids";
        if (GAIN.equals(goal)) return "Prise de masse";
        return "Maintien";
    }
}

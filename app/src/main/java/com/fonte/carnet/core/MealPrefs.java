package com.fonte.carnet.core;

/** Réglages de « Mes repas ». */
public class MealPrefs {
    public static final String[] VARIETY = {"Simple", "Équilibré", "Varié"};

    /** 3, 4 ou 5 prises par jour. */
    public int meals = 5;
    public boolean shaker = true;
    public boolean shakerEveryDay;
    /** Jours d'entraînement pour le shaker : bit 0 = lundi … bit 6 = dimanche ; -1 = pas encore réglé. */
    public int trainingMask = -1;
    public boolean noPork;
    public boolean vegetarian;
    public boolean lactoseFree;
    public boolean fish;
    public boolean creole;
    public boolean budget;
    /** Aliments à éviter, séparés par des virgules. */
    public String avoid = "";
    public boolean batch = true;
    /** 0 simple, 1 équilibré, 2 varié. */
    public int variety = 1;

    public MealPrefs copy() {
        MealPrefs p = new MealPrefs();
        p.meals = meals;
        p.shaker = shaker;
        p.shakerEveryDay = shakerEveryDay;
        p.trainingMask = trainingMask;
        p.noPork = noPork;
        p.vegetarian = vegetarian;
        p.lactoseFree = lactoseFree;
        p.fish = fish;
        p.creole = creole;
        p.budget = budget;
        p.avoid = avoid;
        p.batch = batch;
        p.variety = variety;
        return p;
    }

    public String mealsLabel() {
        switch (meals) {
            case 3:
                return "3 repas, sans collation";
            case 4:
                return "3 repas + 1 collation";
            default:
                return "3 repas + 2 collations";
        }
    }
}

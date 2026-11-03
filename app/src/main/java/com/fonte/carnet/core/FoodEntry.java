package com.fonte.carnet.core;

/** Aliment consommé (valeurs absolues pour la quantité mangée). */
public class FoodEntry {
    public static final String BREAKFAST = "Petit-déjeuner";
    public static final String LUNCH = "Déjeuner";
    public static final String DINNER = "Dîner";
    public static final String SNACK = "Collations";
    public static final String[] MEALS = {BREAKFAST, LUNCH, DINNER, SNACK};

    public long id;
    public int day;
    public long time;
    public String meal = SNACK;
    public String foodId;
    public String name;
    public double grams;
    public double kcal;
    public double protein;
    public double carbs;
    public double fat;
    /** Chemin de la photo du repas (stockage privé), ou null. */
    public String photo;

    public static FoodEntry of(Food f, double grams) {
        FoodEntry e = new FoodEntry();
        e.foodId = f.id;
        e.name = f.name;
        e.grams = grams;
        double k = grams / 100.0;
        e.kcal = f.kcal * k;
        e.protein = f.protein * k;
        e.carbs = f.carbs * k;
        e.fat = f.fat * k;
        return e;
    }

    /** Meilleur repas par défaut selon l'heure. */
    public static String mealFor(long time) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(time);
        int h = c.get(java.util.Calendar.HOUR_OF_DAY);
        if (h >= 5 && h < 10) return BREAKFAST;
        if (h >= 11 && h < 15) return LUNCH;
        if (h >= 18 && h < 23) return DINNER;
        return SNACK;
    }
}

package com.fonte.carnet.core;

/** Recette de base (plat, petit-déjeuner, collation ou shaker), pour une portion « type ». */
public class Recipe {
    public static final String BREAKFAST = "breakfast";
    public static final String SNACK = "snack";
    public static final String MAIN = "main";
    public static final String SHAKER = "shaker";

    // Étiquettes utilisées par les préférences.
    public static final String MEAT = "viande";
    public static final String PORK = "porc";
    public static final String FISH = "poisson";
    public static final String LACTOSE = "lactose";
    public static final String CREOLE = "creole";
    public static final String BUDGET = "budget";
    public static final String PRICEY = "cher";
    /** Plat plaisir, plus gras : proposé moins souvent. */
    public static final String TREAT = "plaisir";

    /** Rôle d'un ingrédient quand on ajuste les quantités. */
    public static final int PROT = 0;
    public static final int REST = 1;
    public static final int FIXED = 2;
    /** Matières grasses (huile, oléagineux, avocat, fromage) : ajustées pour viser les lipides. */
    public static final int FAT = 3;

    public static class Ing {
        public final String foodId;
        public final double grams;
        public final int role;

        Ing(String foodId, double grams, int role) {
            this.foodId = foodId;
            this.grams = grams;
            this.role = role;
        }
    }

    public final String id;
    public final String kind;
    public final String name;
    public final int minutes;
    public final String[] tags;
    public final Ing[] ingredients;
    public final String[] steps;

    Recipe(String id, String kind, String name, int minutes, String[] tags, Ing[] ingredients, String[] steps) {
        this.id = id;
        this.kind = kind;
        this.name = name;
        this.minutes = minutes;
        this.tags = tags;
        this.ingredients = ingredients;
        this.steps = steps;
    }

    public boolean has(String tag) {
        for (String t : tags) if (t.equals(tag)) return true;
        return false;
    }

    public boolean vegetarian() {
        return !has(MEAT) && !has(FISH) && !has(PORK);
    }
}

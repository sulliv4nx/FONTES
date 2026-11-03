package com.fonte.carnet.core;

/** Aliment : valeurs pour 100 g (ou 100 ml). */
public class Food {
    public String id;
    public String name;
    public String category;
    public double kcal;
    public double protein;
    public double carbs;
    public double fat;
    /** Portion habituelle en g (0 si aucune). */
    public double portion;
    public String portionLabel;
    public String barcode;
    public boolean custom;

    public Food() {
    }

    Food(String id, String name, String category, double kcal, double protein, double carbs, double fat, double portion, String portionLabel) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.kcal = kcal;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.portion = portion;
        this.portionLabel = portionLabel;
    }

    /** « 165 kcal · P 31 · G 0 · L 3,6 (100 g) » */
    public String per100() {
        return Fmt.kg(Math.round(kcal)) + " kcal · P " + Fmt.kg(Stats.round1(protein)) + " · G " + Fmt.kg(Stats.round1(carbs))
                + " · L " + Fmt.kg(Stats.round1(fat)) + " pour 100 g";
    }
}

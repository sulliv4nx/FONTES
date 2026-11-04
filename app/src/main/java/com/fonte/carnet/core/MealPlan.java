package com.fonte.carnet.core;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Plan de repas sur plusieurs jours, avec l'état de la liste de courses. */
public class MealPlan {
    // Créneaux d'une journée, dans l'ordre.
    public static final String BREAKFAST = "breakfast";
    public static final String SNACK_AM = "snack_am";
    public static final String LUNCH = "lunch";
    public static final String SNACK_PM = "snack_pm";
    public static final String SHAKER = "shaker";
    public static final String DINNER = "dinner";

    public static class Item {
        public String foodId;
        public String name;
        public double grams;

        public Item() {
        }

        public Item(String foodId, String name, double grams) {
            this.foodId = foodId;
            this.name = name;
            this.grams = grams;
        }
    }

    public static class Meal {
        public int day;
        public String slot;
        public String recipeId;
        public String name;
        public final List<Item> items = new ArrayList<>();
        /** Objectif visé par ce repas (calories, protéines), gardé pour les remplacements. */
        public double targetKcal;
        public double targetProtein;
        public double targetFat;
        public boolean eaten;
        /** Entrées du journal créées quand le repas a été coché. */
        public final List<Long> entries = new ArrayList<>();

        public Nutrition.Totals totals() {
            Nutrition.Totals t = new Nutrition.Totals();
            for (Item i : items) {
                Food f = Foods.find(i.foodId);
                if (f == null) continue;
                double k = i.grams / 100.0;
                t.kcal += f.kcal * k;
                t.protein += f.protein * k;
                t.carbs += f.carbs * k;
                t.fat += f.fat * k;
            }
            return t;
        }

        public double grams() {
            double g = 0;
            for (Item i : items) g += i.grams;
            return g;
        }

        public String kind() {
            return MealPlan.kind(slot);
        }

        /** « Avoine 70 g · Lait demi-écrémé 250 ml · 1 banane · Whey 30 g ». */
        public String summary() {
            StringBuilder b = new StringBuilder();
            for (Item i : items) {
                if (b.length() > 0) b.append(" · ");
                b.append(Groceries.line(i.foodId, i.name, i.grams));
            }
            return b.toString();
        }
    }

    public static class Extra {
        public String name;
        public boolean checked;
    }

    public int start;
    public int days = 7;
    public long seed;
    public long created;
    /** Objectifs d'un jour sans séance. */
    public int kcal;
    public int protein;
    public int carbs;
    public int fat;
    /** Objectifs d'un jour d'entraînement (0 = comme un jour sans séance). */
    public int trainKcal;
    public int trainProtein;
    public int trainCarbs;
    public int trainFat;
    /** Jours d'entraînement prévus (bit 0 = lundi) au moment du calcul. */
    public int trainingMask;
    public final List<Meal> meals = new ArrayList<>();
    /** Articles cochés dans la liste de courses (identifiants d'aliments). */
    public final Set<String> checked = new LinkedHashSet<>();
    public final List<Extra> extras = new ArrayList<>();

    public static String kind(String slot) {
        if (BREAKFAST.equals(slot)) return Recipe.BREAKFAST;
        if (SHAKER.equals(slot)) return Recipe.SHAKER;
        if (LUNCH.equals(slot) || DINNER.equals(slot)) return Recipe.MAIN;
        return Recipe.SNACK;
    }

    /** Repas du journal correspondant à un créneau. */
    public static String journalMeal(String slot) {
        if (BREAKFAST.equals(slot)) return FoodEntry.BREAKFAST;
        if (LUNCH.equals(slot)) return FoodEntry.LUNCH;
        if (DINNER.equals(slot)) return FoodEntry.DINNER;
        return FoodEntry.SNACK;
    }

    public static String slotLabel(String slot) {
        switch (slot) {
            case BREAKFAST:
                return "Petit-déjeuner";
            case LUNCH:
                return "Déjeuner";
            case DINNER:
                return "Dîner";
            case SHAKER:
                return "Shaker";
            default:
                return "Collation";
        }
    }

    /** Moment indicatif de la journée. */
    public static String slotTime(String slot) {
        switch (slot) {
            case BREAKFAST:
                return "7 h 30";
            case SNACK_AM:
                return "10 h 30";
            case LUNCH:
                return "12 h 30";
            case SNACK_PM:
                return "16 h 30";
            case SHAKER:
                return "après la séance";
            default:
                return "20 h";
        }
    }

    /** Heure utilisée pour classer l'entrée dans le journal. */
    public static int slotHour(String slot) {
        switch (slot) {
            case BREAKFAST:
                return 7;
            case SNACK_AM:
                return 10;
            case LUNCH:
                return 12;
            case SNACK_PM:
            case SHAKER:
                return 17;
            default:
                return 20;
        }
    }

    /** Jour d'entraînement prévu dans ce plan. */
    public boolean trainingDay(int day) {
        return trainKcal > 0 && (trainingMask & (1 << MealPlanner.weekday(day))) != 0;
    }

    /** Objectifs du jour {kcal, protéines, glucides, lipides}. */
    public int[] targetsOn(int day) {
        if (trainingDay(day)) return new int[]{trainKcal, trainProtein, trainCarbs, trainFat};
        return new int[]{kcal, protein, carbs, fat};
    }

    public int end() {
        return Days.add(start, days - 1);
    }

    public boolean covers(int day) {
        return day >= start && day <= end();
    }

    public List<Meal> mealsOf(int day) {
        List<Meal> out = new ArrayList<>();
        for (Meal m : meals) if (m.day == day) out.add(m);
        return out;
    }

    public Nutrition.Totals totals(int day) {
        Nutrition.Totals t = new Nutrition.Totals();
        for (Meal m : mealsOf(day)) {
            Nutrition.Totals x = m.totals();
            t.kcal += x.kcal;
            t.protein += x.protein;
            t.carbs += x.carbs;
            t.fat += x.fat;
        }
        return t;
    }

    /** Même plat que le jour d'avant ou d'après au même créneau (cuisine en lot) : 1 = 1er jour, 2 = 2e jour, 0 = non. */
    public int batchIndex(Meal m) {
        if (!Recipe.MAIN.equals(m.kind())) return 0;
        int first = m.day;
        while (sameAt(Days.add(first, -1), m)) first = Days.add(first, -1);
        int last = m.day;
        while (sameAt(Days.add(last, 1), m)) last = Days.add(last, 1);
        int pos = diff(first, m.day);
        int len = diff(first, last) + 1;
        if (len < 2) return 0;
        if (pos % 2 == 1) return 2;
        return pos + 1 < len ? 1 : 0;
    }

    private boolean sameAt(int day, Meal m) {
        Meal o = find(day, m.slot);
        return o != null && same(o, m);
    }

    private static int diff(int from, int to) {
        int n = 0;
        int d = from;
        while (d < to && n < 400) {
            d = Days.add(d, 1);
            n++;
        }
        return n;
    }

    /** Le repas jumeau d'un plat cuisiné en lot, ou null. */
    public Meal batchPartner(Meal m) {
        int i = batchIndex(m);
        if (i == 1) return find(Days.add(m.day, 1), m.slot);
        if (i == 2) return find(Days.add(m.day, -1), m.slot);
        return null;
    }

    private static boolean same(Meal a, Meal b) {
        return a.recipeId != null && a.recipeId.equals(b.recipeId);
    }

    public Meal find(int day, String slot) {
        for (Meal m : meals) if (m.day == day && m.slot.equals(slot)) return m;
        return null;
    }

    /** « du 3 au 9 novembre », « du 30 novembre au 6 décembre ». */
    public String rangeLabel() {
        long a = Days.start(start);
        long b = Days.start(end());
        SimpleDateFormat month = new SimpleDateFormat("MMMM", Locale.FRENCH);
        SimpleDateFormat dayNum = new SimpleDateFormat("d", Locale.FRENCH);
        String ma = month.format(new Date(a));
        String mb = month.format(new Date(b));
        String da = dayNum.format(new Date(a));
        String db = dayNum.format(new Date(b));
        if (da.equals("1")) da = "1er";
        if (ma.equals(mb)) return "du " + da + " au " + db + " " + mb;
        return "du " + da + " " + ma + " au " + db + " " + mb;
    }
}

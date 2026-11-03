package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Liste de courses calculée à partir du plan : quantités crues ou sèches, regroupées par rayon. */
public final class ShoppingList {
    private ShoppingList() {
    }

    public static final String MINE = "Mes ajouts";

    public static class Line {
        /** Identifiant d'aliment, ou « extra:n » pour un article ajouté à la main. */
        public String key;
        public String name;
        public String amount;
        public String aisle;
        public boolean pantry;
        public boolean checked;
        public int extraIndex = -1;
    }

    public static List<Line> build(MealPlan plan) {
        Map<String, Double> grams = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        for (MealPlan.Meal m : plan.meals) {
            for (MealPlan.Item i : m.items) {
                Double g = grams.get(i.foodId);
                grams.put(i.foodId, (g == null ? 0 : g) + i.grams);
                names.put(i.foodId, i.name);
            }
        }
        List<Line> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : grams.entrySet()) {
            Groceries.Info info = Groceries.info(e.getKey(), names.get(e.getKey()));
            Line l = new Line();
            l.key = e.getKey();
            l.name = info.shopName;
            l.amount = Groceries.shopAmount(e.getKey(), names.get(e.getKey()), e.getValue());
            l.aisle = info.aisle;
            l.pantry = info.pantry;
            l.checked = plan.checked.contains(l.key);
            out.add(l);
        }
        Collections.sort(out, (a, b) -> {
            int x = aisleIndex(a.aisle) - aisleIndex(b.aisle);
            if (x != 0) return x;
            if (a.pantry != b.pantry) return a.pantry ? 1 : -1;
            return a.name.compareTo(b.name);
        });
        for (int i = 0; i < plan.extras.size(); i++) {
            MealPlan.Extra x = plan.extras.get(i);
            Line l = new Line();
            l.key = "extra:" + i;
            l.name = x.name;
            l.amount = "";
            l.aisle = MINE;
            l.checked = x.checked;
            l.extraIndex = i;
            out.add(l);
        }
        return out;
    }

    private static int aisleIndex(String aisle) {
        for (int i = 0; i < Groceries.AISLES.length; i++) if (Groceries.AISLES[i].equals(aisle)) return i;
        return Groceries.AISLES.length;
    }

    public static int checkedCount(List<Line> lines) {
        int n = 0;
        for (Line l : lines) if (l.checked) n++;
        return n;
    }

    /** Texte à partager : seulement ce qu'il reste à acheter. */
    public static String text(MealPlan plan, List<Line> lines) {
        StringBuilder b = new StringBuilder("Liste de courses · semaine " + plan.rangeLabel() + "\n");
        String aisle = null;
        int count = 0;
        for (Line l : lines) {
            if (l.checked) continue;
            if (!l.aisle.equals(aisle)) {
                aisle = l.aisle;
                b.append('\n').append(aisle).append('\n');
            }
            b.append("- ").append(l.name);
            if (!l.amount.isEmpty()) b.append(" : ").append(l.amount);
            b.append('\n');
            count++;
        }
        if (count == 0) b.append("\nTout est coché.\n");
        return b.toString();
    }
}

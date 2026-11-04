package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.List;

/** Portions toutes prêtes pour un aliment : la sienne si elle est connue, sinon une portion courante devinée d'après son nom. */
public final class Portions {
    private Portions() {
    }

    public static class Portion {
        public final String label;
        public final double grams;

        public Portion(String label, double grams) {
            this.label = label;
            this.grams = grams;
        }

        /** « 1 filet · 140 g », « 1 verre · 200 ml », ou « 100 g » sans étiquette. */
        public String text(boolean liquid) {
            String amount = Fmt.kg(grams) + (liquid ? " ml" : " g");
            return label == null ? amount : label + " · " + amount;
        }
    }

    /** {mot-clé au début du nom, étiquette, grammes}. Le premier qui convient l'emporte. */
    private static final String[][] START = {
            {"blanc d oeuf", "1 blanc", "33"},
            {"jaune d oeuf", "1 jaune", "17"},
            {"oeuf", "1 œuf", "50"},
            {"yaourt", "1 pot", "125"},
            {"yogourt", "1 pot", "125"},
            {"skyr", "1 pot", "150"},
            {"petit suisse", "1 pot", "60"},
            {"fromage blanc", "1 bol", "150"},
            {"lait", "1 bol", "250"},
            {"jus", "1 verre", "200"},
            {"nectar", "1 verre", "200"},
            {"soda", "1 canette", "330"},
            {"cola", "1 canette", "330"},
            {"boisson", "1 verre", "250"},
            {"biere", "1 demi", "250"},
            {"vin", "1 verre", "125"},
            {"cafe", "1 tasse", "150"},
            {"the", "1 tasse", "200"},
            {"baguette", "1/4 de baguette", "60"},
            {"pain au chocolat", "1 pain au chocolat", "65"},
            {"pain", "1 tranche", "30"},
            {"biscotte", "1 biscotte", "8"},
            {"croissant", "1 croissant", "60"},
            {"banane", "1 banane", "120"},
            {"pomme de terre", "1 portion", "200"},
            {"pomme", "1 pomme", "150"},
            {"poire", "1 poire", "150"},
            {"orange", "1 orange", "150"},
            {"mangue", "1 mangue", "250"},
            {"kiwi", "1 kiwi", "80"},
            {"clementine", "1 clémentine", "60"},
            {"mandarine", "1 mandarine", "60"},
            {"letchi", "10 letchis", "100"},
            {"litchi", "10 litchis", "100"},
            {"ananas", "1 tranche", "80"},
            {"avocat", "1/2 avocat", "75"},
            {"frite", "1 cornet", "150"},
            {"puree", "1 portion", "200"},
            {"pizza", "1 part", "120"},
            {"sandwich", "1 sandwich", "200"},
            {"hamburger", "1 burger", "200"},
            {"cheeseburger", "1 burger", "200"},
            {"beurre", "1 noisette", "10"},
            {"huile", "1 c. à soupe", "10"},
            {"sucre", "1 morceau", "5"},
            {"miel", "1 c. à soupe", "15"},
            {"confiture", "1 c. à soupe", "15"},
            {"chocolat", "1 carré", "5"},
            {"barre", "1 barre", "40"},
            {"biscuit", "1 biscuit", "10"},
            {"cookie", "1 cookie", "15"},
            {"amande", "1 poignée", "30"},
            {"noix", "1 poignée", "30"},
            {"cacahuete", "1 poignée", "30"},
    };

    /** {mot-clé n'importe où dans le nom, étiquette, grammes}. */
    private static final String[][] ANY = {
            {"steak", "1 steak", "125"},
            {"bifteck", "1 steak", "125"},
            {"escalope", "1 escalope", "140"},
            {"filet", "1 filet", "140"},
            {"pave", "1 pavé", "130"},
            {"cuisse", "1 cuisse", "120"},
            {"cotelette", "1 côtelette", "100"},
            {"saucisse", "1 saucisse", "100"},
            {"jambon", "1 tranche", "40"},
            {"riz", "1 assiette", "200"},
            {"pate", "1 assiette", "200"},
            {"semoule", "1 assiette", "200"},
            {"quinoa", "1 assiette", "200"},
            {"boulgour", "1 assiette", "200"},
            {"lentille", "1 assiette", "200"},
            {"haricot", "1 portion", "150"},
            {"pois chiche", "1 portion", "150"},
            {"grain", "1 portion", "150"},
    };

    private static final String[] LIQUID = {"lait", "jus", "nectar", "soda", "cola", "boisson", "biere", "vin", "cafe", "the ", "eau",
            "sirop", "smoothie"};

    /** Probablement un liquide (quantité en ml). */
    public static boolean liquid(Food f) {
        String n = " " + FoodSearch.norm(f.name) + " ";
        for (String w : LIQUID) if (n.startsWith(" " + w)) return true;
        String c = FoodSearch.norm(f.category);
        return c.contains("boisson") || c.contains("eaux");
    }

    /** Portion courante devinée, ou null. */
    public static Portion guess(Food f) {
        if (f == null || f.name == null) return null;
        String n = FoodSearch.norm(f.name);
        for (String[] r : START) {
            if (n.equals(r[0]) || n.startsWith(r[0] + " ")) return new Portion(r[1], Double.parseDouble(r[2]));
        }
        int comma = f.name.indexOf(',');
        String head = " " + n + " ";
        for (String[] r : ANY) {
            if (head.contains(" " + r[0])) return new Portion(r[1], Double.parseDouble(r[2]));
        }
        String c = FoodSearch.norm(f.category);
        if (c.contains("fromage")) return new Portion("1 portion", 30);
        if (Ciqual.isCiqual(f) && Ciqual.GROUP_DISHES.equals(Ciqual.groupCode(f))) return new Portion("1 assiette", 300);
        if (c.contains("legume")) return new Portion("1 portion", 150);
        if (comma < 0 && c.contains("fruit")) return new Portion("1 fruit", 120);
        return null;
    }

    /**
     * Portions proposées, sans doublon : 100 g, la portion de l'aliment (ou devinée), et « Ma portion »
     * (la dernière quantité notée) si elle est différente.
     */
    public static List<Portion> forFood(Food f, double lastGrams) {
        List<Portion> out = new ArrayList<>();
        out.add(new Portion(null, 100));
        Portion own = null;
        if (f.portion > 0) own = new Portion(f.portionLabel == null || f.portionLabel.isEmpty() ? "1 portion" : f.portionLabel, f.portion);
        else own = guess(f);
        if (own != null && Math.abs(own.grams - 100) > 0.5) out.add(own);
        if (lastGrams > 0 && Math.abs(lastGrams - 100) > 0.5 && (own == null || Math.abs(own.grams - lastGrams) > 0.5)) {
            out.add(new Portion("Ma portion", lastGrams));
        }
        return out;
    }
}

package com.fonte.carnet.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Comment afficher et acheter chaque aliment des recettes : nom court, unité, rayon,
 * et passage du poids cuit (base d'aliments) au poids cru ou sec acheté.
 */
public final class Groceries {
    private Groceries() {
    }

    public static final String MEAT = "Viandes, poissons & œufs";
    public static final String PRODUCE = "Fruits & légumes";
    public static final String GROCERY = "Épicerie";
    public static final String FRESH = "Frais";
    public static final String SUPPLEMENTS = "Compléments";
    public static final String OTHER = "Autres";
    public static final String[] AISLES = {MEAT, PRODUCE, GROCERY, FRESH, SUPPLEMENTS, OTHER};

    static final int G = 0;
    static final int ML = 1;
    static final int PIECE = 2;

    public static final class Info {
        public final String foodId;
        /** Nom dans les recettes. */
        public final String label;
        /** Nom dans la liste de courses. */
        public final String shopName;
        public final String aisle;
        /** Poids acheté = poids cuit × raw. */
        final double raw;
        /** « cru » ou « sec » quand le poids acheté diffère du poids cuit. */
        final String rawWord;
        final int unit;
        /** Poids d'une pièce (œuf, banane, boîte, pot…) ou 0. */
        final double piece;
        final String one;
        final String many;
        /** Afficher la recette en pièces (« 2 œufs ») plutôt qu'en grammes. */
        final boolean countInRecipe;
        /** Souvent déjà au placard (huile, miel…). */
        public final boolean pantry;

        Info(String foodId, String label, String shopName, String aisle, double raw, String rawWord, int unit,
             double piece, String one, String many, boolean countInRecipe, boolean pantry) {
            this.foodId = foodId;
            this.label = label;
            this.shopName = shopName;
            this.aisle = aisle;
            this.raw = raw;
            this.rawWord = rawWord;
            this.unit = unit;
            this.piece = piece;
            this.one = one;
            this.many = many;
            this.countInRecipe = countInRecipe;
            this.pantry = pantry;
        }
    }

    private static Map<String, Info> table;

    private static void put(Map<String, Info> m, Info i) {
        m.put(i.foodId, i);
    }

    private static Info g(String id, String label, String shop, String aisle, double raw, String word) {
        return new Info(id, label, shop, aisle, raw, word, G, 0, null, null, false, false);
    }

    /** Vendu au poids, avec un repère en pièces (« ≈ 5 pavés »). */
    private static Info gp(String id, String label, String shop, String aisle, double raw, String word, double piece, String one, String many) {
        return new Info(id, label, shop, aisle, raw, word, G, piece, one, many, false, false);
    }

    private static Info pc(String id, String label, String shop, String aisle, double piece, String one, String many, boolean countInRecipe) {
        return new Info(id, label, shop, aisle, 1, null, PIECE, piece, one, many, countInRecipe, false);
    }

    private static Info ml(String id, String label, String shop, String aisle) {
        return new Info(id, label, shop, aisle, 1, null, ML, 0, null, null, false, false);
    }

    private static Info pantry(String id, String label, String shop, int unit) {
        return new Info(id, label, shop, GROCERY, 1, null, unit, 0, null, null, false, true);
    }

    private static synchronized Map<String, Info> table() {
        if (table != null) return table;
        Map<String, Info> m = new HashMap<>();
        put(m, g("poulet_blanc", "Blanc de poulet", "Blanc de poulet", MEAT, 1.33, "cru"));
        put(m, g("poulet_cuisse", "Cuisse de poulet", "Cuisses de poulet sans peau", MEAT, 1.3, "cru"));
        put(m, g("dinde", "Escalope de dinde", "Escalopes de dinde", MEAT, 1.3, "cru"));
        put(m, g("steak_5", "Steak haché 5 %", "Steak haché 5 %", MEAT, 1.25, "cru"));
        put(m, g("boeuf", "Bœuf (rumsteck)", "Rumsteck", MEAT, 1.3, "cru"));
        put(m, g("porc_filet", "Filet de porc", "Filet de porc", MEAT, 1.3, "cru"));
        put(m, gp("saumon", "Saumon", "Pavés de saumon", MEAT, 1.2, "cru", 130, "pavé", "pavés"));
        put(m, gp("cabillaud", "Cabillaud", "Filets de cabillaud", MEAT, 1.2, "cru", 150, "filet", "filets"));
        put(m, g("crevettes", "Crevettes", "Crevettes cuites décortiquées", MEAT, 1, null));
        put(m, pc("oeuf", "Œuf", "Œufs", MEAT, 60, "œuf", "œufs", true));
        put(m, g("blanc_oeuf", "Blancs d'œufs", "Blancs d'œufs (brique)", FRESH, 1, null));
        put(m, g("saucisse", "Saucisse fumée", "Saucisses fumées", MEAT, 1, null));
        put(m, g("cari_poulet", "Cari poulet", "Cuisses de poulet (pour le cari)", MEAT, 0.75, null));
        put(m, g("rougail_saucisse", "Rougail saucisse", "Saucisses fumées (pour le rougail)", MEAT, 0.6, null));
        put(m, pc("thon", "Thon au naturel", "Thon au naturel", GROCERY, 100, "boîte", "boîtes", false));
        put(m, g("tofu", "Tofu ferme", "Tofu ferme", FRESH, 1, null));

        put(m, g("riz_blanc", "Riz", "Riz basmati", GROCERY, 1 / 2.6, "sec"));
        put(m, g("riz_complet", "Riz complet", "Riz complet", GROCERY, 1 / 2.5, "sec"));
        put(m, g("pates", "Pâtes", "Pâtes", GROCERY, 1 / 2.4, "sec"));
        put(m, g("pates_completes", "Pâtes complètes", "Pâtes complètes", GROCERY, 1 / 2.3, "sec"));
        put(m, g("quinoa", "Quinoa", "Quinoa", GROCERY, 1 / 2.7, "sec"));
        put(m, g("semoule", "Semoule", "Semoule", GROCERY, 1 / 2.5, "sec"));
        put(m, g("lentilles", "Lentilles", "Lentilles", GROCERY, 1 / 2.5, "sec"));
        put(m, g("grains", "Grains", "Grains secs (lentilles ou haricots)", GROCERY, 0.4, "sec"));
        put(m, g("pois_chiches", "Pois chiches", "Pois chiches (conserve, égouttés)", GROCERY, 1, null));
        put(m, g("haricots_rouges", "Haricots rouges", "Haricots rouges (conserve, égouttés)", GROCERY, 1, null));
        put(m, g("mais", "Maïs doux", "Maïs doux (conserve)", GROCERY, 1, null));
        put(m, g("avoine", "Avoine", "Flocons d'avoine", GROCERY, 1, null));
        put(m, g("muesli", "Muesli", "Muesli", GROCERY, 1, null));
        put(m, g("pain_complet", "Pain complet", "Pain complet", GROCERY, 1, null));
        put(m, pc("galette_riz", "Galettes de riz", "Galettes de riz soufflé", GROCERY, 9, "galette", "galettes", true));
        put(m, g("amandes", "Amandes", "Amandes", GROCERY, 1, null));
        put(m, g("cajou", "Noix de cajou", "Noix de cajou", GROCERY, 1, null));
        put(m, g("noix", "Noix", "Cerneaux de noix", GROCERY, 1, null));
        put(m, g("dattes", "Dattes", "Dattes", GROCERY, 1, null));
        put(m, g("beurre_cacahuete", "Beurre de cacahuète", "Beurre de cacahuète", GROCERY, 1, null));
        put(m, ml("lait_amande", "Boisson à l'amande", "Boisson à l'amande sans sucre", GROCERY));
        put(m, pantry("huile", "Huile d'olive", "Huile d'olive", ML));
        put(m, pantry("miel", "Miel", "Miel", G));

        put(m, g("brocoli", "Brocolis", "Brocolis", PRODUCE, 1, null));
        put(m, g("haricots_verts", "Haricots verts", "Haricots verts", PRODUCE, 1, null));
        put(m, g("courgette", "Courgette", "Courgettes", PRODUCE, 1.1, null));
        put(m, g("epinards", "Épinards", "Épinards surgelés", PRODUCE, 1, null));
        put(m, g("chouchou", "Chouchou", "Chouchous", PRODUCE, 1.1, null));
        put(m, g("pdt", "Pommes de terre", "Pommes de terre", PRODUCE, 1.05, null));
        put(m, g("patate_douce", "Patate douce", "Patates douces", PRODUCE, 1.1, null));
        put(m, pc("tomate", "Tomate", "Tomates", PRODUCE, 120, "tomate", "tomates", false));
        put(m, pc("carotte", "Carotte", "Carottes", PRODUCE, 80, "carotte", "carottes", false));
        put(m, g("salade", "Salade verte", "Salade verte", PRODUCE, 1, null));
        put(m, pc("concombre", "Concombre", "Concombres", PRODUCE, 300, "concombre", "concombres", false));
        put(m, pc("avocat", "Avocat", "Avocats", PRODUCE, 150, "avocat", "avocats", false));
        put(m, pc("banane", "Banane", "Bananes", PRODUCE, 120, "banane", "bananes", true));
        put(m, pc("pomme", "Pomme", "Pommes", PRODUCE, 150, "pomme", "pommes", true));
        put(m, pc("orange", "Orange", "Oranges", PRODUCE, 150, "orange", "oranges", true));
        put(m, pc("mangue", "Mangue", "Mangues", PRODUCE, 250, "mangue", "mangues", false));
        put(m, pc("ananas", "Ananas", "Ananas", PRODUCE, 800, "ananas", "ananas", false));
        put(m, g("fraises", "Fraises", "Fraises", PRODUCE, 1, null));

        put(m, ml("lait_demi", "Lait demi-écrémé", "Lait demi-écrémé", FRESH));
        put(m, ml("lait_entier", "Lait entier", "Lait entier", FRESH));
        put(m, gp("skyr", "Skyr nature", "Skyr nature", FRESH, 1, null, 150, "pot de 150 g", "pots de 150 g"));
        put(m, pc("yaourt", "Yaourt nature", "Yaourts nature", FRESH, 125, "pot", "pots", true));
        put(m, g("fromage_blanc_0", "Fromage blanc 0 %", "Fromage blanc 0 %", FRESH, 1, null));
        put(m, g("fromage_blanc_3", "Fromage blanc 3 %", "Fromage blanc 3 %", FRESH, 1, null));
        put(m, g("emmental", "Emmental râpé", "Emmental râpé", FRESH, 1, null));

        put(m, gp("whey", "Whey", "Whey", SUPPLEMENTS, 1, null, 30, "dose", "doses"));
        put(m, pc("barre_proteinee", "Barre protéinée", "Barres protéinées", SUPPLEMENTS, 60, "barre", "barres", true));
        table = m;
        return m;
    }

    /** Informations d'un aliment (une fiche générique s'il n'est pas répertorié). */
    public static Info info(String foodId, String fallbackName) {
        Info i = table().get(foodId);
        if (i != null) return i;
        String n = fallbackName == null ? foodId : fallbackName;
        return new Info(foodId, n, n, OTHER, 1, null, G, 0, null, null, false, false);
    }

    private static Map<String, Double> caps;

    /** Quantité maximale raisonnable dans un seul repas (g), ou 0 si pas de limite. */
    public static synchronized double max(String foodId) {
        if (caps == null) {
            Map<String, Double> c = new HashMap<>();
            String[] ids = {"fromage_blanc_0", "fromage_blanc_3", "skyr", "yaourt", "oeuf", "blanc_oeuf", "whey",
                    "barre_proteinee", "thon", "lait_demi", "lait_entier", "lait_amande", "avoine", "muesli", "pain_complet",
                    "galette_riz", "banane", "dattes", "miel", "huile", "amandes", "cajou", "noix", "beurre_cacahuete",
                    "avocat", "emmental", "tofu", "mangue", "ananas", "fraises", "orange", "pomme", "cari_poulet",
                    "rougail_saucisse", "saucisse", "riz_blanc", "riz_complet", "pates", "pates_completes", "quinoa",
                    "semoule", "pdt", "patate_douce", "lentilles", "pois_chiches", "haricots_rouges", "grains", "mais"};
            double[] g = {300, 300, 300, 250, 240, 200, 45,
                    60, 160, 400, 400, 400, 100, 90, 120,
                    45, 240, 60, 20, 15, 30, 30, 30, 30,
                    100, 30, 300, 250, 250, 250, 300, 300, 350,
                    280, 150, 400, 400, 400, 400, 350,
                    350, 500, 450, 300, 250, 250, 200, 150};
            for (int i = 0; i < ids.length; i++) c.put(ids[i], g[i]);
            caps = c;
        }
        Double v = caps.get(foodId);
        return v == null ? 0 : v;
    }

    /** Pas d'arrondi des quantités d'une recette (œuf entier, demi-banane, 5 g…). */
    public static double step(String foodId, double grams) {
        Info i = table().get(foodId);
        if (i != null && i.countInRecipe && i.piece > 0) {
            return "banane".equals(foodId) ? i.piece / 2 : i.piece;
        }
        if ("whey".equals(foodId)) return 5;
        return grams < 100 ? 5 : 10;
    }

    // ---------------------------------------------------------------- affichage dans les recettes

    private static String count(double n, String one, String many) {
        double halves = Math.round(n * 2) / 2.0;
        if (halves <= 0) halves = 0.5;
        long whole = (long) Math.floor(halves);
        boolean half = halves - whole > 0.01;
        if (whole == 0) return "1/2 " + one;
        return whole + " " + (whole > 1 ? many : one) + (half ? " 1/2" : "");
    }

    /** « 160 g », « 250 ml », « 2 œufs », « 1 banane 1/2 ». */
    public static String amount(String foodId, double grams) {
        Info i = info(foodId, null);
        if (i.countInRecipe && i.piece > 0) return count(grams / i.piece, i.one, i.many);
        if (i.unit == ML && !i.pantry) return Fmt.kg(Math.round(grams)) + " ml";
        return Fmt.kg(Math.round(grams)) + " g";
    }

    /** « Riz 220 g » ou « 2 œufs ». */
    public static String line(String foodId, String fallbackName, double grams) {
        Info i = info(foodId, fallbackName);
        if (i.countInRecipe && i.piece > 0) return amount(foodId, grams);
        return i.label + " " + amount(foodId, grams);
    }

    /** « ≈ 85 g sec » pour les féculents, « ≈ 215 g cru » pour les viandes, ou null. */
    public static String rawHint(String foodId, double grams) {
        Info i = table().get(foodId);
        if (i == null || i.rawWord == null || Math.abs(i.raw - 1) < 0.02) return null;
        return "≈ " + Fmt.kg(roundTo(grams * i.raw, 5)) + " g " + i.rawWord;
    }

    /** « cuit » quand la quantité de la recette est un poids cuit. */
    public static boolean cooked(String foodId) {
        Info i = table().get(foodId);
        return i != null && i.rawWord != null && Math.abs(i.raw - 1) >= 0.02;
    }

    // ---------------------------------------------------------------- liste de courses

    static double roundTo(double v, double step) {
        return Math.max(step, Math.round(v / step) * step);
    }

    /** Quantité à acheter pour un total de recettes (poids cuit) : « 1,5 kg », « 12 », « 3 boîtes », « 4 L ». */
    public static String shopAmount(String foodId, String fallbackName, double recipeGrams) {
        Info i = info(foodId, fallbackName);
        double g = recipeGrams * i.raw;
        if (i.unit == PIECE && i.piece > 0) {
            long n = Math.max(1, (long) Math.ceil(g / i.piece - 0.15));
            // Le nom dit déjà ce qu'on compte (« Œufs 12 ») ; on précise seulement les contenants.
            boolean container = "boîte".equals(i.one) || "pot".equals(i.one);
            return container ? n + " " + (n > 1 ? i.many : i.one) : String.valueOf(n);
        }
        if (i.unit == ML) {
            double ml = i.pantry ? g / 0.92 : g;
            if (ml >= 900) return Fmt.kg(Math.ceil(ml / 500.0) * 0.5) + " L";
            return "≈ " + Fmt.kg(roundTo(ml, 50)) + " ml";
        }
        String w;
        double mid = i.piece > 0 && i.piece <= 50 ? 10 : 50;
        if (g < 100) w = Fmt.kg(roundTo(g, 10)) + " g";
        else if (roundTo(g, mid) < 1000) w = Fmt.kg(roundTo(g, mid)) + " g";
        else w = Fmt.kg(roundTo(g, 100) / 1000.0) + " kg";
        if (i.piece > 0 && i.many != null) {
            long n = Math.max(1, Math.round(g / i.piece));
            w += " · " + (i.foodId.equals("whey") ? "" : "≈ ") + n + " " + (n > 1 ? i.many : i.one);
        }
        return w;
    }
}

package com.fonte.carnet.core;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Base d'aliments courants. Valeurs moyennes indicatives pour 100 g (aliments cuits quand c'est précisé),
 * d'après les tables de composition usuelles. Pour un produit précis, le code-barres ou l'étiquette font foi.
 */
public final class Foods {
    private Foods() {
    }

    public static final String PROTEINS = "Viandes, poissons, œufs";
    public static final String DAIRY = "Produits laitiers";
    public static final String STARCH = "Féculents & céréales";
    public static final String VEG = "Légumes";
    public static final String FRUIT = "Fruits";
    public static final String FAT = "Matières grasses & oléagineux";
    public static final String DISHES = "Plats";
    public static final String DRINKS = "Boissons";
    public static final String SWEET = "Sucré & encas";
    public static final String MINE = "Mes aliments";

    public static final String[] CATEGORIES = {PROTEINS, DAIRY, STARCH, VEG, FRUIT, FAT, DISHES, DRINKS, SWEET, MINE};

    private static List<Food> cache;

    private static Food f(String id, String name, String cat, double kcal, double p, double c, double l, double portion, String label) {
        return new Food(id, name, cat, kcal, p, c, l, portion, label);
    }

    public static synchronized List<Food> all() {
        if (cache != null) return cache;
        List<Food> l = new ArrayList<>();
        // Viandes, poissons, œufs (cuits)
        l.add(f("poulet_blanc", "Blanc de poulet cuit", PROTEINS, 165, 31, 0, 3.6, 120, "1 filet"));
        l.add(f("poulet_cuisse", "Cuisse de poulet cuite, sans peau", PROTEINS, 209, 26, 0, 10.9, 130, "1 cuisse"));
        l.add(f("dinde", "Escalope de dinde cuite", PROTEINS, 135, 29, 0, 1.7, 120, "1 escalope"));
        l.add(f("steak_5", "Steak haché 5 % cuit", PROTEINS, 170, 26, 0, 7, 100, "1 steak"));
        l.add(f("steak_15", "Steak haché 15 % cuit", PROTEINS, 250, 25, 0, 17, 100, "1 steak"));
        l.add(f("boeuf", "Bœuf (rumsteck) cuit", PROTEINS, 180, 28, 0, 7, 150, "1 pièce"));
        l.add(f("porc_filet", "Filet de porc cuit", PROTEINS, 143, 26, 0, 3.5, 130, "1 portion"));
        l.add(f("jambon", "Jambon blanc", PROTEINS, 115, 20, 1, 3.5, 40, "1 tranche"));
        l.add(f("saucisse", "Saucisse fumée cuite", PROTEINS, 310, 13, 1, 28, 70, "1 saucisse"));
        l.add(f("saumon", "Saumon cuit", PROTEINS, 206, 22, 0, 13, 120, "1 pavé"));
        l.add(f("thon", "Thon au naturel (boîte)", PROTEINS, 116, 26, 0, 1, 100, "1 petite boîte égouttée"));
        l.add(f("cabillaud", "Cabillaud cuit", PROTEINS, 105, 23, 0, 0.9, 130, "1 filet"));
        l.add(f("crevettes", "Crevettes cuites", PROTEINS, 99, 24, 0.2, 0.3, 100, "1 portion"));
        l.add(f("oeuf", "Œuf entier", PROTEINS, 143, 12.6, 0.7, 9.5, 60, "1 œuf"));
        l.add(f("blanc_oeuf", "Blanc d'œuf", PROTEINS, 52, 11, 0.7, 0.2, 33, "1 blanc"));
        l.add(f("tofu", "Tofu ferme", PROTEINS, 144, 17.3, 2.8, 8.7, 100, "1 portion"));
        l.add(f("whey", "Protéine en poudre (whey)", PROTEINS, 380, 78, 6, 5, 30, "1 dose"));
        // Produits laitiers
        l.add(f("lait_demi", "Lait demi-écrémé", DAIRY, 46, 3.3, 4.8, 1.6, 250, "1 verre"));
        l.add(f("lait_entier", "Lait entier", DAIRY, 64, 3.3, 4.8, 3.6, 250, "1 verre"));
        l.add(f("yaourt", "Yaourt nature", DAIRY, 60, 4, 5, 3, 125, "1 pot"));
        l.add(f("skyr", "Skyr nature 0 %", DAIRY, 60, 11, 4, 0.2, 150, "1 pot"));
        l.add(f("fromage_blanc_0", "Fromage blanc 0 %", DAIRY, 47, 7.5, 4, 0.2, 100, "1 portion"));
        l.add(f("fromage_blanc_3", "Fromage blanc 3 %", DAIRY, 77, 7, 3.8, 3.2, 100, "1 portion"));
        l.add(f("emmental", "Emmental râpé", DAIRY, 380, 28, 0.5, 29.5, 20, "1 poignée"));
        l.add(f("mozzarella", "Mozzarella", DAIRY, 254, 18, 2.2, 19.5, 125, "1 boule"));
        l.add(f("camembert", "Camembert", DAIRY, 300, 20, 0.5, 24, 30, "1 part"));
        // Féculents et céréales (cuits sauf mention)
        l.add(f("riz_blanc", "Riz blanc cuit", STARCH, 130, 2.7, 28, 0.3, 200, "1 assiette"));
        l.add(f("riz_complet", "Riz complet cuit", STARCH, 123, 2.7, 25.6, 1, 200, "1 assiette"));
        l.add(f("pates", "Pâtes cuites", STARCH, 150, 5.5, 30, 0.9, 200, "1 assiette"));
        l.add(f("pates_completes", "Pâtes complètes cuites", STARCH, 124, 5.3, 26.5, 0.5, 200, "1 assiette"));
        l.add(f("quinoa", "Quinoa cuit", STARCH, 120, 4.4, 21.3, 1.9, 150, "1 portion"));
        l.add(f("semoule", "Semoule cuite", STARCH, 112, 3.8, 23, 0.2, 150, "1 portion"));
        l.add(f("pdt", "Pomme de terre cuite", STARCH, 87, 1.9, 20, 0.1, 200, "2 moyennes"));
        l.add(f("patate_douce", "Patate douce cuite", STARCH, 90, 2, 20.7, 0.2, 200, "1 moyenne"));
        l.add(f("frites", "Frites", STARCH, 312, 3.4, 41, 15, 120, "1 portion"));
        l.add(f("baguette", "Pain baguette", STARCH, 270, 9, 56, 1.3, 50, "1/5 de baguette"));
        l.add(f("pain_complet", "Pain complet", STARCH, 247, 13, 41, 3.4, 35, "1 tranche"));
        l.add(f("pain_mie", "Pain de mie", STARCH, 266, 8.5, 49, 3.5, 25, "1 tranche"));
        l.add(f("avoine", "Flocons d'avoine (crus)", STARCH, 389, 16.9, 66, 6.9, 50, "1 bol"));
        l.add(f("muesli", "Muesli", STARCH, 370, 10, 66, 6, 50, "1 bol"));
        l.add(f("galette_riz", "Galette de riz soufflé", STARCH, 387, 8, 81, 2.8, 9, "1 galette"));
        l.add(f("lentilles", "Lentilles cuites", STARCH, 116, 9, 20, 0.4, 200, "1 assiette"));
        l.add(f("pois_chiches", "Pois chiches cuits", STARCH, 164, 8.9, 27.4, 2.6, 150, "1 portion"));
        l.add(f("haricots_rouges", "Haricots rouges cuits", STARCH, 127, 8.7, 22.8, 0.5, 150, "1 portion"));
        l.add(f("grains", "Grains créoles (lentilles, haricots cuisinés)", STARCH, 130, 8, 18, 3, 150, "1 louche"));
        l.add(f("mais", "Maïs doux (boîte)", STARCH, 86, 3.2, 19, 1.2, 100, "1 portion"));
        // Légumes
        l.add(f("brocoli", "Brocoli cuit", VEG, 35, 2.4, 7.2, 0.4, 150, "1 portion"));
        l.add(f("haricots_verts", "Haricots verts cuits", VEG, 35, 1.9, 7.9, 0.3, 150, "1 portion"));
        l.add(f("courgette", "Courgette cuite", VEG, 17, 1.2, 3, 0.3, 150, "1 portion"));
        l.add(f("epinards", "Épinards cuits", VEG, 23, 3, 3.8, 0.3, 150, "1 portion"));
        l.add(f("chouchou", "Chouchou (christophine) cuit", VEG, 24, 0.6, 5.1, 0.5, 150, "1 portion"));
        l.add(f("tomate", "Tomate", VEG, 18, 0.9, 3.9, 0.2, 120, "1 tomate"));
        l.add(f("carotte", "Carotte", VEG, 41, 0.9, 9.6, 0.2, 80, "1 carotte"));
        l.add(f("salade", "Salade verte", VEG, 15, 1.4, 2.9, 0.2, 50, "1 bol"));
        l.add(f("concombre", "Concombre", VEG, 15, 0.7, 3.6, 0.1, 100, "1/3 de concombre"));
        l.add(f("avocat", "Avocat", VEG, 160, 2, 8.5, 14.7, 100, "1/2 avocat"));
        // Fruits
        l.add(f("banane", "Banane", FRUIT, 89, 1.1, 22.8, 0.3, 120, "1 banane"));
        l.add(f("pomme", "Pomme", FRUIT, 52, 0.3, 13.8, 0.2, 150, "1 pomme"));
        l.add(f("orange", "Orange", FRUIT, 47, 0.9, 11.8, 0.1, 150, "1 orange"));
        l.add(f("mangue", "Mangue", FRUIT, 60, 0.8, 15, 0.4, 200, "1/2 mangue"));
        l.add(f("ananas", "Ananas", FRUIT, 50, 0.5, 13.1, 0.1, 150, "2 tranches"));
        l.add(f("letchis", "Letchis", FRUIT, 66, 0.8, 16.5, 0.4, 100, "10 letchis"));
        l.add(f("fraises", "Fraises", FRUIT, 32, 0.7, 7.7, 0.3, 150, "1 barquette"));
        l.add(f("raisin", "Raisin", FRUIT, 69, 0.7, 18, 0.2, 100, "1 grappe"));
        l.add(f("dattes", "Dattes séchées", FRUIT, 282, 2.5, 75, 0.4, 24, "3 dattes"));
        // Matières grasses et oléagineux
        l.add(f("huile", "Huile d'olive ou de colza", FAT, 884, 0, 0, 100, 10, "1 cuillère à soupe"));
        l.add(f("beurre", "Beurre", FAT, 717, 0.9, 0.1, 81, 10, "1 noisette"));
        l.add(f("beurre_cacahuete", "Beurre de cacahuète", FAT, 588, 25, 20, 50, 15, "1 cuillère à soupe"));
        l.add(f("amandes", "Amandes", FAT, 579, 21, 21.6, 49.9, 30, "1 poignée"));
        l.add(f("cajou", "Noix de cajou", FAT, 553, 18, 30, 44, 30, "1 poignée"));
        l.add(f("noix", "Noix", FAT, 654, 15, 14, 65, 30, "1 poignée"));
        // Plats (estimations moyennes)
        l.add(f("cari_poulet", "Cari poulet (sans riz)", DISHES, 150, 15, 3, 9, 250, "1 louche"));
        l.add(f("rougail_saucisse", "Rougail saucisse (sans riz)", DISHES, 230, 11, 4, 19, 250, "1 louche"));
        l.add(f("samoussa", "Samoussa", DISHES, 260, 7, 26, 14, 40, "1 samoussa"));
        l.add(f("bouchon", "Bouchon vapeur", DISHES, 230, 12, 20, 11, 30, "1 bouchon"));
        l.add(f("riz_cantonais", "Riz cantonais", DISHES, 165, 5, 25, 5, 300, "1 assiette"));
        l.add(f("pizza", "Pizza margherita", DISHES, 266, 11.4, 33, 10, 300, "1 pizza individuelle"));
        l.add(f("burger", "Hamburger", DISHES, 254, 13, 30, 9.5, 200, "1 burger"));
        l.add(f("sandwich", "Sandwich jambon-beurre", DISHES, 260, 11, 32, 9, 200, "1 sandwich"));
        l.add(f("omelette", "Omelette nature", DISHES, 154, 11, 0.7, 12, 120, "2 œufs"));
        l.add(f("salade_composee", "Salade composée (poulet, crudités)", DISHES, 110, 9, 6, 6, 300, "1 bol"));
        // Boissons
        l.add(f("jus_orange", "Jus d'orange", DRINKS, 45, 0.7, 10.4, 0.2, 250, "1 verre"));
        l.add(f("soda", "Soda type cola", DRINKS, 42, 0, 10.6, 0, 330, "1 canette"));
        l.add(f("biere", "Bière blonde", DRINKS, 43, 0.5, 3.6, 0, 330, "1 bouteille"));
        l.add(f("vin", "Vin rouge", DRINKS, 85, 0.1, 2.6, 0, 125, "1 verre"));
        l.add(f("cafe", "Café noir", DRINKS, 2, 0.3, 0, 0, 100, "1 tasse"));
        l.add(f("lait_amande", "Boisson à l'amande sans sucre", DRINKS, 17, 0.6, 0.7, 1.2, 250, "1 verre"));
        // Sucré et encas
        l.add(f("sucre", "Sucre", SWEET, 400, 0, 100, 0, 5, "1 morceau"));
        l.add(f("miel", "Miel", SWEET, 304, 0.3, 82, 0, 15, "1 cuillère"));
        l.add(f("confiture", "Confiture", SWEET, 278, 0.4, 69, 0.1, 20, "1 cuillère"));
        l.add(f("chocolat_noir", "Chocolat noir 70 %", SWEET, 598, 7.8, 45.9, 42.6, 20, "2 carrés"));
        l.add(f("barre_proteinee", "Barre protéinée", SWEET, 360, 30, 35, 12, 60, "1 barre"));
        l.add(f("biscuits", "Biscuits secs", SWEET, 450, 7, 70, 15, 25, "3 biscuits"));
        cache = Collections.unmodifiableList(l);
        return cache;
    }

    public static Food find(String id) {
        for (Food f : all()) if (f.id.equals(id)) return f;
        return null;
    }

    public static String norm(String s) {
        String n = Normalizer.normalize(s.toLowerCase(Locale.FRENCH), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").replace("œ", "oe").trim();
    }

    /** Recherche par mots (tous les mots doivent apparaître). */
    public static List<Food> search(List<Food> foods, String query) {
        String q = norm(query == null ? "" : query);
        List<Food> out = new ArrayList<>();
        String[] words = q.isEmpty() ? new String[0] : q.split("\\s+");
        for (Food f : foods) {
            String n = norm(f.name);
            boolean ok = true;
            for (String w : words) if (!n.contains(w)) ok = false;
            if (ok) out.add(f);
        }
        return out;
    }
}

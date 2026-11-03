package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.fonte.carnet.core.Recipe.BREAKFAST;
import static com.fonte.carnet.core.Recipe.BUDGET;
import static com.fonte.carnet.core.Recipe.CREOLE;
import static com.fonte.carnet.core.Recipe.FISH;
import static com.fonte.carnet.core.Recipe.FIXED;
import static com.fonte.carnet.core.Recipe.LACTOSE;
import static com.fonte.carnet.core.Recipe.MAIN;
import static com.fonte.carnet.core.Recipe.MEAT;
import static com.fonte.carnet.core.Recipe.PORK;
import static com.fonte.carnet.core.Recipe.PRICEY;
import static com.fonte.carnet.core.Recipe.PROT;
import static com.fonte.carnet.core.Recipe.REST;
import static com.fonte.carnet.core.Recipe.SHAKER;
import static com.fonte.carnet.core.Recipe.SNACK;
import static com.fonte.carnet.core.Recipe.TREAT;

/**
 * Recettes intégrées. Les quantités sont celles d'une portion « type » (aliments cuits quand la base
 * d'aliments les donne cuits) : le générateur les ajuste ensuite à tes objectifs.
 */
public final class Recipes {
    private Recipes() {
    }

    private static List<Recipe> cache;

    private static Recipe.Ing p(String id, double g) {
        return new Recipe.Ing(id, g, PROT);
    }

    private static Recipe.Ing r(String id, double g) {
        return new Recipe.Ing(id, g, REST);
    }

    private static Recipe.Ing f(String id, double g) {
        return new Recipe.Ing(id, g, Recipe.FAT);
    }

    private static Recipe.Ing v(String id, double g) {
        return new Recipe.Ing(id, g, FIXED);
    }

    private static String[] tags(String... t) {
        return t;
    }

    private static String[] steps(String... s) {
        return s;
    }

    private static Recipe.Ing[] ing(Recipe.Ing... i) {
        return i;
    }

    private static Recipe rec(String id, String kind, String name, int min, String[] tags, Recipe.Ing[] ing, String[] steps) {
        return new Recipe(id, kind, name, min, tags, ing, steps);
    }

    public static synchronized List<Recipe> all() {
        if (cache != null) return cache;
        List<Recipe> l = new ArrayList<>();

        // ------------------------------------------------------------ petits-déjeuners
        l.add(rec("pdj_porridge", BREAKFAST, "Porridge banane & whey", 10, tags(LACTOSE, BUDGET),
                ing(r("avoine", 70), r("lait_demi", 250), r("banane", 120), p("whey", 30)),
                steps("Faire chauffer l'avoine dans le lait 4 à 5 min en remuant.",
                        "Hors du feu, ajouter la whey et bien mélanger.",
                        "Couper la banane en rondelles par-dessus.")));
        l.add(rec("pdj_porridge_amande", BREAKFAST, "Porridge amande, banane & whey", 10, tags(BUDGET),
                ing(r("avoine", 70), r("lait_amande", 250), r("banane", 120), f("amandes", 10), p("whey", 30)),
                steps("Faire chauffer l'avoine dans la boisson à l'amande 4 à 5 min.",
                        "Hors du feu, ajouter la whey et mélanger.",
                        "Ajouter la banane en rondelles et les amandes concassées.")));
        l.add(rec("pdj_omelette", BREAKFAST, "Omelette, pain complet & orange", 10, tags(BUDGET),
                ing(p("oeuf", 120), p("blanc_oeuf", 100), r("pain_complet", 70), r("orange", 150)),
                steps("Battre les œufs entiers et les blancs avec sel et poivre.",
                        "Cuire l'omelette 2 à 3 min dans une poêle antiadhésive.",
                        "Servir avec le pain grillé et l'orange.")));
        l.add(rec("pdj_tartines_avocat", BREAKFAST, "Tartines œufs & avocat", 10, tags(),
                ing(r("pain_complet", 80), p("oeuf", 120), f("avocat", 60), v("tomate", 100)),
                steps("Cuire les œufs 9 min pour des œufs durs, ou au plat.",
                        "Écraser l'avocat sur le pain grillé.",
                        "Ajouter les œufs en tranches et la tomate.")));
        l.add(rec("pdj_fromage_blanc", BREAKFAST, "Fromage blanc, muesli & fraises", 5, tags(LACTOSE),
                ing(p("fromage_blanc_0", 250), r("muesli", 60), r("fraises", 150), r("miel", 10)),
                steps("Verser le fromage blanc dans un bol.",
                        "Ajouter le muesli, les fraises coupées et le miel.")));
        l.add(rec("pdj_skyr_bowl", BREAKFAST, "Bol skyr, avoine & cacahuète", 5, tags(LACTOSE),
                ing(p("skyr", 200), p("whey", 15), r("avoine", 50), r("banane", 120), f("beurre_cacahuete", 15)),
                steps("Mélanger le skyr, la whey et les flocons d'avoine.",
                        "Ajouter la banane en rondelles.",
                        "Finir avec le beurre de cacahuète.")));
        l.add(rec("pdj_pancakes", BREAKFAST, "Pancakes protéinés à la banane", 15, tags(BUDGET),
                ing(r("avoine", 60), p("oeuf", 120), p("whey", 20), r("banane", 120), r("miel", 10)),
                steps("Mixer l'avoine, les œufs, la whey et la moitié de la banane.",
                        "Cuire de petites crêpes 1 à 2 min de chaque côté à la poêle.",
                        "Servir avec le reste de banane et le miel.")));
        l.add(rec("pdj_mangue", BREAKFAST, "Bol mangue, fromage blanc & cajou", 5, tags(LACTOSE, CREOLE),
                ing(p("fromage_blanc_3", 200), p("whey", 15), r("mangue", 200), r("avoine", 30), f("cajou", 15)),
                steps("Couper la mangue en dés.",
                        "Mélanger le fromage blanc, la whey et l'avoine, ajouter la mangue.",
                        "Parsemer de noix de cajou.")));

        // ------------------------------------------------------------ collations
        l.add(rec("col_skyr_fraises", SNACK, "Skyr, fraises & amandes", 3, tags(LACTOSE),
                ing(p("skyr", 150), r("fraises", 150), f("amandes", 15)),
                steps("Verser le skyr, ajouter les fraises coupées et les amandes.")));
        l.add(rec("col_fromage_blanc_miel", SNACK, "Fromage blanc, miel & noix", 3, tags(LACTOSE, BUDGET),
                ing(p("fromage_blanc_0", 200), r("miel", 15), f("noix", 15)),
                steps("Mélanger le fromage blanc et le miel, ajouter les noix.")));
        l.add(rec("col_oeufs_crudites", SNACK, "Œufs durs & crudités", 12, tags(BUDGET),
                ing(p("oeuf", 120), v("carotte", 80), v("concombre", 100), r("pain_complet", 35)),
                steps("Cuire les œufs 9 min dans l'eau bouillante, puis les refroidir.",
                        "Couper la carotte et le concombre en bâtonnets.",
                        "Se prépare la veille et se garde 3 jours au frais.")));
        l.add(rec("col_barre_fruit", SNACK, "Barre protéinée & orange", 1, tags(),
                ing(p("barre_proteinee", 60), r("orange", 150)),
                steps("Pratique à emporter à la salle ou au travail.")));
        l.add(rec("col_tartine_thon", SNACK, "Tartine au thon & tomate", 5, tags(FISH, BUDGET),
                ing(r("pain_complet", 70), p("thon", 80), v("tomate", 100)),
                steps("Égoutter le thon et l'écraser avec un peu de poivre et de citron.",
                        "Tartiner sur le pain et ajouter la tomate en tranches.")));
        l.add(rec("col_ananas", SNACK, "Ananas & fromage blanc", 5, tags(LACTOSE, CREOLE),
                ing(p("fromage_blanc_0", 150), r("ananas", 150), f("amandes", 10)),
                steps("Couper l'ananas en dés.", "Servir sur le fromage blanc avec les amandes.")));
        l.add(rec("col_yaourt_banane", SNACK, "Yaourt, banane & cajou", 2, tags(LACTOSE, BUDGET),
                ing(p("yaourt", 125), r("banane", 120), f("cajou", 15)),
                steps("Servir le yaourt avec la banane coupée et les noix de cajou.")));
        l.add(rec("col_galettes", SNACK, "Galettes de riz, cacahuète & banane", 2, tags(BUDGET),
                ing(r("galette_riz", 27), f("beurre_cacahuete", 15), r("banane", 120)),
                steps("Tartiner les galettes de beurre de cacahuète.", "Ajouter la banane en rondelles.")));
        l.add(rec("col_dattes", SNACK, "Dattes & amandes avant la séance", 1, tags(),
                ing(r("dattes", 32), f("amandes", 20)),
                steps("À prendre 30 à 60 min avant l'entraînement.")));

        // ------------------------------------------------------------ shakers
        l.add(rec("sh_recup", SHAKER, "Shaker récup", 2, tags(LACTOSE, BUDGET),
                ing(p("whey", 30), r("lait_demi", 300), r("avoine", 30)),
                steps("Mixer ou secouer le tout 30 secondes.", "À boire dans l'heure qui suit la séance.")));
        l.add(rec("sh_banane_cacahuete", SHAKER, "Shaker banane & cacahuète", 3, tags(LACTOSE),
                ing(p("whey", 30), r("lait_demi", 250), r("banane", 120), f("beurre_cacahuete", 15)),
                steps("Mixer tous les ingrédients jusqu'à ce que ce soit lisse.")));
        l.add(rec("sh_fruits_rouges", SHAKER, "Shaker fruits rouges", 3, tags(LACTOSE),
                ing(p("whey", 30), r("lait_demi", 250), r("fraises", 150), r("avoine", 20)),
                steps("Mixer les fraises avec le lait, la whey et l'avoine.")));
        l.add(rec("sh_gainer", SHAKER, "Gainer maison", 3, tags(LACTOSE, BUDGET),
                ing(p("whey", 40), r("lait_entier", 300), r("avoine", 60), r("miel", 15)),
                steps("Mixer l'avoine en poudre, puis ajouter le lait, la whey et le miel.",
                        "Idéal les jours où l'appétit manque.")));
        l.add(rec("sh_amande", SHAKER, "Shaker léger amande & banane", 2, tags(),
                ing(p("whey", 30), r("lait_amande", 300), r("banane", 120)),
                steps("Mixer ou secouer le tout 30 secondes.")));
        l.add(rec("sh_mangue", SHAKER, "Shaker mangue", 3, tags(CREOLE),
                ing(p("whey", 30), r("lait_amande", 250), r("mangue", 150), r("avoine", 20)),
                steps("Mixer la mangue avec la boisson à l'amande, la whey et l'avoine.")));

        // ------------------------------------------------------------ plats (déjeuner et dîner)
        l.add(rec("plat_poulet_riz", MAIN, "Poulet, riz & brocolis", 25, tags(MEAT, BUDGET),
                ing(p("poulet_blanc", 160), r("riz_blanc", 220), v("brocoli", 150), f("huile", 10)),
                steps("Rincer le riz puis le cuire 12 min dans 2 fois son volume d'eau salée.",
                        "Couper le poulet en dés et le saisir 8 min à la poêle avec l'huile, sel, poivre et paprika.",
                        "Cuire les brocolis 6 min à la vapeur pour qu'ils restent croquants.")));
        l.add(rec("plat_saumon_patate", MAIN, "Saumon, patate douce & haricots verts", 30, tags(FISH, PRICEY),
                ing(p("saumon", 140), r("patate_douce", 250), v("haricots_verts", 150), f("huile", 5)),
                steps("Couper la patate douce en cubes, l'enrober d'huile et la rôtir 25 min à 200 °C.",
                        "Ajouter le saumon sur la plaque pour les 12 dernières minutes.",
                        "Cuire les haricots verts 8 min à l'eau bouillante salée.")));
        l.add(rec("plat_steak_pates", MAIN, "Steak haché 5 %, pâtes complètes & courgettes", 20, tags(MEAT),
                ing(p("steak_5", 150), r("pates_completes", 250), v("courgette", 200), f("huile", 5)),
                steps("Cuire les pâtes selon le paquet.",
                        "Faire revenir les courgettes en rondelles 8 min avec l'huile.",
                        "Cuire le steak 2 à 3 min de chaque côté.")));
        l.add(rec("plat_cari_poulet", MAIN, "Cari poulet, riz & grains", 45, tags(MEAT, CREOLE),
                ing(p("poulet_cuisse", 170), r("riz_blanc", 200), r("grains", 100), v("tomate", 100), f("huile", 10)),
                steps("Faire revenir oignon, ail, gingembre et curcuma, puis le poulet en morceaux.",
                        "Ajouter tomates et thym, couvrir et laisser mijoter 30 min.",
                        "Servir avec le riz et les grains.")));
        l.add(rec("plat_rougail_poulet", MAIN, "Rougail poulet, riz & lentilles", 35, tags(MEAT, CREOLE, BUDGET),
                ing(p("poulet_blanc", 160), r("riz_blanc", 200), r("lentilles", 100), v("tomate", 150), f("huile", 10)),
                steps("Couper le poulet en morceaux et le dorer avec l'huile, l'oignon, l'ail et le gingembre.",
                        "Ajouter les tomates en dés et le piment, laisser mijoter 20 min à couvert.",
                        "Servir avec le riz et les lentilles.")));
        l.add(rec("plat_cari_dinde", MAIN, "Cari dinde, riz & chouchou", 35, tags(MEAT, CREOLE, BUDGET),
                ing(p("dinde", 160), r("riz_blanc", 220), v("chouchou", 150), f("huile", 10)),
                steps("Faire revenir oignon, ail, gingembre et curcuma dans l'huile.",
                        "Ajouter la dinde en morceaux et le chouchou en dés, couvrir et cuire 20 min.",
                        "Servir avec le riz.")));
        l.add(rec("plat_cari_tofu", MAIN, "Cari tofu, riz & lentilles", 30, tags(CREOLE, BUDGET),
                ing(p("tofu", 180), r("riz_blanc", 180), r("lentilles", 120), v("tomate", 100), f("huile", 10)),
                steps("Dorer le tofu en cubes dans l'huile, puis le réserver.",
                        "Faire revenir oignon, ail, curcuma et tomates, remettre le tofu et mijoter 10 min.",
                        "Servir avec le riz et les lentilles.")));
        l.add(rec("plat_tofu_quinoa", MAIN, "Tofu sauté, quinoa & brocolis", 25, tags(),
                ing(p("tofu", 150), r("quinoa", 200), v("brocoli", 150), f("huile", 10)),
                steps("Rincer le quinoa et le cuire 12 min.",
                        "Couper le tofu en cubes et le dorer 8 min à la poêle avec l'huile et un peu de sauce soja.",
                        "Ajouter les brocolis cuits à la vapeur.")));
        l.add(rec("plat_rougail_saucisse", MAIN, "Rougail saucisse, riz & grains", 40, tags(MEAT, PORK, CREOLE, TREAT),
                ing(p("saucisse", 120), r("riz_blanc", 200), r("grains", 100), v("tomate", 150)),
                steps("Blanchir les saucisses 5 min puis les couper en rondelles.",
                        "Les faire revenir avec oignon, ail, piment, puis ajouter les tomates.",
                        "Mijoter 20 min et servir avec le riz et les grains.")));
        l.add(rec("plat_cabillaud_riz", MAIN, "Cabillaud, riz & épinards", 20, tags(FISH),
                ing(p("cabillaud", 180), r("riz_blanc", 200), v("epinards", 150), f("huile", 10)),
                steps("Cuire le riz 12 min.",
                        "Cuire le cabillaud 10 min au four à 180 °C avec citron et herbes.",
                        "Faire tomber les épinards 3 min à la poêle avec l'huile.")));
        l.add(rec("plat_dinde_pdt", MAIN, "Dinde, pommes de terre & haricots verts", 30, tags(MEAT, BUDGET),
                ing(p("dinde", 160), r("pdt", 300), v("haricots_verts", 150), f("huile", 10)),
                steps("Cuire les pommes de terre 20 min à l'eau ou à la vapeur.",
                        "Saisir l'escalope de dinde 4 min de chaque côté avec l'huile.",
                        "Cuire les haricots verts 8 min.")));
        l.add(rec("plat_chili", MAIN, "Chili con carne & riz", 35, tags(MEAT, BUDGET),
                ing(p("steak_5", 120), r("haricots_rouges", 150), r("riz_blanc", 150), v("tomate", 150)),
                steps("Faire revenir la viande hachée avec oignon, ail, cumin et paprika.",
                        "Ajouter les tomates et les haricots rouges, mijoter 20 min.",
                        "Servir avec le riz.")));
        l.add(rec("plat_thon_pates", MAIN, "Pâtes au thon & tomates", 15, tags(FISH, BUDGET),
                ing(p("thon", 120), r("pates", 220), v("tomate", 150), f("huile", 10)),
                steps("Cuire les pâtes selon le paquet.",
                        "Faire revenir les tomates coupées 5 min avec l'huile et l'ail.",
                        "Ajouter le thon égoutté et mélanger aux pâtes.")));
        l.add(rec("plat_crevettes", MAIN, "Crevettes sautées, riz & courgettes", 20, tags(FISH, PRICEY),
                ing(p("crevettes", 180), r("riz_blanc", 220), v("courgette", 150), f("huile", 10)),
                steps("Cuire le riz 12 min.",
                        "Faire sauter les courgettes 6 min avec l'huile et l'ail.",
                        "Ajouter les crevettes 3 min, avec du citron vert.")));
        l.add(rec("plat_boeuf_patate", MAIN, "Bœuf, patate douce & brocolis", 30, tags(MEAT, PRICEY),
                ing(p("boeuf", 160), r("patate_douce", 250), v("brocoli", 150), f("huile", 5)),
                steps("Rôtir la patate douce en cubes 25 min à 200 °C.",
                        "Saisir le bœuf 2 à 4 min de chaque côté selon la cuisson voulue.",
                        "Cuire les brocolis 6 min à la vapeur.")));
        l.add(rec("plat_lentilles_oeufs", MAIN, "Lentilles, œufs & riz complet", 30, tags(BUDGET),
                ing(p("oeuf", 120), r("lentilles", 200), r("riz_complet", 120), v("carotte", 80), f("huile", 5)),
                steps("Cuire les lentilles 20 min avec la carotte en rondelles et du thym.",
                        "Cuire le riz complet selon le paquet.",
                        "Ajouter les œufs mollets (6 min) au moment de servir.")));
        l.add(rec("plat_couscous_poulet", MAIN, "Couscous poulet & légumes", 35, tags(MEAT),
                ing(p("poulet_cuisse", 150), r("semoule", 200), v("courgette", 150), v("carotte", 80), r("pois_chiches", 60)),
                steps("Faire mijoter le poulet, les légumes et les pois chiches 25 min avec ras-el-hanout.",
                        "Verser le même volume d'eau bouillante sur la semoule, couvrir 5 min.",
                        "Égrainer la semoule et servir avec le bouillon.")));
        l.add(rec("plat_omelette_pdt", MAIN, "Omelette, pommes de terre & salade", 20, tags(LACTOSE, BUDGET),
                ing(p("oeuf", 120), p("blanc_oeuf", 100), r("pdt", 250), v("salade", 80), f("huile", 10), f("emmental", 20)),
                steps("Cuire les pommes de terre en rondelles 10 min à la poêle avec l'huile.",
                        "Verser les œufs et les blancs battus avec le fromage, cuire 5 min à feu doux.",
                        "Servir avec la salade.")));
        l.add(rec("plat_rougail_thon", MAIN, "Rougail thon, riz & lentilles", 25, tags(FISH, CREOLE, BUDGET),
                ing(p("thon", 150), r("riz_blanc", 200), r("lentilles", 120), v("tomate", 100), f("huile", 10)),
                steps("Faire revenir oignon, ail et piment avec l'huile, ajouter les tomates.",
                        "Ajouter le thon égoutté et laisser mijoter 10 min.",
                        "Servir avec le riz et les lentilles.")));
        l.add(rec("plat_porc_chouchou", MAIN, "Porc, riz & chouchou", 30, tags(MEAT, PORK, CREOLE),
                ing(p("porc_filet", 150), r("riz_blanc", 200), v("chouchou", 150), f("huile", 10)),
                steps("Couper le filet de porc en morceaux et le dorer avec l'huile, l'ail et le thym.",
                        "Ajouter le chouchou en dés, couvrir et cuire 15 min.",
                        "Servir avec le riz.")));
        l.add(rec("plat_bowl_saumon", MAIN, "Bowl saumon, quinoa & avocat", 20, tags(FISH, PRICEY),
                ing(p("saumon", 120), r("quinoa", 180), f("avocat", 60), v("concombre", 100)),
                steps("Cuire le quinoa 12 min et le laisser tiédir.",
                        "Cuire le saumon 10 min au four ou à la poêle, puis l'émietter.",
                        "Disposer quinoa, saumon, avocat et concombre dans un bol.")));
        l.add(rec("plat_pates_dinde", MAIN, "Pâtes complètes, dinde & épinards", 20, tags(MEAT, LACTOSE),
                ing(p("dinde", 140), r("pates_completes", 220), v("epinards", 150), f("emmental", 15)),
                steps("Cuire les pâtes selon le paquet.",
                        "Saisir la dinde en lanières 6 min, ajouter les épinards 3 min.",
                        "Mélanger avec les pâtes et le fromage râpé.")));
        l.add(rec("plat_chili_vege", MAIN, "Chili végé, riz & maïs", 30, tags(BUDGET),
                ing(p("tofu", 100), r("haricots_rouges", 150), r("mais", 80), r("riz_blanc", 150), v("tomate", 150)),
                steps("Émietter le tofu et le faire revenir avec oignon, cumin et paprika.",
                        "Ajouter tomates, haricots rouges et maïs, mijoter 15 min.",
                        "Servir avec le riz.")));
        l.add(rec("plat_salade_poulet", MAIN, "Salade poulet, quinoa & avocat", 20, tags(MEAT),
                ing(p("poulet_blanc", 150), r("quinoa", 150), f("avocat", 60), v("tomate", 120), v("salade", 60), f("huile", 5)),
                steps("Cuire le quinoa 12 min et le laisser refroidir.",
                        "Griller le poulet 8 min et le couper en lamelles.",
                        "Mélanger avec la salade, la tomate, l'avocat et l'huile.")));
        l.add(rec("plat_poisson_rougail", MAIN, "Poisson grillé, riz & rougail tomate", 25, tags(FISH, CREOLE),
                ing(p("cabillaud", 180), r("riz_blanc", 220), v("tomate", 150), f("huile", 10)),
                steps("Cuire le riz 12 min.",
                        "Rougail : couper les tomates en petits dés avec oignon, piment et un filet d'huile.",
                        "Griller le poisson 4 à 5 min de chaque côté.")));
        l.add(rec("plat_steak_frites", MAIN, "Steak, frites au four & salade", 35, tags(MEAT),
                ing(p("steak_5", 150), r("pdt", 300), f("huile", 10), v("salade", 80)),
                steps("Couper les pommes de terre en frites, les enrober d'huile et les cuire 30 min à 210 °C.",
                        "Cuire le steak 2 à 3 min de chaque côté.",
                        "Servir avec la salade.")));
        l.add(rec("plat_couscous_vege", MAIN, "Couscous végé pois chiches & œufs", 30, tags(BUDGET),
                ing(p("oeuf", 120), p("blanc_oeuf", 100), r("pois_chiches", 150), r("semoule", 150), v("courgette", 150), v("carotte", 80)),
                steps("Faire mijoter les légumes et les pois chiches 20 min avec ras-el-hanout.",
                        "Préparer la semoule avec le même volume d'eau bouillante.",
                        "Servir avec les œufs durs coupés en deux et les blancs cuits en omelette fine, roulée et tranchée.")));

        cache = Collections.unmodifiableList(l);
        return cache;
    }

    public static Recipe find(String id) {
        if (id == null) return null;
        for (Recipe r : all()) if (r.id.equals(id)) return r;
        return null;
    }

    public static List<Recipe> of(String kind) {
        List<Recipe> out = new ArrayList<>();
        for (Recipe r : all()) if (r.kind.equals(kind)) out.add(r);
        return out;
    }
}

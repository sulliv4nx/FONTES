package com.fonte.carnet.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class FoodSearchTest {

    private static final String TSV = "# Table Ciqual 2025\n"
            + "# code\tnom\tgroupe_code\tgroupe\tkcal\tproteines\tglucides\tlipides\tfibres\tsucres\n"
            + "36018\tPoulet, filet, sans peau, rôti\t04\tviandes cuites\t150\t30\t0\t3\t\t\n"
            + "36020\tPoulet, cuisse, avec peau, rôtie\t04\tviandes cuites\t230\t25\t0\t15\t\t\n"
            + "25601\tPoulet basquaise\t01\tplats composés\t110\t10\t5\t5\t1\t2\n"
            + "36005\tNuggets de poulet\t04\tproduits à base de viande\t250\t15\t17\t13\t\t\n"
            + "22000\tŒuf, cru\t04\tœufs\t140\t12,5\t0,7\t9,8\t\t\n"
            + "13000\tLait demi-écrémé, UHT\t05\tlaits\t46\t3.3\t4.7\t1.6\t0\t4.7\n"
            + "20000\tChouchou, cuit\t02\tlégumes cuits\t21\t0.9\t3.1\t0.2\t1.5\t\n"
            + "abc\tligne\tincomplète\n";

    private static List<Food> ciqual() throws Exception {
        List<Food> l = Ciqual.parse(new ByteArrayInputStream(TSV.getBytes(StandardCharsets.UTF_8)));
        Ciqual.setAll(l);
        return l;
    }

    @Test
    public void parsesCiqualTable() throws Exception {
        List<Food> l = ciqual();
        assertEquals(7, l.size());
        Food f = Ciqual.find("ciqual_36018");
        assertNotNull(f);
        assertEquals("Poulet, filet, sans peau, rôti", f.name);
        assertEquals("Viandes cuites", f.category);
        assertEquals(150, f.kcal, 1e-9);
        assertEquals(30, f.protein, 1e-9);
        assertEquals("01", Ciqual.groupCode(Ciqual.find("ciqual_25601")));
        assertNull(Ciqual.find("36018"));
        assertNull(Ciqual.find("ciqual_1"));
        // Valeur avec virgule non convertie : comptée 0 plutôt que de casser la lecture.
        assertEquals(0, Ciqual.find("ciqual_22000").protein, 1e-9);
    }

    @Test
    public void textMatching() {
        assertEquals(Arrays.asList("oeuf", "dur"), FoodSearch.tokens("Œufs durs"));
        assertEquals(Arrays.asList("blanc", "poulet"), FoodSearch.tokens("blanc de poulet"));
        assertEquals("mais", FoodSearch.stem("mais"));
        assertEquals("pate", FoodSearch.stem("pates"));
        assertTrue(FoodSearch.score("Poulet, filet, sans peau, rôti", FoodSearch.tokens("poulet"), "poulet") > 0);
        assertTrue(FoodSearch.score("Pâtes cuites", FoodSearch.tokens("pate"), "pate") > 0);
        assertTrue(FoodSearch.score("Bœuf, steak haché", FoodSearch.tokens("boeuf"), "boeuf") > 0);
        // Tous les mots doivent être trouvés.
        assertEquals(-1, FoodSearch.score("Poulet basquaise", FoodSearch.tokens("poulet riz"), "poulet riz"));
        // Synonyme : « blanc de poulet » trouve le filet.
        assertTrue(FoodSearch.score("Poulet, filet, sans peau, rôti", FoodSearch.tokens("blanc de poulet"), "blanc de poulet") > 0);
        // « maïs » ne doit pas trouver « maigre ».
        assertEquals(-1, FoodSearch.score("Bœuf maigre", FoodSearch.tokens("maïs"), "maïs"));
    }

    @Test
    public void mergesSourcesWithoutDuplicates() throws Exception {
        FoodSearch.Input in = new FoodSearch.Input();
        Food recent = Foods.find("poulet_blanc");
        assertNotNull(recent);
        in.recent = Arrays.asList(recent);
        Food mine = new Food();
        mine.id = "perso_1";
        mine.name = "Aiguillettes de poulet";
        mine.custom = true;
        mine.barcode = "3000000000001";
        in.mine = Arrays.asList(mine);
        in.fonte = Foods.all();
        in.ciqual = ciqual();
        in.dishes = Recipes.all();
        List<FoodSearch.Online> online = new ArrayList<>();
        online.add(product("3000000000001", "Aiguillettes de poulet", 0, false));
        online.add(product("3000000000002", "Poulet rôti tranché", 1, false));
        online.add(product("3000000000003", "Poulet rôti tranché", 2, false));
        online.add(product("3000000000004", "Poulet rôti tranché", 2, true));
        in.online = online;

        List<FoodSearch.Result> r = FoodSearch.search("poulet", in);
        assertFalse(r.isEmpty());
        // Le récent passe en premier.
        assertEquals(FoodSearch.RECENT, r.get(0).source);
        assertEquals("poulet_blanc", r.get(0).food.id);
        // Pas de doublon : le récent n'apparaît pas une deuxième fois depuis la base Fonte,
        // et le produit déjà gardé (même code-barres) ne revient pas d'Open Food Facts.
        int recentCount = 0, sameBarcode = 0;
        boolean dish = false, ciq = false, off = false;
        for (FoodSearch.Result x : r) {
            if (x.food != null && "poulet_blanc".equals(x.food.id)) recentCount++;
            if (x.food != null && "3000000000001".equals(x.food.barcode)) sameBarcode++;
            if (FoodSearch.DISH.equals(x.source)) dish = true;
            if (FoodSearch.CIQUAL.equals(x.source)) ciq = true;
            if (FoodSearch.ONLINE.equals(x.source)) off = true;
        }
        assertEquals(1, recentCount);
        assertEquals(1, sameBarcode);
        assertTrue(dish);
        assertTrue(ciq);
        assertTrue(off);
        int[] c = FoodSearch.counts(r);
        assertEquals(r.size(), c[FoodSearch.ALL]);
        assertEquals(r.size(), c[FoodSearch.FOODS] + c[FoodSearch.BRANDS] + c[FoodSearch.DISHES]);
        // Plats : ceux de Fonte (cari poulet…) et les plats composés de Ciqual.
        boolean basquaise = false, cari = false;
        for (FoodSearch.Result x : FoodSearch.filter(r, FoodSearch.DISHES)) {
            if (x.food != null && x.food.id.equals("ciqual_25601")) basquaise = true;
            if (x.dish != null && x.dish.id.equals("plat_cari_poulet")) cari = true;
        }
        assertTrue(basquaise);
        assertTrue(cari);
        // Marques : le produit gardé et les produits en ligne ; le produit de La Réunion passe devant.
        List<FoodSearch.Result> brands = FoodSearch.filter(r, FoodSearch.BRANDS);
        assertEquals(4, brands.size());
        int reunion = -1, france = -1, incomplete = -1;
        for (int i = 0; i < brands.size(); i++) {
            if ("3000000000003".equals(brands.get(i).food.barcode)) reunion = i;
            if ("3000000000002".equals(brands.get(i).food.barcode)) france = i;
            if ("3000000000004".equals(brands.get(i).food.barcode)) incomplete = i;
        }
        assertTrue(reunion >= 0 && france >= 0 && incomplete >= 0);
        // À nom égal : La Réunion avant la France, et une fiche complète avant une fiche incomplète.
        assertTrue(reunion < france);
        assertTrue(reunion < incomplete);
        assertTrue(FoodSearch.search("", in).isEmpty());
        assertTrue(FoodSearch.search("zzzz", in).isEmpty());
    }

    private static FoodSearch.Online product(String code, String name, int country, boolean incomplete) {
        FoodSearch.Online o = new FoodSearch.Online();
        o.food = new Food();
        o.food.id = "off_" + code;
        o.food.name = name;
        o.food.barcode = code;
        o.food.kcal = 120;
        o.food.protein = 22;
        o.brand = "[Marque]";
        o.country = country;
        o.incomplete = incomplete;
        return o;
    }

    @Test
    public void dishTotalsAndPortions() throws Exception {
        Recipe cari = Recipes.find("plat_cari_poulet");
        double[] t = FoodSearch.dishTotals(cari);
        assertTrue(t[0] > 300 && t[0] < 1200);
        assertTrue(t[1] > 20);
        assertTrue(t[4] > 200);
        assertEquals("1 assiette", FoodSearch.dishPortion(cari));

        ciqual();
        Portions.Portion p = Portions.guess(Ciqual.find("ciqual_36018"));
        assertEquals("1 filet", p.label);
        assertEquals(140, p.grams, 1e-9);
        assertEquals("1 œuf", Portions.guess(Ciqual.find("ciqual_22000")).label);
        assertEquals("1 bol", Portions.guess(Ciqual.find("ciqual_13000")).label);
        assertTrue(Portions.liquid(Ciqual.find("ciqual_13000")));
        assertFalse(Portions.liquid(Ciqual.find("ciqual_36018")));
        assertEquals("1 assiette", Portions.guess(Ciqual.find("ciqual_25601")).label);
        assertEquals("1 portion", Portions.guess(Ciqual.find("ciqual_20000")).label);
        // 100 g, la portion devinée, puis « Ma portion » si elle diffère.
        List<Portions.Portion> l = Portions.forFood(Ciqual.find("ciqual_36018"), 160);
        assertEquals(3, l.size());
        assertNull(l.get(0).label);
        assertEquals("100 g", l.get(0).text(false));
        assertEquals("1 filet · 140 g", l.get(1).text(false));
        assertEquals("Ma portion", l.get(2).label);
        assertEquals(2, Portions.forFood(Ciqual.find("ciqual_36018"), 140).size());
        Food plain = new Food();
        plain.name = "Truc inconnu";
        assertEquals(1, Portions.forFood(plain, 0).size());
        Food own = Foods.find("poulet_blanc");
        assertEquals(own.portion, Portions.forFood(own, 0).get(1).grams, 1e-9);
    }
}

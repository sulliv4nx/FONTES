package com.fonte.carnet.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class MealPlannerTest {
    private static final int START = 20261103; // mardi 3 novembre 2026

    private static MealPlanner.Targets gain() {
        return new MealPlanner.Targets(2500, 200, 268, 70);
    }

    private static MealPrefs prefs() {
        MealPrefs p = new MealPrefs();
        p.trainingMask = 1 | 2 | 8 | 16; // lun, mar, jeu, ven
        return p;
    }

    @Test
    public void recipesUseKnownFoodsAndHaveSteps() {
        Set<String> ids = new HashSet<>();
        for (Recipe r : Recipes.all()) {
            assertTrue("doublon " + r.id, ids.add(r.id));
            assertTrue(r.id, r.steps.length > 0);
            assertTrue(r.id, r.ingredients.length > 0);
            boolean prot = false;
            for (Recipe.Ing i : r.ingredients) {
                assertNotNull(r.id + " : " + i.foodId, Foods.find(i.foodId));
                if (i.role == Recipe.PROT) prot = true;
            }
            if (!Recipe.SNACK.equals(r.kind)) assertTrue("source de protéines : " + r.id, prot);
        }
        assertTrue(Recipes.of(Recipe.MAIN).size() >= 20);
        assertTrue(Recipes.of(Recipe.SHAKER).size() >= 4);
    }

    @Test
    public void daySlotsAndShares() {
        List<String> five = MealPlanner.slots(5, true);
        assertEquals(5, five.size());
        assertTrue(five.contains(MealPlan.SHAKER));
        assertFalse(five.contains(MealPlan.SNACK_PM));
        assertEquals(3, MealPlanner.slots(3, false).size());
        assertEquals(4, MealPlanner.slots(3, true).size());
        for (int meals = 3; meals <= 5; meals++) {
            for (boolean sh : new boolean[]{false, true}) {
                List<String> s = MealPlanner.slots(meals, sh);
                double kcal = 0, prot = 0;
                for (String x : s) {
                    kcal += MealPlanner.share(s, x);
                    prot += MealPlanner.proteinShare(s, x);
                }
                assertEquals(1.0, kcal, 0.001);
                assertEquals(1.0, prot, 0.001);
            }
        }
    }

    @Test
    public void shakerOnTrainingDays() {
        MealPrefs p = prefs();
        assertTrue(MealPlanner.shakerDay(p, 20261102)); // lundi
        assertFalse(MealPlanner.shakerDay(p, 20261104)); // mercredi
        p.shakerEveryDay = true;
        assertTrue(MealPlanner.shakerDay(p, 20261104));
        p.shaker = false;
        assertFalse(MealPlanner.shakerDay(p, 20261102));
        assertEquals(0, MealPlanner.weekday(20261102));
        assertEquals(6, MealPlanner.weekday(20261108));
    }

    @Test
    public void weekHitsTargets() {
        for (long seed = 1; seed <= 40; seed++) {
            for (int meals = 3; meals <= 5; meals++) {
                MealPrefs p = prefs();
                p.meals = meals;
                p.variety = (int) (seed % 3);
                p.batch = seed % 2 == 0;
                MealPlan plan = MealPlanner.generate(p, gain(), START, seed);
                assertEquals(7, countDays(plan));
                for (int d = 0; d < 7; d++) {
                    int day = Days.add(START, d);
                    Nutrition.Totals t = plan.totals(day);
                    String where = "graine " + seed + ", " + meals + " repas, jour " + day;
                    assertEquals(where + " kcal", 2500, t.kcal, 2500 * 0.08);
                    // Jamais moins de 90 % des protéines visées ; un léger surplus est toléré.
                    assertTrue(where + " protéines " + t.protein, t.protein >= 200 * 0.9 && t.protein <= 200 * 1.2);
                    assertEquals(where + " lipides", 70, t.fat, 70 * 0.6);
                    MealPlan.Meal lunch = plan.find(day, MealPlan.LUNCH);
                    MealPlan.Meal dinner = plan.find(day, MealPlan.DINNER);
                    assertNotNull(lunch);
                    assertNotNull(dinner);
                    assertFalse(where + " même plat midi et soir", lunch.recipeId.equals(dinner.recipeId));
                }
            }
        }
    }

    /** Méthode 4.3 : jours d'entraînement à 3 320 kcal (séance en glucides), autres jours à 2 940 kcal. */
    @Test
    public void trainingDaysGetSessionCalories() {
        MealPlanner.Targets rest = new MealPlanner.Targets(2940, 180, 371, 82);
        MealPlanner.Targets train = new MealPlanner.Targets(3320, 180, 466, 82);
        for (long seed = 1; seed <= 20; seed++) {
            MealPrefs p = prefs();
            p.meals = 3 + (int) (seed % 3);
            MealPlan plan = MealPlanner.generate(p, rest, train, START, seed);
            assertEquals(3320, plan.trainKcal);
            int trainingDays = 0;
            double protein = 0;
            for (int d = 0; d < 7; d++) {
                int day = Days.add(START, d);
                boolean training = plan.trainingDay(day);
                if (training) trainingDays++;
                int[] target = plan.targetsOn(day);
                assertEquals(training ? 3320 : 2940, target[0]);
                Nutrition.Totals t = plan.totals(day);
                String where = "graine " + seed + ", jour " + day;
                assertEquals(where + " kcal", target[0], t.kcal, target[0] * 0.08);
                // Les féculents apportent aussi des protéines : un surplus est toléré les gros jours.
                assertTrue(where + " protéines " + t.protein, t.protein >= 180 * 0.9 && t.protein <= 180 * 1.5);
                protein += t.protein;
            }
            assertEquals(4, trainingDays);
            assertTrue("graine " + seed + " protéines moyennes " + protein / 7, protein / 7 <= 180 * 1.2);
        }
        // Sans objectifs « séance » distincts, tous les jours visent la même chose.
        MealPlan same = MealPlanner.generate(prefs(), rest, START, 3);
        assertEquals(0, same.trainKcal);
        assertFalse(same.trainingDay(20261103));
        assertEquals(2940, same.targetsOn(20261103)[0]);
    }

    @Test
    public void lighterTargetsToo() {
        MealPlanner.Targets lose = new MealPlanner.Targets(1800, 150, 176, 55);
        for (long seed = 1; seed <= 20; seed++) {
            MealPrefs p = prefs();
            p.meals = 4;
            MealPlan plan = MealPlanner.generate(p, lose, START, seed);
            for (int d = 0; d < 7; d++) {
                Nutrition.Totals t = plan.totals(Days.add(START, d));
                assertEquals("kcal graine " + seed, 1800, t.kcal, 1800 * 0.08);
                assertTrue("protéines graine " + seed + " : " + t.protein, t.protein >= 150 * 0.9 && t.protein <= 150 * 1.2);
            }
        }
    }

    private static int countDays(MealPlan plan) {
        Set<Integer> days = new HashSet<>();
        for (MealPlan.Meal m : plan.meals) days.add(m.day);
        return days.size();
    }

    @Test
    public void preferencesAreRespected() {
        for (long seed = 1; seed <= 30; seed++) {
            MealPrefs p = prefs();
            p.noPork = true;
            p.lactoseFree = true;
            MealPlan plan = MealPlanner.generate(p, gain(), START, seed);
            for (MealPlan.Meal m : plan.meals) {
                Recipe r = Recipes.find(m.recipeId);
                assertFalse(r.id, r.has(Recipe.PORK));
                assertFalse(r.id, r.has(Recipe.LACTOSE));
            }
            MealPrefs v = prefs();
            v.vegetarian = true;
            v.fish = true;
            for (MealPlan.Meal m : MealPlanner.generate(v, gain(), START, seed).meals) {
                assertTrue(m.recipeId, Recipes.find(m.recipeId).vegetarian());
            }
        }
    }

    @Test
    public void avoidedFoodsAreLeftOut() {
        MealPrefs p = prefs();
        p.avoid = "thon, Œufs";
        for (long seed = 1; seed <= 20; seed++) {
            for (MealPlan.Meal m : MealPlanner.generate(p, gain(), START, seed).meals) {
                for (MealPlan.Item i : m.items) {
                    assertFalse(m.recipeId, i.foodId.equals("thon"));
                    assertFalse(m.recipeId, i.foodId.equals("oeuf"));
                }
            }
        }
    }

    @Test
    public void fishAtLeastTwiceWhenAsked() {
        for (long seed = 1; seed <= 40; seed++) {
            MealPrefs p = prefs();
            p.fish = true;
            p.variety = (int) (seed % 3);
            p.batch = seed % 2 == 1;
            MealPlan plan = MealPlanner.generate(p, gain(), START, seed);
            int fish = 0;
            for (MealPlan.Meal m : plan.meals) {
                if (Recipe.MAIN.equals(m.kind()) && Recipes.find(m.recipeId).has(Recipe.FISH)) fish++;
            }
            assertTrue("graine " + seed + " : " + fish, fish >= 2);
        }
    }

    @Test
    public void batchCookingRepeatsDishOnPairs() {
        MealPrefs p = prefs();
        p.batch = true;
        MealPlan plan = MealPlanner.generate(p, gain(), START, 3);
        for (int d = 0; d < 6; d += 2) {
            MealPlan.Meal a = plan.find(Days.add(START, d), MealPlan.LUNCH);
            MealPlan.Meal b = plan.find(Days.add(START, d + 1), MealPlan.LUNCH);
            assertEquals(a.recipeId, b.recipeId);
            assertEquals(1, plan.batchIndex(a));
            assertEquals(2, plan.batchIndex(b));
            assertTrue(plan.batchPartner(a) == b);
        }
        MealPrefs q = prefs();
        q.batch = false;
        q.variety = 2;
        MealPlan solo = MealPlanner.generate(q, gain(), START, 3);
        for (int d = 0; d < 6; d++) {
            MealPlan.Meal a = solo.find(Days.add(START, d), MealPlan.LUNCH);
            MealPlan.Meal b = solo.find(Days.add(START, d + 1), MealPlan.LUNCH);
            assertFalse(a.recipeId.equals(b.recipeId));
        }
    }

    @Test
    public void scalingHitsMealTarget() {
        Recipe r = Recipes.find("plat_poulet_riz");
        MealPlan.Meal m = MealPlanner.build(r, START, MealPlan.LUNCH, 691, 59, 17);
        Nutrition.Totals t = m.totals();
        assertEquals(691, t.kcal, 30);
        assertEquals(59, t.protein, 4);
        MealPlan.Meal big = MealPlanner.build(r, START, MealPlan.LUNCH, 900, 70, 22);
        assertEquals(900, big.totals().kcal, 40);
        // Les quantités restent raisonnables (plafonds par aliment).
        MealPlan.Meal huge = MealPlanner.build(Recipes.find("pdj_fromage_blanc"), START, MealPlan.BREAKFAST, 1200, 90, 30);
        for (MealPlan.Item i : huge.items) {
            double cap = Groceries.max(i.foodId);
            if (cap > 0) assertTrue(i.foodId + " " + i.grams, i.grams <= cap);
        }
    }

    @Test
    public void swapsStayWithinTenPercent() {
        MealPrefs p = prefs();
        MealPlan.Meal m = MealPlanner.build(Recipes.find("plat_poulet_riz"), START, MealPlan.LUNCH, 691, 59, 17);
        List<MealPlanner.Option> o = MealPlanner.alternatives(p, m, 3, false);
        assertEquals(3, o.size());
        Nutrition.Totals ref = m.totals();
        for (MealPlanner.Option x : o) {
            assertFalse(x.recipe.id.equals(m.recipeId));
            assertTrue(x.recipe.id, x.close);
            assertEquals(x.recipe.id, ref.kcal, x.meal.totals().kcal, ref.kcal * 0.10);
        }
        List<MealPlanner.Option> all = MealPlanner.alternatives(p, m, 0, true);
        assertEquals(Recipes.of(Recipe.MAIN).size() - 1, all.size());
    }

    @Test
    public void groceriesDisplayAndShopping() {
        assertEquals("2 œufs", Groceries.amount("oeuf", 120));
        assertEquals("1 banane 1/2", Groceries.amount("banane", 180));
        assertEquals("1/2 banane", Groceries.amount("banane", 60));
        assertEquals("250 ml", Groceries.amount("lait_demi", 250));
        assertEquals("Riz 220 g", Groceries.line("riz_blanc", null, 220));
        assertEquals("≈ 85 g sec", Groceries.rawHint("riz_blanc", 220));
        assertEquals("≈ 215 g cru", Groceries.rawHint("poulet_blanc", 160));
        assertEquals(null, Groceries.rawHint("brocoli", 150));
        assertEquals("1 kg", Groceries.shopAmount("riz_blanc", null, 2600));
        assertEquals("12", Groceries.shopAmount("oeuf", null, 700));
        assertEquals("3 boîtes", Groceries.shopAmount("thon", null, 280));
        assertEquals("4 L", Groceries.shopAmount("lait_demi", null, 3750));
        assertEquals("420 g · 14 doses", Groceries.shopAmount("whey", null, 420));
        assertEquals("1,5 kg", Groceries.shopAmount("patate_douce", null, 1360));

        MealPlan plan = MealPlanner.generate(prefs(), gain(), START, 11);
        List<ShoppingList.Line> lines = ShoppingList.build(plan);
        assertFalse(lines.isEmpty());
        Set<String> keys = new HashSet<>();
        for (ShoppingList.Line l : lines) {
            assertTrue("doublon " + l.key, keys.add(l.key));
            assertFalse(l.name, l.amount.isEmpty());
        }
        plan.checked.add(lines.get(0).key);
        MealPlan.Extra x = new MealPlan.Extra();
        x.name = "Café";
        plan.extras.add(x);
        List<ShoppingList.Line> again = ShoppingList.build(plan);
        assertEquals(lines.size() + 1, again.size());
        assertEquals(1, ShoppingList.checkedCount(again));
        assertEquals(ShoppingList.MINE, again.get(again.size() - 1).aisle);
        String text = ShoppingList.text(plan, again);
        assertTrue(text.contains("Café"));
        assertFalse(text.contains("- " + lines.get(0).name + " :"));
    }

    @Test
    public void labelsAndTraining() {
        MealPlan plan = new MealPlan();
        plan.start = START;
        assertEquals("du 3 au 9 novembre", plan.rangeLabel());
        plan.start = 20261127;
        assertEquals("du 27 novembre au 3 décembre", plan.rangeLabel());
        plan.start = 20261201;
        assertEquals("du 1er au 7 décembre", plan.rangeLabel());

        boolean[] trained = new boolean[28];
        int today = 20261103; // mardi
        // Entraînements les mardis et vendredis des 4 dernières semaines.
        for (int i = 0; i < 28; i++) {
            int wd = MealPlanner.weekday(Days.add(today, -i));
            trained[i] = wd == 1 || wd == 4;
        }
        assertEquals((1 << 1) | (1 << 4), MealPlanner.detectTraining(trained, today));
        assertEquals(1 | 2 | 8 | 16, MealPlanner.detectTraining(new boolean[28], today));
        assertEquals("Mar", MealPlanner.weekdayShort(today));
    }
}

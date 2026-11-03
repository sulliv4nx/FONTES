package com.fonte.carnet.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class ComposerTest {
    private static final int DAY = 20261103;
    private static final List<String> MINE = Arrays.asList("oeuf", "pates", "poulet_blanc", "epinards", "steak_5");

    private static Composer.Proposal named(List<Composer.Proposal> l, String name) {
        for (Composer.Proposal p : l) if (p.recipe.name.equals(name)) return p;
        return null;
    }

    @Test
    public void catalogFoodsExist() {
        Set<String> ids = new HashSet<>();
        for (Composer.Choice c : Composer.catalog()) {
            assertNotNull(c.foodId, Foods.find(c.foodId));
            assertTrue("doublon " + c.foodId, ids.add(c.foodId));
        }
        assertTrue(Composer.choice("bredes").reunion);
        assertFalse(Composer.choice("pates").reunion);
    }

    @Test
    public void proposesPlatesAndRecipesWithMyIngredients() {
        List<Composer.Proposal> l = Composer.propose(MINE, true, DAY, MealPlan.LUNCH, 700, 60, 20);
        assertTrue(l.size() >= 5);
        Composer.Proposal plate = named(l, "Poulet, pâtes & épinards");
        assertNotNull(plate);
        assertTrue(plate.nothingToBuy());
        assertNotNull(named(l, "Steak haché, pâtes & épinards"));
        Composer.Proposal rougail = named(l, "Rougail z'œufs, riz & épinards");
        assertNotNull("rougail z'œufs adapté aux épinards", rougail);
        assertTrue(rougail.reunion);
        assertTrue(rougail.need.contains("riz_blanc"));
        assertTrue(rougail.need.contains("tomate"));
        assertFalse(rougail.need.contains("huile"));
        Set<String> sel = new HashSet<>(MINE);
        for (Composer.Proposal p : l) {
            boolean usesProtein = false;
            for (MealPlan.Item i : p.meal.items) {
                Recipe r = p.recipe;
                for (Recipe.Ing g : r.ingredients) if (g.role == Recipe.PROT && sel.contains(g.foodId)) usesProtein = true;
            }
            assertTrue(p.recipe.name, usesProtein);
            assertEquals(p.recipe.name, 700, p.meal.totals().kcal, 700 * 0.12);
        }
        // Les plats sans rien à acheter passent avant ceux qui demandent beaucoup d'ingrédients en plus.
        assertTrue(l.get(0).need.size() <= 1);
    }

    @Test
    public void withoutCompletingNothingToBuy() {
        List<Composer.Proposal> l = Composer.propose(MINE, false, DAY, MealPlan.DINNER, 650, 55, 18);
        assertFalse(l.isEmpty());
        for (Composer.Proposal p : l) assertTrue(p.recipe.name, p.need.isEmpty());
    }

    @Test
    public void reunionIngredientsGiveReunionDishes() {
        List<Composer.Proposal> l = Composer.propose(Arrays.asList("morue", "riz_blanc", "tomate"), true, DAY, MealPlan.LUNCH, 700, 60, 20);
        Composer.Proposal p = named(l, "Rougail morue, riz & grains");
        assertNotNull(p);
        assertTrue(p.reunion);
        assertEquals(Arrays.asList("grains"), p.need);
        List<Composer.Proposal> cabri = Composer.propose(Arrays.asList("cabri"), true, DAY, MealPlan.DINNER, 650, 55, 18);
        assertNotNull(named(cabri, "Massalé cabri, riz & grains"));
        assertNotNull(named(cabri, "Cabri, riz & brocolis"));
    }

    @Test
    public void breakfastUsesBreakfastRecipes() {
        List<Composer.Proposal> l = Composer.propose(Arrays.asList("oeuf"), true, DAY, MealPlan.BREAKFAST, 600, 45, 18);
        assertFalse(l.isEmpty());
        for (Composer.Proposal p : l) assertEquals(Recipe.BREAKFAST, p.recipe.kind);
    }

    @Test
    public void composedIdsResolve() {
        Recipe plate = Composer.plate("poulet_blanc", "pates", "epinards");
        Recipe again = Recipes.find(plate.id);
        assertNotNull(again);
        assertEquals("Poulet, pâtes & épinards", again.name);
        assertEquals(3, again.steps.length);
        Recipe sub = Recipes.find("plat_rougail_zoeufs~bredes>epinards");
        assertNotNull(sub);
        assertEquals("Rougail z'œufs, riz & épinards", sub.name);
        assertTrue(sub.steps[3].contains("épinards"));
        assertEquals(null, Recipes.find("auto:inconnu::"));
        assertEquals("Cari dinde, riz & grains", Recipes.find("plat_cari_poulet~poulet_cuisse>dinde").name);
        assertEquals("Pâtes, dinde & épinards", Composer.replaceWord("Pâtes complètes, dinde & épinards", "pâtes complètes", "pâtes"));
        assertEquals("Riz complet & riz", Composer.replaceWord("Riz complet & riz", "riz", "riz"));
        assertEquals("Riz complet, quinoa", Composer.replaceWord("Riz complet, riz", "riz", "quinoa"));
    }

    @Test
    public void fillWeekKeepsTargetsAndAlternates() {
        MealPrefs prefs = new MealPrefs();
        prefs.trainingMask = 1 | 2 | 8 | 16;
        MealPlan plan = MealPlanner.generate(prefs, new MealPlanner.Targets(2500, 200, 268, 70), DAY, 5);
        plan.find(DAY, MealPlan.LUNCH).eaten = true;
        List<Recipe> dishes = new ArrayList<>();
        dishes.add(Composer.plate("poulet_blanc", "pates", "epinards"));
        dishes.add(Recipes.find("plat_rougail_zoeufs~bredes>epinards"));
        dishes.add(Composer.plate("steak_5", "pates", "epinards"));
        List<Composer.Placement> list = Composer.fillWeek(plan, dishes, DAY, true);
        assertEquals(13, list.size()); // 14 repas principaux, moins le déjeuner déjà mangé
        for (Composer.Placement p : list) {
            assertFalse(p.old.eaten);
            assertEquals(p.old.day, p.fresh.day);
            assertEquals(p.old.slot, p.fresh.slot);
            assertEquals(p.old.targetKcal, p.fresh.totals().kcal, p.old.targetKcal * 0.12);
            plan.meals.set(plan.meals.indexOf(p.old), p.fresh);
        }
        for (int d = 0; d < 7; d++) {
            int day = Days.add(DAY, d);
            assertFalse(plan.find(day, MealPlan.LUNCH).recipeId.equals(plan.find(day, MealPlan.DINNER).recipeId));
        }
        // Cuisine en lot : le dîner de mardi est repris mercredi.
        assertEquals(plan.find(DAY, MealPlan.DINNER).recipeId, plan.find(Days.add(DAY, 1), MealPlan.DINNER).recipeId);
        assertEquals(1, plan.batchIndex(plan.find(DAY, MealPlan.DINNER)));
        // Plus tard dans la semaine seulement.
        assertEquals(4, Composer.fillWeek(plan, dishes, Days.add(DAY, 5), true).size());
    }
}

package com.fonte.carnet.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class NutritionTest {

    private static Profile man() {
        Profile p = new Profile();
        p.sex = Profile.MALE;
        p.birthYear = 1996;
        p.heightCm = 180;
        p.activity = 1.375;
        return p;
    }

    @Test
    public void bmrAndTargets() {
        Profile p = man();
        assertEquals(1780.0, Nutrition.bmr(p, 80, 30), 1e-9);
        Profile w = new Profile();
        w.sex = Profile.FEMALE;
        w.heightCm = 165;
        assertEquals(1330.25, Nutrition.bmr(w, 60, 28), 1e-9);
        assertEquals(2448, Nutrition.maintenance(p, 80, 30));
        p.goal = Profile.LOSE;
        assertEquals(1960, Nutrition.target(p, 80, 30));
        p.goal = Profile.GAIN;
        assertEquals(2690, Nutrition.target(p, 80, 30));
        p.kcalOverride = 2500;
        assertEquals(2500, Nutrition.target(p, 80, 30));
    }

    @Test
    public void macrosAndWater() {
        Profile p = man();
        Nutrition.Macros m = Nutrition.macros(p, 80, 1960);
        assertEquals(160, m.protein);
        assertEquals(72, m.fat);
        assertEquals(168, m.carbs);
        assertEquals(2800, Nutrition.water(p, 80, false));
        assertEquals(3300, Nutrition.water(p, 80, true));
        p.waterOverride = 2000;
        assertEquals(2000, Nutrition.water(p, 80, true));
    }

    @Test
    public void burnedCalories() {
        assertEquals(420, Nutrition.kcal(5.0, 80, 60));
        int day = Days.key(1_700_000_000_000L);
        Session s = new Session();
        s.id = 1;
        s.name = "Push";
        s.start = 1_700_000_000_000L;
        s.end = s.start + 60 * 60_000L;
        WodResult r = new WodResult();
        r.wodName = "Fran";
        r.date = s.start;
        r.minutes = 5;
        BodyLog.Activity a = new BodyLog.Activity();
        a.day = day;
        a.name = "Course";
        a.minutes = 30;
        a.kcal = 300;
        List<Nutrition.Burn> b = Nutrition.burned(day, Arrays.asList(s), Arrays.asList(r), new ArrayList<>(), Arrays.asList(a), 80);
        assertEquals(3, b.size());
        assertEquals(420 + Nutrition.kcal(8.0, 80, 5) + 300, Nutrition.total(b));
        assertTrue(Nutrition.burned(Days.add(day, 1), Arrays.asList(s), Arrays.asList(r), new ArrayList<>(), Arrays.asList(a), 80).isEmpty());
    }

    @Test
    public void bodyFatAndBmi() {
        assertEquals(16.1, Nutrition.navyBodyFat(Profile.MALE, 180, 38, 85, 0), 0.3);
        assertTrue(Nutrition.navyBodyFat(Profile.FEMALE, 165, 32, 72, 98) > 15);
        assertEquals(0.0, Nutrition.navyBodyFat(Profile.FEMALE, 165, 32, 72, 0), 1e-9);
        assertEquals(24.7, Stats.round1(Nutrition.bmi(80, 180)), 1e-9);
        assertEquals("corpulence normale", Nutrition.bmiLabel(24.7));
    }

    @Test
    public void entriesAndTotals() {
        Food chicken = Foods.find("poulet_blanc");
        FoodEntry e = FoodEntry.of(chicken, 150);
        assertEquals(247.5, e.kcal, 1e-9);
        assertEquals(46.5, e.protein, 1e-9);
        e.day = 20261103;
        e.meal = FoodEntry.LUNCH;
        FoodEntry o = FoodEntry.of(Foods.find("riz_blanc"), 200);
        o.day = 20261103;
        o.meal = FoodEntry.DINNER;
        Nutrition.Totals t = Nutrition.totals(Arrays.asList(e, o), 20261103, null);
        assertEquals(247.5 + 260, t.kcal, 1e-9);
        assertEquals(247.5, Nutrition.totals(Arrays.asList(e, o), 20261103, FoodEntry.LUNCH).kcal, 1e-9);
    }

    @Test
    public void labelParser() {
        String label = "Valeurs nutritionnelles moyennes Pour 100 g Par portion (30 g)\n"
                + "Énergie 1567 kJ 470 kJ\n370 kcal 111 kcal\n"
                + "Matières grasses 6,9 g 2,1 g\ndont acides gras saturés 1,2 g 0,4 g\n"
                + "Glucides 58,7 g 17,6 g\ndont sucres 1,1 g 0,3 g\nFibres alimentaires 10 g 3 g\n"
                + "Protéines 13,5 g 4,1 g\nSel 0,02 g 0,01 g";
        Food f = LabelParser.parse(label);
        assertNotNull(f);
        assertEquals(370.0, f.kcal, 1e-9);
        assertEquals(6.9, f.fat, 1e-9);
        assertEquals(58.7, f.carbs, 1e-9);
        assertEquals(13.5, f.protein, 1e-9);
        assertEquals(30.0, f.portion, 1e-9);

        // Valeurs passées à la ligne, énergie en kJ seulement
        Food g = LabelParser.parse("ENERGIE\n1000 kJ\nPROTEINES\n20 g\nGLUCIDES\n5 g\nLIPIDES\n10 g");
        assertNotNull(g);
        assertEquals(239.0, g.kcal, 1e-9);
        assertEquals(20.0, g.protein, 1e-9);
        assertEquals(10.0, g.fat, 1e-9);

        assertNull(LabelParser.parse("Ingrédients : farine de blé, sucre, sel"));
        assertNull(LabelParser.parse(""));
    }

    @Test
    public void days() {
        assertEquals(20261103, Days.key(Days.start(20261103)));
        assertEquals(20270101, Days.add(20261231, 1));
        assertEquals(20260228, Days.add(20260301, -1));
        assertEquals(20261102, Days.monday(20261103));
        assertEquals(20261102, Days.monday(20261108));
        assertEquals("M", Days.initial(20261103));
    }

    @Test
    public void foodDatabase() {
        Set<String> ids = new HashSet<>();
        for (Food f : Foods.all()) {
            assertTrue("doublon " + f.id, ids.add(f.id));
            assertTrue(Arrays.asList(Foods.CATEGORIES).contains(f.category));
            assertTrue(f.name, f.kcal >= 0 && f.protein >= 0 && f.carbs >= 0 && f.fat >= 0);
            assertTrue(f.name, f.protein + f.carbs + f.fat <= 101);
            boolean alcohol = f.id.equals("biere") || f.id.equals("vin");
            if (!alcohol && f.kcal >= 40) {
                double atwater = 4 * f.protein + 4 * f.carbs + 9 * f.fat;
                assertTrue(f.name + " : " + atwater + " vs " + f.kcal, Math.abs(atwater - f.kcal) <= f.kcal * 0.2 + 10);
            }
        }
        assertFalse(Foods.search(Foods.all(), "poulet").isEmpty());
        assertEquals("oeuf", Foods.search(Foods.all(), "oeuf entier").get(0).id);
        assertEquals(Foods.all().size(), Foods.search(Foods.all(), "").size());
    }
}

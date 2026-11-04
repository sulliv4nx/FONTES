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

    /** Exemple du document : homme, 30 ans, 1,78 m, 90 kg, 15 % de masse grasse, bureau. */
    private static Profile man() {
        Profile p = new Profile();
        p.sex = Profile.MALE;
        p.birthYear = 1996;
        p.heightCm = 178;
        return p;
    }

    private static final double W = 90, FAT = 15;
    private static final int AGE = 30, SESSION = 378;

    @Test
    public void bmrEquations() {
        Profile p = man();
        assertEquals(1867.5, Nutrition.mifflin(p, W, AGE), 1e-9);
        assertEquals(2096.83, Nutrition.tenHaaf(p, W, AGE), 0.01);
        assertEquals(2183.0, Nutrition.cunningham(Nutrition.leanMass(W, FAT)), 1e-9);
        assertEquals(2226.25, Nutrition.tenHaafLean(Nutrition.leanMass(W, FAT)), 0.01);
        // Ten Haaf par défaut ; les équations à masse maigre retombent sur Ten Haaf sans masse grasse.
        assertEquals(Profile.BMR_TENHAAF, Nutrition.formula(p, FAT));
        p.bmrFormula = Profile.BMR_CUNNINGHAM;
        assertEquals(Profile.BMR_CUNNINGHAM, Nutrition.formula(p, FAT));
        assertEquals(Profile.BMR_TENHAAF, Nutrition.formula(p, 0));
        assertEquals(2096.83, Nutrition.bmr(p, W, AGE, 0), 0.01);
        Profile w = new Profile();
        w.sex = Profile.FEMALE;
        w.heightCm = 165;
        assertEquals(1330.25, Nutrition.mifflin(w, 60, 28), 1e-9);
        assertEquals(1487.58, Nutrition.tenHaaf(w, 60, 28), 0.01);
        // 60 min de musculation (5 MET) pour 90 kg : 378 kcal nettes, 473 brutes.
        assertEquals(378, Nutrition.netKcal(Nutrition.MET_MUSCU, W, 60));
        assertEquals(473, Nutrition.kcal(Nutrition.MET_MUSCU, W, 60));
    }

    @Test
    public void maintenanceExample() {
        Profile p = man();
        Nutrition.Targets rest = Nutrition.targets(p, W, AGE, FAT, 0, 0, false);
        assertEquals(2097, rest.bmr);
        assertEquals(2936, rest.expenditure);
        assertEquals(0, rest.goalKcal);
        assertEquals(2940, rest.base);
        assertEquals(2940, rest.kcal);
        assertEquals(180, rest.protein);
        assertEquals(82, rest.fat);
        assertEquals(371, rest.carbs);
        Nutrition.Targets train = Nutrition.targets(p, W, AGE, FAT, SESSION, 60, false);
        assertEquals(380, train.sessionKcal);
        assertEquals(3320, train.kcal);
        assertEquals(180, train.protein);
        assertEquals(82, train.fat);
        assertEquals(466, train.carbs);
        // Eau : 35 ml/kg, +500 ml par heure de séance.
        assertEquals(3150, rest.water);
        assertEquals(3650, train.water);
        assertEquals(3400, Nutrition.water(p, W, 30));
    }

    @Test
    public void gainAndLossExamples() {
        Profile p = man();
        p.goal = Profile.GAIN;
        p.gainRate = 0.35;
        Nutrition.Targets gain = Nutrition.targets(p, W, AGE, FAT, 0, 0, false);
        assertEquals(346, gain.goalKcal); // 0,315 kg × 1 100 kcal
        assertEquals(3280, gain.kcal);
        assertEquals(180, gain.protein);
        assertEquals(91, gain.fat);
        assertTrue(gain.carbs / W >= 4.5);
        p.goal = Profile.LOSE;
        p.lossRate = 0.75;
        Nutrition.Targets loss = Nutrition.targets(p, W, AGE, FAT, 0, 0, false);
        assertEquals(-742, loss.goalKcal);
        assertEquals(2190, loss.kcal);
        // Sèche : 2,6 g par kg de masse maigre (76,5 kg).
        assertTrue(loss.proteinLean);
        assertEquals(199, loss.protein);
        assertEquals(61, loss.fat);
        assertEquals(211, loss.carbs);
        // Sans masse grasse : 2,2 g par kg de poids.
        Nutrition.Targets noFat = Nutrition.targets(p, W, AGE, 0, 0, 0, false);
        assertFalse(noFat.proteinLean);
        assertEquals(198, noFat.protein);
        // Jamais sous le métabolisme de base.
        p.lossRate = 1.0;
        p.daily = 1.2;
        Nutrition.Targets low = Nutrition.targets(p, W, AGE, FAT, 0, 0, false);
        assertTrue(low.floored);
        assertEquals(2100, low.kcal);
    }

    @Test
    public void limitsAndModes() {
        Profile p = man();
        // Lipides : jamais sous 0,5 g/kg, même à 20 %.
        p.fatPct = 20;
        p.kcalOverride = 1800;
        Nutrition.Targets t = Nutrition.targets(p, W, AGE, FAT, SESSION, 60, false);
        assertTrue(t.imposed);
        assertEquals(1800, t.kcal);
        assertEquals(0, t.sessionKcal);
        assertEquals(45, t.fat);
        assertEquals(180, t.protein);
        assertEquals(169, t.carbs);
        // Correction d'après les pesées.
        p.kcalOverride = 0;
        p.fatPct = 25;
        p.kcalAdjust = -150;
        assertEquals(2790, Nutrition.targets(p, W, AGE, FAT, 0, 0, false).kcal);
        p.kcalAdjust = 0;
        // Mode pourcentages (article) : « Prise de muscle » 40 / 50 / 10.
        p.method = Profile.METHOD_PERCENT;
        p.split = "muscle";
        Nutrition.Targets pct = Nutrition.targets(p, W, AGE, FAT, 0, 0, false);
        assertEquals(294, pct.protein);
        assertEquals(368, pct.carbs);
        assertEquals(33, pct.fat);
        // Personnalisée : le total est ramené à 100 %.
        p.split = Profile.SPLIT_CUSTOM;
        p.pctProtein = 30;
        p.pctCarbs = 50;
        p.pctFat = 30;
        assertEquals(27, p.percents()[0]);
        assertEquals(45, p.percents()[1]);
        assertEquals(28, p.percents()[2]);
        p.waterOverride = 2000;
        assertEquals(2000, Nutrition.water(p, W, 60));
    }

    private static BodyLog.Weight weigh(int day, double kg) {
        BodyLog.Weight w = new BodyLog.Weight();
        w.day = day;
        w.kg = kg;
        return w;
    }

    @Test
    public void weightTrend() {
        int today = Days.key(1_800_000_000_000L);
        Profile p = man();
        p.goal = Profile.LOSE;
        p.lossRate = 0.75;
        List<BodyLog.Weight> list = new ArrayList<>();
        // Perte de 0,2 kg par semaine au lieu de 0,675 visé : il faut baisser de 200 kcal (écart ≈ 520 kcal/j).
        for (int i = 0; i <= 21; i += 3) list.add(weigh(Days.add(today, -i), 90 + 0.2 * i / 7.0));
        Nutrition.Trend t = Nutrition.trend(p, list, 90, today);
        assertTrue(t.ready());
        assertEquals(-0.2, t.kgPerWeek, 0.01);
        assertEquals(-0.675, t.expected, 1e-9);
        assertEquals(-200, t.suggestion);
        // Rythme tenu : rien à corriger.
        list.clear();
        for (int i = 0; i <= 21; i += 3) list.add(weigh(Days.add(today, -i), 90 + 0.68 * i / 7.0));
        assertEquals(0, Nutrition.trend(p, list, 90, today).suggestion);
        // Maintien mais poids qui baisse de 0,15 kg/sem (≈ 165 kcal/j) : +150 kcal.
        p.goal = Profile.MAINTAIN;
        list.clear();
        for (int i = 0; i <= 21; i += 3) list.add(weigh(Days.add(today, -i), 90 + 0.15 * i / 7.0));
        assertEquals(150, Nutrition.trend(p, list, 90, today).suggestion);
        // Pas assez de pesées, ou pesées d'avant la dernière correction : pas de proposition.
        p.adjustDay = Days.add(today, -10);
        Nutrition.Trend few = Nutrition.trend(p, list, 90, today);
        assertFalse(few.ready());
        assertEquals(0, few.suggestion);
    }

    @Test
    public void burnedCalories() {
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
        assertEquals(Nutrition.netKcal(5.0, 80, 60) + Nutrition.netKcal(8.0, 80, 5) + 300, Nutrition.total(b));
        assertEquals(336, Nutrition.netKcal(5.0, 80, 60));
        assertEquals(95, Nutrition.minutes(b));
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
        // Glucides hors fibres (convention des étiquettes européennes).
        assertEquals(1.8, Foods.find("avocat").carbs, 1e-9);
        assertEquals(12.5, Foods.find("lentilles").carbs, 1e-9);
        assertFalse(Foods.search(Foods.all(), "poulet").isEmpty());
        assertEquals("oeuf", Foods.search(Foods.all(), "oeuf entier").get(0).id);
        assertEquals(Foods.all().size(), Foods.search(Foods.all(), "").size());
    }
}

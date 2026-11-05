package com.fonte.carnet.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DayPlannerTest {

    private static final DayPlanner.Resolver FOODS = Foods::find;

    @Test
    public void clockParsing() {
        assertEquals("6h15", DayPrefs.clock(375));
        assertEquals("0h05", DayPrefs.clock(5));
        assertEquals(18 * 60 + 30, DayPrefs.parseClock("18h30"));
        assertEquals(18 * 60 + 30, DayPrefs.parseClock("18:30"));
        assertEquals(6 * 60 + 15, DayPrefs.parseClock("615"));
        assertEquals(7 * 60, DayPrefs.parseClock("7h"));
        assertEquals(-1, DayPrefs.parseClock("25h00"));
        assertEquals(-1, DayPrefs.parseClock("abc"));
    }

    @Test
    public void scheduleAroundTraining() {
        DayPrefs p = new DayPrefs();
        DayPlanner.Schedule s = DayPlanner.schedule(p);
        assertEquals(5, s.times.length);
        assertEquals("6h15", DayPrefs.clock(s.times[0]));
        assertEquals("16h00", DayPrefs.clock(s.times[3]));
        assertEquals("20h30", DayPrefs.clock(s.times[4]));
        assertEquals(3, s.pre);
        assertEquals(4, s.post);
        assertEquals(DayPlanner.BREAKFAST, s.kinds[0]);
        assertEquals(DayPlanner.LUNCH, s.kinds[2]);
        for (int i = 1; i < s.times.length; i++) assertTrue(s.times[i] - s.times[i - 1] >= 120);
        // Heure imposée par l'utilisateur.
        p.times.put(2, 12 * 60 + 30);
        assertEquals("12h30", DayPrefs.clock(DayPlanner.schedule(p).times[2]));
        // Séance au réveil : le premier repas vient après.
        DayPrefs m = new DayPrefs();
        m.wake = 5 * 60 + 30;
        m.training = 6 * 60;
        DayPlanner.Schedule ms = DayPlanner.schedule(m);
        assertEquals(0, ms.post);
        assertEquals(-1, ms.pre);
        assertEquals("7h30", DayPrefs.clock(ms.times[0]));
        // 4 et 6 repas.
        p.times.clear();
        p.meals = 4;
        assertEquals(4, DayPlanner.schedule(p).times.length);
        p.meals = 6;
        assertEquals(6, DayPlanner.schedule(p).times.length);
    }

    private static DayPlanner.Line line(DayPlanner.Slot s, String role) {
        for (DayPlanner.Line l : s.lines) if (role.equals(l.role)) return l;
        return null;
    }

    @Test
    public void trainingDayHitsTargets() {
        DayPrefs p = new DayPrefs();
        DayPlanner.Day d = DayPlanner.build(p, 3320, 180, 466, 82, true, FOODS);
        assertEquals(6, d.slots.size());
        assertEquals(3320, d.kcal, 3320 * 0.03);
        assertEquals(180, d.protein, 180 * 0.05);
        assertEquals(466, d.carbs, 466 * 0.08);
        assertEquals(82, d.fat, 82 * 0.08);
        // Collation d'avant la séance : un fruit, 30 minutes avant.
        DayPlanner.Slot snack = d.slots.get(4);
        assertFalse(snack.main());
        assertEquals("18h00", DayPrefs.clock(snack.minutes));
        assertEquals(DayPlanner.FRUIT, snack.lines.get(0).role);
        // Protéines réparties à parts égales ; plus de glucides avant la séance qu'au déjeuner.
        for (DayPlanner.Slot s : d.slots) if (s.main()) assertEquals(36, s.protein, 4);
        assertTrue(d.slot(3).carbs > d.slot(2).carbs);
        assertTrue(d.slot(3).fat < d.slot(2).fat);
        // Petit-déjeuner : avoine et blanc d'œuf ; déjeuner : riz, poulet, légumes, huile.
        DayPlanner.Slot b = d.slot(0);
        assertEquals("avoine", line(b, DayPlanner.CARBS).food.id);
        assertEquals("blanc_oeuf", line(b, DayPlanner.PROTEIN).food.id);
        assertNotNull(line(b, DayPlanner.FRUIT));
        DayPlanner.Slot lunch = d.slot(2);
        assertEquals("riz_blanc", line(lunch, DayPlanner.CARBS).food.id);
        assertEquals("poulet_blanc", line(lunch, DayPlanner.PROTEIN).food.id);
        assertNotNull(line(lunch, DayPlanner.VEG));
        assertEquals("Repas 3 · déjeuner", DayPlanner.title(lunch, true));
        assertEquals("Repas 4 · avant la séance", DayPlanner.title(d.slot(3), true));
        // Toutes les quantités sont des multiples de 5 g.
        for (DayPlanner.Slot s : d.slots) for (DayPlanner.Line l : s.lines) assertEquals(0, l.grams % 5, 1e-9);
    }

    @Test
    public void equivalentsKeepTheMainNutrient() {
        DayPlanner.Day d = DayPlanner.build(new DayPrefs(), 3320, 180, 466, 82, true, FOODS);
        DayPlanner.Line rice = line(d.slot(2), DayPlanner.CARBS);
        assertFalse(rice.alts.isEmpty());
        for (DayPlanner.Alt a : rice.alts) {
            assertEquals(rice.carbs, a.food.carbs * a.grams / 100, 2.5);
            assertFalse(a.food.id.equals(rice.food.id));
        }
        DayPlanner.Line chicken = line(d.slot(2), DayPlanner.PROTEIN);
        for (DayPlanner.Alt a : chicken.alts) assertEquals(chicken.protein, a.food.protein * a.grams / 100, 2.5);
        assertTrue(DayPlanner.altText(rice).startsWith("ou "));
        assertTrue(DayPlanner.altText(line(d.slot(2), DayPlanner.VEG)).contains("brèdes"));
    }

    @Test
    public void restDayDropsStarchBeforeTraining() {
        DayPlanner.Day d = DayPlanner.build(new DayPrefs(), 2940, 180, 371, 82, false, FOODS);
        assertEquals(5, d.slots.size());
        for (DayPlanner.Slot s : d.slots) assertTrue(s.main());
        DayPlanner.Slot four = d.slot(3);
        assertNull(line(four, DayPlanner.CARBS));
        assertNotNull(line(four, DayPlanner.FRUIT));
        assertNotNull(line(four, DayPlanner.PROTEIN));
        assertEquals("Repas 4 · collation", DayPlanner.title(four, false));
        assertEquals(2940, d.kcal, 2940 * 0.03);
        assertEquals(180, d.protein, 180 * 0.05);
        assertEquals(82, d.fat, 82 * 0.08);
    }

    @Test
    public void choicesAndOverrides() {
        DayPrefs p = new DayPrefs();
        // Base imposée à un repas : patate douce au déjeuner.
        p.firsts.put(DayPrefs.firstKey(2, DayPlanner.CARBS), "patate_douce");
        DayPlanner.Day d = DayPlanner.build(p, 3320, 180, 466, 82, true, FOODS);
        assertEquals("patate_douce", line(d.slot(2), DayPlanner.CARBS).food.id);
        assertEquals("riz_blanc", line(d.slot(1), DayPlanner.CARBS).food.id);
        // Sans poulet ni dinde : la base devient le cabillaud.
        p.proteins.remove("poulet_blanc");
        p.proteins.remove("dinde");
        d = DayPlanner.build(p, 3320, 180, 466, 82, true, FOODS);
        assertEquals("cabillaud", line(d.slot(2), DayPlanner.PROTEIN).food.id);
        // Collation d'avant la séance : la mangue remplace la banane, à glucides égaux (120 g de banane = 24 g).
        DayPlanner.Slot snack = d.slots.get(4);
        assertEquals("banane", snack.lines.get(0).food.id);
        assertEquals(120, snack.lines.get(0).grams, 1e-9);
        p.firsts.put(DayPrefs.firstKey(-1, DayPlanner.FRUIT), "mangue");
        snack = DayPlanner.build(p, 3320, 180, 466, 82, true, FOODS).slots.get(4);
        assertEquals("mangue", snack.lines.get(0).food.id);
        assertEquals(180, snack.lines.get(0).grams, 1e-9);
        // Pas de collation avant la séance si elle est désactivée.
        p.preSnack = false;
        assertEquals(5, DayPlanner.build(p, 3320, 180, 466, 82, true, FOODS).slots.size());
    }

    @Test
    public void wording() {
        DayPlanner.Line oil = DayPlanner.line(DayPlanner.FAT, Foods.find("huile"), 10);
        assertEquals("1 c. à soupe d'huile d'olive", DayPlanner.text(oil));
        assertEquals("10 g", DayPlanner.detail(oil));
        DayPlanner.Line oats = DayPlanner.line(DayPlanner.CARBS, Foods.find("avoine"), 95);
        assertEquals("95 g de flocons d'avoine", DayPlanner.text(oats));
        DayPlanner.Line rice = DayPlanner.line(DayPlanner.CARBS, Foods.find("riz_blanc"), 295);
        assertEquals("295 g de riz cuit", DayPlanner.text(rice));
        assertNotNull(DayPlanner.detail(rice));
        DayPlanner.Line banana = DayPlanner.line(DayPlanner.FRUIT, Foods.find("banane"), 100);
        assertEquals("1 banane", DayPlanner.text(banana));
        assertEquals("100 g", DayPlanner.detail(banana));
        DayPlanner.Line whites = DayPlanner.line(DayPlanner.PROTEIN, Foods.find("blanc_oeuf"), 115);
        assertEquals("3 blancs ½", DayPlanner.detail(whites));
        assertEquals("115 g de blanc d'œuf", DayPlanner.text(whites));
        assertEquals("amandes", DayPlanner.phrase(Foods.find("amandes")));
        assertEquals(4, DayPlanner.defaultRules(3150, 3650).size());
    }
}

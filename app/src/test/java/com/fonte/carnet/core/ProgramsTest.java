package com.fonte.carnet.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ProgramsTest {

    private static final Exercise BENCH = new Exercise(Library.BENCH, "Développé couché", Library.PUSH, false);

    @Test
    public void roundToStep() {
        assertEquals(72.5, Programs.round(71.6, 2.5), 1e-9);
        assertEquals(70.0, Programs.round(71.2, 2.5), 1e-9);
        assertEquals(71.25, Programs.round(71.3, 1.25), 1e-9);
        assertEquals(71.3, Programs.round(71.3, 0), 1e-9);
    }

    @Test
    public void smolovJrStructure() {
        Program p = Programs.smolovJr(BENCH, 100, 2.5, 5, 2.5, 42L);
        assertEquals(13, p.days.size());
        assertEquals(42L, p.startedAt);
        // Semaine 1 : 6×6 à 70 kg, 7×5 à 75, 8×4 à 80, 10×3 à 85
        int[] sets = {6, 7, 8, 10};
        int[] reps = {6, 5, 4, 3};
        double[] w1 = {70, 75, 80, 85};
        for (int d = 0; d < 4; d++) {
            Program.Day day = p.days.get(d);
            assertEquals("Semaine 1", day.group);
            assertEquals(1, day.exercises.size());
            ExerciseEntry e = day.exercises.get(0);
            assertEquals(Library.BENCH, e.exerciseId);
            assertEquals(sets[d], e.sets.size());
            for (SetEntry s : e.sets) {
                assertEquals(reps[d], s.reps);
                assertEquals(w1[d], s.weight, 1e-9);
                assertFalse(s.done);
            }
        }
        // Semaine 2 : +2,5 kg ; semaine 3 : +5 kg
        assertEquals(72.5, p.days.get(4).exercises.get(0).sets.get(0).weight, 1e-9);
        assertEquals(87.5, p.days.get(7).exercises.get(0).sets.get(0).weight, 1e-9);
        assertEquals(75.0, p.days.get(8).exercises.get(0).sets.get(0).weight, 1e-9);
        assertEquals(90.0, p.days.get(11).exercises.get(0).sets.get(0).weight, 1e-9);
        assertEquals("J1 · 6×6 · 70 kg", p.days.get(0).name);
        // Test du max : tentative à 105 kg
        List<SetEntry> test = p.days.get(12).exercises.get(0).sets;
        assertEquals(105.0, test.get(test.size() - 1).weight, 1e-9);
        assertEquals("Tentative", test.get(test.size() - 1).label);
    }

    @Test
    public void smolovJrRounding() {
        Program p = Programs.smolovJr(BENCH, 87, 2.5, 5, 2.5, 0);
        // 87 × 0,70 = 60,9 → 60 ; 87 × 0,85 + 5 = 78,95 → 80
        assertEquals(60.0, p.days.get(0).exercises.get(0).sets.get(0).weight, 1e-9);
        assertEquals(80.0, p.days.get(11).exercises.get(0).sets.get(0).weight, 1e-9);
    }

    @Test
    public void wendlerStructure() {
        List<Exercise> lifts = Arrays.asList(
                new Exercise(Library.OHP, "Développé militaire", Library.PUSH, false),
                new Exercise(Library.DEADLIFT, "Soulevé de terre", Library.LEGS, false),
                new Exercise(Library.BENCH, "Développé couché", Library.PUSH, false),
                new Exercise(Library.SQUAT, "Squat", Library.LEGS, false));
        Program p = Programs.wendler(lifts, new double[]{60, 200, 100, 0}, true, 2.5, 7L);
        // 3 mouvements × 4 semaines
        assertEquals(12, p.days.size());
        Program.Day bench1 = p.days.get(2);
        assertEquals("Développé couché · 5/5/5+", bench1.name);
        List<SetEntry> s = bench1.exercises.get(0).sets;
        // TM = 90 kg : 65 % = 58,5 → 57,5 ; 75 % = 67,5 ; 85 % = 76,5 → 77,5
        assertEquals(57.5, s.get(0).weight, 1e-9);
        assertEquals(67.5, s.get(1).weight, 1e-9);
        assertEquals(77.5, s.get(2).weight, 1e-9);
        assertTrue(s.get(2).amrap);
        assertFalse(s.get(1).amrap);
        // BBB : 5×10 à 45 kg
        assertEquals(8, s.size());
        assertEquals(45.0, s.get(7).weight, 1e-9);
        assertEquals(10, s.get(7).reps);
        // Semaine 4 : décharge, pas de série max ni de BBB
        Program.Day deload = p.days.get(11);
        assertEquals("Semaine 4 · décharge", deload.group);
        assertEquals(3, deload.exercises.get(0).sets.size());
        for (SetEntry x : deload.exercises.get(0).sets) assertFalse(x.amrap);
    }

    @Test
    public void wendlerWithoutAnyMaxIsNull() {
        List<Exercise> lifts = Arrays.asList(BENCH);
        assertNull(Programs.wendler(lifts, new double[]{0}, false, 2.5, 0));
    }

    @Test
    public void programProgress() {
        Program p = Programs.smolovJr(BENCH, 100, 2.5, 5, 2.5, 0);
        assertEquals(0, p.nextIndex());
        p.days.get(0).sessionId = 5;
        p.days.get(1).sessionId = 6;
        assertEquals(2, p.doneCount());
        assertEquals(2, p.nextIndex());
        for (Program.Day d : p.days) d.sessionId = 1;
        assertTrue(p.finished());
        assertEquals(-1, p.nextIndex());
    }
}

package com.fonte.carnet.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/** Cohérence des données intégrées : ids uniques, exercices existants, formats valides. */
public class LibraryDataTest {

    private static Set<Long> exerciseIds() {
        Set<Long> ids = new HashSet<>();
        for (Exercise e : Library.defaults()) assertTrue("id en double : " + e.id, ids.add(e.id));
        return ids;
    }

    @Test
    public void exercisesAndTargets() {
        Set<Long> ids = exerciseIds();
        Set<String> names = new HashSet<>();
        for (Exercise e : Library.defaults()) {
            assertTrue("nom en double : " + e.name, names.add(e.name.toLowerCase()));
            assertTrue(Arrays.asList(Library.CATEGORIES).contains(e.category));
            assertTrue(e.id < Library.FIRST_CUSTOM_ID);
        }
        for (Library.Target t : Library.targets()) {
            assertTrue("cible inconnue : " + t.id, ids.contains(t.id));
            for (String m : t.muscles) assertTrue(Arrays.asList(Library.MUSCLES).contains(m));
        }
        for (String m : Library.MUSCLES) {
            boolean found = false;
            for (Library.Target t : Library.targets()) if (t.primary().equals(m)) found = true;
            assertTrue("aucun exercice pour " + m, found);
        }
    }

    @Test
    public void templates() {
        Set<Long> ids = exerciseIds();
        Set<Long> tids = new HashSet<>();
        for (Template t : Templates.all()) {
            assertTrue(t.builtin());
            assertTrue(tids.add(t.id));
            assertTrue(Arrays.asList(Templates.GROUPS).contains(t.group));
            assertFalse(t.exercises.isEmpty());
            for (ExerciseEntry e : t.exercises) {
                assertTrue("exercice inconnu dans " + t.name, ids.contains(e.exerciseId));
                assertFalse(e.sets.isEmpty());
            }
        }
    }

    @Test
    public void wods() {
        Set<String> ids = new HashSet<>();
        List<String> formats = Arrays.asList(Wod.FOR_TIME, Wod.AMRAP, Wod.EMOM, Wod.TABATA);
        int heroes = 0;
        int girls = 0;
        for (Wod w : Wods.all()) {
            assertTrue("WOD en double : " + w.id, ids.add(w.id));
            assertTrue(formats.contains(w.format));
            assertFalse(w.name, w.lines.isEmpty());
            assertFalse(w.name, w.tags.isEmpty());
            for (String t : w.tags) assertTrue(w.name + " : " + t, Arrays.asList(Wods.TAGS).contains(t));
            if (Wod.AMRAP.equals(w.format) || Wod.EMOM.equals(w.format)) assertTrue(w.name, w.minutes > 0);
            if (Wod.HERO.equals(w.kind)) heroes++;
            if (Wod.GIRL.equals(w.kind)) girls++;
            Clock c = Clock.forWod(w, 10);
            assertTrue(c.phases.size() >= 2);
        }
        assertTrue(heroes >= 30);
        assertTrue(girls >= 15);
        for (String tag : Wods.TAGS) assertTrue("aucun WOD pour " + tag, Wods.byTag(tag).size() >= 3);
        assertNotNull(Wods.find("murph"));
        assertEquals("AMRAP 20 min", Wods.find("cindy").formatLabel());
        assertEquals("E3MOM 15 min", Wods.find("force_souffle").formatLabel());
    }

    @Test
    public void routines() {
        Set<String> ids = new HashSet<>();
        for (Routine r : Routines.all()) {
            assertTrue(ids.add(r.id));
            assertTrue(Arrays.asList(Routines.FOCUS).contains(r.focus));
            assertFalse(r.moves.isEmpty());
            for (Routine.Move m : r.moves) assertTrue(r.name + " / " + m.name, m.seconds > 0 || m.reps > 0);
            assertTrue(r.estimatedMinutes() >= 3 && r.estimatedMinutes() <= 30);
        }
        for (String f : Routines.FOCUS) assertFalse(Routines.byFocus(f).isEmpty());
    }

    @Test
    public void generator() {
        List<ExerciseEntry> s = Generator.propose(Arrays.asList(Library.PECS, Library.TRICEPS), Generator.HYPERTROPHY, 45, 1);
        assertEquals(4, s.size());
        Set<Long> seen = new HashSet<>();
        for (ExerciseEntry e : s) assertTrue(seen.add(e.exerciseId));
        // le premier exercice est un polyarticulaire à 4×8
        assertEquals(4, s.get(0).sets.size());
        assertEquals(8, s.get(0).sets.get(0).reps);
        // chaque muscle est travaillé en principal
        boolean pecs = false;
        boolean triceps = false;
        for (ExerciseEntry e : s) {
            for (Library.Target t : Library.targets()) {
                if (t.id != e.exerciseId) continue;
                if (t.primary().equals(Library.PECS)) pecs = true;
                if (t.primary().equals(Library.TRICEPS)) triceps = true;
            }
        }
        assertTrue(pecs && triceps);
        // même graine = même séance, autre graine = autre séance possible
        assertEquals(s.get(0).exerciseId, Generator.propose(Arrays.asList(Library.PECS, Library.TRICEPS), Generator.HYPERTROPHY, 45, 1).get(0).exerciseId);
        // force : 5×5 sur les polyarticulaires
        List<ExerciseEntry> f = Generator.propose(Arrays.asList(Library.QUADS), Generator.STRENGTH, 30, 3);
        assertEquals(3, f.size());
        assertEquals(5, f.get(0).sets.size());
        assertEquals(5, f.get(0).sets.get(0).reps);
        // beaucoup de muscles : au moins un exercice chacun
        List<ExerciseEntry> many = Generator.propose(Arrays.asList(Library.MUSCLES), Generator.ENDURANCE, 30, 9);
        assertEquals(Library.MUSCLES.length, many.size());
        assertEquals("Pectoraux + Triceps", Generator.title(Arrays.asList(Library.TRICEPS, Library.PECS)));
    }

    @Test
    public void wodResults() {
        WodResult a = new WodResult();
        a.wodId = "fran";
        a.type = WodResult.TIME;
        a.seconds = 300;
        WodResult b = new WodResult();
        b.wodId = "fran";
        b.type = WodResult.TIME;
        b.seconds = 280;
        b.rx = false;
        WodResult c = new WodResult();
        c.wodId = "fran";
        c.type = WodResult.TIME;
        c.seconds = 290;
        assertEquals(c, WodResult.best(Arrays.asList(a, b, c), "fran", WodResult.TIME));
        assertEquals("4:50", c.score());
        WodResult r = new WodResult();
        r.type = WodResult.ROUNDS;
        r.rounds = 8;
        r.reps = 12;
        assertEquals("8 tours + 12", r.score());
    }
}

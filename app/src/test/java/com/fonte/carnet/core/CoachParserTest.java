package com.fonte.carnet.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CoachParserTest {

    private static final CoachParser.ExerciseMatcher LIB = CoachParser.libraryMatcher(Library.defaults());
    private static final CoachParser.Maxes MAXES = id -> id == Library.SQUAT ? 140 : 0;

    private static CoachParser.Result parse(String s) {
        return CoachParser.parse(s, LIB, MAXES, 2.5);
    }

    @Test
    public void classicFormats() {
        CoachParser.Result r = parse("Squat 5x5 100kg\n"
                + "Développé couché 4 × 8 @ 70\n"
                + "- Soulevé de terre : 3x5 à 140 kg\n"
                + "3 séries de 12 curl biceps\n"
                + "Rowing barre 4*10 60\n"
                + "Tractions 4 x max");
        assertEquals(6, r.items.size());
        CoachParser.Item sq = r.items.get(0);
        assertEquals(Library.SQUAT, sq.exerciseId);
        assertEquals(5, sq.sets);
        assertEquals(5, sq.reps);
        assertEquals(100.0, sq.weightForSet(0), 1e-9);

        CoachParser.Item dc = r.items.get(1);
        assertEquals(Library.BENCH, dc.exerciseId);
        assertEquals(4, dc.sets);
        assertEquals(8, dc.reps);
        assertEquals(70.0, dc.weightForSet(0), 1e-9);

        assertEquals(Library.DEADLIFT, r.items.get(2).exerciseId);
        assertEquals(140.0, r.items.get(2).weightForSet(0), 1e-9);

        CoachParser.Item curl = r.items.get(3);
        assertEquals(21, curl.exerciseId);
        assertEquals(3, curl.sets);
        assertEquals(12, curl.reps);
        assertTrue(curl.weights.isEmpty());

        assertEquals(12, r.items.get(4).exerciseId);
        assertEquals(60.0, r.items.get(4).weightForSet(0), 1e-9);

        CoachParser.Item pull = r.items.get(5);
        assertEquals(11, pull.exerciseId);
        assertTrue(pull.max);
        ExerciseEntry e = pull.toEntry(11);
        assertEquals(4, e.sets.size());
        assertTrue(e.sets.get(0).amrap);
        assertEquals(0, e.sets.get(0).reps);
    }

    @Test
    public void englishNamesPercentAndNameOnPreviousLine() {
        CoachParser.Result r = parse("A1) Back squat\n5x3 @ 80%\nB. Bench press 5x5x60\nRDL 3x10 (tempo 3010) RPE 8\n"
                + "Bulgarian split squat 3x10/jambe 20kg");
        assertEquals(4, r.items.size());
        CoachParser.Item sq = r.items.get(0);
        assertEquals(Library.SQUAT, sq.exerciseId);
        assertEquals(80, sq.percent);
        assertEquals(112.5, sq.weightForSet(0), 1e-9); // 80 % de 140, arrondi à 2,5
        assertEquals(Library.BENCH, r.items.get(1).exerciseId);
        assertEquals(60.0, r.items.get(1).weightForSet(0), 1e-9);
        CoachParser.Item rdl = r.items.get(2);
        assertEquals(17, rdl.exerciseId);
        assertTrue(rdl.weights.isEmpty()); // ni le tempo ni le RPE ne sont des charges
        CoachParser.Item bss = r.items.get(3);
        assertEquals(54, bss.exerciseId);
        assertEquals(10, bss.reps);
        assertEquals(20.0, bss.weightForSet(0), 1e-9);
    }

    @Test
    public void perSetWeightsAndUnknownExercise() {
        CoachParser.Result r = parse("Front squat 3x3 100/105/110 kg\nSled push 4x20 m\nÉchauffement 10 min");
        CoachParser.Item fs = r.items.get(0);
        assertEquals(16, fs.exerciseId);
        assertEquals(3, fs.weights.size());
        assertEquals(110.0, fs.weightForSet(2), 1e-9);
        CoachParser.Item sled = r.items.get(1);
        assertEquals(0, sled.exerciseId);
        assertEquals("Sled push", sled.name);
        assertEquals(1, r.ignored.size());
    }

    @Test
    public void percentWithoutKnownMaxKeepsPercent() {
        CoachParser.Result r = parse("Développé couché 5x5 75%");
        CoachParser.Item it = r.items.get(0);
        assertTrue(it.weights.isEmpty());
        assertEquals(75, it.percent);
        assertEquals("5×5 · 75 % du max", it.summary());
    }

    @Test
    public void detectsWodBlock() {
        CoachParser.Result r = parse("Force\nSquat 5x5 100kg\n\nWOD : AMRAP 12 min\n10 wall balls\n15 KB swings\n200 m run\n\nCurl 3x12");
        assertEquals(2, r.items.size());
        assertNotNull(r.wod);
        assertEquals(Wod.AMRAP, r.wod.format);
        assertEquals(12, r.wod.minutes);
        assertEquals(3, r.wod.lines.size());
        assertEquals("10 wall balls", r.wod.lines.get(0));
    }

    @Test
    public void detectsForTimeRoundsAndEmom() {
        CoachParser.Result r = parse("5 rounds for time (cap 20 min)\n400 m run\n15 burpees");
        assertNotNull(r.wod);
        assertEquals(Wod.FOR_TIME, r.wod.format);
        assertEquals(20, r.wod.minutes);
        assertEquals("5 tours :", r.wod.lines.get(0));
        assertEquals(3, r.wod.lines.size());
        assertTrue(r.items.isEmpty());

        CoachParser.Result e = parse("E2MOM 16'\n5 power cleans\n10 burpees");
        assertEquals(Wod.EMOM, e.wod.format);
        assertEquals(120, e.wod.every);
        assertEquals(16, e.wod.minutes);
    }

    @Test
    public void matcherHandlesAccentsPluralsAndAliases() {
        assertEquals(Library.DEADLIFT, LIB.match("SOULEVÉS DE TERRE"));
        assertEquals(11, LIB.match("tractions lestées"));
        assertEquals(9, LIB.match("Push press"));
        assertEquals(19, LIB.match("leg press"));
        assertEquals(Library.OHP, LIB.match("Strict press"));
        assertEquals(46, LIB.match("hammer curls"));
        assertEquals(0, LIB.match("sled push"));
    }

    @Test
    public void noSchemeMeansNoItem() {
        assertNull(CoachParser.parseLine("Repos 2 min entre les séries"));
        CoachParser.Result r = parse("");
        assertTrue(r.items.isEmpty());
        assertNull(r.wod);
        assertFalse(parse("Squat 5x5").items.isEmpty());
    }

    @Test
    public void rampMergedShoulderWordsAndCardio() {
        CoachParser.Result r = parse("Échauffement:\n"
                + "A) rotation d'épaules 2x10\n"
                + "B) Face pull élastique 2x15\n"
                + "C) Strict press 1x10 @ 20kg\n"
                + "Strict press 1x6 @ 30kg\n"
                + "Strict press 1x4 @ 40kg\n"
                + "Strict press 1x1 @ 62,5kg\n"
                + "Strict press 4x3 @ 55kg\n"
                + "D) Z press kettlebell 3x6\n"
                + "E) Rowing haltère un bras 4x8\n\n"
                + "Endurance :\n"
                + "250 m rameur\n"
                + "2 min corde à sauter\n"
                + "1 min repos");
        assertEquals(5, r.items.size());
        // « épaules » n'est pas l'épaulé.
        assertEquals(62, r.items.get(0).exerciseId);
        assertEquals(15, r.items.get(1).exerciseId);
        // Montée en charge : un seul exercice, toutes les séries dans l'ordre.
        CoachParser.Item press = r.items.get(2);
        assertEquals(Library.OHP, press.exerciseId);
        assertEquals(8, press.totalSets());
        ExerciseEntry e = press.toEntry(Library.OHP);
        assertEquals(8, e.sets.size());
        assertEquals(20, e.sets.get(0).weight, 1e-9);
        assertEquals(10, e.sets.get(0).reps);
        assertEquals(62.5, e.sets.get(3).weight, 1e-9);
        assertEquals(55, e.sets.get(7).weight, 1e-9);
        assertEquals(3, e.sets.get(7).reps);
        // Le Z press n'est pas le développé militaire.
        assertEquals(61, r.items.get(3).exerciseId);
        assertEquals(13, r.items.get(4).exerciseId);
        // Cardio : rameur et corde ; le repos et le titre ne sont pas « non utilisés ».
        assertEquals(2, r.cardio.size());
        assertEquals("rameur", r.cardio.get(0).kind);
        assertEquals(250, r.cardio.get(0).meters);
        assertEquals(1, r.cardio.get(0).minutes);
        assertTrue(r.cardio.get(0).estimated);
        assertEquals("corde", r.cardio.get(1).kind);
        assertEquals(2, r.cardio.get(1).minutes);
        assertTrue(r.ignored.isEmpty());
        // « Épaulé » seul reste l'épaulé ; une lettre de partie sépare deux lignes du même exercice.
        assertEquals(24, parse("Épaulé 5x3 60kg").items.get(0).exerciseId);
        assertEquals(2, parse("A) Squat 5x5 100kg\nB) Squat 3x3 110kg").items.size());
        // Intervalles et tours : 3 tours de 5x400 m = 6 km ; les rowings de force ne sont pas du cardio.
        CoachParser.Result t = parse("3 tours :\nCourse 5x400 m\n15 cal assault bike");
        assertEquals(6000, t.cardio.get(0).meters);
        assertEquals(45, t.cardio.get(1).kcal);
        assertTrue(parse("Bent over row 4x10 50kg").cardio.isEmpty());
        assertTrue(parse("Farmer walk 4x30 m").cardio.isEmpty());
    }
}

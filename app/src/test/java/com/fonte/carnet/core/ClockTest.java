package com.fonte.carnet.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ClockTest {

    @Test
    public void amrapCountsDownThenFinishes() {
        Clock c = Clock.amrap(10, 10);
        assertEquals(2, c.phases.size());
        assertTrue(c.current().prep);
        c.update(9_999);
        assertTrue(c.current().prep);
        assertEquals(1, c.secondsLeft(9_001));
        c.update(10_000);
        assertEquals("AMRAP", c.current().label);
        assertEquals(600, c.secondsLeft(10_000));
        c.update(10_000 + 599_000);
        assertEquals(1, c.secondsLeft(10_000 + 599_000));
        c.update(10_000 + 600_000);
        assertTrue(c.finished());
        assertEquals(600_000, c.workElapsed(700_000));
        assertTrue(c.roundCounter);
        assertEquals(WodResult.ROUNDS, c.scoreType);
    }

    @Test
    public void updateCanSkipSeveralPhases() {
        Clock c = Clock.emom(60, 5, 0, null);
        assertEquals(5, c.phases.size());
        c.update(185_000);
        assertEquals(3, c.index());
        assertEquals("Minute 4 / 5", c.current().label);
        assertEquals(55, c.secondsLeft(185_000));
        assertEquals(300, c.plannedSeconds());
    }

    @Test
    public void forTimeWaitsForFinishOrCap() {
        Clock free = Clock.forTime(0, 0);
        free.update(3_600_000);
        assertFalse(free.finished());
        free.next(754_000);
        assertTrue(free.finished());
        assertEquals(754_000, free.workElapsed(900_000));

        Clock capped = Clock.forTime(15, 10);
        capped.update(10_000 + 15 * 60_000);
        assertTrue(capped.finished());
        assertEquals(15 * 60_000, capped.workElapsed(2_000_000));
    }

    @Test
    public void tabataHasNoFinalRest() {
        Clock c = Clock.tabata(8, 0);
        assertEquals(15, c.phases.size());
        assertEquals(8 * 20 + 7 * 10, c.plannedSeconds());
        c.update(20_000);
        assertTrue(c.current().rest);
        c.update(30_000);
        assertEquals(2, c.current().round);
    }

    @Test
    public void circuitMixesTimedAndManualMoves() {
        Routine r = new Routine("x", "Test", Routines.CORE, 2, 10, 30, "")
                .t("Planche", 30, "")
                .r("Dead bug", 12, "");
        Clock c = Clock.circuit(r, 0);
        // Planche, repos, dead bug, repos de tour, planche, repos, dead bug
        assertEquals(7, c.phases.size());
        c.update(30_000);
        assertTrue(c.current().rest);
        c.update(40_000);
        assertTrue(c.current().manual);
        c.update(500_000);
        assertTrue(c.current().manual); // attend le geste
        c.next(60_000);
        assertEquals(30, c.current().seconds); // repos entre les tours
        c.update(90_000);
        assertEquals("Planche", c.current().label);
        assertEquals(2, c.current().round);
    }

    @Test
    public void wodClockUsesWodFormat() {
        Clock fran = Clock.forWod(Wods.find("fran"), 10);
        assertEquals("Fran", fran.title);
        assertEquals(WodResult.TIME, fran.scoreType);
        Clock chelsea = Clock.forWod(Wods.find("chelsea"), 0);
        assertEquals(30, chelsea.phases.size());
        assertEquals("5 pull-ups · 10 push-ups · 15 air squats", chelsea.phases.get(0).detail);
        Clock roue = Clock.forWod(Wods.find("roue_cardio"), 0);
        assertTrue(roue.phases.get(1).detail.startsWith("Min 2"));
        Clock e3 = Clock.forWod(Wods.find("force_souffle"), 0);
        assertEquals(5, e3.phases.size());
        assertEquals(180, e3.phases.get(0).seconds);
        Clock tab = Clock.forWod(Wods.find("tabata_total"), 0);
        assertTrue(tab.phases.get(0).detail.startsWith("air squats"));
        assertTrue(tab.phases.get(2).detail.startsWith("push-ups"));
    }

    @Test
    public void previousGoesBack() {
        Clock c = Clock.emom(60, 3, 0, null);
        c.update(130_000);
        assertEquals(2, c.index());
        c.previous(130_000);
        assertEquals(1, c.index());
        c.update(189_000);
        assertEquals(1, c.index());
        c.update(190_000);
        assertEquals(2, c.index());
    }
}

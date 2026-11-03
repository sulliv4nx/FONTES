package com.fonte.carnet.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class StatsTest {

    private static SetEntry done(double w, int r) {
        SetEntry s = new SetEntry(w, r);
        s.done = true;
        return s;
    }

    private static Session session(long id, long start, long exId, SetEntry... sets) {
        Session s = new Session();
        s.id = id;
        s.start = start;
        s.end = start + 3600_000;
        ExerciseEntry e = new ExerciseEntry(exId);
        e.sets.addAll(Arrays.asList(sets));
        s.exercises.add(e);
        return s;
    }

    private static final Stats.Names NAMES = id -> "Couché";

    @Test
    public void epley() {
        assertEquals(100.0, Stats.e1rm(100, 1), 1e-9);
        assertEquals(116.6667, Stats.e1rm(100, 5), 1e-3);
        assertEquals(0.0, Stats.e1rm(0, 5), 1e-9);
        assertEquals(0.0, Stats.e1rm(100, 0), 1e-9);
    }

    @Test
    public void recordsIgnoreUndoneSetsAndHighReps() {
        SetEntry undone = new SetEntry(200, 1);
        Session s = session(1, 1000, 1, done(100, 5), undone, done(60, 20));
        Map<Long, Stats.Record> r = Stats.records(Arrays.asList(s));
        Stats.Record rec = r.get(1L);
        assertNotNull(rec);
        assertEquals(100.0, rec.maxWeight, 1e-9);
        assertEquals(116.6667, rec.bestE1rm, 1e-3); // la série de 20 ne compte pas pour le 1RM
        assertEquals(20, rec.maxReps);
        assertEquals(1, rec.sessions);
    }

    @Test
    public void inProgressSessionsAreIgnored() {
        Session s = session(1, 1000, 1, done(100, 5));
        s.end = 0;
        assertTrue(Stats.records(Arrays.asList(s)).isEmpty());
    }

    @Test
    public void newRecordsDetected() {
        Session old = session(1, 1000, 1, done(100, 5));
        Session now = session(2, 2000, 1, done(105, 5));
        List<String> prs = Stats.newRecords(now, Arrays.asList(old, now), NAMES);
        assertEquals(2, prs.size());
        assertTrue(prs.get(0).startsWith("Couché : 1RM estimé 122,5 kg"));
        assertTrue(prs.get(1).startsWith("Couché : charge max 105 kg × 5"));
    }

    @Test
    public void firstTimeIsNotARecord() {
        Session now = session(2, 2000, 1, done(105, 5));
        assertTrue(Stats.newRecords(now, new ArrayList<>(), NAMES).isEmpty());
    }

    @Test
    public void bodyweightRepsRecord() {
        Session old = session(1, 1000, 11, done(0, 10));
        Session now = session(2, 2000, 11, done(0, 12));
        List<String> prs = Stats.newRecords(now, Arrays.asList(old), NAMES);
        assertEquals(Arrays.asList("Couché : 12 répétitions (avant 10)"), prs);
    }

    @Test
    public void liveRecordUsesSessionSoFar() {
        Stats.Record prev = Stats.records(Arrays.asList(session(1, 1000, 1, done(100, 5)))).get(1L);
        Session cur = session(2, 2000, 1, done(110, 1), done(110, 1));
        cur.end = 0;
        SetEntry second = cur.exercises.get(0).sets.get(1);
        Stats.Record soFar = Stats.sessionBest(cur, 1, second);
        // 110 kg : record de charge à la 1re série, mais pas à la 2e
        assertNotNull(Stats.liveRecord(cur.exercises.get(0).sets.get(0), prev, null));
        assertNull(Stats.liveRecord(second, prev, soFar));
    }

    @Test
    public void lastPerformanceAndSummary() {
        Session a = session(1, 1000, 1, done(100, 5));
        Session b = session(2, 5000, 1, done(102.5, 5), done(102.5, 4));
        Session c = session(3, 3000, 2, done(140, 5));
        List<SetEntry> last = Stats.lastPerformance(1, Arrays.asList(a, b, c), 0);
        assertEquals("102,5×5 · 102,5×4", Stats.summarize(last));
        assertEquals("100×5", Stats.summarize(Stats.lastPerformance(1, Arrays.asList(a, b, c), 2)));
        assertNull(Stats.lastPerformance(9, Arrays.asList(a, b, c), 0));
    }

    @Test
    public void progressSortedByDate() {
        Session a = session(1, 5000, 1, done(100, 1));
        Session b = session(2, 1000, 1, done(90, 1));
        List<double[]> pts = Stats.progress(1, Arrays.asList(a, b), false);
        assertEquals(2, pts.size());
        assertEquals(90.0, pts.get(0)[1], 1e-9);
        assertEquals(100.0, pts.get(1)[1], 1e-9);
    }

    @Test
    public void dropUndoneCleansSession() {
        Session s = session(1, 0, 1, done(100, 5), new SetEntry(100, 5));
        ExerciseEntry empty = new ExerciseEntry(2);
        empty.sets.add(new SetEntry(50, 5));
        s.exercises.add(empty);
        s.dropUndone();
        assertEquals(1, s.exercises.size());
        assertEquals(1, s.exercises.get(0).sets.size());
        assertEquals(500.0, s.tonnage(), 1e-9);
    }
}

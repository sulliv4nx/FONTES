package com.fonte.carnet.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CardioTest {

    @Test
    public void kcalFromIntensityOrErgo() {
        // Rameur modéré (MET 7) 20 min à 80 kg : (7 − 1) × 3,5 × 80 / 200 × 20 = 168 kcal nettes.
        assertEquals(168, Cardio.netKcal("rameur", 1, 20, 0, 80));
        assertTrue(Cardio.netKcal("rameur", 2, 20, 0, 80) > Cardio.netKcal("rameur", 0, 20, 0, 80));
        // Calories de l'ergo : le repos (1 MET, 28 kcal en 20 min à 80 kg) est déduit.
        assertEquals(250 - 28, Cardio.netKcal("rameur", 1, 20, 250, 80));
        assertEquals(0, Cardio.netKcal("rameur", 1, 0, 0, 80));
        // Type inconnu : « autre ».
        assertEquals("Autre", Cardio.name("trampoline"));
    }

    @Test
    public void distances() {
        assertEquals(3000, Cardio.meters("rameur", 3000));
        assertEquals(6200, Cardio.meters("course", 6.2));
        assertEquals("6,2 km", Cardio.distance("course", 6200));
        assertEquals("3 000 m", Cardio.distance("rameur", 3000));
        assertNull(Cardio.distance("corde", 0));
        assertNull(Cardio.unit("corde"));
    }

    @Test
    public void sessionCardioIsNotCountedTwice() {
        Session s = new Session();
        s.id = 7;
        s.name = "Séance libre";
        s.start = Days.start(20261105) + 18 * 3600_000L;
        s.end = s.start + 60 * 60_000L;
        ExerciseEntry e = new ExerciseEntry();
        s.exercises.add(e);
        BodyLog.Activity bike = new BodyLog.Activity();
        bike.name = "Vélo d'appartement";
        bike.day = 20261105;
        bike.minutes = 20;
        bike.kcal = 120;
        bike.sessionId = 7;
        bike.sport = Cardio.MUSCU;
        List<BodyLog.Activity> acts = new ArrayList<>();
        acts.add(bike);
        List<Session> sessions = new ArrayList<>();
        sessions.add(s);
        List<Nutrition.Burn> b = Nutrition.burned(20261105, sessions, Collections.emptyList(), Collections.emptyList(), acts, 80);
        assertEquals(2, b.size());
        // 60 min de séance, dont 20 de vélo : 40 min de muscu.
        assertEquals(40, b.get(0).minutes);
        assertEquals(Nutrition.netKcal(Nutrition.MET_MUSCU, 80, 40), b.get(0).kcal);
        assertEquals(120, b.get(1).kcal);
        // Séance de cardio seul : pas de muscu comptée.
        s.exercises.clear();
        b = Nutrition.burned(20261105, sessions, Collections.emptyList(), Collections.emptyList(), acts, 80);
        assertEquals(1, b.size());
        assertEquals("Vélo d'appartement", b.get(0).label);
        // Résumé affiché.
        bike.intensity = 1;
        bike.kind = "velo_appart";
        bike.meters = 8500;
        assertEquals("20 min · 8,5 km · modéré · ≈ 120 kcal", Cardio.summary(bike));
    }
}

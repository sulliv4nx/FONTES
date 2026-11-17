package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.List;

/** Une séance d'entraînement (en cours si end == 0). */
public class Session {
    public long id;
    public String name;
    public long start;
    public long end;
    public List<ExerciseEntry> exercises = new ArrayList<>();
    /** Lien vers le programme : startedAt du programme + index du jour (-1 si séance libre). */
    public long programKey;
    public int programDay = -1;
    /** Records battus pendant la séance (texte prêt à afficher). */
    public List<String> records = new ArrayList<>();

    /** Durée saisie à la main pour une séance notée après coup (minutes, 0 = chronomètre). */
    public int manualMinutes;

    public boolean inProgress() {
        return end == 0;
    }

    public double tonnage() {
        double t = 0;
        for (ExerciseEntry e : exercises)
            for (SetEntry s : e.sets)
                if (s.done) t += s.volume();
        return t;
    }

    public int doneSets() {
        int n = 0;
        for (ExerciseEntry e : exercises) n += e.doneCount();
        return n;
    }

    public long durationMs() {
        if (end == 0 && manualMinutes > 0) return manualMinutes * 60_000L;
        long stop = end == 0 ? System.currentTimeMillis() : end;
        return Math.max(0, stop - start);
    }

    /** Retire les séries non validées et les exercices vides (appelé à la fin de la séance). */
    public void dropUndone() {
        List<ExerciseEntry> kept = new ArrayList<>();
        for (ExerciseEntry e : exercises) {
            List<SetEntry> sets = new ArrayList<>();
            for (SetEntry s : e.sets) if (s.done) sets.add(s);
            if (!sets.isEmpty()) {
                e.sets = sets;
                kept.add(e);
            }
        }
        exercises = kept;
    }
}

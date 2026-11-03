package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.List;

/** Un exercice dans une séance, avec ses séries. */
public class ExerciseEntry {
    public long exerciseId;
    public List<SetEntry> sets = new ArrayList<>();

    public ExerciseEntry() {
    }

    public ExerciseEntry(long exerciseId) {
        this.exerciseId = exerciseId;
    }

    public ExerciseEntry copy(boolean keepDone) {
        ExerciseEntry e = new ExerciseEntry(exerciseId);
        for (SetEntry s : sets) e.sets.add(s.copy(keepDone));
        return e;
    }

    public int doneCount() {
        int n = 0;
        for (SetEntry s : sets) if (s.done) n++;
        return n;
    }
}

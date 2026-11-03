package com.fonte.carnet.core;

/** Une série : charge (kg) × répétitions. */
public class SetEntry {
    public double weight;
    public int reps;
    public boolean done;
    /** Série « max de répétitions » (ex. 5+ en 5/3/1). */
    public boolean amrap;
    /** Libellé facultatif : « Échauffement », « Tentative »… */
    public String label;

    public SetEntry() {
    }

    public SetEntry(double weight, int reps) {
        this.weight = weight;
        this.reps = reps;
    }

    public SetEntry copy(boolean keepDone) {
        SetEntry s = new SetEntry(weight, reps);
        s.done = keepDone && done;
        s.amrap = amrap;
        s.label = label;
        return s;
    }

    public double volume() {
        return weight * reps;
    }
}

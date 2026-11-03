package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.List;

/** Programme en cours : une suite de séances prescrites. */
public class Program {
    public String type;
    public String title;
    public String summary;
    public long startedAt;
    public List<Day> days = new ArrayList<>();

    public static class Day {
        /** Regroupement affiché, ex. « Semaine 1 ». */
        public String group;
        /** Nom court, ex. « J1 · 6×6 · 70 % ». */
        public String name;
        public List<ExerciseEntry> exercises = new ArrayList<>();
        /** Id de la séance qui a validé ce jour (0 si à faire). */
        public long sessionId;

        public boolean isDone() {
            return sessionId != 0;
        }
    }

    public int doneCount() {
        int n = 0;
        for (Day d : days) if (d.isDone()) n++;
        return n;
    }

    /** Index du premier jour non fait, ou -1 si le programme est terminé. */
    public int nextIndex() {
        for (int i = 0; i < days.size(); i++) if (!days.get(i).isDone()) return i;
        return -1;
    }

    public boolean finished() {
        return nextIndex() < 0;
    }
}

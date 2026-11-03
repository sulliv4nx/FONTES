package com.fonte.carnet.core;

import java.util.List;

/** Générateurs de programmes à pourcentages. */
public final class Programs {
    private Programs() {
    }

    public static final String SMOLOV_JR = "smolov_jr";
    public static final String WENDLER = "wendler_531";

    /** Smolov Jr : {séries, répétitions, % du max} pour les 4 séances de la semaine. */
    static final int[][] SMOLOV = {{6, 6, 70}, {7, 5, 75}, {8, 4, 80}, {10, 3, 85}};

    /** 5/3/1 : {%, reps} des 3 séries principales, semaine par semaine. */
    static final int[][][] W531 = {
            {{65, 5}, {75, 5}, {85, 5}},
            {{70, 3}, {80, 3}, {90, 3}},
            {{75, 5}, {85, 3}, {95, 1}},
            {{40, 5}, {50, 5}, {60, 5}},
    };
    static final String[] W531_NAMES = {"5/5/5+", "3/3/3+", "5/3/1+", "décharge"};

    /** Arrondit au multiple de step le plus proche (step ≤ 0 : pas d'arrondi). */
    public static double round(double w, double step) {
        if (step <= 0) return w;
        return Math.round(w / step) * step;
    }

    /**
     * Smolov Jr : 3 semaines × 4 séances (6×6 à 70 %, 7×5 à 75 %, 8×4 à 80 %, 10×3 à 85 %),
     * + incW2 kg en semaine 2, + incW3 kg en semaine 3, puis une séance de test du max.
     */
    public static Program smolovJr(Exercise ex, double oneRm, double incW2, double incW3, double step, long now) {
        Program p = new Program();
        p.type = SMOLOV_JR;
        p.title = "Smolov Jr · " + ex.name;
        p.summary = "Max " + Fmt.kg(oneRm) + " kg · +" + Fmt.kg(incW2) + " kg en S2 · +" + Fmt.kg(incW3) + " kg en S3";
        p.startedAt = now;
        double[] inc = {0, incW2, incW3};
        for (int w = 0; w < 3; w++) {
            for (int d = 0; d < SMOLOV.length; d++) {
                int sets = SMOLOV[d][0];
                int reps = SMOLOV[d][1];
                double load = round(oneRm * SMOLOV[d][2] / 100.0 + inc[w], step);
                Program.Day day = new Program.Day();
                day.group = "Semaine " + (w + 1);
                day.name = "J" + (d + 1) + " · " + Fmt.scheme(sets, reps) + " · " + Fmt.kg(load) + " kg";
                ExerciseEntry e = new ExerciseEntry(ex.id);
                for (int i = 0; i < sets; i++) e.sets.add(new SetEntry(load, reps));
                day.exercises.add(e);
                p.days.add(day);
            }
        }
        Program.Day test = new Program.Day();
        test.group = "Semaine 4";
        test.name = "Test du nouveau max";
        ExerciseEntry e = new ExerciseEntry(ex.id);
        int[][] warmup = {{50, 5}, {70, 3}, {80, 2}, {90, 1}};
        for (int[] wu : warmup) e.sets.add(labelled(round(oneRm * wu[0] / 100.0, step), wu[1], "Échauffement"));
        e.sets.add(labelled(round(oneRm, step), 1, "Ancien max"));
        e.sets.add(labelled(round(oneRm + incW3, step), 1, "Tentative"));
        test.exercises.add(e);
        p.days.add(test);
        return p;
    }

    /**
     * 5/3/1 de Wendler sur 4 semaines. Max d'entraînement = 90 % du 1RM.
     * Les mouvements dont le 1RM est ≤ 0 sont ignorés. bbb ajoute 5×10 à 50 % (semaines 1 à 3).
     * Renvoie null si aucun mouvement n'a de 1RM.
     */
    public static Program wendler(List<Exercise> lifts, double[] oneRms, boolean bbb, double step, long now) {
        Program p = new Program();
        p.type = WENDLER;
        p.title = "5/3/1 Wendler";
        p.summary = "Max d'entraînement = 90 % du 1RM" + (bbb ? " · BBB 5×10" : "");
        p.startedAt = now;
        for (int w = 0; w < W531.length; w++) {
            for (int i = 0; i < lifts.size(); i++) {
                if (oneRms[i] <= 0) continue;
                Exercise ex = lifts.get(i);
                double tm = oneRms[i] * 0.9;
                Program.Day day = new Program.Day();
                day.group = "Semaine " + (w + 1) + (w == 3 ? " · décharge" : "");
                day.name = ex.name + " · " + W531_NAMES[w];
                ExerciseEntry e = new ExerciseEntry(ex.id);
                for (int s = 0; s < 3; s++) {
                    SetEntry set = new SetEntry(round(tm * W531[w][s][0] / 100.0, step), W531[w][s][1]);
                    set.amrap = (w < 3 && s == 2);
                    e.sets.add(set);
                }
                if (bbb && w < 3) {
                    for (int s = 0; s < 5; s++) e.sets.add(labelled(round(tm * 0.5, step), 10, "BBB"));
                }
                day.exercises.add(e);
                p.days.add(day);
            }
        }
        return p.days.isEmpty() ? null : p;
    }

    private static SetEntry labelled(double w, int reps, String label) {
        SetEntry s = new SetEntry(w, reps);
        s.label = label;
        return s;
    }
}

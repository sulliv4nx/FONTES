package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Propose une séance de musculation selon les muscles choisis, l'objectif et la durée. */
public final class Generator {
    private Generator() {
    }

    public static final String STRENGTH = "Force";
    public static final String HYPERTROPHY = "Volume";
    public static final String ENDURANCE = "Endurance";
    public static final String[] GOALS = {STRENGTH, HYPERTROPHY, ENDURANCE};

    public static final int[] DURATIONS = {30, 45, 60, 75};

    /** Nombre d'exercices pour une durée donnée (≈ 10-12 min par exercice). */
    public static int exerciseCount(int minutes) {
        if (minutes <= 30) return 3;
        if (minutes <= 45) return 4;
        if (minutes <= 60) return 5;
        return 6;
    }

    /** {séries, répétitions} selon l'objectif. */
    public static int[] scheme(String goal, boolean compound) {
        if (STRENGTH.equals(goal)) return compound ? new int[]{5, 5} : new int[]{3, 8};
        if (ENDURANCE.equals(goal)) return compound ? new int[]{3, 15} : new int[]{3, 20};
        return compound ? new int[]{4, 8} : new int[]{3, 12};
    }

    /**
     * Choisit des exercices en tournant sur les muscles demandés : d'abord un polyarticulaire par muscle,
     * puis des exercices d'isolation. Polyarticulaires en tête de séance.
     */
    public static List<ExerciseEntry> propose(List<String> muscles, String goal, int minutes, long seed) {
        List<ExerciseEntry> out = new ArrayList<>();
        if (muscles == null || muscles.isEmpty()) return out;
        Random rnd = new Random(seed);
        List<Library.Target> all = Library.targets();
        int count = Math.max(exerciseCount(minutes), muscles.size());
        Set<Long> used = new HashSet<>();
        List<Library.Target> picked = new ArrayList<>();

        // Passes successives : polyarticulaire principal, isolation principale, puis muscle secondaire.
        for (int pass = 0; pass < 6 && picked.size() < count; pass++) {
            for (String m : muscles) {
                if (picked.size() >= count) break;
                boolean wantCompound = pass % 2 == 0;
                boolean primaryOnly = pass < 4;
                List<Library.Target> cands = new ArrayList<>();
                for (Library.Target t : all) {
                    if (used.contains(t.id)) continue;
                    boolean ok = primaryOnly ? t.primary().equals(m) : t.works(m);
                    if (ok && t.compound == wantCompound) cands.add(t);
                }
                if (cands.isEmpty()) {
                    for (Library.Target t : all) {
                        if (!used.contains(t.id) && (primaryOnly ? t.primary().equals(m) : t.works(m))) cands.add(t);
                    }
                }
                if (cands.isEmpty()) continue;
                Library.Target choice = cands.get(rnd.nextInt(cands.size()));
                used.add(choice.id);
                picked.add(choice);
            }
        }
        // Polyarticulaires d'abord (ordre stable sinon).
        List<Library.Target> ordered = new ArrayList<>();
        for (Library.Target t : picked) if (t.compound) ordered.add(t);
        for (Library.Target t : picked) if (!t.compound) ordered.add(t);
        for (Library.Target t : ordered) {
            int[] s = scheme(goal, t.compound);
            ExerciseEntry e = new ExerciseEntry(t.id);
            for (int i = 0; i < s[0]; i++) e.sets.add(new SetEntry(0, s[1]));
            out.add(e);
        }
        return out;
    }

    /** « Pectoraux + Triceps » */
    public static String title(List<String> muscles) {
        List<String> m = new ArrayList<>(muscles);
        Collections.sort(m, (a, b) -> Integer.compare(indexOf(a), indexOf(b)));
        StringBuilder sb = new StringBuilder();
        for (String s : m) {
            if (sb.length() > 0) sb.append(" + ");
            sb.append(s);
        }
        return sb.toString();
    }

    private static int indexOf(String muscle) {
        for (int i = 0; i < Library.MUSCLES.length; i++) if (Library.MUSCLES[i].equals(muscle)) return i;
        return 99;
    }
}

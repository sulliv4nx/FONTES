package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Séances types de musculation intégrées (ids négatifs, jamais enregistrées). */
public final class Templates {
    private Templates() {
    }

    public static final String FULL = "Corps complet";
    public static final String SPLIT = "Split par groupe musculaire";
    public static final String STRENGTH = "Force";

    public static final String[] GROUPS = {FULL, SPLIT, STRENGTH};

    private static List<Template> cache;

    /** {exerciseId, séries, reps} */
    private static Template t(long id, String group, String name, String description, long[][] plan) {
        Template t = new Template();
        t.id = id;
        t.group = group;
        t.name = name;
        t.description = description;
        for (long[] p : plan) {
            ExerciseEntry e = new ExerciseEntry(p[0]);
            for (int s = 0; s < p[1]; s++) e.sets.add(new SetEntry(0, (int) p[2]));
            t.exercises.add(e);
        }
        return t;
    }

    public static synchronized List<Template> all() {
        if (cache != null) return cache;
        List<Template> l = new ArrayList<>();
        l.add(t(-1, FULL, "Full body A", "Les grands mouvements, 3 fois par semaine en alternance avec B.",
                new long[][]{{Library.SQUAT, 3, 8}, {Library.BENCH, 3, 8}, {12, 3, 8}, {Library.OHP, 3, 10}, {23, 3, 12}}));
        l.add(t(-2, FULL, "Full body B", "À alterner avec la séance A.",
                new long[][]{{Library.DEADLIFT, 3, 5}, {5, 3, 10}, {11, 3, 8}, {18, 3, 10}, {21, 3, 12}}));
        l.add(t(-3, FULL, "Haut du corps", "Pour un programme haut / bas sur 4 jours.",
                new long[][]{{Library.BENCH, 4, 6}, {12, 4, 8}, {Library.OHP, 3, 8}, {11, 3, 8}, {21, 2, 12}, {48, 2, 12}}));
        l.add(t(-4, FULL, "Bas du corps", "Pour un programme haut / bas sur 4 jours.",
                new long[][]{{Library.SQUAT, 4, 6}, {17, 3, 8}, {18, 3, 10}, {20, 3, 10}, {52, 3, 15}}));
        l.add(t(-5, SPLIT, "Push · pecs, épaules, triceps", null,
                new long[][]{{Library.BENCH, 4, 8}, {40, 3, 10}, {41, 3, 10}, {10, 3, 15}, {49, 3, 12}}));
        l.add(t(-6, SPLIT, "Pull · dos, biceps", null,
                new long[][]{{11, 4, 8}, {12, 4, 8}, {45, 3, 12}, {15, 3, 15}, {46, 3, 12}}));
        l.add(t(-7, SPLIT, "Legs · jambes", null,
                new long[][]{{Library.SQUAT, 4, 6}, {17, 3, 8}, {19, 3, 12}, {51, 3, 12}, {52, 4, 15}}));
        l.add(t(-8, SPLIT, "Bras", null,
                new long[][]{{60, 3, 8}, {21, 3, 10}, {48, 3, 10}, {46, 3, 12}, {49, 3, 12}}));
        l.add(t(-9, SPLIT, "Fessiers", null,
                new long[][]{{20, 4, 10}, {54, 3, 10}, {17, 3, 10}, {59, 3, 15}}));
        l.add(t(-10, SPLIT, "Épaules & abdos", null,
                new long[][]{{Library.OHP, 4, 6}, {10, 3, 15}, {42, 3, 15}, {43, 3, 12}, {58, 3, 10}, {57, 3, 20}}));
        l.add(t(-11, STRENGTH, "Force 5×5 · A", "Ajoute 2,5 kg à chaque séance réussie.",
                new long[][]{{Library.SQUAT, 5, 5}, {Library.BENCH, 5, 5}, {12, 5, 5}}));
        l.add(t(-12, STRENGTH, "Force 5×5 · B", "À alterner avec la séance A.",
                new long[][]{{Library.SQUAT, 5, 5}, {Library.OHP, 5, 5}, {Library.DEADLIFT, 1, 5}}));
        l.add(t(-13, STRENGTH, "Strongman de base", "Gros mouvements, peu de répétitions.",
                new long[][]{{34, 5, 3}, {36, 4, 5}, {16, 4, 5}, {35, 5, 2}}));
        cache = Collections.unmodifiableList(l);
        return cache;
    }

    public static Template find(long id) {
        for (Template t : all()) if (t.id == id) return t;
        return null;
    }
}

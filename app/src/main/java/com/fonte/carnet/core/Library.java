package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.List;

/** Bibliothèque d'exercices intégrée. Les ids sont stables : ne jamais les réattribuer. */
public final class Library {
    private Library() {
    }

    public static final long BENCH = 1;
    public static final long SQUAT = 2;
    public static final long DEADLIFT = 3;
    public static final long OHP = 4;

    /** Les ids des exercices créés par l'utilisateur commencent ici. */
    public static final long FIRST_CUSTOM_ID = 1000;

    public static final String PUSH = "Poussée";
    public static final String PULL = "Tirage";
    public static final String LEGS = "Jambes";
    public static final String ARMS = "Bras & gainage";
    public static final String OLY = "Haltérophilie";
    public static final String CROSSFIT = "CrossFit";
    public static final String STRONGMAN = "Strongman";
    public static final String CUSTOM = "Mes exercices";

    public static final String[] CATEGORIES = {PUSH, PULL, LEGS, ARMS, OLY, CROSSFIT, STRONGMAN, CUSTOM};

    public static List<Exercise> defaults() {
        List<Exercise> l = new ArrayList<>();
        l.add(new Exercise(BENCH, "Développé couché", PUSH, false));
        l.add(new Exercise(SQUAT, "Squat", LEGS, false));
        l.add(new Exercise(DEADLIFT, "Soulevé de terre", LEGS, false));
        l.add(new Exercise(OHP, "Développé militaire", PUSH, false));
        l.add(new Exercise(5, "Développé incliné", PUSH, false));
        l.add(new Exercise(6, "Développé couché haltères", PUSH, false));
        l.add(new Exercise(7, "Dips", PUSH, false));
        l.add(new Exercise(8, "Pompes", PUSH, false));
        l.add(new Exercise(9, "Push press", PUSH, false));
        l.add(new Exercise(10, "Élévations latérales", PUSH, false));
        l.add(new Exercise(11, "Tractions", PULL, false));
        l.add(new Exercise(12, "Rowing barre", PULL, false));
        l.add(new Exercise(13, "Rowing haltère", PULL, false));
        l.add(new Exercise(14, "Tirage vertical", PULL, false));
        l.add(new Exercise(15, "Face pull", PULL, false));
        l.add(new Exercise(16, "Front squat", LEGS, false));
        l.add(new Exercise(17, "Soulevé de terre roumain", LEGS, false));
        l.add(new Exercise(18, "Fentes", LEGS, false));
        l.add(new Exercise(19, "Presse à cuisses", LEGS, false));
        l.add(new Exercise(20, "Hip thrust", LEGS, false));
        l.add(new Exercise(21, "Curl biceps", ARMS, false));
        l.add(new Exercise(22, "Extensions triceps", ARMS, false));
        l.add(new Exercise(23, "Relevés de jambes", ARMS, false));
        l.add(new Exercise(24, "Épaulé (power clean)", OLY, false));
        l.add(new Exercise(25, "Épaulé-jeté", OLY, false));
        l.add(new Exercise(26, "Arraché", OLY, false));
        l.add(new Exercise(27, "Thruster", CROSSFIT, false));
        l.add(new Exercise(28, "Kettlebell swing", CROSSFIT, false));
        l.add(new Exercise(29, "Wall ball", CROSSFIT, false));
        l.add(new Exercise(30, "Box jump", CROSSFIT, false));
        l.add(new Exercise(31, "Burpees", CROSSFIT, false));
        l.add(new Exercise(32, "Muscle-up", CROSSFIT, false));
        l.add(new Exercise(33, "Toes to bar", CROSSFIT, false));
        l.add(new Exercise(34, "Log press", STRONGMAN, false));
        l.add(new Exercise(35, "Pierre d'Atlas", STRONGMAN, false));
        l.add(new Exercise(36, "Soulevé de terre trap bar", STRONGMAN, false));
        l.add(new Exercise(37, "Axle press", STRONGMAN, false));
        return l;
    }
}

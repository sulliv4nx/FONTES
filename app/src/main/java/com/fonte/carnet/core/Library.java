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
        // v2 : exercices d'isolation pour les séances par muscle.
        l.add(new Exercise(38, "Écarté couché haltères", PUSH, false));
        l.add(new Exercise(39, "Écarté à la poulie", PUSH, false));
        l.add(new Exercise(40, "Développé incliné haltères", PUSH, false));
        l.add(new Exercise(41, "Développé épaules haltères", PUSH, false));
        l.add(new Exercise(42, "Oiseau (deltoïdes arrière)", PULL, false));
        l.add(new Exercise(43, "Shrugs", PULL, false));
        l.add(new Exercise(44, "Pull-over", PULL, false));
        l.add(new Exercise(45, "Tirage horizontal à la poulie", PULL, false));
        l.add(new Exercise(46, "Curl marteau", ARMS, false));
        l.add(new Exercise(47, "Curl pupitre", ARMS, false));
        l.add(new Exercise(48, "Barre au front", ARMS, false));
        l.add(new Exercise(49, "Extension triceps à la poulie", ARMS, false));
        l.add(new Exercise(50, "Leg extension", LEGS, false));
        l.add(new Exercise(51, "Leg curl", LEGS, false));
        l.add(new Exercise(52, "Mollets debout", LEGS, false));
        l.add(new Exercise(53, "Mollets assis", LEGS, false));
        l.add(new Exercise(54, "Squat bulgare", LEGS, false));
        l.add(new Exercise(55, "Goblet squat", LEGS, false));
        l.add(new Exercise(56, "Good morning", LEGS, false));
        l.add(new Exercise(57, "Crunch", ARMS, false));
        l.add(new Exercise(58, "Roue abdominale", ARMS, false));
        l.add(new Exercise(59, "Abduction de hanche", LEGS, false));
        l.add(new Exercise(60, "Développé couché prise serrée", PUSH, false));
        return l;
    }

    // ---------------------------------------------------------------- muscles

    public static final String PECS = "Pectoraux";
    public static final String BACK = "Dos";
    public static final String SHOULDERS = "Épaules";
    public static final String BICEPS = "Biceps";
    public static final String TRICEPS = "Triceps";
    public static final String QUADS = "Quadriceps";
    public static final String HAMS = "Ischio-jambiers";
    public static final String GLUTES = "Fessiers";
    public static final String CALVES = "Mollets";
    public static final String ABS = "Abdos";

    public static final String[] MUSCLES = {PECS, BACK, SHOULDERS, BICEPS, TRICEPS, QUADS, HAMS, GLUTES, CALVES, ABS};

    /** Exercice ciblé : muscles (le premier est le principal) et polyarticulaire ou non. */
    public static final class Target {
        public final long id;
        public final boolean compound;
        public final String[] muscles;

        Target(long id, boolean compound, String... muscles) {
            this.id = id;
            this.compound = compound;
            this.muscles = muscles;
        }

        public String primary() {
            return muscles[0];
        }

        public boolean works(String muscle) {
            for (String m : muscles) if (m.equals(muscle)) return true;
            return false;
        }
    }

    /** Exercices de musculation utilisables pour les propositions de séance. */
    public static List<Target> targets() {
        List<Target> t = new ArrayList<>();
        t.add(new Target(BENCH, true, PECS, TRICEPS, SHOULDERS));
        t.add(new Target(5, true, PECS, SHOULDERS));
        t.add(new Target(6, true, PECS, TRICEPS));
        t.add(new Target(40, true, PECS, SHOULDERS));
        t.add(new Target(7, true, TRICEPS, PECS));
        t.add(new Target(8, true, PECS, TRICEPS));
        t.add(new Target(38, false, PECS));
        t.add(new Target(39, false, PECS));
        t.add(new Target(OHP, true, SHOULDERS, TRICEPS));
        t.add(new Target(41, true, SHOULDERS, TRICEPS));
        t.add(new Target(10, false, SHOULDERS));
        t.add(new Target(42, false, SHOULDERS, BACK));
        t.add(new Target(15, false, SHOULDERS, BACK));
        t.add(new Target(11, true, BACK, BICEPS));
        t.add(new Target(12, true, BACK, BICEPS));
        t.add(new Target(13, true, BACK, BICEPS));
        t.add(new Target(14, true, BACK, BICEPS));
        t.add(new Target(45, true, BACK, BICEPS));
        t.add(new Target(44, false, BACK, PECS));
        t.add(new Target(43, false, BACK));
        t.add(new Target(21, false, BICEPS));
        t.add(new Target(46, false, BICEPS));
        t.add(new Target(47, false, BICEPS));
        t.add(new Target(60, true, TRICEPS, PECS));
        t.add(new Target(48, false, TRICEPS));
        t.add(new Target(49, false, TRICEPS));
        t.add(new Target(22, false, TRICEPS));
        t.add(new Target(SQUAT, true, QUADS, GLUTES));
        t.add(new Target(16, true, QUADS, GLUTES));
        t.add(new Target(19, true, QUADS, GLUTES));
        t.add(new Target(54, true, QUADS, GLUTES));
        t.add(new Target(55, true, QUADS, GLUTES));
        t.add(new Target(18, true, QUADS, GLUTES));
        t.add(new Target(50, false, QUADS));
        t.add(new Target(DEADLIFT, true, HAMS, GLUTES, BACK));
        t.add(new Target(17, true, HAMS, GLUTES));
        t.add(new Target(56, true, HAMS, BACK));
        t.add(new Target(51, false, HAMS));
        t.add(new Target(20, true, GLUTES, HAMS));
        t.add(new Target(59, false, GLUTES));
        t.add(new Target(52, false, CALVES));
        t.add(new Target(53, false, CALVES));
        t.add(new Target(23, false, ABS));
        t.add(new Target(57, false, ABS));
        t.add(new Target(58, false, ABS));
        return t;
    }

    /** Exercices au poids du corps (pas de charge à saisir par défaut). */
    public static boolean bodyweight(long id) {
        return id == 7 || id == 8 || id == 11 || id == 23 || id == 57 || id == 58;
    }
}

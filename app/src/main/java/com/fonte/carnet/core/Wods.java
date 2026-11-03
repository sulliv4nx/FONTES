package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bibliothèque de WOD intégrés. Charges RX indiquées « homme / femme » en kg
 * (conversions arrondies des standards en livres). Les ids sont stables : ne jamais les réattribuer.
 */
public final class Wods {
    private Wods() {
    }

    public static final String CARDIO = "Cardio";
    public static final String GYM = "Gymnastique";
    public static final String HALTERO = "Haltérophilie";
    public static final String FORCE = "Force";
    public static final String LEGS = "Jambes";
    public static final String UPPER = "Haut du corps";
    public static final String NO_GEAR = "Sans matériel";
    public static final String SHORT = "Court & intense";
    public static final String LONG = "Long & endurance";

    public static final String[] TAGS = {CARDIO, GYM, HALTERO, FORCE, LEGS, UPPER, NO_GEAR, SHORT, LONG};

    private static final String HERO_NOTE = "WOD Hero, créé en hommage à un membre des forces armées, de la police ou des secours mort en service. "
            + "Adapte charges et volume à ton niveau.";

    private static List<Wod> cache;

    private static String[] t(String... tags) {
        return tags;
    }

    private static Wod hero(String id, String name, String format, int minutes, String note, String[] tags, String... lines) {
        return new Wod(id, name, Wod.HERO, format, minutes, note == null ? HERO_NOTE : note + " " + HERO_NOTE, tags, lines);
    }

    private static Wod girl(String id, String name, String format, int minutes, String note, String[] tags, String... lines) {
        return new Wod(id, name, Wod.GIRL, format, minutes, note, tags, lines);
    }

    private static Wod fonte(String id, String name, String format, int minutes, String note, String[] tags, String... lines) {
        return new Wod(id, name, Wod.FONTE, format, minutes, note, tags, lines);
    }

    public static synchronized List<Wod> all() {
        if (cache != null) return cache;
        List<Wod> l = new ArrayList<>();
        String ft = Wod.FOR_TIME;
        String am = Wod.AMRAP;
        String em = Wod.EMOM;

        // ------------------------------------------------------------ WOD Heroes
        l.add(hero("murph", "Murph", ft, 0, "RX avec gilet lesté de 9 kg. Tu peux fractionner pull-ups, push-ups et squats (ex. 20 tours de 5-10-15), mais commence et termine par la course.",
                t(LONG, GYM, CARDIO), "Course 1,6 km", "100 pull-ups", "200 push-ups", "300 air squats", "Course 1,6 km"));
        l.add(hero("dt", "DT", ft, 0, null, t(HALTERO, FORCE),
                "5 tours :", "12 soulevés de terre", "9 hang power cleans", "6 push jerks", "(Barre à 70 / 47,5 kg)"));
        l.add(hero("jt", "JT", ft, 0, null, t(GYM, UPPER, NO_GEAR),
                "21-15-9 répétitions :", "Handstand push-ups", "Ring dips", "Push-ups"));
        l.add(hero("michael", "Michael", ft, 0, null, t(CARDIO, NO_GEAR),
                "3 tours :", "Course 800 m", "50 back extensions", "50 sit-ups"));
        l.add(hero("nate", "Nate", am, 20, null, t(GYM, UPPER),
                "2 muscle-ups", "4 handstand push-ups", "8 kettlebell swings (32 / 24 kg)"));
        l.add(hero("randy", "Randy", ft, 0, null, t(HALTERO, SHORT),
                "75 power snatches (34 / 25 kg)"));
        l.add(hero("tommy_v", "Tommy V", ft, 0, null, t(HALTERO, GYM),
                "21 thrusters", "12 grimpers de corde (4,5 m)", "15 thrusters", "9 grimpers de corde", "9 thrusters",
                "6 grimpers de corde", "(Thrusters à 52 / 35 kg)"));
        l.add(hero("badger", "Badger", ft, 0, null, t(HALTERO, CARDIO, LONG),
                "3 tours :", "30 squat cleans (43 / 30 kg)", "30 pull-ups", "Course 800 m"));
        l.add(hero("josh", "Josh", ft, 0, null, t(HALTERO, GYM),
                "21 overhead squats", "42 pull-ups", "15 overhead squats", "30 pull-ups", "9 overhead squats", "18 pull-ups",
                "(Overhead squats à 43 / 30 kg)"));
        l.add(hero("daniel", "Daniel", ft, 0, null, t(CARDIO, GYM, LONG),
                "50 pull-ups", "Course 400 m", "21 thrusters (43 / 30 kg)", "Course 800 m", "21 thrusters", "Course 400 m", "50 pull-ups"));
        l.add(hero("jason", "Jason", ft, 0, null, t(GYM, LEGS),
                "100 air squats", "5 muscle-ups", "75 air squats", "10 muscle-ups", "50 air squats", "15 muscle-ups",
                "25 air squats", "20 muscle-ups"));
        l.add(hero("ryan", "Ryan", ft, 0, null, t(GYM, CARDIO),
                "5 tours :", "7 muscle-ups", "21 burpees (toucher une cible 30 cm au-dessus de ta portée)"));
        l.add(hero("erin", "Erin", ft, 0, null, t(UPPER, GYM),
                "5 tours :", "15 split cleans à l'haltère (18 / 11 kg)", "21 pull-ups"));
        l.add(hero("mr_joshua", "Mr. Joshua", ft, 0, null, t(FORCE, CARDIO),
                "5 tours :", "Course 400 m", "30 GHD sit-ups", "15 soulevés de terre (113 / 79 kg)"));
        l.add(hero("danny", "Danny", am, 20, null, t(CARDIO, UPPER),
                "30 box jumps (60 / 50 cm)", "20 push presses (52 / 35 kg)", "30 pull-ups"));
        l.add(hero("hansen", "Hansen", ft, 0, null, t(CARDIO),
                "5 tours :", "30 kettlebell swings (32 / 24 kg)", "30 burpees", "30 GHD sit-ups"));
        l.add(hero("griff", "Griff", ft, 0, null, t(CARDIO, NO_GEAR),
                "Course 800 m", "Course 400 m en arrière", "Course 800 m", "Course 400 m en arrière"));
        l.add(hero("luce", "Luce", ft, 0, "RX avec gilet lesté de 9 kg.", t(LONG, GYM),
                "3 tours :", "Course 1 km", "10 muscle-ups", "100 air squats"));
        l.add(hero("wittman", "Wittman", ft, 0, null, t(CARDIO, HALTERO),
                "7 tours :", "15 kettlebell swings (24 / 16 kg)", "15 power cleans (43 / 30 kg)", "15 box jumps (60 / 50 cm)"));
        l.add(hero("mcghee", "McGhee", am, 30, null, t(FORCE, LONG),
                "5 soulevés de terre (125 / 84 kg)", "13 push-ups", "9 box jumps (60 / 50 cm)"));
        l.add(hero("jerry", "Jerry", ft, 0, null, t(CARDIO, LONG),
                "Course 1,6 km", "Rameur 2 000 m", "Course 1,6 km"));
        l.add(hero("the_seven", "The Seven", ft, 0, null, t(GYM, FORCE, LONG),
                "7 tours :", "7 handstand push-ups", "7 thrusters (61 / 43 kg)", "7 knees-to-elbows", "7 soulevés de terre (111 / 75 kg)",
                "7 burpees", "7 kettlebell swings (32 / 24 kg)", "7 pull-ups"));
        l.add(hero("rj", "RJ", ft, 0, null, t(CARDIO, UPPER),
                "5 tours :", "Course 800 m", "5 grimpers de corde (4,5 m)", "50 push-ups"));
        l.add(hero("whitten", "Whitten", ft, 0, null, t(CARDIO, LONG),
                "5 tours :", "22 kettlebell swings (32 / 24 kg)", "22 box jumps (60 / 50 cm)", "Course 400 m", "22 burpees",
                "22 wall balls (9 / 6 kg)"));
        l.add(hero("bull", "Bull", ft, 0, null, t(LONG, GYM),
                "2 tours :", "200 double unders", "50 overhead squats (61 / 43 kg)", "50 pull-ups", "Course 1,6 km"));
        l.add(hero("rankel", "Rankel", am, 20, null, t(FORCE, CARDIO),
                "6 soulevés de terre (102 / 70 kg)", "7 burpee pull-ups", "10 kettlebell swings (32 / 24 kg)", "Course 200 m"));
        l.add(hero("holbrook", "Holbrook", ft, 0, "Chaque tour est chronométré, avec 1 min de repos entre les tours.", t(HALTERO, CARDIO),
                "10 tours :", "5 thrusters (52 / 35 kg)", "10 pull-ups", "Sprint 100 m", "Repos 1 min"));
        l.add(hero("abbate", "Abbate", ft, 0, null, t(HALTERO, CARDIO, LONG),
                "Course 1,6 km", "21 clean & jerks (70 / 47,5 kg)", "Course 800 m", "21 clean & jerks", "Course 1,6 km"));
        l.add(hero("moore", "Moore", am, 20, "Score : tours complets (note le total de HSPU).", t(GYM, CARDIO),
                "1 grimper de corde (4,5 m)", "Course 400 m", "Max handstand push-ups"));
        l.add(hero("wilmot", "Wilmot", ft, 0, null, t(GYM, LEGS),
                "6 tours :", "50 air squats", "25 ring dips"));
        l.add(hero("small", "Small", ft, 0, null, t(CARDIO, LONG),
                "3 tours :", "Rameur 1 000 m", "50 burpees", "50 box jumps (60 / 50 cm)", "Course 800 m"));
        l.add(hero("morrison", "Morrison", ft, 0, null, t(CARDIO, LEGS),
                "50-40-30-20-10 répétitions :", "Wall balls (9 / 6 kg)", "Box jumps (60 / 50 cm)", "Kettlebell swings (24 / 16 kg)"));
        l.add(hero("manion", "Manion", ft, 0, null, t(LEGS, CARDIO),
                "7 tours :", "Course 400 m", "29 back squats (61 / 43 kg)"));
        l.add(hero("gator", "Gator", ft, 0, null, t(FORCE, LEGS, UPPER),
                "8 tours :", "5 front squats (84 / 57 kg)", "26 ring push-ups"));
        l.add(hero("bradshaw", "Bradshaw", ft, 0, null, t(GYM, FORCE),
                "10 tours :", "3 handstand push-ups", "6 soulevés de terre (102 / 70 kg)", "12 pull-ups", "24 double unders"));
        l.add(hero("kalsu", "Kalsu", ft, 0, "Lance le chrono, fais 5 burpees, puis enchaîne les thrusters. Au début de chaque minute, 5 burpees.",
                t(HALTERO, LONG), "100 thrusters (61 / 43 kg)", "5 burpees au début de chaque minute"));
        l.add(hero("roy", "Roy", ft, 0, null, t(FORCE, GYM),
                "5 tours :", "15 soulevés de terre (102 / 70 kg)", "20 box jumps (60 / 50 cm)", "25 pull-ups"));
        l.add(hero("coe", "Coe", ft, 0, null, t(HALTERO, UPPER),
                "10 tours :", "10 thrusters (43 / 30 kg)", "10 ring push-ups"));
        l.add(hero("glen", "Glen", ft, 0, null, t(LONG, HALTERO),
                "30 clean & jerks (61 / 43 kg)", "Course 1,6 km", "10 grimpers de corde (4,5 m)", "Course 1,6 km", "100 burpees"));
        l.add(hero("lumberjack_20", "Lumberjack 20", ft, 0, null, t(LONG, CARDIO),
                "20 soulevés de terre (125 / 84 kg)", "Course 400 m", "20 kettlebell swings (32 / 24 kg)", "Course 400 m",
                "20 overhead squats (52 / 35 kg)", "Course 400 m", "20 burpees", "Course 400 m", "20 chest-to-bar pull-ups",
                "Course 400 m", "20 box jumps (60 / 50 cm)", "Course 400 m", "20 squat cleans aux haltères (20 / 14 kg)", "Course 400 m"));
        l.add(hero("jack", "Jack", am, 20, null, t(CARDIO, HALTERO),
                "10 push presses (52 / 35 kg)", "10 kettlebell swings (24 / 16 kg)", "10 box jumps (60 / 50 cm)"));
        l.add(hero("jared", "Jared", ft, 0, null, t(GYM, UPPER, LONG),
                "4 tours :", "Course 800 m", "40 pull-ups", "70 push-ups"));
        l.add(hero("dork", "Dork", ft, 0, null, t(CARDIO),
                "6 tours :", "60 double unders", "30 kettlebell swings (24 / 16 kg)", "15 burpees"));

        // ------------------------------------------------------------ The Girls
        l.add(girl("fran", "Fran", ft, 0, "Un classique très court et très intense.", t(HALTERO, GYM, SHORT),
                "21-15-9 répétitions :", "Thrusters (43 / 30 kg)", "Pull-ups"));
        l.add(girl("grace", "Grace", ft, 0, null, t(HALTERO, SHORT),
                "30 clean & jerks (61 / 43 kg)"));
        l.add(girl("isabel", "Isabel", ft, 0, null, t(HALTERO, SHORT),
                "30 snatches (61 / 43 kg)"));
        l.add(girl("helen", "Helen", ft, 0, null, t(CARDIO),
                "3 tours :", "Course 400 m", "21 kettlebell swings (24 / 16 kg)", "12 pull-ups"));
        l.add(girl("cindy", "Cindy", am, 20, null, t(GYM, UPPER),
                "5 pull-ups", "10 push-ups", "15 air squats"));
        l.add(girl("annie", "Annie", ft, 0, null, t(CARDIO, SHORT),
                "50-40-30-20-10 répétitions :", "Double unders", "Sit-ups"));
        l.add(girl("karen", "Karen", ft, 0, null, t(LEGS, SHORT),
                "150 wall balls (9 / 6 kg)"));
        l.add(girl("diane", "Diane", ft, 0, null, t(FORCE, GYM, SHORT),
                "21-15-9 répétitions :", "Soulevés de terre (102 / 70 kg)", "Handstand push-ups"));
        l.add(girl("elizabeth", "Elizabeth", ft, 0, null, t(HALTERO, UPPER),
                "21-15-9 répétitions :", "Cleans (61 / 43 kg)", "Ring dips"));
        l.add(girl("jackie", "Jackie", ft, 0, null, t(CARDIO),
                "Rameur 1 000 m", "50 thrusters (barre de 20 / 15 kg)", "30 pull-ups"));
        l.add(girl("nancy", "Nancy", ft, 0, null, t(CARDIO, HALTERO),
                "5 tours :", "Course 400 m", "15 overhead squats (43 / 30 kg)"));
        l.add(girl("amanda", "Amanda", ft, 0, null, t(GYM, HALTERO, SHORT),
                "9-7-5 répétitions :", "Muscle-ups", "Squat snatches (61 / 43 kg)"));
        l.add(girl("kelly", "Kelly", ft, 0, null, t(CARDIO, LEGS, LONG),
                "5 tours :", "Course 400 m", "30 box jumps (60 / 50 cm)", "30 wall balls (9 / 6 kg)"));
        l.add(girl("eva", "Eva", ft, 0, null, t(LONG, CARDIO),
                "5 tours :", "Course 800 m", "30 kettlebell swings (32 / 24 kg)", "30 pull-ups"));
        l.add(girl("barbara", "Barbara", ft, 0, "3 min de repos entre les tours.", t(GYM, NO_GEAR),
                "5 tours :", "20 pull-ups", "30 push-ups", "40 sit-ups", "50 air squats", "Repos 3 min"));
        l.add(girl("chelsea", "Chelsea", em, 30, "Si tu ne finis pas les 3 mouvements dans la minute, le WOD s'arrête : note ton nombre de tours.",
                t(GYM, UPPER), "5 pull-ups", "10 push-ups", "15 air squats"));
        l.add(girl("mary", "Mary", am, 20, null, t(GYM),
                "5 handstand push-ups", "10 pistols (alternés)", "15 pull-ups"));
        l.add(girl("angie", "Angie", ft, 0, "Termine toutes les répétitions d'un mouvement avant de passer au suivant.", t(GYM, LONG),
                "100 pull-ups", "100 push-ups", "100 sit-ups", "100 air squats"));
        l.add(girl("nicole", "Nicole", am, 20, "Score : total de pull-ups (note-le en commentaire).", t(GYM, CARDIO),
                "Course 400 m", "Max pull-ups"));

        // ------------------------------------------------------------ WOD Fonte, par objectif
        l.add(fonte("moteur_16", "Moteur 16", am, 16, "Allure régulière : vise des tours à temps constant.", t(CARDIO),
                "Course 400 m", "15 kettlebell swings (24 / 16 kg)", "10 burpees"));
        l.add(fonte("roue_cardio", "Roue cardio", em, 20, "Le 4e intervalle est un vrai repos.", t(CARDIO, NO_GEAR),
                "Min 1 : 15 calories au rameur (ou 40 jumping jacks)", "Min 2 : 12 burpees", "Min 3 : 40 mountain climbers",
                "Min 4 : repos").alt());
        l.add(fonte("echelle_500", "Échelle 500", ft, 25, null, t(CARDIO, LEGS),
                "3 tours :", "Rameur 500 m", "20 box jump overs (60 / 50 cm)", "15 wall balls (9 / 6 kg)"));
        l.add(fonte("barre_anneaux", "Barre & anneaux", em, 15, "Qualité avant tout : mouvements stricts.", t(GYM, UPPER),
                "Min 1 : 5 à 8 pull-ups stricts", "Min 2 : 10 push-ups déclinés", "Min 3 : 30 s de hollow hold").alt());
        l.add(fonte("kipping_12", "Kipping 12", am, 12, null, t(GYM),
                "5 chest-to-bar pull-ups", "10 handstand push-ups (ou pike push-ups)", "15 air squats"));
        l.add(fonte("toes_dips", "Core & dips", ft, 15, null, t(GYM, UPPER),
                "10 tours :", "5 toes to bar", "10 dips", "15 push-ups"));
        l.add(fonte("clean_ladder", "Échelle d'épaulé", em, 10, "Monte la charge chaque minute tant que la technique reste propre.", t(HALTERO, FORCE),
                "3 power cleans"));
        l.add(fonte("snatch_go", "Snatch & Go", ft, 10, null, t(HALTERO, SHORT),
                "21-15-9 répétitions :", "Power snatches (43 / 30 kg)", "Burpees par-dessus la barre"));
        l.add(fonte("jerk_complex", "Complexe jeté", em, 12, null, t(HALTERO, UPPER),
                "Min 1 : 3 push jerks (lourd)", "Min 2 : 6 front squats (60 % du max)", "Min 3 : 9 toes to bar").alt());
        l.add(fonte("force_souffle", "Force & souffle", em, 15, "Toutes les 3 min, 5 tours. Squats à environ 80 % de ton max.", t(FORCE, LEGS),
                "5 back squats lourds", "10 box jumps (60 / 50 cm)", "Course 200 m").every(180));
        l.add(fonte("deadlift_express", "Deadlift express", am, 10, null, t(FORCE, SHORT),
                "5 soulevés de terre (100 / 70 kg)", "10 burpees"));
        l.add(fonte("jambes_feu", "Jambes de feu", ft, 20, null, t(LEGS, CARDIO),
                "4 tours :", "20 wall balls (9 / 6 kg)", "20 fentes marchées", "20 box jumps (60 / 50 cm)"));
        l.add(fonte("front_run", "Front & run", am, 15, null, t(LEGS, HALTERO),
                "10 front squats (50 / 35 kg)", "10 box jumps (60 / 50 cm)", "Course 200 m"));
        l.add(fonte("upper_20", "Upper 20", am, 20, null, t(UPPER),
                "10 push-ups", "10 ring rows", "10 push presses (40 / 30 kg)", "10 sit-ups"));
        l.add(fonte("chambre_hotel", "Chambre d'hôtel", am, 20, "Zéro matériel, idéal en déplacement.", t(NO_GEAR, CARDIO),
                "10 burpees", "20 air squats", "30 mountain climbers", "40 jumping jacks"));
        l.add(fonte("tabata_total", "Tabata total", Wod.TABATA, 4, "20 s d'effort, 10 s de repos, 8 tours. Score : total de répétitions.", t(NO_GEAR, SHORT),
                "Tours impairs : air squats", "Tours pairs : push-ups").tours(8));
        l.add(fonte("les_300", "Les 300", ft, 30, "Fractionne comme tu veux.", t(NO_GEAR, LONG),
                "100 air squats", "100 sit-ups", "100 push-ups"));
        l.add(fonte("sprint_7", "Sprint 7", ft, 7, null, t(SHORT, CARDIO),
                "21-15-9 répétitions :", "Kettlebell swings (24 / 16 kg)", "Burpees"));
        l.add(fonte("tabata_wb", "Tabata wall ball", Wod.TABATA, 4, "Score : total de répétitions.", t(SHORT, LEGS),
                "Wall balls (9 / 6 kg)").tours(8));
        l.add(fonte("chipper_40", "Chipper 40", ft, 40, null, t(LONG, CARDIO),
                "50 double unders", "40 kettlebell swings (24 / 16 kg)", "30 box jumps (60 / 50 cm)", "20 power cleans (52 / 35 kg)",
                "10 handstand push-ups", "20 power cleans", "30 box jumps", "40 kettlebell swings", "50 double unders"));
        cache = Collections.unmodifiableList(l);
        return cache;
    }

    public static Wod find(String id) {
        for (Wod w : all()) if (w.id.equals(id)) return w;
        return null;
    }

    public static List<Wod> byKind(String kind) {
        List<Wod> out = new ArrayList<>();
        for (Wod w : all()) if (w.kind.equals(kind)) out.add(w);
        return out;
    }

    public static List<Wod> byTag(String tag) {
        List<Wod> out = new ArrayList<>();
        for (Wod w : all()) if (w.tags.contains(tag)) out.add(w);
        return out;
    }
}

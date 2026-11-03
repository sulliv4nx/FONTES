package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Circuits de renforcement musculaire intégrés (poids du corps, élastique léger au besoin). */
public final class Routines {
    private Routines() {
    }

    public static final String CORE = "Gainage & abdos";
    public static final String SHOULDERS = "Épaules";
    public static final String KNEES = "Genoux & chevilles";
    public static final String BACK = "Dos & posture";
    public static final String HIPS = "Hanches & fessiers";
    public static final String MOBILITY = "Mobilité";
    public static final String FULL = "Full body";

    public static final String[] FOCUS = {CORE, SHOULDERS, KNEES, BACK, HIPS, MOBILITY, FULL};

    private static List<Routine> cache;

    public static synchronized List<Routine> all() {
        if (cache != null) return cache;
        List<Routine> l = new ArrayList<>();

        l.add(new Routine("gainage_express", "Gainage express", CORE, 3, 15, 45,
                "Un gainage complet en 10 minutes, à placer en fin de séance.")
                .t("Gainage ventral", 40, "Coudes sous les épaules, corps aligné, serre abdos et fessiers.")
                .t("Gainage latéral droit", 30, "Hanches hautes, épaule bien au-dessus du coude.")
                .t("Gainage latéral gauche", 30, "Hanches hautes, épaule bien au-dessus du coude.")
                .r("Dead bug", 12, "Bas du dos plaqué au sol, allonge lentement bras et jambe opposés.")
                .t("Hollow hold", 25, "Bas du dos collé au sol, bras et jambes tendus. Plie les genoux si besoin."));

        l.add(new Routine("sangle_abdo", "Sangle abdominale", CORE, 2, 15, 60,
                "Abdos profonds et obliques, sans matériel.")
                .r("Bird dog", 20, "À quatre pattes, tends bras et jambe opposés sans bouger le dos. Alterne.")
                .r("Crunch inversé", 15, "Enroule le bassin vers la poitrine, sans élan.")
                .t("Shoulder taps en planche", 30, "Pieds écartés, bassin immobile, touche l'épaule opposée.")
                .r("Russian twist", 20, "Buste incliné en arrière, tourne les épaules, pas seulement les bras.")
                .t("Gainage ventral", 45, "Corps aligné, respiration calme."));

        l.add(new Routine("coiffe", "Épaules solides", SHOULDERS, 2, 20, 60,
                "Coiffe des rotateurs et omoplates, avec un élastique léger. Idéal avant le haut du corps ou les WOD de gymnastique.")
                .r("Rotation externe à l'élastique", 15, "Coude collé au corps, ouvre l'avant-bras vers l'extérieur.")
                .r("Face pull à l'élastique", 15, "Tire vers le front, coudes hauts, serre les omoplates.")
                .r("Pull-apart à l'élastique", 15, "Bras tendus, écarte l'élastique jusqu'à la poitrine.")
                .r("Y-T-W au sol", 10, "À plat ventre, lève les bras en Y, puis T, puis W, pouces vers le ciel.")
                .r("Pompes scapulaires", 12, "Bras tendus en planche, rapproche puis écarte les omoplates."));

        l.add(new Routine("epaules_mobiles", "Épaules mobiles", SHOULDERS, 1, 10, 0,
                "Amplitude des épaules pour l'overhead : snatch, jerk, handstand.")
                .t("Cercles de bras", 30, "Petits puis grands cercles, dans les deux sens.")
                .r("Dislocations au bâton", 12, "Bras tendus, passe le bâton de l'avant vers l'arrière, sans forcer.")
                .t("Étirement pectoral au mur (droit)", 30, "Avant-bras contre le mur, tourne doucement le buste.")
                .t("Étirement pectoral au mur (gauche)", 30, "Avant-bras contre le mur, tourne doucement le buste.")
                .t("Suspension passive à la barre", 30, "Épaules relâchées, respire. Pieds au sol si besoin.")
                .t("Posture de l'enfant, bras tendus", 40, "Fesses vers les talons, mains loin devant."));

        l.add(new Routine("genoux", "Genoux solides", KNEES, 2, 20, 60,
                "Quadriceps, fessiers et contrôle du genou : prévention pour squats, sauts et course.")
                .t("Chaise contre le mur", 40, "Cuisses parallèles au sol, genoux au-dessus des chevilles.")
                .r("Fente arrière lente", 20, "Descente en 3 secondes, genou avant aligné avec le pied. Alterne.")
                .r("Descente de marche", 20, "Debout sur une marche, descends lentement le talon opposé. 10 par jambe.")
                .r("Pont fessier sur une jambe", 20, "Pousse dans le talon, bassin bien horizontal. 10 par jambe.")
                .r("Montées sur pointes", 20, "Monte haut, redescends lentement."));

        l.add(new Routine("chevilles", "Chevilles & pieds", KNEES, 2, 10, 45,
                "Stabilité et mobilité des chevilles (box jumps, double unders, course).")
                .t("Équilibre sur la jambe droite", 30, "Genou légèrement fléchi. Ferme les yeux pour corser.")
                .t("Équilibre sur la jambe gauche", 30, "Genou légèrement fléchi. Ferme les yeux pour corser.")
                .r("Montées sur pointes", 20, "Lentement, sur toute l'amplitude.")
                .t("Marche sur les talons", 30, "Pointes des pieds relevées.")
                .r("Genou au mur", 20, "Pied à une main du mur, avance le genou vers le mur sans décoller le talon. Alterne."));

        l.add(new Routine("dos", "Dos & lombaires", BACK, 2, 15, 45,
                "Renforce l'arrière du corps et soulage le bas du dos.")
                .r("Superman", 12, "À plat ventre, décolle bras et jambes 2 s, regard vers le sol.")
                .r("Bird dog", 20, "Dos immobile, bras et jambe opposés tendus. Alterne.")
                .r("Pont fessier", 15, "Pousse dans les talons, serre les fessiers en haut, sans cambrer.")
                .r("Chat-vache", 10, "À quatre pattes, enroule puis creuse le dos lentement.")
                .t("Gainage ventral", 40, "Bassin neutre, ni creusé ni relevé."));

        l.add(new Routine("posture", "Posture & bureau", BACK, 2, 10, 30,
                "Contre les journées assises : haut du dos, nuque et hanches.")
                .r("Rentré de menton", 10, "Recule la tête sans baisser le regard, comme pour faire un double menton.")
                .r("W au sol", 12, "À plat ventre, coudes pliés, serre les omoplates en levant les bras.")
                .t("Étirement des fléchisseurs de hanche (droit)", 30, "Genou au sol, serre le fessier et avance le bassin.")
                .t("Étirement des fléchisseurs de hanche (gauche)", 30, "Genou au sol, serre le fessier et avance le bassin.")
                .r("Rotation thoracique à quatre pattes", 10, "Main derrière la tête, ouvre le coude vers le plafond. Par côté."));

        l.add(new Routine("fessiers", "Hanches & fessiers", HIPS, 3, 15, 45,
                "Fessiers et stabilisateurs de hanche, utiles pour le squat et le soulevé de terre.")
                .r("Pont fessier", 20, "Serre les fessiers 1 s en haut.")
                .r("Clamshell côté droit", 15, "Allongé sur le côté, genoux fléchis, ouvre le genou sans rouler le bassin.")
                .r("Clamshell côté gauche", 15, "Allongé sur le côté, genoux fléchis, ouvre le genou sans rouler le bassin.")
                .r("Fire hydrant", 20, "À quatre pattes, ouvre le genou sur le côté, dos immobile. Alterne.")
                .r("Squat sumo lent", 15, "Pieds larges, descente en 3 secondes, genoux vers l'extérieur."));

        l.add(new Routine("mobilite_matin", "Mobilité du matin", MOBILITY, 1, 5, 0,
                "10 minutes pour dérouiller tout le corps.")
                .t("Chat-vache", 45, "Mouvement lent, calé sur la respiration.")
                .t("Rotations thoraciques", 45, "À quatre pattes, ouvre un bras vers le ciel puis l'autre.")
                .t("Fente basse avec rotation", 60, "Coude vers le pied avant, puis ouvre le bras vers le ciel. Change de côté à mi-temps.")
                .t("Squat profond tenu", 45, "Talons au sol, coudes qui poussent les genoux vers l'extérieur.")
                .t("Pigeon (droit)", 40, "Bassin face au sol, respire dans l'étirement.")
                .t("Pigeon (gauche)", 40, "Bassin face au sol, respire dans l'étirement.")
                .t("Étirement des ischios", 45, "Jambes tendues, dos long, penche-toi depuis les hanches."));

        l.add(new Routine("mobilite_wod", "Mobilité avant WOD", MOBILITY, 1, 5, 0,
                "Échauffement articulaire hanches et épaules avant un WOD ou une séance lourde.")
                .t("Cercles de bras", 30, "Grands cercles dans les deux sens.")
                .r("Dislocations au bâton", 12, "Bras tendus, sans forcer.")
                .r("Cossack squat", 10, "Transfert latéral, jambe tendue, talon au sol. Alterne.")
                .r("Fentes avec rotation", 10, "Tourne le buste vers la jambe avant. Alterne.")
                .t("Squat profond tenu", 40, "Pousse les genoux vers l'extérieur avec les coudes.")
                .r("Inchworm", 6, "Marche avec les mains jusqu'à la planche, puis reviens."));

        l.add(new Routine("full_pdc", "Full body poids du corps", FULL, 3, 15, 60,
                "Renfo général sans matériel, à la maison ou en déplacement.")
                .r("Air squats", 20, "Hanches sous les genoux en bas, talons au sol.")
                .r("Pompes", 12, "Corps gainé. Sur les genoux si besoin.")
                .r("Fentes alternées", 20, "Grand pas, genou arrière qui frôle le sol.")
                .r("Superman", 12, "Décolle bras et jambes 2 s.")
                .t("Gainage ventral", 40, "Corps aligné.")
                .t("Mountain climbers", 30, "Rythme soutenu, bassin bas."));

        l.add(new Routine("prevention_course", "Prévention course", FULL, 2, 15, 45,
                "Chevilles, genoux, hanches et gainage pour courir sans pépins.")
                .r("Montées sur pointes", 20, "Lentement, sur toute l'amplitude.")
                .r("Pont fessier sur une jambe", 20, "10 par jambe, bassin horizontal.")
                .t("Équilibre sur une jambe", 40, "Change de jambe à mi-temps.")
                .r("Fente arrière", 20, "Genou avant stable. Alterne.")
                .t("Gainage latéral droit", 30, "Hanches hautes.")
                .t("Gainage latéral gauche", 30, "Hanches hautes."));

        cache = Collections.unmodifiableList(l);
        return cache;
    }

    public static Routine find(String id) {
        for (Routine r : all()) if (r.id.equals(id)) return r;
        return null;
    }

    public static List<Routine> byFocus(String focus) {
        List<Routine> out = new ArrayList<>();
        for (Routine r : all()) if (r.focus.equals(focus)) out.add(r);
        return out;
    }
}

package com.fonte.carnet.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** Suivi corporel : pesées, mensurations, eau, activités hors séances. */
public final class BodyLog {
    private BodyLog() {
    }

    public static class Weight {
        public long id;
        public int day;
        public long time;
        public double kg;
    }

    public static class Measures {
        public long id;
        public int day;
        public long time;
        /** Mesure → valeur (cm, ou % pour la masse grasse). */
        public final Map<String, Double> values = new LinkedHashMap<>();
    }

    public static class Water {
        public long id;
        public int day;
        public long time;
        public int ml;
    }

    /** Activité notée à la main : hors séance (onglet Nutrition) ou cardio d'une séance (vélo, rameur…). */
    public static class Activity {
        public long id;
        public int day;
        public long time;
        public String name;
        public int minutes;
        /** Calories nettes (repos déduit). */
        public int kcal;
        /** Cardio d'une séance : type (voir {@link Cardio}), sinon null. */
        public String kind;
        /** 0 facile, 1 modéré, 2 intense, -1 non précisé. */
        public int intensity = -1;
        public int meters;
        /** Calories affichées par l'ergo ou la montre (0 = non notées). */
        public int shownKcal;
        /** Discipline (Cardio.MUSCU, CROSSFIT, RENFO) ; null = activité hors séance. */
        public String sport;
        /** Séance de musculation à laquelle le cardio appartient (0 = aucune). */
        public long sessionId;

        public boolean training() {
            return sport != null;
        }
    }

    public static final String WAIST = "Tour de taille";
    public static final String HIPS = "Hanches";
    public static final String CHEST = "Poitrine";
    public static final String SHOULDERS = "Épaules";
    public static final String ARM = "Bras";
    public static final String THIGH = "Cuisse";
    public static final String CALF = "Mollet";
    public static final String NECK = "Cou";
    public static final String BODY_FAT = "Masse grasse (%)";

    public static final String[] MEASURES = {WAIST, HIPS, CHEST, SHOULDERS, ARM, THIGH, CALF, NECK, BODY_FAT};

    public static String unit(String measure) {
        return BODY_FAT.equals(measure) ? "%" : "cm";
    }
}

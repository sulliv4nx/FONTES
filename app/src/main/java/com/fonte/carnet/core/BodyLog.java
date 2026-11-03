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

    public static class Activity {
        public long id;
        public int day;
        public long time;
        public String name;
        public int minutes;
        public int kcal;
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

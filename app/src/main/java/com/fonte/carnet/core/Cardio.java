package com.fonte.carnet.core;

/**
 * Cardio et ergos notés à la main dans une séance (vélo 20 min, rameur, SkiErg…). Les calories nettes viennent
 * des MET moyens du Compendium des activités physiques selon l'intensité, ou des calories affichées par l'ergo
 * (le repos, déjà compté dans le métabolisme, est alors déduit).
 */
public final class Cardio {
    private Cardio() {
    }

    /** Discipline à laquelle l'activité est rattachée. */
    public static final String MUSCU = "muscu";
    public static final String CROSSFIT = "crossfit";
    public static final String RENFO = "renfo";

    public static final String[] IDS = {"velo_appart", "air_bike", "rameur", "skierg", "course", "tapis", "corde", "elliptique",
            "velo", "natation", "marche", "autre"};
    public static final String[] NAMES = {"Vélo d'appartement", "Air bike", "Rameur", "SkiErg", "Course à pied", "Tapis de course",
            "Corde à sauter", "Elliptique", "Vélo", "Natation", "Marche", "Autre"};
    /** Sous-titre court (exemples de machines). */
    public static final String[] HINTS = {"BikeErg, vélo de biking", "Assault, Echo", "Concept2", "", "", "", "", "", "Route, VTT",
            "", "Marche rapide, en côte", ""};
    /** MET facile / modéré / intense. */
    private static final double[][] METS = {
            {4.8, 6.8, 8.8},   // vélo d'appartement : 50-90 W, 90-100 W, 100-160 W
            {6.8, 8.8, 11.0},  // air bike
            {4.8, 7.0, 8.5},   // rameur : léger, 100 W, 150 W
            {5.0, 6.8, 9.0},   // SkiErg (machine de ski de fond)
            {8.3, 9.8, 11.5},  // course : 8, 10, 12 km/h
            {8.3, 9.8, 11.5},  // tapis
            {8.8, 11.8, 12.3}, // corde : lent, modéré, rapide
            {4.0, 5.0, 6.5},   // elliptique
            {6.8, 8.0, 10.0},  // vélo : 16-19, 19-22, 22-25 km/h
            {5.8, 8.3, 9.8},   // natation (crawl)
            {3.0, 4.3, 6.0},   // marche
            {4.0, 6.0, 8.0}};  // autre
    /** Unité de distance proposée : « m », « km » ou null (pas de distance). */
    private static final String[] UNITS = {"km", "km", "m", "m", "km", "km", null, "km", "km", "m", "km", null};

    public static final String[] INTENSITIES = {"Facile", "Modéré", "Intense"};

    public static int index(String id) {
        if (id == null) return IDS.length - 1;
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
        return IDS.length - 1;
    }

    public static String name(String id) {
        return NAMES[index(id)];
    }

    public static String unit(String id) {
        return UNITS[index(id)];
    }

    public static double met(String id, int intensity) {
        return METS[index(id)][Math.max(0, Math.min(2, intensity))];
    }

    /**
     * Calories nettes : celles de l'ergo moins le repos si elles sont données, sinon d'après le MET.
     *
     * @param shown calories affichées par l'ergo ou la montre (0 = inconnues)
     */
    public static int netKcal(String id, int intensity, int minutes, int shown, double weight) {
        if (minutes <= 0) return 0;
        if (shown > 0) return Math.max(0, shown - Nutrition.kcal(1.0, weight, minutes));
        return Nutrition.netKcal(met(id, intensity), weight, minutes);
    }

    /** Distance saisie (en m) affichée dans l'unité de l'activité : « 3 000 m », « 6,2 km ». */
    public static String distance(String id, int meters) {
        if (meters <= 0) return null;
        if ("m".equals(unit(id))) return group(meters) + " m";
        return Fmt.kg(Math.round(meters / 100.0) / 10.0) + " km";
    }

    /** Distance tapée par l'utilisateur dans l'unité de l'activité → mètres. */
    public static int meters(String id, double typed) {
        if (typed <= 0) return 0;
        return "m".equals(unit(id)) ? (int) Math.round(typed) : (int) Math.round(typed * 1000);
    }

    private static String group(int n) {
        String s = String.valueOf(n);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) b.append(' ');
            b.append(s.charAt(i));
        }
        return b.toString();
    }

    /** « 20 min · 6,2 km · modéré · ≈ 180 kcal » */
    public static String summary(BodyLog.Activity a) {
        StringBuilder b = new StringBuilder();
        b.append(a.minutes).append(" min");
        String d = distance(a.kind, a.meters);
        if (d != null) b.append(" · ").append(d);
        if (a.intensity >= 0 && a.intensity < 3) b.append(" · ").append(INTENSITIES[a.intensity].toLowerCase(java.util.Locale.FRENCH));
        b.append(" · ≈ ").append(a.kcal).append(" kcal");
        return b.toString();
    }

    /** Libellé de la discipline : « Muscu », « CrossFit », « Renfo ». */
    public static String sportLabel(String sport) {
        if (CROSSFIT.equals(sport)) return "CrossFit";
        if (RENFO.equals(sport)) return "Renfo";
        if (MUSCU.equals(sport)) return "Muscu";
        return "Activité";
    }
}

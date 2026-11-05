package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Réglages de la journée type (diète à équivalences) : horaires, repas, aliments proposés, rappels et règles. */
public class DayPrefs {
    /** « Mes repas » affiche la journée type (par défaut depuis la 4.6) plutôt que les plats de la semaine. */
    public boolean dayMode = true;
    /** Heure du lever et de la séance habituelle, en minutes depuis minuit. */
    public int wake = 5 * 60 + 45;
    public int training = 18 * 60 + 30;
    /** Repas principaux : 4, 5 ou 6. */
    public int meals = 5;
    /** Un fruit 30 minutes avant la séance. */
    public boolean preSnack = true;
    /** Plus de glucides avant et après la séance (sinon : parts égales). */
    public boolean aroundTraining = true;

    /** Aliments proposés, par famille (identifiants Fonte, Ciqual ou perso). */
    public final Set<String> carbs = new LinkedHashSet<>(Arrays.asList(DayPlanner.DEFAULT_CARBS));
    public final Set<String> proteins = new LinkedHashSet<>(Arrays.asList(DayPlanner.DEFAULT_PROTEINS));
    public final Set<String> fats = new LinkedHashSet<>(Arrays.asList(DayPlanner.DEFAULT_FATS));
    public final Set<String> fruits = new LinkedHashSet<>(Arrays.asList(DayPlanner.DEFAULT_FRUITS));
    public final Set<String> vegetables = new LinkedHashSet<>(Arrays.asList(DayPlanner.DEFAULT_VEG));

    /** Heure imposée d'un repas (indice du repas → minutes). */
    public final Map<Integer, Integer> times = new HashMap<>();
    /** Aliment de base imposé à un repas : clé « indice:famille » → identifiant. */
    public final Map<String, String> firsts = new HashMap<>();
    /** Pour un seul jour : clé « jour:indice:famille » → identifiant. */
    public final Map<String, String> dayFirsts = new HashMap<>();

    /** Rappel affiché avec un repas (compléments notés par l'utilisateur, sans dose proposée). */
    public static class Reminder {
        public String text;
        /** Indice du repas (0 = repas 1). */
        public int slot;

        public Reminder(String text, int slot) {
            this.text = text;
            this.slot = slot;
        }
    }

    public final List<Reminder> reminders = new ArrayList<>();
    /** Mes règles (null = règles proposées par défaut). */
    public List<String> rules;

    /** Repas cochés : « jour:indice » → identifiant de l'entrée du journal. */
    public final Map<String, Long> eaten = new LinkedHashMap<>();
    /** Liste de courses de la journée type : articles cochés et ajoutés. */
    public final Set<String> shopChecked = new LinkedHashSet<>();
    public final List<MealPlan.Extra> shopExtras = new ArrayList<>();

    public Set<String> family(String role) {
        switch (role) {
            case DayPlanner.CARBS:
                return carbs;
            case DayPlanner.PROTEIN:
                return proteins;
            case DayPlanner.FAT:
                return fats;
            case DayPlanner.FRUIT:
                return fruits;
            default:
                return vegetables;
        }
    }

    public static String eatenKey(int day, int slot) {
        return day + ":" + slot;
    }

    public static String firstKey(int slot, String role) {
        return slot + ":" + role;
    }

    /** Bases imposées pour ce jour (permanentes, puis celles du jour seulement). */
    public Map<String, String> firstsFor(int day) {
        Map<String, String> m = new HashMap<>(firsts);
        String prefix = day + ":";
        for (Map.Entry<String, String> e : dayFirsts.entrySet()) {
            if (e.getKey().startsWith(prefix)) m.put(e.getKey().substring(prefix.length()), e.getValue());
        }
        return m;
    }

    /** « 6h15 » */
    public static String clock(int minutes) {
        int m = ((minutes % 1440) + 1440) % 1440;
        int h = m / 60, mm = m % 60;
        return h + "h" + (mm < 10 ? "0" : "") + mm;
    }

    /** « 6h15 », « 6:15 », « 615 » ou « 6 » → minutes, ou -1. */
    public static int parseClock(String s) {
        if (s == null) return -1;
        String t = s.trim().toLowerCase(java.util.Locale.ROOT).replace(" ", "");
        if (t.isEmpty()) return -1;
        try {
            int h, m = 0;
            int sep = t.indexOf('h');
            if (sep < 0) sep = t.indexOf(':');
            if (sep >= 0) {
                h = Integer.parseInt(t.substring(0, sep));
                String rest = t.substring(sep + 1);
                if (!rest.isEmpty()) m = Integer.parseInt(rest);
            } else if (t.length() >= 3) {
                h = Integer.parseInt(t.substring(0, t.length() - 2));
                m = Integer.parseInt(t.substring(t.length() - 2));
            } else {
                h = Integer.parseInt(t);
            }
            if (h < 0 || h > 23 || m < 0 || m > 59) return -1;
            return h * 60 + m;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}

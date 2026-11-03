package com.fonte.carnet.core;

import java.util.Calendar;

/** Jours au format entier aaaammjj (fuseau du téléphone). */
public final class Days {
    private Days() {
    }

    public static int key(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        return key(c);
    }

    private static int key(Calendar c) {
        return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH);
    }

    public static int today() {
        return key(System.currentTimeMillis());
    }

    /** Minuit du jour (ms). */
    public static long start(int day) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(day / 10000, (day / 100) % 100 - 1, day % 100, 0, 0, 0);
        return c.getTimeInMillis();
    }

    public static int add(int day, int delta) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(start(day));
        c.add(Calendar.DAY_OF_MONTH, delta);
        return key(c);
    }

    /** Lundi de la semaine du jour. */
    public static int monday(int day) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(start(day));
        int back = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        c.add(Calendar.DAY_OF_MONTH, -back);
        return key(c);
    }

    /** « Aujourd'hui », « Hier », « Demain » ou « Lundi 2 novembre ». */
    public static String label(int day) {
        int t = today();
        if (day == t) return "Aujourd'hui";
        if (day == add(t, -1)) return "Hier";
        if (day == add(t, 1)) return "Demain";
        return Fmt.day(start(day));
    }

    /** Initiale du jour de la semaine (L, M, M, J, V, S, D). */
    public static String initial(int day) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(start(day));
        String[] l = {"D", "L", "M", "M", "J", "V", "S"};
        return l[c.get(Calendar.DAY_OF_WEEK) - 1];
    }
}

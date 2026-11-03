package com.fonte.carnet.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Formatage et lecture des nombres « à la française ». */
public final class Fmt {
    private Fmt() {
    }

    /** 102.5 → « 102,5 » ; 100 → « 100 » ; 101.25 → « 101,25 ». */
    public static String kg(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "0";
        long cents = Math.round(v * 100);
        long whole = cents / 100;
        long frac = Math.abs(cents % 100);
        String sign = (cents < 0 && whole == 0) ? "-" : "";
        if (frac == 0) return sign + whole;
        if (frac % 10 == 0) return sign + whole + "," + (frac / 10);
        return sign + whole + "," + (frac < 10 ? "0" : "") + frac;
    }

    /** Tonnage : en kg sous 1 t, puis en tonnes avec une décimale. */
    public static String tonnage(double kg) {
        if (kg < 1000) return kg(Math.round(kg)) + " kg";
        return kg(Math.round(kg / 100.0) / 10.0) + " t";
    }

    /** Lit « 102,5 » ou « 102.5 ». Renvoie NaN si illisible. */
    public static double parse(String s) {
        if (s == null) return Double.NaN;
        String t = s.trim().replace(',', '.').replace(" ", "");
        if (t.isEmpty()) return Double.NaN;
        try {
            double v = Double.parseDouble(t);
            return (Double.isNaN(v) || Double.isInfinite(v)) ? Double.NaN : v;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    public static int parseInt(String s) {
        double v = parse(s);
        if (Double.isNaN(v) || v < 0) return 0;
        return (int) Math.min(9999, Math.round(v));
    }

    /** « 6×6 » */
    public static String scheme(int sets, int reps) {
        return sets + "×" + reps;
    }

    /** « 1 h 05 » ou « 42 min ». */
    public static String duration(long ms) {
        long min = ms / 60000;
        if (min < 60) return min + " min";
        long h = min / 60;
        long m = min % 60;
        return h + " h " + (m < 10 ? "0" : "") + m;
    }

    /** Chrono « 1:05 » ou « 1:02:09 ». */
    public static String clock(long ms) {
        long s = Math.max(0, ms) / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        String ss = (sec < 10 ? "0" : "") + sec;
        if (h > 0) return h + ":" + (m < 10 ? "0" : "") + m + ":" + ss;
        return m + ":" + ss;
    }

    public static String day(long time) {
        return cap(new SimpleDateFormat("EEEE d MMMM", Locale.FRENCH).format(new Date(time)));
    }

    public static String shortDay(long time) {
        return new SimpleDateFormat("d MMM yyyy", Locale.FRENCH).format(new Date(time));
    }

    public static String month(long time) {
        return cap(new SimpleDateFormat("MMMM yyyy", Locale.FRENCH).format(new Date(time)));
    }

    public static String isoDate(long time) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.FRENCH).format(new Date(time));
    }

    public static String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.FRENCH) + s.substring(1);
    }

    /** Pluriel simple : plural(3, "série") → « 3 séries ». */
    public static String plural(int n, String word) {
        return n + " " + word + (n > 1 ? "s" : "");
    }
}

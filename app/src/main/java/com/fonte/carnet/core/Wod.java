package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Un WOD (Workout Of the Day) : intégré (Hero, Girl, Fonte) ou créé par l'utilisateur. */
public class Wod {
    public static final String HERO = "hero";
    public static final String GIRL = "girl";
    public static final String FONTE = "fonte";
    public static final String CUSTOM = "custom";

    public static final String FOR_TIME = "for_time";
    public static final String AMRAP = "amrap";
    public static final String EMOM = "emom";
    public static final String TABATA = "tabata";

    public String id;
    public String name;
    public String kind = CUSTOM;
    public String format = FOR_TIME;
    /** AMRAP / EMOM : durée ; For Time : cap (0 = sans cap). */
    public int minutes;
    /** EMOM : secondes par intervalle. */
    public int every = 60;
    /** Tabata : nombre de tours (8 par défaut). */
    public int rounds;
    /** EMOM : true si chaque intervalle enchaîne la ligne suivante (min 1 : …, min 2 : …). */
    public boolean alternate;
    public List<String> lines = new ArrayList<>();
    public String note;
    public List<String> tags = new ArrayList<>();

    public Wod() {
    }

    Wod(String id, String name, String kind, String format, int minutes, String note, String[] tags, String... lines) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.format = format;
        this.minutes = minutes;
        this.note = note;
        this.tags = new ArrayList<>(Arrays.asList(tags));
        this.lines = new ArrayList<>(Arrays.asList(lines));
    }

    Wod every(int seconds) {
        this.every = seconds;
        return this;
    }

    Wod alt() {
        this.alternate = true;
        return this;
    }

    Wod tours(int n) {
        this.rounds = n;
        return this;
    }

    /** « For Time · cap 40 min », « AMRAP 20 min », « EMOM 12 min »… */
    public String formatLabel() {
        switch (format) {
            case AMRAP:
                return "AMRAP " + minutes + " min";
            case EMOM:
                if (every != 60 && every > 0) {
                    return (every % 60 == 0 ? "E" + (every / 60) + "MOM " : "EMOM (" + every + " s) ") + minutes + " min";
                }
                return "EMOM " + minutes + " min";
            case TABATA:
                return "Tabata · " + (rounds > 0 ? rounds : 8) + " tours";
            default:
                return minutes > 0 ? "For Time · cap " + minutes + " min" : "For Time";
        }
    }

    public String kindLabel() {
        switch (kind) {
            case HERO:
                return "WOD Hero";
            case GIRL:
                return "The Girls";
            case FONTE:
                return "WOD Fonte";
            default:
                return "Mon WOD";
        }
    }

    /** Type de score naturel du WOD. */
    public String scoreType() {
        switch (format) {
            case AMRAP:
                return WodResult.ROUNDS;
            case EMOM:
                return WodResult.DONE;
            case TABATA:
                return WodResult.REPS;
            default:
                return WodResult.TIME;
        }
    }

    /**
     * Consigne de chaque intervalle d'un EMOM : une ligne par intervalle si alternate,
     * sinon toutes les lignes à chaque intervalle. Les lignes entre parenthèses sont des consignes.
     */
    public List<String> emomMoves() {
        List<String> moves = new ArrayList<>();
        for (String l : lines) {
            String t = l.trim();
            if (t.isEmpty() || t.startsWith("(")) continue;
            moves.add(t);
        }
        if (alternate || moves.size() <= 1) return moves;
        StringBuilder sb = new StringBuilder();
        for (String m : moves) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(m);
        }
        List<String> one = new ArrayList<>();
        one.add(sb.toString());
        return one;
    }

    public Wod copy() {
        Wod w = new Wod();
        w.id = id;
        w.name = name;
        w.kind = kind;
        w.format = format;
        w.minutes = minutes;
        w.every = every;
        w.rounds = rounds;
        w.alternate = alternate;
        w.lines = new ArrayList<>(lines);
        w.note = note;
        w.tags = new ArrayList<>(tags);
        return w;
    }
}

package com.fonte.carnet.core;

import java.util.List;

/** Score enregistré pour un WOD. */
public class WodResult {
    public static final String TIME = "time";
    public static final String ROUNDS = "rounds";
    public static final String REPS = "reps";
    public static final String DONE = "done";

    public long id;
    public String wodId;
    public String wodName;
    public long date;
    public String type = DONE;
    /** TIME : durée en secondes. */
    public int seconds;
    /** ROUNDS : tours complets + répétitions ; REPS : total dans reps. */
    public int rounds;
    public int reps;
    public boolean rx = true;
    /** Le cap de temps a été atteint (For Time non terminé). */
    public boolean capped;
    public String note;
    /** Durée approximative de l'effort (min), pour les calories dépensées. */
    public int minutes;

    /** « 12:34 », « 8 tours + 12 », « 156 reps », « Fait ». */
    public String score() {
        switch (type) {
            case TIME:
                return capped ? "Cap (" + reps + " reps)" : Fmt.clock(seconds * 1000L);
            case ROUNDS:
                return Fmt.plural(rounds, "tour") + (reps > 0 ? " + " + reps : "");
            case REPS:
                return reps + " reps";
            default:
                return "Fait";
        }
    }

    public String fullScore() {
        return score() + (rx ? " · RX" : " · adapté");
    }

    /** true si a est meilleur que b (même type de score). RX passe avant adapté. */
    public static boolean better(WodResult a, WodResult b) {
        if (b == null) return true;
        if (a.rx != b.rx) return a.rx;
        switch (a.type) {
            case TIME:
                if (a.capped != b.capped) return !a.capped;
                if (a.capped) return a.reps > b.reps;
                return a.seconds < b.seconds;
            case ROUNDS:
                return a.rounds * 10000 + a.reps > b.rounds * 10000 + b.reps;
            case REPS:
                return a.reps > b.reps;
            default:
                return a.date < b.date;
        }
    }

    /** Meilleur score parmi les résultats d'un WOD (même type que le WOD), ou null. */
    public static WodResult best(List<WodResult> results, String wodId, String type) {
        WodResult best = null;
        for (WodResult r : results) {
            if (!r.wodId.equals(wodId) || !r.type.equals(type) || DONE.equals(type)) continue;
            if (better(r, best)) best = r;
        }
        return best;
    }
}

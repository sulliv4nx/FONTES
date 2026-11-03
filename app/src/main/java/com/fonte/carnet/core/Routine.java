package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.List;

/** Circuit guidé de renforcement musculaire. */
public class Routine {
    public static class Move {
        public String name;
        public String cue;
        /** Durée en secondes (0 = exercice en répétitions). */
        public int seconds;
        public int reps;

        Move(String name, int seconds, int reps, String cue) {
            this.name = name;
            this.seconds = seconds;
            this.reps = reps;
            this.cue = cue;
        }
    }

    public String id;
    public String name;
    public String focus;
    public String description;
    public int rounds = 1;
    /** Repos entre deux exercices (s). */
    public int rest;
    /** Repos entre deux tours (s). */
    public int roundRest;
    public final List<Move> moves = new ArrayList<>();

    Routine(String id, String name, String focus, int rounds, int rest, int roundRest, String description) {
        this.id = id;
        this.name = name;
        this.focus = focus;
        this.rounds = rounds;
        this.rest = rest;
        this.roundRest = roundRest;
        this.description = description;
    }

    Routine t(String name, int seconds, String cue) {
        moves.add(new Move(name, seconds, 0, cue));
        return this;
    }

    Routine r(String name, int reps, String cue) {
        moves.add(new Move(name, 0, reps, cue));
        return this;
    }

    /** Durée estimée en minutes (3 s par répétition pour les exercices en reps). */
    public int estimatedMinutes() {
        int perRound = 0;
        for (Move m : moves) perRound += m.seconds > 0 ? m.seconds : m.reps * 3;
        perRound += rest * Math.max(0, moves.size() - 1);
        int total = perRound * rounds + roundRest * Math.max(0, rounds - 1);
        return Math.max(1, Math.round(total / 60f));
    }
}

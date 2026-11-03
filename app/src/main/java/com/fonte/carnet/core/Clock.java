package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Moteur de chrono commun (WOD et circuits de renfo) : une suite de phases.
 * Une phase est minutée (compte à rebours), en montée (For Time, avec cap éventuel)
 * ou manuelle (exercice en répétitions : on passe à la suite d'un geste).
 * Le temps est fourni de l'extérieur (« t » = temps actif en ms, pauses exclues) : logique testable.
 */
public class Clock {

    public static class Phase {
        public String label;
        public String detail;
        /** Durée en secondes (cap pour une phase en montée ; 0 = sans limite). */
        public int seconds;
        public boolean countUp;
        public boolean manual;
        public boolean rest;
        public boolean prep;
        public int round;
        public int rounds;
        /** Phase manuelle : nombre de répétitions à faire. */
        public int reps;

        static Phase timed(String label, int seconds) {
            Phase p = new Phase();
            p.label = label;
            p.seconds = seconds;
            return p;
        }
    }

    public final List<Phase> phases = new ArrayList<>();
    public String title = "Chrono";
    /** Affiche un compteur de tours (AMRAP). */
    public boolean roundCounter;
    /** Score attendu en fin de chrono : temps (For Time) ou tours (AMRAP). */
    public String scoreType = WodResult.DONE;

    private int index;
    private long phaseStart;
    private boolean finished;
    private long finishedAt = -1;

    // ---------------------------------------------------------------- moteur

    public Phase current() {
        return index < phases.size() ? phases.get(index) : null;
    }

    public int index() {
        return index;
    }

    public boolean finished() {
        return finished;
    }

    /** Fait avancer les phases minutées dont le temps est écoulé. */
    public void update(long t) {
        while (!finished && index < phases.size()) {
            Phase p = phases.get(index);
            if (p.manual || p.seconds <= 0) return;
            long end = phaseStart + p.seconds * 1000L;
            if (t < end) return;
            phaseStart = end;
            advance(end);
        }
    }

    /** Passe à la phase suivante (série faite, « Terminé », ou saut). */
    public void next(long t) {
        if (finished) return;
        phaseStart = t;
        advance(t);
    }

    public void previous(long t) {
        if (phases.isEmpty()) return;
        finished = false;
        finishedAt = -1;
        index = Math.max(0, Math.min(index, phases.size()) - 1);
        phaseStart = t;
    }

    private void advance(long t) {
        index++;
        if (index >= phases.size()) {
            index = phases.size();
            finished = true;
            finishedAt = t;
        }
    }

    public long phaseElapsed(long t) {
        return Math.max(0, (finished ? finishedAt : t) - phaseStart);
    }

    /** Temps restant de la phase minutée (ms), ou -1 si la phase n'est pas minutée. */
    public long phaseRemaining(long t) {
        Phase p = current();
        if (p == null || p.manual || p.countUp || p.seconds <= 0) return -1;
        return Math.max(0, p.seconds * 1000L - phaseElapsed(t));
    }

    /** Secondes restantes arrondies au-dessus (pour les bips 3-2-1), ou -1. */
    public int secondsLeft(long t) {
        long r = phaseRemaining(t);
        return r < 0 ? -1 : (int) ((r + 999) / 1000);
    }

    /** Durée de la préparation initiale (ms). */
    public long prepMs() {
        long ms = 0;
        for (Phase p : phases) {
            if (!p.prep) break;
            ms += p.seconds * 1000L;
        }
        return ms;
    }

    /** Temps de travail écoulé (hors préparation), figé à la fin. */
    public long workElapsed(long t) {
        long end = finished ? finishedAt : t;
        return Math.max(0, end - prepMs());
    }

    /** Durée totale prévue (s) si toutes les phases sont minutées, sinon -1. */
    public int plannedSeconds() {
        int s = 0;
        for (Phase p : phases) {
            if (p.prep) continue;
            if (p.manual || p.seconds <= 0) return -1;
            s += p.seconds;
        }
        return s;
    }

    public Phase upcoming() {
        for (int i = index + 1; i < phases.size(); i++) {
            Phase p = phases.get(i);
            if (!p.rest) return p;
        }
        return null;
    }

    // ---------------------------------------------------------------- fabriques

    private static Clock withPrep(String title, int prepSeconds) {
        Clock c = new Clock();
        c.title = title;
        if (prepSeconds > 0) {
            Phase p = Phase.timed("Prêt ?", prepSeconds);
            p.prep = true;
            p.detail = "Départ imminent";
            c.phases.add(p);
        }
        return c;
    }

    public static Clock amrap(int minutes, int prep) {
        Clock c = withPrep("AMRAP " + minutes + " min", prep);
        Phase p = Phase.timed("AMRAP", minutes * 60);
        p.detail = "Un maximum de tours en " + minutes + " min";
        c.phases.add(p);
        c.roundCounter = true;
        c.scoreType = WodResult.ROUNDS;
        return c;
    }

    /** For Time : chrono en montée, cap en minutes (0 = sans limite). */
    public static Clock forTime(int capMinutes, int prep) {
        Clock c = withPrep(capMinutes > 0 ? "For Time · cap " + capMinutes + " min" : "For Time", prep);
        Phase p = new Phase();
        p.label = "For Time";
        p.detail = capMinutes > 0 ? "Cap à " + capMinutes + " min" : "Touche « Terminé » à la fin du WOD";
        p.countUp = true;
        p.seconds = capMinutes * 60;
        c.phases.add(p);
        c.roundCounter = true;
        c.scoreType = WodResult.TIME;
        return c;
    }

    /** EMOM : une phase par intervalle de everySeconds, rounds fois. */
    public static Clock emom(int everySeconds, int rounds, int prep, List<String> moves) {
        String every = everySeconds % 60 == 0 ? (everySeconds / 60 == 1 ? "chaque minute" : "toutes les " + everySeconds / 60 + " min")
                : "toutes les " + everySeconds + " s";
        Clock c = withPrep("EMOM · " + rounds + " × " + Fmt.clock(everySeconds * 1000L), prep);
        for (int r = 1; r <= rounds; r++) {
            Phase p = Phase.timed((everySeconds == 60 ? "Minute " : "Intervalle ") + r + " / " + rounds, everySeconds);
            p.round = r;
            p.rounds = rounds;
            if (moves != null && !moves.isEmpty()) p.detail = moves.get((r - 1) % moves.size());
            else p.detail = "Départ " + every;
            c.phases.add(p);
        }
        c.scoreType = WodResult.DONE;
        return c;
    }

    /** Intervalles travail / repos (Tabata = 20 / 10 × 8). Pas de repos après le dernier tour. */
    public static Clock intervals(String title, int work, int rest, int rounds, int prep) {
        Clock c = withPrep(title, prep);
        for (int r = 1; r <= rounds; r++) {
            Phase w = Phase.timed("Travail", work);
            w.round = r;
            w.rounds = rounds;
            w.detail = "Tour " + r + " / " + rounds;
            c.phases.add(w);
            if (rest > 0 && r < rounds) {
                Phase p = Phase.timed("Repos", rest);
                p.rest = true;
                p.round = r;
                p.rounds = rounds;
                p.detail = "Tour " + (r + 1) + " ensuite";
                c.phases.add(p);
            }
        }
        c.scoreType = WodResult.REPS;
        return c;
    }

    public static Clock tabata(int rounds, int prep) {
        return intervals("Tabata", 20, 10, rounds, prep);
    }

    /** Chrono adapté à un WOD. */
    public static Clock forWod(Wod w, int prep) {
        Clock c;
        switch (w.format) {
            case Wod.AMRAP:
                c = amrap(Math.max(1, w.minutes), prep);
                break;
            case Wod.EMOM: {
                int every = w.every > 0 ? w.every : 60;
                int rounds = Math.max(1, w.minutes * 60 / every);
                c = emom(every, rounds, prep, w.emomMoves());
                break;
            }
            case Wod.TABATA: {
                c = tabata(Math.max(1, w.rounds > 0 ? w.rounds : 8), prep);
                List<String> moves = new ArrayList<>();
                for (String l : w.lines) {
                    String m = l.trim();
                    int colon = m.indexOf(" : ");
                    if (colon > 0 && m.toLowerCase().startsWith("tours")) m = m.substring(colon + 3);
                    if (!m.isEmpty() && !m.startsWith("(")) moves.add(m);
                }
                if (!moves.isEmpty()) {
                    for (Phase p : c.phases) {
                        if (p.prep || p.rest) continue;
                        p.detail = moves.get((p.round - 1) % moves.size()) + " · tour " + p.round + " / " + p.rounds;
                    }
                }
                break;
            }
            default:
                c = forTime(w.minutes, prep);
                break;
        }
        c.title = w.name;
        return c;
    }

    /** Circuit guidé de renforcement. */
    public static Clock circuit(Routine r, int prep) {
        Clock c = withPrep(r.name, prep);
        int total = r.moves.size();
        for (int round = 1; round <= r.rounds; round++) {
            for (int i = 0; i < total; i++) {
                Routine.Move m = r.moves.get(i);
                Phase p = new Phase();
                p.label = m.name;
                p.detail = m.cue;
                p.round = round;
                p.rounds = r.rounds;
                if (m.seconds > 0) {
                    p.seconds = m.seconds;
                } else {
                    p.manual = true;
                    p.reps = m.reps;
                }
                c.phases.add(p);
                boolean lastOfRound = i == total - 1;
                boolean lastOfAll = lastOfRound && round == r.rounds;
                int rest = lastOfRound ? r.roundRest : r.rest;
                if (!lastOfAll && rest > 0) {
                    Phase pause = Phase.timed("Récupération", rest);
                    pause.rest = true;
                    pause.round = round;
                    pause.rounds = r.rounds;
                    c.phases.add(pause);
                }
            }
        }
        c.scoreType = WodResult.DONE;
        return c;
    }
}

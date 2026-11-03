package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Calculs : 1RM estimé, records, progression. */
public final class Stats {
    private Stats() {
    }

    /** Au-delà, l'estimation du 1RM n'est plus fiable : la série ne compte pas pour le 1RM estimé. */
    public static final int MAX_REPS_FOR_E1RM = 12;
    private static final double EPS = 0.05;

    public interface Names {
        String of(long exerciseId);
    }

    /** 1RM estimé (formule d'Epley). */
    public static double e1rm(double weight, int reps) {
        if (weight <= 0 || reps <= 0) return 0;
        if (reps == 1) return weight;
        return weight * (1 + reps / 30.0);
    }

    public static class Record {
        public long exerciseId;
        public double bestE1rm;
        public double e1rmWeight;
        public int e1rmReps;
        public long e1rmDate;
        public double maxWeight;
        public int maxWeightReps;
        public long maxWeightDate;
        public int maxReps;
        public double maxRepsWeight;
        public long maxRepsDate;
        public int sessions;
        public long lastDate;

        public Record(long exerciseId) {
            this.exerciseId = exerciseId;
        }

        /** Aucune série chargée : exercice au poids du corps. */
        public boolean bodyweight() {
            return maxWeight <= 0;
        }

        void add(SetEntry s, long date) {
            if (!s.done || s.reps <= 0) return;
            if (s.reps <= MAX_REPS_FOR_E1RM) {
                double e = e1rm(s.weight, s.reps);
                if (e > bestE1rm + 1e-9) {
                    bestE1rm = e;
                    e1rmWeight = s.weight;
                    e1rmReps = s.reps;
                    e1rmDate = date;
                }
            }
            if (s.weight > maxWeight + 1e-9 || (Math.abs(s.weight - maxWeight) < 1e-9 && s.reps > maxWeightReps)) {
                maxWeight = s.weight;
                maxWeightReps = s.reps;
                maxWeightDate = date;
            }
            if (s.reps > maxReps) {
                maxReps = s.reps;
                maxRepsWeight = s.weight;
                maxRepsDate = date;
            }
        }
    }

    /** Records par exercice sur les séances terminées. */
    public static Map<Long, Record> records(List<Session> sessions) {
        Map<Long, Record> map = new LinkedHashMap<>();
        for (Session s : sessions) {
            if (s.inProgress()) continue;
            Set<Long> seen = new HashSet<>();
            for (ExerciseEntry e : s.exercises) {
                if (e.doneCount() == 0) continue;
                Record r = map.get(e.exerciseId);
                if (r == null) {
                    r = new Record(e.exerciseId);
                    map.put(e.exerciseId, r);
                }
                if (seen.add(e.exerciseId)) {
                    r.sessions++;
                    r.lastDate = Math.max(r.lastDate, s.start);
                }
                for (SetEntry set : e.sets) r.add(set, s.start);
            }
        }
        return map;
    }

    /** Meilleures performances d'un exercice dans une seule séance (toutes les entrées confondues). */
    public static Record sessionBest(Session s, long exerciseId, SetEntry exclude) {
        Record r = new Record(exerciseId);
        for (ExerciseEntry e : s.exercises) {
            if (e.exerciseId != exerciseId) continue;
            for (SetEntry set : e.sets) if (set != exclude) r.add(set, s.start);
        }
        return r;
    }

    /** Records battus par une séance par rapport à l'historique (hors cette séance). */
    public static List<String> newRecords(Session session, List<Session> history, Names names) {
        List<Session> others = new ArrayList<>();
        for (Session h : history) if (h.id != session.id) others.add(h);
        Map<Long, Record> prev = records(others);
        List<String> out = new ArrayList<>();
        Set<Long> done = new HashSet<>();
        for (ExerciseEntry e : session.exercises) {
            if (!done.add(e.exerciseId)) continue;
            Record p = prev.get(e.exerciseId);
            if (p == null || p.sessions == 0) continue;
            Record c = sessionBest(session, e.exerciseId, null);
            String name = names.of(e.exerciseId);
            if (c.bestE1rm > p.bestE1rm + EPS) {
                out.add(name + " : 1RM estimé " + Fmt.kg(round1(c.bestE1rm)) + " kg (avant " + Fmt.kg(round1(p.bestE1rm)) + " kg)");
            }
            if (c.maxWeight > p.maxWeight + EPS) {
                out.add(name + " : charge max " + Fmt.kg(c.maxWeight) + " kg × " + c.maxWeightReps + " (avant " + Fmt.kg(p.maxWeight) + " kg)");
            } else if (p.bodyweight() && c.bodyweight() && c.maxReps > p.maxReps) {
                out.add(name + " : " + c.maxReps + " répétitions (avant " + p.maxReps + ")");
            }
        }
        return out;
    }

    /**
     * Message à afficher quand on valide une série qui bat le record (historique + reste de la séance),
     * ou null. prev peut être null (exercice jamais fait : pas de message).
     */
    public static String liveRecord(SetEntry set, Record prev, Record sessionSoFar) {
        if (prev == null || prev.sessions == 0 || !set.done || set.reps <= 0) return null;
        double bestE = Math.max(prev.bestE1rm, sessionSoFar == null ? 0 : sessionSoFar.bestE1rm);
        double bestW = Math.max(prev.maxWeight, sessionSoFar == null ? 0 : sessionSoFar.maxWeight);
        int bestR = Math.max(prev.maxReps, sessionSoFar == null ? 0 : sessionSoFar.maxReps);
        if (set.weight > bestW + EPS) return "Record de charge : " + Fmt.kg(set.weight) + " kg !";
        if (set.reps <= MAX_REPS_FOR_E1RM) {
            double e = e1rm(set.weight, set.reps);
            if (e > bestE + EPS) return "Record : 1RM estimé " + Fmt.kg(round1(e)) + " kg !";
        }
        if (set.weight <= 0 && bestW <= 0 && set.reps > bestR) return "Record : " + set.reps + " répétitions !";
        return null;
    }

    /** Points (date, valeur) de progression : meilleur 1RM estimé par séance, ou max de reps si poids du corps. */
    public static List<double[]> progress(long exerciseId, List<Session> sessions, boolean reps) {
        List<double[]> pts = new ArrayList<>();
        for (Session s : sessions) {
            if (s.inProgress()) continue;
            Record r = sessionBest(s, exerciseId, null);
            double v = reps ? r.maxReps : r.bestE1rm;
            if (v > 0) pts.add(new double[]{s.start, round1(v)});
        }
        Collections.sort(pts, (a, b) -> Double.compare(a[0], b[0]));
        return pts;
    }

    /** Séries validées de la dernière séance terminée contenant l'exercice (hors excludeId), ou null. */
    public static List<SetEntry> lastPerformance(long exerciseId, List<Session> sessions, long excludeId) {
        Session best = null;
        for (Session s : sessions) {
            if (s.inProgress() || s.id == excludeId) continue;
            if (best != null && s.start <= best.start) continue;
            for (ExerciseEntry e : s.exercises) {
                if (e.exerciseId == exerciseId && e.doneCount() > 0) {
                    best = s;
                    break;
                }
            }
        }
        if (best == null) return null;
        List<SetEntry> out = new ArrayList<>();
        for (ExerciseEntry e : best.exercises)
            if (e.exerciseId == exerciseId)
                for (SetEntry s : e.sets) if (s.done) out.add(s);
        return out;
    }

    /** « 100×5 · 100×5 · 97,5×4 » (5 séries max). */
    public static String summarize(List<SetEntry> sets) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (SetEntry s : sets) {
            if (n == 5) {
                sb.append(" …");
                break;
            }
            if (n > 0) sb.append(" · ");
            sb.append(s.weight > 0 ? Fmt.kg(s.weight) + "×" + s.reps : s.reps + " reps");
            n++;
        }
        return sb.toString();
    }

    /** {nombre de séances, tonnage} de la semaine en cours (lundi → dimanche). */
    public static double[] thisWeek(List<Session> sessions, long now) {
        Calendar c = Calendar.getInstance(Locale.FRANCE);
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.setTimeInMillis(now);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        int dow = c.get(Calendar.DAY_OF_WEEK);
        int back = (dow + 5) % 7; // lundi → 0, dimanche → 6
        c.add(Calendar.DAY_OF_MONTH, -back);
        long from = c.getTimeInMillis();
        double count = 0;
        double tonnage = 0;
        for (Session s : sessions) {
            if (s.inProgress() || s.start < from) continue;
            count++;
            tonnage += s.tonnage();
        }
        return new double[]{count, tonnage};
    }

    public static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}

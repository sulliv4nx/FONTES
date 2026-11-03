package com.fonte.carnet.core;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lit une programmation de coach écrite librement (texte collé ou scanné) :
 * « Squat 5x5 100kg », « Développé couché 4 × 8 @ 70 », « 3 séries de 12 curl », « Front squat 5x3 75% »…
 * Les blocs AMRAP / EMOM / For Time sont repérés et proposés comme WOD.
 */
public final class CoachParser {
    private CoachParser() {
    }

    public interface ExerciseMatcher {
        /** Id de l'exercice correspondant au nom, ou 0. */
        long match(String name);
    }

    public interface Maxes {
        /** Max (1RM estimé) de l'exercice en kg, ou 0. */
        double of(long exerciseId);
    }

    public static class Item {
        public String raw;
        public String name;
        public long exerciseId;
        public int sets;
        public int reps;
        public boolean max;
        public final List<Double> weights = new ArrayList<>();
        public int percent;

        public double weightForSet(int i) {
            if (weights.isEmpty()) return 0;
            return weights.get(Math.min(i, weights.size() - 1));
        }

        /** Séries prêtes pour une séance. */
        public ExerciseEntry toEntry(long id) {
            ExerciseEntry e = new ExerciseEntry(id);
            for (int i = 0; i < sets; i++) {
                SetEntry s = new SetEntry(weightForSet(i), max ? 0 : reps);
                s.amrap = max;
                e.sets.add(s);
            }
            return e;
        }

        /** « 5×5 · 100 kg », « 4×max », « 5×3 · 75 % » */
        public String summary() {
            StringBuilder sb = new StringBuilder();
            sb.append(sets).append("×").append(max ? "max" : String.valueOf(reps));
            if (!weights.isEmpty()) {
                sb.append(" · ");
                for (int i = 0; i < weights.size(); i++) {
                    if (i > 0) sb.append("/");
                    sb.append(Fmt.kg(weights.get(i)));
                }
                sb.append(" kg");
                if (percent > 0) sb.append(" (").append(percent).append(" %)");
            } else if (percent > 0) {
                sb.append(" · ").append(percent).append(" % du max");
            }
            return sb.toString();
        }
    }

    public static class Result {
        public final List<Item> items = new ArrayList<>();
        public final List<String> ignored = new ArrayList<>();
        /** Partie WOD détectée (AMRAP, EMOM, For Time…), ou null. */
        public Wod wod;
    }

    private static final String NUM = "(\\d{1,3}(?:[.,]\\d{1,2})?)";
    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern BULLET = Pattern.compile("^(?:[-–—•*·>]+\\s*|\\d{1,2}[.)]\\s+|[A-Ha-h]\\d?\\s*[).:]\\s*)");
    private static final Pattern SRW = Pattern.compile("(\\d{1,2})\\s*[x×*]\\s*(\\d{1,3})\\s*[x×*]\\s*" + NUM + "\\s*(?:kgs?|kilos?)?", FLAGS);
    private static final Pattern SR = Pattern.compile("(\\d{1,2})\\s*[x×*]\\s*(\\d{1,3}|max|amrap)(?:\\s*(?:reps?|r[ée]p[ée]titions?|r[ée]ps?)\\b)?", FLAGS);
    private static final Pattern SERIES = Pattern.compile("(\\d{1,2})\\s*(?:s[ée]ries?|sets?)\\s*(?:de\\s*|of\\s*|x\\s*)?(\\d{1,3}|max)(?:\\s*(?:reps?|r[ée]p[ée]titions?|r[ée]ps?)\\b)?", FLAGS);
    private static final Pattern PERCENT = Pattern.compile("(\\d{2,3})\\s*%", FLAGS);
    private static final Pattern KG = Pattern.compile(NUM + "((?:\\s*/\\s*\\d{1,3}(?:[.,]\\d{1,2})?)*)\\s*(?:kgs?|kilos?)\\b", FLAGS);
    private static final Pattern AT = Pattern.compile("(?:@|\\s[àa]\\s)\\s*" + NUM + "(?![\\d%]|\\s*%)", FLAGS);
    private static final Pattern NOISE = Pattern.compile(
            "\\(.*?\\)|\\brpe\\s*\\d+(?:[.,]5)?|\\btempo\\s*\\d{3,4}|\\b(?:repos|rest|r[ée]cup[ée]ration)\\b.*$"
                    + "|\\d+\\s*(?:min|mn|sec|s)\\b|\\d+\\s*['\"]"
                    + "|/\\s*(?:jambe|bras|c[ôo]t[ée])s?|\\b(?:par|chaque)\\s+(?:jambe|bras|c[ôo]t[ée])s?\\b", FLAGS);
    private static final Pattern BARE_NUMBER = Pattern.compile("(?:^|\\s)" + NUM + "(?=\\s|$)");
    private static final Pattern WORDS = Pattern.compile("\\b(?:reps?|r[ée]p[ée]titions?|r[ée]ps?|s[ée]ries?|sets?|kgs?|kilos?|de|of|m|km|cal|calories?|m[èe]tres?)\\b", FLAGS);
    private static final Pattern PART = Pattern.compile("^(?:[A-Ha-h]\\d?\\s*[).:-]|partie\\b|part\\b|force\\b|strength\\b|muscu\\w*\\b|accessoires?\\b|skill\\b|[ée]chauffement\\b|warm\\s?-?up\\b)", FLAGS);
    private static final Pattern MINUTES = Pattern.compile("(\\d{1,3})\\s*(?:min|mn|minutes?|')", FLAGS);
    private static final Pattern AFTER_KEYWORD = Pattern.compile("(?:amrap|emom|cap|tabata)\\s*:?\\s*(\\d{1,3})", FLAGS);
    private static final Pattern EXMOM = Pattern.compile("e(\\d)mom", FLAGS);
    private static final Pattern ROUNDS = Pattern.compile("(\\d{1,2})\\s*(?:rounds?|tours?|rft)\\b", FLAGS);

    private static class Scheme {
        int sets;
        int reps;
        boolean max;
        int start;
        int end;
    }

    // ---------------------------------------------------------------- analyse

    public static Result parse(String text, ExerciseMatcher matcher, Maxes maxes, double rounding) {
        Result res = new Result();
        if (text == null) return res;
        String pending = null;
        boolean inWod = false;
        for (String raw : text.replace('\r', '\n').split("\n")) {
            String line = BULLET.matcher(raw.trim()).replaceFirst("").trim();
            if (line.isEmpty()) {
                inWod = false;
                if (pending != null) res.ignored.add(pending);
                pending = null;
                continue;
            }
            String n = key(line);
            if (isWodHeader(n)) {
                res.wod = newWod(line, n);
                inWod = true;
                if (pending != null) res.ignored.add(pending);
                pending = null;
                continue;
            }
            if (inWod && !PART.matcher(raw.trim()).find()) {
                res.wod.lines.add(line);
                continue;
            }
            inWod = false;
            Item it = parseLine(line);
            if (it == null) {
                if (pending != null) res.ignored.add(pending);
                pending = line;
                continue;
            }
            if (it.name.isEmpty() && pending != null) it.name = cleanName(pending);
            pending = null;
            if (it.name.isEmpty()) {
                res.ignored.add(line);
                continue;
            }
            it.exerciseId = matcher == null ? 0 : matcher.match(it.name);
            if (it.percent > 0 && it.weights.isEmpty() && it.exerciseId != 0 && maxes != null) {
                double m = maxes.of(it.exerciseId);
                if (m > 0) it.weights.add(Programs.round(m * it.percent / 100.0, rounding));
            }
            res.items.add(it);
        }
        if (pending != null) res.ignored.add(pending);
        if (res.wod != null && res.wod.lines.isEmpty()) res.wod = null;
        return res;
    }

    /** Analyse une ligne « exercice + séries × reps (+ charge) », ou null si aucune série n'est trouvée. */
    static Item parseLine(String line) {
        String work = " " + line + " ";
        Scheme sc = null;
        Item it = new Item();
        it.raw = line;

        Matcher m = SRW.matcher(work);
        if (m.find()) {
            sc = scheme(m, m.group(1), m.group(2));
            it.weights.add(Fmt.parse(m.group(3)));
        } else {
            m = SR.matcher(work);
            if (m.find()) {
                sc = scheme(m, m.group(1), m.group(2));
            } else {
                m = SERIES.matcher(work);
                if (m.find()) sc = scheme(m, m.group(1), m.group(2));
            }
        }
        if (sc == null || sc.sets <= 0 || sc.sets > 20 || (!sc.max && (sc.reps <= 0 || sc.reps > 200))) return null;
        it.sets = sc.sets;
        it.reps = sc.reps;
        it.max = sc.max;
        String rest = work.substring(0, sc.start) + " " + work.substring(sc.end);

        Matcher p = PERCENT.matcher(rest);
        if (p.find()) {
            it.percent = Integer.parseInt(p.group(1));
            rest = rest.substring(0, p.start()) + " " + rest.substring(p.end());
        }
        if (it.weights.isEmpty()) {
            Matcher k = KG.matcher(rest);
            if (k.find()) {
                it.weights.add(Fmt.parse(k.group(1)));
                String more = k.group(2);
                if (more != null && !more.trim().isEmpty()) {
                    for (String w : more.split("/")) {
                        double v = Fmt.parse(w);
                        if (!Double.isNaN(v)) it.weights.add(v);
                    }
                }
                rest = rest.substring(0, k.start()) + " " + rest.substring(k.end());
            } else {
                Matcher at = AT.matcher(rest);
                if (at.find()) {
                    it.weights.add(Fmt.parse(at.group(1)));
                    rest = rest.substring(0, at.start()) + " " + rest.substring(at.end());
                }
            }
        }
        rest = NOISE.matcher(rest).replaceAll(" ");
        if (it.weights.isEmpty() && it.percent == 0) {
            Matcher b = BARE_NUMBER.matcher(rest);
            if (b.find()) {
                double v = Fmt.parse(b.group(1));
                if (!Double.isNaN(v) && v > 0 && v <= 500) {
                    it.weights.add(v);
                    rest = rest.substring(0, b.start()) + " " + rest.substring(b.end());
                }
            }
        }
        List<Double> clean = new ArrayList<>();
        for (Double w : it.weights) if (w != null && !Double.isNaN(w) && w > 0 && w <= 600) clean.add(w);
        it.weights.clear();
        it.weights.addAll(clean);
        it.name = cleanName(rest);
        return it;
    }

    private static Scheme scheme(Matcher m, String sets, String reps) {
        Scheme s = new Scheme();
        s.start = m.start();
        s.end = m.end();
        s.sets = Integer.parseInt(sets);
        String r = reps.toLowerCase(Locale.ROOT);
        if (r.equals("max") || r.equals("amrap")) {
            s.max = true;
        } else {
            s.reps = Integer.parseInt(r);
        }
        return s;
    }

    static String cleanName(String s) {
        String t = NOISE.matcher(s).replaceAll(" ");
        t = WORDS.matcher(t).replaceAll(" ");
        t = t.replaceAll("[@:;,=+]|\\s[àa]\\s|\\s[x×*]\\s", " ");
        t = t.replaceAll("\\s+", " ").trim();
        t = t.replaceAll("^[\\s\\-–—.]+|[\\s\\-–—.]+$", "").trim();
        return t;
    }

    private static boolean isWodHeader(String k) {
        String s = " " + k + " ";
        return s.contains(" amrap") || s.contains(" emom") || EXMOM.matcher(s).find() || s.contains(" for time ")
                || s.contains(" rft ") || s.contains(" tabata") || s.contains(" metcon") || s.startsWith(" wod ")
                || s.contains(" chipper ");
    }

    private static Wod newWod(String line, String k) {
        Wod w = new Wod();
        w.kind = Wod.CUSTOM;
        w.name = "WOD du coach";
        Matcher ex = EXMOM.matcher(k);
        if (k.contains("amrap")) {
            w.format = Wod.AMRAP;
        } else if (k.contains("emom") || ex.find()) {
            w.format = Wod.EMOM;
            Matcher e2 = EXMOM.matcher(k);
            if (e2.find()) w.every = Integer.parseInt(e2.group(1)) * 60;
        } else if (k.contains("tabata")) {
            w.format = Wod.TABATA;
            w.rounds = 8;
        } else {
            w.format = Wod.FOR_TIME;
        }
        Matcher mm = AFTER_KEYWORD.matcher(line);
        Matcher mn = MINUTES.matcher(line);
        if (mn.find()) w.minutes = Integer.parseInt(mn.group(1));
        else if (mm.find()) w.minutes = Integer.parseInt(mm.group(1));
        if (Wod.AMRAP.equals(w.format) && w.minutes <= 0) w.minutes = 12;
        if (Wod.EMOM.equals(w.format) && w.minutes <= 0) w.minutes = 10;
        Matcher r = ROUNDS.matcher(line);
        if (r.find()) w.lines.add(r.group(1) + " tours :");
        return w;
    }

    // ---------------------------------------------------------------- correspondance des noms

    /** Clé de comparaison : minuscules, sans accents ni ponctuation, mots au singulier. */
    public static String key(String s) {
        String n = Normalizer.normalize(s.toLowerCase(Locale.FRENCH), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        n = n.replaceAll("[^a-z0-9]+", " ").trim();
        if (n.isEmpty()) return n;
        StringBuilder sb = new StringBuilder();
        for (String tok : n.split(" ")) {
            if (tok.length() > 3 && tok.endsWith("s") && !tok.endsWith("ss")) tok = tok.substring(0, tok.length() - 1);
            if (sb.length() > 0) sb.append(' ');
            sb.append(tok);
        }
        return sb.toString();
    }

    private static boolean containsPhrase(String text, String phrase) {
        return !phrase.isEmpty() && (" " + text + " ").contains(" " + phrase + " ");
    }

    private static final Map<String, Long> ALIASES = new LinkedHashMap<>();

    private static void alias(long id, String... names) {
        for (String n : names) ALIASES.put(key(n), id);
    }

    static {
        alias(Library.BENCH, "dc", "bench", "bench press", "developpe couche", "couche");
        alias(5, "developpe incline", "incline", "incline bench");
        alias(6, "developpe couche haltere", "dumbbell bench");
        alias(Library.SQUAT, "squat", "back squat", "squat arriere", "back sq", "bs");
        alias(16, "front squat", "squat avant", "fs");
        alias(Library.DEADLIFT, "deadlift", "sdt", "dl", "souleve de terre", "souleve");
        alias(17, "rdl", "romanian deadlift", "sdt roumain", "souleve de terre roumain", "jambes tendues");
        alias(Library.OHP, "ohp", "militaire", "developpe militaire", "overhead press", "strict press", "press");
        alias(9, "push press");
        alias(7, "dips", "dip");
        alias(8, "pompes", "push up", "pushup");
        alias(10, "elevations laterales", "elevation laterale", "lateral raise");
        alias(11, "tractions", "traction", "pull up", "pullup", "chin up", "strict pull up");
        alias(12, "rowing", "row", "barbell row", "bent over row", "rowing barre");
        alias(13, "rowing haltere", "dumbbell row", "db row");
        alias(14, "tirage vertical", "lat pulldown", "pulldown", "tirage poitrine");
        alias(15, "face pull");
        alias(18, "fentes", "fente", "lunge", "walking lunge", "fentes marchees");
        alias(19, "presse", "leg press", "presse a cuisse");
        alias(20, "hip thrust", "hip thrusts");
        alias(21, "curl", "biceps curl", "curl biceps", "curl barre");
        alias(22, "extension triceps", "triceps extension", "triceps");
        alias(23, "releve de jambe", "leg raise", "hanging leg raise", "releve de genou");
        alias(24, "power clean", "epaule", "clean");
        alias(25, "clean and jerk", "clean jerk", "epaule jete", "c j");
        alias(26, "snatch", "arrache", "power snatch");
        alias(27, "thruster");
        alias(28, "kettlebell swing", "kb swing", "swing");
        alias(29, "wall ball", "wallball");
        alias(30, "box jump");
        alias(31, "burpee");
        alias(32, "muscle up");
        alias(33, "toes to bar", "t2b", "ttb");
        alias(34, "log press", "log");
        alias(36, "trap bar deadlift", "trap bar", "hex bar");
        alias(38, "ecarte", "ecarte couche", "dumbbell fly", "fly");
        alias(39, "ecarte poulie", "cable fly", "cable crossover", "vis a vis");
        alias(40, "developpe incline haltere", "incline dumbbell press");
        alias(41, "developpe epaule haltere", "dumbbell shoulder press", "arnold press");
        alias(42, "oiseau", "reverse fly", "rear delt fly");
        alias(43, "shrug", "haussement d epaule");
        alias(44, "pull over", "pullover");
        alias(45, "tirage horizontal", "seated row", "cable row");
        alias(46, "curl marteau", "hammer curl");
        alias(47, "curl pupitre", "preacher curl");
        alias(48, "barre au front", "skull crusher", "skullcrusher");
        alias(49, "extension triceps poulie", "pushdown", "triceps pushdown", "poulie triceps");
        alias(50, "leg extension");
        alias(51, "leg curl", "ischio machine");
        alias(52, "mollet", "mollets debout", "calf raise", "standing calf raise");
        alias(53, "mollets assis", "seated calf raise");
        alias(54, "squat bulgare", "bulgarian split squat", "split squat", "bulgare");
        alias(55, "goblet squat", "goblet");
        alias(56, "good morning");
        alias(57, "crunch");
        alias(58, "roue abdominale", "ab wheel", "roulette");
        alias(59, "abduction", "hip abduction");
        alias(60, "developpe couche prise serree", "close grip bench", "prise serree");
    }

    /** Correspondance avec la bibliothèque : nom exact, alias, puis mots en commun. */
    public static ExerciseMatcher libraryMatcher(List<Exercise> exercises) {
        return name -> {
            String n = key(name);
            if (n.isEmpty()) return 0;
            for (Exercise e : exercises) if (key(e.name).equals(n)) return e.id;
            long best = 0;
            int bestLen = 0;
            for (Map.Entry<String, Long> a : ALIASES.entrySet()) {
                if (containsPhrase(n, a.getKey()) && a.getKey().length() > bestLen) {
                    best = a.getValue();
                    bestLen = a.getKey().length();
                }
            }
            for (Exercise e : exercises) {
                String k = key(e.name);
                if (containsPhrase(n, k) && k.length() > bestLen) {
                    best = e.id;
                    bestLen = k.length();
                }
            }
            if (best != 0) return best;
            String[] toks = n.split(" ");
            double bestScore = 0;
            for (Exercise e : exercises) {
                String[] et = key(e.name).split(" ");
                int common = 0;
                for (String a : toks) for (String b : et) if (a.equals(b) && a.length() > 2) common++;
                double score = common / (double) Math.max(toks.length, et.length);
                if (score > bestScore) {
                    bestScore = score;
                    best = e.id;
                }
            }
            return bestScore >= 0.66 ? best : 0;
        };
    }
}

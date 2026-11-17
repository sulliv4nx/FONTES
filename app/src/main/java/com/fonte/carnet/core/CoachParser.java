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
 * Les blocs AMRAP / EMOM / For Time sont repérés et proposés comme WOD. Les lignes qui se suivent pour un même exercice
 * (montée en charge) forment un seul exercice ; le cardio (« 250 m rameur », « 2 min corde à sauter ») est repéré à part.
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
        /** Lignes suivantes du même exercice (montée en charge, top set puis back-off…). */
        public final List<Item> more = new ArrayList<>();
        /** Libellé des séries de cette ligne : « Top set », « Back-off −10 % », « Par côté »… */
        public String label;
        /** Back-off : pourcentage retiré à la charge de la ligne précédente (10 pour « @-10% »). */
        public int backoff;
        /** Maintien en secondes (« 20"/20" »), 0 sinon. */
        public int holdSec;
        /** Consigne du coach (tempo, RIR…). */
        public String note;
        /** Repos en secondes (« R. 2' », « Pause 1' »), 0 sinon. */
        public int rest;

        /** Nombre total de séries, lignes suivantes comprises. */
        public int totalSets() {
            int n = sets;
            for (Item m : more) n += m.sets;
            return n;
        }

        public double weightForSet(int i) {
            if (weights.isEmpty()) return 0;
            return weights.get(Math.min(i, weights.size() - 1));
        }

        /** Séries prêtes pour une séance. */
        public ExerciseEntry toEntry(long id) {
            ExerciseEntry e = new ExerciseEntry(id);
            addSets(e);
            for (Item m : more) m.addSets(e);
            e.note = note;
            e.restSec = rest;
            return e;
        }

        private void addSets(ExerciseEntry e) {
            for (int i = 0; i < sets; i++) {
                SetEntry s = new SetEntry(weightForSet(i), max ? 0 : reps);
                s.amrap = max;
                s.label = label;
                e.sets.add(s);
            }
        }

        /** « 5×5 · 100 kg », « 4×max », « 5×3 · 75 % » ; plusieurs lignes : « 1×10 · 20 kg, 1×6 · 30 kg, … » */
        public String summary() {
            if (more.isEmpty()) return part();
            StringBuilder sb = new StringBuilder(part());
            for (Item m : more) sb.append(", ").append(m.part());
            return sb.toString();
        }

        private String part() {
            StringBuilder sb = new StringBuilder();
            if (holdSec > 0) {
                sb.append(sets).append("×").append(holdSec).append(" s par côté");
                return sb.toString();
            }
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
            if (label != null) sb.append(" · ").append(label.toLowerCase(Locale.FRENCH));
            return sb.toString();
        }
    }

    /** Cardio ou ergo lu dans la programmation : « 250 m rameur », « 2 min corde à sauter », « 15 cal air bike ». */
    public static class CardioItem {
        public String raw;
        /** Type (voir {@link Cardio}). */
        public String kind;
        public int minutes;
        public int meters;
        /** Calories demandées (« 15 cal »), 0 sinon. */
        public int kcal;
        /** Durée déduite de la distance ou des calories. */
        public boolean estimated;

        /** « 250 m (≈ 1 min) », « 2 min », « 15 cal (≈ 1 min) » */
        public String summary() {
            StringBuilder sb = new StringBuilder();
            if (meters > 0) sb.append(Cardio.distance(kind, meters));
            else if (kcal > 0) sb.append(kcal).append(" cal");
            if (sb.length() > 0) {
                if (estimated) sb.append(" (≈ ").append(minutes).append(" min)");
                else sb.append(" · ").append(minutes).append(" min");
            } else {
                sb.append(minutes).append(" min");
            }
            return sb.toString();
        }
    }

    public static class Result {
        public final List<Item> items = new ArrayList<>();
        public final List<CardioItem> cardio = new ArrayList<>();
        public final List<String> ignored = new ArrayList<>();
        /** Partie WOD détectée (AMRAP, EMOM, For Time…), ou null. */
        public Wod wod;
    }

    private static final String NUM = "(\\d{1,3}(?:[.,]\\d{1,2})?)";
    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern BULLET = Pattern.compile("^(?:[-–—•*·>]+\\s*|\\d{1,2}[.)]\\s+|[A-Ha-h]\\d?\\s*[).:]\\s*"
            + "|(?:bloc|block|partie|part|superset)\\s*[0-9A-Ha-h]{1,2}\\s*[:.)-]\\s*)", Pattern.CASE_INSENSITIVE);
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
    /** Lettre de partie : « C) », « D. », « E: ». */
    private static final Pattern LABEL = Pattern.compile("^(?:[A-Ha-h]\\d?\\s*[).:]|(?:bloc|block|partie|part|superset)\\s*[0-9A-Ha-h]{1,2}\\s*[:.)-])",
            Pattern.CASE_INSENSITIVE);
    /** « 8/8 » : répétitions par côté. */
    private static final Pattern SIDE = Pattern.compile("(?<![\\d.,/])(\\d{1,3})\\s*/\\s*(\\d{1,3})(?![\\d.,]|\\s*(?:kg|%|kilo))", FLAGS);
    /** « 20"/20" » : maintien par côté. */
    private static final Pattern SIDE_TIME = Pattern.compile("(\\d{1,3})\\s*(?:\"|s\\b|sec\\b)\\s*/\\s*(\\d{1,3})\\s*(?:\"|s\\b|sec\\b)?", FLAGS);
    /** « Find 5 rep », « find a heavy rep », « trouver ton 3RM », « Top set : 5 reps », « 1RM ». */
    private static final Pattern FIND = Pattern.compile("\\b(?:find|trouve[rz]?|cherche[rz]?|monte[rz]?\\s+(?:jusqu'?\\s*)?[àa]?)\\s+"
            + "(?:(?:a|an|un|une|ton|ta|le|la|ta|your)\\s+)?(?:(\\d{1,2})\\s*)?(?:(?:heavy|lourde?s?|max(?:imal)?|bonne?s?)\\s+)?"
            + "(rm\\b|reps?\\b|r[ée]p(?:s|[ée]titions?)?\\b|single\\b|double\\b|triple\\b)", FLAGS);
    private static final Pattern TOP_SET = Pattern.compile("\\btop\\s*-?\\s*set\\s*:?\\s*(?:de\\s+|of\\s+)?(\\d{1,2})\\s*(?:reps?|r[ée]ps?|r[ée]p[ée]titions?)\\b", FLAGS);
    private static final Pattern RM = Pattern.compile("(?<![\\dx×*])(\\d{1,2})\\s*rm\\b", FLAGS);
    /** Back-off relatif : « @-10% » (10 % de moins que la ligne d'avant). */
    private static final Pattern REL = Pattern.compile("(?:@\\s*)?[-−–]\\s*(\\d{1,2})\\s*%", FLAGS);
    private static final Pattern BACKOFF_WORD = Pattern.compile("\\bback\\s*-?\\s*off\\b", FLAGS);
    /** Repos dans la ligne : « R. 2' », « Pause 1' », « repos 90 s », « rest 2 min ». */
    private static final Pattern REST_IN = Pattern.compile("(?:^|[\\s.,;])(?:r\\.?|rest|repos|r[ée]cup(?:[ée]ration)?|pause)\\s*:?\\s*(\\d{1,3})\\s*"
            + "('|min\\b|mn\\b|minutes?\\b|\"|s\\b|sec\\b|secondes?\\b)", FLAGS);
    private static final Pattern RIR = Pattern.compile("\\b(?:rir|ri)\\s*:?\\s*(\\d)(?:\\s*[-–à]\\s*(\\d))?\\b", FLAGS);
    /** Consigne de tempo en fin de nom : « Back squat stato 1" en montée ». */
    private static final Pattern NOTE_TAIL = Pattern.compile("\\s+(?:stato|tempo|isom[ée]trique|pause\\s+\\d|iso\\s+\\d)\\b.*$", FLAGS);
    /** Mots du rôle de la série, à retirer du nom. */
    private static final Pattern ROLE_WORDS = Pattern.compile("\\b(?:top\\s*-?\\s*set|back\\s*-?\\s*off(?:\\s*sets?)?|working\\s*sets?|sets?|find|heavy)\\b", FLAGS);
    /** Titres de partie sans exercice : « FORCE », « Warm up », « Accessoires »… */
    private static final java.util.Set<String> HEADERS = new java.util.HashSet<>(java.util.Arrays.asList(
            "force", "strength", "warm up", "warmup", "warn up", "echauffement", "mobilite", "mobility", "skill", "accessoire",
            "accessory", "finisher", "conditioning", "endurance", "cardio", "gainage", "core", "technique", "activation", "prehab",
            "cool down", "retour au calme", "etirement", "stretching", "muscu", "musculation", "hypertrophie", "puissance",
            "power", "bloc", "block", "partie", "part", "travail", "metcon", "wod"));
    private static final Pattern AMOUNT = Pattern.compile(
            "(\\d{1,5}(?:[.,]\\d{1,2})?)\\s*(km|kms|m|mt|mètres?|metres?|kcal|cals?|calories?|min|mn|minutes?|sec|secondes?|s|'|\")(?![a-zà-ü])", FLAGS);
    private static final Pattern CLOCK = Pattern.compile("\\b(\\d{1,2}):(\\d{2})\\b");
    private static final Pattern REST_LINE = Pattern.compile(
            "^(?:(?:\\d{1,3}\\s*(?:min|mn|minutes?|sec|secondes?|s|'|\")?\\s*(?:de\\s+)?)?(?:repos|rest|r[ée]cup[ée]ration|r[ée]cup)\\b.*"
                    + "|(?:r\\.?|pause|repos|rest|r[ée]cup)\\s*:?\\s*\\d{1,3}\\s*(?:'|min|mn|minutes?|\"|s|sec|secondes?)?.*)$", FLAGS);

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
        // Dernier exercice lu juste au-dessus (pour réunir les lignes d'une montée en charge).
        Item last = null;
        // Tours du bloc en cours (« Endurance : 3 tours »), appliqués au cardio.
        int rounds = 1;
        for (String raw : normalize(text).replace('\r', '\n').split("\n")) {
            String trimmed = raw.trim();
            String line = BULLET.matcher(trimmed).replaceFirst("").trim();
            if (line.isEmpty()) {
                inWod = false;
                last = null;
                rounds = 1;
                if (pending != null) res.ignored.add(pending);
                pending = null;
                continue;
            }
            String n = key(line);
            if (isWodHeader(n)) {
                res.wod = newWod(line, n);
                inWod = true;
                last = null;
                if (pending != null) res.ignored.add(pending);
                pending = null;
                continue;
            }
            if (inWod && !PART.matcher(trimmed).find()) {
                res.wod.lines.add(line);
                continue;
            }
            inWod = false;
            if (HEADERS.contains(n)) {
                // Titre de partie (« FORCE », « Warm up ») : compris, rien à noter.
                if (pending != null) res.ignored.add(pending);
                pending = null;
                last = null;
                continue;
            }
            CardioItem cardio = parseCardio(line, rounds);
            if (cardio != null) {
                res.cardio.add(cardio);
                pending = null;
                last = null;
                continue;
            }
            Item it = parseLine(line);
            if (it == null) {
                if (REST_LINE.matcher(line).matches()) {
                    // « 1 min repos », « R. 2' » : le repos de l'exercice juste au-dessus.
                    int sec = restSeconds(line);
                    if (last != null && sec > 0) last.rest = sec;
                    continue;
                }
                Matcher r = ROUNDS.matcher(line);
                if (r.find()) rounds = Math.max(1, Math.min(20, Integer.parseInt(r.group(1))));
                if (pending != null) res.ignored.add(pending);
                pending = line;
                last = null;
                continue;
            }
            if (it.name.isEmpty() && pending != null) {
                // Nom sur la ligne d'avant (« BACK SQUAT stato 1" en montée », puis « TOP SET : find 5 rep »).
                String[] nn = splitNote(pending);
                it.name = tidy(cleanName(nn[0]));
                it.note = join(nn[1], it.note);
            } else if (it.name.isEmpty() && last != null) {
                // Ligne sans nom sous un exercice (« BACK OFF SET : 4x5 @-10% ») : même exercice.
                pending = null;
                addPart(last, it, rounding);
                continue;
            }
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
            // Même exercice sur la ligne suivante, sans nouvelle lettre de partie : on continue le même exercice.
            if (last != null && !LABEL.matcher(trimmed).find() && sameExercise(last, it)) {
                addPart(last, it, rounding);
                continue;
            }
            res.items.add(it);
            last = it;
        }
        if (pending != null) res.ignored.add(pending);
        if (res.wod != null && res.wod.lines.isEmpty()) res.wod = null;
        return res;
    }

    /** Ajoute une ligne à l'exercice : back-off calculé si la charge d'avant est connue, consignes et repos réunis. */
    private static void addPart(Item last, Item it, double rounding) {
        if (it.backoff > 0 && it.weights.isEmpty()) {
            Item prev = last.more.isEmpty() ? last : last.more.get(last.more.size() - 1);
            if (!prev.weights.isEmpty()) {
                double base = prev.weights.get(prev.weights.size() - 1);
                it.weights.add(Programs.round(base * (100 - it.backoff) / 100.0, rounding));
            }
        }
        last.more.add(it);
        last.note = join(last.note, it.note);
        if (it.rest > 0) last.rest = it.rest;
    }

    private static String join(String a, String b) {
        if (a == null || a.isEmpty()) return b == null || b.isEmpty() ? null : b;
        if (b == null || b.isEmpty()) return a;
        return a + " · " + b;
    }

    /** Sépare le nom de la consigne de tempo : {nom, consigne ou null}. */
    static String[] splitNote(String s) {
        Matcher m = NOTE_TAIL.matcher(s);
        if (m.find()) return new String[]{s.substring(0, m.start()), Fmt.cap(m.group().trim())};
        return new String[]{s, null};
    }

    /** Nom écrit tout en majuscules → « Back squat » ; les sigles des noms mixtes (KB, DB, ATG) restent. */
    static String tidy(String name) {
        if (name.isEmpty() || !name.equals(name.toUpperCase(Locale.FRENCH)) || name.equals(name.toLowerCase(Locale.FRENCH))) {
            return name;
        }
        return Fmt.cap(name.toLowerCase(Locale.FRENCH));
    }

    /** Durée d'un repos écrit « 2' », « 90 s », « 1 min 30 », en secondes. */
    static int restSeconds(String line) {
        int sec = 0;
        Matcher a = Pattern.compile("(\\d{1,3})\\s*('|min\\b|mn\\b|minutes?\\b|\"|s\\b|sec\\b|secondes?\\b)", FLAGS).matcher(line);
        while (a.find()) {
            int v = Integer.parseInt(a.group(1));
            String u = a.group(2).toLowerCase(Locale.ROOT);
            sec += u.startsWith("'") || u.startsWith("m") ? v * 60 : v;
        }
        return Math.min(1800, sec);
    }

    /** Lettres cyrilliques qui ressemblent aux latines (« КВ » pour « KB »), guillemets typographiques. */
    static String normalize(String text) {
        String cyr = "АВЕКМНОРСТХУаеорсухкмнвтіј";
        String lat = "ABEKMHOPCTXYaeopcyxkmhbtij";
        StringBuilder b = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int k = cyr.indexOf(ch);
            if (k >= 0) ch = lat.charAt(k);
            else if (ch == '″' || ch == '“' || ch == '”' || ch == '„') ch = '"';
            else if (ch == '′' || ch == '’' || ch == '‘' || ch == '`' || ch == '´') ch = '\'';
            b.append(ch);
        }
        return b.toString();
    }

    /** Analyse une ligne « exercice + séries × reps (+ charge) », ou null si aucune série n'est trouvée. */
    static Item parseLine(String line) {
        String work = " " + line + " ";
        Scheme sc = null;
        Item it = new Item();
        it.raw = line;
        boolean topSet = Pattern.compile("\\btop\\s*-?\\s*set\\b", FLAGS).matcher(work).find();

        Matcher m = SRW.matcher(work);
        if (m.find()) {
            sc = scheme(m, m.group(1), m.group(2));
            it.weights.add(Fmt.parse(m.group(3)));
        } else if ((m = SR.matcher(work)).find()) {
            sc = scheme(m, m.group(1), m.group(2));
        } else if ((m = SERIES.matcher(work)).find()) {
            sc = scheme(m, m.group(1), m.group(2));
        } else if ((m = FIND.matcher(work)).find()) {
            // « Find 5 rep », « find a heavy rep » : une série dont la charge est à trouver.
            sc = single(m, findReps(m.group(1), m.group(2)));
            it.label = topSet ? "Top set" : "À trouver";
        } else if ((m = TOP_SET.matcher(work)).find()) {
            sc = single(m, Integer.parseInt(m.group(1)));
            it.label = "Top set";
        } else if ((m = RM.matcher(work)).find()) {
            sc = single(m, Integer.parseInt(m.group(1)));
            it.label = topSet ? "Top set" : m.group(1) + "RM";
        } else if ((m = SIDE_TIME.matcher(work)).find() && m.group(1).equals(m.group(2))) {
            // « 20"/20" » : maintien par côté, une série.
            sc = single(m, 1);
            it.holdSec = Integer.parseInt(m.group(1));
            it.label = it.holdSec + " s par côté";
        } else if ((m = SIDE.matcher(work)).find() && m.group(1).equals(m.group(2))) {
            // « 8/8 » : répétitions par côté, une série.
            sc = single(m, Integer.parseInt(m.group(1)));
            it.label = "Par côté";
        }
        if (sc == null || sc.sets <= 0 || sc.sets > 20 || (!sc.max && (sc.reps <= 0 || sc.reps > 200))) return null;
        it.sets = sc.sets;
        it.reps = sc.reps;
        it.max = sc.max;
        // « 3x8/8 » : par côté.
        Matcher side = Pattern.compile("^\\s*/\\s*(\\d{1,3})(?![\\d.,]|\\s*(?:kg|%))").matcher(work.substring(sc.end));
        if (it.label == null && side.find() && Integer.parseInt(side.group(1)) == it.reps) {
            sc.end += side.end();
            it.label = "Par côté";
        }
        String rest = work.substring(0, sc.start) + " " + work.substring(sc.end);

        // Repos et RIR dans la ligne.
        Matcher r = REST_IN.matcher(rest);
        if (r.find()) {
            it.rest = restSeconds(r.group().trim());
            rest = rest.substring(0, r.start()) + " " + rest.substring(r.end());
        }
        Matcher rir = RIR.matcher(rest);
        if (rir.find()) {
            it.note = "RIR " + rir.group(1) + (rir.group(2) != null ? "-" + rir.group(2) : "");
            rest = rest.substring(0, rir.start()) + " " + rest.substring(rir.end());
        }
        // Back-off : « @-10% » ou « back off … 90 % du top set ».
        Matcher rel = REL.matcher(rest);
        if (rel.find()) {
            it.backoff = Integer.parseInt(rel.group(1));
            rest = rest.substring(0, rel.start()) + " " + rest.substring(rel.end());
        } else if (BACKOFF_WORD.matcher(rest).find() && topSet) {
            Matcher p = PERCENT.matcher(rest);
            if (p.find()) {
                int v = Integer.parseInt(p.group(1));
                if (v >= 50 && v < 100) {
                    it.backoff = 100 - v;
                    rest = rest.substring(0, p.start()) + " " + rest.substring(p.end());
                }
            }
        }
        if (it.backoff > 0) it.label = "Back-off −" + it.backoff + " %";
        else if (it.label == null && BACKOFF_WORD.matcher(rest).find()) it.label = "Back-off";
        else if (it.label == null && topSet) it.label = "Top set";

        if (it.backoff == 0) {
            Matcher p = PERCENT.matcher(rest);
            if (p.find()) {
                it.percent = Integer.parseInt(p.group(1));
                rest = rest.substring(0, p.start()) + " " + rest.substring(p.end());
            }
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
        String[] nn = splitNote(rest);
        rest = nn[0];
        it.note = join(nn[1], it.note);
        rest = NOISE.matcher(rest).replaceAll(" ");
        rest = ROLE_WORDS.matcher(rest).replaceAll(" ");
        if (it.weights.isEmpty() && it.percent == 0 && it.backoff == 0) {
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
        String name = cleanName(rest);
        // Restes d'une consigne (« a », « un ») : pas un nom.
        if (key(name).matches("(?:a|an|un|une|de|of|la|le)?")) name = "";
        it.name = tidy(name);
        return it;
    }

    private static Scheme single(Matcher m, int reps) {
        Scheme s = new Scheme();
        s.start = m.start();
        s.end = m.end();
        s.sets = 1;
        s.reps = reps;
        return s;
    }

    private static int findReps(String n, String word) {
        if (n != null) return Integer.parseInt(n);
        String w = word.toLowerCase(Locale.ROOT);
        if (w.startsWith("double")) return 2;
        if (w.startsWith("triple")) return 3;
        return 1;
    }

    private static boolean sameExercise(Item a, Item b) {
        if (a.exerciseId != 0 || b.exerciseId != 0) return a.exerciseId == b.exerciseId;
        return key(a.name).equals(key(b.name));
    }

    // ---------------------------------------------------------------- cardio

    /** Mots du cardio, du plus précis au plus général : {expression, type}. */
    private static final String[][] CARDIO_WORDS = {
            {"corde a sauter", "corde"}, {"jump rope", "corde"}, {"single under", "corde"}, {"double under", "corde"}, {"corde", "corde"},
            {"assault bike", "air_bike"}, {"echo bike", "air_bike"}, {"air bike", "air_bike"}, {"airbike", "air_bike"},
            {"assault", "air_bike"}, {"echo", "air_bike"},
            {"bike erg", "velo_appart"}, {"bikeerg", "velo_appart"}, {"velo d appartement", "velo_appart"}, {"spinning", "velo_appart"},
            {"velo", "velo_appart"}, {"bike", "velo_appart"},
            {"ski erg", "skierg"}, {"skierg", "skierg"}, {"ski", "skierg"},
            {"rameur", "rameur"}, {"rower", "rameur"}, {"rowerg", "rameur"}, {"row erg", "rameur"}, {"concept2", "rameur"},
            {"rowing", "rameur"}, {"row", "rameur"},
            {"tapis", "tapis"}, {"treadmill", "tapis"},
            {"course a pied", "course"}, {"course", "course"}, {"running", "course"}, {"run", "course"}, {"footing", "course"},
            {"jogging", "course"}, {"sprint", "course"},
            {"elliptique", "elliptique"}, {"elliptical", "elliptique"},
            {"natation", "natation"}, {"nage", "natation"}, {"swim", "natation"}, {"swimming", "natation"},
            {"marche", "marche"}, {"walk", "marche"}};
    /** Exercices de force qui contiennent un mot du cardio (rowing haltère, farmer walk…). */
    private static final String[] NOT_CARDIO = {"haltere", "barre", "dumbbell", "db", "kettlebell", "kb", "upright", "ring", "renegade",
            "seal", "pendlay", "t bar", "farmer", "carry", "lunge", "fente", "sled", "traineau", "prowler", "yoke", "sandbag", "sac",
            "bear", "crawl", "inverse", "un bra", "machine"};
    /** Allure moyenne pour déduire une durée d'une distance (m par minute). */
    private static double metersPerMinute(String kind) {
        switch (kind) {
            case "rameur":
                return 250;
            case "skierg":
                return 230;
            case "velo_appart":
            case "air_bike":
            case "velo":
                return 400;
            case "course":
            case "tapis":
                return 170;
            case "natation":
                return 40;
            case "marche":
                return 100;
            default:
                return 150;
        }
    }

    static String cardioKind(String text) {
        String k = key(text);
        for (String bad : NOT_CARDIO) if (containsPhrase(k, bad)) return null;
        for (String[] w : CARDIO_WORDS) if (containsPhrase(k, w[0])) return w[1];
        return null;
    }

    /** « 250 m rameur », « 2 min corde à sauter », « Course 5x400 m », « 15 cal air bike » ; null si ce n'est pas du cardio. */
    static CardioItem parseCardio(String line, int rounds) {
        String kind = cardioKind(line);
        if (kind == null) return null;
        int meters = 0, kcal = 0;
        double seconds = 0;
        boolean found = false;
        Matcher a = AMOUNT.matcher(line);
        int first = -1;
        while (a.find()) {
            if (first < 0) first = a.start();
            double v = Fmt.parse(a.group(1));
            if (Double.isNaN(v) || v <= 0) continue;
            String u = a.group(2).toLowerCase(Locale.FRENCH);
            found = true;
            if (u.startsWith("km")) meters += (int) Math.round(v * 1000);
            else if (u.equals("m") || u.equals("mt") || u.startsWith("mè") || u.startsWith("me")) meters += (int) Math.round(v);
            else if (u.startsWith("kcal") || u.startsWith("cal")) kcal += (int) Math.round(v);
            else if (u.startsWith("min") || u.equals("mn") || u.equals("'")) seconds += v * 60;
            else seconds += v;
        }
        if (seconds == 0) {
            Matcher c = CLOCK.matcher(line);
            if (c.find()) {
                seconds = Integer.parseInt(c.group(1)) * 60 + Integer.parseInt(c.group(2));
                found = true;
            }
        }
        if (!found) return null;
        // « 5x400 m » : multiplicateur juste avant la première quantité.
        int times = rounds;
        if (first > 0) {
            Matcher x = Pattern.compile("(\\d{1,2})\\s*[x×*]\\s*$").matcher(line.substring(0, first));
            if (x.find()) times *= Integer.parseInt(x.group(1));
        }
        CardioItem c = new CardioItem();
        c.raw = line;
        c.kind = kind;
        c.meters = meters * times;
        c.kcal = kcal * times;
        if (seconds > 0) {
            c.minutes = Math.max(1, (int) Math.ceil(seconds * times / 60.0));
        } else if (c.meters > 0) {
            c.minutes = Math.max(1, (int) Math.round(c.meters / metersPerMinute(kind)));
            c.estimated = true;
        } else if (c.kcal > 0) {
            c.minutes = Math.max(1, (int) Math.round(c.kcal / ("air_bike".equals(kind) ? 15.0 : 12.0)));
            c.estimated = true;
        } else {
            return null;
        }
        return c;
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
    /** Mots qui font d'un mouvement une variante à part. */
    private static final String[] VARIANTS = {"single leg", "single arm", "unilateral", "unilaterale", "unijambiste", "une jambe",
            "1 jambe", "atg", "deficit", "sl"};
    /** Noms reconnus seulement quand ils forment tout le nom. */
    private static final Map<String, Long> EXACT = new LinkedHashMap<>();

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
        alias(24, "power clean", "clean");
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
        alias(61, "z press", "zpress");
        alias(62, "rotation d epaule", "rotation epaule", "rotation de l epaule", "rotation externe", "rotation interne",
                "external rotation", "internal rotation", "cuban rotation", "cercle de bra", "rotation de bra");
        alias(63, "band pull apart", "pull apart", "ecartement elastique");
        // « Épaulé » seul (sans accent : « epaule ») : la ligne entière, pour ne pas confondre avec l'épaule.
        EXACT.put("epaule", 24L);
        EXACT.put("epaule power", 24L);
    }

    /** Correspondance avec la bibliothèque : nom exact, alias, puis mots en commun. */
    public static ExerciseMatcher libraryMatcher(List<Exercise> exercises) {
        return name -> {
            String n = key(name);
            if (n.isEmpty()) return 0;
            for (Exercise e : exercises) if (key(e.name).equals(n)) return e.id;
            Long exact = EXACT.get(n);
            if (exact != null) return exact;
            // Variante (« single leg hip thrust », « ATG split squat ») : un exercice à part, pas le mouvement de base.
            for (String v : VARIANTS) if (containsPhrase(n, v)) return 0;
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

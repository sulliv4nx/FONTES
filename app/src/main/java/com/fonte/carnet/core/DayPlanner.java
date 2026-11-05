package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Journée type sur le principe d'une diète de préparateur : des repas à heures fixes, chacun fait de blocs
 * (glucides, protéines, légumes ou fruit, lipides) avec un aliment de base et ses équivalences.
 * <ul>
 * <li>protéines réparties à parts égales entre les repas, lipides aussi (moitié avant la séance) ;</li>
 * <li>glucides : plus avant et après la séance les jours d'entraînement ; le jour de repos, le repas d'avant la séance
 * n'a pas de féculent (protéines, fruit, lipides) ;</li>
 * <li>quantités de chaque repas calculées pour tomber sur ses protéines, glucides et lipides ;</li>
 * <li>équivalences : même quantité du nutriment principal du bloc.</li>
 * </ul>
 */
public final class DayPlanner {
    private DayPlanner() {
    }

    public static final String CARBS = "glucides";
    public static final String PROTEIN = "proteines";
    public static final String FAT = "lipides";
    public static final String FRUIT = "fruit";
    public static final String VEG = "legumes";

    public static final String BREAKFAST = "petit-déjeuner";
    public static final String SNACK = "collation";
    public static final String LUNCH = "déjeuner";
    public static final String DINNER = "dîner";
    public static final String PRE = "avant la séance";
    public static final String POST = "après la séance";
    public static final String PRESNACK = "presnack";

    /** Catalogue proposé dans les réglages : {identifiant, libellé}. « q:… » = aliment de la table Ciqual cherché par son nom. */
    public static final String[][] CARB_CHOICES = {{"riz_blanc", "Riz"}, {"patate_douce", "Patate douce"}, {"quinoa", "Quinoa"},
            {"pain_complet", "Pain complet"}, {"avoine", "Flocons d'avoine"}, {"pates", "Pâtes"}, {"riz_complet", "Riz complet"},
            {"semoule", "Semoule"}, {"pdt", "Pomme de terre"}, {"q:manioc", "Manioc"}, {"q:fruit a pain", "Fruit à pain"}};
    public static final String[][] PROTEIN_CHOICES = {{"poulet_blanc", "Poulet"}, {"dinde", "Dinde"}, {"cabillaud", "Cabillaud"},
            {"thon", "Thon"}, {"steak_5", "Bœuf maigre"}, {"blanc_oeuf", "Blanc d'œuf"}, {"whey", "Whey"}, {"morue", "Morue"},
            {"oeuf", "Œufs entiers"}, {"cabri", "Cabri"}};
    public static final String[][] FAT_CHOICES = {{"huile", "Huile d'olive"}, {"huile_lin", "Huile de lin"}, {"amandes", "Amandes"},
            {"noix", "Noix"}, {"avocat", "Avocat"}};
    public static final String[][] FRUIT_CHOICES = {{"banane", "Banane"}, {"mangue", "Mangue"}, {"ananas", "Ananas"}, {"pomme", "Pomme"},
            {"orange", "Orange"}};
    public static final String[][] VEG_CHOICES = {{"bredes", "Brèdes"}, {"chouchou", "Chouchou"}, {"haricots_verts", "Haricots verts"},
            {"brocoli", "Brocoli"}, {"courgette", "Courgette"}, {"epinards", "Épinards"}};

    static final String[] DEFAULT_CARBS = {"riz_blanc", "patate_douce", "quinoa", "pain_complet", "avoine"};
    static final String[] DEFAULT_PROTEINS = {"poulet_blanc", "dinde", "cabillaud", "thon", "steak_5", "blanc_oeuf", "whey"};
    static final String[] DEFAULT_FATS = {"huile", "huile_lin", "amandes", "noix"};
    static final String[] DEFAULT_FRUITS = {"banane", "mangue", "ananas"};
    static final String[] DEFAULT_VEG = {"bredes", "chouchou", "haricots_verts", "brocoli"};

    public static String[][] choices(String role) {
        switch (role) {
            case CARBS:
                return CARB_CHOICES;
            case PROTEIN:
                return PROTEIN_CHOICES;
            case FAT:
                return FAT_CHOICES;
            case FRUIT:
                return FRUIT_CHOICES;
            default:
                return VEG_CHOICES;
        }
    }

    /** Trouve un aliment par son identifiant (base Fonte, table Ciqual, aliments perso). */
    public interface Resolver {
        Food find(String id);
    }

    // ---------------------------------------------------------------- résultat

    public static class Alt {
        public final Food food;
        public final double grams;

        Alt(Food food, double grams) {
            this.food = food;
            this.grams = grams;
        }
    }

    public static class Line {
        public String role;
        public Food food;
        public double grams;
        public double kcal, protein, carbs, fat;
        public final List<Alt> alts = new ArrayList<>();
        /** Légumes « au choix » : les autres légumes sont proposés tels quels. */
        public final List<Food> choice = new ArrayList<>();
    }

    public static class Slot {
        public int index;
        public int minutes;
        public String kind;
        public final List<Line> lines = new ArrayList<>();
        public double kcal, protein, carbs, fat;
        public double targetKcal, targetProtein, targetCarbs, targetFat;

        /** Repas principal (pas la collation d'avant la séance). */
        public boolean main() {
            return !PRESNACK.equals(kind);
        }
    }

    public static class Day {
        public boolean training;
        public int sessionTime;
        public final List<Slot> slots = new ArrayList<>();
        public double kcal, protein, carbs, fat;
        public int targetKcal, targetProtein, targetCarbs, targetFat;

        public Slot slot(int index) {
            for (Slot s : slots) if (s.index == index && s.main()) return s;
            return null;
        }
    }

    // ---------------------------------------------------------------- horaires

    /** Horaires des repas principaux et rôle de chacun. */
    public static class Schedule {
        public int[] times;
        public String[] kinds;
        /** Repas juste avant / juste après la séance (-1 s'il n'y en a pas). */
        public int pre = -1;
        public int post = -1;
    }

    private static int round15(double m) {
        return (int) (Math.round(m / 15.0) * 15);
    }

    private static void spread(int[] t, int from, int to, int a, int b) {
        int n = to - from;
        for (int i = from; i <= to; i++) t[i] = n == 0 ? a : round15(a + (b - a) * (double) (i - from) / n);
    }

    public static Schedule schedule(DayPrefs p) {
        int n = Math.max(4, Math.min(6, p.meals));
        int first = p.wake + 30;
        int session = p.training;
        Schedule s = new Schedule();
        s.times = new int[n];
        if (session <= first + 30) {
            // Séance au réveil : le premier repas vient après.
            first = session + 90;
            s.post = 0;
        }
        int end = Math.max(p.wake + 14 * 60 + 45, Math.min(23 * 60, session + 120));
        end = Math.min(23 * 60 + 30, Math.max(end, first + (n - 1) * 120));
        spread(s.times, 0, n - 1, first, end);
        if (s.post < 0 && session >= first + 150) {
            for (int i = 0; i < n; i++) if (s.times[i] < session) s.pre = i;
            if (s.pre > 0) {
                int preTime = Math.max(session - 150, first + 120 * s.pre);
                spread(s.times, 0, s.pre, first, preTime);
            }
            if (s.pre >= 0 && s.pre + 1 < n) {
                s.post = s.pre + 1;
                int postTime = session + 120;
                int last = Math.min(23 * 60 + 30, Math.max(end, postTime + 150 * (n - 1 - s.post)));
                spread(s.times, s.post, n - 1, postTime, last);
            }
        }
        for (Map.Entry<Integer, Integer> e : p.times.entrySet()) {
            if (e.getKey() >= 0 && e.getKey() < n && e.getValue() >= 0 && e.getValue() < 1440) s.times[e.getKey()] = e.getValue();
        }
        s.kinds = new String[n];
        int lunch = -1;
        int best = Integer.MAX_VALUE;
        for (int i = 1; i < n; i++) {
            if (i == s.pre || i == s.post) continue;
            int d = Math.abs(s.times[i] - (12 * 60 + 30));
            if (d < best && d <= 120) {
                best = d;
                lunch = i;
            }
        }
        for (int i = 0; i < n; i++) {
            if (i == s.pre) s.kinds[i] = PRE;
            else if (i == s.post) s.kinds[i] = POST;
            else if (i == 0) s.kinds[i] = BREAKFAST;
            else if (i == lunch) s.kinds[i] = LUNCH;
            else if (i == n - 1 && s.times[i] >= 18 * 60) s.kinds[i] = DINNER;
            else s.kinds[i] = SNACK;
        }
        return s;
    }

    /** « Repas 3 · déjeuner » ; le jour de repos, le repas d'avant la séance devient une collation. */
    public static String title(Slot s, boolean training) {
        if (PRESNACK.equals(s.kind)) return "Collation avant la séance";
        String k = s.kind;
        if (!training && (PRE.equals(k) || POST.equals(k))) k = PRE.equals(k) ? SNACK : (s.minutes >= 18 * 60 ? DINNER : LUNCH);
        return "Repas " + (s.index + 1) + " · " + k;
    }

    // ---------------------------------------------------------------- aliments

    private static final Map<String, Food> QUERY_CACHE = new HashMap<>();

    /** Aliment d'un identifiant ; « q:manioc » = le premier aliment Ciqual dont le nom commence par « manioc ». */
    public static Food resolve(String id, Resolver r) {
        if (id == null) return null;
        if (id.startsWith("q:")) {
            String q = id.substring(2);
            synchronized (QUERY_CACHE) {
                if (QUERY_CACHE.containsKey(q)) return QUERY_CACHE.get(q);
            }
            if (!Ciqual.loaded()) return null;
            Food found = null;
            String head = FoodSearch.norm(q);
            Food cooked = null;
            for (Food f : Ciqual.all()) {
                String n = FoodSearch.norm(f.name);
                if (!n.startsWith(head)) continue;
                if (found == null) found = f;
                if (cooked == null && n.contains("cuit")) cooked = f;
            }
            Food pick = cooked != null ? cooked : found;
            synchronized (QUERY_CACHE) {
                QUERY_CACHE.put(q, pick);
            }
            return pick;
        }
        return r.find(id);
    }

    /** Aliments retenus d'une famille, dans l'ordre du catalogue, puis les autres (Ciqual, perso). */
    public static List<Food> selected(DayPrefs p, String role, Resolver r) {
        List<Food> out = new ArrayList<>();
        java.util.Set<String> set = p.family(role);
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String[] c : choices(role)) {
            if (!set.contains(c[0])) continue;
            seen.add(c[0]);
            Food f = resolve(c[0], r);
            if (f != null) out.add(f);
        }
        for (String id : set) {
            if (seen.contains(id)) continue;
            Food f = resolve(id, r);
            if (f != null) out.add(f);
        }
        return out;
    }

    private static final String[] BREAKFAST_CARBS = {"avoine", "pain_complet"};
    private static final String[] BREAKFAST_PROTEINS = {"blanc_oeuf", "whey", "oeuf"};
    private static final String[] BREAKFAST_FATS = {"amandes", "noix"};

    private static Food base(Map<String, String> firsts, int slot, String role, String kind, List<Food> pool, Resolver r) {
        String forced = firsts.get(DayPrefs.firstKey(slot, role));
        if (forced != null) {
            Food f = resolve(forced, r);
            if (f != null) return f;
        }
        if (pool.isEmpty()) return null;
        String[] pref = null;
        if (BREAKFAST.equals(kind)) {
            if (CARBS.equals(role)) pref = BREAKFAST_CARBS;
            else if (PROTEIN.equals(role)) pref = BREAKFAST_PROTEINS;
            else if (FAT.equals(role)) pref = BREAKFAST_FATS;
        }
        if (pref != null) {
            for (String id : pref) for (Food f : pool) if (id.equals(f.id)) return f;
        }
        return pool.get(0);
    }

    /** Au petit-déjeuner, les équivalences proposent d'abord les aliments du matin. */
    private static List<Food> prefer(List<Food> pool, String kind, String role) {
        if (!BREAKFAST.equals(kind)) return pool;
        String[] pref = CARBS.equals(role) ? BREAKFAST_CARBS : PROTEIN.equals(role) ? BREAKFAST_PROTEINS : BREAKFAST_FATS;
        List<Food> out = new ArrayList<>();
        for (String id : pref) for (Food f : pool) if (id.equals(f.id)) out.add(f);
        for (Food f : pool) if (!out.contains(f)) out.add(f);
        return out;
    }

    // ---------------------------------------------------------------- calcul

    /**
     * Journée type.
     *
     * @param kcal,protein,carbs,fat objectifs du jour (jour de séance ou de repos)
     * @param training               jour d'entraînement
     */
    public static Day build(DayPrefs p, int kcal, int protein, int carbs, int fat, boolean training, Resolver r) {
        return build(p, kcal, protein, carbs, fat, training, r, p.firsts);
    }

    /** @param firsts bases imposées « indice:famille » → identifiant (celles du jour comprises) */
    public static Day build(DayPrefs p, int kcal, int protein, int carbs, int fat, boolean training, Resolver r,
                            Map<String, String> firsts) {
        Day day = new Day();
        day.training = training;
        day.sessionTime = p.training;
        day.targetKcal = kcal;
        day.targetProtein = protein;
        day.targetCarbs = carbs;
        day.targetFat = fat;
        Schedule s = schedule(p);
        int n = s.times.length;
        List<Food> carbPool = selected(p, CARBS, r);
        List<Food> protPool = selected(p, PROTEIN, r);
        List<Food> fatPool = selected(p, FAT, r);
        List<Food> fruitPool = selected(p, FRUIT, r);
        List<Food> vegPool = selected(p, VEG, r);
        Food fruit = fruitPool.isEmpty() ? null : fruitPool.get(0);

        // Collation d'avant la séance (un fruit).
        Slot snack = null;
        if (training && p.preSnack && fruit != null && p.training - 30 > p.wake) {
            snack = new Slot();
            snack.index = -1;
            snack.kind = PRESNACK;
            snack.minutes = p.training - 30;
            // Repère : 120 g de banane ; un autre fruit en donne les mêmes glucides.
            Food sf = base(firsts, -1, FRUIT, PRESNACK, fruitPool, r);
            Food ref = resolve("banane", r);
            if (ref == null) ref = fruit;
            double g = 120;
            if (sf.carbs > 0 && ref.carbs > 0 && (sf.id == null || !sf.id.equals(ref.id))) {
                g = Math.max(60, Math.min(300, roundTo(ref.carbs * 120 / sf.carbs, 5)));
            }
            Line l = line(FRUIT, sf, g);
            alts(l, fruitPool, FRUIT);
            snack.lines.add(l);
            total(snack);
        }

        boolean[] starch = new boolean[n];
        boolean[] hasFruit = new boolean[n];
        boolean[] hasVeg = new boolean[n];
        double[] carbW = new double[n];
        double[] fatW = new double[n];
        for (int i = 0; i < n; i++) {
            String k = s.kinds[i];
            starch[i] = !(i == s.pre && !training);
            hasFruit[i] = BREAKFAST.equals(k) || (i == s.pre && fruit != null);
            hasVeg[i] = LUNCH.equals(k) || DINNER.equals(k) || POST.equals(k);
            carbW[i] = 1;
            fatW[i] = 1;
            if (training && p.aroundTraining) {
                if (i == s.pre) carbW[i] = 1.35;
                if (i == s.post) carbW[i] = 1.25;
            }
            if (training && i == s.pre) fatW[i] = 0.5;
            if (!starch[i]) carbW[i] = 0;
        }
        double carbsLeft = carbs - (snack != null ? snack.carbs : 0);
        double fixedNoStarch = 0;
        for (int i = 0; i < n; i++) {
            if (!starch[i] && hasFruit[i] && fruit != null) fixedNoStarch += fruit.carbs * 150 / 100.0;
        }
        carbsLeft -= fixedNoStarch;
        double cw = 0, fw = 0;
        for (int i = 0; i < n; i++) {
            cw += carbW[i];
            fw += fatW[i];
        }
        double macroKcal = 4.0 * protein + 4.0 * carbs + 9.0 * fat;
        double ratio = macroKcal > 0 && kcal > 0 ? kcal / macroKcal : 1;
        for (int i = 0; i < n; i++) {
            Slot slot = new Slot();
            slot.index = i;
            slot.minutes = s.times[i];
            slot.kind = s.kinds[i];
            slot.targetProtein = protein / (double) n;
            slot.targetFat = fw > 0 ? fat * fatW[i] / fw : 0;
            slot.targetCarbs = starch[i] ? (cw > 0 ? Math.max(0, carbsLeft) * carbW[i] / cw : 0)
                    : (hasFruit[i] && fruit != null ? fruit.carbs * 1.5 : 0);
            slot.targetKcal = ratio * (4 * slot.targetProtein + 4 * slot.targetCarbs + 9 * slot.targetFat);
            Food c = starch[i] ? base(firsts, i, CARBS, slot.kind, carbPool, r) : null;
            Food pr = base(firsts, i, PROTEIN, slot.kind, protPool, r);
            Food f = base(firsts, i, FAT, slot.kind, fatPool, r);
            List<Line> fixed = new ArrayList<>();
            if (hasFruit[i] && fruit != null) {
                Food fr = base(firsts, i, FRUIT, slot.kind, fruitPool, r);
                Line l = line(FRUIT, fr, starch[i] ? 100 : 150);
                alts(l, fruitPool, FRUIT);
                fixed.add(l);
            }
            if (hasVeg[i] && !vegPool.isEmpty()) {
                Line l = line(VEG, vegPool.get(0), 150);
                l.choice.addAll(vegPool);
                fixed.add(l);
            }
            solve(slot, c, pr, f, fixed);
            if (c != null) alts(find(slot, CARBS), prefer(carbPool, slot.kind, CARBS), CARBS);
            alts(find(slot, PROTEIN), prefer(protPool, slot.kind, PROTEIN), PROTEIN);
            alts(find(slot, FAT), prefer(fatPool, slot.kind, FAT), FAT);
            slot.lines.sort((x, y) -> rank(x.role) - rank(y.role));
            total(slot);
            day.slots.add(slot);
        }
        if (snack != null) {
            // La collation se place dans l'ordre des heures.
            int at = day.slots.size();
            for (int i = 0; i < day.slots.size(); i++) {
                if (day.slots.get(i).minutes > snack.minutes) {
                    at = i;
                    break;
                }
            }
            day.slots.add(at, snack);
        }
        sum(day);
        correct(day, fat, kcal, fatPool, carbPool);
        return day;
    }

    private static void sum(Day day) {
        day.kcal = day.protein = day.carbs = day.fat = 0;
        for (Slot x : day.slots) {
            total(x);
            day.kcal += x.kcal;
            day.protein += x.protein;
            day.carbs += x.carbs;
            day.fat += x.fat;
        }
    }

    /** Remplace une ligne par la même avec une autre quantité (équivalences recalculées). */
    private static void regram(Slot s, Line l, double grams, List<Food> pool) {
        int i = s.lines.indexOf(l);
        Line n = line(l.role, l.food, grams);
        alts(n, prefer(pool, s.kind, l.role), l.role);
        s.lines.set(i, n);
    }

    /**
     * Les arrondis à 5 g font perdre quelques grammes par repas : on rattrape les lipides de la journée, puis les
     * calories avec les féculents.
     */
    private static void correct(Day day, int fatTarget, int kcalTarget, List<Food> fatPool, List<Food> carbPool) {
        for (int it = 0; it < 6; it++) {
            double gap = fatTarget - day.fat;
            if (Math.abs(gap) < 3.5) break;
            Slot bestSlot = null;
            Line bestLine = null;
            for (Slot s : day.slots) {
                if (!s.main()) continue;
                Line l = find(s, FAT);
                if (l == null) continue;
                double g = l.grams + (gap > 0 ? 5 : -5);
                if (g < 5 || g > cap(l.food, FAT)) continue;
                double miss = gap > 0 ? s.targetFat - s.fat : s.fat - s.targetFat;
                if (bestLine == null || miss > (gap > 0 ? bestSlot.targetFat - bestSlot.fat : bestSlot.fat - bestSlot.targetFat)) {
                    bestSlot = s;
                    bestLine = l;
                }
            }
            if (bestLine == null) break;
            regram(bestSlot, bestLine, bestLine.grams + (gap > 0 ? 5 : -5), fatPool);
            sum(day);
        }
        if (kcalTarget <= 0) return;
        for (int it = 0; it < 8; it++) {
            double gap = kcalTarget - day.kcal;
            if (Math.abs(gap) < kcalTarget * 0.012) break;
            Slot bestSlot = null;
            Line bestLine = null;
            for (Slot s : day.slots) {
                if (!s.main()) continue;
                Line l = find(s, CARBS);
                if (l == null) continue;
                if (bestLine == null || l.grams > bestLine.grams) {
                    bestSlot = s;
                    bestLine = l;
                }
            }
            if (bestLine == null) break;
            double per10 = bestLine.food.kcal / 10.0;
            if (per10 <= 0) break;
            double delta = Math.max(-40, Math.min(40, Math.round(gap / per10) * 10));
            if (delta == 0) delta = gap > 0 ? 10 : -10;
            double g = Math.max(0, Math.min(cap(bestLine.food, CARBS), bestLine.grams + delta));
            if (g == bestLine.grams) break;
            regram(bestSlot, bestLine, g, carbPool);
            sum(day);
        }
    }

    /** Ordre d'affichage des blocs d'un repas. */
    private static int rank(String role) {
        switch (role) {
            case CARBS:
                return 0;
            case PROTEIN:
                return 1;
            case VEG:
                return 2;
            case FRUIT:
                return 3;
            default:
                return 4;
        }
    }

    private static Line find(Slot s, String role) {
        for (Line l : s.lines) if (role.equals(l.role)) return l;
        return null;
    }

    static Line line(String role, Food f, double grams) {
        Line l = new Line();
        l.role = role;
        l.food = f;
        l.grams = grams;
        double k = grams / 100.0;
        l.kcal = f.kcal * k;
        l.protein = f.protein * k;
        l.carbs = f.carbs * k;
        l.fat = f.fat * k;
        return l;
    }

    private static void total(Slot s) {
        s.kcal = s.protein = s.carbs = s.fat = 0;
        for (Line l : s.lines) {
            s.kcal += l.kcal;
            s.protein += l.protein;
            s.carbs += l.carbs;
            s.fat += l.fat;
        }
    }

    /** Nutriment principal d'une famille, pour 100 g. */
    static double main(Food f, String role) {
        switch (role) {
            case PROTEIN:
                return f.protein;
            case FAT:
                return f.fat;
            default:
                return f.carbs;
        }
    }

    private static double amount(Line l) {
        switch (l.role) {
            case PROTEIN:
                return l.protein;
            case FAT:
                return l.fat;
            default:
                return l.carbs;
        }
    }

    /** Équivalences : même quantité du nutriment principal (jusqu'à 3, quantités raisonnables seulement). */
    static void alts(Line l, List<Food> pool, String role) {
        if (l == null) return;
        double need = amount(l);
        if (need <= 0) return;
        for (Food f : pool) {
            if (l.alts.size() >= 3) break;
            if (f.id != null && f.id.equals(l.food.id)) continue;
            double d = main(f, role);
            if (d <= 0) continue;
            double g = roundTo(need * 100 / d, step(f));
            if (g <= 0 || g > Math.max(900, l.grams * 4)) continue;
            l.alts.add(new Alt(f, g));
        }
    }

    static double step(Food f) {
        return 5;
    }

    static double roundTo(double v, double step) {
        return Math.round(v / step) * step;
    }

    /** Plafonds raisonnables pour un seul repas (g). */
    static double cap(Food f, String role) {
        double c = f.id == null ? 0 : Groceries.max(f.id);
        if (c > 0) return c;
        if (FAT.equals(role)) return 40;
        if (PROTEIN.equals(role)) return 300;
        return 600;
    }

    /**
     * Quantités du repas : glucides, protéines et lipides de base, après les légumes et le fruit fixés.
     * Moindres carrés pondérés (protéines d'abord) sans quantité négative.
     */
    static void solve(Slot slot, Food carb, Food prot, Food fat, List<Line> fixed) {
        double fp = 0, fc = 0, ff = 0, fk = 0;
        for (Line l : fixed) {
            fp += l.protein;
            fc += l.carbs;
            ff += l.fat;
            fk += l.kcal;
        }
        // Équations : protéines, glucides, lipides et calories (÷ 4, pour les mettre à l'échelle des grammes).
        double[] b = {slot.targetProtein - fp, slot.targetCarbs - fc, slot.targetFat - ff, (slot.targetKcal - fk) / 4.0};
        Food[] vars = {carb, prot, fat};
        String[] roles = {CARBS, PROTEIN, FAT};
        double[] w = {4, 1, 2, 1.5};
        double[] best = new double[3];
        double bestErr = Double.MAX_VALUE;
        for (int mask = 1; mask < 8; mask++) {
            List<Integer> idx = new ArrayList<>();
            for (int j = 0; j < 3; j++) if ((mask & (1 << j)) != 0 && vars[j] != null) idx.add(j);
            if (idx.isEmpty()) continue;
            int k = idx.size();
            double[][] a = new double[4][k];
            for (int e = 0; e < 4; e++) {
                for (int j = 0; j < k; j++) a[e][j] = coef(vars[idx.get(j)], e);
            }
            double[] x = leastSquares(a, b, w);
            if (x == null) continue;
            boolean ok = true;
            for (double v : x) if (v < -1e-9) ok = false;
            if (!ok) continue;
            double[] full = new double[3];
            for (int j = 0; j < k; j++) full[idx.get(j)] = x[j];
            double err = error(vars, full, b, w);
            if (err < bestErr - 1e-9) {
                bestErr = err;
                best = full;
            }
        }
        for (int j = 0; j < 3; j++) {
            if (vars[j] == null) continue;
            double g = Math.min(best[j], cap(vars[j], roles[j]));
            g = roundTo(g, 5);
            if (g <= 0) continue;
            slot.lines.add(line(roles[j], vars[j], g));
        }
        // Ordre d'affichage : protéines, glucides, puis légumes ou fruit, puis lipides (glucides d'abord au petit-déjeuner).
        List<Line> ordered = new ArrayList<>();
        Line c = find(slot, CARBS), pr = find(slot, PROTEIN), f = find(slot, FAT);
        if (BREAKFAST.equals(slot.kind)) {
            if (c != null) ordered.add(c);
            if (pr != null) ordered.add(pr);
        } else {
            if (pr != null) ordered.add(pr);
            if (c != null) ordered.add(c);
        }
        ordered.addAll(fixed);
        if (f != null) ordered.add(f);
        slot.lines.clear();
        slot.lines.addAll(ordered);
    }

    /** Apport d'un gramme d'aliment dans l'équation e (protéines, glucides, lipides, calories ÷ 4). */
    private static double coef(Food f, int e) {
        switch (e) {
            case 0:
                return f.protein / 100.0;
            case 1:
                return f.carbs / 100.0;
            case 2:
                return f.fat / 100.0;
            default:
                return f.kcal / 400.0;
        }
    }

    private static double error(Food[] vars, double[] x, double[] b, double[] w) {
        double err = 0;
        for (int e = 0; e < b.length; e++) {
            double v = 0;
            for (int j = 0; j < 3; j++) {
                if (vars[j] == null) continue;
                v += x[j] * coef(vars[j], e);
            }
            double d = v - b[e];
            err += w[e] * d * d;
        }
        return err;
    }

    /** Moindres carrés pondérés : (AᵀWA) x = AᵀWb, par élimination de Gauss. */
    static double[] leastSquares(double[][] a, double[] b, double[] w) {
        int k = a[0].length;
        double[][] m = new double[k][k + 1];
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                double s = 0;
                for (int e = 0; e < a.length; e++) s += a[e][i] * w[e] * a[e][j];
                m[i][j] = s;
            }
            double s = 0;
            for (int e = 0; e < a.length; e++) s += a[e][i] * w[e] * b[e];
            m[i][k] = s;
        }
        for (int c = 0; c < k; c++) {
            int piv = c;
            for (int r = c + 1; r < k; r++) if (Math.abs(m[r][c]) > Math.abs(m[piv][c])) piv = r;
            if (Math.abs(m[piv][c]) < 1e-12) return null;
            double[] t = m[c];
            m[c] = m[piv];
            m[piv] = t;
            for (int r = 0; r < k; r++) {
                if (r == c) continue;
                double f = m[r][c] / m[c][c];
                for (int j = c; j <= k; j++) m[r][j] -= f * m[c][j];
            }
        }
        double[] x = new double[k];
        for (int i = 0; i < k; i++) x[i] = m[i][k] / m[i][i];
        return x;
    }

    // ---------------------------------------------------------------- textes

    private static final Map<String, String> PHRASE = new HashMap<>();

    static {
        String[][] p = {{"riz_blanc", "riz cuit"}, {"riz_complet", "riz complet cuit"}, {"patate_douce", "patate douce"},
                {"quinoa", "quinoa cuit"}, {"pain_complet", "pain complet"}, {"avoine", "flocons d'avoine"}, {"pates", "pâtes cuites"},
                {"semoule", "semoule cuite"}, {"pdt", "pomme de terre"}, {"poulet_blanc", "blanc de poulet cuit"},
                {"dinde", "escalope de dinde cuite"}, {"cabillaud", "cabillaud cuit"}, {"thon", "thon au naturel"},
                {"steak_5", "steak haché 5 %"}, {"blanc_oeuf", "blanc d'œuf"}, {"whey", "whey"}, {"morue", "morue dessalée"},
                {"oeuf", "œufs entiers"}, {"cabri", "cabri cuit"}, {"huile", "huile d'olive"}, {"huile_lin", "huile de lin"},
                {"amandes", "amandes"}, {"noix", "noix"}, {"avocat", "avocat"}};
        for (String[] x : p) PHRASE.put(x[0], x[1]);
    }

    /** Nom court en minuscules : « blanc de poulet cuit ». */
    public static String phrase(Food f) {
        String s = f.id == null ? null : PHRASE.get(f.id);
        if (s != null) return s;
        String n = f.name == null ? "" : f.name.replaceAll("\\s*\\([^)]*\\)", "").trim();
        if (n.length() > 1 && !Character.isUpperCase(n.charAt(1))) n = Character.toLowerCase(n.charAt(0)) + n.substring(1);
        return n;
    }

    private static boolean vowel(String s) {
        if (s.isEmpty()) return false;
        char c = Character.toLowerCase(s.charAt(0));
        return "aeiouyéèêëàâîïôœh".indexOf(c) >= 0;
    }

    /** « 110 g de flocons d'avoine », « 1 c. à soupe d'huile d'olive », « 1 banane ». */
    public static String text(Line l) {
        String ph = phrase(l.food);
        if (FAT.equals(l.role) && l.food.fat >= 90) {
            String spoon = spoons(l.grams);
            if (spoon != null) return spoon + (vowel(ph) ? " d'" : " de ") + ph;
        }
        if (FRUIT.equals(l.role)) {
            String a = Groceries.amount(l.food.id, l.grams);
            if (!a.endsWith(" g") && !a.endsWith(" ml")) return a;
        }
        if ("oeuf".equals(l.food.id)) {
            String a = Groceries.amount("oeuf", l.grams);
            if (!a.endsWith(" g")) return a;
        }
        if (VEG.equals(l.role)) return Fmt.kg(l.grams) + " g de légumes au choix";
        return Fmt.kg(l.grams) + " g" + (vowel(ph) ? " d'" : " de ") + ph;
    }

    /** Précision entre parenthèses : poids cru, nombre de blancs, poids du fruit. */
    public static String detail(Line l) {
        if (FAT.equals(l.role) && l.food.fat >= 90 && spoons(l.grams) != null) return Fmt.kg(l.grams) + " g";
        if (FRUIT.equals(l.role)) {
            String a = Groceries.amount(l.food.id, l.grams);
            if (!a.endsWith(" g") && !a.endsWith(" ml")) return Fmt.kg(l.grams) + " g";
            return null;
        }
        if ("blanc_oeuf".equals(l.food.id)) {
            double halves = Math.round(l.grams / 33.0 * 2);
            if (halves <= 0) return null;
            long whole = (long) Math.floor(halves / 2);
            boolean half = ((long) halves) % 2 == 1;
            if (whole == 0) return "½ blanc";
            return whole + (whole > 1 ? " blancs" : " blanc") + (half ? " ½" : "");
        }
        if ("oeuf".equals(l.food.id)) return Fmt.kg(l.grams) + " g";
        if (VEG.equals(l.role)) return null;
        String raw = l.food.id == null ? null : Groceries.rawHint(l.food.id, l.grams);
        return raw;
    }

    /** « ou 155 g de pain complet · 230 g de riz cuit », ou la liste des légumes au choix. */
    public static String altText(Line l) {
        StringBuilder b = new StringBuilder();
        if (VEG.equals(l.role)) {
            for (Food f : l.choice) {
                if (b.length() > 0) b.append(", ");
                b.append(phrase(f).replace(" cuits", "").replace(" cuites", "").replace(" cuit", ""));
            }
            return b.toString();
        }
        for (Alt a : l.alts) {
            b.append(b.length() == 0 ? "ou " : " · ");
            Line tmp = line(l.role, a.food, a.grams);
            b.append(text(tmp));
        }
        return b.toString();
    }

    static String spoons(double g) {
        if (Math.abs(g - 5) < 0.5) return "1 c. à café";
        if (Math.abs(g - 10) < 0.5) return "1 c. à soupe";
        if (Math.abs(g - 15) < 0.5) return "1,5 c. à soupe";
        if (Math.abs(g - 20) < 0.5) return "2 c. à soupe";
        if (Math.abs(g - 25) < 0.5) return "2,5 c. à soupe";
        if (Math.abs(g - 30) < 0.5) return "3 c. à soupe";
        return null;
    }

    /** Règles proposées par défaut (l'eau est calculée d'après le profil). */
    public static List<String> defaultRules(int waterRest, int waterTraining) {
        List<String> r = new ArrayList<>();
        r.add("Eau : " + Fmt.kg(waterRest / 1000.0) + " L par jour, " + Fmt.kg(waterTraining / 1000.0) + " L avec la séance");
        r.add("Viande et poisson pesés crus, riz pesé cuit : les deux poids sont affichés");
        r.add("Pesée le lundi matin, à jeun. Après 2 semaines, l'objectif est corrigé d'après la balance.");
        r.add("1 repas libre par semaine : note-le dans le journal tel quel");
        return r;
    }
}

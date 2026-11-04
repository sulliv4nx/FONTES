package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Génère une semaine de repas qui colle aux objectifs : chaque recette est ajustée
 * (portion de protéines d'un côté, féculents et matières grasses de l'autre, légumes fixes)
 * pour viser les calories et les protéines de chaque repas.
 */
public final class MealPlanner {
    private MealPlanner() {
    }

    private static final double MIN_FACTOR = 0.4;
    private static final double MIN_PROT = 0.75;
    private static final double MIN_FAT = 0.5;
    private static final double MAX_FACTOR = 2.2;
    /** Les féculents peuvent monter plus haut : les jours de séance demandent beaucoup de glucides. */
    private static final double MAX_REST = 3.0;
    /** Écart toléré pour proposer un remplacement. */
    public static final double SWAP_TOLERANCE = 0.10;

    public static class Targets {
        public int kcal;
        public int protein;
        public int carbs;
        public int fat;

        public Targets(int kcal, int protein, int carbs, int fat) {
            this.kcal = kcal;
            this.protein = protein;
            this.carbs = carbs;
            this.fat = fat;
        }
    }

    // ---------------------------------------------------------------- structure d'une journée

    /** 0 = lundi … 6 = dimanche. */
    public static int weekday(int day) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(Days.start(day));
        return (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
    }

    public static boolean shakerDay(MealPrefs p, int day) {
        if (!p.shaker) return false;
        if (p.shakerEveryDay) return true;
        int mask = p.trainingMask < 0 ? 0 : p.trainingMask;
        return (mask & (1 << weekday(day))) != 0;
    }

    /** Créneaux du jour, dans l'ordre. Le shaker remplace la collation de l'après-midi (ou s'ajoute s'il n'y en a pas). */
    public static List<String> slots(int meals, boolean shaker) {
        List<String> s = new ArrayList<>();
        s.add(MealPlan.BREAKFAST);
        if (meals >= 5) s.add(MealPlan.SNACK_AM);
        s.add(MealPlan.LUNCH);
        if (meals >= 4) s.add(shaker ? MealPlan.SHAKER : MealPlan.SNACK_PM);
        else if (shaker) s.add(MealPlan.SHAKER);
        s.add(MealPlan.DINNER);
        return s;
    }

    /** Part des calories du jour pour un créneau. */
    public static double share(List<String> slots, String slot) {
        int n = slots.size();
        switch (slot) {
            case MealPlan.BREAKFAST:
                return n == 3 ? 0.30 : n == 4 ? 0.25 : 0.24;
            case MealPlan.SNACK_AM:
                return 0.09;
            case MealPlan.LUNCH:
                return n == 3 ? 0.37 : n == 4 ? 0.32 : 0.28;
            case MealPlan.SNACK_PM:
            case MealPlan.SHAKER:
                return n == 4 ? 0.15 : 0.14;
            default:
                return n == 3 ? 0.33 : n == 4 ? 0.28 : 0.25;
        }
    }

    /** Part des protéines du jour : un peu plus aux plats et au shaker, un peu moins aux collations. */
    public static double proteinShare(List<String> slots, String slot) {
        double total = 0;
        for (String x : slots) total += proteinWeight(x);
        return proteinWeight(slot) / total;
    }

    private static double proteinWeight(String slot) {
        switch (slot) {
            case MealPlan.BREAKFAST:
                return 0.22;
            case MealPlan.SNACK_AM:
                return 0.09;
            case MealPlan.SNACK_PM:
                return 0.10;
            case MealPlan.SHAKER:
                return 0.18;
            default:
                return 0.30;
        }
    }

    // ---------------------------------------------------------------- préférences

    private static String root(String w) {
        String n = Foods.norm(w);
        if (n.length() > 3 && n.endsWith("s")) n = n.substring(0, n.length() - 1);
        return n;
    }

    private static List<String> avoidTerms(MealPrefs p) {
        List<String> out = new ArrayList<>();
        if (p.avoid == null) return out;
        for (String part : p.avoid.split("[,;/\\n]")) {
            String t = root(part.trim());
            if (t.length() >= 3) out.add(t);
        }
        return out;
    }

    static boolean avoided(Recipe r, List<String> terms) {
        if (terms.isEmpty()) return false;
        StringBuilder text = new StringBuilder(Foods.norm(r.name));
        for (Recipe.Ing i : r.ingredients) {
            Food f = Foods.find(i.foodId);
            if (f != null) text.append(' ').append(Foods.norm(f.name));
            text.append(' ').append(Foods.norm(Groceries.info(i.foodId, null).label));
        }
        String all = text.toString();
        for (String t : terms) if (all.contains(t)) return true;
        return false;
    }

    private static boolean allowed(Recipe r, MealPrefs p, boolean strict) {
        if (p.vegetarian && !r.vegetarian()) return false;
        if (p.noPork && r.has(Recipe.PORK)) return false;
        if (p.lactoseFree && r.has(Recipe.LACTOSE)) return false;
        if (strict && p.budget && r.has(Recipe.PRICEY)) return false;
        return true;
    }

    /** Recettes compatibles avec les préférences (en assouplissant si rien ne passe). */
    public static List<Recipe> candidates(MealPrefs p, String kind) {
        List<Recipe> all = Recipes.of(kind);
        List<String> terms = avoidTerms(p);
        List<Recipe> out = new ArrayList<>();
        for (Recipe r : all) if (allowed(r, p, true) && !avoided(r, terms)) out.add(r);
        if (!out.isEmpty()) return out;
        for (Recipe r : all) if (allowed(r, p, false)) out.add(r);
        if (!out.isEmpty()) return out;
        return new ArrayList<>(all);
    }

    private static double weight(Recipe r, MealPrefs p) {
        double w = 1;
        if (p.creole && r.has(Recipe.CREOLE)) w *= 3;
        if (p.budget && r.has(Recipe.BUDGET)) w *= 2;
        if (r.has(Recipe.TREAT)) w *= 0.35;
        return w;
    }

    /** Tirage pondéré sans remise. */
    static List<Recipe> pick(List<Recipe> pool, int n, MealPrefs p, Random rnd) {
        List<Recipe> left = new ArrayList<>(pool);
        List<Recipe> out = new ArrayList<>();
        while (out.size() < n && !left.isEmpty()) {
            double total = 0;
            for (Recipe r : left) total += weight(r, p);
            double x = rnd.nextDouble() * total;
            int idx = left.size() - 1;
            for (int i = 0; i < left.size(); i++) {
                x -= weight(left.get(i), p);
                if (x <= 0) {
                    idx = i;
                    break;
                }
            }
            out.add(left.remove(idx));
        }
        return out;
    }

    /** Pas plus de max recettes portant l'étiquette (par ex. poisson) dans la sélection. */
    static void limit(List<Recipe> set, List<Recipe> pool, String tag, int max, Random rnd) {
        int count = 0;
        for (int i = 0; i < set.size(); i++) {
            if (!set.get(i).has(tag)) continue;
            count++;
            if (count <= max) continue;
            List<Recipe> other = new ArrayList<>();
            for (Recipe r : pool) if (!r.has(tag) && !set.contains(r)) other.add(r);
            if (other.isEmpty()) return;
            set.set(i, other.get(rnd.nextInt(other.size())));
        }
    }

    /** Garantit au moins une recette portant l'étiquette, sans retirer celles qui portent keep. */
    static void ensure(List<Recipe> set, List<Recipe> pool, String tag, Random rnd, String... keep) {
        for (Recipe r : set) if (r.has(tag)) return;
        List<Recipe> with = new ArrayList<>();
        for (Recipe r : pool) if (r.has(tag) && !set.contains(r)) with.add(r);
        if (with.isEmpty()) return;
        Recipe add = with.get(rnd.nextInt(with.size()));
        for (int i = set.size() - 1; i >= 0; i--) {
            boolean protectedOne = false;
            for (String k : keep) if (set.get(i).has(k)) protectedOne = true;
            if (!protectedOne) {
                set.set(i, add);
                return;
            }
        }
        set.add(add);
    }

    // ---------------------------------------------------------------- ajustement d'une recette

    /** Ajuste la recette au créneau pour viser ces calories, protéines et lipides. */
    public static MealPlan.Meal build(Recipe r, int day, String slot, double kcal, double protein, double fat) {
        // Totaux par rôle pour la portion type : [protéines, reste, fixe, gras]
        double[] k = new double[4];
        double[] pr = new double[4];
        double[] fa = new double[4];
        double[] hi = {MAX_FACTOR, MAX_REST, 1, 1.6};
        for (Recipe.Ing i : r.ingredients) {
            Food f = Foods.find(i.foodId);
            if (f == null) continue;
            k[i.role] += f.kcal * i.grams / 100.0;
            pr[i.role] += f.protein * i.grams / 100.0;
            fa[i.role] += f.fat * i.grams / 100.0;
            double cap = Groceries.max(i.foodId);
            if (cap > 0 && i.role != Recipe.FIXED) hi[i.role] = Math.min(hi[i.role], cap / i.grams);
        }
        double loA = Math.min(MIN_PROT, hi[Recipe.PROT]);
        double loB = Math.min(MIN_FACTOR, hi[Recipe.REST]);
        double loC = Math.min(MIN_FAT, hi[Recipe.FAT]);
        boolean hasP = k[Recipe.PROT] > 0, hasR = k[Recipe.REST] > 0, hasF = k[Recipe.FAT] > 0;
        double[] lo = {hasP ? loA : 1, hasR ? loB : 1, 1, hasF ? loC : 1};
        double[] up = {hasP ? hi[Recipe.PROT] : 1, hasR ? hi[Recipe.REST] : 1, 1, hasF ? hi[Recipe.FAT] : 1};
        // Recherche sur grille (grossière puis fine) du meilleur compromis calories > protéines > lipides.
        double[] best = {1, 1, 1, 1};
        double bestErr = Double.MAX_VALUE;
        double[] stepFor = {0.1, 0.1, 1, 0.25};
        double[] center = null;
        for (int pass = 0; pass < 2; pass++) {
            double[] from = new double[4];
            double[] to = new double[4];
            double[] st = new double[4];
            for (int j = 0; j < 4; j++) {
                if (pass == 0) {
                    from[j] = lo[j];
                    to[j] = up[j];
                    st[j] = stepFor[j];
                } else {
                    from[j] = Math.max(lo[j], center[j] - stepFor[j]);
                    to[j] = Math.min(up[j], center[j] + stepFor[j]);
                    st[j] = stepFor[j] / 5;
                }
            }
            for (double a = from[0]; a <= to[0] + 1e-9; a += st[0]) {
                for (double b = from[1]; b <= to[1] + 1e-9; b += st[1]) {
                    for (double c = from[3]; c <= to[3] + 1e-9; c += st[3]) {
                        double kk = a * k[0] + b * k[1] + k[2] + c * k[3];
                        double pp = a * pr[0] + b * pr[1] + pr[2] + c * pr[3];
                        double ff = a * fa[0] + b * fa[1] + fa[2] + c * fa[3];
                        // Écarts exprimés en part des calories du repas, pour les comparer entre eux.
                        double scale = Math.max(100, kcal);
                        double ek = (kk - kcal) / scale;
                        double ep = 4 * (pp - protein) / scale;
                        double ef = fat > 0 ? 9 * (ff - fat) / scale : 0;
                        // Manquer de protéines compte plus qu'en avoir un peu trop.
                        double wp = ep < 0 ? 8 : 5;
                        double err = 12 * ek * ek + wp * ep * ep + 1.5 * ef * ef
                                + 0.01 * ((a - 1) * (a - 1) + (b - 1) * (b - 1));
                        if (err < bestErr) {
                            bestErr = err;
                            best = new double[]{a, b, 1, c};
                        }
                    }
                }
            }
            center = best;
        }
        double a = best[0], b = best[1], c = best[3];
        MealPlan.Meal m = new MealPlan.Meal();
        m.day = day;
        m.slot = slot;
        m.recipeId = r.id;
        m.name = r.name;
        m.targetKcal = kcal;
        m.targetProtein = protein;
        m.targetFat = fat;
        double[] factor = {a, b, 1, c};
        for (Recipe.Ing i : r.ingredients) {
            double g = i.grams * factor[i.role];
            double step = Groceries.step(i.foodId, g);
            g = Math.max(step, Math.round(g / step) * step);
            double cap = Groceries.max(i.foodId);
            if (cap > 0 && g > cap) g = Math.max(step, Math.floor(cap / step) * step);
            Food f = Foods.find(i.foodId);
            m.items.add(new MealPlan.Item(i.foodId, f != null ? f.name : i.foodId, g));
        }
        return m;
    }

    // ---------------------------------------------------------------- génération de la semaine

    private static int[] distinct(int variety) {
        // petit-déjeuner, collation, shaker, plat
        if (variety <= 0) return new int[]{1, 2, 1, 3};
        if (variety == 1) return new int[]{2, 3, 1, 5};
        return new int[]{3, 4, 2, 8};
    }

    public static MealPlan generate(MealPrefs p, Targets t, int start, long seed) {
        return generate(p, t, t, start, seed);
    }

    /**
     * Semaine de repas. Les jours d'entraînement prévus visent les objectifs « séance » (les calories de la
     * séance passent en glucides), les autres jours ceux d'un jour sans séance.
     */
    public static MealPlan generate(MealPrefs p, Targets rest, Targets training, int start, long seed) {
        Random rnd = new Random(seed);
        MealPlan plan = new MealPlan();
        plan.start = start;
        plan.days = 7;
        plan.seed = seed;
        plan.kcal = rest.kcal;
        plan.protein = rest.protein;
        plan.carbs = rest.carbs;
        plan.fat = rest.fat;
        plan.trainingMask = Math.max(0, p.trainingMask);
        if (training != null && training != rest && training.kcal > rest.kcal) {
            plan.trainKcal = training.kcal;
            plan.trainProtein = training.protein;
            plan.trainCarbs = training.carbs;
            plan.trainFat = training.fat;
        }

        int[] n = distinct(p.variety);
        List<Recipe> breakfasts = pick(candidates(p, Recipe.BREAKFAST), n[0], p, rnd);
        List<Recipe> snackPool = candidates(p, Recipe.SNACK);
        List<Recipe> snacks = pick(snackPool, n[1], p, rnd);
        List<Recipe> shakerPool = candidates(p, Recipe.SHAKER);
        List<Recipe> shakers = pick(shakerPool, n[2], p, rnd);
        List<Recipe> mainPool = candidates(p, Recipe.MAIN);
        List<Recipe> mains = pick(mainPool, n[3], p, rnd);
        limit(mains, mainPool, Recipe.FISH, Math.max(1, Math.round(mains.size() * 0.4f)), rnd);
        if (p.fish) ensure(mains, mainPool, Recipe.FISH, rnd);
        if (p.creole) ensure(mains, mainPool, Recipe.CREOLE, rnd, Recipe.FISH);

        String[][] mainOf = assignMains(p, mains, plan.days, rnd);

        int snackTurn = rnd.nextInt(Math.max(1, snacks.size()));
        for (int d = 0; d < plan.days; d++) {
            int day = Days.add(start, d);
            Targets t = plan.trainingDay(day) ? training : rest;
            List<String> slots = slots(p.meals, shakerDay(p, day));
            List<MealPlan.Meal> dayMeals = new ArrayList<>();
            String lastSnack = null;
            for (String slot : slots) {
                Recipe r;
                switch (slot) {
                    case MealPlan.BREAKFAST:
                        r = breakfasts.get(d % breakfasts.size());
                        break;
                    case MealPlan.SHAKER:
                        r = shakers.get(d % shakers.size());
                        break;
                    case MealPlan.LUNCH:
                        r = Recipes.find(mainOf[d][0]);
                        break;
                    case MealPlan.DINNER:
                        r = Recipes.find(mainOf[d][1]);
                        break;
                    default:
                        r = snacks.get(snackTurn % snacks.size());
                        if (snacks.size() > 1 && r.id.equals(lastSnack)) {
                            snackTurn++;
                            r = snacks.get(snackTurn % snacks.size());
                        }
                        snackTurn++;
                        lastSnack = r.id;
                        break;
                }
                double s = share(slots, slot);
                dayMeals.add(build(r, day, slot, t.kcal * s, t.protein * proteinShare(slots, slot), t.fat * s));
            }
            balance(dayMeals, t);
            repairProtein(dayMeals, t, snackPool);
            repairMains(dayMeals, t, mains);
            plan.meals.addAll(dayMeals);
        }
        return plan;
    }

    /** Corrige le déjeuner et le dîner pour que la journée tombe juste (les collations s'ajustent moins bien). */
    static void balance(List<MealPlan.Meal> dayMeals, Targets t) {
        double k = 0, pro = 0, fat = 0;
        for (MealPlan.Meal m : dayMeals) {
            Nutrition.Totals x = m.totals();
            k += x.kcal;
            pro += x.protein;
            fat += x.fat;
        }
        double dk = t.kcal - k;
        double dp = t.protein - pro;
        double df = t.fat - fat;
        if (Math.abs(dk) < 25 && Math.abs(dp) < 4 && Math.abs(df) < 4) return;
        for (int i = 0; i < dayMeals.size(); i++) {
            MealPlan.Meal m = dayMeals.get(i);
            if (!Recipe.MAIN.equals(m.kind())) continue;
            Recipe r = Recipes.find(m.recipeId);
            if (r == null) continue;
            dayMeals.set(i, build(r, m.day, m.slot, bounded(m.targetKcal, dk / 2), bounded(m.targetProtein, dp / 2),
                    bounded(m.targetFat, df / 2)));
        }
    }

    /** Protéines pour 100 kcal de la portion type. */
    static double proteinDensity(Recipe r) {
        double k = 0, pro = 0;
        for (Recipe.Ing i : r.ingredients) {
            Food f = Foods.find(i.foodId);
            if (f == null) continue;
            k += f.kcal * i.grams / 100.0;
            pro += f.protein * i.grams / 100.0;
        }
        return k > 0 ? pro * 100 / k : 0;
    }

    /** Si la journée manque de protéines, les collations peu protéinées sont remplacées par la plus protéinée. */
    static void repairProtein(List<MealPlan.Meal> dayMeals, Targets t, List<Recipe> snackPool) {
        if (proteinOf(dayMeals) >= t.protein * 0.95) return;
        List<Recipe> ranked = new ArrayList<>(snackPool);
        Collections.sort(ranked, (x, y) -> Double.compare(proteinDensity(y), proteinDensity(x)));
        if (ranked.isEmpty()) return;
        Set<String> used = new HashSet<>();
        boolean changed = false;
        for (int i = 0; i < dayMeals.size(); i++) {
            MealPlan.Meal m = dayMeals.get(i);
            if (!Recipe.SNACK.equals(m.kind())) continue;
            Recipe cur = Recipes.find(m.recipeId);
            Recipe best = null;
            for (Recipe r : ranked) {
                if (!used.contains(r.id)) {
                    best = r;
                    break;
                }
            }
            if (best == null) best = ranked.get(0);
            used.add(best.id);
            if (cur != null && proteinDensity(cur) >= proteinDensity(best) * 0.8) continue;
            dayMeals.set(i, build(best, m.day, m.slot, m.targetKcal, m.targetProtein, m.targetFat));
            changed = true;
        }
        if (changed) balance(dayMeals, t);
    }

    /** Dernier recours si la journée manque encore de protéines : le plat le moins protéiné est remplacé. */
    static void repairMains(List<MealPlan.Meal> dayMeals, Targets t, List<Recipe> mains) {
        if (proteinOf(dayMeals) >= t.protein * 0.92 || mains.size() < 2) return;
        int worst = -1;
        double low = Double.MAX_VALUE;
        for (int i = 0; i < dayMeals.size(); i++) {
            MealPlan.Meal m = dayMeals.get(i);
            if (!Recipe.MAIN.equals(m.kind())) continue;
            Recipe r = Recipes.find(m.recipeId);
            double d = r == null ? 0 : proteinDensity(r);
            if (d < low) {
                low = d;
                worst = i;
            }
        }
        if (worst < 0) return;
        Set<String> taken = new HashSet<>();
        for (MealPlan.Meal m : dayMeals) {
            Recipe r = Recipes.find(m.recipeId);
            if (r != null && Recipe.MAIN.equals(r.kind)) taken.add(mainFood(r));
        }
        Recipe best = null;
        for (Recipe r : mains) {
            if (taken.contains(mainFood(r))) continue;
            if (best == null || proteinDensity(r) > proteinDensity(best)) best = r;
        }
        if (best == null || proteinDensity(best) <= low * 1.1) return;
        MealPlan.Meal m = dayMeals.get(worst);
        dayMeals.set(worst, build(best, m.day, m.slot, m.targetKcal, m.targetProtein, m.targetFat));
        balance(dayMeals, t);
    }

    private static double proteinOf(List<MealPlan.Meal> meals) {
        double p = 0;
        for (MealPlan.Meal m : meals) p += m.totals().protein;
        return p;
    }

    /** Objectif corrigé, sans descendre sous la moitié ni dépasser le double de l'objectif initial. */
    private static double bounded(double target, double delta) {
        return Math.max(target * 0.5, Math.min(target * 2, target + delta));
    }

    /** Plat du déjeuner et du dîner de chaque jour (cuisine en lot : le même plat deux jours de suite). */
    static String[][] assignMains(MealPrefs p, List<Recipe> mains, int days, Random rnd) {
        String[][] out = new String[days][2];
        List<int[]> units = new ArrayList<>();
        int step = p.batch ? 2 : 1;
        for (int d = 0; d < days; d += step) units.add(new int[]{d, Math.min(days - 1, d + step - 1)});
        Map<String, Integer> used = new HashMap<>();
        List<Recipe> order = new ArrayList<>(mains);
        Collections.shuffle(order, rnd);
        String[] previous = new String[2];
        for (int[] u : units) {
            for (int s = 0; s < 2; s++) {
                String lunch = s == 1 ? out[u[0]][0] : null;
                Recipe best = null;
                // 3 : tout respecter ; 2 : accepter le même aliment principal ; 1 : accepter de répéter le plat précédent.
                for (int level = 3; level >= 1 && best == null; level--) {
                    best = choose(order, used, previous[s], lunch, level);
                }
                if (best == null) best = order.get(0);
                int use = used.containsKey(best.id) ? used.get(best.id) : 0;
                for (int d = u[0]; d <= u[1]; d++) out[d][s] = best.id;
                used.put(best.id, use + (u[1] - u[0] + 1));
                previous[s] = best.id;
            }
        }
        if (p.fish) {
            Recipe fish = null;
            for (Recipe r : mains) if (r.has(Recipe.FISH)) fish = r;
            if (fish != null) {
                // Au moins deux repas de poisson dans la semaine, en remplaçant de préférence des dîners.
                for (int s = 1; s >= 0 && countFish(out) < 2; s--) {
                    for (int[] u : units) {
                        if (countFish(out) >= 2) break;
                        if (isFish(out[u[0]][s])) continue;
                        Recipe other = Recipes.find(out[u[0]][1 - s]);
                        if (other != null && mainFood(other).equals(mainFood(fish))) continue;
                        for (int d = u[0]; d <= u[1]; d++) out[d][s] = fish.id;
                    }
                }
            }
        }
        return out;
    }

    private static Recipe choose(List<Recipe> order, Map<String, Integer> used, String previous, String lunchId, int level) {
        Recipe lunch = Recipes.find(lunchId);
        Recipe best = null;
        int bestUse = Integer.MAX_VALUE;
        for (Recipe r : order) {
            if (lunch != null && r.id.equals(lunch.id)) continue;
            if (level >= 3 && lunch != null && mainFood(r).equals(mainFood(lunch))) continue;
            if (level >= 2 && r.id.equals(previous)) continue;
            int use = used.containsKey(r.id) ? used.get(r.id) : 0;
            if (use < bestUse) {
                best = r;
                bestUse = use;
            }
        }
        return best;
    }

    /** Aliment protéiné principal d'un plat (pour ne pas manger deux fois le même dans la journée). */
    static String mainFood(Recipe r) {
        for (Recipe.Ing i : r.ingredients) if (i.role == Recipe.PROT) return i.foodId;
        return r.id;
    }

    private static boolean isFish(String id) {
        Recipe r = Recipes.find(id);
        return r != null && r.has(Recipe.FISH);
    }

    static int countFish(String[][] out) {
        int n = 0;
        for (String[] day : out) for (String id : day) if (isFish(id)) n++;
        return n;
    }

    // ---------------------------------------------------------------- remplacements

    public static class Option {
        public final MealPlan.Meal meal;
        public final Recipe recipe;
        /** Dans la tolérance de ±10 % en calories et en protéines. */
        public final boolean close;

        Option(MealPlan.Meal meal, Recipe recipe, boolean close) {
            this.meal = meal;
            this.recipe = recipe;
            this.close = close;
        }
    }

    /** Plats de remplacement, ajustés au même objectif, les plus proches d'abord. */
    public static List<Option> alternatives(MealPrefs p, MealPlan.Meal current, int max, boolean all) {
        Nutrition.Totals now = current.totals();
        double k0 = current.targetKcal > 0 ? current.targetKcal : now.kcal;
        double p0 = current.targetProtein > 0 ? current.targetProtein : now.protein;
        List<Option> close = new ArrayList<>();
        List<Option> far = new ArrayList<>();
        Random rnd = new Random(current.day * 31L + current.slot.hashCode() + (current.recipeId == null ? 0 : current.recipeId.hashCode()));
        List<Recipe> pool = candidates(p, current.kind());
        Collections.shuffle(pool, rnd);
        for (Recipe r : pool) {
            if (r.id.equals(current.recipeId)) continue;
            MealPlan.Meal m = build(r, current.day, current.slot, k0, p0, current.targetFat > 0 ? current.targetFat : now.fat);
            Nutrition.Totals t = m.totals();
            boolean ok = Math.abs(t.kcal - now.kcal) <= SWAP_TOLERANCE * Math.max(1, now.kcal)
                    && Math.abs(t.protein - now.protein) <= Math.max(3, SWAP_TOLERANCE * now.protein);
            (ok ? close : far).add(new Option(m, r, ok));
        }
        Collections.sort(far, (x, y) -> Double.compare(distance(x.meal, now), distance(y.meal, now)));
        List<Option> out = new ArrayList<>(close);
        out.addAll(far);
        if (all) {
            Collections.sort(out, (x, y) -> x.recipe.name.compareTo(y.recipe.name));
            return out;
        }
        return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
    }

    private static double distance(MealPlan.Meal m, Nutrition.Totals ref) {
        Nutrition.Totals t = m.totals();
        return Math.abs(t.kcal - ref.kcal) / Math.max(1, ref.kcal) + Math.abs(t.protein - ref.protein) / Math.max(1, ref.protein);
    }

    /** Jours d'entraînement habituels d'après l'historique (au moins 2 fois en 4 semaines), sinon lun, mar, jeu, ven. */
    public static int detectTraining(boolean[] trainedLast28, int today) {
        int[] count = new int[7];
        for (int i = 0; i < trainedLast28.length; i++) {
            if (trainedLast28[i]) count[weekday(Days.add(today, -i))]++;
        }
        int mask = 0;
        for (int d = 0; d < 7; d++) if (count[d] >= 2) mask |= 1 << d;
        return mask != 0 ? mask : (1 | 2 | 8 | 16);
    }

    public static String weekdayShort(int day) {
        String[] l = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        return l[weekday(day)];
    }

    public static String weekdayLong(int day) {
        String[] l = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"};
        return l[weekday(day)];
    }

    static String lower(String s) {
        return s.toLowerCase(Locale.FRENCH);
    }
}

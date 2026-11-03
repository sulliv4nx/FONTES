package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * « Composer mes repas » : à partir des ingrédients choisis, propose des recettes qui les utilisent
 * (avec des équivalences : brèdes ou épinards, riz ou riz complet…) et des assiettes composées
 * protéine + féculent + légume, toutes ajustées à l'objectif du repas.
 */
public final class Composer {
    private Composer() {
    }

    public static final String PROTEINS = "Viandes, poissons & œufs";
    public static final String STARCHES = "Féculents";
    public static final String VEGETABLES = "Légumes";

    // ---------------------------------------------------------------- ingrédients proposés

    public static final class Choice {
        public final String foodId;
        public final String label;
        /** Ingrédient typique de la cuisine réunionnaise (badge 974). */
        public final boolean reunion;
        public final String group;

        Choice(String foodId, String label, boolean reunion, String group) {
            this.foodId = foodId;
            this.label = label;
            this.reunion = reunion;
            this.group = group;
        }
    }

    private static List<Choice> catalog;

    private static void add(List<Choice> l, String group, String id, String label, boolean reunion) {
        l.add(new Choice(id, label, reunion, group));
    }

    public static synchronized List<Choice> catalog() {
        if (catalog != null) return catalog;
        List<Choice> l = new ArrayList<>();
        add(l, PROTEINS, "poulet_blanc", "Blanc de poulet", false);
        add(l, PROTEINS, "poulet_cuisse", "Cuisse de poulet", false);
        add(l, PROTEINS, "dinde", "Dinde", false);
        add(l, PROTEINS, "steak_5", "Steak haché", false);
        add(l, PROTEINS, "boeuf", "Bœuf", false);
        add(l, PROTEINS, "oeuf", "Œufs", false);
        add(l, PROTEINS, "thon", "Thon", false);
        add(l, PROTEINS, "morue", "Morue", true);
        add(l, PROTEINS, "camarons", "Camarons", true);
        add(l, PROTEINS, "zourite", "Zourite", true);
        add(l, PROTEINS, "cabri", "Cabri", true);
        add(l, PROTEINS, "saumon", "Saumon", false);
        add(l, PROTEINS, "cabillaud", "Poisson blanc", false);
        add(l, PROTEINS, "crevettes", "Crevettes", false);
        add(l, PROTEINS, "saucisse", "Saucisse fumée", false);
        add(l, PROTEINS, "porc_filet", "Porc", false);
        add(l, PROTEINS, "tofu", "Tofu", false);
        add(l, STARCHES, "riz_blanc", "Riz", false);
        add(l, STARCHES, "riz_complet", "Riz complet", false);
        add(l, STARCHES, "pates", "Pâtes", false);
        add(l, STARCHES, "pates_completes", "Pâtes complètes", false);
        add(l, STARCHES, "grains", "Grains", true);
        add(l, STARCHES, "lentilles", "Lentilles de Cilaos", true);
        add(l, STARCHES, "haricots_rouges", "Haricots rouges", false);
        add(l, STARCHES, "pois_chiches", "Pois chiches", false);
        add(l, STARCHES, "patate_douce", "Patate douce", false);
        add(l, STARCHES, "pdt", "Pomme de terre", false);
        add(l, STARCHES, "quinoa", "Quinoa", false);
        add(l, STARCHES, "semoule", "Semoule", false);
        add(l, STARCHES, "mais", "Maïs", false);
        add(l, STARCHES, "pain_complet", "Pain complet", false);
        add(l, VEGETABLES, "epinards", "Épinards", false);
        add(l, VEGETABLES, "bredes", "Brèdes", true);
        add(l, VEGETABLES, "chouchou", "Chouchou", true);
        add(l, VEGETABLES, "ti_jacques", "Ti jacques", true);
        add(l, VEGETABLES, "citrouille", "Citrouille", true);
        add(l, VEGETABLES, "tomate", "Tomates", false);
        add(l, VEGETABLES, "brocoli", "Brocolis", false);
        add(l, VEGETABLES, "courgette", "Courgettes", false);
        add(l, VEGETABLES, "haricots_verts", "Haricots verts", false);
        add(l, VEGETABLES, "carotte", "Carottes", false);
        add(l, VEGETABLES, "salade", "Salade", false);
        catalog = Collections.unmodifiableList(l);
        return catalog;
    }

    public static Choice choice(String foodId) {
        for (Choice c : catalog()) if (c.foodId.equals(foodId)) return c;
        return null;
    }

    public static String label(String foodId) {
        Choice c = choice(foodId);
        if (c != null) return c.label;
        return Groceries.info(foodId, null).label;
    }

    private static boolean in(String group, String foodId) {
        Choice c = choice(foodId);
        return c != null && c.group.equals(group);
    }

    // ---------------------------------------------------------------- équivalences

    /** Aliments interchangeables dans une recette, avec le mot qui les désigne dans les noms de plats. */
    private static final String[][] FAMILIES = {
            {"epinards", "épinards", "bredes", "brèdes"},
            {"riz_blanc", "riz", "riz_complet", "riz complet"},
            {"pates", "pâtes", "pates_completes", "pâtes complètes"},
            {"poulet_blanc", "poulet", "poulet_cuisse", "poulet", "dinde", "dinde"},
            {"crevettes", "crevettes", "camarons", "camarons"},
            {"grains", "grains", "lentilles", "lentilles", "haricots_rouges", "haricots rouges"},
    };

    private static String[] family(String foodId) {
        for (String[] f : FAMILIES) for (int i = 0; i < f.length; i += 2) if (f[i].equals(foodId)) return f;
        return null;
    }

    private static String word(String[] family, String foodId) {
        for (int i = 0; i < family.length; i += 2) if (family[i].equals(foodId)) return family[i + 1];
        return null;
    }

    /** Remplace un mot entier (insensible à la casse) en gardant la majuscule initiale. */
    static String replaceWord(String text, String from, String to) {
        if (from == null || to == null || from.equals(to)) return text;
        String lower = text.toLowerCase(Locale.FRENCH);
        String f = from.toLowerCase(Locale.FRENCH);
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (true) {
            int j = lower.indexOf(f, i);
            if (j < 0) break;
            boolean startOk = j == 0 || !Character.isLetter(lower.charAt(j - 1));
            int end = j + f.length();
            boolean endOk = end >= lower.length() || !Character.isLetter(lower.charAt(end));
            // « riz » ne doit pas toucher « riz complet » quand on remplace le riz blanc.
            if (startOk && endOk && !(f.equals("riz") && lower.startsWith(" complet", end))
                    && !(f.equals("pâtes") && lower.startsWith(" complètes", end))) {
                out.append(text, i, j);
                String rep = to;
                if (Character.isUpperCase(text.charAt(j))) rep = Fmt.cap(to);
                out.append(rep);
                i = end;
            } else {
                out.append(text, i, end);
                i = end;
            }
        }
        out.append(text.substring(i));
        return out.toString();
    }

    /**
     * Recette avec des ingrédients remplacés, identifiée par « idDeBase~de>vers~de>vers ».
     * Le nom et les étapes suivent (« Rougail z'œufs, riz & brèdes » → « … & épinards »).
     */
    public static Recipe substituted(String id) {
        String[] parts = id.split("~");
        Recipe base = Recipes.find(parts[0]);
        if (base == null) return null;
        Map<String, String> map = new HashMap<>();
        for (int i = 1; i < parts.length; i++) {
            String[] ft = parts[i].split(">");
            if (ft.length == 2) map.put(ft[0], ft[1]);
        }
        String name = base.name;
        String[] steps = base.steps.clone();
        Recipe.Ing[] ings = new Recipe.Ing[base.ingredients.length];
        for (int i = 0; i < ings.length; i++) {
            Recipe.Ing g = base.ingredients[i];
            String to = map.get(g.foodId);
            if (to == null) {
                ings[i] = g;
                continue;
            }
            ings[i] = new Recipe.Ing(to, g.grams, g.role);
            String[] fam = family(g.foodId);
            if (fam != null) {
                String a = word(fam, g.foodId);
                String b = word(fam, to);
                name = replaceWord(name, a, b);
                for (int k = 0; k < steps.length; k++) steps[k] = replaceWord(steps[k], a, b);
            }
        }
        name = name.replace("amarons sautées", "amarons sautés");
        return new Recipe(id, base.kind, name, base.minutes, base.tags, ings, steps);
    }

    // ---------------------------------------------------------------- assiettes composées

    private static final String AUTO = "auto:";

    private static double baseGrams(String foodId) {
        switch (foodId) {
            case "oeuf":
                return 180;
            case "thon":
                return 120;
            case "saucisse":
                return 110;
            case "crevettes":
            case "camarons":
                return 180;
            case "cabillaud":
            case "morue":
            case "zourite":
            case "tofu":
                return 170;
            case "pain_complet":
                return 80;
            case "pdt":
                return 280;
            case "patate_douce":
                return 250;
            case "quinoa":
                return 180;
            case "mais":
                return 150;
            default:
                return in(PROTEINS, foodId) ? 150 : in(STARCHES, foodId) ? 200 : 150;
        }
    }

    /** Mot du plat : « Poulet », « Omelette », « pâtes », « épinards ». */
    private static String dishWord(String foodId) {
        switch (foodId) {
            case "poulet_blanc":
            case "poulet_cuisse":
                return "Poulet";
            case "oeuf":
                return "Omelette";
            case "cabillaud":
                return "Poisson blanc";
            case "steak_5":
                return "Steak haché";
            case "porc_filet":
                return "Porc";
            case "pain_complet":
                return "pain complet";
            case "pdt":
                return "pommes de terre";
            case "lentilles":
                return "lentilles";
            default:
                return label(foodId);
        }
    }

    private static String lowerFirst(String s) {
        if (s.isEmpty()) return s;
        return s.substring(0, 1).toLowerCase(Locale.FRENCH) + s.substring(1);
    }

    private static String cookStarch(String id) {
        switch (id) {
            case "riz_blanc":
            case "riz_complet":
                return "Rincer le riz puis le cuire 12 min (20 min pour le complet) dans 2 fois son volume d'eau salée.";
            case "pates":
            case "pates_completes":
                return "Cuire les pâtes selon le paquet dans une grande casserole d'eau salée.";
            case "quinoa":
                return "Rincer le quinoa et le cuire 12 min dans 2 fois son volume d'eau.";
            case "semoule":
                return "Verser le même volume d'eau bouillante sur la semoule, couvrir 5 min puis égrainer.";
            case "pdt":
                return "Cuire les pommes de terre 20 min à l'eau ou à la vapeur.";
            case "patate_douce":
                return "Couper la patate douce en cubes, l'enrober d'un peu d'huile et la rôtir 25 min à 200 °C.";
            case "grains":
            case "lentilles":
                return "Cuire les grains 25 min avec oignon, ail et thym (ou rincer une conserve et la réchauffer).";
            case "haricots_rouges":
            case "pois_chiches":
                return "Rincer la conserve et la réchauffer 5 min avec un peu d'oignon.";
            case "mais":
                return "Égoutter le maïs et le réchauffer 2 min.";
            case "pain_complet":
                return "Griller le pain.";
            default:
                return "Cuire le féculent selon le paquet.";
        }
    }

    private static String cookProtein(String id) {
        switch (id) {
            case "poulet_blanc":
            case "poulet_cuisse":
                return "Couper le poulet en morceaux et le saisir 8 à 10 min à la poêle avec l'huile, sel, poivre et curcuma.";
            case "dinde":
                return "Saisir la dinde 4 min de chaque côté avec l'huile.";
            case "steak_5":
                return "Cuire le steak haché 2 à 3 min de chaque côté.";
            case "boeuf":
                return "Saisir le bœuf 2 à 4 min de chaque côté selon la cuisson voulue.";
            case "oeuf":
                return "Battre les œufs avec sel et poivre et cuire l'omelette 3 min à feu moyen.";
            case "thon":
                return "Égoutter le thon et le réchauffer 2 min avec un filet de citron.";
            case "saumon":
            case "cabillaud":
                return "Cuire le poisson 10 min au four à 180 °C, ou 4 min de chaque côté à la poêle.";
            case "morue":
                return "Dessaler la morue la veille (24 h au frais, eau changée 3 fois), puis la pocher 10 min.";
            case "crevettes":
            case "camarons":
                return "Les faire sauter 3 à 5 min à feu vif avec l'ail et l'huile.";
            case "zourite":
                return "Cuire le zourite 40 min à la cocotte-minute, puis le faire revenir avec l'ail.";
            case "cabri":
                return "Faire revenir le cabri puis le mijoter 1 h 15 à couvert (35 min à la cocotte-minute).";
            case "saucisse":
                return "Blanchir les saucisses 5 min, puis les dorer à la poêle.";
            case "porc_filet":
                return "Dorer le porc en morceaux 10 min avec l'ail et le thym.";
            case "tofu":
                return "Couper le tofu en cubes et le dorer 8 min dans l'huile.";
            default:
                return "Cuire la viande ou le poisson à la poêle avec l'huile.";
        }
    }

    private static String cookVeg(String id) {
        switch (id) {
            case "epinards":
            case "bredes":
                return "Faire tomber les " + lowerFirst(label(id)) + " 3 à 5 min à la poêle avec un peu d'ail.";
            case "tomate":
                return "Rougail tomate : couper les tomates en petits dés avec oignon et piment.";
            case "chouchou":
                return "Cuire le chouchou en dés 15 min à couvert avec ail et thym.";
            case "citrouille":
                return "Cuire la citrouille en dés 15 min avec ail et thym, jusqu'à ce qu'elle fonde.";
            case "ti_jacques":
                return "Faire revenir le ti jacques 25 min avec oignon, ail, gingembre et curcuma.";
            case "salade":
                return "Laver la salade et l'assaisonner au dernier moment.";
            case "carotte":
                return "Râper les carottes ou les cuire 10 min à la vapeur.";
            default:
                return "Cuire les " + lowerFirst(label(id)) + " 6 à 8 min à la vapeur.";
        }
    }

    /** Assiette protéine + féculent + légume (féculent ou légume facultatif). */
    public static Recipe plate(String protein, String starch, String veg) {
        String id = AUTO + protein + ":" + (starch == null ? "" : starch) + ":" + (veg == null ? "" : veg);
        List<Recipe.Ing> ing = new ArrayList<>();
        ing.add(new Recipe.Ing(protein, baseGrams(protein), Recipe.PROT));
        if (starch != null) ing.add(new Recipe.Ing(starch, baseGrams(starch), Recipe.REST));
        if (veg != null) ing.add(new Recipe.Ing(veg, baseGrams(veg), Recipe.FIXED));
        ing.add(new Recipe.Ing("huile", 10, Recipe.FAT));
        String name = dishWord(protein);
        if (starch != null && veg != null) name += ", " + lowerFirst(dishWord(starch)) + " & " + lowerFirst(dishWord(veg));
        else if (starch != null) name += " & " + lowerFirst(dishWord(starch));
        else if (veg != null) name += " & " + lowerFirst(dishWord(veg));
        List<String> steps = new ArrayList<>();
        if (starch != null) steps.add(cookStarch(starch));
        steps.add(cookProtein(protein));
        if (veg != null) steps.add(cookVeg(veg));
        int minutes = 15;
        if (starch != null && (starch.equals("riz_complet") || starch.equals("patate_douce") || starch.equals("grains")
                || starch.equals("lentilles") || starch.equals("pdt"))) minutes = 30;
        if (protein.equals("cabri") || protein.equals("zourite")) minutes = 60;
        List<String> tags = new ArrayList<>();
        Choice pc = choice(protein);
        if (protein.equals("saucisse") || protein.equals("porc_filet")) tags.add(Recipe.PORK);
        if (protein.equals("thon") || protein.equals("saumon") || protein.equals("cabillaud") || protein.equals("morue")
                || protein.equals("crevettes") || protein.equals("camarons") || protein.equals("zourite")) {
            tags.add(Recipe.FISH);
        } else if (!protein.equals("oeuf") && !protein.equals("tofu")) {
            tags.add(Recipe.MEAT);
        }
        boolean reunion = (pc != null && pc.reunion) || (veg != null && choice(veg) != null && choice(veg).reunion)
                || (starch != null && choice(starch) != null && choice(starch).reunion);
        if (reunion) tags.add(Recipe.CREOLE);
        return new Recipe(id, Recipe.MAIN, name, minutes, tags.toArray(new String[0]),
                ing.toArray(new Recipe.Ing[0]), steps.toArray(new String[0]));
    }

    /** Retrouve une recette composée ou adaptée à partir de son identifiant (null si inconnu). */
    public static Recipe resolve(String id) {
        if (id == null) return null;
        if (id.startsWith(AUTO)) {
            String[] p = id.substring(AUTO.length()).split(":", -1);
            if (p.length != 3 || Foods.find(p[0]) == null) return null;
            String s = p[1].isEmpty() ? null : p[1];
            String v = p[2].isEmpty() ? null : p[2];
            if ((s != null && Foods.find(s) == null) || (v != null && Foods.find(v) == null)) return null;
            return plate(p[0], s, v);
        }
        if (id.contains("~")) return substituted(id);
        return null;
    }

    // ---------------------------------------------------------------- propositions

    public static final class Proposal {
        public final Recipe recipe;
        /** Recette ajustée à l'objectif du repas. */
        public final MealPlan.Meal meal;
        /** Ingrédients choisis utilisés par le plat. */
        public final List<String> have;
        /** Ingrédients à acheter en plus (hors placard). */
        public final List<String> need;
        public final boolean reunion;
        public final boolean lowProtein;
        final int score;

        Proposal(Recipe recipe, MealPlan.Meal meal, List<String> have, List<String> need, boolean reunion, boolean lowProtein, int score) {
            this.recipe = recipe;
            this.meal = meal;
            this.have = have;
            this.need = need;
            this.reunion = reunion;
            this.lowProtein = lowProtein;
            this.score = score;
        }

        public boolean nothingToBuy() {
            return need.isEmpty();
        }
    }

    /** Huile, miel… : supposés au placard, jamais « à acheter » ni bloquants. */
    static boolean pantry(String foodId) {
        return Groceries.info(foodId, null).pantry;
    }

    /**
     * Repas possibles avec ces ingrédients, pour ce créneau et cet objectif.
     *
     * @param complete autoriser les plats qui demandent d'autres ingrédients (ajoutés à la liste de courses)
     */
    public static List<Proposal> propose(Collection<String> selected, boolean complete, int day, String slot,
                                         double kcal, double protein, double fat) {
        Set<String> sel = new LinkedHashSet<>(selected);
        List<Proposal> out = new ArrayList<>();
        if (sel.isEmpty()) return out;
        boolean anyProtein = false;
        for (String s : sel) if (in(PROTEINS, s)) anyProtein = true;
        String kind = MealPlan.kind(slot);
        Set<String> seen = new LinkedHashSet<>();

        // 1. Recettes de la base, avec équivalences.
        for (Recipe base : Recipes.of(kind)) {
            Map<String, String> subs = new HashMap<>();
            Set<String> inRecipe = new LinkedHashSet<>();
            for (Recipe.Ing i : base.ingredients) inRecipe.add(i.foodId);
            List<String> have = new ArrayList<>();
            List<String> need = new ArrayList<>();
            boolean usesProtein = false;
            for (Recipe.Ing i : base.ingredients) {
                String id = i.foodId;
                if (!sel.contains(id)) {
                    String[] fam = family(id);
                    if (fam != null) {
                        for (int k = 0; k < fam.length; k += 2) {
                            String alt = fam[k];
                            if (sel.contains(alt) && !inRecipe.contains(alt) && !subs.containsValue(alt)) {
                                subs.put(id, alt);
                                id = alt;
                                break;
                            }
                        }
                    }
                }
                if (sel.contains(id)) {
                    if (!have.contains(id)) have.add(id);
                    if (i.role == Recipe.PROT) usesProtein = true;
                } else if (!pantry(id)) {
                    if (!need.contains(id)) need.add(id);
                }
            }
            if (have.isEmpty()) continue;
            if (anyProtein && !usesProtein) continue;
            if (!anyProtein && have.size() < Math.min(2, sel.size())) continue;
            if (!complete && !need.isEmpty()) continue;
            Recipe r = base;
            if (!subs.isEmpty()) {
                StringBuilder id = new StringBuilder(base.id);
                for (Recipe.Ing i : base.ingredients) {
                    String to = subs.get(i.foodId);
                    if (to != null) id.append('~').append(i.foodId).append('>').append(to);
                }
                r = substituted(id.toString());
                if (r == null) continue;
            }
            if (!seen.add(r.name.toLowerCase(Locale.FRENCH))) continue;
            out.add(make(r, day, slot, kcal, protein, fat, have, need));
        }

        // 2. Assiettes composées (repas principaux seulement).
        if (Recipe.MAIN.equals(kind)) {
            List<String> prots = new ArrayList<>();
            List<String> starches = new ArrayList<>();
            List<String> vegs = new ArrayList<>();
            for (String s : sel) {
                if (in(PROTEINS, s)) prots.add(s);
                else if (in(STARCHES, s)) starches.add(s);
                else if (in(VEGETABLES, s)) vegs.add(s);
            }
            boolean addedStarch = false;
            boolean addedVeg = false;
            if (starches.isEmpty() && complete) {
                starches.add("riz_blanc");
                addedStarch = true;
            }
            if (vegs.isEmpty() && complete) {
                vegs.add("brocoli");
                addedVeg = true;
            }
            int n = 0;
            for (int pi = 0; pi < prots.size(); pi++) {
                int combos = Math.max(1, Math.min(2, starches.size()));
                for (int si = 0; si < combos; si++) {
                    String s = starches.isEmpty() ? null : starches.get((pi + si) % starches.size());
                    String v = vegs.isEmpty() ? null : vegs.get((pi + si) % vegs.size());
                    Recipe r = plate(prots.get(pi), s, v);
                    if (!seen.add(r.name.toLowerCase(Locale.FRENCH))) continue;
                    List<String> have = new ArrayList<>();
                    List<String> need = new ArrayList<>();
                    have.add(prots.get(pi));
                    if (s != null) (addedStarch ? need : have).add(s);
                    if (v != null) (addedVeg ? need : have).add(v);
                    out.add(make(r, day, slot, kcal, protein, fat, have, need));
                    if (++n >= 12) break;
                }
                if (n >= 12) break;
            }
        }

        Collections.sort(out, (a, b) -> b.score != a.score ? b.score - a.score : a.recipe.name.compareTo(b.recipe.name));
        // Deux plats avec les mêmes aliments (hors matières grasses) : on garde le mieux classé.
        List<Proposal> unique = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        for (Proposal p : out) {
            List<String> ids = new ArrayList<>();
            for (Recipe.Ing i : p.recipe.ingredients) if (i.role != Recipe.FAT) ids.add(i.foodId);
            Collections.sort(ids);
            if (keys.add(ids.toString())) unique.add(p);
        }
        return unique;
    }

    private static Proposal make(Recipe r, int day, String slot, double kcal, double protein, double fat,
                                 List<String> have, List<String> need) {
        MealPlan.Meal m = MealPlanner.build(r, day, slot, kcal, protein, fat);
        boolean low = m.totals().protein < protein * 0.85;
        boolean reunion = r.has(Recipe.CREOLE);
        int score = have.size() * 3 - need.size() + (reunion ? 1 : 0) - (low ? 1 : 0);
        return new Proposal(r, m, have, need, reunion, low, score);
    }

    // ---------------------------------------------------------------- placement dans le plan

    /** Un remplacement à faire dans le plan. */
    public static final class Placement {
        public final MealPlan.Meal old;
        public final MealPlan.Meal fresh;

        Placement(MealPlan.Meal old, MealPlan.Meal fresh) {
            this.old = old;
            this.fresh = fresh;
        }
    }

    private static double or(double v, double fallback) {
        return v > 0 ? v : fallback;
    }

    /** Le plat ajusté à l'objectif d'un repas existant du plan. */
    public static MealPlan.Meal fit(Recipe r, MealPlan.Meal target) {
        Nutrition.Totals t = target.totals();
        return MealPlanner.build(r, target.day, target.slot, or(target.targetKcal, t.kcal),
                or(target.targetProtein, t.protein), or(target.targetFat, t.fat));
    }

    /**
     * Répartit les plats validés sur la semaine, à partir de fromDay : les plats principaux aux déjeuners
     * et dîners, les autres à leur créneau. Avec la cuisine en lot, un plat couvre deux jours de suite.
     * Les repas déjà cochés ne bougent pas.
     */
    public static List<Placement> fillWeek(MealPlan plan, List<Recipe> dishes, int fromDay, boolean batch) {
        List<Placement> out = new ArrayList<>();
        if (plan == null || dishes.isEmpty()) return out;
        int first = Math.max(plan.start, fromDay);
        List<Integer> days = new ArrayList<>();
        for (int d = first; d <= plan.end(); d = Days.add(d, 1)) days.add(d);
        Map<String, List<Recipe>> byKind = new HashMap<>();
        for (Recipe r : dishes) {
            List<Recipe> l = byKind.get(r.kind);
            if (l == null) byKind.put(r.kind, l = new ArrayList<>());
            l.add(r);
        }
        for (Map.Entry<String, List<Recipe>> e : byKind.entrySet()) {
            List<Recipe> list = e.getValue();
            String[] slots = Recipe.MAIN.equals(e.getKey()) ? new String[]{MealPlan.LUNCH, MealPlan.DINNER}
                    : Recipe.BREAKFAST.equals(e.getKey()) ? new String[]{MealPlan.BREAKFAST}
                    : Recipe.SHAKER.equals(e.getKey()) ? new String[]{MealPlan.SHAKER}
                    : new String[]{MealPlan.SNACK_AM, MealPlan.SNACK_PM};
            int step = batch && Recipe.MAIN.equals(e.getKey()) ? 2 : 1;
            int turn = 0;
            for (int u = 0; u < days.size(); u += step) {
                for (String slot : slots) {
                    Recipe r = list.get(turn % list.size());
                    turn++;
                    for (int k = u; k < Math.min(days.size(), u + step); k++) {
                        MealPlan.Meal m = plan.find(days.get(k), slot);
                        if (m == null || m.eaten) continue;
                        out.add(new Placement(m, fit(r, m)));
                    }
                }
            }
        }
        return out;
    }
}

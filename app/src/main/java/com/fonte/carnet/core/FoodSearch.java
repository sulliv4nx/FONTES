package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Recherche d'aliments dans plusieurs sources à la fois : tes aliments et tes récents, la base Fonte, les plats Fonte,
 * la table Ciqual (hors ligne) et les produits de marque d'Open Food Facts (en ligne). Les résultats sont fusionnés,
 * sans doublons, et classés par pertinence.
 */
public final class FoodSearch {
    private FoodSearch() {
    }

    public static final String RECENT = "recent";
    public static final String MINE = "mine";
    public static final String FONTE = "fonte";
    public static final String DISH = "dish";
    public static final String CIQUAL = "ciqual";
    public static final String ONLINE = "online";

    public static final int ALL = 0;
    public static final int FOODS = 1;
    public static final int BRANDS = 2;
    public static final int DISHES = 3;

    /** Un produit trouvé en ligne. */
    public static class Online {
        public Food food;
        public String brand;
        public String quantity;
        /** Calories ou macros manquantes sur la fiche. */
        public boolean incomplete;
        /** 2 = vendu à La Réunion, 1 = en France, 0 = ailleurs. */
        public int country;
    }

    /** Sources à fouiller. */
    public static class Input {
        public List<Food> recent = Collections.emptyList();
        public List<Food> mine = Collections.emptyList();
        public List<Food> fonte = Collections.emptyList();
        public List<Food> ciqual = Collections.emptyList();
        public List<Recipe> dishes = Collections.emptyList();
        public List<Online> online = Collections.emptyList();
    }

    public static class Result {
        public final String source;
        public final Food food;
        public final Recipe dish;
        public final Online online;
        public int score;

        Result(String source, Food food, Recipe dish, Online online) {
            this.source = source;
            this.food = food;
            this.dish = dish;
            this.online = online;
        }

        public String name() {
            return dish != null ? dish.name : food.name;
        }

        /** Aliment générique, produit de marque ou plat. */
        public int kind() {
            if (dish != null) return DISHES;
            if (online != null) return BRANDS;
            if (food.barcode != null && !food.barcode.isEmpty()) return BRANDS;
            if (Ciqual.isCiqual(food) && Ciqual.GROUP_DISHES.equals(Ciqual.groupCode(food))) return DISHES;
            if (Foods.DISHES.equals(food.category)) return DISHES;
            return FOODS;
        }
    }

    // ---------------------------------------------------------------- texte

    private static final Set<String> STOP = new HashSet<>();
    private static final Map<String, String[]> SYNONYMS = new HashMap<>();

    static {
        for (String w : new String[]{"de", "du", "des", "d", "la", "le", "les", "l", "a", "au", "aux", "en", "et", "un", "une", "avec"}) {
            STOP.add(w);
        }
        syn("steak", "boeuf", "bifteck");
        syn("boeuf", "steak");
        syn("yaourt", "yogourt", "yoghourt");
        syn("yogourt", "yaourt");
        syn("patate", "pomme");
        syn("pdt", "pomme");
        syn("frite", "pomme");
        syn("coca", "cola");
        syn("ketchup", "sauce");
        syn("blanc", "filet", "escalope");
        syn("escalope", "filet", "blanc");
        syn("filet", "escalope", "blanc");
        syn("chouchou", "christophine");
        syn("christophine", "chouchou");
        syn("brede", "epinard");
        syn("ti", "petit");
    }

    private static void syn(String w, String... alt) {
        SYNONYMS.put(w, alt);
    }

    /** Minuscules, sans accents ni ponctuation. */
    public static String norm(String s) {
        if (s == null) return "";
        return Foods.norm(s).replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** Pluriel simple : « œufs » → « oeuf », « pâtes » → « pate ». */
    static String stem(String w) {
        if (KEEP.contains(w)) return w;
        if (w.length() > 3 && (w.endsWith("s") || w.endsWith("x"))) return w.substring(0, w.length() - 1);
        return w;
    }

    /** Mots qui finissent par s ou x au singulier. */
    private static final Set<String> KEEP = new HashSet<>(Arrays.asList("mais", "pois", "noix", "jus", "radis", "ananas",
            "cassis", "gras", "frais", "anis", "brebis", "gros", "riz", "prix", "bois", "souris", "tapas", "couscous", "lys",
            "paris", "pays", "chips", "corps", "repas", "dos", "os", "bras", "sans", "plus", "moins", "tres", "apres", "dans"));

    static List<String> words(String s) {
        List<String> out = new ArrayList<>();
        for (String w : norm(s).split(" ")) {
            if (w.isEmpty()) continue;
            out.add(stem(w));
        }
        return out;
    }

    /** Mots de la recherche, sans les petits mots (« de », « la »…). */
    public static List<String> tokens(String query) {
        List<String> out = new ArrayList<>();
        for (String w : words(query)) if (!STOP.contains(w)) out.add(w);
        if (out.isEmpty()) out.addAll(words(query));
        return out;
    }

    private static int quality(String token, List<String> nameWords) {
        int best = 0;
        for (String w : nameWords) {
            int q = 0;
            if (w.equals(token)) q = 3;
            else if (token.length() >= 2 && w.startsWith(token)) q = 2;
            else if (token.length() >= 4 && w.contains(token)) q = 1;
            if (q > best) best = q;
        }
        return best;
    }

    /** Nom découpé une fois pour toutes (la table Ciqual compte plus de 3 000 noms). */
    private static final class Parsed {
        final String norm;
        final List<String> words;
        final List<String> head;

        Parsed(String name) {
            norm = norm(name);
            words = words(name);
            int comma = name.indexOf(',');
            head = comma > 0 ? words(name.substring(0, comma)) : words;
        }
    }

    private static final Map<String, Parsed> PARSED = new java.util.concurrent.ConcurrentHashMap<>();

    private static Parsed parsed(String name) {
        Parsed p = PARSED.get(name);
        if (p == null) {
            p = new Parsed(name);
            if (PARSED.size() < 20000) PARSED.put(name, p);
        }
        return p;
    }

    /** Pertinence d'un nom pour la recherche, ou -1 s'il ne correspond pas. */
    static int score(String name, List<String> tokens, String query) {
        if (tokens.isEmpty() || name == null) return -1;
        Parsed parsed = parsed(name);
        List<String> nameWords = parsed.words;
        if (nameWords.isEmpty()) return -1;
        List<String> head = parsed.head;
        int total = 0;
        boolean inHead = true;
        for (String t : tokens) {
            int q = quality(t, nameWords);
            String[] alt = SYNONYMS.get(t);
            if (alt != null) for (String a : alt) q = Math.max(q, Math.min(2, quality(a, nameWords)));
            if (q == 0) return -1;
            total += q * 10;
            int h = quality(t, head);
            if (alt != null) for (String a : alt) h = Math.max(h, quality(a, head));
            if (h == 0) inHead = false;
        }
        int s = total;
        String first = nameWords.get(0);
        String t0 = tokens.get(0);
        if (first.equals(t0) || (t0.length() >= 3 && first.startsWith(t0))) s += 15;
        if (inHead) s += 10;
        int significant = 0;
        for (String w : nameWords) if (!STOP.contains(w)) significant++;
        s -= Math.min(24, 2 * Math.max(0, significant - tokens.size()));
        if (parsed.norm.equals(parsed(query).norm)) s += 40;
        return s;
    }

    private static int bonus(String source) {
        switch (source) {
            case RECENT:
                return 30;
            case MINE:
                return 25;
            case FONTE:
                return 12;
            case DISH:
                return 10;
            case ONLINE:
                return -4;
            default:
                return 0;
        }
    }

    // ---------------------------------------------------------------- recherche

    /** Résultats classés, toutes sources confondues. */
    public static List<Result> search(String query, Input in) {
        List<String> tokens = tokens(query);
        List<Result> out = new ArrayList<>();
        if (tokens.isEmpty()) return out;
        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        Set<String> barcodes = new HashSet<>();
        addFoods(out, in.recent, RECENT, tokens, query, ids, names, barcodes);
        addFoods(out, in.mine, MINE, tokens, query, ids, names, barcodes);
        addFoods(out, in.fonte, FONTE, tokens, query, ids, names, barcodes);
        for (Recipe r : in.dishes) {
            int s = score(r.name, tokens, query);
            if (s < 0) continue;
            String key = "dish:" + key(r.name);
            if (!names.add(key)) continue;
            Result res = new Result(DISH, null, r, null);
            res.score = s + bonus(DISH);
            out.add(res);
        }
        addFoods(out, in.ciqual, CIQUAL, tokens, query, ids, names, barcodes);
        int order = 0;
        for (Online o : in.online) {
            order++;
            if (o == null || o.food == null) continue;
            String code = o.food.barcode;
            if (code != null && !code.isEmpty() && barcodes.contains(code)) continue;
            String label = o.brand == null || o.brand.isEmpty() ? o.food.name : o.food.name + " " + o.brand;
            int s = Math.max(score(o.food.name, tokens, query), score(label, tokens, query));
            if (s < 0) continue;
            if (code != null && !code.isEmpty() && !barcodes.add(code)) continue;
            Result res = new Result(ONLINE, o.food, null, o);
            res.score = s + bonus(ONLINE) + 4 * o.country - Math.min(6, order / 4) - (o.incomplete ? 6 : 0);
            out.add(res);
        }
        // Un seul mot (« poulet », « riz ») : on cherche plutôt l'aliment que les plats qui en contiennent.
        if (tokens.size() == 1) {
            for (Result r : out) if (r.kind() == DISHES) r.score -= 15;
        }
        // Tri stable : à pertinence égale, l'ordre des sources est gardé.
        List<Result> sorted = new ArrayList<>(out);
        Collections.sort(sorted, (a, b) -> Integer.compare(b.score, a.score));
        return sorted;
    }

    private static String key(String name) {
        StringBuilder b = new StringBuilder();
        for (String w : parsed(name).words) b.append(w).append(' ');
        return b.toString().trim();
    }

    private static void addFoods(List<Result> out, List<Food> foods, String source, List<String> tokens, String query,
                                 Set<String> ids, Set<String> names, Set<String> barcodes) {
        if (foods == null) return;
        for (Food f : foods) {
            if (f == null || f.name == null) continue;
            if (f.id != null && ids.contains(f.id)) continue;
            int s = score(f.name, tokens, query);
            if (s < 0) continue;
            String k = key(f.name);
            if (!names.add(k)) continue;
            if (f.id != null) ids.add(f.id);
            if (f.barcode != null && !f.barcode.isEmpty()) barcodes.add(f.barcode);
            Result r = new Result(source, f, null, null);
            r.score = s + bonus(source);
            out.add(r);
        }
    }

    /** Prépare les noms à l'avance (à lancer hors du fil principal, après le chargement de Ciqual). */
    public static void warm(List<Food> foods) {
        if (foods == null) return;
        for (Food f : foods) if (f != null && f.name != null) parsed(f.name);
    }

    public static List<Result> filter(List<Result> all, int kind) {
        if (kind == ALL) return all;
        List<Result> out = new ArrayList<>();
        for (Result r : all) if (r.kind() == kind) out.add(r);
        return out;
    }

    /** Nombre de résultats {tout, aliments, marques, plats}. */
    public static int[] counts(List<Result> all) {
        int[] c = new int[4];
        c[ALL] = all.size();
        for (Result r : all) c[r.kind()]++;
        return c;
    }

    // ---------------------------------------------------------------- plats

    /** Valeurs d'un plat Fonte pour sa portion type : {kcal, protéines, glucides, lipides, grammes}. */
    public static double[] dishTotals(Recipe r) {
        double[] t = new double[5];
        for (Recipe.Ing i : r.ingredients) {
            Food f = Foods.find(i.foodId);
            if (f == null) continue;
            double k = i.grams / 100.0;
            t[0] += f.kcal * k;
            t[1] += f.protein * k;
            t[2] += f.carbs * k;
            t[3] += f.fat * k;
            t[4] += i.grams;
        }
        return t;
    }

    /** « 1 assiette » pour un plat, « 1 bol » pour un petit-déjeuner, « 1 portion » sinon. */
    public static String dishPortion(Recipe r) {
        if (Recipe.MAIN.equals(r.kind)) return "1 assiette";
        if (Recipe.SHAKER.equals(r.kind)) return "1 shaker";
        return "1 portion";
    }
}

package com.fonte.carnet.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Table Ciqual 2025 de l'ANSES (licence ouverte) : environ 3 400 aliments génériques consommés en France,
 * valeurs pour 100 g. Le fichier ciqual.tsv est produit à la compilation par tools/CiqualImport.java.
 */
public final class Ciqual {
    private Ciqual() {
    }

    public static final String PREFIX = "ciqual_";
    /** Groupe Ciqual des « entrées et plats composés ». */
    public static final String GROUP_DISHES = "01";

    /** Ouvre le fichier de la table (les assets de l'appli). */
    public interface Source {
        InputStream open() throws IOException;
    }

    private static Source source;
    private static List<Food> foods;
    private static Map<String, Food> byId;
    private static final Map<String, String> GROUPS = new HashMap<>();

    public static synchronized void setSource(Source s) {
        source = s;
    }

    /** Chargée ? (sinon, la recherche se fait sans Ciqual). */
    public static synchronized boolean loaded() {
        return foods != null;
    }

    /** Aliments de la table (liste vide si le fichier est absent). Chargée au premier appel. */
    public static synchronized List<Food> all() {
        if (foods == null) {
            List<Food> list = Collections.emptyList();
            if (source != null) {
                try (InputStream in = source.open()) {
                    list = parse(in);
                } catch (IOException e) {
                    list = Collections.emptyList();
                }
            }
            setAll(list);
        }
        return foods;
    }

    /** Pour les tests : remplace la table. */
    public static synchronized void setAll(List<Food> list) {
        foods = Collections.unmodifiableList(new ArrayList<>(list));
        byId = new HashMap<>();
        for (Food f : foods) byId.put(f.id, f);
    }

    public static Food find(String id) {
        if (id == null || !id.startsWith(PREFIX)) return null;
        all();
        synchronized (Ciqual.class) {
            return byId.get(id);
        }
    }

    public static boolean isCiqual(Food f) {
        return f != null && f.id != null && f.id.startsWith(PREFIX);
    }

    /** Code du groupe Ciqual d'un aliment de la table (« 01 » pour les plats composés), ou "". */
    public static synchronized String groupCode(Food f) {
        String g = f == null ? null : GROUPS.get(f.id);
        return g == null ? "" : g;
    }

    public static List<Food> parse(InputStream in) throws IOException {
        List<Food> out = new ArrayList<>();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = r.readLine()) != null) {
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            String[] c = line.split("\t", -1);
            if (c.length < 8) continue;
            Food f = new Food();
            f.id = PREFIX + c[0].trim();
            f.name = c[1].trim();
            if (f.name.isEmpty()) continue;
            f.category = c[3].trim().isEmpty() ? "Ciqual" : capitalize(c[3].trim());
            f.kcal = num(c[4]);
            f.protein = num(c[5]);
            f.carbs = num(c[6]);
            f.fat = num(c[7]);
            synchronized (Ciqual.class) {
                GROUPS.put(f.id, c[2].trim());
            }
            out.add(f);
        }
        return out;
    }

    private static double num(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

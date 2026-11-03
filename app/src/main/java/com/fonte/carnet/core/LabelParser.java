package com.fonte.carnet.core;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lit le tableau nutritionnel d'une étiquette (texte issu de la photo) : énergie, protéines,
 * glucides, lipides pour 100 g, et la portion si elle est indiquée. On prend la première
 * colonne chiffrée, qui est celle « pour 100 g » sur les étiquettes européennes.
 */
public final class LabelParser {
    private LabelParser() {
    }

    private static final Pattern KCAL = Pattern.compile("(\\d{1,4}(?:\\.\\d{1,2})?)\\s*kcal");
    private static final Pattern KJ = Pattern.compile("(\\d{2,4}(?:\\.\\d{1,2})?)\\s*kj");
    private static final Pattern GRAMS = Pattern.compile("(?:^|[^\\d.])(\\d{1,3}(?:\\.\\d{1,2})?)\\s*g(?![a-z])");
    private static final Pattern NUMBER = Pattern.compile("(?:^|[^\\d.])(\\d{1,3}(?:\\.\\d{1,2})?)(?![\\d.])");
    private static final Pattern PORTION = Pattern.compile("(?:portion|serving|part)[^\\d]{0,12}(\\d{1,4})\\s*(?:g|ml)");

    static String normalize(String s) {
        String n = Normalizer.normalize(s.toLowerCase(Locale.FRENCH), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        n = n.replaceAll("(\\d),(\\d)", "$1.$2");
        n = n.replace("<", " ");
        return n;
    }

    /** Valeurs lues (pour 100 g) ou null si rien d'exploitable. Le nom est laissé vide. */
    public static Food parse(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        String[] lines = normalize(text).split("\n");
        Food f = new Food();
        f.kcal = -1;
        f.protein = -1;
        f.carbs = -1;
        f.fat = -1;

        String all = String.join("\n", lines);
        Matcher k = KCAL.matcher(all);
        if (k.find()) {
            f.kcal = Double.parseDouble(k.group(1));
        } else {
            Matcher j = KJ.matcher(all);
            if (j.find()) f.kcal = Math.round(Double.parseDouble(j.group(1)) / 4.184);
        }
        f.protein = nutrient(lines, new String[]{"proteine", "protein"}, null);
        f.carbs = nutrient(lines, new String[]{"glucide", "carbohydrate", "hydrates de carbone"}, "dont");
        f.fat = nutrient(lines, new String[]{"matieres grasses", "matiere grasse", "lipide", "graisses", "fat"}, "dont");
        Matcher p = PORTION.matcher(all);
        if (p.find()) {
            double portion = Double.parseDouble(p.group(1));
            if (portion > 0 && portion <= 1000) f.portion = portion;
        }

        int found = 0;
        if (f.protein >= 0) found++;
        if (f.carbs >= 0) found++;
        if (f.fat >= 0) found++;
        if (f.kcal < 0 && found < 2) return null;
        if (f.protein < 0) f.protein = 0;
        if (f.carbs < 0) f.carbs = 0;
        if (f.fat < 0) f.fat = 0;
        if (f.kcal < 0) f.kcal = Math.round(4 * f.protein + 4 * f.carbs + 9 * f.fat);
        if (f.kcal > 950 || f.protein > 100 || f.carbs > 100 || f.fat > 100) return null;
        f.name = "";
        f.custom = true;
        return f;
    }

    /** Première quantité en g sur la ligne du nutriment (ou la suivante si la valeur est passée à la ligne). */
    private static double nutrient(String[] lines, String[] keys, String excludeWord) {
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i];
            boolean match = false;
            for (String key : keys) if (l.contains(key)) match = true;
            if (!match) continue;
            if (excludeWord != null && l.trim().startsWith(excludeWord)) continue;
            if (l.contains("satur") || l.contains("sucre") && !l.contains("glucide")) continue;
            double v = firstGrams(l);
            if (v < 0 && i + 1 < lines.length) v = firstNumber(lines[i + 1]);
            if (v >= 0) return v;
        }
        return -1;
    }

    private static double firstGrams(String l) {
        Matcher m = GRAMS.matcher(l);
        if (m.find()) return Double.parseDouble(m.group(1));
        Matcher n = NUMBER.matcher(l);
        if (n.find()) return Double.parseDouble(n.group(1));
        return -1;
    }

    private static double firstNumber(String l) {
        Matcher n = NUMBER.matcher(l);
        return n.find() ? Double.parseDouble(n.group(1)) : -1;
    }
}

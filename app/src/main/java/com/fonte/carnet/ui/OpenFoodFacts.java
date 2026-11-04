package com.fonte.carnet.ui;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.fonte.carnet.core.Food;
import com.fonte.carnet.core.FoodSearch;
import com.fonte.carnet.core.Foods;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Open Food Facts (base ouverte et gratuite, nécessite une connexion) : fiche d'un produit par son code-barres,
 * et recherche par nom. La recherche est limitée à 10 requêtes par minute : Fonte en envoie au plus une toutes les
 * 6 secondes, et garde les réponses en mémoire.
 */
final class OpenFoodFacts {
    private OpenFoodFacts() {
    }

    static final String USER_AGENT = "Fonte/4.4 (Android; https://github.com/sulliv4nx/FONTES)";
    private static final long MIN_INTERVAL = 6000;
    private static final String FIELDS = "code,product_name,product_name_fr,brands,quantity,serving_quantity,nutriments,countries_tags";

    /** Réponse d'une recherche. */
    static final class Page {
        final java.util.List<FoodSearch.Online> products = new java.util.ArrayList<>();
        int count;
        /** null si tout va bien ; OFFLINE, BUSY ou ERROR sinon. */
        String error;
    }

    static final String OFFLINE = "offline";
    static final String BUSY = "busy";
    static final String ERROR = "error";

    interface SearchCallback {
        /** Appelé sur le fil principal. */
        void done(String query, int page, Page result);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final java.util.Map<String, Page> CACHE = new java.util.LinkedHashMap<String, Page>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, Page> e) {
            return size() > 40;
        }
    };
    private static long lastRequest;
    private static Runnable pending;

    /** Réponse déjà en mémoire, ou null. */
    static synchronized Page cached(String query, int page) {
        return CACHE.get(cacheKey(query, page));
    }

    private static String cacheKey(String query, int page) {
        return FoodSearch.norm(query) + "|" + page;
    }

    /**
     * Recherche par nom. Une recherche demandée trop tôt après la précédente attend son tour ; seule la dernière
     * demande en attente est gardée.
     */
    static void search(String query, int page, SearchCallback cb) {
        Page hit = cached(query, page);
        if (hit != null) {
            MAIN.post(() -> cb.done(query, page, hit));
            return;
        }
        long wait;
        synchronized (OpenFoodFacts.class) {
            if (pending != null) MAIN.removeCallbacks(pending);
            wait = Math.max(0, lastRequest + MIN_INTERVAL - System.currentTimeMillis());
            pending = () -> {
                synchronized (OpenFoodFacts.class) {
                    pending = null;
                    lastRequest = System.currentTimeMillis();
                }
                new Thread(() -> {
                    Page p = fetchPage(query, page);
                    if (p.error == null) {
                        synchronized (OpenFoodFacts.class) {
                            CACHE.put(cacheKey(query, page), p);
                        }
                    }
                    MAIN.post(() -> cb.done(query, page, p));
                }).start();
            };
            MAIN.postDelayed(pending, wait);
        }
    }

    /** Annule une recherche qui attend encore son tour. */
    static synchronized void cancelPending() {
        if (pending != null) MAIN.removeCallbacks(pending);
        pending = null;
    }

    private static Page fetchPage(String query, int page) {
        String q;
        try {
            q = java.net.URLEncoder.encode(query.trim(), "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            q = query.trim();
        }
        Page p = request("https://world.openfoodfacts.org/cgi/search.pl?search_terms=" + q
                + "&search_simple=1&action=process&json=1&lc=fr&sort_by=unique_scans_n&page_size=20&page=" + page + "&fields=" + FIELDS);
        if (ERROR.equals(p.error) || BUSY.equals(p.error)) {
            Page alt = request("https://search.openfoodfacts.org/search?q=" + q + "&langs=fr&page_size=20&page=" + page
                    + "&fields=" + FIELDS);
            if (alt.error == null) return alt;
        }
        return p;
    }

    private static Page request(String address) {
        Page p = new Page();
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(address).openConnection();
            con.setConnectTimeout(10000);
            con.setReadTimeout(15000);
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Accept", "application/json");
            int status = con.getResponseCode();
            if (status == 429 || status == 503) {
                p.error = BUSY;
                return p;
            }
            if (status >= 400) {
                p.error = ERROR;
                return p;
            }
            JSONObject o = new JSONObject(read(con.getInputStream()));
            JSONArray list = o.optJSONArray("products");
            if (list == null) list = o.optJSONArray("hits");
            if (list == null) {
                p.error = ERROR;
                return p;
            }
            p.count = o.optInt("count", list.length());
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.optJSONObject(i);
                FoodSearch.Online x = item == null ? null : online(item);
                if (x != null) p.products.add(x);
            }
        } catch (java.net.UnknownHostException | java.net.ConnectException | java.net.SocketTimeoutException e) {
            p.error = OFFLINE;
        } catch (Exception e) {
            Log.e("Fonte", "Open Food Facts (recherche)", e);
            p.error = ERROR;
        } finally {
            if (con != null) con.disconnect();
        }
        return p;
    }

    /** Produit d'une liste de résultats (null s'il n'a ni nom ni valeurs). */
    static FoodSearch.Online online(JSONObject p) {
        String code = p.optString("code", "").replaceAll("\\D", "");
        String name = p.optString("product_name_fr", "").trim();
        if (name.isEmpty()) name = p.optString("product_name", "").trim();
        if (name.isEmpty()) return null;
        JSONObject n = p.optJSONObject("nutriments");
        double kcal = Double.NaN, pr = Double.NaN, c = Double.NaN, f = Double.NaN;
        if (n != null) {
            kcal = n.optDouble("energy-kcal_100g", Double.NaN);
            if (Double.isNaN(kcal)) {
                double kj = n.optDouble("energy_100g", Double.NaN);
                if (!Double.isNaN(kj)) kcal = kj / 4.184;
            }
            pr = n.optDouble("proteins_100g", Double.NaN);
            c = n.optDouble("carbohydrates_100g", Double.NaN);
            f = n.optDouble("fat_100g", Double.NaN);
        }
        if (Double.isNaN(kcal) && Double.isNaN(pr) && Double.isNaN(c) && Double.isNaN(f)) return null;
        FoodSearch.Online o = new FoodSearch.Online();
        Food food = new Food();
        food.id = "off_" + (code.isEmpty() ? Integer.toHexString(name.hashCode()) : code);
        food.name = name;
        food.barcode = code.isEmpty() ? null : code;
        food.category = Foods.MINE;
        food.protein = clean(pr);
        food.carbs = clean(c);
        food.fat = clean(f);
        food.kcal = Double.isNaN(kcal) ? Math.round(4 * food.protein + 4 * food.carbs + 9 * food.fat) : Math.round(kcal);
        double serving = p.optDouble("serving_quantity", 0);
        if (!Double.isNaN(serving) && serving > 0 && serving < 2000) {
            food.portion = serving;
            food.portionLabel = "1 portion";
        }
        o.food = food;
        String brand = p.optString("brands", "").trim();
        int comma = brand.indexOf(',');
        if (comma > 0) brand = brand.substring(0, comma).trim();
        o.brand = brand;
        o.quantity = p.optString("quantity", "").trim();
        o.incomplete = Double.isNaN(kcal) || Double.isNaN(pr) || Double.isNaN(c) || Double.isNaN(f);
        JSONArray countries = p.optJSONArray("countries_tags");
        if (countries != null) {
            for (int i = 0; i < countries.length(); i++) {
                String t = countries.optString(i, "");
                if (t.equals("en:reunion")) o.country = 2;
                else if (t.equals("en:france") && o.country < 1) o.country = 1;
            }
        }
        return o;
    }

    /** Fiche à garder dans « Mes aliments » (avec la marque dans le nom). */
    static Food toCustom(FoodSearch.Online o) {
        Food f = new Food();
        f.name = o.brand == null || o.brand.isEmpty() || o.food.name.toLowerCase().contains(o.brand.toLowerCase())
                ? o.food.name : o.food.name + " · " + o.brand;
        f.barcode = o.food.barcode;
        f.kcal = o.food.kcal;
        f.protein = o.food.protein;
        f.carbs = o.food.carbs;
        f.fat = o.food.fat;
        f.portion = o.food.portion;
        f.portionLabel = o.food.portionLabel;
        f.category = Foods.MINE;
        f.custom = true;
        return f;
    }

    interface Callback {
        /** food non null si le produit est trouvé. Appelé sur le fil principal. */
        void done(Food food, String error);
    }

    static void fetch(String barcode, Callback cb) {
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            Food food = null;
            String error = null;
            HttpURLConnection con = null;
            try {
                String digits = barcode.replaceAll("\\D", "");
                URL url = new URL("https://world.openfoodfacts.org/api/v2/product/" + digits
                        + ".json?fields=product_name,product_name_fr,brands,nutriments,serving_quantity");
                con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(10000);
                con.setReadTimeout(10000);
                con.setRequestProperty("User-Agent", USER_AGENT);
                int status = con.getResponseCode();
                InputStream in = status >= 400 ? con.getErrorStream() : con.getInputStream();
                String body = in == null ? "{}" : read(in);
                JSONObject o = new JSONObject(body);
                JSONObject p = o.optJSONObject("product");
                if (o.optInt("status", 0) != 1 || p == null) {
                    error = "Ce produit n'est pas encore dans Open Food Facts.";
                } else {
                    food = parse(p, digits);
                    if (food == null) error = "Le produit est connu, mais sans valeurs nutritionnelles.";
                }
            } catch (Exception e) {
                Log.e("Fonte", "Open Food Facts", e);
                error = "Connexion impossible. Vérifie ta connexion internet.";
            } finally {
                if (con != null) con.disconnect();
            }
            final Food f = food;
            final String err = error;
            main.post(() -> cb.done(f, err));
        }).start();
    }

    private static Food parse(JSONObject p, String barcode) {
        JSONObject n = p.optJSONObject("nutriments");
        if (n == null) return null;
        Food f = new Food();
        String name = p.optString("product_name_fr", "").trim();
        if (name.isEmpty()) name = p.optString("product_name", "").trim();
        String brand = p.optString("brands", "").trim();
        int comma = brand.indexOf(',');
        if (comma > 0) brand = brand.substring(0, comma).trim();
        if (name.isEmpty()) name = brand.isEmpty() ? "Produit " + barcode : brand;
        else if (!brand.isEmpty() && !name.toLowerCase().contains(brand.toLowerCase())) name = name + " · " + brand;
        f.name = name;
        f.barcode = barcode;
        f.category = Foods.MINE;
        f.custom = true;
        double kcal = n.optDouble("energy-kcal_100g", Double.NaN);
        if (Double.isNaN(kcal)) {
            double kj = n.optDouble("energy_100g", Double.NaN);
            if (!Double.isNaN(kj)) kcal = kj / 4.184;
        }
        f.protein = clean(n.optDouble("proteins_100g", 0));
        f.carbs = clean(n.optDouble("carbohydrates_100g", 0));
        f.fat = clean(n.optDouble("fat_100g", 0));
        f.kcal = Double.isNaN(kcal) ? Math.round(4 * f.protein + 4 * f.carbs + 9 * f.fat) : Math.round(kcal);
        if (f.kcal <= 0 && f.protein + f.carbs + f.fat <= 0) return null;
        double serving = p.optDouble("serving_quantity", 0);
        if (!Double.isNaN(serving) && serving > 0 && serving < 2000) {
            f.portion = serving;
            f.portionLabel = "1 portion";
        }
        return f;
    }

    private static double clean(double v) {
        return Double.isNaN(v) || v < 0 ? 0 : Math.round(v * 10) / 10.0;
    }

    private static String read(InputStream in) throws java.io.IOException {
        try (InputStream s = in) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int r;
            while ((r = s.read(chunk)) > 0) buf.write(chunk, 0, r);
            return new String(buf.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}

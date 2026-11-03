package com.fonte.carnet.ui;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.fonte.carnet.core.Food;
import com.fonte.carnet.core.Foods;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Fiche produit depuis Open Food Facts (base ouverte et gratuite, nécessite une connexion). */
final class OpenFoodFacts {
    private OpenFoodFacts() {
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
                con.setRequestProperty("User-Agent", "Fonte/3.0 (Android)");
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

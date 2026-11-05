package com.fonte.carnet.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Ciqual;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Food;
import com.fonte.carnet.core.FoodEntry;
import com.fonte.carnet.core.FoodSearch;
import com.fonte.carnet.core.Foods;
import com.fonte.carnet.core.LabelParser;
import com.fonte.carnet.core.Recipe;
import com.fonte.carnet.core.Recipes;
import com.fonte.carnet.core.Stats;

import java.util.ArrayList;
import java.util.List;

/**
 * Ajout d'aliments à un repas. Une seule recherche fouille tes aliments et tes récents, la base Fonte, les plats Fonte,
 * la table Ciqual (hors ligne) et Open Food Facts (en ligne). Aussi : code-barres, étiquette, photo du repas, saisie.
 */
public class FoodAddScreen extends Screen {
    private static final int REQ_BARCODE_CAM = 41;
    private static final int REQ_BARCODE_PICK = 42;
    private static final int REQ_LABEL_CAM = 43;
    private static final int REQ_LABEL_PICK = 44;
    private static final int REQ_MEAL_CAM = 45;
    private static final int REQ_MEAL_PICK = 46;

    static final int CIQUAL_TEXT = 0xFFA9C1FF;
    static final int CIQUAL_BG = 0x297AA2FF;
    static final int OFF_TEXT = 0xFFFFB38A;
    static final int OFF_BG = 0x29FF8F5A;
    static final int DISH_TEXT = 0xFF85DBA1;
    static final int DISH_BG = 0x2485DBA1;
    static final int CREOLE_TEXT = 0xFFFFC66B;
    static final int CREOLE_BG = 0x24FFC66B;

    /** Recherche en ligne : nombre de lettres minimum et délai après la dernière frappe. */
    private static final int ONLINE_MIN = 3;
    private static final long DEBOUNCE = 700;

    private String meal;
    private final int day;
    private String query = "";
    private String photo;
    private LinearLayout results;
    private int filter = FoodSearch.ALL;
    private int shown = 15;

    // Recherche en ligne (Open Food Facts) pour la requête affichée.
    private String onlineQuery = "";
    private final List<FoodSearch.Online> online = new ArrayList<>();
    private int onlinePages;
    private int onlineCount;
    /** null = rien demandé, "loading", "done", ou une erreur d'OpenFoodFacts (OFFLINE, BUSY, ERROR). */
    private String onlineState;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable launch = this::searchOnline;

    public FoodAddScreen(String meal, int day) {
        this.meal = meal;
        this.day = day;
    }

    @Override
    public String title() {
        return "Ajouter · " + meal;
    }

    @Override
    public void onHide() {
        handler.removeCallbacks(launch);
        OpenFoodFacts.cancelPending();
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        View[] meals = new View[FoodEntry.MEALS.length];
        for (int i = 0; i < meals.length; i++) {
            String m = FoodEntry.MEALS[i];
            meals[i] = Ui.chip(a, m, m.equals(meal), v -> {
                meal = m;
                a.refresh();
                a.updateHeader();
            });
        }
        c.addView(Ui.chipRow(a, meals));
        c.addView(tools());
        if (photo != null) c.addView(photoCard());

        EditText search = Ui.field(a, "Aliment, produit ou plat…", Ui.TEXT_FIELD);
        search.setText(query);
        search.setSelectAllOnFocus(false);
        Icon lens = new Icon(Icon.SEARCH, Ui.MUTED);
        lens.setBounds(0, 0, Ui.dp(20), Ui.dp(20));
        search.setCompoundDrawablesRelative(lens, null, null, null);
        search.setCompoundDrawablePadding(Ui.dp(10));
        search.setContentDescription("Rechercher un aliment, un produit ou un plat");
        Ui.onText(search, t -> {
            if (t.equals(query)) return;
            query = t;
            shown = 15;
            if (!FoodSearch.norm(t).equals(FoodSearch.norm(onlineQuery))) onlineState = null;
            fillResults();
            scheduleOnline();
        });
        c.addView(search, Ui.mlp(-1, -2, 0, 6, 0, 4));
        results = Ui.vbox(a);
        c.addView(results);
        fillResults();
        if (!Ciqual.loaded()) {
            new Thread(() -> {
                FoodSearch.warm(Ciqual.all());
                handler.post(() -> {
                    if (results != null && results.isAttachedToWindow()) fillResults();
                });
            }).start();
        }
        return Ui.page(a, c);
    }

    /** Code-barres, étiquette, photo du repas, saisie : quatre boutons compacts. */
    private View tools() {
        LinearLayout row = Ui.hbox(a);
        row.setPadding(0, Ui.dp(8), 0, Ui.dp(2));
        Object[][] t = {
                {Icon.BARCODE, "Code-barres", (View.OnClickListener) v -> Camera.choose(a, "Code-barres du produit", REQ_BARCODE_CAM, REQ_BARCODE_PICK)},
                {Icon.CAMERA, "Étiquette", (View.OnClickListener) v -> Camera.choose(a, "Tableau nutritionnel", REQ_LABEL_CAM, REQ_LABEL_PICK)},
                {Icon.CAMERA, photo == null ? "Photo repas" : "Photo ✓", (View.OnClickListener) v -> Camera.choose(a, "Photo du repas", REQ_MEAL_CAM, REQ_MEAL_PICK)},
                {Icon.EDIT, "Saisie", (View.OnClickListener) v -> a.show(new FoodFormScreen(null, meal, day, photo))}};
        for (int i = 0; i < t.length; i++) {
            LinearLayout b = Ui.vbox(a);
            b.setGravity(Gravity.CENTER);
            b.setMinimumHeight(Ui.dp(56));
            b.setPadding(Ui.dp(4), Ui.dp(8), Ui.dp(4), Ui.dp(8));
            b.setBackground(Ui.pressable(Ui.rect(Ui.SURFACE, 12), 12));
            b.setOnClickListener((View.OnClickListener) t[i][2]);
            ImageView ic = new ImageView(a);
            ic.setImageDrawable(new Icon((Integer) t[i][0], Ui.ACCENT));
            b.addView(ic, Ui.lp(Ui.dp(20), Ui.dp(20)));
            TextView l = Ui.medium(a, (String) t[i][1], 12, Ui.TEXT);
            l.setGravity(Gravity.CENTER);
            l.setSingleLine(true);
            l.setPadding(0, Ui.dp(4), 0, 0);
            b.addView(l, Ui.lp(-1, -2));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            if (i < t.length - 1) lp.rightMargin = Ui.dp(8);
            row.addView(b, lp);
        }
        return row;
    }

    private View photoCard() {
        LinearLayout card = Ui.card(a);
        LinearLayout row = Ui.hbox(a);
        Bitmap bmp = Photos.thumb(photo, Ui.dp(64));
        if (bmp != null) {
            ImageView iv = new ImageView(a);
            iv.setImageBitmap(bmp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setClipToOutline(true);
            iv.setBackground(Ui.rect(Ui.SURFACE2, 12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(64), Ui.dp(64));
            lp.rightMargin = Ui.dp(12);
            row.addView(iv, lp);
        }
        LinearLayout txt = Ui.vbox(a);
        txt.addView(Ui.medium(a, "Photo jointe au repas", 15, Ui.TEXT));
        txt.addView(Ui.text(a, "Elle accompagnera les aliments que tu ajoutes maintenant. Choisis-les ci-dessous avec leur quantité.", 12, Ui.MUTED));
        row.addView(txt, Ui.weight(1));
        card.addView(row);
        TextView remove = Ui.button(a, "Retirer la photo", Ui.DANGER_GHOST, v -> {
            photo = null;
            a.refresh();
        });
        remove.setMinHeight(Ui.dp(40));
        card.addView(remove);
        return card;
    }

    // ---------------------------------------------------------------- recherche en ligne

    private void scheduleOnline() {
        handler.removeCallbacks(launch);
        String q = query.trim();
        if (q.length() < ONLINE_MIN) {
            OpenFoodFacts.cancelPending();
            return;
        }
        if (FoodSearch.norm(q).equals(FoodSearch.norm(onlineQuery)) && onlineState != null) return;
        OpenFoodFacts.Page hit = OpenFoodFacts.cached(q, 1);
        if (hit != null) {
            receive(q, 1, hit);
            return;
        }
        handler.postDelayed(launch, DEBOUNCE);
    }

    private void searchOnline() {
        String q = query.trim();
        if (q.length() < ONLINE_MIN) return;
        onlineQuery = q;
        online.clear();
        onlinePages = 0;
        onlineCount = 0;
        onlineState = "loading";
        fillResults();
        OpenFoodFacts.search(q, 1, this::receive);
    }

    private void loadMoreOnline() {
        if (!"done".equals(onlineState)) return;
        String q = onlineQuery;
        int page = onlinePages + 1;
        onlineState = "loading";
        fillResults();
        OpenFoodFacts.search(q, page, this::receive);
    }

    private void receive(String q, int page, OpenFoodFacts.Page p) {
        if (!FoodSearch.norm(q).equals(FoodSearch.norm(query))) return;
        onlineQuery = q;
        if (page == 1) online.clear();
        if (p.error != null) {
            onlineState = p.error;
        } else {
            online.addAll(p.products);
            onlinePages = page;
            onlineCount = p.count;
            onlineState = "done";
        }
        if (results != null && results.isAttachedToWindow()) fillResults();
    }

    private boolean offline() {
        return OpenFoodFacts.OFFLINE.equals(onlineState);
    }

    // ---------------------------------------------------------------- résultats

    private FoodSearch.Input input() {
        FoodSearch.Input in = new FoodSearch.Input();
        in.recent = st.recentFoods(12);
        in.mine = st.customFoods;
        in.fonte = Foods.all();
        in.ciqual = Ciqual.loaded() ? Ciqual.all() : new ArrayList<>();
        in.dishes = Recipes.all();
        in.online = FoodSearch.norm(onlineQuery).equals(FoodSearch.norm(query)) ? online : new ArrayList<>();
        return in;
    }

    private void fillResults() {
        if (results == null) return;
        results.removeAllViews();
        String q = query.trim();
        if (q.isEmpty()) {
            fillBrowse();
            return;
        }
        List<FoodSearch.Result> all = FoodSearch.search(q, input());
        int[] counts = FoodSearch.counts(all);
        results.addView(filterBar(counts));
        results.addView(status(counts[FoodSearch.ALL]));
        if (filter == FoodSearch.BRANDS && !offline() && q.length() >= ONLINE_MIN) results.addView(brandsBanner());
        if (offline()) results.addView(offlineBanner());

        List<FoodSearch.Result> list = FoodSearch.filter(all, filter);
        if (list.isEmpty()) {
            results.addView(Ui.hint(a, filter == FoodSearch.BRANDS
                    ? (q.length() < ONLINE_MIN ? "Tape au moins 3 lettres pour chercher les produits de marque."
                    : !"done".equals(onlineState) && !offline() ? "Recherche des produits en cours…" : "Aucun produit de marque trouvé.")
                    : "Aucun résultat ici. Essaie un autre mot, ou regarde dans « Tout »."));
        } else {
            if (filter == FoodSearch.ALL) results.addView(Ui.section(a, "Les plus proches"));
            int n = 0;
            for (FoodSearch.Result r : list) {
                if (n++ >= shown) break;
                results.addView(row(r));
            }
            if (list.size() > shown) {
                int more = list.size() - shown;
                TextView b = Ui.button(a, "Voir les " + more + " autres résultats", Ui.GHOST, v -> {
                    shown += 25;
                    fillResults();
                });
                results.addView(b);
            } else if (filter == FoodSearch.BRANDS && "done".equals(onlineState) && online.size() < onlineCount) {
                results.addView(Ui.button(a, "Charger 20 produits de plus", Ui.GHOST, v -> loadMoreOnline()));
            }
        }
        if (filter == FoodSearch.BRANDS) {
            TextView n = Ui.text(a, "Une fiche incomplète s'ouvre pour que tu complètes les valeurs manquantes, en les lisant sur "
                    + "l'emballage ou en photographiant l'étiquette.", 12, Ui.FAINT);
            n.setPadding(0, Ui.dp(8), 0, 0);
            results.addView(n);
        }
        results.addView(createCard(q));
        TextView credits = Ui.text(a, filter == FoodSearch.BRANDS
                ? "Données Open Food Facts (ODbL), saisies par la communauté : vérifie l'étiquette en cas de doute."
                : "Sources : Ciqual 2025 (ANSES, licence ouverte) · Open Food Facts (ODbL) · recettes Paradoxe", 11, Ui.FAINT);
        credits.setGravity(Gravity.CENTER);
        credits.setPadding(0, Ui.dp(14), 0, Ui.dp(4));
        results.addView(credits);
    }

    /** Sans recherche : récents, tes aliments, puis la base Fonte par rayon. */
    private void fillBrowse() {
        results.addView(Ui.hint(a, "Cherche parmi tes aliments, la base de l'appli, ses plats (créoles compris), les "
                + "3 400 aliments de la table Ciqual et les produits de marque d'Open Food Facts."));
        List<Food> recent = st.recentFoods(8);
        if (!recent.isEmpty()) {
            results.addView(Ui.section(a, "Récents"));
            for (Food f : recent) results.addView(row(new ResultView(FoodSearch.RECENT, f)));
        }
        if (!st.customFoods.isEmpty()) {
            results.addView(Ui.section(a, Foods.MINE));
            for (Food f : st.customFoods) results.addView(row(new ResultView(FoodSearch.MINE, f)));
        }
        for (String cat : Foods.CATEGORIES) {
            if (Foods.MINE.equals(cat)) continue;
            results.addView(Ui.section(a, cat));
            for (Food f : Foods.all()) if (cat.equals(f.category)) results.addView(row(new ResultView(FoodSearch.FONTE, f)));
        }
    }

    private View filterBar(int[] counts) {
        String[] names = {"Tout", "Aliments", "Marques", "Plats"};
        LinearLayout s = Ui.hbox(a);
        s.setBackground(Ui.rect(Ui.SURFACE, 14));
        s.setPadding(Ui.dp(4), Ui.dp(4), Ui.dp(4), Ui.dp(4));
        boolean waiting = query.trim().length() >= ONLINE_MIN && (onlineState == null || "loading".equals(onlineState));
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            String n;
            if (i == FoodSearch.BRANDS && offline()) n = names[i] + " · –";
            else if (i == FoodSearch.BRANDS && waiting && counts[i] == 0) n = names[i] + " · …";
            else n = names[i] + " · " + counts[i];
            TextView t = Ui.medium(a, n, 13, i == filter ? Ui.TEXT : Ui.MUTED);
            t.setGravity(Gravity.CENTER);
            t.setMinHeight(Ui.dp(40));
            t.setSingleLine(true);
            t.setAutoSizeTextTypeUniformWithConfiguration(10, 13, 1, TypedValue.COMPLEX_UNIT_SP);
            t.setBackground(Ui.pressable(Ui.rect(i == filter ? Ui.SURFACE2 : 0x00000000, 11), 11));
            t.setContentDescription(names[i] + ", " + counts[i] + " résultats" + (i == filter ? ", sélectionné" : ""));
            t.setOnClickListener(v -> {
                filter = idx;
                shown = 15;
                fillResults();
            });
            s.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        }
        s.setLayoutParams(Ui.mlp(-1, -2, 0, 4, 0, 6));
        return s;
    }

    private View status(int total) {
        LinearLayout row = Ui.hbox(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(2), 0, 0, Ui.dp(4));
        String text;
        int icon = Icon.CHECK;
        int color = Ui.OK;
        if (query.trim().length() < ONLINE_MIN) {
            text = total + " résultats hors ligne · 3 lettres pour chercher aussi les marques";
            icon = Icon.SEARCH;
            color = Ui.MUTED;
        } else if (onlineState == null || "loading".equals(onlineState)) {
            text = "Recherche en ligne… · " + total + " résultats déjà trouvés";
            icon = Icon.GLOBE;
            color = OFF_TEXT;
        } else if (offline()) {
            text = total + " résultats · sans réseau";
            icon = Icon.OFFLINE;
            color = CREOLE_TEXT;
        } else if (OpenFoodFacts.BUSY.equals(onlineState) || OpenFoodFacts.ERROR.equals(onlineState)) {
            text = "Open Food Facts ne répond pas pour l'instant · " + total + " résultats hors ligne";
            icon = Icon.GLOBE;
            color = CREOLE_TEXT;
        } else {
            text = (Ciqual.loaded() && !Ciqual.all().isEmpty() ? "4 sources fouillées" : "3 sources fouillées") + " · " + total + " résultats";
        }
        ImageView ic = new ImageView(a);
        ic.setImageDrawable(new Icon(icon, color));
        row.addView(ic, Ui.lp(Ui.dp(16), Ui.dp(16)));
        TextView t = Ui.text(a, text, 12, Ui.MUTED);
        t.setPadding(Ui.dp(8), 0, 0, 0);
        row.addView(t, Ui.weight(1));
        if (OpenFoodFacts.BUSY.equals(onlineState) || OpenFoodFacts.ERROR.equals(onlineState) || offline()) {
            TextView retry = Ui.medium(a, "Réessayer", 13, Ui.ACCENT);
            retry.setPadding(Ui.dp(10), Ui.dp(10), 0, Ui.dp(10));
            retry.setOnClickListener(v -> {
                onlineState = null;
                searchOnline();
            });
            row.addView(retry);
        }
        return row;
    }

    private View brandsBanner() {
        LinearLayout card = banner(Icon.GLOBE, OFF_TEXT);
        String head = !"done".equals(onlineState) && online.isEmpty() ? "Recherche sur Open Food Facts…"
                : Fmt.plural(onlineCount, "produit") + " sur Open Food Facts.";
        ((TextView) card.getChildAt(1)).setText(boldStart(head, " Ceux vendus à La Réunion et en France passent en premier. "
                + "Un produit choisi est gardé dans tes aliments, pour le retrouver même sans réseau."));
        return card;
    }

    private View offlineBanner() {
        LinearLayout card = banner(Icon.OFFLINE, CREOLE_TEXT);
        ((TextView) card.getChildAt(1)).setText(boldStart("Pas de réseau.", " L'appli cherche dans tes aliments, ses plats et Ciqual, "
                + "qui sont sur le téléphone. Les produits de marque reviendront avec la connexion."));
        return card;
    }

    private LinearLayout banner(int icon, int color) {
        LinearLayout card = Ui.hbox(a);
        card.setGravity(Gravity.TOP);
        card.setBackground(Ui.rect(Ui.SURFACE, 12));
        card.setPadding(Ui.dp(12), Ui.dp(12), Ui.dp(12), Ui.dp(12));
        ImageView ic = new ImageView(a);
        ic.setImageDrawable(new Icon(icon, color));
        card.addView(ic, Ui.lp(Ui.dp(18), Ui.dp(18)));
        TextView t = Ui.text(a, "", 13, Ui.MUTED);
        t.setPadding(Ui.dp(10), 0, 0, 0);
        card.addView(t, Ui.weight(1));
        card.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 8));
        return card;
    }

    private CharSequence boldStart(String head, String rest) {
        android.text.SpannableStringBuilder b = new android.text.SpannableStringBuilder(head + rest);
        b.setSpan(new android.text.style.ForegroundColorSpan(Ui.TEXT), 0, head.length(), 0);
        b.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, head.length(), 0);
        return b;
    }

    private View createCard(String q) {
        LinearLayout card = Ui.vbox(a);
        card.setBackground(Ui.rect(Ui.SURFACE, 12));
        card.setPadding(Ui.dp(12), Ui.dp(12), Ui.dp(12), Ui.dp(12));
        card.setLayoutParams(Ui.mlp(-1, -2, 0, 8, 0, 0));
        card.addView(Ui.text(a, offline() ? "Ton paquet n'est pas dans la liste ? Photographie son tableau nutritionnel : il est lu sur "
                + "le téléphone, sans réseau." : "Pas le bon ? Crée-le en quelques secondes, ou scanne son code-barres.", 13, Ui.MUTED));
        String label = q.length() > 22 ? q.substring(0, 21) + "…" : q;
        TextView create = Ui.smallButton(a, "+ Créer « " + label + " »", Ui.GHOST, v -> {
            Food f = new Food();
            f.name = Character.toUpperCase(q.charAt(0)) + q.substring(1);
            a.show(new FoodFormScreen(f, meal, day, photo));
        });
        TextView scan = Ui.smallButton(a, offline() ? "Photo de l'étiquette" : "Code-barres", Ui.GHOST, v -> {
            if (offline()) Camera.choose(a, "Tableau nutritionnel", REQ_LABEL_CAM, REQ_LABEL_PICK);
            else Camera.choose(a, "Code-barres du produit", REQ_BARCODE_CAM, REQ_BARCODE_PICK);
        });
        LinearLayout buttons = Ui.buttonRow(a, scan, create);
        buttons.setPadding(0, Ui.dp(10), 0, 0);
        card.addView(buttons);
        return card;
    }

    // ---------------------------------------------------------------- lignes

    /** Ligne d'un aliment parcouru sans recherche. */
    private static final class ResultView {
        final String source;
        final Food food;

        ResultView(String source, Food food) {
            this.source = source;
            this.food = food;
        }
    }

    private View row(ResultView v) {
        return line(v.source, v.food, null, null);
    }

    private View row(FoodSearch.Result r) {
        return line(r.source, r.food, r.dish, r.online);
    }

    private View line(String source, Food f, Recipe dish, FoodSearch.Online off) {
        LinearLayout row = Ui.hbox(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(12), Ui.dp(10), Ui.dp(12), Ui.dp(10));
        row.setBackground(Ui.pressable(Ui.rect(Ui.SURFACE, 12), 12));
        row.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 6));
        row.setMinimumHeight(Ui.dp(52));
        LinearLayout txt = Ui.vbox(a);
        txt.addView(Ui.text(a, dish != null ? dish.name : f.name, 15, Ui.TEXT));
        LinearLayout meta = Ui.hbox(a);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        meta.setPadding(0, Ui.dp(4), 0, 0);
        String detail;
        if (dish != null) {
            meta.addView(tag("Plat Paradoxe", DISH_TEXT, DISH_BG));
            if (dish.has(Recipe.CREOLE)) meta.addView(tag("Réunionnais", CREOLE_TEXT, CREOLE_BG));
            double[] t = FoodSearch.dishTotals(dish);
            detail = FoodSearch.dishPortion(dish) + " ≈ " + Fmt.kg(Math.round(t[0])) + " kcal · P " + Math.round(t[1]);
            row.setOnClickListener(v -> a.show(new DishScreen(dish, meal, day, photo)));
        } else if (off != null) {
            meta.addView(tag("Marque", OFF_TEXT, OFF_BG));
            if (off.incomplete) meta.addView(tag("Fiche incomplète", CREOLE_TEXT, CREOLE_BG));
            String brand = off.brand == null || off.brand.isEmpty() ? "" : off.brand;
            String qty = off.quantity == null || off.quantity.isEmpty() ? "" : off.quantity;
            String head = brand + (brand.isEmpty() || qty.isEmpty() ? "" : " · ") + qty;
            detail = (head.isEmpty() ? "" : head + " · ") + macros(f);
            row.setOnClickListener(v -> openOnline(off));
        } else {
            switch (source) {
                case FoodSearch.RECENT:
                    meta.addView(tag("Récent", Ui.ACCENT, Ui.ACCENT_SOFT));
                    break;
                case FoodSearch.MINE:
                    meta.addView(tag(f.barcode != null && !f.barcode.isEmpty() ? "Dans tes aliments" : "Mes aliments", Ui.ACCENT, Ui.ACCENT_SOFT));
                    break;
                case FoodSearch.CIQUAL:
                    meta.addView(tag("Ciqual", CIQUAL_TEXT, CIQUAL_BG));
                    break;
                default:
                    break;
            }
            detail = macros(f);
            if (FoodSearch.RECENT.equals(source)) {
                double last = st.lastGrams(f.id);
                if (last > 0) detail += " · la dernière fois " + Fmt.kg(last) + (com.fonte.carnet.core.Portions.liquid(f) ? " ml" : " g");
            }
            row.setOnClickListener(v -> QuantityDialog.add(a, f, sourceLabel(f), meal, day, photo, null, null));
            if (f.custom) {
                row.setOnLongClickListener(v -> {
                    Ui.confirm(a, "Supprimer « " + f.name + " » de tes aliments ?", "Ton journal n'est pas modifié.", "Supprimer", () -> {
                        st.deleteCustomFood(f);
                        fillResults();
                    });
                    return true;
                });
            }
        }
        TextView d = Ui.text(a, detail, 12, Ui.MUTED);
        d.setPadding(meta.getChildCount() > 0 ? Ui.dp(6) : 0, 0, 0, 0);
        meta.addView(d, Ui.weight(1));
        txt.addView(meta);
        row.addView(txt, Ui.weight(1));
        ImageView go = new ImageView(a);
        go.setImageDrawable(new Icon(Icon.PLUS, Ui.ACCENT));
        row.addView(go, Ui.lp(Ui.dp(20), Ui.dp(20)));
        return row;
    }

    private TextView tag(String s, int color, int bg) {
        TextView t = Ui.medium(a, s, 11, color);
        t.setBackground(Ui.rect(bg, 7));
        t.setPadding(Ui.dp(7), Ui.dp(2), Ui.dp(7), Ui.dp(2));
        t.setSingleLine(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = Ui.dp(4);
        t.setLayoutParams(lp);
        return t;
    }

    /** « 150 kcal · P 30 · G 0 · L 3 /100 g » */
    static String macros(Food f) {
        return Fmt.kg(Math.round(f.kcal)) + " kcal · P " + Fmt.kg(Stats.round1(f.protein)) + " · G " + Fmt.kg(Stats.round1(f.carbs))
                + " · L " + Fmt.kg(Stats.round1(f.fat)) + " /100" + (com.fonte.carnet.core.Portions.liquid(f) ? " ml" : " g");
    }

    private static String sourceLabel(Food f) {
        if (Ciqual.isCiqual(f)) return "Ciqual 2025 · valeurs moyennes";
        if (f.custom) return f.barcode != null && !f.barcode.isEmpty() ? "Gardé d'Open Food Facts" : "Mes aliments";
        return "Base Paradoxe";
    }

    /** Produit en ligne : la fiche complète s'ouvre sur la quantité, l'incomplète sur le formulaire. */
    private void openOnline(FoodSearch.Online o) {
        Food known = o.food.barcode == null ? null : st.foodByBarcode(o.food.barcode);
        if (known != null) {
            QuantityDialog.add(a, known, sourceLabel(known), meal, day, photo, null, null);
            return;
        }
        Food custom = OpenFoodFacts.toCustom(o);
        if (o.incomplete) {
            a.show(new FoodFormScreen(custom, meal, day, photo).withNote("Fiche Open Food Facts incomplète : complète les valeurs "
                    + "manquantes avec l'emballage. Le produit sera gardé dans tes aliments."));
            return;
        }
        QuantityDialog.add(a, custom, "Open Food Facts" + (o.brand == null || o.brand.isEmpty() ? "" : " · " + o.brand), meal, day, photo,
                f -> {
                    Food again = f.barcode == null ? null : st.foodByBarcode(f.barcode);
                    return again != null ? again : st.saveCustomFood(f);
                }, null);
    }

    // ---------------------------------------------------------------- photos

    @Override
    public boolean onActivityResult(int request, int res, Intent data) {
        if (request < REQ_BARCODE_CAM || request > REQ_MEAL_PICK) return false;
        if (res != Activity.RESULT_OK) return true;
        boolean camera = request == REQ_BARCODE_CAM || request == REQ_LABEL_CAM || request == REQ_MEAL_CAM;
        Uri uri = Camera.result(a, camera, data);
        if (uri == null) {
            Ui.toast(a, "Aucune image reçue.");
            return true;
        }
        if (request == REQ_MEAL_CAM || request == REQ_MEAL_PICK) {
            String path = Photos.saveMeal(a, uri);
            if (path == null) {
                Ui.toast(a, "Impossible d'enregistrer la photo.");
            } else {
                photo = path;
                a.refresh();
                Ui.toast(a, "Photo jointe : ajoute maintenant les aliments du repas.");
            }
            return true;
        }
        @SuppressWarnings("deprecation")
        ProgressDialog wait = new ProgressDialog(a);
        wait.setCancelable(false);
        if (request == REQ_BARCODE_CAM || request == REQ_BARCODE_PICK) {
            wait.setMessage("Lecture du code-barres…");
            wait.show();
            Barcodes.scan(a, uri, (code, error) -> {
                if (code == null) {
                    wait.dismiss();
                    Ui.info(a, "Code-barres illisible", "Cadre bien le code-barres, sans reflet, puis réessaie. "
                            + "Tu peux aussi photographier l'étiquette nutritionnelle.");
                    return;
                }
                Food known = st.foodByBarcode(code);
                if (known != null) {
                    wait.dismiss();
                    QuantityDialog.add(a, known, meal, day, photo, null);
                    return;
                }
                wait.setMessage("Recherche du produit " + code + "…");
                OpenFoodFacts.fetch(code, (food, err) -> {
                    wait.dismiss();
                    if (food != null) {
                        a.show(new FoodFormScreen(food, meal, day, photo));
                        Ui.toast(a, "Produit trouvé : vérifie les valeurs puis ajoute-le.");
                    } else {
                        new AlertDialog.Builder(a)
                                .setTitle("Produit introuvable")
                                .setMessage(err + "\n\nCode lu : " + code + ". Tu peux photographier l'étiquette ou saisir les valeurs.")
                                .setPositiveButton("Photo de l'étiquette", (d, w) -> Camera.choose(a, "Tableau nutritionnel", REQ_LABEL_CAM, REQ_LABEL_PICK))
                                .setNeutralButton("Saisir", (d, w) -> {
                                    Food f = new Food();
                                    f.barcode = code;
                                    a.show(new FoodFormScreen(f, meal, day, photo));
                                })
                                .setNegativeButton("Annuler", null)
                                .show();
                    }
                });
            });
        } else {
            wait.setMessage("Lecture de l'étiquette…");
            wait.show();
            Ocr.recognize(a, uri, (text, error) -> {
                wait.dismiss();
                Food f = text == null ? null : LabelParser.parse(text);
                if (f == null) {
                    Ui.info(a, "Tableau illisible", "Je n'ai pas trouvé de valeurs nutritionnelles lisibles. Photographie le tableau "
                            + "bien à plat et éclairé, ou saisis les valeurs.");
                    return;
                }
                a.show(new FoodFormScreen(f, meal, day, photo));
                Ui.toast(a, "Valeurs lues : vérifie-les et donne un nom au produit.");
            });
        }
        return true;
    }
}

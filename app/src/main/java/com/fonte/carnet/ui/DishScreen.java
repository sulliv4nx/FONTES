package com.fonte.carnet.ui;

import android.app.AlertDialog;
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
import com.fonte.carnet.core.Recipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Un plat Fonte noté comme repas : nombre d'assiettes, ingrédients modifiables, ajout au journal en un seul plat ou
 * ingrédient par ingrédient, et « Garder comme mon plat ».
 */
public class DishScreen extends Screen {
    private static final double[] FACTORS = {0.5, 1, 1.5, 2};
    private static final String[] FACTOR_LABELS = {"½", "1", "1 ½", "2"};

    private final Recipe recipe;
    private final String meal;
    private final int day;
    private final String photo;
    private double factor = 1;
    private boolean single = true;

    /** Un ingrédient et sa quantité pour une portion. */
    private static final class Item {
        Food food;
        double grams;

        Item(Food food, double grams) {
            this.food = food;
            this.grams = grams;
        }
    }

    private final List<Item> items = new ArrayList<>();

    // Vues mises à jour sans tout reconstruire (pour ne pas perdre la saisie en cours).
    private TextView kcalView;
    private TextView subView;
    private TextView macroView;

    public DishScreen(Recipe recipe, String meal, int day, String photo) {
        this.recipe = recipe;
        this.meal = meal;
        this.day = day;
        this.photo = photo;
        for (Recipe.Ing i : recipe.ingredients) {
            Food f = Foods.find(i.foodId);
            if (f != null) items.add(new Item(f, i.grams));
        }
    }

    @Override
    public String title() {
        return recipe.name;
    }

    private boolean main() {
        return Recipe.MAIN.equals(recipe.kind);
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        LinearLayout tags = Ui.hbox(a);
        tags.addView(tag("Plat Paradoxe", FoodAddScreen.DISH_TEXT, FoodAddScreen.DISH_BG));
        if (recipe.has(Recipe.CREOLE)) tags.addView(tag("Réunionnais", FoodAddScreen.CREOLE_TEXT, FoodAddScreen.CREOLE_BG));
        c.addView(tags);

        TextView q = Ui.text(a, main() ? "Combien d'assiettes ?" : "Combien de portions ?", 13, Ui.MUTED);
        q.setPadding(0, Ui.dp(14), 0, Ui.dp(8));
        c.addView(q);
        View[] chips = new View[FACTORS.length];
        for (int i = 0; i < FACTORS.length; i++) {
            double f = FACTORS[i];
            chips[i] = Ui.chip(a, FACTOR_LABELS[i], Math.abs(factor - f) < 1e-9, v -> {
                factor = f;
                a.refresh();
            });
        }
        c.addView(Ui.chipRow(a, chips));

        LinearLayout total = Ui.card(a);
        total.setLayoutParams(Ui.mlp(-1, -2, 0, 12, 0, 0));
        LinearLayout kRow = Ui.hbox(a);
        kRow.setGravity(Gravity.BOTTOM);
        kcalView = Ui.text(a, "", 30, Ui.TEXT);
        kcalView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        Ui.tabular(kcalView);
        kRow.addView(kcalView);
        subView = Ui.text(a, "", 15, Ui.MUTED);
        subView.setPadding(Ui.dp(6), 0, 0, Ui.dp(5));
        kRow.addView(subView, Ui.weight(1));
        total.addView(kRow);
        macroView = Ui.text(a, "", 13, Ui.MUTED);
        macroView.setPadding(0, Ui.dp(8), 0, 0);
        total.addView(macroView);
        c.addView(total);

        LinearLayout list = Ui.card(a);
        list.setLayoutParams(Ui.mlp(-1, -2, 0, 12, 0, 0));
        list.setPadding(Ui.dp(16), Ui.dp(4), Ui.dp(16), Ui.dp(4));
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) list.addView(Ui.divider(a));
            list.addView(itemRow(items.get(i)));
        }
        if (!items.isEmpty()) list.addView(Ui.divider(a));
        LinearLayout actions = Ui.hbox(a);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView add = Ui.medium(a, "+ Ajouter un ingrédient", 14, Ui.ACCENT);
        add.setMinHeight(Ui.dp(48));
        add.setGravity(Gravity.CENTER_VERTICAL);
        add.setOnClickListener(v -> pick(null));
        actions.addView(add, Ui.weight(1));
        list.addView(actions);
        c.addView(list);
        TextView hint = Ui.text(a, "Change une quantité : les calories suivent. Touche un ingrédient pour le remplacer, par exemple le riz "
                + "par des grains ou des lentilles.", 12, Ui.FAINT);
        hint.setPadding(0, Ui.dp(8), 0, 0);
        c.addView(hint);

        TextView j = Ui.text(a, "Dans le journal", 13, Ui.MUTED);
        j.setPadding(0, Ui.dp(16), 0, Ui.dp(8));
        c.addView(j);
        c.addView(Ui.segmented(a, new String[]{"Un seul plat", "Chaque ingrédient"}, single ? 0 : 1, i -> {
            single = i == 0;
            a.refresh();
        }));

        LinearLayout buttons = Ui.hbox(a);
        buttons.setPadding(0, Ui.dp(18), 0, 0);
        TextView keep = Ui.button(a, "Garder comme mon plat", Ui.GHOST, v -> keep());
        TextView go = Ui.button(a, QuantityDialog.addLabel(meal), Ui.PRIMARY, v -> addToJournal());
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, -2, 1);
        l1.rightMargin = Ui.dp(8);
        buttons.addView(keep, l1);
        buttons.addView(go, new LinearLayout.LayoutParams(0, -2, 1.4f));
        c.addView(buttons);
        TextView foot = Ui.text(a, "« Garder comme mon plat » le range dans tes aliments, avec ces quantités.", 12, Ui.FAINT);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, Ui.dp(10), 0, 0);
        c.addView(foot);
        updateTotals();
        return Ui.page(a, c);
    }

    private View itemRow(Item it) {
        LinearLayout row = Ui.hbox(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(Ui.dp(56));
        TextView name = Ui.text(a, it.food.name, 15, Ui.TEXT);
        name.setPadding(0, Ui.dp(8), Ui.dp(8), Ui.dp(8));
        name.setOnClickListener(v -> pick(it));
        name.setContentDescription(it.food.name + ", toucher pour remplacer");
        row.addView(name, Ui.weight(1));
        EditText g = Ui.field(a, "g", Ui.DECIMAL);
        g.setText(Fmt.kg(Math.round(it.grams * factor)));
        g.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        g.setContentDescription("Grammes de " + it.food.name);
        Ui.onText(g, t -> {
            double v = Fmt.parse(t);
            it.grams = Double.isNaN(v) || v < 0 ? 0 : v / factor;
            updateTotals();
        });
        row.addView(g, new LinearLayout.LayoutParams(Ui.dp(92), -2));
        TextView unit = Ui.text(a, "g", 14, Ui.MUTED);
        unit.setPadding(Ui.dp(6), 0, Ui.dp(4), 0);
        row.addView(unit);
        ImageView remove = Ui.iconButton(a, Icon.CLOSE, Ui.MUTED, v -> {
            items.remove(it);
            a.refresh();
        });
        remove.setContentDescription("Retirer " + it.food.name);
        row.addView(remove);
        return row;
    }

    /** {kcal, protéines, glucides, lipides, grammes} pour le nombre d'assiettes choisi. */
    private double[] totals() {
        double[] t = new double[5];
        for (Item it : items) {
            double g = it.grams * factor;
            double k = g / 100.0;
            t[0] += it.food.kcal * k;
            t[1] += it.food.protein * k;
            t[2] += it.food.carbs * k;
            t[3] += it.food.fat * k;
            t[4] += g;
        }
        return t;
    }

    private String portionText() {
        String unit = main() ? "assiette" : "portion";
        String n = FACTOR_LABELS[0];
        for (int i = 0; i < FACTORS.length; i++) if (Math.abs(FACTORS[i] - factor) < 1e-9) n = FACTOR_LABELS[i];
        return n + " " + unit + (factor > 1 ? "s" : "");
    }

    private void updateTotals() {
        if (kcalView == null) return;
        double[] t = totals();
        kcalView.setText(Fmt.kg(Math.round(t[0])));
        subView.setText("kcal pour " + portionText() + " · " + Fmt.kg(Math.round(t[4])) + " g");
        macroView.setText("Protéines " + Math.round(t[1]) + " g · Glucides " + Math.round(t[2]) + " g · Lipides " + Math.round(t[3]) + " g");
    }

    private void addToJournal() {
        double[] t = totals();
        if (t[4] <= 0) {
            Ui.toast(a, "Ajoute au moins un ingrédient.");
            return;
        }
        long now = System.currentTimeMillis();
        if (single) {
            FoodEntry e = new FoodEntry();
            e.day = day;
            e.meal = meal;
            e.photo = photo;
            e.time = now;
            e.name = recipe.name + (Math.abs(factor - 1) < 1e-9 ? "" : " (" + portionText() + ")");
            e.grams = t[4];
            e.kcal = t[0];
            e.protein = t[1];
            e.carbs = t[2];
            e.fat = t[3];
            st.addFood(e);
        } else {
            for (Item it : items) {
                double g = it.grams * factor;
                if (g <= 0) continue;
                FoodEntry e = FoodEntry.of(it.food, g);
                e.day = day;
                e.meal = meal;
                e.photo = photo;
                e.time = now;
                st.addFood(e);
            }
        }
        a.back();
        Ui.toast(a, "Ajouté : " + recipe.name + " · " + Fmt.kg(Math.round(t[0])) + " kcal");
    }

    /** Range le plat dans « Mes aliments », valeurs pour 100 g, une portion = la quantité choisie. */
    private void keep() {
        double[] t = totals();
        if (t[4] <= 0) {
            Ui.toast(a, "Ajoute au moins un ingrédient.");
            return;
        }
        Food f = new Food();
        String name = recipe.name + " (mon plat)";
        for (Food c : st.customFoods) if (name.equals(c.name)) f.id = c.id;
        f.name = name;
        double k = 100.0 / t[4];
        f.kcal = Math.round(t[0] * k);
        f.protein = Math.round(t[1] * k * 10) / 10.0;
        f.carbs = Math.round(t[2] * k * 10) / 10.0;
        f.fat = Math.round(t[3] * k * 10) / 10.0;
        f.portion = Math.round(t[4]);
        f.portionLabel = portionText();
        st.saveCustomFood(f);
        Ui.toast(a, "Gardé dans tes aliments : " + name);
    }

    /** Choisit un ingrédient à ajouter (it == null) ou à remplacer. */
    private void pick(Item it) {
        LinearLayout box = Ui.vbox(a);
        box.setPadding(Ui.dp(20), Ui.dp(4), Ui.dp(20), Ui.dp(4));
        EditText search = Ui.field(a, "Rechercher un ingrédient…", Ui.TEXT_FIELD);
        box.addView(search);
        LinearLayout list = Ui.vbox(a);
        list.setPadding(0, Ui.dp(8), 0, 0);
        box.addView(list);
        android.widget.ScrollView sv = new android.widget.ScrollView(a);
        sv.addView(box);
        AlertDialog d = new AlertDialog.Builder(a)
                .setTitle(it == null ? "Ajouter un ingrédient" : "Remplacer " + it.food.name)
                .setView(sv)
                .setNegativeButton("Annuler", null)
                .create();
        Runnable fill = () -> {
            list.removeAllViews();
            String query = search.getText().toString().trim();
            if (query.isEmpty()) {
                list.addView(Ui.text(a, "Tape le nom d'un aliment : base de l'appli, Ciqual et tes aliments.", 13, Ui.MUTED));
                return;
            }
            FoodSearch.Input in = new FoodSearch.Input();
            in.mine = st.customFoods;
            in.fonte = Foods.all();
            in.ciqual = Ciqual.all();
            int n = 0;
            for (FoodSearch.Result r : FoodSearch.search(query, in)) {
                if (r.food == null) continue;
                if (n++ >= 25) break;
                Food f = r.food;
                LinearLayout row = Ui.vbox(a);
                row.setPadding(Ui.dp(4), Ui.dp(10), Ui.dp(4), Ui.dp(10));
                row.setBackground(Ui.pressable(Ui.rect(0x00000000, 10), 10));
                row.addView(Ui.text(a, f.name, 15, Ui.TEXT));
                row.addView(Ui.text(a, FoodAddScreen.macros(f), 12, Ui.MUTED));
                row.setOnClickListener(v -> {
                    if (it == null) {
                        com.fonte.carnet.core.Portions.Portion p = com.fonte.carnet.core.Portions.guess(f);
                        double g = f.portion > 0 ? f.portion : p != null ? p.grams : 100;
                        items.add(new Item(f, g / factor));
                    } else {
                        it.food = f;
                    }
                    Ui.hideKeyboard(search);
                    d.dismiss();
                    a.refresh();
                });
                list.addView(row);
            }
            if (n == 0) list.addView(Ui.text(a, "Aucun aliment trouvé.", 13, Ui.MUTED));
        };
        Ui.onText(search, t -> fill.run());
        fill.run();
        d.show();
    }

    private TextView tag(String s, int color, int bg) {
        TextView t = Ui.medium(a, s, 11, color);
        t.setBackground(Ui.rect(bg, 7));
        t.setPadding(Ui.dp(7), Ui.dp(2), Ui.dp(7), Ui.dp(2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = Ui.dp(6);
        t.setLayoutParams(lp);
        return t;
    }
}

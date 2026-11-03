package com.fonte.carnet.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Food;
import com.fonte.carnet.core.FoodEntry;
import com.fonte.carnet.core.Foods;
import com.fonte.carnet.core.LabelParser;

import java.util.List;

/** Ajout d'aliments à un repas : recherche, code-barres, étiquette, photo du repas, saisie manuelle. */
public class FoodAddScreen extends Screen {
    private static final int REQ_BARCODE_CAM = 41;
    private static final int REQ_BARCODE_PICK = 42;
    private static final int REQ_LABEL_CAM = 43;
    private static final int REQ_LABEL_PICK = 44;
    private static final int REQ_MEAL_CAM = 45;
    private static final int REQ_MEAL_PICK = 46;

    private String meal;
    private final int day;
    private String query = "";
    private String photo;
    private LinearLayout results;

    public FoodAddScreen(String meal, int day) {
        this.meal = meal;
        this.day = day;
    }

    @Override
    public String title() {
        return "Ajouter · " + meal;
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

        c.addView(Ui.grid(a, 2,
                Ui.tile(a, Icon.BARCODE, "Code-barres", "Fiche produit Open Food Facts",
                        v -> Camera.choose(a, "Code-barres du produit", REQ_BARCODE_CAM, REQ_BARCODE_PICK)),
                Ui.tile(a, Icon.CAMERA, "Étiquette", "Lit le tableau nutritionnel",
                        v -> Camera.choose(a, "Tableau nutritionnel", REQ_LABEL_CAM, REQ_LABEL_PICK)),
                Ui.tile(a, Icon.CAMERA, "Photo du repas", photo == null ? "Jointe au journal" : "Photo jointe ✓",
                        v -> Camera.choose(a, "Photo du repas", REQ_MEAL_CAM, REQ_MEAL_PICK)),
                Ui.tile(a, Icon.EDIT, "Saisie manuelle", "Calories et macros",
                        v -> a.show(new FoodFormScreen(null, meal, day, photo)))));

        if (photo != null) c.addView(photoCard());

        EditText search = Ui.field(a, "Rechercher un aliment…", Ui.TEXT_FIELD);
        search.setText(query);
        search.setSelectAllOnFocus(false);
        Ui.onText(search, t -> {
            query = t;
            fillResults();
        });
        c.addView(search, Ui.mlp(-1, -2, 0, 4, 0, 4));
        results = Ui.vbox(a);
        c.addView(results);
        fillResults();
        return Ui.page(a, c);
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

    private void fillResults() {
        if (results == null) return;
        results.removeAllViews();
        String q = query.trim();
        if (q.isEmpty()) {
            List<Food> recent = st.recentFoods(8);
            if (!recent.isEmpty()) {
                results.addView(Ui.section(a, "Récents"));
                for (Food f : recent) results.addView(row(f));
            }
            if (!st.customFoods.isEmpty()) {
                results.addView(Ui.section(a, Foods.MINE));
                for (Food f : st.customFoods) results.addView(row(f));
            }
            for (String cat : Foods.CATEGORIES) {
                if (Foods.MINE.equals(cat)) continue;
                results.addView(Ui.section(a, cat));
                for (Food f : Foods.all()) if (cat.equals(f.category)) results.addView(row(f));
            }
            return;
        }
        List<Food> found = Foods.search(st.allFoods(), q);
        TextView create = Ui.button(a, "+ Créer « " + q + " »", Ui.GHOST, v -> {
            Food f = new Food();
            f.name = Character.toUpperCase(q.charAt(0)) + q.substring(1);
            a.show(new FoodFormScreen(f, meal, day, photo));
        });
        results.addView(create);
        if (found.isEmpty()) {
            results.addView(Ui.hint(a, "Aucun aliment trouvé. Essaie un autre mot, scanne le code-barres ou crée l'aliment."));
        }
        int n = 0;
        for (Food f : found) {
            if (n++ >= 40) break;
            results.addView(row(f));
        }
    }

    private View row(Food f) {
        LinearLayout row = Ui.hbox(a);
        row.setPadding(Ui.dp(12), Ui.dp(10), Ui.dp(12), Ui.dp(10));
        row.setBackground(Ui.pressable(Ui.rect(Ui.SURFACE, 12), 12));
        row.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 6));
        row.setOnClickListener(v -> QuantityDialog.add(a, f, meal, day, photo, null));
        if (f.custom) {
            row.setOnLongClickListener(v -> {
                Ui.confirm(a, "Supprimer « " + f.name + " » de tes aliments ?", "Ton journal n'est pas modifié.", "Supprimer", () -> {
                    st.deleteCustomFood(f);
                    fillResults();
                });
                return true;
            });
        }
        LinearLayout txt = Ui.vbox(a);
        txt.addView(Ui.text(a, f.name, 15, Ui.TEXT));
        txt.addView(Ui.text(a, f.per100(), 12, Ui.MUTED));
        row.addView(txt, Ui.weight(1));
        ImageView go = new ImageView(a);
        go.setImageDrawable(new Icon(Icon.PLUS, Ui.ACCENT));
        row.addView(go, Ui.lp(Ui.dp(20), Ui.dp(20)));
        return row;
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

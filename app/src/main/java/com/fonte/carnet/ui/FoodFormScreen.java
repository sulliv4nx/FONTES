package com.fonte.carnet.ui;

import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Food;
import com.fonte.carnet.core.FoodEntry;

/** Saisie (ou vérification) des valeurs d'un aliment pour 100 g, puis ajout au repas. */
public class FoodFormScreen extends Screen {
    private final Food draft = new Food();
    private final String meal;
    private final int day;
    private final String photo;
    private final boolean prefilled;
    private String nameText;
    private String kcalText;
    private String proteinText;
    private String carbsText;
    private String fatText;
    private String portionText;
    private String gramsText;
    private boolean keep = true;

    /** Texte d'aide affiché en tête du formulaire (remplace celui par défaut). */
    private String note;

    public FoodFormScreen withNote(String n) {
        note = n;
        return this;
    }

    /** @param prefill valeurs lues (code-barres, étiquette) ou nom à créer ; null pour une saisie vierge. */
    public FoodFormScreen(Food prefill, String meal, int day, String photo) {
        this.meal = meal;
        this.day = day;
        this.photo = photo;
        prefilled = prefill != null && (prefill.kcal > 0 || prefill.protein > 0 || prefill.carbs > 0 || prefill.fat > 0);
        if (prefill != null) {
            draft.name = prefill.name;
            draft.barcode = prefill.barcode;
            draft.portionLabel = prefill.portionLabel;
        }
        nameText = prefill == null || prefill.name == null ? "" : prefill.name;
        kcalText = prefilled ? Fmt.kg(prefill.kcal) : "";
        proteinText = prefilled ? Fmt.kg(prefill.protein) : "";
        carbsText = prefilled ? Fmt.kg(prefill.carbs) : "";
        fatText = prefilled ? Fmt.kg(prefill.fat) : "";
        portionText = prefill != null && prefill.portion > 0 ? Fmt.kg(prefill.portion) : "";
        gramsText = prefill != null && prefill.portion > 0 ? Fmt.kg(prefill.portion) : "100";
    }

    @Override
    public String title() {
        return prefilled ? "Vérifier l'aliment" : "Nouvel aliment";
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        if (note != null) {
            c.addView(Ui.hint(a, note));
        } else if (prefilled) {
            c.addView(Ui.hint(a, draft.barcode != null
                    ? "Valeurs issues d'Open Food Facts (code " + draft.barcode + "). Vérifie-les avec l'emballage."
                    : "Valeurs lues sur la photo de l'étiquette. Vérifie-les : la lecture peut se tromper d'un chiffre."));
        }
        EditText name = Ui.field(a, "ex. Yaourt grec", Ui.TEXT_FIELD);
        name.setText(nameText);
        Ui.onText(name, t -> nameText = t);
        c.addView(Ui.labelled(a, "Nom", name));

        LinearLayout card = Ui.card(a);
        card.addView(Ui.medium(a, "Pour 100 g (ou 100 ml)", 15, Ui.TEXT));
        TextView sub = Ui.text(a, "Recopie la colonne « pour 100 g » de l'étiquette.", 12, Ui.MUTED);
        sub.setPadding(0, 0, 0, Ui.dp(8));
        card.addView(sub);
        card.addView(pair(field("Calories (kcal)", kcalText, t -> kcalText = t), field("Protéines (g)", proteinText, t -> proteinText = t)));
        card.addView(pair(field("Glucides (g)", carbsText, t -> carbsText = t), field("Lipides (g)", fatText, t -> fatText = t)));
        c.addView(card);

        c.addView(pair(field("Portion habituelle (g)", portionText, t -> portionText = t),
                field("Quantité mangée (g)", gramsText, t -> gramsText = t)));

        CheckBox save = new CheckBox(a);
        save.setText("Enregistrer dans mes aliments");
        save.setTextColor(Ui.TEXT);
        save.setChecked(keep);
        save.setOnCheckedChangeListener((b, on) -> keep = on);
        c.addView(save);
        c.addView(Ui.button(a, "Ajouter à : " + meal, Ui.PRIMARY, v -> submit()));
        return Ui.page(a, c);
    }

    private interface Setter {
        void set(String s);
    }

    private LinearLayout field(String label, String value, Setter setter) {
        EditText e = Ui.field(a, "0", Ui.DECIMAL);
        e.setText(value);
        Ui.onText(e, setter::set);
        return Ui.labelled(a, label, e);
    }

    private LinearLayout pair(View left, View right) {
        LinearLayout row = Ui.hbox(a);
        row.setGravity(android.view.Gravity.TOP);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
        p.rightMargin = Ui.dp(10);
        row.addView(left, p);
        row.addView(right, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    private static double num(String s) {
        double v = Fmt.parse(s);
        return Double.isNaN(v) || v < 0 ? 0 : v;
    }

    private void submit() {
        String n = nameText.trim();
        if (n.isEmpty()) {
            Ui.toast(a, "Donne un nom à l'aliment.");
            return;
        }
        draft.name = n;
        draft.protein = num(proteinText);
        draft.carbs = num(carbsText);
        draft.fat = num(fatText);
        double kcal = Fmt.parse(kcalText);
        draft.kcal = Double.isNaN(kcal) || kcal <= 0 ? Math.round(4 * draft.protein + 4 * draft.carbs + 9 * draft.fat) : kcal;
        if (draft.kcal > 950 || draft.protein > 100 || draft.carbs > 100 || draft.fat > 100) {
            Ui.toast(a, "Valeurs trop élevées pour 100 g : vérifie la saisie.");
            return;
        }
        draft.portion = num(portionText);
        double grams = Fmt.parse(gramsText);
        if (Double.isNaN(grams) || grams <= 0 || grams > 5000) {
            Ui.toast(a, "Indique la quantité mangée en grammes.");
            return;
        }
        Food f = draft;
        if (keep) f = st.saveCustomFood(draft);
        FoodEntry e = FoodEntry.of(f, grams);
        if (!keep) e.foodId = null;
        e.day = day;
        e.meal = meal;
        e.photo = photo;
        st.addFood(e);
        Ui.hideKeyboard(a.getWindow().getDecorView());
        a.back();
        Ui.toast(a, "Ajouté : " + f.name + " · " + Fmt.kg(Math.round(e.kcal)) + " kcal");
    }
}

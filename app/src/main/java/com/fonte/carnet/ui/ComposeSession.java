package com.fonte.carnet.ui;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.fonte.carnet.core.Composer;
import com.fonte.carnet.core.Days;
import com.fonte.carnet.core.MealPlan;
import com.fonte.carnet.data.Store;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** État partagé par les écrans « Composer mes repas » (sélection, créneau visé, plats validés). */
final class ComposeSession {
    static final int AMBER = 0xFFFFC66B;

    final Store st;
    final Set<String> selected = new LinkedHashSet<>();
    boolean complete;
    String slot = MealPlan.LUNCH;
    /** Plats validés, par identifiant de recette, dans l'ordre de validation. */
    final Map<String, Composer.Proposal> validated = new LinkedHashMap<>();
    private List<Composer.Proposal> cache;

    ComposeSession(Store st) {
        this.st = st;
        selected.addAll(st.mealPrefs().pantry);
        complete = st.mealPrefs().complete;
    }

    void toggle(String foodId) {
        if (!selected.remove(foodId)) selected.add(foodId);
        changed();
    }

    void clear() {
        selected.clear();
        changed();
    }

    void setComplete(boolean on) {
        complete = on;
        changed();
    }

    void setSlot(String s) {
        slot = s;
        cache = null;
    }

    private void changed() {
        cache = null;
        st.mealPrefs().pantry.clear();
        st.mealPrefs().pantry.addAll(selected);
        st.mealPrefs().complete = complete;
        st.saveSoon();
    }

    double[] target() {
        return st.slotTarget(slot);
    }

    List<Composer.Proposal> proposals() {
        if (cache == null) {
            double[] t = target();
            cache = Composer.propose(selected, complete, Days.today(), slot, t[0], t[1], t[2]);
        }
        return cache;
    }

    boolean isValidated(Composer.Proposal p) {
        return validated.containsKey(p.recipe.id);
    }

    void toggleValidated(Composer.Proposal p) {
        if (validated.remove(p.recipe.id) == null) validated.put(p.recipe.id, p);
    }

    /** Ingrédients à acheter en plus pour les plats validés. */
    List<String> needed() {
        Set<String> out = new LinkedHashSet<>();
        for (Composer.Proposal p : validated.values()) out.addAll(p.need);
        return new ArrayList<>(out);
    }

    static String names(List<String> ids) {
        StringBuilder b = new StringBuilder();
        for (String id : ids) {
            if (b.length() > 0) b.append(", ");
            String l = Composer.label(id);
            b.append(b.length() == 0 ? l : l.toLowerCase(java.util.Locale.FRENCH));
        }
        return b.toString();
    }

    String slotWord() {
        switch (slot) {
            case MealPlan.BREAKFAST:
                return "un petit-déjeuner";
            case MealPlan.DINNER:
                return "un dîner";
            case MealPlan.SNACK_AM:
            case MealPlan.SNACK_PM:
                return "une collation";
            default:
                return "un déjeuner";
        }
    }

    /** « ✓ Poulet · Pâtes · Épinards · + riz, tomates » en couleurs. */
    static CharSequence haveNeed(Composer.Proposal p) {
        SpannableStringBuilder b = new SpannableStringBuilder();
        StringBuilder have = new StringBuilder("✓ ");
        for (int i = 0; i < p.have.size(); i++) {
            if (i > 0) have.append(" · ");
            have.append(Composer.label(p.have.get(i)));
        }
        int s = b.length();
        b.append(have);
        b.setSpan(new ForegroundColorSpan(Ui.OK), s, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (!p.need.isEmpty()) {
            b.append("  ·  ");
            int n = b.length();
            b.append("+ ").append(names(p.need).toLowerCase(java.util.Locale.FRENCH));
            b.setSpan(new ForegroundColorSpan(AMBER), n, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return b;
    }

    /** Libellé de puce avec le petit badge « 974 ». */
    static CharSequence chipLabel(String label, boolean reunion, boolean selected) {
        SpannableStringBuilder b = new SpannableStringBuilder((selected ? "✓ " : "") + label);
        if (reunion) {
            b.append("  ");
            int s = b.length();
            b.append("974");
            b.setSpan(new ForegroundColorSpan(AMBER), s, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            b.setSpan(new RelativeSizeSpan(0.8f), s, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            b.setSpan(new StyleSpan(Typeface.BOLD), s, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return b;
    }
}

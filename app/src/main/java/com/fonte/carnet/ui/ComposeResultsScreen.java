package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fonte.carnet.core.Composer;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.MealPlan;
import com.fonte.carnet.core.Nutrition;

import java.util.ArrayList;
import java.util.List;

/** Composer mes repas, étape 2 : les repas possibles, à valider. */
public class ComposeResultsScreen extends ScrollScreen {
    private final ComposeSession session;
    private int filter;

    ComposeResultsScreen(ComposeSession session) {
        this.session = session;
    }

    @Override
    public String title() {
        return "Repas proposés";
    }

    @Override
    public View build() {
        List<Composer.Proposal> all = session.proposals();
        List<Composer.Proposal> reunion = new ArrayList<>();
        List<Composer.Proposal> ready = new ArrayList<>();
        for (Composer.Proposal p : all) {
            if (p.reunion) reunion.add(p);
            if (p.nothingToBuy()) ready.add(p);
        }
        LinearLayout c = Ui.vbox(a);

        // Ingrédients choisis
        List<View> minis = new ArrayList<>();
        for (String id : session.selected) {
            TextView t = Ui.medium(a, Composer.label(id), 12, Ui.TEXT);
            t.setBackground(Ui.rect(Ui.SURFACE2, 14));
            t.setPadding(Ui.dp(10), Ui.dp(5), Ui.dp(10), Ui.dp(5));
            minis.add(t);
        }
        TextView edit = Ui.medium(a, "Modifier", 13, Ui.ACCENT);
        edit.setPadding(Ui.dp(4), Ui.dp(5), Ui.dp(4), Ui.dp(5));
        edit.setOnClickListener(v -> a.back());
        minis.add(edit);
        View flow = Ui.flow(a, minis.toArray(new View[0]));
        ((LinearLayout.LayoutParams) flow.getLayoutParams()).bottomMargin = Ui.dp(12);
        c.addView(flow);

        LinearLayout seg = Ui.segmented(a, new String[]{"Tous · " + all.size(), "Réunionnais · " + reunion.size(),
                "Rien à acheter · " + ready.size()}, filter, i -> {
            filter = i;
            scrollTop();
            a.refresh();
        });
        for (int i = 0; i < seg.getChildCount(); i++) {
            TextView st = (TextView) seg.getChildAt(i);
            st.setTextSize(12);
            st.setSingleLine(false);
            st.setMaxLines(2);
        }
        seg.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 10));
        c.addView(seg);

        double[] t = session.target();
        LinearLayout targetRow = Ui.hbox(a);
        targetRow.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 10));
        targetRow.addView(Ui.text(a, "Ajustés pour " + session.slotWord() + " ≈ " + Fmt.kg(Math.round(t[0] / 10) * 10)
                + " kcal · P " + Math.round(t[1]) + " g", 13, Ui.MUTED), Ui.weight(1));
        TextView change = Ui.medium(a, "Changer", 13, Ui.ACCENT);
        change.setPadding(Ui.dp(8), Ui.dp(8), 0, Ui.dp(8));
        change.setOnClickListener(v -> chooseSlot());
        targetRow.addView(change);
        c.addView(targetRow);

        List<Composer.Proposal> shown = filter == 1 ? reunion : filter == 2 ? ready : all;
        if (shown.isEmpty()) {
            LinearLayout card = Ui.card(a);
            card.addView(Ui.medium(a, "Aucun repas ici", 16, Ui.TEXT));
            TextView h = Ui.text(a, session.complete
                    ? "Ajoute une protéine (viande, poisson, œufs, tofu) ou un féculent, ou change de filtre."
                    : "Active « Compléter si besoin » pour voir les plats qui demandent un ingrédient en plus.", 13, Ui.MUTED);
            h.setPadding(0, Ui.dp(4), 0, 0);
            card.addView(h);
            c.addView(card);
        }
        for (Composer.Proposal p : shown) c.addView(card(p));

        ScrollView sv = page(c);
        LinearLayout root = Ui.vbox(a);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(bottomBar());
        return root;
    }

    private void chooseSlot() {
        String[] slots = {MealPlan.BREAKFAST, MealPlan.LUNCH, MealPlan.SNACK_AM, MealPlan.DINNER};
        String[] labels = {"Petit-déjeuner", "Déjeuner", "Collation", "Dîner"};
        new AlertDialog.Builder(a)
                .setTitle("Ajuster pour")
                .setItems(labels, (d, w) -> {
                    session.setSlot(slots[w]);
                    scrollTop();
                    a.refresh();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private View card(Composer.Proposal p) {
        boolean on = session.isValidated(p);
        Nutrition.Totals t = p.meal.totals();
        LinearLayout card = Ui.clickCard(a, v -> a.show(new ComposeDetailScreen(session, p)));
        if (on) card.setBackground(Ui.pressable(Ui.outline(Ui.SURFACE, Ui.ACCENT, 18), 18));
        LinearLayout head = Ui.hbox(a);
        head.setGravity(Gravity.TOP);
        head.addView(Ui.medium(a, p.recipe.name, 17, Ui.TEXT), Ui.weight(1));
        TextView k = Ui.medium(a, Fmt.kg(Math.round(t.kcal)) + " kcal", 14, Ui.TEXT);
        Ui.tabular(k);
        k.setPadding(Ui.dp(8), Ui.dp(2), 0, 0);
        head.addView(k);
        card.addView(head);
        TextView hn = Ui.text(a, ComposeSession.haveNeed(p), 13, Ui.MUTED);
        hn.setPadding(0, Ui.dp(6), 0, 0);
        card.addView(hn);
        card.addView(Ui.macroDots(a, t.protein, t.carbs, t.fat));
        List<View> tags = new ArrayList<>();
        if (p.reunion) tags.add(Ui.badge(a, "Réunionnais", ComposeSession.AMBER, 0x24FFC66B));
        tags.add(Ui.badge(a, p.recipe.minutes + " min", Ui.MUTED, Ui.SURFACE2));
        if (p.nothingToBuy()) tags.add(Ui.badge(a, "Rien à acheter", Ui.OK, Ui.OK_SOFT));
        if (p.lowProtein) tags.add(Ui.badge(a, "Protéines un peu justes", Ui.MUTED, Ui.SURFACE2));
        View tf = Ui.flow(a, tags.toArray(new View[0]));
        ((LinearLayout.LayoutParams) tf.getLayoutParams()).topMargin = Ui.dp(8);
        card.addView(tf);
        TextView valid = Ui.smallButton(a, on ? "✓ Validé" : "Valider", on ? Ui.PRIMARY : Ui.SECONDARY, v -> {
            session.toggleValidated(p);
            a.refresh();
        });
        valid.setContentDescription(on ? p.recipe.name + ", validé. Appuyer pour retirer." : "Valider " + p.recipe.name);
        card.addView(Ui.buttonRow(a,
                Ui.smallButton(a, "Voir", Ui.GHOST, v -> a.show(new ComposeDetailScreen(session, p))),
                valid));
        return card;
    }

    private View bottomBar() {
        int n = session.validated.size();
        LinearLayout bar = Ui.hbox(a);
        bar.setBackground(Ui.rect(Ui.SURFACE, 0));
        int side = Ui.dp(Ui.pad(a));
        bar.setPadding(side, Ui.dp(10), side, Ui.dp(12));
        View line = new View(a);
        LinearLayout wrap = Ui.vbox(a);
        line.setBackgroundColor(Ui.LINE);
        wrap.addView(line, Ui.lp(-1, 1));
        LinearLayout txt = Ui.vbox(a);
        txt.addView(Ui.medium(a, n == 0 ? "Aucun repas validé" : n + " repas validé" + (n > 1 ? "s" : ""), 15, Ui.TEXT));
        List<String> need = session.needed();
        TextView sub = Ui.text(a, n == 0 ? "Valide les plats qui te tentent." : need.isEmpty() ? "Rien à acheter en plus"
                : "À acheter en plus : " + ComposeSession.names(need).toLowerCase(java.util.Locale.FRENCH), 12, Ui.FAINT);
        sub.setMaxLines(2);
        txt.addView(sub);
        bar.addView(txt, Ui.weight(1));
        TextView place = Ui.button(a, "Placer dans mon plan", Ui.PRIMARY, v -> {
            if (session.validated.isEmpty()) {
                Ui.toast(a, "Valide au moins un repas.");
                return;
            }
            PlaceSheet.show(a, session);
        });
        place.setTextSize(15);
        place.setAlpha(n == 0 ? 0.6f : 1f);
        LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
        lp.leftMargin = Ui.dp(12);
        bar.addView(place, lp);
        wrap.addView(bar, Ui.lp(-1, -2));
        return wrap;
    }
}

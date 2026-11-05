package com.fonte.carnet.ui;

import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Composer;
import com.fonte.carnet.core.Foods;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Composer mes repas, étape 1 : choisir ses ingrédients. */
public class ComposeScreen extends ScrollScreen {
    private ComposeSession session;
    private String query = "";
    private LinearLayout selectionBox;
    private LinearLayout groupsBox;
    private TextView cta;

    @Override
    public String title() {
        return "Composer mes repas";
    }

    @Override
    public View build() {
        if (session == null) session = new ComposeSession(st);
        LinearLayout c = Ui.vbox(a);
        c.addView(Ui.hint(a, "Choisis ce que tu as dans le frigo ou ce que tu as envie de manger. L'appli compose des repas avec, ajustés à ton objectif."));

        EditText search = Ui.field(a, "Rechercher un ingrédient", Ui.TEXT_FIELD);
        search.setText(query);
        search.setSelectAllOnFocus(false);
        search.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 12));
        Ui.onText(search, s -> {
            query = s;
            fillGroups();
        });
        c.addView(search);

        selectionBox = Ui.card(a);
        c.addView(selectionBox);
        groupsBox = Ui.vbox(a);
        c.addView(groupsBox);

        LinearLayout opt = Ui.card(a);
        opt.setLayoutParams(Ui.mlp(-1, -2, 0, 12, 0, 8));
        opt.addView(Ui.switchRow(a, "Compléter si besoin",
                "L'appli peut ajouter du riz, des tomates ou de l'huile pour finir le plat. Ce qui manque va dans ta liste de courses.",
                session.complete, on -> {
                    session.setComplete(on);
                    updateCount();
                }));
        c.addView(opt);
        TextView legend = Ui.text(a, ComposeSession.chipLabel("", true, false), 12, Ui.FAINT);
        legend.append("  ingrédient de la cuisine réunionnaise");
        legend.setPadding(0, 0, 0, Ui.dp(12));
        c.addView(legend);

        cta = Ui.button(a, "", Ui.PRIMARY, v -> {
            if (session.selected.isEmpty()) {
                Ui.toast(a, "Choisis au moins un ingrédient.");
                return;
            }
            a.show(new ComposeResultsScreen(session));
        });
        c.addView(cta);
        fillSelection();
        fillGroups();
        updateCount();
        return page(c);
    }

    private void update() {
        fillSelection();
        fillGroups();
        updateCount();
    }

    private void updateCount() {
        if (cta == null) return;
        if (session.selected.isEmpty()) {
            cta.setText("Choisis tes ingrédients");
            cta.setAlpha(0.6f);
            return;
        }
        int n = session.proposals().size();
        cta.setAlpha(n > 0 ? 1f : 0.6f);
        cta.setText(n > 0 ? "Voir les repas possibles · " + n : "Aucun repas possible");
    }

    private void fillSelection() {
        selectionBox.removeAllViews();
        LinearLayout head = Ui.hbox(a);
        TextView label = Ui.medium(a, ("Ma sélection · " + session.selected.size()).toUpperCase(Locale.FRENCH), 11, Ui.MUTED);
        label.setLetterSpacing(0.06f);
        head.addView(label, Ui.weight(1));
        if (!session.selected.isEmpty()) {
            TextView clear = Ui.medium(a, "Tout effacer", 13, Ui.ACCENT);
            clear.setPadding(Ui.dp(8), Ui.dp(8), 0, Ui.dp(8));
            clear.setOnClickListener(v -> {
                session.clear();
                update();
            });
            head.addView(clear);
        }
        selectionBox.addView(head);
        if (session.selected.isEmpty()) {
            TextView t = Ui.text(a, "Touche les ingrédients ci-dessous pour les ajouter.", 13, Ui.FAINT);
            t.setPadding(0, Ui.dp(6), 0, 0);
            selectionBox.addView(t);
            return;
        }
        List<View> chips = new ArrayList<>();
        for (String id : session.selected) {
            TextView chip = Ui.medium(a, Composer.label(id) + "  ✕", 13, Ui.BG);
            chip.setGravity(Gravity.CENTER);
            chip.setMinHeight(Ui.dp(36));
            chip.setPadding(Ui.dp(13), Ui.dp(6), Ui.dp(11), Ui.dp(6));
            chip.setBackground(Ui.pressable(Ui.rect(Ui.ACCENT, 18), 18));
            chip.setContentDescription("Retirer " + Composer.label(id));
            chip.setOnClickListener(v -> {
                session.toggle(id);
                update();
            });
            chips.add(chip);
        }
        View flow = Ui.flow(a, chips.toArray(new View[0]));
        ((LinearLayout.LayoutParams) flow.getLayoutParams()).topMargin = Ui.dp(10);
        selectionBox.addView(flow);
    }

    private void fillGroups() {
        groupsBox.removeAllViews();
        String q = Foods.norm(query);
        String[] groups = {Composer.PROTEINS, Composer.STARCHES, Composer.VEGETABLES};
        int shown = 0;
        for (String g : groups) {
            List<View> chips = new ArrayList<>();
            for (Composer.Choice ch : Composer.catalog()) {
                if (!ch.group.equals(g)) continue;
                if (!q.isEmpty() && !Foods.norm(ch.label).contains(q)) continue;
                boolean on = session.selected.contains(ch.foodId);
                TextView t = Ui.medium(a, ComposeSession.chipLabel(ch.label, ch.reunion, on), 13, on ? Ui.ACCENT : Ui.MUTED);
                t.setGravity(Gravity.CENTER);
                t.setMinHeight(Ui.dp(40));
                t.setPadding(Ui.dp(13), Ui.dp(8), Ui.dp(13), Ui.dp(8));
                t.setBackground(Ui.pressable(on ? Ui.outline(Ui.ACCENT_SOFT, Ui.ACCENT, 20) : Ui.outline(0x00000000, Ui.LINE, 20), 20));
                t.setContentDescription(ch.label + (ch.reunion ? ", cuisine réunionnaise" : "") + (on ? ", choisi" : ""));
                t.setOnClickListener(v -> {
                    session.toggle(ch.foodId);
                    update();
                });
                chips.add(t);
            }
            if (chips.isEmpty()) continue;
            shown += chips.size();
            groupsBox.addView(Ui.section(a, g));
            groupsBox.addView(Ui.flow(a, chips.toArray(new View[0])));
        }
        if (shown == 0) {
            TextView t = Ui.hint(a, "Aucun ingrédient ne correspond. Les autres aliments se notent depuis le journal.");
            t.setPadding(0, Ui.dp(12), 0, 0);
            groupsBox.addView(t);
        }
    }
}

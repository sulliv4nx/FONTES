package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.ExerciseEntry;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Session;
import com.fonte.carnet.core.SetEntry;
import com.fonte.carnet.core.Stats;

/** Détail d'une séance terminée. */
public class SessionDetailScreen extends Screen {
    private final long id;

    public SessionDetailScreen(long id) {
        this.id = id;
    }

    @Override
    public String title() {
        Session s = st.session(id);
        return s == null ? "Séance" : Fmt.shortDay(s.start);
    }

    @Override
    public int actionIcon() {
        return Icon.MORE;
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        Session s = st.session(id);
        if (s == null) {
            c.addView(Ui.text(a, "Cette séance a été supprimée.", 15, Ui.MUTED));
            return Ui.page(a, c);
        }
        c.addView(Ui.heading(a, s.name));
        TextView when = Ui.text(a, Fmt.day(s.start), 14, Ui.MUTED);
        when.setPadding(0, Ui.dp(2), 0, Ui.dp(12));
        c.addView(when);
        c.addView(Ui.statRow(a,
                Ui.stat(a, Fmt.duration(s.durationMs()), "durée"),
                Ui.stat(a, String.valueOf(s.doneSets()), "séries"),
                Ui.stat(a, Fmt.tonnage(s.tonnage()), "soulevés")));

        if (!s.records.isEmpty()) {
            LinearLayout rec = Ui.card(a);
            rec.setBackground(Ui.rect(Ui.ACCENT_SOFT, 16));
            rec.addView(Ui.medium(a, "Records battus", 15, Ui.ACCENT));
            for (String r : s.records) {
                TextView t = Ui.text(a, "• " + r, 14, Ui.TEXT);
                t.setPadding(0, Ui.dp(4), 0, 0);
                rec.addView(t);
            }
            c.addView(rec);
        }

        for (ExerciseEntry e : s.exercises) {
            LinearLayout card = Ui.clickCard(a, v -> a.show(new ExerciseScreen(e.exerciseId)));
            card.addView(Ui.medium(a, st.name(e.exerciseId), 16, Ui.TEXT));
            int k = 0;
            for (SetEntry set : e.sets) {
                k++;
                LinearLayout row = Ui.hbox(a);
                row.setPadding(0, Ui.dp(5), 0, 0);
                TextView n = Ui.text(a, String.valueOf(k), 14, Ui.FAINT);
                row.addView(n, Ui.lp(Ui.dp(28), -2));
                String load = set.weight > 0 ? Fmt.kg(set.weight) + " kg × " + set.reps : set.reps + " reps";
                TextView l = Ui.medium(a, load, 15, Ui.TEXT);
                Ui.tabular(l);
                row.addView(l, Ui.weight(1));
                if (set.weight > 0 && set.reps > 1 && set.reps <= Stats.MAX_REPS_FOR_E1RM) {
                    TextView est = Ui.text(a, "1RM ≈ " + Fmt.kg(Stats.round1(Stats.e1rm(set.weight, set.reps))), 13, Ui.MUTED);
                    Ui.tabular(est);
                    row.addView(est);
                }
                card.addView(row);
            }
            c.addView(card);
        }

        c.addView(Ui.button(a, "Refaire cette séance", Ui.SECONDARY,
                v -> a.startSession(s.name, s.exercises, 0, -1)));
        return Ui.page(a, c);
    }

    @Override
    public void onAction() {
        Session s = st.session(id);
        if (s == null) return;
        String[] items = {"Refaire cette séance", "Enregistrer comme modèle", "Supprimer la séance"};
        new AlertDialog.Builder(a)
                .setItems(items, (d, w) -> {
                    if (w == 0) {
                        a.startSession(s.name, s.exercises, 0, -1);
                    } else if (w == 1) {
                        Ui.prompt(a, "Nom du modèle", s.name, "Nom", n -> {
                            st.saveTemplate(n, s.exercises);
                            Ui.toast(a, "Modèle enregistré : retrouve-le dans Programmes.");
                        });
                    } else {
                        Ui.confirm(a, "Supprimer cette séance ?", "Elle sera retirée de l'historique et des records.",
                                "Supprimer", () -> {
                                    st.deleteSession(s);
                                    a.back();
                                });
                    }
                })
                .show();
    }
}

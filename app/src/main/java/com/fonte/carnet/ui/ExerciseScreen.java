package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Exercise;
import com.fonte.carnet.core.ExerciseEntry;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Session;
import com.fonte.carnet.core.SetEntry;
import com.fonte.carnet.core.Stats;

import java.util.ArrayList;
import java.util.List;

/** Fiche d'un exercice : records, courbe de progression, historique. */
public class ExerciseScreen extends Screen {
    private final long id;

    public ExerciseScreen(long id) {
        this.id = id;
    }

    @Override
    public String title() {
        return st.name(id);
    }

    @Override
    public int actionIcon() {
        Exercise e = st.exercise(id);
        return e != null && e.custom ? Icon.MORE : -1;
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        Exercise ex = st.exercise(id);
        c.addView(Ui.heading(a, st.name(id)));
        if (ex != null && ex.category != null) c.addView(Ui.text(a, ex.category, 14, Ui.MUTED));

        Stats.Record r = Stats.records(st.sessions).get(id);
        if (r == null) {
            TextView t = Ui.text(a, "Pas encore de séance avec cet exercice.", 15, Ui.MUTED);
            t.setPadding(0, Ui.dp(16), 0, 0);
            c.addView(t);
            return Ui.page(a, c);
        }
        c.addView(Ui.space(a, 12));
        if (r.bodyweight()) {
            c.addView(Ui.statRow(a,
                    Ui.stat(a, String.valueOf(r.maxReps), "reps max · " + Fmt.shortDay(r.maxRepsDate)),
                    Ui.stat(a, String.valueOf(r.sessions), Fmt.plural(r.sessions, "séance").replaceFirst("^\\d+ ", ""))));
        } else {
            c.addView(Ui.statRow(a,
                    Ui.stat(a, Fmt.kg(Stats.round1(r.bestE1rm)) + " kg", "1RM estimé"),
                    Ui.stat(a, Fmt.kg(r.maxWeight) + " × " + r.maxWeightReps, "charge max (kg)")));
            c.addView(Ui.statRow(a,
                    Ui.stat(a, String.valueOf(r.maxReps), "reps max en une série"),
                    Ui.stat(a, String.valueOf(r.sessions), Fmt.plural(r.sessions, "séance").replaceFirst("^\\d+ ", ""))));
            if (r.e1rmReps > 0) {
                TextView how = Ui.text(a, "1RM estimé d'après " + Fmt.kg(r.e1rmWeight) + " kg × " + r.e1rmReps
                        + " le " + Fmt.shortDay(r.e1rmDate) + " (formule d'Epley).", 12, Ui.MUTED);
                how.setPadding(Ui.dp(2), 0, 0, Ui.dp(4));
                c.addView(how);
            }
        }

        c.addView(Ui.section(a, r.bodyweight() ? "Progression · reps max" : "Progression · 1RM estimé"));
        LinearLayout chartCard = Ui.card(a);
        ChartView chart = new ChartView(a);
        chart.setData(Stats.progress(id, st.sessions, r.bodyweight()), r.bodyweight() ? "reps" : "kg");
        chartCard.addView(chart, Ui.lp(-1, Ui.dp(190)));
        c.addView(chartCard);

        c.addView(Ui.section(a, "Historique"));
        int shown = 0;
        for (Session s : st.sessions) {
            if (shown >= 40) break;
            List<SetEntry> sets = new ArrayList<>();
            for (ExerciseEntry e : s.exercises) if (e.exerciseId == id) for (SetEntry x : e.sets) if (x.done) sets.add(x);
            if (sets.isEmpty()) continue;
            shown++;
            LinearLayout card = Ui.clickCard(a, v -> a.show(new SessionDetailScreen(s.id)));
            card.addView(Ui.medium(a, Fmt.day(s.start), 14, Ui.TEXT));
            TextView sum = Ui.text(a, Stats.summarize(sets), 14, Ui.MUTED);
            Ui.tabular(sum);
            card.addView(sum);
            c.addView(card);
        }
        return Ui.page(a, c);
    }

    @Override
    public void onAction() {
        Exercise e = st.exercise(id);
        if (e == null || !e.custom) return;
        String[] items = {"Renommer", "Supprimer"};
        new AlertDialog.Builder(a)
                .setTitle(e.name)
                .setItems(items, (d, w) -> {
                    if (w == 0) {
                        Ui.prompt(a, "Renommer l'exercice", e.name, "Nom", n -> {
                            Exercise other = st.findByName(n);
                            if (other != null && other != e) {
                                Ui.toast(a, "Un exercice porte déjà ce nom.");
                                return;
                            }
                            e.name = n;
                            st.save();
                            a.refresh();
                            a.updateHeader();
                        });
                    } else if (st.isUsed(e.id)) {
                        Ui.info(a, "Suppression impossible",
                                "Cet exercice est utilisé dans tes séances, modèles ou programme. Supprime-les d'abord.");
                    } else {
                        Ui.confirm(a, "Supprimer « " + e.name + " » ?", "L'exercice sera retiré de la bibliothèque.",
                                "Supprimer", () -> {
                                    st.deleteExercise(e);
                                    a.back();
                                });
                    }
                })
                .show();
    }
}

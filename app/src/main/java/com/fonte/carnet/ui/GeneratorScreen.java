package com.fonte.carnet.ui;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.ExerciseEntry;
import com.fonte.carnet.core.Generator;
import com.fonte.carnet.core.Library;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Propose une séance selon les muscles choisis, l'objectif et le temps disponible. */
public class GeneratorScreen extends Screen {
    private final Set<String> muscles = new LinkedHashSet<>();
    private String goal = Generator.HYPERTROPHY;
    private int minutes = 45;
    private long seed = System.nanoTime();
    private List<ExerciseEntry> proposal;

    @Override
    public String title() {
        return "Séance par muscle";
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        TextView s = Ui.section(a, "Muscles à travailler");
        s.setLayoutParams(Ui.mlp(-1, -2, 4, 4, 4, 8));
        c.addView(s);
        View[] mc = new View[Library.MUSCLES.length];
        for (int i = 0; i < Library.MUSCLES.length; i++) {
            String m = Library.MUSCLES[i];
            TextView chip = Ui.chip(a, m, muscles.contains(m), v -> {
                if (!muscles.remove(m)) muscles.add(m);
                proposal = null;
                a.refresh();
            });
            chip.setGravity(Gravity.CENTER);
            mc[i] = chip;
        }
        c.addView(Ui.grid(a, 2, mc));

        c.addView(Ui.section(a, "Objectif"));
        String[] goalLabels = {"Force · 5×5", "Volume · 4×8 à 3×12", "Endurance · 3×15"};
        View[] gc = new View[Generator.GOALS.length];
        for (int i = 0; i < Generator.GOALS.length; i++) {
            String g = Generator.GOALS[i];
            gc[i] = Ui.chip(a, goalLabels[i], g.equals(goal), v -> {
                goal = g;
                if (proposal != null) proposal = Generator.propose(new ArrayList<>(muscles), goal, minutes, seed);
                a.refresh();
            });
        }
        c.addView(Ui.chipRow(a, gc));

        c.addView(Ui.section(a, "Temps disponible"));
        View[] dc = new View[Generator.DURATIONS.length];
        for (int i = 0; i < Generator.DURATIONS.length; i++) {
            int d = Generator.DURATIONS[i];
            dc[i] = Ui.chip(a, d + " min", d == minutes, v -> {
                minutes = d;
                if (proposal != null) proposal = Generator.propose(new ArrayList<>(muscles), goal, minutes, seed);
                a.refresh();
            });
        }
        c.addView(Ui.chipRow(a, dc));

        c.addView(Ui.button(a, proposal == null ? "Proposer une séance" : "Autre proposition", Ui.PRIMARY, v -> {
            if (muscles.isEmpty()) {
                Ui.toast(a, "Choisis au moins un muscle.");
                return;
            }
            seed = System.nanoTime();
            proposal = Generator.propose(new ArrayList<>(muscles), goal, minutes, seed);
            a.refresh();
        }));

        if (proposal != null && !proposal.isEmpty()) {
            String name = "Séance " + Generator.title(new ArrayList<>(muscles));
            c.addView(Ui.section(a, "Ta séance"));
            LinearLayout card = Ui.card(a);
            card.addView(Ui.medium(a, name, 17, Ui.TEXT));
            for (ExerciseEntry e : proposal) {
                LinearLayout line = Ui.hbox(a);
                line.setPadding(0, Ui.dp(8), 0, 0);
                line.addView(Ui.text(a, st.name(e.exerciseId), 16, Ui.TEXT), Ui.weight(1));
                int reps = e.sets.isEmpty() ? 0 : e.sets.get(0).reps;
                TextView sc = Ui.medium(a, e.sets.size() + "×" + reps, 15, Ui.ACCENT);
                Ui.tabular(sc);
                line.addView(sc);
                card.addView(line);
            }
            TextView hint = Ui.text(a, "Polyarticulaires en premier. Repos conseillé : " + rest() + ". "
                    + "Tu pourras ajouter, retirer ou changer des exercices pendant la séance.", 13, Ui.MUTED);
            hint.setPadding(0, Ui.dp(10), 0, 0);
            card.addView(hint);
            c.addView(card);
            c.addView(Ui.button(a, "Démarrer cette séance", Ui.PRIMARY, v -> a.startSession(name, proposal, 0, -1)));
            c.addView(Ui.button(a, "Enregistrer comme modèle", Ui.GHOST, v -> Ui.prompt(a, "Nom du modèle", name, "Nom", n -> {
                st.saveTemplate(n, proposal);
                Ui.toast(a, "Modèle enregistré dans Séances types.");
            })));
        }
        return Ui.page(a, c);
    }

    private String rest() {
        if (Generator.STRENGTH.equals(goal)) return "2 à 3 min";
        if (Generator.ENDURANCE.equals(goal)) return "45 s à 1 min";
        return "1 min 30 à 2 min";
    }
}

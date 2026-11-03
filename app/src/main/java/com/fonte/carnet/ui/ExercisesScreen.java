package com.fonte.carnet.ui;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Exercise;
import com.fonte.carnet.core.Library;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bibliothèque complète des exercices. */
public class ExercisesScreen extends Screen {

    @Override
    public String title() {
        return "Exercices";
    }

    @Override
    public int actionIcon() {
        return Icon.PLUS;
    }

    @Override
    public void onAction() {
        Ui.prompt(a, "Nouvel exercice", "", "Nom de l'exercice", n -> {
            if (st.findByName(n) != null) {
                Ui.toast(a, "Cet exercice existe déjà.");
                return;
            }
            st.addExercise(n);
            a.refresh();
        });
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        for (String cat : Library.CATEGORIES) {
            List<Exercise> list = new ArrayList<>();
            for (Exercise e : st.exercises) if (cat.equals(e.category)) list.add(e);
            if (list.isEmpty()) continue;
            Collections.sort(list, (x, y) -> x.name.compareToIgnoreCase(y.name));
            c.addView(Ui.section(a, cat));
            LinearLayout card = Ui.card(a);
            card.setPadding(Ui.dp(4), Ui.dp(4), Ui.dp(4), Ui.dp(4));
            for (Exercise e : list) {
                TextView row = Ui.text(a, e.name, 16, Ui.TEXT);
                row.setPadding(Ui.dp(12), Ui.dp(12), Ui.dp(12), Ui.dp(12));
                row.setBackground(Ui.pressable(Ui.rect(0x00000000, 12), 12));
                row.setOnClickListener(v -> a.show(new ExerciseScreen(e.id)));
                card.addView(row);
            }
            c.addView(card);
        }
        TextView hint = Ui.text(a, "Touche + pour ajouter un exercice. Tu peux aussi le créer directement pendant une séance.",
                13, Ui.MUTED);
        hint.setPadding(0, Ui.dp(8), 0, 0);
        c.addView(hint);
        return Ui.page(a, c);
    }
}

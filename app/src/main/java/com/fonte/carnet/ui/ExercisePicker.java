package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fonte.carnet.core.Exercise;
import com.fonte.carnet.core.Library;
import com.fonte.carnet.data.Store;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Boîte de dialogue de choix d'un exercice, avec recherche et création. */
final class ExercisePicker {
    private final MainActivity a;
    private final Store st;
    private final Consumer<Exercise> callback;
    private final LinearLayout list;
    private final EditText search;
    private final AlertDialog dialog;

    static void show(MainActivity a, Consumer<Exercise> callback) {
        new ExercisePicker(a, callback);
    }

    private ExercisePicker(MainActivity a, Consumer<Exercise> callback) {
        this.a = a;
        this.st = a.store;
        this.callback = callback;

        LinearLayout root = Ui.vbox(a);
        root.setPadding(Ui.dp(16), Ui.dp(8), Ui.dp(16), 0);
        search = Ui.field(a, "Rechercher ou créer…", Ui.TEXT_FIELD);
        root.addView(search, Ui.lp(-1, -2));
        ScrollView sv = new ScrollView(a);
        list = Ui.vbox(a);
        list.setPadding(0, Ui.dp(4), 0, Ui.dp(8));
        sv.addView(list);
        root.addView(sv, Ui.mlp(-1, Ui.dp(360), 0, 8, 0, 0));

        dialog = new AlertDialog.Builder(a)
                .setTitle("Choisir un exercice")
                .setView(root)
                .setNegativeButton("Fermer", null)
                .create();
        Ui.onText(search, t -> fill());
        fill();
        if (dialog.getWindow() != null) {
            // Clavier fermé à l'ouverture : on voit la liste ; il s'ouvre si on touche la recherche.
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
        dialog.show();
    }

    private static String norm(String s) {
        String n = Normalizer.normalize(s.toLowerCase(Locale.FRENCH), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").trim();
    }

    private void pick(Exercise e) {
        Ui.hideKeyboard(search);
        dialog.dismiss();
        callback.accept(e);
    }

    private void fill() {
        list.removeAllViews();
        String raw = search.getText().toString().trim();
        String q = norm(raw);
        if (!raw.isEmpty() && st.findByName(raw) == null) {
            TextView create = row("+ Créer « " + raw + " »", Ui.ACCENT);
            create.setOnClickListener(v -> pick(st.addExercise(raw)));
            list.addView(create);
        }
        int shown = 0;
        for (String cat : Library.CATEGORIES) {
            List<Exercise> match = new ArrayList<>();
            for (Exercise e : st.exercises) {
                if (cat.equals(e.category) && (q.isEmpty() || norm(e.name).contains(q))) match.add(e);
            }
            if (match.isEmpty()) continue;
            Collections.sort(match, (x, y) -> norm(x.name).compareTo(norm(y.name)));
            TextView head = Ui.section(a, cat);
            head.setLayoutParams(Ui.mlp(-1, -2, 4, 14, 4, 4));
            list.addView(head);
            for (Exercise e : match) {
                TextView r = row(e.name, Ui.TEXT);
                r.setOnClickListener(v -> pick(e));
                list.addView(r);
                shown++;
            }
        }
        // Exercices dont la catégorie n'est pas connue (sauvegarde importée, par exemple).
        for (Exercise e : st.exercises) {
            boolean known = false;
            for (String cat : Library.CATEGORIES) if (cat.equals(e.category)) known = true;
            if (!known && (q.isEmpty() || norm(e.name).contains(q))) {
                TextView r = row(e.name, Ui.TEXT);
                r.setOnClickListener(v -> pick(e));
                list.addView(r);
                shown++;
            }
        }
        if (shown == 0 && raw.isEmpty()) list.addView(Ui.text(a, "Aucun exercice.", 15, Ui.MUTED));
    }

    private TextView row(String label, int color) {
        TextView t = Ui.text(a, label, 16, color);
        t.setPadding(Ui.dp(8), Ui.dp(12), Ui.dp(8), Ui.dp(12));
        t.setBackground(Ui.pressable(Ui.rect(0x00000000, 10), 10));
        return t;
    }
}

package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fonte.carnet.core.Exercise;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Library;
import com.fonte.carnet.core.Program;
import com.fonte.carnet.core.Programs;
import com.fonte.carnet.core.Stats;
import com.fonte.carnet.data.Store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Boîtes de dialogue de configuration des programmes à pourcentages. */
final class ProgramSetup {
    private ProgramSetup() {
    }

    /** Max proposé par défaut : meilleur 1RM estimé de l'historique, arrondi. */
    private static String suggestedMax(Store st, long exerciseId) {
        Map<Long, Stats.Record> recs = Stats.records(st.sessions);
        Stats.Record r = recs.get(exerciseId);
        if (r == null || r.bestE1rm <= 0) return "";
        return Fmt.kg(Programs.round(r.bestE1rm, Math.max(0.5, st.settings.rounding)));
    }

    private static TextView note(MainActivity a, String s) {
        TextView t = Ui.text(a, s, 13, Ui.MUTED);
        t.setPadding(Ui.dp(2), Ui.dp(4), Ui.dp(2), Ui.dp(8));
        return t;
    }

    private static ScrollView wrap(MainActivity a, LinearLayout box) {
        ScrollView sv = new ScrollView(a);
        box.setPadding(Ui.dp(20), Ui.dp(8), Ui.dp(20), Ui.dp(8));
        sv.addView(box);
        return sv;
    }

    private static double positive(EditText e) {
        double v = Fmt.parse(e.getText().toString());
        return Double.isNaN(v) ? -1 : v;
    }

    /** Remplace le programme en cours (après confirmation) puis l'affiche. */
    private static void apply(MainActivity a, Program p) {
        Store st = a.store;
        Runnable go = () -> {
            st.setProgram(p);
            a.goTab(MainActivity.TAB_PROGRAMS);
            a.show(new ProgramScreen());
        };
        if (st.program != null && !st.program.finished()) {
            Ui.confirm(a, "Remplacer le programme en cours ?",
                    "« " + st.program.title + " » sera arrêté. Tes séances déjà faites restent dans l'historique.",
                    "Remplacer", go);
        } else {
            go.run();
        }
    }

    static void smolovJr(MainActivity a) {
        Store st = a.store;
        final long[] exId = {Library.BENCH};
        LinearLayout box = Ui.vbox(a);
        box.addView(note(a, "3 semaines, 4 séances par semaine sur un seul mouvement : 6×6 à 70 %, 7×5 à 75 %, "
                + "8×4 à 80 % et 10×3 à 85 % de ton max, avec une charge qui monte chaque semaine."));

        TextView exButton = Ui.button(a, st.name(exId[0]), Ui.SECONDARY, null);
        EditText max = Ui.field(a, "ex. 100", Ui.DECIMAL);
        max.setText(suggestedMax(st, exId[0]));
        exButton.setOnClickListener(v -> ExercisePicker.show(a, ex -> {
            exId[0] = ex.id;
            exButton.setText(ex.name);
            max.setText(suggestedMax(st, ex.id));
        }));
        box.addView(Ui.labelled(a, "Mouvement", exButton));
        box.addView(Ui.labelled(a, "Ton max actuel sur 1 répétition (kg)", max));
        EditText inc2 = Ui.field(a, "2,5", Ui.DECIMAL);
        inc2.setText("2,5");
        EditText inc3 = Ui.field(a, "5", Ui.DECIMAL);
        inc3.setText("5");
        LinearLayout incs = Ui.hbox(a);
        LinearLayout l2 = Ui.labelled(a, "Ajout semaine 2 (kg)", inc2);
        LinearLayout l3 = Ui.labelled(a, "Ajout semaine 3 (kg)", inc3);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, -2, 1);
        p2.rightMargin = Ui.dp(10);
        incs.addView(l2, p2);
        incs.addView(l3, new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(incs);
        box.addView(note(a, "Repère courant : +2,5 à 5 kg en semaine 2, +5 à 10 kg en semaine 3. Programme très exigeant : "
                + "pas pour les débutants, toujours avec un pareur ou des sécurités."));

        AlertDialog d = new AlertDialog.Builder(a)
                .setTitle("Smolov Jr")
                .setView(wrap(a, box))
                .setPositiveButton("Créer le programme", null)
                .setNegativeButton("Annuler", null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            double m = positive(max);
            double i2 = positive(inc2);
            double i3 = positive(inc3);
            if (m <= 0 || m > 1000) {
                max.setError("Indique ton max en kg");
                return;
            }
            if (i2 < 0 || i2 > 50) {
                inc2.setError("Valeur entre 0 et 50");
                return;
            }
            if (i3 < 0 || i3 > 50) {
                inc3.setError("Valeur entre 0 et 50");
                return;
            }
            Exercise ex = st.exercise(exId[0]);
            if (ex == null) return;
            Ui.hideKeyboard(max);
            d.dismiss();
            apply(a, Programs.smolovJr(ex, m, i2, i3, st.settings.rounding, System.currentTimeMillis()));
        }));
        d.show();
    }

    static void wendler(MainActivity a) {
        Store st = a.store;
        long[] ids = {Library.OHP, Library.DEADLIFT, Library.BENCH, Library.SQUAT};
        LinearLayout box = Ui.vbox(a);
        box.addView(note(a, "4 semaines, une séance par mouvement. Les charges sont calculées sur 90 % de ton max ; "
                + "la dernière série des semaines 1 à 3 se fait au maximum de répétitions propres. "
                + "Laisse vide un mouvement pour l'ignorer."));
        EditText[] fields = new EditText[ids.length];
        for (int i = 0; i < ids.length; i++) {
            fields[i] = Ui.field(a, "max en kg", Ui.DECIMAL);
            fields[i].setText(suggestedMax(st, ids[i]));
            box.addView(Ui.labelled(a, st.name(ids[i]) + " : max sur 1 rép. (kg)", fields[i]));
        }
        CheckBox bbb = new CheckBox(a);
        bbb.setText("Ajouter 5×10 à 50 % (« Boring But Big »)");
        bbb.setTextColor(Ui.TEXT);
        bbb.setTextSize(15);
        box.addView(bbb, Ui.mlp(-1, -2, 0, 4, 0, 8));

        AlertDialog d = new AlertDialog.Builder(a)
                .setTitle("5/3/1 Wendler")
                .setView(wrap(a, box))
                .setPositiveButton("Créer le programme", null)
                .setNegativeButton("Annuler", null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            double[] maxes = new double[ids.length];
            List<Exercise> lifts = new ArrayList<>();
            for (int i = 0; i < ids.length; i++) {
                String raw = fields[i].getText().toString().trim();
                double m = raw.isEmpty() ? 0 : positive(fields[i]);
                if (m < 0 || m > 1000) {
                    fields[i].setError("Valeur invalide");
                    return;
                }
                maxes[i] = m;
                Exercise ex = st.exercise(ids[i]);
                lifts.add(ex != null ? ex : new Exercise(ids[i], st.name(ids[i]), Library.PUSH, false));
            }
            Program p = Programs.wendler(lifts, maxes, bbb.isChecked(), st.settings.rounding, System.currentTimeMillis());
            if (p == null) {
                fields[0].setError("Indique au moins un max");
                return;
            }
            Ui.hideKeyboard(fields[0]);
            d.dismiss();
            apply(a, p);
        }));
        d.show();
    }

    /** Relance le même type de programme (après un cycle terminé). */
    static void restart(MainActivity a, Program p) {
        if (p != null && Programs.WENDLER.equals(p.type)) wendler(a);
        else smolovJr(a);
    }
}

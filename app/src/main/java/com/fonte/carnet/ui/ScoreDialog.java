package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Wod;
import com.fonte.carnet.core.WodResult;
import com.fonte.carnet.data.Store;

/** Saisie d'un score de WOD (temps, tours + reps, reps ou simplement « fait »). */
final class ScoreDialog {
    private ScoreDialog() {
    }

    /**
     * @param seconds temps proposé (For Time), -1 si inconnu
     * @param rounds  tours proposés (AMRAP), -1 si inconnu
     */
    static void show(MainActivity a, Wod w, int seconds, int rounds, Runnable onSaved) {
        Store st = a.store;
        String type = w.scoreType();
        LinearLayout box = Ui.vbox(a);
        box.setPadding(Ui.dp(20), Ui.dp(8), Ui.dp(20), Ui.dp(4));
        TextView fmt = Ui.text(a, w.formatLabel(), 14, Ui.MUTED);
        fmt.setPadding(0, 0, 0, Ui.dp(8));
        box.addView(fmt);

        EditText min = null;
        EditText sec = null;
        EditText roundsField = null;
        EditText repsField = null;
        CheckBox capped = null;
        if (WodResult.TIME.equals(type)) {
            LinearLayout row = Ui.hbox(a);
            min = Ui.field(a, "min", Ui.INTEGER);
            sec = Ui.field(a, "s", Ui.INTEGER);
            if (seconds >= 0) {
                min.setText(String.valueOf(seconds / 60));
                sec.setText(String.valueOf(seconds % 60));
            }
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
            p.rightMargin = Ui.dp(10);
            row.addView(Ui.labelled(a, "Minutes", min), p);
            row.addView(Ui.labelled(a, "Secondes", sec), new LinearLayout.LayoutParams(0, -2, 1));
            box.addView(row);
            capped = new CheckBox(a);
            capped.setText("Cap atteint avant la fin");
            capped.setTextColor(Ui.TEXT);
            box.addView(capped);
            repsField = Ui.field(a, "répétitions faites", Ui.INTEGER);
            box.addView(Ui.labelled(a, "Si cap atteint : répétitions faites", repsField));
        } else if (WodResult.ROUNDS.equals(type)) {
            LinearLayout row = Ui.hbox(a);
            roundsField = Ui.field(a, "tours", Ui.INTEGER);
            if (rounds >= 0) roundsField.setText(String.valueOf(rounds));
            repsField = Ui.field(a, "reps", Ui.INTEGER);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
            p.rightMargin = Ui.dp(10);
            row.addView(Ui.labelled(a, "Tours complets", roundsField), p);
            row.addView(Ui.labelled(a, "+ répétitions", repsField), new LinearLayout.LayoutParams(0, -2, 1));
            box.addView(row);
        } else if (WodResult.REPS.equals(type)) {
            repsField = Ui.field(a, "total", Ui.INTEGER);
            box.addView(Ui.labelled(a, "Total de répétitions", repsField));
        }
        CheckBox rx = new CheckBox(a);
        rx.setText("RX (charges et standards prescrits)");
        rx.setTextColor(Ui.TEXT);
        rx.setChecked(true);
        box.addView(rx);
        EditText note = Ui.field(a, "charges utilisées, ressenti…", Ui.TEXT_FIELD);
        box.addView(Ui.labelled(a, "Note (facultatif)", note));

        ScrollView sv = new ScrollView(a);
        sv.addView(box);
        AlertDialog d = new AlertDialog.Builder(a)
                .setTitle("Score · " + w.name)
                .setView(sv)
                .setPositiveButton("Enregistrer", null)
                .setNegativeButton("Annuler", null)
                .create();
        final EditText fMin = min;
        final EditText fSec = sec;
        final EditText fRounds = roundsField;
        final EditText fReps = repsField;
        final CheckBox fCapped = capped;
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            WodResult r = new WodResult();
            r.wodId = w.id;
            r.wodName = w.name;
            r.date = System.currentTimeMillis();
            r.type = type;
            r.rx = rx.isChecked();
            String n = note.getText().toString().trim();
            r.note = n.isEmpty() ? null : n;
            if (WodResult.TIME.equals(type)) {
                r.capped = fCapped != null && fCapped.isChecked();
                if (r.capped) {
                    r.reps = Fmt.parseInt(fReps.getText().toString());
                    r.seconds = w.minutes * 60;
                } else {
                    r.seconds = Fmt.parseInt(fMin.getText().toString()) * 60 + Fmt.parseInt(fSec.getText().toString());
                    if (r.seconds <= 0) {
                        fMin.setError("Indique ton temps");
                        return;
                    }
                }
            } else if (WodResult.ROUNDS.equals(type)) {
                r.rounds = Fmt.parseInt(fRounds.getText().toString());
                r.reps = Fmt.parseInt(fReps.getText().toString());
                if (r.rounds <= 0 && r.reps <= 0) {
                    fRounds.setError("Indique tes tours");
                    return;
                }
            } else if (WodResult.REPS.equals(type)) {
                r.reps = Fmt.parseInt(fReps.getText().toString());
                if (r.reps <= 0) {
                    fReps.setError("Indique le total");
                    return;
                }
            }
            Ui.hideKeyboard(note);
            d.dismiss();
            boolean record = st.addWodResult(r);
            Ui.toast(a, record ? "🏆 Nouveau record sur " + w.name + " : " + r.score() : "Score enregistré : " + r.score());
            if (onSaved != null) onSaved.run();
        }));
        d.show();
    }
}

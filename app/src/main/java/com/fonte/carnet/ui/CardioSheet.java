package com.fonte.carnet.ui;

import android.app.Dialog;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.BodyLog;
import com.fonte.carnet.core.Cardio;
import com.fonte.carnet.core.Days;
import com.fonte.carnet.core.Fmt;

import java.util.ArrayList;
import java.util.List;

/** Saisie d'un cardio ou d'un ergo à la main (vélo 20 min, rameur 2 000 m…), dans une séance ou seul. */
final class CardioSheet {
    private CardioSheet() {
    }

    /**
     * Nouveau cardio.
     *
     * @param sport     Cardio.MUSCU, CROSSFIT ou RENFO
     * @param sessionId séance de musculation en cours (0 = cardio seul)
     */
    static void add(MainActivity a, String sport, long sessionId, Runnable done) {
        BodyLog.Activity act = new BodyLog.Activity();
        act.kind = "velo_appart";
        act.intensity = 1;
        act.minutes = 20;
        act.sport = sport;
        act.sessionId = sessionId;
        if (sessionId != 0) {
            com.fonte.carnet.core.Session s = a.store.current != null && a.store.current.id == sessionId ? a.store.current
                    : a.store.session(sessionId);
            if (s != null) {
                act.day = Days.key(s.start);
                if (!s.inProgress()) act.time = s.end;
            }
        }
        open(a, act, true, done);
    }

    static void edit(MainActivity a, BodyLog.Activity act, Runnable done) {
        open(a, act, false, done);
    }

    private static void open(MainActivity a, BodyLog.Activity act, boolean isNew, Runnable done) {
        double weight = a.store.weightForCalc();
        String[] kind = {act.kind == null ? "autre" : act.kind};
        int[] intensity = {act.intensity < 0 ? 1 : act.intensity};

        LinearLayout box = Ui.vbox(a);
        TextView title = Ui.text(a, isNew ? "Cardio / ergo" : act.name, 19, Ui.TEXT);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        box.addView(title);
        TextView sub = Ui.text(a, "Noté à la main, " + (act.sessionId != 0 ? "dans ta séance." : "dans " + Cardio.sportLabel(act.sport) + "."),
                13, Ui.MUTED);
        sub.setPadding(0, Ui.dp(2), 0, Ui.dp(10));
        box.addView(sub);

        LinearLayout chips = Ui.vbox(a);
        box.addView(chips);
        EditText other = Ui.field(a, "Nom (battle rope, stepper…)", Ui.TEXT_FIELD);
        if (isNew || !"autre".equals(act.kind)) other.setText("");
        else other.setText(act.name);
        LinearLayout otherBox = Ui.labelled(a, "Activité", other);
        otherBox.setPadding(0, Ui.dp(8), 0, 0);
        box.addView(otherBox);

        TextView il = Ui.text(a, "Intensité", 13, Ui.MUTED);
        il.setPadding(0, Ui.dp(12), 0, Ui.dp(6));
        box.addView(il);
        LinearLayout segBox = Ui.vbox(a);
        box.addView(segBox);

        EditText minutes = Ui.field(a, "20", Ui.INTEGER);
        minutes.setText(String.valueOf(act.minutes > 0 ? act.minutes : 20));
        LinearLayout minBox = Ui.labelled(a, "Durée (minutes)", minutes);
        minBox.setPadding(0, Ui.dp(10), 0, 0);
        box.addView(minBox);

        EditText dist = Ui.field(a, "facultatif", Ui.DECIMAL);
        TextView distLabel = Ui.text(a, "", 13, Ui.MUTED);
        LinearLayout distBox = Ui.vbox(a);
        distBox.setPadding(0, Ui.dp(10), 0, 0);
        distBox.addView(distLabel);
        distBox.addView(dist, Ui.mlp(-1, -2, 0, 4, 0, 0));
        box.addView(distBox);
        if (act.meters > 0) {
            dist.setText("m".equals(Cardio.unit(kind[0])) ? String.valueOf(act.meters) : Fmt.kg(act.meters / 1000.0));
        }

        EditText shown = Ui.field(a, "facultatif", Ui.INTEGER);
        if (act.shownKcal > 0) shown.setText(String.valueOf(act.shownKcal));
        LinearLayout shownBox = Ui.labelled(a, "Calories affichées par l'ergo ou la montre", shown);
        shownBox.setPadding(0, Ui.dp(10), 0, 0);
        box.addView(shownBox);

        TextView live = Ui.medium(a, "", 15, Ui.ACCENT);
        live.setPadding(0, Ui.dp(12), 0, 0);
        box.addView(live);
        TextView note = Ui.text(a, "", 12, Ui.FAINT);
        note.setPadding(0, Ui.dp(2), 0, 0);
        box.addView(note);

        Runnable update = () -> {
            int m = Fmt.parseInt(minutes.getText().toString());
            int s = Fmt.parseInt(shown.getText().toString());
            int k = Cardio.netKcal(kind[0], intensity[0], m, s, weight);
            live.setText(m > 0 ? "≈ " + k + " kcal nettes" : "Indique la durée");
            note.setText(s > 0 ? "Calories de l'ergo, moins le repos déjà compté dans ton métabolisme."
                    : "Estimées d'après l'intensité et ton poids (" + Fmt.kg(weight) + " kg). Les calories de l'ergo sont plus justes.");
        };
        Runnable[] paint = new Runnable[1];
        paint[0] = () -> {
            chips.removeAllViews();
            List<View> list = new ArrayList<>();
            for (int i = 0; i < Cardio.IDS.length; i++) {
                String id = Cardio.IDS[i];
                list.add(Ui.toggleChip(a, Cardio.NAMES[i], id.equals(kind[0]), v -> {
                    kind[0] = id;
                    paint[0].run();
                }));
            }
            chips.addView(Ui.flow(a, list.toArray(new View[0])));
            int idx = Cardio.index(kind[0]);
            if (!Cardio.HINTS[idx].isEmpty()) {
                TextView h = Ui.text(a, Cardio.HINTS[idx], 12, Ui.FAINT);
                h.setPadding(0, Ui.dp(6), 0, 0);
                chips.addView(h);
            }
            otherBox.setVisibility("autre".equals(kind[0]) ? View.VISIBLE : View.GONE);
            String unit = Cardio.unit(kind[0]);
            distBox.setVisibility(unit == null ? View.GONE : View.VISIBLE);
            if (unit != null) distLabel.setText("Distance en " + unit + " (facultatif)");
            segBox.removeAllViews();
            segBox.addView(Ui.segmented(a, Cardio.INTENSITIES, intensity[0], i -> {
                intensity[0] = i;
                paint[0].run();
            }));
            update.run();
        };
        paint[0].run();
        Ui.onText(minutes, t -> update.run());
        Ui.onText(shown, t -> update.run());

        TextView cancel = Ui.button(a, "Annuler", Ui.GHOST, null);
        TextView ok = Ui.button(a, isNew ? "Ajouter" : "Enregistrer", Ui.PRIMARY, null);
        LinearLayout buttons = Ui.hbox(a);
        buttons.setPadding(0, Ui.dp(14), 0, 0);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, -2, 1);
        l1.rightMargin = Ui.dp(8);
        buttons.addView(cancel, l1);
        buttons.addView(ok, new LinearLayout.LayoutParams(0, -2, 2));
        box.addView(buttons);
        TextView delete = null;
        if (!isNew) {
            delete = Ui.button(a, "Supprimer", Ui.DANGER_GHOST, null);
            delete.setLayoutParams(Ui.mlp(-1, -2, 0, 8, 0, 0));
            box.addView(delete);
        }

        Dialog d = Ui.sheet(a, box);
        if (d.getWindow() != null) d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        cancel.setOnClickListener(v -> {
            Ui.hideKeyboard(minutes);
            d.dismiss();
        });
        if (delete != null) {
            delete.setOnClickListener(v -> {
                Ui.hideKeyboard(minutes);
                d.dismiss();
                Ui.confirm(a, "Supprimer ce cardio ?", act.name + " · " + Cardio.summary(act), "Supprimer", () -> {
                    a.store.deleteActivity(act);
                    if (done != null) done.run();
                });
            });
        }
        ok.setOnClickListener(v -> {
            int m = Fmt.parseInt(minutes.getText().toString());
            if (m <= 0 || m > 600) {
                minutes.setError("Entre 1 et 600 min");
                return;
            }
            String name = Cardio.name(kind[0]);
            if ("autre".equals(kind[0])) {
                String typed = other.getText().toString().trim();
                if (!typed.isEmpty()) name = Fmt.cap(typed);
            }
            act.kind = kind[0];
            act.name = name;
            act.intensity = intensity[0];
            act.minutes = m;
            act.meters = Cardio.unit(kind[0]) == null ? 0 : Cardio.meters(kind[0], Fmt.parse(dist.getText().toString()));
            act.shownKcal = Math.max(0, Fmt.parseInt(shown.getText().toString()));
            act.kcal = Cardio.netKcal(act.kind, act.intensity, m, act.shownKcal, weight);
            a.store.addCardio(act);
            Ui.hideKeyboard(minutes);
            d.dismiss();
            if (isNew) Ui.toast(a, name + " · " + m + " min ajouté");
            if (done != null) done.run();
        });
        d.show();
    }

    /** Ligne d'un cardio noté (nom, détail) ; toucher pour le modifier. */
    static View row(MainActivity a, BodyLog.Activity act, boolean withDate, Runnable done) {
        LinearLayout card = Ui.clickCard(a, v -> edit(a, act, done));
        LinearLayout top = Ui.hbox(a);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(Ui.medium(a, act.name, 16, Ui.TEXT), Ui.weight(1));
        top.addView(Ui.badge(a, "Cardio", Ui.ACCENT, Ui.ACCENT_SOFT));
        card.addView(top);
        TextView t = Ui.text(a, (withDate ? Fmt.day(act.time) + " · " : "") + Cardio.summary(act), 13, Ui.MUTED);
        t.setPadding(0, Ui.dp(2), 0, 0);
        card.addView(t);
        return card;
    }
}

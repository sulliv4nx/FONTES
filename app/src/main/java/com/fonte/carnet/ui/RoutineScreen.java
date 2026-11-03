package com.fonte.carnet.ui;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Clock;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Routine;
import com.fonte.carnet.core.Routines;

/** Détail d'un circuit de renfo et lancement guidé. */
public class RoutineScreen extends Screen {
    private final String id;

    public RoutineScreen(String id) {
        this.id = id;
    }

    @Override
    public String title() {
        Routine r = Routines.find(id);
        return r == null ? "Renfo" : r.name;
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        Routine r = Routines.find(id);
        if (r == null) {
            c.addView(Ui.text(a, "Circuit introuvable.", 15, Ui.MUTED));
            return Ui.page(a, c);
        }
        c.addView(Ui.heading(a, r.name));
        TextView d = Ui.text(a, r.description, 14, Ui.MUTED);
        d.setPadding(0, Ui.dp(4), 0, Ui.dp(10));
        c.addView(d);
        c.addView(Ui.statRow(a,
                Ui.stat(a, "≈ " + r.estimatedMinutes() + " min", "durée"),
                Ui.stat(a, String.valueOf(r.rounds), r.rounds > 1 ? "tours" : "tour"),
                Ui.stat(a, r.rest + " s", "pause")));

        LinearLayout card = Ui.card(a);
        int i = 0;
        for (Routine.Move m : r.moves) {
            if (i++ > 0) card.addView(Ui.divider(a));
            LinearLayout line = Ui.hbox(a);
            line.addView(Ui.medium(a, m.name, 16, Ui.TEXT), Ui.weight(1));
            TextView amount = Ui.medium(a, m.seconds > 0 ? Fmt.clock(m.seconds * 1000L) : m.reps + " reps", 15, Ui.ACCENT);
            Ui.tabular(amount);
            line.addView(amount);
            card.addView(line);
            if (m.cue != null && !m.cue.isEmpty()) {
                TextView cue = Ui.text(a, m.cue, 13, Ui.MUTED);
                cue.setPadding(0, Ui.dp(2), 0, 0);
                card.addView(cue);
            }
        }
        c.addView(card);
        if (r.rounds > 1 && r.roundRest > 0) {
            TextView t = Ui.text(a, r.roundRest + " s de récupération entre les tours.", 13, Ui.MUTED);
            t.setPadding(0, 0, 0, Ui.dp(8));
            c.addView(t);
        }
        c.addView(Ui.button(a, "Commencer", Ui.PRIMARY, v -> {
            ClockRunner.start(a, st.settings, Clock.circuit(r, st.settings.prepSeconds), null, r.id);
            a.show(new ClockScreen());
            a.refreshKeepScreenOn();
        }));
        TextView safe = Ui.text(a, "Les exercices en répétitions attendent que tu touches « Fait ✓ ». Arrête-toi en cas de douleur.", 12, Ui.FAINT);
        safe.setPadding(0, Ui.dp(4), 0, 0);
        c.addView(safe);
        return Ui.page(a, c);
    }
}

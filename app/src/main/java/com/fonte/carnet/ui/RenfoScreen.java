package com.fonte.carnet.ui;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.RenfoLog;
import com.fonte.carnet.core.Routine;
import com.fonte.carnet.core.Routines;

/** Accueil renforcement musculaire : circuits guidés par zone. */
public class RenfoScreen extends Screen {

    @Override
    public String title() {
        return "Renforcement";
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        View running = CrossFitScreen.runningCard(a);
        if (running != null) c.addView(running);
        TextView intro = Ui.text(a, "Circuits guidés au poids du corps (élastique léger pour les épaules) : "
                + "l'appli enchaîne les exercices, les temps et les pauses pour toi.", 14, Ui.MUTED);
        intro.setPadding(Ui.dp(4), Ui.dp(4), Ui.dp(4), Ui.dp(4));
        c.addView(intro);

        for (String focus : Routines.FOCUS) {
            c.addView(Ui.section(a, focus));
            for (Routine r : Routines.byFocus(focus)) c.addView(routineCard(r));
        }

        if (!st.renfoLogs.isEmpty()) {
            c.addView(Ui.section(a, "Dernières séances"));
            int n = 0;
            for (RenfoLog l : st.renfoLogs) {
                if (n++ >= 5) break;
                LinearLayout row = Ui.card(a);
                LinearLayout line = Ui.hbox(a);
                LinearLayout txt = Ui.vbox(a);
                txt.addView(Ui.medium(a, l.name, 15, Ui.TEXT));
                txt.addView(Ui.text(a, Fmt.day(l.date), 13, Ui.MUTED));
                line.addView(txt, Ui.weight(1));
                line.addView(Ui.text(a, Fmt.clock(l.seconds * 1000L), 15, Ui.MUTED));
                row.addView(line);
                c.addView(row);
            }
        }
        return Ui.page(a, c);
    }

    private View routineCard(Routine r) {
        LinearLayout card = Ui.clickCard(a, v -> a.show(new RoutineScreen(r.id)));
        LinearLayout top = Ui.hbox(a);
        top.addView(Ui.medium(a, r.name, 17, Ui.TEXT), Ui.weight(1));
        top.addView(Ui.badge(a, "≈ " + r.estimatedMinutes() + " min", Ui.ACCENT, Ui.ACCENT_SOFT));
        card.addView(top);
        TextView d = Ui.text(a, r.description, 13, Ui.MUTED);
        d.setPadding(0, Ui.dp(4), 0, 0);
        card.addView(d);
        return card;
    }
}

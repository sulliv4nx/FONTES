package com.fonte.carnet.ui;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Program;
import com.fonte.carnet.core.Stats;
import com.fonte.carnet.core.Template;

import java.util.ArrayList;

/** Accueil de l'onglet Séance quand aucune séance n'est en cours. */
public class HomeScreen extends Screen {

    @Override
    public String title() {
        return "Fonte";
    }

    @Override
    public int actionIcon() {
        return Icon.GEAR;
    }

    @Override
    public void onAction() {
        a.show(new SettingsScreen());
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        long now = System.currentTimeMillis();
        c.addView(Ui.text(a, Fmt.day(now), 14, Ui.MUTED));
        TextView h = Ui.heading(a, "Séance du jour");
        h.setPadding(0, Ui.dp(2), 0, Ui.dp(12));
        c.addView(h);

        Program p = st.program;
        if (p != null) {
            LinearLayout card = Ui.card(a);
            card.addView(Ui.medium(a, "PROGRAMME EN COURS", 11, Ui.ACCENT));
            TextView t = Ui.medium(a, p.title, 18, Ui.TEXT);
            t.setPadding(0, Ui.dp(4), 0, 0);
            card.addView(t);
            card.addView(Ui.progress(a, p.doneCount(), p.days.size()));
            card.addView(Ui.text(a, p.doneCount() + " / " + Fmt.plural(p.days.size(), "séance"), 13, Ui.MUTED));
            int next = p.nextIndex();
            if (next >= 0) {
                Program.Day d = p.days.get(next);
                TextView nt = Ui.text(a, "Prochaine : " + d.group + " · " + d.name, 15, Ui.TEXT);
                nt.setPadding(0, Ui.dp(10), 0, Ui.dp(6));
                card.addView(nt);
                card.addView(Ui.button(a, "Commencer", Ui.PRIMARY, v -> a.startProgramDay(next)));
            } else {
                TextView done = Ui.text(a, "Programme terminé, bravo ! Mets ton max à jour et relance un cycle.", 15, Ui.TEXT);
                done.setPadding(0, Ui.dp(10), 0, Ui.dp(6));
                card.addView(done);
            }
            card.addView(Ui.button(a, "Voir le programme", Ui.GHOST, v -> {
                a.goTab(MainActivity.TAB_PROGRAMS);
                a.show(new ProgramScreen());
            }));
            c.addView(card);
        }

        c.addView(Ui.button(a, "Séance libre", p == null ? Ui.PRIMARY : Ui.SECONDARY,
                v -> a.startSession("Séance libre", new ArrayList<>(), 0, -1)));

        if (!st.templates.isEmpty()) {
            c.addView(Ui.section(a, "Mes modèles"));
            for (Template t : st.templates) {
                LinearLayout row = Ui.clickCard(a, v -> a.startSession(t.name, t.exercises, 0, -1));
                LinearLayout line = Ui.hbox(a);
                LinearLayout txt = Ui.vbox(a);
                txt.addView(Ui.medium(a, t.name, 16, Ui.TEXT));
                txt.addView(Ui.text(a, Fmt.plural(t.exercises.size(), "exercice"), 13, Ui.MUTED));
                line.addView(txt, Ui.weight(1));
                android.widget.ImageView go = new android.widget.ImageView(a);
                go.setImageDrawable(new Icon(Icon.CHEVRON, Ui.FAINT));
                line.addView(go, Ui.lp(Ui.dp(20), Ui.dp(20)));
                row.addView(line);
                c.addView(row);
            }
        }

        c.addView(Ui.section(a, "Cette semaine"));
        double[] week = Stats.thisWeek(st.sessions, now);
        c.addView(Ui.statRow(a,
                Ui.stat(a, String.valueOf((int) week[0]), Fmt.plural((int) week[0], "séance").replaceFirst("^\\d+ ", "")),
                Ui.stat(a, Fmt.tonnage(week[1]), "soulevés"),
                Ui.stat(a, String.valueOf(st.sessions.size()), "au total")));

        if (st.sessions.isEmpty() && p == null) {
            LinearLayout tip = Ui.card(a);
            tip.addView(Ui.medium(a, "Bienvenue !", 16, Ui.TEXT));
            TextView tt = Ui.text(a, "Lance une séance libre pour noter tes séries, ou choisis un programme "
                    + "(Smolov Jr, 5/3/1…) dans l'onglet Programmes. Tes records se mettent à jour tout seuls.", 14, Ui.MUTED);
            tt.setPadding(0, Ui.dp(4), 0, 0);
            tip.addView(tt);
            c.addView(tip);
        }
        return Ui.page(a, c);
    }
}

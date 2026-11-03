package com.fonte.carnet.ui;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Program;
import com.fonte.carnet.core.Stats;
import com.fonte.carnet.core.Templates;

import java.util.ArrayList;

/** Accueil Muscu (aucune séance en cours) : programme, séance libre, séances types, proposition, coach. */
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
        TextView h = Ui.heading(a, "Musculation");
        h.setPadding(0, Ui.dp(2), 0, Ui.dp(12));
        c.addView(h);

        View running = CrossFitScreen.runningCard(a);
        if (running != null) c.addView(running);

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
            card.addView(Ui.button(a, "Voir le programme", Ui.GHOST, v -> a.show(new ProgramScreen())));
            c.addView(card);
        }

        c.addView(Ui.button(a, "Séance libre", p == null ? Ui.PRIMARY : Ui.SECONDARY,
                v -> a.startSession("Séance libre", new ArrayList<>(), 0, -1)));

        c.addView(Ui.section(a, "Préparer ma séance"));
        c.addView(Ui.grid(a, 2,
                Ui.tile(a, "Par muscle", "Une séance proposée selon tes choix", v -> a.show(new GeneratorScreen())),
                Ui.tile(a, "Séances types", Templates.all().size() + " séances + tes modèles", v -> a.show(new TemplatesScreen())),
                Ui.tile(a, "Prog. du coach", "Scanne ou colle ta programmation", v -> a.show(new CoachScreen())),
                Ui.tile(a, "Programmes", "Smolov Jr, 5/3/1", v -> a.show(new ProgramsScreen())),
                Ui.tile(a, "Chrono", "Repos, intervalles, EMOM…", v -> a.show(new TimerSetupScreen(TimerSetupScreen.INTERVALS)))));

        c.addView(Ui.section(a, "Cette semaine"));
        double[] week = Stats.thisWeek(st.sessions, now);
        int wods = 0;
        int renfo = 0;
        long monday = now - weekOffset(now);
        for (com.fonte.carnet.core.WodResult r : st.wodResults) if (r.date >= monday) wods++;
        for (com.fonte.carnet.core.RenfoLog l : st.renfoLogs) if (l.date >= monday) renfo++;
        c.addView(Ui.statRow(a,
                Ui.stat(a, String.valueOf((int) week[0]), "muscu"),
                Ui.stat(a, String.valueOf(wods), "WOD"),
                Ui.stat(a, String.valueOf(renfo), "renfo")));
        TextView ton = Ui.text(a, Fmt.tonnage(week[1]) + " soulevés en musculation cette semaine.", 13, Ui.MUTED);
        ton.setPadding(Ui.dp(4), 0, Ui.dp(4), 0);
        c.addView(ton);

        if (st.sessions.isEmpty() && st.wodResults.isEmpty() && p == null) {
            LinearLayout tip = Ui.card(a);
            tip.addView(Ui.medium(a, "Bienvenue !", 16, Ui.TEXT));
            TextView tt = Ui.text(a, "Musculation ici, WOD et chrono dans CrossFit, circuits guidés dans Renfo. "
                    + "Tes records se mettent à jour tout seuls.", 14, Ui.MUTED);
            tt.setPadding(0, Ui.dp(4), 0, 0);
            tip.addView(tt);
            c.addView(tip);
        }
        return Ui.page(a, c);
    }

    /** Millisecondes écoulées depuis lundi 0 h. */
    private static long weekOffset(long now) {
        java.util.Calendar cal = java.util.Calendar.getInstance(java.util.Locale.FRANCE);
        cal.setTimeInMillis(now);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        int back = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7;
        cal.add(java.util.Calendar.DAY_OF_MONTH, -back);
        return now - cal.getTimeInMillis();
    }
}

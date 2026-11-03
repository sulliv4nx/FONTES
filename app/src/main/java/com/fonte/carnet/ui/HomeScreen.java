package com.fonte.carnet.ui;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Program;
import com.fonte.carnet.core.Session;
import com.fonte.carnet.core.Stats;
import com.fonte.carnet.core.Templates;

import java.util.ArrayList;

/** Segment Muscu de l'onglet Sport : séance en cours, programme, séance libre et préparation. */
public class HomeScreen extends Screen {

    @Override
    public String title() {
        return "Musculation";
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);

        Session cur = st.current;
        if (cur != null) {
            LinearLayout card = Ui.clickCard(a, v -> a.show(new SessionScreen()));
            card.setBackground(Ui.pressable(Ui.outline(Ui.ACCENT_SOFT, Ui.ACCENT, 18), 18));
            card.addView(Ui.cardHeader(a, Icon.BARBELL, Ui.ACCENT, "Séance en cours", null, null));
            card.addView(Ui.medium(a, cur.name, 16, Ui.TEXT));
            card.addView(Ui.text(a, Fmt.plural(cur.doneSets(), "série") + " validée" + (cur.doneSets() > 1 ? "s" : "")
                    + " · " + Fmt.duration(cur.durationMs()), 13, Ui.MUTED));
            card.addView(Ui.button(a, "Reprendre la séance", Ui.PRIMARY, v -> a.show(new SessionScreen())));
            c.addView(card);
        }
        View running = CrossFitScreen.runningCard(a);
        if (running != null) c.addView(running);

        Program p = st.program;
        if (p != null) {
            LinearLayout card = Ui.card(a);
            card.addView(Ui.cardHeader(a, Icon.PROGRAM, Ui.ACCENT, p.title, "Détail", v -> a.show(new ProgramScreen())));
            card.addView(Ui.progress(a, p.doneCount(), p.days.size()));
            card.addView(Ui.text(a, p.doneCount() + " / " + Fmt.plural(p.days.size(), "séance") + " faites", 13, Ui.MUTED));
            int next = p.nextIndex();
            if (next >= 0) {
                Program.Day d = p.days.get(next);
                TextView nt = Ui.medium(a, "Prochaine : " + d.group + " · " + d.name, 15, Ui.TEXT);
                nt.setPadding(0, Ui.dp(10), 0, Ui.dp(4));
                card.addView(nt);
                card.addView(Ui.button(a, "Commencer", Ui.PRIMARY, v -> a.startProgramDay(next)));
            } else {
                TextView done = Ui.text(a, "Programme terminé, bravo ! Mets ton max à jour et relance un cycle.", 14, Ui.TEXT);
                done.setPadding(0, Ui.dp(10), 0, 0);
                card.addView(done);
            }
            c.addView(card);
        }

        if (cur == null) {
            c.addView(Ui.button(a, "Démarrer une séance libre", p == null ? Ui.PRIMARY : Ui.SECONDARY,
                    v -> a.startSession("Séance libre", new ArrayList<>(), 0, -1)));
        }

        c.addView(Ui.section(a, "Préparer ma séance"));
        c.addView(Ui.grid(a, 2,
                Ui.tile(a, Icon.SPARK, "Par muscle", "Séance proposée selon tes choix", v -> a.show(new GeneratorScreen())),
                Ui.tile(a, Icon.HISTORY, "Séances types", Templates.all().size() + " séances et tes modèles", v -> a.show(new TemplatesScreen())),
                Ui.tile(a, Icon.CAMERA, "Programmation du coach", "Photo ou texte collé", v -> a.show(new CoachScreen())),
                Ui.tile(a, Icon.PROGRAM, "Programmes", "Smolov Jr, 5/3/1", v -> a.show(new ProgramsScreen())),
                Ui.tile(a, Icon.TIMER, "Chrono", "Repos, intervalles, EMOM", v -> a.show(new TimerSetupScreen(TimerSetupScreen.INTERVALS))),
                Ui.tile(a, Icon.TROPHY, "Calculateur de max", "1RM et pourcentages", v -> a.show(new CalculatorScreen()))));

        c.addView(Ui.section(a, "Cette semaine"));
        double[] week = Stats.thisWeek(st.sessions, System.currentTimeMillis());
        c.addView(Ui.statRow(a,
                Ui.stat(a, String.valueOf((int) week[0]), Fmt.plural((int) week[0], "séance").replaceFirst("^\\d+ ", "")),
                Ui.stat(a, Fmt.tonnage(week[1]), "soulevés"),
                Ui.stat(a, String.valueOf(st.sessions.size()), "au total")));
        return Ui.page(a, c);
    }
}

package com.fonte.carnet.ui;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Wod;
import com.fonte.carnet.core.WodResult;
import com.fonte.carnet.core.Wods;

/** Accueil CrossFit : chrono, WOD Heroes, Girls, WOD par objectif, mes WOD et derniers scores. */
public class CrossFitScreen extends Screen {

    @Override
    public String title() {
        return "CrossFit";
    }

    @Override
    public int actionIcon() {
        return Icon.PLUS;
    }

    @Override
    public void onAction() {
        a.show(new WodEditScreen(null));
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        View running = runningCard(a);
        if (running != null) c.addView(running);

        TextView s1 = Ui.section(a, "Chrono");
        s1.setLayoutParams(Ui.mlp(-1, -2, 0, 4, 0, 8));
        c.addView(s1);
        c.addView(Ui.grid(a, 2,
                Ui.tile(a, Icon.TIMER, "AMRAP", "Max de tours", v -> a.show(new TimerSetupScreen(TimerSetupScreen.AMRAP))),
                Ui.tile(a, Icon.TIMER, "EMOM", "Chaque minute", v -> a.show(new TimerSetupScreen(TimerSetupScreen.EMOM))),
                Ui.tile(a, Icon.TIMER, "For Time", "Chrono + cap", v -> a.show(new TimerSetupScreen(TimerSetupScreen.FOR_TIME))),
                Ui.tile(a, Icon.TIMER, "Tabata", "20 s / 10 s", v -> a.show(new TimerSetupScreen(TimerSetupScreen.TABATA))),
                Ui.tile(a, Icon.TIMER, "Intervalles", "Travail / repos", v -> a.show(new TimerSetupScreen(TimerSetupScreen.INTERVALS)))));

        c.addView(Ui.section(a, "WOD de référence"));
        c.addView(Ui.grid(a, 2,
                Ui.tile(a, Icon.TROPHY, "WOD Heroes", Wods.byKind(Wod.HERO).size() + " WOD · Murph, DT…",
                        v -> a.show(new WodListScreen(Wod.HERO, null))),
                Ui.tile(a, Icon.KETTLEBELL, "The Girls", Wods.byKind(Wod.GIRL).size() + " WOD · Fran, Cindy…",
                        v -> a.show(new WodListScreen(Wod.GIRL, null)))));

        c.addView(Ui.section(a, "Selon ce que tu veux travailler"));
        View[] tags = new View[Wods.TAGS.length];
        for (int i = 0; i < Wods.TAGS.length; i++) {
            String tag = Wods.TAGS[i];
            int n = Wods.byTag(tag).size();
            for (Wod w : st.customWods) if (w.tags.contains(tag)) n++;
            tags[i] = Ui.tile(a, Icon.PULSE, tag, Fmt.plural(n, "WOD"), v -> a.show(new WodListScreen(null, tag)));
        }
        c.addView(Ui.grid(a, 2, tags));

        c.addView(Ui.section(a, "Mes WOD"));
        if (st.customWods.isEmpty()) {
            TextView t = Ui.text(a, "Crée tes WOD (ou ceux de ta box) pour les relancer avec le bon chrono et suivre tes scores.", 14, Ui.MUTED);
            t.setPadding(0, 0, 0, Ui.dp(4));
            c.addView(t);
        } else {
            for (Wod w : st.customWods) c.addView(WodListScreen.wodCard(this, w));
        }
        c.addView(Ui.button(a, "+ Créer un WOD", Ui.SECONDARY, v -> a.show(new WodEditScreen(null))));

        if (!st.wodResults.isEmpty()) {
            c.addView(Ui.section(a, "Derniers scores"));
            int n = 0;
            for (WodResult r : st.wodResults) {
                if (n++ >= 5) break;
                LinearLayout card = Ui.clickCard(a, v -> {
                    if (st.wod(r.wodId) != null) a.show(new WodScreen(r.wodId));
                });
                LinearLayout line = Ui.hbox(a);
                LinearLayout txt = Ui.vbox(a);
                txt.addView(Ui.medium(a, r.wodName, 16, Ui.TEXT));
                txt.addView(Ui.text(a, Fmt.day(r.date), 13, Ui.MUTED));
                line.addView(txt, Ui.weight(1));
                TextView score = Ui.medium(a, r.score(), 16, Ui.ACCENT);
                Ui.tabular(score);
                line.addView(score);
                card.addView(line);
                c.addView(card);
            }
        }
        return Ui.page(a, c);
    }

    /** Carte « chrono en cours » (ou null si aucun chrono). */
    static View runningCard(MainActivity a) {
        ClockRunner r = ClockRunner.active();
        if (r == null) return null;
        LinearLayout card = Ui.clickCard(a, v -> a.show(new ClockScreen()));
        card.setBackground(Ui.pressable(Ui.outline(Ui.ACCENT_SOFT, Ui.ACCENT, 16), 16));
        card.addView(Ui.medium(a, r.clock.finished() ? "Chrono terminé" : "Chrono en cours", 12, Ui.ACCENT));
        card.addView(Ui.medium(a, r.clock.title, 17, Ui.TEXT));
        card.addView(Ui.text(a, "Touche pour y revenir", 13, Ui.MUTED));
        return card;
    }
}

package com.fonte.carnet.ui;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Exercise;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Library;
import com.fonte.carnet.core.Stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Records par exercice + accès au calculateur et à la bibliothèque. */
public class RecordsScreen extends Screen {

    @Override
    public String title() {
        return "Records";
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        c.addView(linkCard("Calculateur de max", "1RM estimé et tableau des pourcentages",
                v -> a.show(new CalculatorScreen())));
        c.addView(linkCard("Bibliothèque d'exercices", Fmt.plural(st.exercises.size(), "exercice") + " · ajoute les tiens",
                v -> a.show(new ExercisesScreen())));

        addWodRecords(c);

        Map<Long, Stats.Record> recs = Stats.records(st.sessions);
        if (recs.isEmpty()) {
            TextView t = Ui.text(a, "Tes records de musculation apparaîtront ici après ta première séance.", 15, Ui.MUTED);
            t.setPadding(0, Ui.dp(16), 0, 0);
            c.addView(t);
            return Ui.page(a, c);
        }
        List<String> cats = new ArrayList<>();
        for (String cat : Library.CATEGORIES) cats.add(cat);
        for (String cat : cats) {
            List<Exercise> list = new ArrayList<>();
            for (Exercise e : st.exercises) if (cat.equals(e.category) && recs.containsKey(e.id)) list.add(e);
            if (list.isEmpty()) continue;
            Collections.sort(list, (x, y) -> x.name.compareToIgnoreCase(y.name));
            c.addView(Ui.section(a, cat));
            for (Exercise e : list) c.addView(recordCard(e, recs.get(e.id)));
        }
        // Exercices hors catégorie connue ou supprimés.
        for (Map.Entry<Long, Stats.Record> en : recs.entrySet()) {
            Exercise e = st.exercise(en.getKey());
            if (e == null || !cats.contains(e.category)) {
                Exercise shown = e != null ? e : new Exercise(en.getKey(), st.name(en.getKey()), "", false);
                c.addView(recordCard(shown, en.getValue()));
            }
        }
        return Ui.page(a, c);
    }

    /** Meilleur score de chaque WOD déjà fait. */
    private void addWodRecords(LinearLayout c) {
        java.util.LinkedHashMap<String, com.fonte.carnet.core.WodResult> best = new java.util.LinkedHashMap<>();
        for (com.fonte.carnet.core.WodResult r : st.wodResults) {
            if (best.containsKey(r.wodId)) continue;
            com.fonte.carnet.core.Wod w = st.wod(r.wodId);
            String type = w != null ? w.scoreType() : r.type;
            com.fonte.carnet.core.WodResult b = com.fonte.carnet.core.WodResult.best(st.wodResults, r.wodId, type);
            if (b != null) best.put(r.wodId, b);
        }
        if (best.isEmpty()) return;
        c.addView(Ui.section(a, "WOD"));
        for (com.fonte.carnet.core.WodResult b : best.values()) {
            LinearLayout card = Ui.clickCard(a, v -> {
                if (st.wod(b.wodId) != null) a.show(new WodScreen(b.wodId));
            });
            LinearLayout line = Ui.hbox(a);
            LinearLayout txt = Ui.vbox(a);
            txt.addView(Ui.medium(a, b.wodName, 16, Ui.TEXT));
            txt.addView(Ui.text(a, Fmt.shortDay(b.date) + (b.rx ? " · RX" : " · adapté"), 12, Ui.MUTED));
            line.addView(txt, Ui.weight(1));
            TextView sc = Ui.medium(a, b.score(), 16, Ui.ACCENT);
            Ui.tabular(sc);
            line.addView(sc);
            card.addView(line);
            c.addView(card);
        }
        c.addView(Ui.section(a, "Musculation"));
    }

    private View linkCard(String title, String sub, View.OnClickListener l) {
        LinearLayout card = Ui.clickCard(a, l);
        LinearLayout line = Ui.hbox(a);
        LinearLayout txt = Ui.vbox(a);
        txt.addView(Ui.medium(a, title, 16, Ui.TEXT));
        txt.addView(Ui.text(a, sub, 13, Ui.MUTED));
        line.addView(txt, Ui.weight(1));
        ImageView go = new ImageView(a);
        go.setImageDrawable(new Icon(Icon.CHEVRON, Ui.FAINT));
        line.addView(go, Ui.lp(Ui.dp(20), Ui.dp(20)));
        card.addView(line);
        return card;
    }

    static String headline(Stats.Record r) {
        if (r.bodyweight()) return r.maxReps + " répétitions max";
        StringBuilder sb = new StringBuilder();
        if (r.bestE1rm > 0) sb.append("1RM estimé ").append(Fmt.kg(Stats.round1(r.bestE1rm))).append(" kg · ");
        sb.append("max ").append(Fmt.kg(r.maxWeight)).append(" kg × ").append(r.maxWeightReps);
        return sb.toString();
    }

    private View recordCard(Exercise e, Stats.Record r) {
        LinearLayout card = Ui.clickCard(a, v -> a.show(new ExerciseScreen(e.id)));
        card.addView(Ui.medium(a, e.name, 16, Ui.TEXT));
        TextView h = Ui.medium(a, headline(r), 14, Ui.ACCENT);
        Ui.tabular(h);
        h.setPadding(0, Ui.dp(2), 0, 0);
        card.addView(h);
        card.addView(Ui.text(a, Fmt.plural(r.sessions, "séance") + " · dernière le " + Fmt.shortDay(r.lastDate), 12, Ui.MUTED));
        return card;
    }
}

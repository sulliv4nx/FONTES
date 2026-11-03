package com.fonte.carnet.ui;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Session;

/** Liste des séances terminées, groupées par mois. */
public class HistoryScreen extends Screen {
    private static final int PAGE = 60;
    private int limit = PAGE;

    @Override
    public String title() {
        return "Historique";
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        if (st.sessions.isEmpty()) {
            TextView t = Ui.text(a, "Aucune séance pour l'instant. Tes séances terminées apparaîtront ici.", 15, Ui.MUTED);
            t.setPadding(Ui.dp(4), Ui.dp(12), Ui.dp(4), 0);
            c.addView(t);
            return Ui.page(a, c);
        }
        String month = null;
        int n = Math.min(limit, st.sessions.size());
        for (int i = 0; i < n; i++) {
            Session s = st.sessions.get(i);
            String m = Fmt.month(s.start);
            if (!m.equals(month)) {
                TextView sec = Ui.section(a, m);
                if (month == null) sec.setLayoutParams(Ui.mlp(-1, -2, 4, 8, 4, 8));
                c.addView(sec);
                month = m;
            }
            c.addView(sessionCard(s));
        }
        if (st.sessions.size() > limit) {
            c.addView(Ui.button(a, "Afficher plus", Ui.GHOST, v -> {
                limit += PAGE;
                a.refresh();
            }));
        }
        return Ui.page(a, c);
    }

    private View sessionCard(Session s) {
        LinearLayout card = Ui.clickCard(a, v -> a.show(new SessionDetailScreen(s.id)));
        LinearLayout top = Ui.hbox(a);
        TextView name = Ui.medium(a, s.name, 16, Ui.TEXT);
        top.addView(name, Ui.weight(1));
        if (!s.records.isEmpty()) {
            top.addView(Ui.badge(a, Fmt.plural(s.records.size(), "record"), Ui.ACCENT, Ui.ACCENT_SOFT));
        }
        card.addView(top);
        card.addView(Ui.text(a, Fmt.day(s.start), 13, Ui.MUTED));
        TextView stats = Ui.text(a, Fmt.duration(s.durationMs()) + " · "
                + Fmt.plural(s.exercises.size(), "exercice") + " · "
                + Fmt.plural(s.doneSets(), "série") + " · "
                + Fmt.tonnage(s.tonnage()), 13, Ui.MUTED);
        stats.setPadding(0, Ui.dp(2), 0, 0);
        card.addView(stats);
        return card;
    }
}

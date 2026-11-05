package com.fonte.carnet.ui;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.RenfoLog;
import com.fonte.carnet.core.Session;
import com.fonte.carnet.core.WodResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Historique unifié : séances de musculation, scores de WOD et circuits de renfo, groupés par mois. */
public class HistoryScreen extends Screen {
    private static final int PAGE = 60;
    private static final String ALL = "Tout";
    private static final String MUSCU = "Muscu";
    private static final String WOD = "CrossFit";
    private static final String RENFO = "Renfo";
    private int limit = PAGE;
    private String filter = ALL;

    private static class Entry {
        long date;
        Object item;

        Entry(long date, Object item) {
            this.date = date;
            this.item = item;
        }
    }

    @Override
    public String title() {
        return "Historique";
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        String[] filters = {ALL, MUSCU, WOD, RENFO};
        View[] chips = new View[filters.length];
        for (int i = 0; i < filters.length; i++) {
            String f = filters[i];
            chips[i] = Ui.chip(a, f, f.equals(filter), v -> {
                filter = f;
                limit = PAGE;
                a.refresh();
            });
        }
        c.addView(Ui.chipRow(a, chips));

        List<Entry> entries = new ArrayList<>();
        if (ALL.equals(filter) || MUSCU.equals(filter)) for (Session s : st.sessions) entries.add(new Entry(s.start, s));
        if (ALL.equals(filter) || WOD.equals(filter)) for (WodResult r : st.wodResults) entries.add(new Entry(r.date, r));
        if (ALL.equals(filter) || RENFO.equals(filter)) for (RenfoLog l : st.renfoLogs) entries.add(new Entry(l.date, l));
        for (com.fonte.carnet.core.BodyLog.Activity x : st.cardioAlone(null)) {
            String sp = com.fonte.carnet.core.Cardio.CROSSFIT.equals(x.sport) ? WOD
                    : com.fonte.carnet.core.Cardio.RENFO.equals(x.sport) ? RENFO : MUSCU;
            if (ALL.equals(filter) || sp.equals(filter)) entries.add(new Entry(x.time, x));
        }
        Collections.sort(entries, (x, y) -> Long.compare(y.date, x.date));

        if (entries.isEmpty()) {
            TextView t = Ui.text(a, "Rien pour l'instant. Tes séances, WOD, circuits et cardios apparaîtront ici.", 15, Ui.MUTED);
            t.setPadding(0, Ui.dp(12), 0, 0);
            c.addView(t);
            return Ui.page(a, c);
        }
        String month = null;
        int n = Math.min(limit, entries.size());
        for (int i = 0; i < n; i++) {
            Entry e = entries.get(i);
            String m = Fmt.month(e.date);
            if (!m.equals(month)) {
                c.addView(Ui.section(a, m));
                month = m;
            }
            if (e.item instanceof Session) c.addView(sessionCard((Session) e.item));
            else if (e.item instanceof WodResult) c.addView(wodCard((WodResult) e.item));
            else if (e.item instanceof com.fonte.carnet.core.BodyLog.Activity)
                c.addView(CardioSheet.row(a, (com.fonte.carnet.core.BodyLog.Activity) e.item, true, a::refresh));
            else c.addView(renfoCard((RenfoLog) e.item));
        }
        if (entries.size() > limit) {
            c.addView(Ui.button(a, "Afficher plus", Ui.GHOST, v -> {
                limit += PAGE;
                a.refresh();
            }));
        }
        return Ui.page(a, c);
    }

    private LinearLayout header(LinearLayout card, String name, String badge, int color, int bg) {
        LinearLayout top = Ui.hbox(a);
        top.addView(Ui.medium(a, name, 16, Ui.TEXT), Ui.weight(1));
        if (badge != null) top.addView(Ui.badge(a, badge, color, bg));
        card.addView(top);
        return top;
    }

    private View sessionCard(Session s) {
        LinearLayout card = Ui.clickCard(a, v -> a.show(new SessionDetailScreen(s.id)));
        header(card, s.name, s.records.isEmpty() ? "Muscu" : Fmt.plural(s.records.size(), "record"),
                s.records.isEmpty() ? Ui.MUTED : Ui.ACCENT, s.records.isEmpty() ? Ui.SURFACE2 : Ui.ACCENT_SOFT);
        card.addView(Ui.text(a, Fmt.day(s.start), 13, Ui.MUTED));
        int cardio = 0;
        for (com.fonte.carnet.core.BodyLog.Activity x : st.cardio(s.id)) cardio += x.minutes;
        String detail = s.exercises.isEmpty() ? "" : " · " + Fmt.plural(s.exercises.size(), "exercice") + " · "
                + Fmt.plural(s.doneSets(), "série") + " · " + Fmt.tonnage(s.tonnage());
        TextView stats = Ui.text(a, Fmt.duration(s.durationMs()) + detail + (cardio > 0 ? " · cardio " + cardio + " min" : ""),
                13, Ui.MUTED);
        stats.setPadding(0, Ui.dp(2), 0, 0);
        card.addView(stats);
        return card;
    }

    private View wodCard(WodResult r) {
        LinearLayout card = Ui.clickCard(a, v -> {
            if (st.wod(r.wodId) != null) a.show(new WodScreen(r.wodId));
            else Ui.info(a, r.wodName, r.fullScore() + (r.note != null ? "\n" + r.note : ""));
        });
        card.setOnLongClickListener(v -> {
            Ui.confirm(a, "Supprimer ce score ?", r.wodName + " · " + r.fullScore(), "Supprimer", () -> {
                st.deleteWodResult(r);
                a.refresh();
            });
            return true;
        });
        header(card, r.wodName, "WOD", Ui.ACCENT, Ui.ACCENT_SOFT);
        card.addView(Ui.text(a, Fmt.day(r.date), 13, Ui.MUTED));
        TextView sc = Ui.medium(a, r.fullScore(), 14, Ui.TEXT);
        Ui.tabular(sc);
        sc.setPadding(0, Ui.dp(2), 0, 0);
        card.addView(sc);
        return card;
    }

    private View renfoCard(RenfoLog l) {
        LinearLayout card = Ui.clickCard(a, v -> {
            if (com.fonte.carnet.core.Routines.find(l.routineId) != null) a.show(new RoutineScreen(l.routineId));
        });
        card.setOnLongClickListener(v -> {
            Ui.confirm(a, "Supprimer ce circuit ?", l.name + " · " + Fmt.day(l.date), "Supprimer", () -> {
                st.deleteRenfo(l);
                a.refresh();
            });
            return true;
        });
        header(card, l.name, "Renfo", Ui.OK, Ui.OK_SOFT);
        card.addView(Ui.text(a, Fmt.day(l.date) + " · " + Fmt.clock(l.seconds * 1000L), 13, Ui.MUTED));
        return card;
    }
}

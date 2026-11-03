package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Clock;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Wod;
import com.fonte.carnet.core.WodResult;

import java.util.List;

/** Fiche d'un WOD : déroulé, chrono adapté, scores. */
public class WodScreen extends Screen {
    private final String id;

    public WodScreen(String id) {
        this.id = id;
    }

    @Override
    public String title() {
        Wod w = st.wod(id);
        return w == null ? "WOD" : w.name;
    }

    @Override
    public int actionIcon() {
        Wod w = st.wod(id);
        return w != null && Wod.CUSTOM.equals(w.kind) ? Icon.MORE : -1;
    }

    @Override
    public void onAction() {
        Wod w = st.wod(id);
        if (w == null || !Wod.CUSTOM.equals(w.kind)) return;
        String[] items = {"Modifier", "Supprimer"};
        new AlertDialog.Builder(a)
                .setTitle(w.name)
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        a.show(new WodEditScreen(w));
                    } else {
                        Ui.confirm(a, "Supprimer « " + w.name + " » ?", "Tes scores déjà enregistrés restent dans l'historique.",
                                "Supprimer", () -> {
                                    st.deleteCustomWod(w);
                                    a.back();
                                });
                    }
                })
                .show();
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        Wod w = st.wod(id);
        if (w == null) {
            c.addView(Ui.text(a, "Ce WOD n'existe plus.", 15, Ui.MUTED));
            return Ui.page(a, c);
        }
        c.addView(Ui.heading(a, w.name));
        LinearLayout meta = Ui.hbox(a);
        meta.setPadding(0, Ui.dp(6), 0, Ui.dp(12));
        meta.addView(Ui.badge(a, w.formatLabel(), Ui.BG, Ui.ACCENT));
        TextView kind = Ui.text(a, "  " + w.kindLabel(), 14, Ui.MUTED);
        meta.addView(kind);
        c.addView(meta);

        LinearLayout card = Ui.card(a);
        for (String l : w.lines) {
            boolean header = l.endsWith(":");
            boolean hint = l.startsWith("(");
            TextView t = header ? Ui.medium(a, l, 16, Ui.ACCENT) : Ui.text(a, l, hint ? 14 : 17, hint ? Ui.MUTED : Ui.TEXT);
            t.setPadding(0, Ui.dp(4), 0, Ui.dp(4));
            card.addView(t);
        }
        c.addView(card);
        if (w.note != null && !w.note.isEmpty()) {
            TextView n = Ui.text(a, w.note, 13, Ui.MUTED);
            n.setPadding(0, 0, 0, Ui.dp(8));
            c.addView(n);
        }
        if (!w.tags.isEmpty()) {
            View[] tags = new View[w.tags.size()];
            for (int i = 0; i < tags.length; i++) {
                String tag = w.tags.get(i);
                tags[i] = Ui.chip(a, tag, false, v -> a.show(new WodListScreen(null, tag)));
            }
            c.addView(Ui.chipRow(a, tags));
        }

        c.addView(Ui.button(a, "Lancer le chrono", Ui.PRIMARY, v -> {
            ClockRunner.start(a, st.settings, Clock.forWod(w, st.settings.prepSeconds), w.id, null);
            a.show(new ClockScreen());
            a.refreshKeepScreenOn();
        }));
        c.addView(Ui.button(a, "Noter un score sans chrono", Ui.SECONDARY, v -> ScoreDialog.show(a, w, -1, -1, a::refresh)));

        List<WodResult> results = st.results(w.id);
        c.addView(Ui.section(a, "Mes scores"));
        if (results.isEmpty()) {
            TextView t = Ui.text(a, "Pas encore de score. Lance le chrono, ton résultat te sera proposé à la fin.", 14, Ui.MUTED);
            t.setPadding(0, 0, 0, 0);
            c.addView(t);
        } else {
            WodResult best = WodResult.best(st.wodResults, w.id, w.scoreType());
            for (WodResult r : results) {
                LinearLayout row = Ui.card(a);
                row.setOnLongClickListener(v -> {
                    Ui.confirm(a, "Supprimer ce score ?", r.fullScore() + " · " + Fmt.shortDay(r.date), "Supprimer", () -> {
                        st.deleteWodResult(r);
                        a.refresh();
                    });
                    return true;
                });
                LinearLayout line = Ui.hbox(a);
                LinearLayout txt = Ui.vbox(a);
                TextView sc = Ui.medium(a, r.fullScore(), 17, r == best ? Ui.ACCENT : Ui.TEXT);
                Ui.tabular(sc);
                txt.addView(sc);
                txt.addView(Ui.text(a, Fmt.day(r.date), 13, Ui.MUTED));
                if (r.note != null) txt.addView(Ui.text(a, r.note, 13, Ui.MUTED));
                line.addView(txt, Ui.weight(1));
                if (r == best) line.addView(Ui.badge(a, "Record", Ui.ACCENT, Ui.ACCENT_SOFT));
                row.addView(line);
                c.addView(row);
            }
            TextView hint = Ui.text(a, "Appui long sur un score pour le supprimer.", 12, Ui.FAINT);
            hint.setPadding(0, 0, 0, 0);
            c.addView(hint);
        }
        return Ui.page(a, c);
    }
}

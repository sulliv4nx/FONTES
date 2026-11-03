package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.ExerciseEntry;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Program;
import com.fonte.carnet.core.Session;
import com.fonte.carnet.core.SetEntry;

/** Détail du programme en cours, séance par séance. */
public class ProgramScreen extends Screen {

    @Override
    public String title() {
        return "Programme";
    }

    @Override
    public int actionIcon() {
        return st.program == null ? -1 : Icon.MORE;
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        Program p = st.program;
        if (p == null) {
            c.addView(Ui.text(a, "Aucun programme en cours.", 15, Ui.MUTED));
            return Ui.page(a, c);
        }
        c.addView(Ui.heading(a, p.title));
        if (p.summary != null && !p.summary.isEmpty()) c.addView(Ui.text(a, p.summary, 14, Ui.MUTED));
        c.addView(Ui.progress(a, p.doneCount(), p.days.size()));
        c.addView(Ui.text(a, p.doneCount() + " / " + Fmt.plural(p.days.size(), "séance") + " faites", 13, Ui.MUTED));

        if (p.finished()) {
            LinearLayout done = Ui.card(a);
            done.setBackground(Ui.rect(Ui.ACCENT_SOFT, 16));
            done.addView(Ui.medium(a, "Programme terminé, bravo !", 16, Ui.ACCENT));
            TextView t = Ui.text(a, "Regarde ton nouveau max dans Records, puis relance un cycle avec ce max.", 14, Ui.TEXT);
            t.setPadding(0, Ui.dp(4), 0, Ui.dp(4));
            done.addView(t);
            done.addView(Ui.button(a, "Relancer un cycle", Ui.PRIMARY, v -> ProgramSetup.restart(a, p)));
            LinearLayout.LayoutParams lp = Ui.mlp(-1, -2, 0, 12, 0, 0);
            c.addView(done, lp);
        }

        int next = p.nextIndex();
        String group = null;
        for (int i = 0; i < p.days.size(); i++) {
            Program.Day d = p.days.get(i);
            if (d.group != null && !d.group.equals(group)) {
                c.addView(Ui.section(a, d.group));
                group = d.group;
            }
            c.addView(dayRow(p, i, i == next));
        }
        return Ui.page(a, c);
    }

    private View dayRow(Program p, int index, boolean isNext) {
        Program.Day d = p.days.get(index);
        LinearLayout card = Ui.clickCard(a, v -> openDay(p, index));
        if (isNext) card.setBackground(Ui.pressable(Ui.outline(Ui.SURFACE, Ui.ACCENT, 16), 16));
        LinearLayout line = Ui.hbox(a);

        ImageView status = new ImageView(a);
        status.setPadding(Ui.dp(5), Ui.dp(5), Ui.dp(5), Ui.dp(5));
        if (d.isDone()) {
            status.setImageDrawable(new Icon(Icon.CHECK, Ui.BG));
            status.setBackground(Ui.rect(Ui.OK, 14));
        } else {
            status.setBackground(Ui.outline(0x00000000, isNext ? Ui.ACCENT : Ui.LINE, 14));
        }
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(Ui.dp(28), Ui.dp(28));
        sp.rightMargin = Ui.dp(12);
        line.addView(status, sp);

        LinearLayout txt = Ui.vbox(a);
        txt.addView(Ui.medium(a, d.name, 16, d.isDone() ? Ui.MUTED : Ui.TEXT));
        String sub;
        if (d.isDone()) {
            Session s = st.session(d.sessionId);
            sub = s != null ? "Fait le " + Fmt.shortDay(s.start) : "Fait";
        } else {
            sub = prescription(d);
        }
        TextView subView = Ui.text(a, sub, 13, Ui.MUTED);
        subView.setMaxLines(2);
        txt.addView(subView);
        line.addView(txt, Ui.weight(1));
        if (isNext) line.addView(Ui.badge(a, "Prochaine", Ui.ACCENT, Ui.ACCENT_SOFT));
        line.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(line);
        return card;
    }

    /** « Squat : 57,5×5 · 67,5×5 · 77,5×5+ … » */
    private String prescription(Program.Day d) {
        StringBuilder sb = new StringBuilder();
        for (ExerciseEntry e : d.exercises) {
            if (sb.length() > 0) sb.append("\n");
            if (d.exercises.size() > 1 || !d.name.startsWith(st.name(e.exerciseId))) {
                sb.append(st.name(e.exerciseId)).append(" : ");
            }
            int n = 0;
            for (SetEntry s : e.sets) {
                if (n == 6) {
                    sb.append(" …");
                    break;
                }
                if (n > 0) sb.append(" · ");
                sb.append(Fmt.kg(s.weight)).append("×").append(s.reps).append(s.amrap ? "+" : "");
                n++;
            }
        }
        return sb.toString();
    }

    private void openDay(Program p, int index) {
        Program.Day d = p.days.get(index);
        if (d.isDone() && st.session(d.sessionId) != null) {
            a.show(new SessionDetailScreen(d.sessionId));
            return;
        }
        new AlertDialog.Builder(a)
                .setTitle(d.group + " · " + d.name)
                .setMessage(prescription(d))
                .setPositiveButton("Commencer", (x, w) -> a.startProgramDay(index))
                .setNegativeButton("Fermer", null)
                .show();
    }

    @Override
    public void onAction() {
        Program p = st.program;
        if (p == null) return;
        String[] items = {"Relancer avec un nouveau max", "Arrêter le programme"};
        new AlertDialog.Builder(a)
                .setItems(items, (d, w) -> {
                    if (w == 0) {
                        ProgramSetup.restart(a, p);
                    } else {
                        Ui.confirm(a, "Arrêter le programme ?", "Tes séances déjà faites restent dans l'historique.",
                                "Arrêter", () -> {
                                    st.setProgram(null);
                                    a.goTab(MainActivity.TAB_PROGRAMS);
                                });
                    }
                })
                .show();
    }
}

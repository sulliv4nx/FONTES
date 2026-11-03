package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.ExerciseEntry;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Program;
import com.fonte.carnet.core.Template;

/** Programme en cours, programmes disponibles et modèles de séance. */
public class ProgramsScreen extends Screen {

    @Override
    public String title() {
        return "Programmes";
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        Program p = st.program;
        if (p != null) {
            TextView sec = Ui.section(a, "En cours");
            sec.setLayoutParams(Ui.mlp(-1, -2, 0, 8, 0, 8));
            c.addView(sec);
            LinearLayout card = Ui.clickCard(a, v -> a.show(new ProgramScreen()));
            card.addView(Ui.medium(a, p.title, 18, Ui.TEXT));
            if (p.summary != null && !p.summary.isEmpty()) card.addView(Ui.text(a, p.summary, 13, Ui.MUTED));
            card.addView(Ui.progress(a, p.doneCount(), p.days.size()));
            int next = p.nextIndex();
            String status = p.doneCount() + " / " + Fmt.plural(p.days.size(), "séance")
                    + (next >= 0 ? " · prochaine : " + p.days.get(next).name : " · terminé");
            card.addView(Ui.text(a, status, 13, Ui.MUTED));
            if (next >= 0) card.addView(Ui.button(a, "Commencer la prochaine séance", Ui.PRIMARY, v -> a.startProgramDay(next)));
            c.addView(card);
        }

        c.addView(Ui.section(a, "Programmes"));
        c.addView(programCard("Smolov Jr", "3 semaines · 4 séances / semaine · 1 mouvement",
                "Le spécialiste d'un mouvement (souvent le développé couché) : volume et intensité élevés, "
                        + "charges de 70 à 85 % du max et +2,5 à 10 kg d'une semaine à l'autre. Pour pratiquants confirmés.",
                v -> ProgramSetup.smolovJr(a)));
        c.addView(programCard("5/3/1 Wendler", "4 semaines · squat, couché, soulevé de terre, militaire",
                "Progression lente et durable sur les 4 mouvements de base, avec une série au maximum de répétitions "
                        + "chaque séance et une semaine de décharge.",
                v -> ProgramSetup.wendler(a)));

        c.addView(Ui.section(a, "Mes modèles de séance"));
        if (st.templates.isEmpty()) {
            TextView t = Ui.text(a, "Un modèle, c'est une séance type à relancer en un geste. Crée-en un depuis "
                    + "une séance (menu ⋯ › Enregistrer comme modèle).", 14, Ui.MUTED);
            t.setPadding(0, 0, 0, 0);
            c.addView(t);
        }
        for (Template t : st.templates) c.addView(templateCard(t));
        return Ui.page(a, c);
    }

    private View programCard(String name, String meta, String desc, View.OnClickListener setup) {
        LinearLayout card = Ui.card(a);
        card.addView(Ui.medium(a, name, 18, Ui.TEXT));
        TextView m = Ui.text(a, meta, 13, Ui.ACCENT);
        m.setPadding(0, Ui.dp(2), 0, Ui.dp(6));
        card.addView(m);
        card.addView(Ui.text(a, desc, 14, Ui.MUTED));
        card.addView(Ui.button(a, "Configurer", Ui.SECONDARY, setup));
        return card;
    }

    private View templateCard(Template t) {
        LinearLayout card = Ui.clickCard(a, v -> a.startSession(t.name, t.exercises, 0, -1));
        LinearLayout line = Ui.hbox(a);
        LinearLayout txt = Ui.vbox(a);
        txt.addView(Ui.medium(a, t.name, 16, Ui.TEXT));
        StringBuilder sb = new StringBuilder();
        for (ExerciseEntry e : t.exercises) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(st.name(e.exerciseId));
        }
        TextView ex = Ui.text(a, sb.toString(), 13, Ui.MUTED);
        ex.setMaxLines(2);
        ex.setEllipsize(android.text.TextUtils.TruncateAt.END);
        txt.addView(ex);
        line.addView(txt, Ui.weight(1));
        line.addView(Ui.iconButton(a, Icon.MORE, Ui.MUTED, v -> templateMenu(t)));
        card.addView(line);
        return card;
    }

    private void templateMenu(Template t) {
        String[] items = {"Démarrer", "Renommer", "Supprimer"};
        new AlertDialog.Builder(a)
                .setTitle(t.name)
                .setItems(items, (d, w) -> {
                    if (w == 0) {
                        a.startSession(t.name, t.exercises, 0, -1);
                    } else if (w == 1) {
                        Ui.prompt(a, "Renommer le modèle", t.name, "Nom", n -> {
                            t.name = n;
                            st.save();
                            a.refresh();
                        });
                    } else {
                        Ui.confirm(a, "Supprimer « " + t.name + " » ?", "Tes séances passées ne sont pas touchées.",
                                "Supprimer", () -> {
                                    st.deleteTemplate(t);
                                    a.refresh();
                                });
                    }
                })
                .show();
    }
}

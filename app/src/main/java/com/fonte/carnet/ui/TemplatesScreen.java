package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.ExerciseEntry;
import com.fonte.carnet.core.Template;
import com.fonte.carnet.core.Templates;

/** Séances types intégrées + modèles enregistrés par l'utilisateur. */
public class TemplatesScreen extends Screen {

    @Override
    public String title() {
        return "Séances types";
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        TextView intro = Ui.text(a, "Touche une séance pour la voir, puis lance-la. Les charges de ta dernière fois sont reprises "
                + "automatiquement : il ne reste qu'à ajuster.", 14, Ui.MUTED);
        intro.setPadding(0, Ui.dp(4), 0, 0);
        c.addView(intro);

        c.addView(Ui.section(a, "Mes modèles"));
        if (st.templates.isEmpty()) {
            TextView t = Ui.text(a, "Enregistre une séance comme modèle depuis le menu ⋯ d'une séance ou de l'historique.", 14, Ui.MUTED);
            t.setPadding(0, 0, 0, 0);
            c.addView(t);
        }
        for (Template t : st.templates) c.addView(card(t));

        for (String g : Templates.GROUPS) {
            c.addView(Ui.section(a, g));
            for (Template t : Templates.all()) if (g.equals(t.group)) c.addView(card(t));
        }
        return Ui.page(a, c);
    }

    private String exercises(Template t) {
        StringBuilder sb = new StringBuilder();
        for (ExerciseEntry e : t.exercises) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(st.name(e.exerciseId));
        }
        return sb.toString();
    }

    private View card(Template t) {
        LinearLayout card = Ui.clickCard(a, v -> preview(t));
        card.addView(Ui.medium(a, t.name, 16, Ui.TEXT));
        TextView ex = Ui.text(a, exercises(t), 13, Ui.MUTED);
        ex.setMaxLines(2);
        ex.setEllipsize(TextUtils.TruncateAt.END);
        ex.setPadding(0, Ui.dp(3), 0, 0);
        card.addView(ex);
        return card;
    }

    private void preview(Template t) {
        StringBuilder sb = new StringBuilder();
        if (t.description != null) sb.append(t.description).append("\n\n");
        for (ExerciseEntry e : t.exercises) {
            int reps = e.sets.isEmpty() ? 0 : e.sets.get(0).reps;
            sb.append("• ").append(st.name(e.exerciseId)).append(" : ").append(e.sets.size()).append("×").append(reps).append("\n");
        }
        AlertDialog.Builder b = new AlertDialog.Builder(a)
                .setTitle(t.name)
                .setMessage(sb.toString().trim())
                .setPositiveButton("Démarrer", (d, w) -> a.startSession(t.name, t.exercises, 0, -1))
                .setNegativeButton("Fermer", null);
        if (!t.builtin()) {
            b.setNeutralButton("Supprimer", (d, w) -> Ui.confirm(a, "Supprimer « " + t.name + " » ?",
                    "Tes séances passées ne sont pas touchées.", "Supprimer", () -> {
                        st.deleteTemplate(t);
                        a.refresh();
                    }));
        } else {
            b.setNeutralButton("Copier en modèle", (d, w) -> {
                st.saveTemplate(t.name, t.exercises);
                Ui.toast(a, "Ajouté à tes modèles : tu peux le modifier en séance.");
                a.refresh();
            });
        }
        b.show();
    }
}

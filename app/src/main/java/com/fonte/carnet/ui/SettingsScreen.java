package com.fonte.carnet.ui;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;

/** Réglages : minuteur, arrondi, sauvegarde. */
public class SettingsScreen extends Screen {
    private static final int[] REST = {0, 60, 90, 120, 150, 180, 240, 300};
    private static final double[] ROUNDING = {0.5, 1, 1.25, 2.5, 5};

    @Override
    public String title() {
        return "Réglages";
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);

        TextView s1 = Ui.section(a, "Minuteur de repos");
        s1.setLayoutParams(Ui.mlp(-1, -2, 4, 8, 4, 8));
        c.addView(s1);
        View[] restChips = new View[REST.length];
        for (int i = 0; i < REST.length; i++) {
            int sec = REST[i];
            String label = sec == 0 ? "Désactivé" : Fmt.clock(sec * 1000L);
            restChips[i] = Ui.chip(a, label, st.settings.restSeconds == sec, v -> {
                st.settings.restSeconds = sec;
                st.save();
                a.refresh();
            });
        }
        c.addView(Ui.chipRow(a, restChips));
        TextView restNote = Ui.text(a, "Il démarre tout seul quand tu coches une série.", 13, Ui.MUTED);
        restNote.setPadding(Ui.dp(4), 0, 0, Ui.dp(4));
        c.addView(restNote);

        LinearLayout vib = Ui.hbox(a);
        vib.setPadding(Ui.dp(4), Ui.dp(8), 0, Ui.dp(8));
        vib.addView(Ui.text(a, "Vibrer à la fin du repos", 16, Ui.TEXT), Ui.weight(1));
        Switch sw = new Switch(a);
        sw.setChecked(st.settings.vibrate);
        sw.setOnCheckedChangeListener((b, on) -> {
            st.settings.vibrate = on;
            st.save();
        });
        vib.addView(sw);
        c.addView(vib);

        c.addView(Ui.section(a, "Chrono CrossFit et renfo"));
        LinearLayout snd = Ui.hbox(a);
        snd.setPadding(Ui.dp(4), Ui.dp(4), 0, Ui.dp(8));
        snd.addView(Ui.text(a, "Bips (3-2-1 et changements de phase)", 16, Ui.TEXT), Ui.weight(1));
        Switch sw2 = new Switch(a);
        sw2.setChecked(st.settings.sounds);
        sw2.setOnCheckedChangeListener((b, on) -> {
            st.settings.sounds = on;
            st.save();
        });
        snd.addView(sw2);
        c.addView(snd);
        c.addView(Ui.text(a, "Compte à rebours avant le départ", 13, Ui.MUTED));
        int[] preps = {0, 5, 10, 15, 20};
        View[] prepChips = new View[preps.length];
        for (int i = 0; i < preps.length; i++) {
            int p = preps[i];
            prepChips[i] = Ui.chip(a, p == 0 ? "Aucun" : p + " s", st.settings.prepSeconds == p, v -> {
                st.settings.prepSeconds = p;
                st.save();
                a.refresh();
            });
        }
        c.addView(Ui.chipRow(a, prepChips));
        TextView volNote = Ui.text(a, "Le volume des bips suit le volume multimédia du téléphone.", 13, Ui.MUTED);
        volNote.setPadding(Ui.dp(4), 0, 0, 0);
        c.addView(volNote);

        c.addView(Ui.section(a, "Arrondi des charges calculées"));
        View[] roundChips = new View[ROUNDING.length];
        for (int i = 0; i < ROUNDING.length; i++) {
            double r = ROUNDING[i];
            roundChips[i] = Ui.chip(a, Fmt.kg(r) + " kg", Math.abs(st.settings.rounding - r) < 1e-9, v -> {
                st.settings.rounding = r;
                st.save();
                a.refresh();
            });
        }
        c.addView(Ui.chipRow(a, roundChips));
        TextView roundNote = Ui.text(a, "Utilisé par les programmes et le calculateur. 2,5 kg = la plus petite paire "
                + "de disques courante (2 × 1,25 kg). Ne modifie pas un programme déjà créé.", 13, Ui.MUTED);
        roundNote.setPadding(Ui.dp(4), 0, 0, 0);
        c.addView(roundNote);

        c.addView(Ui.section(a, "Sauvegarde"));
        LinearLayout card = Ui.card(a);
        card.addView(Ui.text(a, "Tes données restent uniquement sur ce téléphone. Exporte-les de temps en temps "
                + "(Drive, mail…) pour ne rien perdre si tu changes d'appareil.", 14, Ui.MUTED));
        card.addView(Ui.button(a, "Exporter mes données", Ui.SECONDARY, v -> a.exportData()));
        card.addView(Ui.button(a, "Importer une sauvegarde", Ui.GHOST, v -> a.importData()));
        c.addView(card);

        c.addView(Ui.section(a, "À propos"));
        LinearLayout about = Ui.card(a);
        about.addView(Ui.medium(a, "Fonte 2.0", 16, Ui.TEXT));
        about.addView(Ui.text(a, st.sessions.size() + " séances de muscu · "
                + Fmt.plural(st.wodResults.size(), "score") + " de WOD · "
                + st.renfoLogs.size() + " circuits de renfo · "
                + Fmt.plural(st.templates.size(), "modèle"), 13, Ui.MUTED));
        TextView safety = Ui.text(a, "1RM estimé avec la formule d'Epley. Les programmes à pourcentages sont exigeants : "
                + "échauffe-toi, garde une technique propre et utilise un pareur ou des sécurités sur les charges lourdes.",
                13, Ui.MUTED);
        safety.setPadding(0, Ui.dp(8), 0, 0);
        about.addView(safety);
        c.addView(about);
        return Ui.page(a, c);
    }
}

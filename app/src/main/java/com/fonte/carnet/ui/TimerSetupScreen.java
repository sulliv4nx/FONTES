package com.fonte.carnet.ui;

import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Clock;
import com.fonte.carnet.core.Fmt;

/** Réglage d'un chrono libre : AMRAP, EMOM, For Time, Tabata ou intervalles. */
public class TimerSetupScreen extends Screen {
    static final String AMRAP = "AMRAP";
    static final String EMOM = "EMOM";
    static final String FOR_TIME = "For Time";
    static final String TABATA = "Tabata";
    static final String INTERVALS = "Intervalles";
    private static final String[] MODES = {AMRAP, EMOM, FOR_TIME, TABATA, INTERVALS};

    private String mode;
    private int minutes = 12;
    private int every = 60;
    private int rounds = 10;
    private int cap = 0;
    private int work = 40;
    private int rest = 20;
    private int tabataRounds = 8;

    public TimerSetupScreen() {
        this(AMRAP);
    }

    public TimerSetupScreen(String mode) {
        this.mode = mode;
    }

    @Override
    public String title() {
        return "Chrono";
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        TextView s = Ui.section(a, "Type de chrono");
        s.setLayoutParams(Ui.mlp(-1, -2, 4, 4, 4, 8));
        c.addView(s);
        View[] chips = new View[MODES.length];
        for (int i = 0; i < MODES.length; i++) {
            String m = MODES[i];
            chips[i] = Ui.chip(a, m, m.equals(mode), v -> {
                mode = m;
                a.refresh();
            });
        }
        c.addView(Ui.chipRow(a, chips));

        LinearLayout card = Ui.card(a);
        TextView help = Ui.text(a, help(), 14, Ui.MUTED);
        help.setPadding(0, 0, 0, Ui.dp(8));
        card.addView(help);
        switch (mode) {
            case EMOM:
                card.addView(Ui.text(a, "Un départ toutes les…", 13, Ui.MUTED));
                int[] everies = {30, 60, 90, 120, 180};
                View[] ec = new View[everies.length];
                for (int i = 0; i < everies.length; i++) {
                    int e = everies[i];
                    ec[i] = Ui.chip(a, Fmt.clock(e * 1000L), every == e, v -> {
                        every = e;
                        a.refresh();
                    });
                }
                card.addView(Ui.chipRow(a, ec));
                card.addView(intField("Nombre d'intervalles", rounds, v -> rounds = v));
                break;
            case FOR_TIME:
                card.addView(intField("Cap en minutes (0 = sans limite)", cap, v -> cap = v));
                break;
            case TABATA:
                card.addView(intField("Nombre de tours", tabataRounds, v -> tabataRounds = v));
                break;
            case INTERVALS:
                card.addView(intField("Travail (secondes)", work, v -> work = v));
                card.addView(intField("Repos (secondes)", rest, v -> rest = v));
                card.addView(intField("Nombre de tours", rounds, v -> rounds = v));
                break;
            default:
                int[] quick = {5, 7, 10, 12, 15, 20, 30};
                View[] qc = new View[quick.length];
                for (int i = 0; i < quick.length; i++) {
                    int q = quick[i];
                    qc[i] = Ui.chip(a, q + " min", minutes == q, v -> {
                        minutes = q;
                        a.refresh();
                    });
                }
                card.addView(Ui.chipRow(a, qc));
                card.addView(intField("Durée (minutes)", minutes, v -> minutes = v));
                break;
        }
        c.addView(card);
        TextView prep = Ui.text(a, "Compte à rebours de " + st.settings.prepSeconds + " s avant le départ, bips sur les 3 dernières secondes "
                + "de chaque phase (réglable dans Réglages).", 13, Ui.MUTED);
        prep.setPadding(Ui.dp(4), 0, Ui.dp(4), Ui.dp(8));
        c.addView(prep);
        c.addView(Ui.button(a, "Démarrer", Ui.PRIMARY, v -> start()));
        return Ui.page(a, c);
    }

    private String help() {
        switch (mode) {
            case EMOM:
                return "Every Minute On the Minute : un bloc à chaque départ, le reste du temps sert de récupération.";
            case FOR_TIME:
                return "Le plus vite possible : le chrono monte, touche « Terminé » à la fin. Compteur de tours inclus.";
            case TABATA:
                return "20 s d'effort à fond, 10 s de repos.";
            case INTERVALS:
                return "Alternance travail / repos sur mesure.";
            default:
                return "As Many Rounds As Possible : un maximum de tours dans le temps imparti. Compte tes tours avec « +1 tour ».";
        }
    }

    private interface IntSetter {
        void set(int v);
    }

    private LinearLayout intField(String label, int value, IntSetter setter) {
        EditText e = Ui.field(a, String.valueOf(value), Ui.INTEGER);
        e.setText(String.valueOf(value));
        Ui.onText(e, t -> {
            int v = Fmt.parseInt(t);
            setter.set(v);
        });
        return Ui.labelled(a, label, e);
    }

    private void start() {
        int prep = st.settings.prepSeconds;
        Clock c;
        switch (mode) {
            case EMOM:
                if (rounds <= 0 || rounds > 200) {
                    Ui.toast(a, "Nombre d'intervalles entre 1 et 200.");
                    return;
                }
                c = Clock.emom(every, rounds, prep, null);
                break;
            case FOR_TIME:
                c = Clock.forTime(Math.max(0, Math.min(cap, 600)), prep);
                break;
            case TABATA:
                if (tabataRounds <= 0 || tabataRounds > 100) {
                    Ui.toast(a, "Nombre de tours entre 1 et 100.");
                    return;
                }
                c = Clock.tabata(tabataRounds, prep);
                break;
            case INTERVALS:
                if (work <= 0 || work > 3600 || rest < 0 || rest > 3600 || rounds <= 0 || rounds > 200) {
                    Ui.toast(a, "Vérifie les durées et le nombre de tours.");
                    return;
                }
                c = Clock.intervals("Intervalles " + work + " / " + rest, work, rest, rounds, prep);
                break;
            default:
                if (minutes <= 0 || minutes > 180) {
                    Ui.toast(a, "Durée entre 1 et 180 minutes.");
                    return;
                }
                c = Clock.amrap(minutes, prep);
                break;
        }
        Ui.hideKeyboard(a.getWindow().getDecorView());
        ClockRunner.start(a, st.settings, c, null, null);
        a.show(new ClockScreen());
        a.refreshKeepScreenOn();
    }
}

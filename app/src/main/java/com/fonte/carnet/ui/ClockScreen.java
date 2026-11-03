package com.fonte.carnet.ui;

import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fonte.carnet.core.Clock;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Routine;
import com.fonte.carnet.core.Routines;
import com.fonte.carnet.core.Wod;
import com.fonte.carnet.core.WodResult;

/** Écran du chrono en cours (WOD, minuteur libre ou circuit de renfo). */
public class ClockScreen extends Screen {
    private static final int REST_SOFT = 0x2E5B8CFF;

    private ClockRunner run;
    private LinearLayout root;
    private TextView label;
    private TextView time;
    private TextView unit;
    private TextView detail;
    private TextView nextView;
    private TextView total;
    private TextView roundsView;
    private View fill;
    private LinearLayout bar;
    private TextView mainButton;
    private TextView nextButton;
    private TextView prevButton;
    private TextView stopButton;
    private boolean scorePrompted;

    @Override
    public String title() {
        return run != null ? run.clock.title : "Chrono";
    }

    @Override
    public View build() {
        run = ClockRunner.active();
        if (run == null) {
            LinearLayout c = Ui.vbox(a);
            c.addView(Ui.text(a, "Aucun chrono en cours.", 16, Ui.MUTED));
            return Ui.page(a, c);
        }
        ScrollView sv = new ScrollView(a);
        sv.setFillViewport(true);
        root = Ui.vbox(a);
        root.setPadding(Ui.dp(20), Ui.dp(12), Ui.dp(20), Ui.dp(20));
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        label = Ui.text(a, "", 26, Ui.TEXT);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setGravity(Gravity.CENTER);
        root.addView(label, Ui.mlp(-1, -2, 0, 8, 0, 0));

        time = new TextView(a);
        time.setTextColor(Ui.TEXT);
        time.setTypeface(Typeface.DEFAULT_BOLD);
        time.setGravity(Gravity.CENTER);
        time.setMaxLines(1);
        Ui.tabular(time);
        time.setAutoSizeTextTypeUniformWithConfiguration(36, 120, 2, TypedValue.COMPLEX_UNIT_SP);
        root.addView(time, Ui.lp(-1, Ui.dp(150)));

        unit = Ui.text(a, "", 15, Ui.MUTED);
        unit.setGravity(Gravity.CENTER);
        root.addView(unit, Ui.lp(-1, -2));

        bar = Ui.hbox(a);
        bar.setBackground(Ui.rect(Ui.SURFACE2, 4));
        bar.setWeightSum(1f);
        fill = new View(a);
        fill.setBackground(Ui.rect(Ui.ACCENT, 4));
        bar.addView(fill, new LinearLayout.LayoutParams(0, -1, 0f));
        root.addView(bar, Ui.mlp(-1, Ui.dp(8), 0, 12, 0, 12));

        detail = Ui.text(a, "", 19, Ui.TEXT);
        detail.setGravity(Gravity.CENTER);
        root.addView(detail, Ui.lp(-1, -2));

        nextView = Ui.text(a, "", 14, Ui.MUTED);
        nextView.setGravity(Gravity.CENTER);
        root.addView(nextView, Ui.mlp(-1, -2, 0, 10, 0, 0));

        if (run.clock.roundCounter) {
            LinearLayout rr = Ui.hbox(a);
            rr.setGravity(Gravity.CENTER);
            TextView minus = Ui.chip(a, "−", false, v -> {
                if (run.rounds > 0) run.rounds--;
                refresh();
            });
            minus.setLayoutParams(Ui.lp(Ui.dp(56), -2));
            roundsView = Ui.medium(a, "", 20, Ui.TEXT);
            roundsView.setGravity(Gravity.CENTER);
            Ui.tabular(roundsView);
            TextView plus = Ui.button(a, "+1 tour", Ui.PRIMARY, v -> {
                run.rounds++;
                refresh();
            });
            rr.addView(minus);
            rr.addView(roundsView, Ui.weight(1));
            rr.addView(plus, Ui.lp(Ui.dp(130), -2));
            root.addView(rr, Ui.mlp(-1, -2, 0, 16, 0, 0));
        }

        total = Ui.text(a, "", 14, Ui.MUTED);
        total.setGravity(Gravity.CENTER);
        root.addView(total, Ui.mlp(-1, -2, 0, 12, 0, 0));

        root.addView(new View(a), new LinearLayout.LayoutParams(1, 0, 1));

        LinearLayout controls = Ui.hbox(a);
        prevButton = Ui.button(a, "Précédent", Ui.SECONDARY, v -> run.previous());
        mainButton = Ui.button(a, "Pause", Ui.PRIMARY, v -> onMain());
        nextButton = Ui.button(a, "Passer", Ui.SECONDARY, v -> run.next());
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, -2, 1);
        p1.rightMargin = Ui.dp(8);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, -2, 1.3f);
        p2.rightMargin = Ui.dp(8);
        controls.addView(prevButton, p1);
        controls.addView(mainButton, p2);
        controls.addView(nextButton, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(controls, Ui.mlp(-1, -2, 0, 16, 0, 0));
        stopButton = Ui.button(a, "Arrêter", Ui.DANGER_GHOST, v -> confirmStop());
        root.addView(stopButton);

        sv.addView(root);
        run.setListener(this::refresh);
        refresh();
        return sv;
    }

    @Override
    public void onHide() {
        if (run != null) run.setListener(null);
    }

    @Override
    public boolean onBack() {
        if (run == null) return false;
        if (!run.clock.finished()) {
            confirmStop();
            return true;
        }
        run.stop();
        return false;
    }

    private void confirmStop() {
        if (run.clock.finished()) {
            close();
            return;
        }
        Ui.confirm(a, "Arrêter le chrono ?", "Le chrono en cours sera arrêté.", "Arrêter", this::close);
    }

    private void close() {
        run.stop();
        a.back();
        a.refreshKeepScreenOn();
    }

    private void onMain() {
        Clock c = run.clock;
        if (!c.finished()) {
            if (run.paused()) run.resume();
            else run.pause();
            refresh();
            return;
        }
        if (run.wodId != null && !run.scoreSaved) {
            askScore();
        } else {
            close();
        }
    }

    private void askScore() {
        Wod w = st.wod(run.wodId);
        if (w == null) return;
        int seconds = (int) (run.clock.workElapsed(run.t()) / 1000);
        ScoreDialog.show(a, w, WodResult.TIME.equals(w.scoreType()) ? seconds : -1,
                WodResult.ROUNDS.equals(w.scoreType()) ? run.rounds : -1, () -> {
                    run.scoreSaved = true;
                    refresh();
                });
    }

    private void refresh() {
        if (run == null || root == null) return;
        Clock c = run.clock;
        long t = run.t();
        Clock.Phase p = c.current();
        if (roundsView != null) roundsView.setText("Tours : " + run.rounds);

        if (c.finished() || p == null) {
            root.setBackgroundColor(Ui.OK_SOFT);
            label.setText("Terminé !");
            time.setText(Fmt.clock(c.workElapsed(t)));
            unit.setText("temps total");
            setProgress(1f);
            detail.setText(run.wodId != null && run.scoreSaved ? "Score enregistré." : "Bravo, beau travail.");
            nextView.setText("");
            total.setText("");
            prevButton.setVisibility(View.GONE);
            nextButton.setVisibility(View.GONE);
            stopButton.setVisibility(View.GONE);
            if (run.wodId != null && !run.scoreSaved) {
                mainButton.setText("Enregistrer le score");
                if (!scorePrompted) {
                    scorePrompted = true;
                    root.post(this::askScore);
                }
            } else {
                mainButton.setText("Fermer");
            }
            if (run.routineId != null && !run.scoreSaved) {
                Routine r = Routines.find(run.routineId);
                if (r != null) {
                    st.addRenfo(r, (int) (c.workElapsed(t) / 1000));
                    Ui.toast(a, "Séance de renfo enregistrée.");
                }
                run.scoreSaved = true;
            }
            return;
        }

        int bg = p.prep ? Ui.SURFACE2 : p.rest ? REST_SOFT : Ui.ACCENT_SOFT;
        root.setBackgroundColor(run.paused() ? Ui.SURFACE : bg);
        label.setText(run.paused() ? "En pause" : p.label);
        if (p.manual) {
            time.setText(String.valueOf(p.reps));
            unit.setText("répétitions");
            setProgress(0f);
        } else if (p.countUp) {
            long e = c.phaseElapsed(t);
            time.setText(Fmt.clock(e));
            unit.setText(p.seconds > 0 ? "cap à " + Fmt.clock(p.seconds * 1000L) : "chrono");
            setProgress(p.seconds > 0 ? Math.min(1f, e / (p.seconds * 1000f)) : 0f);
        } else {
            long r = c.phaseRemaining(t);
            time.setText(Fmt.clock(r + 999));
            unit.setText(p.prep ? "avant le départ" : p.rest ? "de récupération" : "restantes");
            setProgress(p.seconds > 0 ? 1f - r / (p.seconds * 1000f) : 0f);
        }
        detail.setText(p.detail == null ? "" : p.detail);
        Clock.Phase up = c.upcoming();
        nextView.setText(up == null ? "" : "Ensuite : " + up.label);
        String info = "Temps total " + Fmt.clock(c.workElapsed(t));
        if (p.round > 0 && p.rounds > 1) info += " · tour " + p.round + " / " + p.rounds;
        info += " · " + (c.index() + 1) + "/" + c.phases.size();
        total.setText(info);

        mainButton.setText(run.paused() ? "Reprendre" : "Pause");
        nextButton.setVisibility(View.VISIBLE);
        nextButton.setText(p.manual ? "Fait ✓" : p.countUp ? "Terminé" : "Passer");
        prevButton.setVisibility(run.routineId != null && c.index() > 0 ? View.VISIBLE : View.GONE);
        stopButton.setVisibility(View.VISIBLE);
    }

    private void setProgress(float ratio) {
        float r = Math.max(0f, Math.min(1f, ratio));
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) fill.getLayoutParams();
        if (Math.abs(lp.weight - r) > 0.001f) {
            lp.weight = r;
            fill.setLayoutParams(lp);
        }
    }
}

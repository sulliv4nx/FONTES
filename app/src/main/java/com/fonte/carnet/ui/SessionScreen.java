package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fonte.carnet.core.Exercise;
import com.fonte.carnet.core.ExerciseEntry;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Session;
import com.fonte.carnet.core.SetEntry;
import com.fonte.carnet.core.Stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Séance en cours : saisie des séries, minuteur de repos, fin de séance. */
public class SessionScreen extends Screen {
    private LinearLayout list;
    private ScrollView scroll;
    private TextView elapsed;
    private LinearLayout restBar;
    private TextView restText;
    private Map<Long, Stats.Record> prev;

    @Override
    public String title() {
        return st.current == null ? "Séance" : st.current.name;
    }

    @Override
    public int actionIcon() {
        return Icon.MORE;
    }

    @Override
    public View build() {
        Session s = st.current;
        if (s == null) {
            TextView t = Ui.text(a, "Aucune séance en cours.", 16, Ui.MUTED);
            t.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(16));
            return t;
        }
        prev = Stats.records(st.sessions);
        LinearLayout root = Ui.vbox(a);

        LinearLayout top = Ui.hbox(a);
        top.setPadding(Ui.dp(16), 0, Ui.dp(16), Ui.dp(8));
        LinearLayout timeBox = Ui.vbox(a);
        elapsed = Ui.medium(a, "0:00", 24, Ui.TEXT);
        Ui.tabular(elapsed);
        timeBox.addView(elapsed);
        timeBox.addView(Ui.text(a, "Durée de la séance", 12, Ui.MUTED));
        top.addView(timeBox, Ui.weight(1));
        int rest = st.settings.restSeconds > 0 ? st.settings.restSeconds : 90;
        TextView restButton = Ui.chip(a, "Repos " + Fmt.clock(rest * 1000L), false, v -> a.startRest(rest));
        restButton.setLayoutParams(Ui.lp(-2, -2));
        top.addView(restButton);
        root.addView(top, Ui.lp(-1, -2));

        restBar = Ui.hbox(a);
        restBar.setBackground(Ui.rect(Ui.ACCENT_SOFT, 14));
        restBar.setPadding(Ui.dp(14), Ui.dp(8), Ui.dp(8), Ui.dp(8));
        ImageView timer = new ImageView(a);
        timer.setImageDrawable(new Icon(Icon.TIMER, Ui.ACCENT));
        restBar.addView(timer, Ui.lp(Ui.dp(24), Ui.dp(24)));
        restText = Ui.medium(a, "", 20, Ui.ACCENT);
        Ui.tabular(restText);
        restText.setPadding(Ui.dp(10), 0, 0, 0);
        restBar.addView(restText, Ui.weight(1));
        restBar.addView(Ui.chip(a, "+30 s", false, v -> a.addRest(30)));
        TextView skip = Ui.chip(a, "Passer", false, v -> a.stopRest());
        skip.setLayoutParams(Ui.lp(-2, -2));
        restBar.addView(skip);
        restBar.setVisibility(View.GONE);
        root.addView(restBar, Ui.mlp(-1, -2, 16, 0, 16, 8));

        list = Ui.vbox(a);
        scroll = Ui.page(a, list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        fill();
        return root;
    }

    @Override
    public void tick() {
        Session s = st.current;
        if (s == null || elapsed == null) return;
        elapsed.setText(Fmt.clock(s.durationMs()));
        long left = a.restLeft();
        if (left > 0) {
            restBar.setVisibility(View.VISIBLE);
            restText.setText("Repos  " + Fmt.clock(left + 999));
        } else {
            restBar.setVisibility(View.GONE);
        }
    }

    // ---------------------------------------------------------------- liste

    private void fill() {
        Session s = st.current;
        if (s == null || list == null) return;
        list.removeAllViews();
        if (s.exercises.isEmpty()) {
            TextView hint = Ui.text(a, "Ajoute ton premier exercice pour commencer. Coche ✓ chaque série terminée : "
                    + "seules les séries cochées sont enregistrées.", 15, Ui.MUTED);
            hint.setPadding(Ui.dp(4), Ui.dp(8), Ui.dp(4), Ui.dp(12));
            list.addView(hint);
        }
        for (int i = 0; i < s.exercises.size(); i++) list.addView(exerciseCard(s, i));
        list.addView(Ui.button(a, "+ Ajouter un exercice", Ui.SECONDARY, v -> ExercisePicker.show(a, this::addExercise)));
        list.addView(Ui.space(a, 6));
        list.addView(Ui.button(a, "Terminer la séance", Ui.PRIMARY, v -> finish()));
    }

    /** Reconstruit la liste en gardant la position de défilement. */
    private void refill() {
        if (scroll == null) return;
        int y = scroll.getScrollY();
        fill();
        scroll.post(() -> scroll.scrollTo(0, y));
    }

    private void addExercise(Exercise ex) {
        Session s = st.current;
        if (s == null) return;
        ExerciseEntry e = new ExerciseEntry(ex.id);
        List<SetEntry> last = Stats.lastPerformance(ex.id, st.sessions, s.id);
        if (last != null && !last.isEmpty()) {
            // On reprend les charges de la dernière fois : il suffit d'ajuster.
            for (SetEntry p : last) e.sets.add(new SetEntry(p.weight, p.reps));
        } else {
            e.sets.add(new SetEntry(0, 0));
        }
        s.exercises.add(e);
        st.save();
        fill();
        scroll.post(() -> scroll.smoothScrollTo(0, Math.max(0, list.getHeight() - scroll.getHeight())));
    }

    private TextView colLabel(String s) {
        TextView t = Ui.medium(a, s, 11, Ui.FAINT);
        t.setLetterSpacing(0.08f);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private View exerciseCard(Session s, int idx) {
        ExerciseEntry e = s.exercises.get(idx);
        LinearLayout card = Ui.card(a);
        card.setPadding(Ui.dp(14), Ui.dp(8), Ui.dp(8), Ui.dp(8));

        LinearLayout head = Ui.hbox(a);
        TextView name = Ui.medium(a, st.name(e.exerciseId), 18, Ui.TEXT);
        name.setPadding(Ui.dp(2), Ui.dp(6), 0, Ui.dp(2));
        head.addView(name, Ui.weight(1));
        head.addView(Ui.iconButton(a, Icon.MORE, Ui.MUTED, v -> exerciseMenu(s, idx)));
        card.addView(head);

        List<SetEntry> last = Stats.lastPerformance(e.exerciseId, st.sessions, s.id);
        if (last != null && !last.isEmpty()) {
            TextView lt = Ui.text(a, "Dernière fois : " + Stats.summarize(last), 13, Ui.MUTED);
            lt.setPadding(Ui.dp(2), 0, Ui.dp(8), Ui.dp(4));
            card.addView(lt);
        }

        LinearLayout cols = Ui.hbox(a);
        cols.setPadding(0, Ui.dp(6), Ui.dp(6), Ui.dp(2));
        cols.addView(colLabel("SÉRIE"), Ui.lp(Ui.dp(44), -2));
        cols.addView(colLabel("KG"), Ui.weight(1));
        cols.addView(colLabel("REPS"), Ui.weight(1));
        cols.addView(new View(a), Ui.lp(Ui.dp(52), 1));
        card.addView(cols);

        String prevLabel = null;
        for (int k = 0; k < e.sets.size(); k++) {
            SetEntry set = e.sets.get(k);
            if (set.label != null && !set.label.equals(prevLabel)) {
                TextView lab = Ui.medium(a, set.label, 12, Ui.ACCENT);
                lab.setPadding(Ui.dp(4), Ui.dp(8), 0, Ui.dp(2));
                card.addView(lab);
            }
            prevLabel = set.label;
            card.addView(setRow(e, set, k));
        }
        TextView add = Ui.button(a, "+ Série", Ui.GHOST, v -> addSet(e));
        add.setMinHeight(Ui.dp(44));
        card.addView(add);
        return card;
    }

    private View setRow(ExerciseEntry e, SetEntry set, int k) {
        LinearLayout row = Ui.hbox(a);
        row.setPadding(0, Ui.dp(3), Ui.dp(6), Ui.dp(3));
        row.setLayoutParams(Ui.mlp(-1, -2, 0, 2, 0, 2));

        LinearLayout numBox = Ui.vbox(a);
        numBox.setGravity(Gravity.CENTER);
        TextView num = Ui.medium(a, String.valueOf(k + 1), 16, Ui.MUTED);
        num.setGravity(Gravity.CENTER);
        numBox.addView(num);
        if (set.amrap) numBox.addView(Ui.medium(a, "MAX", 10, Ui.ACCENT));
        numBox.setOnClickListener(v -> Ui.toast(a, "Appui long pour supprimer la série."));
        numBox.setOnLongClickListener(v -> {
            Ui.confirm(a, "Supprimer la série " + (k + 1) + " ?", st.name(e.exerciseId), "Supprimer", () -> {
                e.sets.remove(set);
                st.save();
                refill();
            });
            return true;
        });
        row.addView(numBox, Ui.lp(Ui.dp(44), Ui.dp(48)));

        EditText kg = Ui.field(a, "kg", Ui.DECIMAL);
        kg.setGravity(Gravity.CENTER);
        if (set.weight > 0) kg.setText(Fmt.kg(set.weight));
        Ui.onText(kg, t -> {
            double v = Fmt.parse(t);
            set.weight = Double.isNaN(v) ? 0 : Math.max(0, v);
            st.saveSoon();
        });
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, -2, 1);
        p1.rightMargin = Ui.dp(8);
        row.addView(kg, p1);

        EditText reps = Ui.field(a, set.amrap ? "max" : "reps", Ui.INTEGER);
        reps.setGravity(Gravity.CENTER);
        if (set.reps > 0) reps.setText(String.valueOf(set.reps));
        Ui.onText(reps, t -> {
            set.reps = Fmt.parseInt(t);
            st.saveSoon();
        });
        row.addView(reps, new LinearLayout.LayoutParams(0, -2, 1));

        ImageView check = new ImageView(a);
        Icon icon = new Icon(Icon.CHECK, Ui.FAINT);
        check.setImageDrawable(icon);
        check.setPadding(Ui.dp(10), Ui.dp(10), Ui.dp(10), Ui.dp(10));
        Runnable paint = () -> {
            row.setBackground(set.done ? Ui.rect(Ui.OK_SOFT, 10) : null);
            check.setBackground(Ui.pressable(Ui.rect(set.done ? Ui.OK : Ui.SURFACE2, 10), 10));
            icon.setColor(set.done ? Ui.BG : Ui.FAINT);
        };
        paint.run();
        check.setOnClickListener(v -> {
            if (!set.done) {
                if (set.reps <= 0) {
                    Ui.toast(a, "Indique d'abord le nombre de répétitions.");
                    reps.requestFocus();
                    return;
                }
                set.done = true;
                Ui.hideKeyboard(v);
                kg.clearFocus();
                reps.clearFocus();
                Session cur = st.current;
                if (cur != null) {
                    String msg = Stats.liveRecord(set, prev.get(e.exerciseId), Stats.sessionBest(cur, e.exerciseId, set));
                    if (msg != null) Ui.toast(a, "🏆 " + msg);
                }
                if (st.settings.restSeconds > 0) a.startRest(st.settings.restSeconds);
            } else {
                set.done = false;
            }
            st.save();
            paint.run();
        });
        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(Ui.dp(44), Ui.dp(44));
        p3.leftMargin = Ui.dp(8);
        row.addView(check, p3);
        return row;
    }

    private void addSet(ExerciseEntry e) {
        SetEntry n;
        if (e.sets.isEmpty()) {
            n = new SetEntry(0, 0);
        } else {
            SetEntry l = e.sets.get(e.sets.size() - 1);
            n = new SetEntry(l.weight, l.reps);
        }
        e.sets.add(n);
        st.save();
        refill();
    }

    private void exerciseMenu(Session s, int idx) {
        if (idx < 0 || idx >= s.exercises.size()) return;
        ExerciseEntry e = s.exercises.get(idx);
        List<String> items = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        items.add("Voir la progression");
        actions.add(() -> a.show(new ExerciseScreen(e.exerciseId)));
        if (idx > 0) {
            items.add("Monter");
            actions.add(() -> {
                Collections.swap(s.exercises, idx, idx - 1);
                st.save();
                refill();
            });
        }
        if (idx < s.exercises.size() - 1) {
            items.add("Descendre");
            actions.add(() -> {
                Collections.swap(s.exercises, idx, idx + 1);
                st.save();
                refill();
            });
        }
        items.add("Retirer l'exercice");
        actions.add(() -> Ui.confirm(a, "Retirer « " + st.name(e.exerciseId) + " » ?",
                "Ses séries de cette séance seront perdues.", "Retirer", () -> {
                    s.exercises.remove(e);
                    st.save();
                    refill();
                }));
        new AlertDialog.Builder(a)
                .setTitle(st.name(e.exerciseId))
                .setItems(items.toArray(new String[0]), (d, w) -> actions.get(w).run())
                .show();
    }

    // ---------------------------------------------------------------- fin de séance

    private void finish() {
        Session s = st.current;
        if (s == null) return;
        int done = s.doneSets();
        int total = 0;
        for (ExerciseEntry e : s.exercises) total += e.sets.size();
        if (done == 0) {
            new AlertDialog.Builder(a)
                    .setTitle("Aucune série cochée")
                    .setMessage("Coche ✓ les séries effectuées pour qu'elles soient enregistrées.")
                    .setPositiveButton("Continuer", null)
                    .setNeutralButton("Abandonner la séance", (d, w) -> abandon())
                    .show();
            return;
        }
        int undone = total - done;
        String msg = Fmt.plural(done, "série") + (done > 1 ? " cochées." : " cochée.");
        if (undone == 1) msg += "\nLa série non cochée ne sera pas enregistrée.";
        else if (undone > 1) msg += "\nLes " + undone + " séries non cochées ne seront pas enregistrées.";
        Ui.confirm(a, "Terminer la séance ?", msg, "Terminer", () -> {
            if (list != null) Ui.hideKeyboard(list);
            Session fin = st.finishCurrent();
            a.stopRest();
            a.goTab(MainActivity.TAB_SESSION);
            a.showSummary(fin);
        });
    }

    private void abandon() {
        Ui.confirm(a, "Abandonner la séance ?", "Rien ne sera enregistré.", "Abandonner", () -> {
            if (list != null) Ui.hideKeyboard(list);
            st.cancelCurrent();
            a.stopRest();
            a.goTab(MainActivity.TAB_SESSION);
        });
    }

    @Override
    public void onAction() {
        Session s = st.current;
        if (s == null) return;
        String[] items = {"Renommer la séance", "Enregistrer comme modèle", "Abandonner la séance"};
        new AlertDialog.Builder(a)
                .setItems(items, (d, w) -> {
                    if (w == 0) {
                        Ui.prompt(a, "Nom de la séance", s.name, "Nom", n -> {
                            s.name = n;
                            st.save();
                            a.updateHeader();
                        });
                    } else if (w == 1) {
                        if (s.exercises.isEmpty()) {
                            Ui.toast(a, "Ajoute d'abord des exercices.");
                            return;
                        }
                        Ui.prompt(a, "Nom du modèle", s.name, "Nom", n -> {
                            st.saveTemplate(n, s.exercises);
                            Ui.toast(a, "Modèle enregistré : retrouve-le dans Programmes.");
                        });
                    } else {
                        abandon();
                    }
                })
                .show();
    }
}

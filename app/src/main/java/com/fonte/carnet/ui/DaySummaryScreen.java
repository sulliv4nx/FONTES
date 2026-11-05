package com.fonte.carnet.ui;

import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.DayPlanner;
import com.fonte.carnet.core.DayPrefs;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.MealPlanner;
import com.fonte.carnet.core.Nutrition;

import java.util.ArrayList;
import java.util.List;

/** Résumé de la journée type : totaux, différences repos / séance, part de chaque repas et règles. */
public class DaySummaryScreen extends ScrollScreen {
    private final int day;

    public DaySummaryScreen(int day) {
        this.day = day;
    }

    @Override
    public String title() {
        return "Résumé du jour";
    }

    @Override
    public int actionIcon() {
        return Icon.SHARE;
    }

    @Override
    public void onAction() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "Ma journée type");
        i.putExtra(Intent.EXTRA_TEXT, text(st.dayPlan(day), day, rules()));
        try {
            a.startActivity(Intent.createChooser(i, "Partager la journée"));
        } catch (RuntimeException e) {
            Ui.toast(a, "Aucune appli pour partager.");
        }
    }

    private List<String> rules() {
        if (st.dayPrefs.rules != null) return st.dayPrefs.rules;
        return DayPlanner.defaultRules(st.restTargets().water, st.trainingTargets().water);
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        DayPlanner.Day plan = st.dayPlan(day);
        c.addView(DayPlanScreen.summary(a, st, plan, day));
        c.addView(changes());
        c.addView(shares(plan));
        c.addView(rulesCard());
        TextView share = Ui.button(a, "Partager la journée", Ui.SECONDARY, v -> onAction());
        share.setLayoutParams(Ui.mlp(-1, -2, 0, 4, 0, 0));
        c.addView(share);
        return page(c);
    }

    private View changes() {
        Nutrition.Targets rest = st.restTargets();
        Nutrition.Targets train = st.trainingTargets();
        LinearLayout card = Ui.card(a);
        card.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 10));
        card.addView(Ui.medium(a, "Repos ou séance", 16, Ui.TEXT));
        card.addView(Ui.row(a, "Jour de repos", Fmt.kg(rest.kcal) + " kcal · G " + rest.carbs + " g · eau " + Fmt.kg(rest.water / 1000.0) + " L"));
        card.addView(Ui.row(a, "Jour de séance", Fmt.kg(train.kcal) + " kcal · G " + train.carbs + " g · eau " + Fmt.kg(train.water / 1000.0) + " L"));
        int dk = train.kcal - rest.kcal;
        int dc = train.carbs - rest.carbs;
        String s = "Avec la séance : " + (dk >= 0 ? "+" : "") + dk + " kcal, surtout en glucides (" + (dc >= 0 ? "+" : "") + dc + " g)"
                + (st.dayPrefs.aroundTraining ? ", placés aux repas d'avant et d'après" : ", répartis sur la journée")
                + (st.dayPrefs.preSnack ? ", et un fruit 30 min avant." : ".")
                + " Sans séance : le repas d'avant devient une collation sans féculent ; protéines et lipides ne bougent pas.";
        TextView t = Ui.text(a, s, 13, Ui.MUTED);
        t.setPadding(0, Ui.dp(8), 0, 0);
        card.addView(t);
        return card;
    }

    private View shares(DayPlanner.Day plan) {
        LinearLayout card = Ui.card(a);
        card.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 10));
        card.addView(Ui.medium(a, "Part de chaque repas", 16, Ui.TEXT));
        double max = 0;
        for (DayPlanner.Slot s : plan.slots) max = Math.max(max, s.kcal);
        for (DayPlanner.Slot s : plan.slots) {
            LinearLayout row = Ui.hbox(a);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, Ui.dp(8), 0, 0);
            TextView time = Ui.text(a, DayPrefs.clock(s.minutes), 12, Ui.MUTED);
            Ui.tabular(time);
            row.addView(time, new LinearLayout.LayoutParams(Ui.dp(44), -2));
            LinearLayout box = Ui.vbox(a);
            String name = DayPlanner.title(s, plan.training);
            box.addView(Ui.text(a, name, 13, Ui.TEXT));
            LinearLayout bar = Ui.hbox(a);
            bar.setWeightSum(1f);
            View fill = new View(a);
            fill.setBackground(Ui.rect(s.main() ? Ui.ACCENT : DayPlanScreen.SESSION_TEXT, 3));
            bar.addView(fill, new LinearLayout.LayoutParams(0, -1, max > 0 ? (float) (s.kcal / max) : 0f));
            box.addView(bar, Ui.mlp(-1, Ui.dp(6), 0, 4, 0, 0));
            row.addView(box, Ui.weight(1));
            int pct = plan.kcal > 0 ? (int) Math.round(100 * s.kcal / plan.kcal) : 0;
            TextView k = Ui.text(a, Math.round(s.kcal) + " kcal · " + pct + " %", 12, Ui.MUTED);
            Ui.tabular(k);
            k.setGravity(Gravity.END);
            k.setPadding(Ui.dp(10), 0, 0, 0);
            row.addView(k, new LinearLayout.LayoutParams(Ui.dp(104), -2));
            card.addView(row);
        }
        return card;
    }

    private View rulesCard() {
        DayPrefs p = st.dayPrefs;
        LinearLayout card = Ui.card(a);
        card.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 10));
        card.addView(Ui.medium(a, "Mes règles", 16, Ui.TEXT));
        TextView sub = Ui.text(a, "Touche une règle pour la modifier ou l'effacer.", 12, Ui.FAINT);
        sub.setPadding(0, Ui.dp(2), 0, Ui.dp(4));
        card.addView(sub);
        List<String> rules = rules();
        for (int i = 0; i < rules.size(); i++) {
            int idx = i;
            TextView t = Ui.text(a, "•  " + rules.get(i), 14, Ui.TEXT);
            t.setPadding(Ui.dp(2), Ui.dp(8), Ui.dp(2), Ui.dp(8));
            t.setBackground(Ui.pressable(Ui.rect(0x00000000, 8), 8));
            t.setOnClickListener(v -> new android.app.AlertDialog.Builder(a)
                    .setTitle("Règle")
                    .setItems(new String[]{"Modifier", "Effacer"}, (d, w) -> {
                        if (w == 0) {
                            Ui.prompt(a, "Modifier la règle", rules.get(idx), "Règle", s -> {
                                List<String> copy = editable();
                                copy.set(idx, s);
                                p.rules = copy;
                                st.save();
                                a.refresh();
                            });
                        } else {
                            List<String> copy = editable();
                            copy.remove(idx);
                            p.rules = copy;
                            st.save();
                            a.refresh();
                        }
                    }).show());
            card.addView(t);
        }
        LinearLayout buttons = Ui.buttonRow(a,
                Ui.smallButton(a, "+ Ajouter", Ui.GHOST, v -> Ui.prompt(a, "Nouvelle règle", null, "Ex. : pas de sucre après 18h", s -> {
                    List<String> copy = editable();
                    copy.add(s);
                    p.rules = copy;
                    st.save();
                    a.refresh();
                })),
                Ui.smallButton(a, "Règles proposées", Ui.GHOST, v -> Ui.confirm(a, "Rétablir les règles proposées ?",
                        "Tes règles seront remplacées par celles proposées par l'appli.", "Rétablir", () -> {
                            p.rules = null;
                            st.save();
                            a.refresh();
                        })));
        card.addView(buttons);
        return card;
    }

    private List<String> editable() {
        return new ArrayList<>(rules());
    }

    /** Texte à partager (message, note, coach). */
    static String text(DayPlanner.Day plan, int day, List<String> rules) {
        StringBuilder b = new StringBuilder();
        b.append("Journée type · ").append(MealPlanner.weekdayLong(day)).append(plan.training ? " (séance)" : " (repos)").append('\n');
        b.append(Math.round(plan.kcal)).append(" kcal · P ").append(Math.round(plan.protein)).append(" g · G ")
                .append(Math.round(plan.carbs)).append(" g · L ").append(Math.round(plan.fat)).append(" g\n");
        boolean sessionShown = false;
        for (DayPlanner.Slot s : plan.slots) {
            if (plan.training && !sessionShown && s.minutes > plan.sessionTime) {
                b.append('\n').append(DayPrefs.clock(plan.sessionTime)).append(" · Séance\n");
                sessionShown = true;
            }
            b.append('\n').append(DayPrefs.clock(s.minutes)).append(" · ").append(DayPlanner.title(s, plan.training))
                    .append(" (").append(Math.round(s.kcal)).append(" kcal)\n");
            for (DayPlanner.Line l : s.lines) {
                String d = DayPlanner.detail(l);
                b.append("- ").append(DayPlanner.text(l)).append(d == null ? "" : " (" + d + ")").append('\n');
                String alt = DayPlanner.altText(l);
                if (!alt.isEmpty()) b.append("  ").append(alt).append('\n');
            }
        }
        if (plan.training && !sessionShown) b.append('\n').append(DayPrefs.clock(plan.sessionTime)).append(" · Séance\n");
        if (rules != null && !rules.isEmpty()) {
            b.append("\nMes règles\n");
            for (String r : rules) b.append("- ").append(r).append('\n');
        }
        return b.toString().trim();
    }
}

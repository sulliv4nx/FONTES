package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.DayPlanner;
import com.fonte.carnet.core.DayPrefs;
import com.fonte.carnet.core.Food;
import com.fonte.carnet.core.MealPrefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/** Réglages de la journée type : horaires, jours de séance, nombre de repas, aliments proposés et rappels. */
public class DaySetupScreen extends ScrollScreen {
    /** Premier passage : le bouton crée la journée type et l'ouvre. */
    private final boolean first;

    public DaySetupScreen() {
        this(false);
    }

    public DaySetupScreen(boolean first) {
        this.first = first;
    }

    @Override
    public String title() {
        return "Ma journée type";
    }

    private DayPrefs p() {
        return st.dayPrefs;
    }

    private void set(Runnable r) {
        r.run();
        st.save();
        a.refresh();
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        TextView intro = Ui.text(a, "Des repas à heures fixes, faits de blocs : glucides, protéines, légumes ou fruit, lipides. "
                + "Chaque aliment a ses équivalences, à la même quantité de glucides, de protéines ou de lipides. "
                + "Les quantités suivent tes objectifs, jour de séance ou de repos.", 13, Ui.MUTED);
        intro.setPadding(Ui.dp(2), 0, Ui.dp(2), Ui.dp(10));
        c.addView(intro);
        c.addView(hoursCard());
        c.addView(mealsCard());
        for (String role : new String[]{DayPlanner.CARBS, DayPlanner.PROTEIN, DayPlanner.FAT, DayPlanner.FRUIT, DayPlanner.VEG}) {
            c.addView(familyCard(role));
        }
        c.addView(remindersCard());
        TextView ok = Ui.button(a, first ? "Créer ma journée type" : "OK", Ui.PRIMARY, v -> {
            p().dayMode = true;
            st.save();
            a.back();
            if (first) {
                // L'écran des plats laisse la place à la journée type.
                if (a.currentScreen() instanceof MealPlanScreen) a.back();
                a.show(new DayPlanScreen(0));
            }
        });
        ok.setLayoutParams(Ui.mlp(-1, -2, 0, 6, 0, 0));
        c.addView(ok);
        return page(c);
    }

    private static TextView label(MainActivity a, String s) {
        TextView t = Ui.medium(a, s, 16, Ui.TEXT);
        t.setPadding(0, 0, 0, Ui.dp(4));
        return t;
    }

    private LinearLayout card() {
        LinearLayout card = Ui.card(a);
        card.setLayoutParams(Ui.mlp(-1, -2, 0, 0, 0, 10));
        return card;
    }

    // ---------------------------------------------------------------- horaires

    private View clockRow(String label, String value, String sub, View.OnClickListener l) {
        LinearLayout row = Ui.hbox(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(Ui.dp(48));
        row.setBackground(Ui.pressable(Ui.rect(0x00000000, 10), 10));
        LinearLayout txt = Ui.vbox(a);
        txt.addView(Ui.text(a, label, 15, Ui.TEXT));
        if (sub != null) txt.addView(Ui.text(a, sub, 12, Ui.FAINT));
        row.addView(txt, Ui.weight(1));
        TextView v = Ui.medium(a, value, 15, Ui.ACCENT);
        v.setBackground(Ui.rect(Ui.SURFACE2, 8));
        v.setPadding(Ui.dp(10), Ui.dp(5), Ui.dp(10), Ui.dp(5));
        Ui.tabular(v);
        row.addView(v);
        row.setContentDescription(label + " : " + value);
        row.setOnClickListener(l);
        return row;
    }

    private void askClock(String title, int current, IntConsumer done) {
        Ui.prompt(a, title, DayPrefs.clock(current), "Ex. : 6h15", s -> {
            int m = DayPrefs.parseClock(s);
            if (m < 0) {
                Ui.toast(a, "Heure non comprise. Écris par exemple 6h15.");
                return;
            }
            done.accept(m);
        });
    }

    private View hoursCard() {
        DayPrefs p = p();
        LinearLayout card = card();
        card.addView(label(a, "Horaires"));
        card.addView(clockRow("Lever", DayPrefs.clock(p.wake), null,
                v -> askClock("Heure du lever", p.wake, m -> set(() -> p.wake = m))));
        card.addView(clockRow("Séance", DayPrefs.clock(p.training), "Heure habituelle de l'entraînement",
                v -> askClock("Heure de la séance", p.training, m -> set(() -> p.training = m))));
        TextView dl = Ui.text(a, "Jours de séance", 13, Ui.MUTED);
        dl.setPadding(0, Ui.dp(10), 0, Ui.dp(6));
        card.addView(dl);
        MealPrefs prefs = st.mealPrefs();
        LinearLayout days = Ui.hbox(a);
        String[] l = {"L", "M", "M", "J", "V", "S", "D"};
        String[] names = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"};
        int mask = Math.max(0, prefs.trainingMask);
        for (int i = 0; i < 7; i++) {
            final int bit = 1 << i;
            boolean on = (mask & bit) != 0;
            TextView d = Ui.medium(a, l[i], 14, on ? Ui.BG : Ui.MUTED);
            d.setGravity(Gravity.CENTER);
            d.setMinHeight(Ui.dp(40));
            d.setBackground(Ui.pressable(on ? Ui.rect(DayPlanScreen.SESSION_TEXT, 10) : Ui.outline(0x00000000, Ui.LINE, 10), 10));
            d.setContentDescription(names[i] + (on ? ", jour de séance" : ", jour de repos"));
            d.setOnClickListener(v -> set(() -> prefs.trainingMask = Math.max(0, prefs.trainingMask) ^ bit));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) lp.leftMargin = Ui.dp(5);
            days.addView(d, lp);
        }
        card.addView(days);
        TextView hint = Ui.text(a, "Les autres jours suivent les objectifs d'un jour de repos.", 12, Ui.FAINT);
        hint.setPadding(0, Ui.dp(8), 0, 0);
        card.addView(hint);
        return card;
    }

    // ---------------------------------------------------------------- repas

    private View mealsCard() {
        DayPrefs p = p();
        LinearLayout card = card();
        card.addView(label(a, "Repas"));
        int sel = Math.max(0, Math.min(2, p.meals - 4));
        LinearLayout seg = Ui.segmented(a, new String[]{"4 repas", "5 repas", "6 repas"}, sel, i -> set(() -> {
            if (p.meals != i + 4) p.times.clear();
            p.meals = i + 4;
        }));
        seg.setLayoutParams(Ui.mlp(-1, -2, 0, 4, 0, 8));
        card.addView(seg);
        card.addView(Ui.switchRow(a, "Un fruit avant la séance", "30 minutes avant, les jours de séance", p.preSnack,
                on -> set(() -> p.preSnack = on)));
        card.addView(Ui.switchRow(a, "Plus de glucides autour de la séance", "Repas d'avant et d'après mieux servis ; sinon, parts égales",
                p.aroundTraining, on -> set(() -> p.aroundTraining = on)));

        TextView tl = Ui.text(a, "Heures des repas", 13, Ui.MUTED);
        tl.setPadding(0, Ui.dp(12), 0, Ui.dp(2));
        card.addView(tl);
        DayPlanner.Schedule s = DayPlanner.schedule(p);
        for (int i = 0; i < s.times.length; i++) {
            int idx = i;
            boolean fixed = p.times.containsKey(i);
            DayPlanner.Slot fake = new DayPlanner.Slot();
            fake.index = i;
            fake.kind = s.kinds[i];
            fake.minutes = s.times[i];
            String name = DayPlanner.title(fake, true);
            card.addView(clockRow(name, DayPrefs.clock(s.times[i]), fixed ? "Heure fixée" : "Calculée d'après le lever et la séance", v -> {
                if (!fixed) {
                    askClock(name, s.times[idx], m -> set(() -> p.times.put(idx, m)));
                    return;
                }
                new AlertDialog.Builder(a).setTitle(name)
                        .setItems(new String[]{"Changer l'heure", "Heure calculée"}, (d, w) -> {
                            if (w == 0) askClock(name, s.times[idx], m -> set(() -> p.times.put(idx, m)));
                            else set(() -> p.times.remove(idx));
                        }).show();
            }));
        }
        return card;
    }

    // ---------------------------------------------------------------- aliments

    private static String familyTitle(String role) {
        switch (role) {
            case DayPlanner.CARBS:
                return "Glucides proposés";
            case DayPlanner.PROTEIN:
                return "Protéines proposées";
            case DayPlanner.FAT:
                return "Lipides proposés";
            case DayPlanner.FRUIT:
                return "Fruits proposés";
            default:
                return "Légumes au choix";
        }
    }

    private View familyCard(String role) {
        DayPrefs p = p();
        Set<String> set = p.family(role);
        LinearLayout card = card();
        card.addView(label(a, familyTitle(role)));
        List<View> chips = new ArrayList<>();
        Set<String> shown = new HashSet<>();
        for (String[] ch : DayPlanner.choices(role)) {
            shown.add(ch[0]);
            chips.add(chip(set, ch[0], ch[1]));
        }
        for (String id : new ArrayList<>(set)) {
            if (shown.contains(id)) continue;
            Food f = DayPlanner.resolve(id, st::food);
            chips.add(chip(set, id, f == null ? id : f.name));
        }
        TextView more = Ui.medium(a, "+ Autre", 13, Ui.ACCENT);
        more.setGravity(Gravity.CENTER);
        more.setMinHeight(Ui.dp(40));
        more.setPadding(Ui.dp(14), Ui.dp(8), Ui.dp(14), Ui.dp(8));
        more.setBackground(Ui.pressable(Ui.outline(0x00000000, Ui.LINE, 20), 20));
        more.setOnClickListener(v -> DayPlanScreen.pickFood(a, role, f -> {
            if (f.id == null) return;
            set(() -> set.add(f.id));
        }));
        chips.add(more);
        card.addView(Ui.flow(a, chips.toArray(new View[0])));
        return card;
    }

    private View chip(Set<String> set, String id, String name) {
        boolean on = set.contains(id);
        return Ui.toggleChip(a, name, on, v -> {
            if (on && set.size() <= 1) {
                Ui.toast(a, "Garde au moins un aliment dans cette famille.");
                return;
            }
            set(() -> {
                if (on) {
                    set.remove(id);
                    // Les bases imposées sur cet aliment tombent avec lui.
                    p().firsts.values().removeIf(id::equals);
                    p().dayFirsts.values().removeIf(id::equals);
                } else {
                    set.add(id);
                }
            });
        });
    }

    // ---------------------------------------------------------------- rappels

    private View remindersCard() {
        DayPrefs p = p();
        LinearLayout card = card();
        card.addView(label(a, "Rappels"));
        TextView sub = Ui.text(a, "Compléments ou consignes à voir avec un repas (l'appli n'ajoute aucune dose).", 12, Ui.FAINT);
        sub.setPadding(0, 0, 0, Ui.dp(6));
        card.addView(sub);
        for (DayPrefs.Reminder r : new ArrayList<>(p.reminders)) {
            LinearLayout row = Ui.hbox(a);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(Ui.dp(44));
            TextView t = Ui.text(a, "Repas " + (r.slot + 1) + " · " + r.text, 14, Ui.TEXT);
            row.addView(t, Ui.weight(1));
            row.addView(Ui.iconButton(a, Icon.CLOSE, Ui.MUTED, v -> set(() -> p.reminders.remove(r))));
            card.addView(row);
        }
        TextView add = Ui.smallButton(a, "+ Ajouter un rappel", Ui.GHOST, v -> Ui.prompt(a, "Rappel", null, "Ex. : oméga 3", text -> {
            int n = DayPlanner.schedule(p).times.length;
            String[] items = new String[n];
            for (int i = 0; i < n; i++) items[i] = "Repas " + (i + 1);
            new AlertDialog.Builder(a).setTitle("Avec quel repas ?")
                    .setItems(items, (d, w) -> set(() -> p.reminders.add(new DayPrefs.Reminder(text, w)))).show();
        }));
        add.setLayoutParams(Ui.mlp(-1, -2, 0, 6, 0, 0));
        card.addView(add);
        return card;
    }
}

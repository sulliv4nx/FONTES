package com.fonte.carnet.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.MealPlan;
import com.fonte.carnet.core.ShoppingList;

import java.util.List;
import java.util.Locale;

/** Liste de courses de la semaine, cochable, à partager ou copier. */
public class ShoppingScreen extends ScrollScreen {
    /** Liste de la journée type (7 prochains jours) plutôt que des plats de la semaine. */
    private final boolean dayType;
    private MealPlan week;

    public ShoppingScreen() {
        this(false);
    }

    public ShoppingScreen(boolean dayType) {
        this.dayType = dayType;
    }

    private MealPlan plan() {
        if (!dayType) return st.mealPlan;
        if (week == null) week = st.dayWeek();
        return week;
    }

    @Override
    public String title() {
        return dayType ? "Courses · journée type" : "Liste de courses";
    }

    @Override
    public int actionIcon() {
        return plan() == null ? -1 : Icon.SHARE;
    }

    @Override
    public void onAction() {
        MealPlan plan = plan();
        if (plan == null) return;
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "Liste de courses");
        i.putExtra(Intent.EXTRA_TEXT, ShoppingList.text(plan, ShoppingList.build(plan)));
        try {
            a.startActivity(Intent.createChooser(i, "Partager la liste"));
        } catch (RuntimeException e) {
            Ui.toast(a, "Aucune appli pour partager.");
        }
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        week = null;
        MealPlan plan = plan();
        if (plan == null) {
            c.addView(Ui.hint(a, "Crée d'abord ton plan de repas : la liste de courses est calculée à partir de lui."));
            c.addView(Ui.button(a, "Créer mon plan", Ui.PRIMARY, v -> a.show(new MealSetupScreen())));
            return page(c);
        }
        List<ShoppingList.Line> lines = ShoppingList.build(plan);
        c.addView(summary(plan, lines));
        String aisle = null;
        LinearLayout group = null;
        for (ShoppingList.Line l : lines) {
            if (!l.aisle.equals(aisle)) {
                aisle = l.aisle;
                c.addView(Ui.section(a, aisle));
                group = Ui.card(a);
                group.setPadding(Ui.dp(16), Ui.dp(4), Ui.dp(16), Ui.dp(4));
                c.addView(group);
            } else {
                View line = new View(a);
                line.setBackgroundColor(Ui.SURFACE2);
                group.addView(line, Ui.lp(-1, 1));
            }
            group.addView(row(l));
        }
        c.addView(addRow());
        TextView t = Ui.text(a, "Épices, ail, oignons, sel et citron ne sont pas comptés : pense à vérifier ton placard.", 12, Ui.FAINT);
        t.setPadding(0, Ui.dp(10), 0, 0);
        c.addView(t);
        return page(c);
    }

    private View summary(MealPlan plan, List<ShoppingList.Line> lines) {
        int done = ShoppingList.checkedCount(lines);
        LinearLayout card = Ui.card(a);
        TextView label = Ui.medium(a, ("Semaine " + plan.rangeLabel()).toUpperCase(Locale.FRENCH), 11, Ui.MUTED);
        label.setLetterSpacing(0.06f);
        card.addView(label);
        LinearLayout row = Ui.hbox(a);
        row.setGravity(Gravity.BOTTOM);
        TextView big = Ui.text(a, done + " ", 24, Ui.TEXT);
        big.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        Ui.tabular(big);
        row.addView(big);
        TextView of = Ui.text(a, "sur " + lines.size() + " article" + (lines.size() > 1 ? "s" : ""), 15, Ui.MUTED);
        of.setPadding(0, 0, 0, Ui.dp(3));
        row.addView(of, Ui.weight(1));
        card.addView(row);
        card.addView(Ui.progress(a, done, lines.size()));
        TextView hint = Ui.text(a, "Quantités pour " + plan.days + " jours, en poids cru ou sec.", 12, Ui.FAINT);
        card.addView(hint);
        LinearLayout actions = Ui.hbox(a);
        actions.setLayoutParams(Ui.mlp(-1, -2, 0, 6, 0, 0));
        TextView copy = Ui.medium(a, "Copier", 14, Ui.ACCENT);
        copy.setPadding(0, Ui.dp(12), Ui.dp(20), Ui.dp(8));
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Liste de courses", ShoppingList.text(plan, lines)));
                Ui.toast(a, "Liste copiée");
            }
        });
        actions.addView(copy);
        if (done > 0) {
            TextView reset = Ui.medium(a, "Tout décocher", 14, Ui.ACCENT);
            reset.setPadding(0, Ui.dp(12), 0, Ui.dp(8));
            reset.setOnClickListener(v -> {
                st.uncheckShopping();
                a.refresh();
            });
            actions.addView(reset);
        }
        card.addView(actions);
        return card;
    }

    private View row(ShoppingList.Line l) {
        LinearLayout r = Ui.hbox(a);
        r.setMinimumHeight(Ui.dp(52));
        r.setPadding(0, Ui.dp(6), 0, Ui.dp(6));
        r.setBackground(Ui.pressable(Ui.rect(0x00000000, 8), 8));
        ImageView box = new ImageView(a);
        if (l.checked) {
            box.setImageDrawable(new Icon(Icon.CHECK, Ui.BG));
            box.setBackground(Ui.rect(Ui.ACCENT, 7));
        } else {
            box.setBackground(Ui.outline(0x00000000, 0xFF4A5A6D, 7));
        }
        box.setPadding(Ui.dp(3), Ui.dp(3), Ui.dp(3), Ui.dp(3));
        r.addView(box, Ui.lp(Ui.dp(22), Ui.dp(22)));
        LinearLayout txt = Ui.vbox(a);
        txt.setPadding(Ui.dp(12), 0, Ui.dp(8), 0);
        TextView name = Ui.text(a, l.name, 15, l.checked ? Ui.FAINT : Ui.TEXT);
        if (l.checked) name.setPaintFlags(name.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        txt.addView(name);
        if (l.pantry) txt.addView(Ui.text(a, "Souvent déjà au placard", 12, Ui.FAINT));
        r.addView(txt, Ui.weight(1));
        if (!l.amount.isEmpty()) {
            TextView q = Ui.medium(a, l.amount, 14, l.checked ? Ui.FAINT : Ui.MUTED);
            Ui.tabular(q);
            q.setGravity(Gravity.END);
            r.addView(q);
        }
        r.setContentDescription(l.name + (l.amount.isEmpty() ? "" : ", " + l.amount) + (l.checked ? ", coché" : ""));
        r.setOnClickListener(v -> {
            st.toggleShop(l.key);
            a.refresh();
        });
        if (l.extraIndex >= 0) {
            r.setOnLongClickListener(v -> {
                Ui.confirm(a, "Retirer « " + l.name + " » ?", "Article ajouté à la main.", "Retirer", () -> {
                    st.deleteShopExtra(l.extraIndex);
                    a.refresh();
                });
                return true;
            });
        }
        return r;
    }

    private View addRow() {
        LinearLayout r = Ui.hbox(a);
        r.setLayoutParams(Ui.mlp(-1, -2, 0, 16, 0, 0));
        EditText e = Ui.field(a, "Ajouter un article", Ui.TEXT_FIELD);
        e.setSelectAllOnFocus(false);
        e.setImeOptions(EditorInfo.IME_ACTION_DONE);
        r.addView(e, Ui.weight(1));
        Runnable add = () -> {
            String s = e.getText().toString().trim();
            if (s.isEmpty()) return;
            st.addShopExtra(s);
            Ui.hideKeyboard(e);
            a.refresh();
        };
        e.setOnEditorActionListener((v, id, ev) -> {
            add.run();
            return true;
        });
        ImageView plus = new ImageView(a);
        plus.setImageDrawable(new Icon(Icon.PLUS, Ui.BG));
        plus.setPadding(Ui.dp(14), Ui.dp(14), Ui.dp(14), Ui.dp(14));
        plus.setBackground(Ui.pressable(Ui.rect(Ui.ACCENT, 12), 12));
        plus.setContentDescription("Ajouter");
        plus.setOnClickListener(v -> add.run());
        LinearLayout.LayoutParams lp = Ui.lp(Ui.dp(52), Ui.dp(52));
        lp.leftMargin = Ui.dp(8);
        r.addView(plus, lp);
        return r;
    }
}

package com.fonte.carnet.ui;

import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Wod;
import com.fonte.carnet.core.WodResult;
import com.fonte.carnet.core.Wods;

import java.util.ArrayList;
import java.util.List;

/** Liste de WOD : par famille (Heroes, Girls) ou par objectif. */
public class WodListScreen extends Screen {
    private final String kind;
    private final String tag;

    public WodListScreen(String kind, String tag) {
        this.kind = kind;
        this.tag = tag;
    }

    @Override
    public String title() {
        if (tag != null) return tag;
        if (Wod.HERO.equals(kind)) return "WOD Heroes";
        if (Wod.GIRL.equals(kind)) return "The Girls";
        return "Mes WOD";
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        String intro;
        List<Wod> list = new ArrayList<>();
        if (tag != null) {
            list.addAll(Wods.byTag(tag));
            for (Wod w : st.customWods) if (w.tags.contains(tag)) list.add(w);
            intro = "WOD de référence et WOD Fonte qui travaillent surtout : " + tag.toLowerCase(java.util.Locale.FRENCH) + ".";
        } else if (Wod.CUSTOM.equals(kind)) {
            list.addAll(st.customWods);
            intro = "Tes WOD enregistrés.";
        } else {
            list.addAll(Wods.byKind(kind));
            intro = Wod.HERO.equals(kind)
                    ? "Les WOD Heroes rendent hommage à des membres des forces armées, de la police ou des secours morts en service. "
                    + "Longs et exigeants : adapte charges et volume à ton niveau."
                    : "Les « Girls » sont les WOD de référence du CrossFit : idéaux pour mesurer tes progrès.";
        }
        TextView t = Ui.text(a, intro, 14, Ui.MUTED);
        t.setPadding(0, Ui.dp(4), 0, Ui.dp(12));
        c.addView(t);
        for (Wod w : list) c.addView(wodCard(this, w));
        if (list.isEmpty()) c.addView(Ui.text(a, "Aucun WOD ici pour l'instant.", 15, Ui.MUTED));
        return Ui.page(a, c);
    }

    static View wodCard(Screen s, Wod w) {
        MainActivity a = s.a;
        LinearLayout card = Ui.clickCard(a, v -> a.show(new WodScreen(w.id)));
        LinearLayout top = Ui.hbox(a);
        top.addView(Ui.medium(a, w.name, 17, Ui.TEXT), Ui.weight(1));
        top.addView(Ui.badge(a, w.formatLabel(), Ui.ACCENT, Ui.ACCENT_SOFT));
        card.addView(top);
        StringBuilder sb = new StringBuilder();
        for (String l : w.lines) {
            if (l.startsWith("(")) continue;
            if (sb.length() > 0) sb.append(" · ");
            sb.append(l);
        }
        TextView lines = Ui.text(a, sb.toString(), 13, Ui.MUTED);
        lines.setMaxLines(2);
        lines.setEllipsize(TextUtils.TruncateAt.END);
        lines.setPadding(0, Ui.dp(4), 0, 0);
        card.addView(lines);
        WodResult best = WodResult.best(s.st.wodResults, w.id, w.scoreType());
        if (best != null) {
            TextView b = Ui.medium(a, "Record : " + best.fullScore(), 13, Ui.ACCENT);
            b.setPadding(0, Ui.dp(4), 0, 0);
            card.addView(b);
        }
        return card;
    }
}

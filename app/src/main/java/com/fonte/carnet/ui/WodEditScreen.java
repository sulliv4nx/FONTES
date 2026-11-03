package com.fonte.carnet.ui;

import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Wod;
import com.fonte.carnet.core.Wods;

import java.util.ArrayList;
import java.util.List;

/** Création / modification d'un WOD perso. */
public class WodEditScreen extends Screen {
    private final Wod draft;
    private final boolean isNew;
    private String linesText;

    /** @param w WOD perso à modifier, ou null pour en créer un. */
    public WodEditScreen(Wod w) {
        isNew = w == null;
        if (w == null) {
            draft = new Wod();
            draft.kind = Wod.CUSTOM;
            draft.name = "";
            draft.format = Wod.FOR_TIME;
        } else {
            draft = w.copy();
        }
        StringBuilder sb = new StringBuilder();
        for (String l : draft.lines) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(l);
        }
        linesText = sb.toString();
    }

    /** Pré-rempli (WOD détecté dans la programmation du coach). */
    static WodEditScreen prefilled(Wod w) {
        WodEditScreen s = new WodEditScreen(null);
        s.draft.name = w.name;
        s.draft.format = w.format;
        s.draft.minutes = w.minutes;
        s.draft.every = w.every;
        s.draft.rounds = w.rounds;
        StringBuilder sb = new StringBuilder();
        for (String l : w.lines) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(l);
        }
        s.linesText = sb.toString();
        return s;
    }

    @Override
    public String title() {
        return isNew ? "Nouveau WOD" : "Modifier le WOD";
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        EditText name = Ui.field(a, "ex. WOD du lundi", Ui.TEXT_FIELD);
        name.setText(draft.name);
        Ui.onText(name, t -> draft.name = t.trim());
        c.addView(Ui.labelled(a, "Nom", name));

        c.addView(Ui.text(a, "Format", 13, Ui.MUTED));
        String[] formats = {Wod.FOR_TIME, Wod.AMRAP, Wod.EMOM, Wod.TABATA};
        String[] labels = {"For Time", "AMRAP", "EMOM", "Tabata"};
        View[] fc = new View[formats.length];
        for (int i = 0; i < formats.length; i++) {
            String f = formats[i];
            fc[i] = Ui.chip(a, labels[i], f.equals(draft.format), v -> {
                draft.format = f;
                if (Wod.AMRAP.equals(f) && draft.minutes <= 0) draft.minutes = 12;
                if (Wod.EMOM.equals(f) && draft.minutes <= 0) draft.minutes = 10;
                if (Wod.TABATA.equals(f) && draft.rounds <= 0) draft.rounds = 8;
                a.refresh();
            });
        }
        c.addView(Ui.chipRow(a, fc));

        switch (draft.format) {
            case Wod.AMRAP:
                c.addView(intField("Durée (minutes)", draft.minutes, v -> draft.minutes = v));
                break;
            case Wod.EMOM: {
                c.addView(intField("Durée totale (minutes)", draft.minutes, v -> draft.minutes = v));
                c.addView(Ui.text(a, "Un départ toutes les…", 13, Ui.MUTED));
                int[] everies = {60, 90, 120, 180};
                View[] ec = new View[everies.length];
                for (int i = 0; i < everies.length; i++) {
                    int e = everies[i];
                    ec[i] = Ui.chip(a, Fmt.clock(e * 1000L), draft.every == e, v -> {
                        draft.every = e;
                        a.refresh();
                    });
                }
                c.addView(Ui.chipRow(a, ec));
                CheckBox alt = new CheckBox(a);
                alt.setText("Une ligne différente à chaque intervalle (min 1, min 2…)");
                alt.setTextColor(Ui.TEXT);
                alt.setChecked(draft.alternate);
                alt.setOnCheckedChangeListener((b, on) -> draft.alternate = on);
                c.addView(alt);
                break;
            }
            case Wod.TABATA:
                c.addView(intField("Nombre de tours", draft.rounds > 0 ? draft.rounds : 8, v -> draft.rounds = v));
                break;
            default:
                c.addView(intField("Cap en minutes (0 = sans limite)", draft.minutes, v -> draft.minutes = v));
                break;
        }

        EditText lines = Ui.area(a, "Un mouvement par ligne, ex.\n5 tours :\n10 thrusters (43 / 30 kg)\n15 pull-ups", 6);
        lines.setText(linesText);
        Ui.onText(lines, t -> linesText = t);
        c.addView(Ui.labelled(a, "Déroulé", lines));

        c.addView(Ui.text(a, "Ce que ça travaille", 13, Ui.MUTED));
        View[] tc = new View[Wods.TAGS.length];
        for (int i = 0; i < Wods.TAGS.length; i++) {
            String tag = Wods.TAGS[i];
            tc[i] = Ui.chip(a, tag, draft.tags.contains(tag), v -> {
                if (draft.tags.contains(tag)) draft.tags.remove(tag);
                else draft.tags.add(tag);
                a.refresh();
            });
        }
        c.addView(Ui.chipRow(a, tc));

        EditText note = Ui.field(a, "charges, consignes…", Ui.TEXT_FIELD);
        note.setText(draft.note == null ? "" : draft.note);
        Ui.onText(note, t -> draft.note = t.trim().isEmpty() ? null : t.trim());
        c.addView(Ui.labelled(a, "Note (facultatif)", note));

        c.addView(Ui.button(a, "Enregistrer", Ui.PRIMARY, v -> save()));
        return Ui.page(a, c);
    }

    private interface IntSetter {
        void set(int v);
    }

    private LinearLayout intField(String label, int value, IntSetter setter) {
        EditText e = Ui.field(a, "0", Ui.INTEGER);
        e.setText(String.valueOf(value));
        Ui.onText(e, t -> setter.set(Fmt.parseInt(t)));
        return Ui.labelled(a, label, e);
    }

    private void save() {
        if (draft.name == null || draft.name.isEmpty()) {
            Ui.toast(a, "Donne un nom à ton WOD.");
            return;
        }
        List<String> lines = new ArrayList<>();
        for (String l : linesText.split("\n")) if (!l.trim().isEmpty()) lines.add(l.trim());
        if (lines.isEmpty()) {
            Ui.toast(a, "Ajoute au moins un mouvement.");
            return;
        }
        if ((Wod.AMRAP.equals(draft.format) || Wod.EMOM.equals(draft.format)) && (draft.minutes <= 0 || draft.minutes > 180)) {
            Ui.toast(a, "Indique une durée entre 1 et 180 minutes.");
            return;
        }
        if (Wod.TABATA.equals(draft.format) && draft.rounds <= 0) draft.rounds = 8;
        if (draft.minutes < 0 || draft.minutes > 600) draft.minutes = 0;
        draft.lines = lines;
        if (draft.tags.isEmpty()) draft.tags.add(Wods.CARDIO);
        Ui.hideKeyboard(a.getWindow().getDecorView());
        Wod saved = st.saveCustomWod(draft.copy());
        a.back();
        if (isNew) a.show(new WodScreen(saved.id));
        Ui.toast(a, "WOD enregistré.");
    }
}

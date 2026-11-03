package com.fonte.carnet.ui;

import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.fonte.carnet.core.BodyLog;
import com.fonte.carnet.core.Fmt;

import java.util.LinkedHashMap;
import java.util.Map;

/** Saisie de mensurations (toutes facultatives). */
public class MeasureEditScreen extends Screen {
    private final Map<String, String> values = new LinkedHashMap<>();

    @Override
    public String title() {
        return "Mensurations";
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        c.addView(Ui.hint(a, "Renseigne seulement ce que tu mesures. Mètre ruban à plat sur la peau, muscles relâchés, "
                + "toujours au même endroit (taille : au niveau du nombril)."));
        LinearLayout row = null;
        int i = 0;
        for (String key : BodyLog.MEASURES) {
            double last = st.measure(key, 0);
            EditText e = Ui.field(a, last > 0 ? "dernier : " + Fmt.kg(last) : BodyLog.unit(key), Ui.DECIMAL);
            if (values.containsKey(key)) e.setText(values.get(key));
            Ui.onText(e, t -> values.put(key, t));
            LinearLayout field = Ui.labelled(a, key + " (" + BodyLog.unit(key) + ")", e);
            if (i % 2 == 0) {
                row = Ui.hbox(a);
                row.setGravity(android.view.Gravity.TOP);
                c.addView(row);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
                p.rightMargin = Ui.dp(10);
                row.addView(field, p);
            } else {
                row.addView(field, new LinearLayout.LayoutParams(0, -2, 1));
            }
            i++;
        }
        if (i % 2 == 1 && row != null) row.addView(new View(a), new LinearLayout.LayoutParams(0, 1, 1));
        c.addView(Ui.button(a, "Enregistrer", Ui.PRIMARY, v -> save()));
        return Ui.page(a, c);
    }

    private void save() {
        BodyLog.Measures m = new BodyLog.Measures();
        for (Map.Entry<String, String> en : values.entrySet()) {
            double v = Fmt.parse(en.getValue());
            if (Double.isNaN(v) || v <= 0) continue;
            boolean pct = BodyLog.BODY_FAT.equals(en.getKey());
            if ((pct && v > 70) || (!pct && (v < 10 || v > 250))) {
                Ui.toast(a, "Valeur improbable pour « " + en.getKey() + " ».");
                return;
            }
            m.values.put(en.getKey(), v);
        }
        if (m.values.isEmpty()) {
            Ui.toast(a, "Renseigne au moins une mesure.");
            return;
        }
        st.addMeasures(m);
        Ui.hideKeyboard(a.getWindow().getDecorView());
        a.back();
        Ui.toast(a, "Mensurations enregistrées.");
    }
}

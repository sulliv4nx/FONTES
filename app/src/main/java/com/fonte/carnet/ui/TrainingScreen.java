package com.fonte.carnet.ui;

import android.view.View;
import android.widget.LinearLayout;

/** Onglet Sport : Muscu, CrossFit et Renfo dans un sélecteur à segments. */
public class TrainingScreen extends Screen {
    public static final int MUSCU = 0;
    public static final int CROSSFIT = 1;
    public static final int RENFO = 2;
    private static final String[] LABELS = {"Muscu", "CrossFit", "Renfo"};

    /** Segment affiché (conservé quand on revient sur l'onglet). */
    static int segment = MUSCU;
    private Screen sub;

    @Override
    public String title() {
        return "Entraînement";
    }

    private Screen sub() {
        if (sub == null) {
            sub = segment == CROSSFIT ? new CrossFitScreen() : segment == RENFO ? new RenfoScreen() : new HomeScreen();
            sub.attach(a);
        }
        return sub;
    }

    @Override
    public int actionIcon() {
        return sub().actionIcon();
    }

    @Override
    public void onAction() {
        sub().onAction();
    }

    @Override
    public View build() {
        LinearLayout root = Ui.vbox(a);
        int pad = Ui.pad(a);
        root.addView(Ui.segmented(a, LABELS, segment, i -> {
            if (i == segment) return;
            segment = i;
            if (sub != null) sub.onHide();
            sub = null;
            a.refresh();
        }), Ui.mlp(-1, -2, pad, 4, pad, 4));
        root.addView(sub().build(), new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }

    @Override
    public void onHide() {
        if (sub != null) sub.onHide();
    }

    @Override
    public void tick() {
        if (sub != null) sub.tick();
    }
}

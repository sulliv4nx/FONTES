package com.fonte.carnet.ui;

import android.view.View;
import android.widget.LinearLayout;

/** Onglet Progrès : historique unifié et records. */
public class ProgressScreen extends Screen {
    private static final String[] LABELS = {"Historique", "Records"};
    static int segment = 0;
    private Screen sub;

    @Override
    public String title() {
        return "Progrès";
    }

    private Screen sub() {
        if (sub == null) {
            sub = segment == 1 ? new RecordsScreen() : new HistoryScreen();
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
            sub = null;
            a.refresh();
        }), Ui.mlp(-1, -2, pad, 4, pad, 4));
        root.addView(sub().build(), new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }
}

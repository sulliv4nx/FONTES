package com.fonte.carnet.ui;

import android.widget.LinearLayout;
import android.widget.ScrollView;

/** Écran défilant qui garde sa position quand il est reconstruit (case cochée, retour d'un sous-écran…). */
abstract class ScrollScreen extends Screen {
    private ScrollView scroll;
    private int scrollY;

    protected ScrollView page(LinearLayout content) {
        ScrollView sv = Ui.page(a, content);
        scroll = sv;
        final int y = scrollY;
        if (y > 0) sv.post(() -> sv.scrollTo(0, y));
        return sv;
    }

    /** Revenir en haut au prochain affichage (changement de jour, par exemple). */
    protected void scrollTop() {
        scrollY = 0;
    }

    @Override
    public void onHide() {
        if (scroll != null) scrollY = scroll.getScrollY();
    }
}

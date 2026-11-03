package com.fonte.carnet.ui;

import android.view.View;

import com.fonte.carnet.data.Store;

/** Un écran de l'appli. MainActivity gère la pile d'écrans et l'en-tête. */
public abstract class Screen {
    protected MainActivity a;
    protected Store st;

    void attach(MainActivity activity) {
        this.a = activity;
        this.st = activity.store;
    }

    public abstract String title();

    /** Construit (ou reconstruit) le contenu de l'écran. */
    public abstract View build();

    /** Icône d'action en haut à droite (Icon.*), ou -1. */
    public int actionIcon() {
        return -1;
    }

    public void onAction() {
    }

    /** true si l'écran a géré le retour lui-même. */
    public boolean onBack() {
        return false;
    }

    /** Appelé chaque seconde tant que l'écran est affiché. */
    public void tick() {
    }

    public void onHide() {
    }

    /** Résultat d'une activité lancée par l'écran (photo, fichier…). true si géré. */
    public boolean onActivityResult(int request, int result, android.content.Intent data) {
        return false;
    }
}

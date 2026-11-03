package com.fonte.carnet.core;

/** Réglages de l'utilisateur. */
public class Settings {
    /** Durée du minuteur de repos, en secondes (0 = désactivé). */
    public int restSeconds = 120;
    /** Pas d'arrondi des charges calculées, en kg. */
    public double rounding = 2.5;
    /** Vibrer à la fin du repos. */
    public boolean vibrate = true;
}

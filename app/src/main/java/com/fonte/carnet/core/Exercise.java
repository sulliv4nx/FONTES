package com.fonte.carnet.core;

/** Un mouvement de la bibliothèque (intégré ou créé par l'utilisateur). */
public class Exercise {
    public long id;
    public String name;
    public String category;
    public boolean custom;

    public Exercise(long id, String name, String category, boolean custom) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.custom = custom;
    }
}

package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.List;

/** Modèle de séance réutilisable (créé par l'utilisateur, ou séance type intégrée). */
public class Template {
    public long id;
    public String name;
    public List<ExerciseEntry> exercises = new ArrayList<>();
    /** Séances types intégrées uniquement (non enregistré). */
    public String group;
    public String description;

    public boolean builtin() {
        return id < 0;
    }
}

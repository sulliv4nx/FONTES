package com.fonte.carnet.core;

import java.util.ArrayList;
import java.util.List;

/** Modèle de séance réutilisable. */
public class Template {
    public long id;
    public String name;
    public List<ExerciseEntry> exercises = new ArrayList<>();
}

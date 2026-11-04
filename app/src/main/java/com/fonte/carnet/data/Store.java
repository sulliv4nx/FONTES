package com.fonte.carnet.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;
import android.util.Log;

import com.fonte.carnet.core.BodyLog;
import com.fonte.carnet.core.Days;
import com.fonte.carnet.core.Exercise;
import com.fonte.carnet.core.Food;
import com.fonte.carnet.core.FoodEntry;
import com.fonte.carnet.core.Foods;
import com.fonte.carnet.core.Nutrition;
import com.fonte.carnet.core.Profile;
import com.fonte.carnet.core.ExerciseEntry;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Composer;
import com.fonte.carnet.core.Library;
import com.fonte.carnet.core.MealPlan;
import com.fonte.carnet.core.MealPlanner;
import com.fonte.carnet.core.MealPrefs;
import com.fonte.carnet.core.Program;
import com.fonte.carnet.core.Recipe;
import com.fonte.carnet.core.RenfoLog;
import com.fonte.carnet.core.Routine;
import com.fonte.carnet.core.Session;
import com.fonte.carnet.core.SetEntry;
import com.fonte.carnet.core.Settings;
import com.fonte.carnet.core.Stats;
import com.fonte.carnet.core.Template;
import com.fonte.carnet.core.Wod;
import com.fonte.carnet.core.WodResult;
import com.fonte.carnet.core.Wods;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Toutes les données de l'appli, gardées en mémoire et enregistrées dans un fichier JSON
 * privé (écriture atomique). Rien ne quitte le téléphone, sauf export manuel.
 */
public final class Store {
    private static final String TAG = "Fonte";
    private static Store instance;

    public static synchronized Store get(Context c) {
        if (instance == null) instance = new Store(c.getApplicationContext());
        return instance;
    }

    private final File dir;
    private final AtomicFile file;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable saver = this::save;
    private boolean dirty;

    public final List<Exercise> exercises = new ArrayList<>();
    /** Séances terminées, de la plus récente à la plus ancienne. */
    public final List<Session> sessions = new ArrayList<>();
    public final List<Template> templates = new ArrayList<>();
    /** WOD créés par l'utilisateur. */
    public final List<Wod> customWods = new ArrayList<>();
    /** Scores de WOD, du plus récent au plus ancien. */
    public final List<WodResult> wodResults = new ArrayList<>();
    /** Circuits de renfo terminés, du plus récent au plus ancien. */
    public final List<RenfoLog> renfoLogs = new ArrayList<>();
    /** Nutrition et suivi corporel (v3). */
    public Profile profile = new Profile();
    public final List<Food> customFoods = new ArrayList<>();
    public final List<FoodEntry> foodEntries = new ArrayList<>();
    public final List<BodyLog.Water> water = new ArrayList<>();
    public final List<BodyLog.Activity> activities = new ArrayList<>();
    /** Pesées, de la plus récente à la plus ancienne. */
    public final List<BodyLog.Weight> weights = new ArrayList<>();
    /** Mensurations, de la plus récente à la plus ancienne. */
    public final List<BodyLog.Measures> measures = new ArrayList<>();
    /** Mes repas (v4) : réglages et plan de la semaine. */
    public MealPrefs mealPrefs = new MealPrefs();
    public MealPlan mealPlan;
    public Session current;
    public Program program;
    public Settings settings = new Settings();
    long nextId = 1;
    long nextExerciseId = Library.FIRST_CUSTOM_ID;
    /** Renseigné si le fichier de données n'a pas pu être lu (une copie a été mise de côté). */
    public String loadError;

    private Store(Context c) {
        dir = c.getFilesDir();
        file = new AtomicFile(new File(dir, "fonte.json"));
        load();
    }

    // ---------------------------------------------------------------- chargement / enregistrement

    private void reset() {
        exercises.clear();
        exercises.addAll(Library.defaults());
        sessions.clear();
        templates.clear();
        customWods.clear();
        wodResults.clear();
        renfoLogs.clear();
        profile = new Profile();
        customFoods.clear();
        foodEntries.clear();
        water.clear();
        activities.clear();
        weights.clear();
        measures.clear();
        mealPrefs = new MealPrefs();
        mealPlan = null;
        current = null;
        program = null;
        settings = new Settings();
        nextId = 1;
        nextExerciseId = Library.FIRST_CUSTOM_ID;
    }

    private void load() {
        reset();
        if (!file.getBaseFile().exists()) return;
        try {
            byte[] bytes = file.readFully();
            JsonCodec.read(this, new JSONObject(new String(bytes, StandardCharsets.UTF_8)));
            sortSessions();
        } catch (Exception e) {
            Log.e(TAG, "Lecture impossible", e);
            loadError = e.getMessage();
            backupUnreadable();
            reset();
        }
    }

    /** Garde une copie du fichier illisible avant qu'il ne soit écrasé. */
    private void backupUnreadable() {
        try {
            byte[] bytes = file.readFully();
            File copy = new File(dir, "fonte-illisible-" + System.currentTimeMillis() + ".json");
            FileOutputStream out = new FileOutputStream(copy);
            try {
                out.write(bytes);
            } finally {
                out.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Copie impossible", e);
        }
    }

    public void save() {
        handler.removeCallbacks(saver);
        dirty = false;
        FileOutputStream out = null;
        try {
            byte[] bytes = JsonCodec.write(this).toString().getBytes(StandardCharsets.UTF_8);
            out = file.startWrite();
            out.write(bytes);
            file.finishWrite(out);
        } catch (Exception e) {
            Log.e(TAG, "Enregistrement impossible", e);
            if (out != null) file.failWrite(out);
        }
    }

    /** Enregistrement différé (saisie au clavier). */
    public void saveSoon() {
        dirty = true;
        handler.removeCallbacks(saver);
        handler.postDelayed(saver, 800);
    }

    /** Enregistre tout de suite s'il reste des modifications en attente. */
    public void flush() {
        if (dirty) save();
    }

    public String exportJson() throws JSONException {
        return JsonCodec.write(this).toString(1);
    }

    /** Remplace toutes les données par celles de la sauvegarde. En cas d'erreur, rien n'est modifié. */
    public void importJson(String json) throws JSONException {
        JSONObject backup = JsonCodec.write(this);
        try {
            JSONObject incoming = new JSONObject(json);
            reset();
            JsonCodec.read(this, incoming);
            sortSessions();
        } catch (JSONException | RuntimeException e) {
            reset();
            JsonCodec.read(this, backup);
            sortSessions();
            throw e instanceof JSONException ? (JSONException) e : new JSONException(e.toString());
        }
        save();
    }

    // ---------------------------------------------------------------- exercices

    public long newId() {
        return nextId++;
    }

    public Exercise exercise(long id) {
        for (Exercise e : exercises) if (e.id == id) return e;
        return null;
    }

    public String name(long id) {
        Exercise e = exercise(id);
        return e == null ? "Exercice supprimé" : e.name;
    }

    public Stats.Names names() {
        return this::name;
    }

    public Exercise findByName(String name) {
        for (Exercise e : exercises) if (e.name.equalsIgnoreCase(name.trim())) return e;
        return null;
    }

    public Exercise addExercise(String name) {
        Exercise e = new Exercise(nextExerciseId++, name.trim(), Library.CUSTOM, true);
        exercises.add(e);
        save();
        return e;
    }

    public boolean isUsed(long exerciseId) {
        List<List<ExerciseEntry>> all = new ArrayList<>();
        for (Session s : sessions) all.add(s.exercises);
        if (current != null) all.add(current.exercises);
        for (Template t : templates) all.add(t.exercises);
        if (program != null) for (Program.Day d : program.days) all.add(d.exercises);
        for (List<ExerciseEntry> l : all) for (ExerciseEntry e : l) if (e.exerciseId == exerciseId) return true;
        return false;
    }

    public void deleteExercise(Exercise e) {
        if (!e.custom || isUsed(e.id)) return;
        exercises.remove(e);
        save();
    }

    // ---------------------------------------------------------------- séances

    public Session startSession(String name, List<ExerciseEntry> plan, long programKey, int programDay) {
        Session s = new Session();
        s.id = newId();
        s.name = name;
        s.start = System.currentTimeMillis();
        for (ExerciseEntry e : plan) s.exercises.add(prefill(e.copy(false), s.id));
        s.programKey = programKey;
        s.programDay = programDay;
        current = s;
        save();
        return s;
    }

    /** Termine la séance en cours. Renvoie la séance enregistrée, ou null si elle était vide. */
    public Session finishCurrent() {
        Session s = current;
        if (s == null) return null;
        current = null;
        s.dropUndone();
        if (s.exercises.isEmpty()) {
            save();
            return null;
        }
        s.end = System.currentTimeMillis();
        s.records = Stats.newRecords(s, sessions, names());
        Program.Day day = programDay(s);
        if (day != null && !day.isDone()) day.sessionId = s.id;
        sessions.add(s);
        sortSessions();
        save();
        return s;
    }

    public void cancelCurrent() {
        current = null;
        save();
    }

    public void deleteSession(Session s) {
        sessions.remove(s);
        if (program != null) for (Program.Day d : program.days) if (d.sessionId == s.id) d.sessionId = 0;
        save();
    }

    public Session session(long id) {
        for (Session s : sessions) if (s.id == id) return s;
        return null;
    }

    /** Jour du programme en cours auquel la séance est rattachée, ou null. */
    public Program.Day programDay(Session s) {
        if (program == null || s.programDay < 0 || s.programKey != program.startedAt) return null;
        if (s.programDay >= program.days.size()) return null;
        return program.days.get(s.programDay);
    }

    private void sortSessions() {
        Collections.sort(sessions, (a, b) -> Long.compare(b.start, a.start));
        Collections.sort(wodResults, (a, b) -> Long.compare(b.date, a.date));
        Collections.sort(renfoLogs, (a, b) -> Long.compare(b.date, a.date));
        Collections.sort(weights, (a, b) -> Long.compare(b.time, a.time));
        Collections.sort(measures, (a, b) -> Long.compare(b.time, a.time));
    }

    // ---------------------------------------------------------------- corps

    /** Dernier poids connu (kg), ou 0. */
    public double weight() {
        return weights.isEmpty() ? 0 : weights.get(0).kg;
    }

    /** Poids pour les calculs : dernière pesée, sinon valeur par défaut. */
    public double weightForCalc() {
        double w = weight();
        return w > 0 ? w : Nutrition.DEFAULT_WEIGHT;
    }

    public BodyLog.Weight addWeight(double kg, int day) {
        BodyLog.Weight w = new BodyLog.Weight();
        w.id = newId();
        w.kg = kg;
        w.day = day;
        w.time = day == Days.today() ? System.currentTimeMillis() : Days.start(day) + 8 * 3600_000L;
        weights.add(w);
        sortSessions();
        save();
        return w;
    }

    public void deleteWeight(BodyLog.Weight w) {
        weights.remove(w);
        save();
    }

    /** Poids enregistré il y a environ n jours (pesée la plus proche avant cette date), ou 0. */
    public double weightDaysAgo(int days) {
        long limit = System.currentTimeMillis() - days * 86_400_000L;
        for (BodyLog.Weight w : weights) if (w.time <= limit) return w.kg;
        return 0;
    }

    public BodyLog.Measures addMeasures(BodyLog.Measures m) {
        m.id = newId();
        if (m.time == 0) m.time = System.currentTimeMillis();
        if (m.day == 0) m.day = Days.key(m.time);
        measures.add(m);
        sortSessions();
        save();
        return m;
    }

    public void deleteMeasures(BodyLog.Measures m) {
        measures.remove(m);
        save();
    }

    /** Dernière valeur d'une mensuration (0 si jamais mesurée). skip = nombre de valeurs récentes à ignorer. */
    public double measure(String key, int skip) {
        int n = 0;
        for (BodyLog.Measures m : measures) {
            Double v = m.values.get(key);
            if (v == null || v <= 0) continue;
            if (n++ == skip) return v;
        }
        return 0;
    }

    /** Masse grasse : mesurée si disponible, sinon estimée (US Navy), sinon 0. */
    public double bodyFat() {
        double measured = measure(BodyLog.BODY_FAT, 0);
        if (measured > 0) return measured;
        return Nutrition.navyBodyFat(profile.sex, profile.heightCm, measure(BodyLog.NECK, 0),
                measure(BodyLog.WAIST, 0), measure(BodyLog.HIPS, 0));
    }

    // ---------------------------------------------------------------- nutrition

    public Food food(String id) {
        if (id == null) return null;
        Food f = Foods.find(id);
        if (f != null) return f;
        for (Food c : customFoods) if (id.equals(c.id)) return c;
        return null;
    }

    public Food foodByBarcode(String code) {
        if (code == null) return null;
        for (Food c : customFoods) if (code.equals(c.barcode)) return c;
        return null;
    }

    public List<Food> allFoods() {
        List<Food> l = new ArrayList<>(customFoods);
        l.addAll(Foods.all());
        return l;
    }

    public Food saveCustomFood(Food f) {
        f.custom = true;
        f.category = Foods.MINE;
        if (f.id == null || f.id.isEmpty()) {
            f.id = "perso_" + newId();
            customFoods.add(0, f);
        } else {
            boolean replaced = false;
            for (int i = 0; i < customFoods.size(); i++) {
                if (customFoods.get(i).id.equals(f.id)) {
                    customFoods.set(i, f);
                    replaced = true;
                }
            }
            if (!replaced) customFoods.add(0, f);
        }
        save();
        return f;
    }

    public void deleteCustomFood(Food f) {
        customFoods.remove(f);
        save();
    }

    /** Aliments utilisés récemment (sans doublon, du plus récent au plus ancien). */
    public List<Food> recentFoods(int max) {
        List<Food> out = new ArrayList<>();
        List<FoodEntry> sorted = new ArrayList<>(foodEntries);
        Collections.sort(sorted, (a, b) -> Long.compare(b.time, a.time));
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (FoodEntry e : sorted) {
            if (out.size() >= max) break;
            if (e.foodId == null || !seen.add(e.foodId)) continue;
            Food f = food(e.foodId);
            if (f != null) out.add(f);
        }
        return out;
    }

    public FoodEntry addFood(FoodEntry e) {
        e.id = newId();
        if (e.time == 0) e.time = System.currentTimeMillis();
        foodEntries.add(e);
        save();
        return e;
    }

    public void deleteFood(FoodEntry e) {
        foodEntries.remove(e);
        if (mealPlan != null) {
            for (MealPlan.Meal m : mealPlan.meals) {
                if (m.entries.remove(Long.valueOf(e.id)) && m.entries.isEmpty()) m.eaten = false;
            }
        }
        if (e.photo != null) {
            boolean shared = false;
            for (FoodEntry o : foodEntries) if (e.photo.equals(o.photo)) shared = true;
            if (!shared) {
                File f = new File(e.photo);
                if (f.exists() && !f.delete()) Log.w(TAG, "photo non supprimée");
            }
        }
        save();
    }

    public List<FoodEntry> foods(int day, String meal) {
        List<FoodEntry> out = new ArrayList<>();
        for (FoodEntry e : foodEntries) if (e.day == day && (meal == null || meal.equals(e.meal))) out.add(e);
        Collections.sort(out, (a, b) -> Long.compare(a.time, b.time));
        return out;
    }

    public void addWater(int ml, int day) {
        BodyLog.Water w = new BodyLog.Water();
        w.id = newId();
        w.ml = ml;
        w.day = day;
        w.time = System.currentTimeMillis();
        water.add(w);
        save();
    }

    /** Retire le dernier ajout d'eau du jour. */
    public boolean undoWater(int day) {
        for (int i = water.size() - 1; i >= 0; i--) {
            if (water.get(i).day == day) {
                water.remove(i);
                save();
                return true;
            }
        }
        return false;
    }

    public BodyLog.Activity addActivity(String name, int minutes, int kcal, int day) {
        BodyLog.Activity a = new BodyLog.Activity();
        a.id = newId();
        a.name = name;
        a.minutes = minutes;
        a.kcal = kcal;
        a.day = day;
        a.time = System.currentTimeMillis();
        activities.add(a);
        save();
        return a;
    }

    public void deleteActivity(BodyLog.Activity a) {
        activities.remove(a);
        save();
    }

    /** Séance, WOD ou renfo ce jour-là. */
    public boolean trained(int day) {
        for (Session s : sessions) if (Days.key(s.start) == day) return true;
        for (WodResult r : wodResults) if (Days.key(r.date) == day) return true;
        for (RenfoLog l : renfoLogs) if (Days.key(l.date) == day) return true;
        return false;
    }

    /** Séance terminée, WOD ou renfo noté ce jour-là (une séance en cours ne compte pas encore). */
    public boolean trainedDone(int day) {
        for (Session s : sessions) if (!s.inProgress() && Days.key(s.start) == day) return true;
        for (WodResult r : wodResults) if (Days.key(r.date) == day) return true;
        for (RenfoLog l : renfoLogs) if (Days.key(l.date) == day) return true;
        return false;
    }

    /** Dépense nette (repos déduit) des séances et activités d'un jour. */
    public List<Nutrition.Burn> burned(int day) {
        return Nutrition.burned(day, sessions, wodResults, renfoLogs, activities, weightForCalc());
    }

    /** Jour d'entraînement prévu (jours repérés ou choisis dans les réglages). */
    public boolean plannedTraining(int day) {
        int mask = Math.max(0, mealPrefs().trainingMask);
        return (mask & (1 << MealPlanner.weekday(day))) != 0;
    }

    /**
     * Séance type : moyenne {kcal nettes, minutes} des jours entraînés des 4 dernières semaines,
     * ou 1 h de musculation s'il n'y a pas d'historique.
     */
    public int[] typicalSession() {
        int today = Days.today();
        int n = 0, k = 0, m = 0;
        for (int i = 1; i <= 28; i++) {
            int d = Days.add(today, -i);
            if (!trainedDone(d)) continue;
            List<Nutrition.Burn> b = burned(d);
            n++;
            k += Nutrition.total(b);
            m += Nutrition.minutes(b);
        }
        if (n == 0) return new int[]{Nutrition.netKcal(Nutrition.MET_MUSCU, weightForCalc(), 60), 60};
        return new int[]{Math.round(k / (float) n), Math.round(m / (float) n)};
    }

    private Nutrition.Targets compute(int sessionKcal, int sessionMinutes, boolean planned) {
        return Nutrition.targets(profile, weightForCalc(), profile.age(System.currentTimeMillis()), bodyFat(),
                sessionKcal, sessionMinutes, planned);
    }

    /**
     * Objectifs d'un jour : jour sans séance + calories nettes des séances notées. Un jour d'entraînement
     * prévu (aujourd'hui ou plus tard) sans séance notée compte une séance type.
     */
    public Nutrition.Targets targets(int day) {
        List<Nutrition.Burn> b = burned(day);
        int k = Nutrition.total(b);
        int m = Nutrition.minutes(b);
        boolean planned = false;
        if (!trainedDone(day) && day >= Days.today() && plannedTraining(day)) {
            int[] s = typicalSession();
            k += s[0];
            m += s[1];
            planned = true;
        }
        return compute(k, m, planned);
    }

    /** Objectifs d'un jour sans séance. */
    public Nutrition.Targets restTargets() {
        return compute(0, 0, false);
    }

    /** Objectifs d'un jour avec une séance type. */
    public Nutrition.Targets trainingTargets() {
        int[] s = typicalSession();
        return compute(s[0], s[1], true);
    }

    public Nutrition.Macros targetMacros() {
        return restTargets().macros();
    }

    public Nutrition.Macros targetMacros(int day) {
        return targets(day).macros();
    }

    public int waterTarget(int day) {
        return targets(day).water;
    }

    /** Pente des pesées comparée au rythme visé, avec la correction proposée. */
    public Nutrition.Trend weightTrend() {
        return Nutrition.trend(profile, weights, weightForCalc(), Days.today());
    }

    /** Valide une correction d'après les pesées ; les pesées suivantes repartent de zéro. */
    public void applyCorrection(int delta) {
        profile.kcalAdjust = Math.max(-600, Math.min(600, profile.kcalAdjust + delta));
        profile.adjustDay = Days.today();
        save();
    }

    public void resetCorrection() {
        profile.kcalAdjust = 0;
        profile.adjustDay = Days.today();
        save();
    }

    // ---------------------------------------------------------------- mes repas

    /** Réglages des repas ; les jours d'entraînement sont devinés d'après l'historique la première fois. */
    public MealPrefs mealPrefs() {
        if (mealPrefs.trainingMask < 0) {
            int today = Days.today();
            boolean[] trained = new boolean[28];
            for (int i = 0; i < trained.length; i++) trained[i] = trained(Days.add(today, -i));
            mealPrefs.trainingMask = MealPlanner.detectTraining(trained, today);
        }
        return mealPrefs;
    }

    /** Objectifs d'un jour sans séance, pour le plan de repas. */
    public MealPlanner.Targets mealTargets() {
        Nutrition.Targets t = restTargets();
        return new MealPlanner.Targets(t.kcal, t.protein, t.carbs, t.fat);
    }

    /** Objectifs d'un jour d'entraînement (séance type), pour le plan de repas. */
    public MealPlanner.Targets mealTrainingTargets() {
        Nutrition.Targets t = trainingTargets();
        return new MealPlanner.Targets(t.kcal, t.protein, t.carbs, t.fat);
    }

    public MealPlan generateMealPlan(MealPrefs prefs, int start) {
        mealPrefs = prefs;
        mealPlan = MealPlanner.generate(prefs, mealTargets(), mealTrainingTargets(), start, System.nanoTime());
        mealPlan.created = System.currentTimeMillis();
        save();
        return mealPlan;
    }

    /** Coche un repas du plan : il est ajouté au journal (et retiré si on décoche). */
    public void setMealEaten(MealPlan.Meal m, boolean eaten) {
        if (eaten == m.eaten) return;
        if (eaten) {
            Nutrition.Totals t = m.totals();
            FoodEntry e = new FoodEntry();
            e.id = newId();
            e.day = m.day;
            e.time = Days.start(m.day) + MealPlan.slotHour(m.slot) * 3_600_000L;
            e.meal = MealPlan.journalMeal(m.slot);
            e.name = m.name;
            e.grams = m.grams();
            e.kcal = t.kcal;
            e.protein = t.protein;
            e.carbs = t.carbs;
            e.fat = t.fat;
            foodEntries.add(e);
            m.entries.clear();
            m.entries.add(e.id);
            m.eaten = true;
        } else {
            for (Long id : m.entries) {
                for (int i = foodEntries.size() - 1; i >= 0; i--) {
                    if (foodEntries.get(i).id == id) foodEntries.remove(i);
                }
            }
            m.entries.clear();
            m.eaten = false;
        }
        save();
    }

    /** Remplace un repas du plan par un autre (le journal suit si le repas était coché). */
    public void replaceMeal(MealPlan.Meal old, MealPlan.Meal fresh) {
        if (mealPlan == null) return;
        int i = mealPlan.meals.indexOf(old);
        if (i < 0) return;
        boolean wasEaten = old.eaten;
        if (wasEaten) setMealEaten(old, false);
        mealPlan.meals.set(i, fresh);
        if (wasEaten) setMealEaten(fresh, true);
        save();
    }

    /** Objectif d'un créneau (kcal, protéines, lipides) : celui du plan s'il y en a un, sinon calculé. */
    public double[] slotTarget(String slot) {
        int today = Days.today();
        if (mealPlan != null) {
            double k = 0, p = 0, f = 0;
            int n = 0;
            for (MealPlan.Meal m : mealPlan.meals) {
                if (!m.slot.equals(slot) || m.day < today || m.targetKcal <= 0) continue;
                k += m.targetKcal;
                p += m.targetProtein;
                f += m.targetFat;
                n++;
            }
            if (n > 0) return new double[]{k / n, p / n, f / n};
        }
        MealPrefs prefs = mealPrefs();
        java.util.List<String> slots = MealPlanner.slots(prefs.meals, MealPlan.SHAKER.equals(slot));
        if (!slots.contains(slot)) slots = MealPlanner.slots(5, MealPlan.SHAKER.equals(slot));
        MealPlanner.Targets t = mealTargets();
        double share = MealPlanner.share(slots, slot);
        return new double[]{t.kcal * share, t.protein * MealPlanner.proteinShare(slots, slot), t.fat * share};
    }

    /** Plan en cours qui couvre aujourd'hui (il est créé avec les réglages actuels s'il n'y en a pas). */
    public MealPlan ensureMealPlan() {
        int today = Days.today();
        if (mealPlan == null || today > mealPlan.end()) generateMealPlan(mealPrefs(), today);
        return mealPlan;
    }

    /** Les ingrédients choisis sont notés comme « déjà là » dans la liste de courses. */
    private void markOwned(java.util.Collection<String> owned) {
        if (mealPlan == null || owned == null) return;
        java.util.Set<String> used = new java.util.HashSet<>();
        for (MealPlan.Meal m : mealPlan.meals) for (MealPlan.Item i : m.items) used.add(i.foodId);
        for (String id : owned) if (used.contains(id)) mealPlan.checked.add(id);
    }

    /** Remplit la semaine avec les plats validés ; renvoie le nombre de repas remplacés. */
    public int placeWeek(java.util.List<Recipe> dishes, java.util.Collection<String> owned) {
        MealPlan plan = ensureMealPlan();
        java.util.List<Composer.Placement> list = Composer.fillWeek(plan, dishes, Days.today(), mealPrefs.batch);
        for (Composer.Placement p : list) replaceMeal(p.old, p.fresh);
        markOwned(owned);
        save();
        return list.size();
    }

    /** Place un plat à un repas précis du plan. */
    public MealPlan.Meal placeOne(Recipe dish, MealPlan.Meal target, java.util.Collection<String> owned) {
        MealPlan.Meal fresh = Composer.fit(dish, target);
        replaceMeal(target, fresh);
        markOwned(owned);
        save();
        return fresh;
    }

    public void toggleShop(String key) {
        if (mealPlan == null) return;
        if (key.startsWith("extra:")) {
            int i = Integer.parseInt(key.substring(6));
            if (i >= 0 && i < mealPlan.extras.size()) mealPlan.extras.get(i).checked = !mealPlan.extras.get(i).checked;
        } else if (!mealPlan.checked.remove(key)) {
            mealPlan.checked.add(key);
        }
        save();
    }

    public void addShopExtra(String name) {
        if (mealPlan == null) return;
        MealPlan.Extra x = new MealPlan.Extra();
        x.name = name;
        mealPlan.extras.add(x);
        save();
    }

    public void deleteShopExtra(int index) {
        if (mealPlan == null || index < 0 || index >= mealPlan.extras.size()) return;
        mealPlan.extras.remove(index);
        save();
    }

    public void uncheckShopping() {
        if (mealPlan == null) return;
        mealPlan.checked.clear();
        for (MealPlan.Extra x : mealPlan.extras) x.checked = false;
        save();
    }

    /**
     * Séries sans charge (séance type, proposition, programmation sans kg) : on reprend la charge
     * de la dernière fois pour la même série, pour n'avoir qu'à ajuster.
     */
    private ExerciseEntry prefill(ExerciseEntry e, long sessionId) {
        boolean empty = true;
        for (SetEntry s : e.sets) if (s.weight > 0) empty = false;
        if (!empty) return e;
        List<SetEntry> last = Stats.lastPerformance(e.exerciseId, sessions, sessionId);
        if (last == null || last.isEmpty()) return e;
        for (int i = 0; i < e.sets.size(); i++) {
            SetEntry ref = last.get(Math.min(i, last.size() - 1));
            if (ref.weight > 0) e.sets.get(i).weight = ref.weight;
        }
        return e;
    }

    // ---------------------------------------------------------------- CrossFit et renfo

    public Wod wod(String id) {
        if (id == null) return null;
        Wod w = Wods.find(id);
        if (w != null) return w;
        for (Wod c : customWods) if (id.equals(c.id)) return c;
        return null;
    }

    public Wod saveCustomWod(Wod w) {
        w.kind = Wod.CUSTOM;
        if (w.id == null || w.id.isEmpty()) {
            w.id = "perso_" + newId();
            customWods.add(w);
        } else {
            boolean replaced = false;
            for (int i = 0; i < customWods.size(); i++) {
                if (customWods.get(i).id.equals(w.id)) {
                    customWods.set(i, w);
                    replaced = true;
                }
            }
            if (!replaced) customWods.add(w);
        }
        for (WodResult r : wodResults) if (w.id.equals(r.wodId)) r.wodName = w.name;
        save();
        return w;
    }

    public void deleteCustomWod(Wod w) {
        customWods.remove(w);
        save();
    }

    public List<WodResult> results(String wodId) {
        List<WodResult> out = new ArrayList<>();
        for (WodResult r : wodResults) if (r.wodId.equals(wodId)) out.add(r);
        return out;
    }

    /** Enregistre un score. Renvoie true si c'est le meilleur score de ce WOD (hors première fois). */
    public boolean addWodResult(WodResult r) {
        WodResult previousBest = WodResult.best(wodResults, r.wodId, r.type);
        r.id = newId();
        wodResults.add(r);
        sortSessions();
        save();
        return previousBest != null && WodResult.better(r, previousBest);
    }

    public void deleteWodResult(WodResult r) {
        wodResults.remove(r);
        save();
    }

    public WodResult wodResult(long id) {
        for (WodResult r : wodResults) if (r.id == id) return r;
        return null;
    }

    public RenfoLog addRenfo(Routine r, int seconds) {
        RenfoLog l = new RenfoLog();
        l.id = newId();
        l.routineId = r.id;
        l.name = r.name;
        l.date = System.currentTimeMillis();
        l.seconds = seconds;
        renfoLogs.add(l);
        sortSessions();
        save();
        return l;
    }

    public void deleteRenfo(RenfoLog l) {
        renfoLogs.remove(l);
        save();
    }

    // ---------------------------------------------------------------- modèles et programmes

    public Template saveTemplate(String name, List<ExerciseEntry> exercises) {
        Template t = new Template();
        t.id = newId();
        t.name = name;
        for (ExerciseEntry e : exercises) {
            ExerciseEntry c = e.copy(false);
            if (!c.sets.isEmpty()) t.exercises.add(c);
        }
        templates.add(t);
        save();
        return t;
    }

    public void deleteTemplate(Template t) {
        templates.remove(t);
        save();
    }

    public void setProgram(Program p) {
        program = p;
        save();
    }

    /** Nom d'une séance issue d'un programme : « Smolov Jr · S1 · J1 · 6×6 · 70 kg ». */
    public static String programSessionName(Program p, int index) {
        Program.Day d = p.days.get(index);
        String group = d.group == null ? "" : d.group.replace("Semaine ", "S");
        int dot = group.indexOf(" ·");
        if (dot > 0) group = group.substring(0, dot);
        String base = p.title.contains(" · ") ? p.title.substring(0, p.title.indexOf(" · ")) : p.title;
        return Fmt.cap(base + (group.isEmpty() ? "" : " · " + group) + " · " + d.name);
    }
}

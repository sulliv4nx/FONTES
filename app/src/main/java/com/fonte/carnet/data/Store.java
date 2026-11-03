package com.fonte.carnet.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;
import android.util.Log;

import com.fonte.carnet.core.Exercise;
import com.fonte.carnet.core.ExerciseEntry;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Library;
import com.fonte.carnet.core.Program;
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

package com.fonte.carnet.data;

import com.fonte.carnet.core.BodyLog;
import com.fonte.carnet.core.Exercise;
import com.fonte.carnet.core.Food;
import com.fonte.carnet.core.FoodEntry;
import com.fonte.carnet.core.Profile;
import com.fonte.carnet.core.ExerciseEntry;
import com.fonte.carnet.core.Library;
import com.fonte.carnet.core.Program;
import com.fonte.carnet.core.RenfoLog;
import com.fonte.carnet.core.Session;
import com.fonte.carnet.core.SetEntry;
import com.fonte.carnet.core.Settings;
import com.fonte.carnet.core.Template;
import com.fonte.carnet.core.Wod;
import com.fonte.carnet.core.WodResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/** Sérialisation JSON de toutes les données (fichier local et sauvegarde exportée). */
final class JsonCodec {
    private JsonCodec() {
    }

    static final int VERSION = 1;

    static JSONObject write(Store s) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("app", "Fonte");
        o.put("version", VERSION);
        o.put("nextId", s.nextId);
        o.put("nextExerciseId", s.nextExerciseId);

        JSONObject st = new JSONObject();
        st.put("restSeconds", s.settings.restSeconds);
        st.put("rounding", finite(s.settings.rounding));
        st.put("vibrate", s.settings.vibrate);
        st.put("sounds", s.settings.sounds);
        st.put("prepSeconds", s.settings.prepSeconds);
        o.put("settings", st);

        JSONArray ex = new JSONArray();
        for (Exercise e : s.exercises) {
            if (!e.custom) continue;
            JSONObject j = new JSONObject();
            j.put("id", e.id);
            j.put("name", e.name);
            j.put("category", e.category);
            ex.put(j);
        }
        o.put("exercises", ex);

        JSONArray sessions = new JSONArray();
        for (Session x : s.sessions) sessions.put(session(x));
        o.put("sessions", sessions);
        if (s.current != null) o.put("current", session(s.current));

        JSONArray templates = new JSONArray();
        for (Template t : s.templates) {
            JSONObject j = new JSONObject();
            j.put("id", t.id);
            j.put("name", t.name);
            j.put("exercises", entries(t.exercises));
            templates.put(j);
        }
        o.put("templates", templates);

        if (s.program != null) {
            Program p = s.program;
            JSONObject j = new JSONObject();
            j.put("type", p.type);
            j.put("title", p.title);
            j.put("summary", p.summary);
            j.put("startedAt", p.startedAt);
            JSONArray days = new JSONArray();
            for (Program.Day d : p.days) {
                JSONObject dj = new JSONObject();
                dj.put("group", d.group);
                dj.put("name", d.name);
                dj.put("sessionId", d.sessionId);
                dj.put("exercises", entries(d.exercises));
                days.put(dj);
            }
            j.put("days", days);
            o.put("program", j);
        }

        JSONArray wods = new JSONArray();
        for (Wod w : s.customWods) wods.put(wod(w));
        o.put("wods", wods);

        JSONArray results = new JSONArray();
        for (WodResult r : s.wodResults) {
            JSONObject j = new JSONObject();
            j.put("id", r.id);
            j.put("wodId", r.wodId);
            j.put("wodName", r.wodName);
            j.put("date", r.date);
            j.put("type", r.type);
            j.put("seconds", r.seconds);
            j.put("rounds", r.rounds);
            j.put("reps", r.reps);
            j.put("rx", r.rx);
            if (r.capped) j.put("capped", true);
            if (r.note != null && !r.note.isEmpty()) j.put("note", r.note);
            if (r.minutes > 0) j.put("minutes", r.minutes);
            results.put(j);
        }
        o.put("wodResults", results);

        JSONArray renfo = new JSONArray();
        for (RenfoLog l : s.renfoLogs) {
            JSONObject j = new JSONObject();
            j.put("id", l.id);
            j.put("routineId", l.routineId);
            j.put("name", l.name);
            j.put("date", l.date);
            j.put("seconds", l.seconds);
            renfo.put(j);
        }
        o.put("renfo", renfo);
        writeBody(s, o);
        return o;
    }

    // ---------------------------------------------------------------- nutrition et corps

    private static void writeBody(Store s, JSONObject o) throws JSONException {
        Profile p = s.profile;
        JSONObject pj = new JSONObject();
        pj.put("sex", p.sex);
        pj.put("birthYear", p.birthYear);
        pj.put("heightCm", finite(p.heightCm));
        pj.put("activity", finite(p.activity));
        pj.put("goal", p.goal);
        pj.put("kcalOverride", p.kcalOverride);
        pj.put("proteinPerKg", finite(p.proteinPerKg));
        pj.put("fatPerKg", finite(p.fatPerKg));
        pj.put("waterOverride", p.waterOverride);
        o.put("profile", pj);

        JSONArray foods = new JSONArray();
        for (Food f : s.customFoods) {
            JSONObject j = new JSONObject();
            j.put("id", f.id);
            j.put("name", f.name);
            j.put("kcal", finite(f.kcal));
            j.put("p", finite(f.protein));
            j.put("c", finite(f.carbs));
            j.put("f", finite(f.fat));
            if (f.portion > 0) j.put("portion", finite(f.portion));
            if (f.portionLabel != null) j.put("portionLabel", f.portionLabel);
            if (f.barcode != null) j.put("barcode", f.barcode);
            foods.put(j);
        }
        o.put("foods", foods);

        JSONArray entries = new JSONArray();
        for (FoodEntry e : s.foodEntries) {
            JSONObject j = new JSONObject();
            j.put("id", e.id);
            j.put("day", e.day);
            j.put("time", e.time);
            j.put("meal", e.meal);
            if (e.foodId != null) j.put("foodId", e.foodId);
            j.put("name", e.name);
            j.put("g", finite(e.grams));
            j.put("kcal", finite(e.kcal));
            j.put("p", finite(e.protein));
            j.put("c", finite(e.carbs));
            j.put("f", finite(e.fat));
            if (e.photo != null) j.put("photo", e.photo);
            entries.put(j);
        }
        o.put("foodEntries", entries);

        JSONArray water = new JSONArray();
        for (BodyLog.Water w : s.water) {
            JSONObject j = new JSONObject();
            j.put("id", w.id);
            j.put("day", w.day);
            j.put("time", w.time);
            j.put("ml", w.ml);
            water.put(j);
        }
        o.put("water", water);

        JSONArray acts = new JSONArray();
        for (BodyLog.Activity a : s.activities) {
            JSONObject j = new JSONObject();
            j.put("id", a.id);
            j.put("day", a.day);
            j.put("time", a.time);
            j.put("name", a.name);
            j.put("minutes", a.minutes);
            j.put("kcal", a.kcal);
            acts.put(j);
        }
        o.put("activities", acts);

        JSONArray weights = new JSONArray();
        for (BodyLog.Weight w : s.weights) {
            JSONObject j = new JSONObject();
            j.put("id", w.id);
            j.put("day", w.day);
            j.put("time", w.time);
            j.put("kg", finite(w.kg));
            weights.put(j);
        }
        o.put("weights", weights);

        JSONArray measures = new JSONArray();
        for (BodyLog.Measures m : s.measures) {
            JSONObject j = new JSONObject();
            j.put("id", m.id);
            j.put("day", m.day);
            j.put("time", m.time);
            JSONObject v = new JSONObject();
            for (java.util.Map.Entry<String, Double> en : m.values.entrySet()) v.put(en.getKey(), finite(en.getValue()));
            j.put("values", v);
            measures.put(j);
        }
        o.put("measures", measures);
    }

    private static void readBody(Store s, JSONObject o) throws JSONException {
        JSONObject pj = o.optJSONObject("profile");
        if (pj != null) {
            Profile p = new Profile();
            p.sex = Profile.FEMALE.equals(pj.optString("sex", Profile.MALE)) ? Profile.FEMALE : Profile.MALE;
            p.birthYear = pj.optInt("birthYear", 0);
            p.heightCm = finite(pj.optDouble("heightCm", 0));
            p.activity = finite(pj.optDouble("activity", 1.375));
            if (p.activity < 1 || p.activity > 2.5) p.activity = 1.375;
            p.goal = pj.optString("goal", Profile.MAINTAIN);
            p.kcalOverride = Math.max(0, pj.optInt("kcalOverride", 0));
            p.proteinPerKg = finite(pj.optDouble("proteinPerKg", 2.0));
            p.fatPerKg = finite(pj.optDouble("fatPerKg", 0.9));
            p.waterOverride = Math.max(0, pj.optInt("waterOverride", 0));
            s.profile = p;
        }
        JSONArray foods = o.optJSONArray("foods");
        if (foods != null) {
            for (int i = 0; i < foods.length(); i++) {
                JSONObject j = foods.getJSONObject(i);
                Food f = new Food();
                f.id = j.getString("id");
                f.name = j.optString("name", "Aliment");
                f.category = com.fonte.carnet.core.Foods.MINE;
                f.custom = true;
                f.kcal = finite(j.optDouble("kcal", 0));
                f.protein = finite(j.optDouble("p", 0));
                f.carbs = finite(j.optDouble("c", 0));
                f.fat = finite(j.optDouble("f", 0));
                f.portion = finite(j.optDouble("portion", 0));
                f.portionLabel = j.has("portionLabel") ? j.getString("portionLabel") : null;
                f.barcode = j.has("barcode") ? j.getString("barcode") : null;
                s.customFoods.add(f);
            }
        }
        JSONArray entries = o.optJSONArray("foodEntries");
        if (entries != null) {
            for (int i = 0; i < entries.length(); i++) {
                JSONObject j = entries.getJSONObject(i);
                FoodEntry e = new FoodEntry();
                e.id = j.getLong("id");
                e.day = j.getInt("day");
                e.time = j.optLong("time", 0);
                e.meal = j.optString("meal", FoodEntry.SNACK);
                e.foodId = j.has("foodId") ? j.getString("foodId") : null;
                e.name = j.optString("name", "Aliment");
                e.grams = finite(j.optDouble("g", 0));
                e.kcal = finite(j.optDouble("kcal", 0));
                e.protein = finite(j.optDouble("p", 0));
                e.carbs = finite(j.optDouble("c", 0));
                e.fat = finite(j.optDouble("f", 0));
                e.photo = j.has("photo") ? j.getString("photo") : null;
                s.foodEntries.add(e);
                s.nextId = Math.max(s.nextId, e.id + 1);
            }
        }
        JSONArray water = o.optJSONArray("water");
        if (water != null) {
            for (int i = 0; i < water.length(); i++) {
                JSONObject j = water.getJSONObject(i);
                BodyLog.Water w = new BodyLog.Water();
                w.id = j.getLong("id");
                w.day = j.getInt("day");
                w.time = j.optLong("time", 0);
                w.ml = j.optInt("ml", 0);
                s.water.add(w);
                s.nextId = Math.max(s.nextId, w.id + 1);
            }
        }
        JSONArray acts = o.optJSONArray("activities");
        if (acts != null) {
            for (int i = 0; i < acts.length(); i++) {
                JSONObject j = acts.getJSONObject(i);
                BodyLog.Activity a = new BodyLog.Activity();
                a.id = j.getLong("id");
                a.day = j.getInt("day");
                a.time = j.optLong("time", 0);
                a.name = j.optString("name", "Activité");
                a.minutes = Math.max(0, j.optInt("minutes", 0));
                a.kcal = Math.max(0, j.optInt("kcal", 0));
                s.activities.add(a);
                s.nextId = Math.max(s.nextId, a.id + 1);
            }
        }
        JSONArray weights = o.optJSONArray("weights");
        if (weights != null) {
            for (int i = 0; i < weights.length(); i++) {
                JSONObject j = weights.getJSONObject(i);
                BodyLog.Weight w = new BodyLog.Weight();
                w.id = j.getLong("id");
                w.day = j.getInt("day");
                w.time = j.optLong("time", 0);
                w.kg = finite(j.optDouble("kg", 0));
                if (w.kg > 0) s.weights.add(w);
                s.nextId = Math.max(s.nextId, w.id + 1);
            }
        }
        JSONArray measures = o.optJSONArray("measures");
        if (measures != null) {
            for (int i = 0; i < measures.length(); i++) {
                JSONObject j = measures.getJSONObject(i);
                BodyLog.Measures m = new BodyLog.Measures();
                m.id = j.getLong("id");
                m.day = j.getInt("day");
                m.time = j.optLong("time", 0);
                JSONObject v = j.optJSONObject("values");
                if (v != null) {
                    java.util.Iterator<String> it = v.keys();
                    while (it.hasNext()) {
                        String k = it.next();
                        double d = finite(v.optDouble(k, 0));
                        if (d > 0) m.values.put(k, d);
                    }
                }
                s.measures.add(m);
                s.nextId = Math.max(s.nextId, m.id + 1);
            }
        }
    }

    private static JSONObject wod(Wod w) throws JSONException {
        JSONObject j = new JSONObject();
        j.put("id", w.id);
        j.put("name", w.name);
        j.put("format", w.format);
        j.put("minutes", w.minutes);
        j.put("every", w.every);
        j.put("rounds", w.rounds);
        if (w.alternate) j.put("alternate", true);
        if (w.note != null && !w.note.isEmpty()) j.put("note", w.note);
        JSONArray lines = new JSONArray();
        for (String l : w.lines) lines.put(l);
        j.put("lines", lines);
        JSONArray tags = new JSONArray();
        for (String t : w.tags) tags.put(t);
        j.put("tags", tags);
        return j;
    }

    private static Wod wod(JSONObject j) throws JSONException {
        Wod w = new Wod();
        w.id = j.getString("id");
        w.kind = Wod.CUSTOM;
        w.name = j.optString("name", "Mon WOD");
        w.format = j.optString("format", Wod.FOR_TIME);
        w.minutes = Math.max(0, j.optInt("minutes", 0));
        w.every = Math.max(10, j.optInt("every", 60));
        w.rounds = Math.max(0, j.optInt("rounds", 0));
        w.alternate = j.optBoolean("alternate", false);
        w.note = j.has("note") ? j.getString("note") : null;
        JSONArray lines = j.optJSONArray("lines");
        if (lines != null) for (int i = 0; i < lines.length(); i++) w.lines.add(lines.getString(i));
        JSONArray tags = j.optJSONArray("tags");
        if (tags != null) for (int i = 0; i < tags.length(); i++) w.tags.add(tags.getString(i));
        return w;
    }

    static void read(Store s, JSONObject o) throws JSONException {
        if (o.optInt("version", 0) < 1 || !o.has("sessions")) {
            throw new JSONException("Ce fichier n'est pas une sauvegarde Fonte.");
        }
        s.nextId = Math.max(1, o.optLong("nextId", 1));
        s.nextExerciseId = Math.max(Library.FIRST_CUSTOM_ID, o.optLong("nextExerciseId", Library.FIRST_CUSTOM_ID));

        JSONObject st = o.optJSONObject("settings");
        if (st != null) {
            Settings set = new Settings();
            set.restSeconds = Math.max(0, st.optInt("restSeconds", set.restSeconds));
            set.rounding = Math.max(0, st.optDouble("rounding", set.rounding));
            if (Double.isNaN(set.rounding)) set.rounding = 2.5;
            set.vibrate = st.optBoolean("vibrate", true);
            set.sounds = st.optBoolean("sounds", true);
            set.prepSeconds = Math.max(0, Math.min(60, st.optInt("prepSeconds", set.prepSeconds)));
            s.settings = set;
        }

        JSONArray ex = o.optJSONArray("exercises");
        if (ex != null) {
            for (int i = 0; i < ex.length(); i++) {
                JSONObject j = ex.getJSONObject(i);
                long id = j.getLong("id");
                if (s.exercise(id) != null) continue;
                s.exercises.add(new Exercise(id, j.getString("name"), j.optString("category", Library.CUSTOM), true));
                s.nextExerciseId = Math.max(s.nextExerciseId, id + 1);
            }
        }

        JSONArray sessions = o.getJSONArray("sessions");
        for (int i = 0; i < sessions.length(); i++) {
            Session x = session(sessions.getJSONObject(i));
            if (x.end == 0) x.end = x.start; // une séance de l'historique est forcément terminée
            s.sessions.add(x);
            s.nextId = Math.max(s.nextId, x.id + 1);
        }
        JSONObject cur = o.optJSONObject("current");
        if (cur != null) {
            s.current = session(cur);
            s.current.end = 0;
            s.nextId = Math.max(s.nextId, s.current.id + 1);
        }

        JSONArray templates = o.optJSONArray("templates");
        if (templates != null) {
            for (int i = 0; i < templates.length(); i++) {
                JSONObject j = templates.getJSONObject(i);
                Template t = new Template();
                t.id = j.getLong("id");
                t.name = j.optString("name", "Modèle");
                readEntries(j.optJSONArray("exercises"), t.exercises);
                s.templates.add(t);
                s.nextId = Math.max(s.nextId, t.id + 1);
            }
        }

        JSONObject pj = o.optJSONObject("program");
        if (pj != null) {
            Program p = new Program();
            p.type = pj.optString("type", "");
            p.title = pj.optString("title", "Programme");
            p.summary = pj.optString("summary", "");
            p.startedAt = pj.optLong("startedAt", 0);
            JSONArray days = pj.optJSONArray("days");
            if (days != null) {
                for (int i = 0; i < days.length(); i++) {
                    JSONObject dj = days.getJSONObject(i);
                    Program.Day d = new Program.Day();
                    d.group = dj.optString("group", "");
                    d.name = dj.optString("name", "Séance " + (i + 1));
                    d.sessionId = dj.optLong("sessionId", 0);
                    readEntries(dj.optJSONArray("exercises"), d.exercises);
                    p.days.add(d);
                }
            }
            if (!p.days.isEmpty()) s.program = p;
        }

        JSONArray wods = o.optJSONArray("wods");
        if (wods != null) for (int i = 0; i < wods.length(); i++) s.customWods.add(wod(wods.getJSONObject(i)));

        JSONArray results = o.optJSONArray("wodResults");
        if (results != null) {
            for (int i = 0; i < results.length(); i++) {
                JSONObject j = results.getJSONObject(i);
                WodResult r = new WodResult();
                r.id = j.getLong("id");
                r.wodId = j.getString("wodId");
                r.wodName = j.optString("wodName", "WOD");
                r.date = j.getLong("date");
                r.type = j.optString("type", WodResult.DONE);
                r.seconds = Math.max(0, j.optInt("seconds", 0));
                r.rounds = Math.max(0, j.optInt("rounds", 0));
                r.reps = Math.max(0, j.optInt("reps", 0));
                r.rx = j.optBoolean("rx", true);
                r.capped = j.optBoolean("capped", false);
                r.note = j.has("note") ? j.getString("note") : null;
                r.minutes = Math.max(0, j.optInt("minutes", 0));
                s.wodResults.add(r);
                s.nextId = Math.max(s.nextId, r.id + 1);
            }
        }

        readBody(s, o);

        JSONArray renfo = o.optJSONArray("renfo");
        if (renfo != null) {
            for (int i = 0; i < renfo.length(); i++) {
                JSONObject j = renfo.getJSONObject(i);
                RenfoLog l = new RenfoLog();
                l.id = j.getLong("id");
                l.routineId = j.optString("routineId", "");
                l.name = j.optString("name", "Renfo");
                l.date = j.getLong("date");
                l.seconds = Math.max(0, j.optInt("seconds", 0));
                s.renfoLogs.add(l);
                s.nextId = Math.max(s.nextId, l.id + 1);
            }
        }
    }

    private static JSONObject session(Session x) throws JSONException {
        JSONObject j = new JSONObject();
        j.put("id", x.id);
        j.put("name", x.name);
        j.put("start", x.start);
        j.put("end", x.end);
        j.put("programKey", x.programKey);
        j.put("programDay", x.programDay);
        j.put("exercises", entries(x.exercises));
        if (!x.records.isEmpty()) {
            JSONArray r = new JSONArray();
            for (String line : x.records) r.put(line);
            j.put("records", r);
        }
        return j;
    }

    private static Session session(JSONObject j) throws JSONException {
        Session x = new Session();
        x.id = j.getLong("id");
        x.name = j.optString("name", "Séance");
        x.start = j.getLong("start");
        x.end = j.optLong("end", 0);
        x.programKey = j.optLong("programKey", 0);
        x.programDay = j.optInt("programDay", -1);
        readEntries(j.optJSONArray("exercises"), x.exercises);
        JSONArray r = j.optJSONArray("records");
        if (r != null) for (int i = 0; i < r.length(); i++) x.records.add(r.getString(i));
        return x;
    }

    private static JSONArray entries(List<ExerciseEntry> list) throws JSONException {
        JSONArray a = new JSONArray();
        for (ExerciseEntry e : list) {
            JSONObject j = new JSONObject();
            j.put("exerciseId", e.exerciseId);
            JSONArray sets = new JSONArray();
            for (SetEntry s : e.sets) {
                JSONObject sj = new JSONObject();
                sj.put("w", finite(s.weight));
                sj.put("r", s.reps);
                if (s.done) sj.put("done", true);
                if (s.amrap) sj.put("amrap", true);
                if (s.label != null) sj.put("label", s.label);
                sets.put(sj);
            }
            j.put("sets", sets);
            a.put(j);
        }
        return a;
    }

    private static void readEntries(JSONArray a, List<ExerciseEntry> out) throws JSONException {
        if (a == null) return;
        for (int i = 0; i < a.length(); i++) {
            JSONObject j = a.getJSONObject(i);
            ExerciseEntry e = new ExerciseEntry(j.getLong("exerciseId"));
            JSONArray sets = j.optJSONArray("sets");
            if (sets != null) {
                for (int k = 0; k < sets.length(); k++) {
                    JSONObject sj = sets.getJSONObject(k);
                    SetEntry s = new SetEntry(finite(sj.optDouble("w", 0)), Math.max(0, sj.optInt("r", 0)));
                    s.done = sj.optBoolean("done", false);
                    s.amrap = sj.optBoolean("amrap", false);
                    s.label = sj.has("label") ? sj.getString("label") : null;
                    e.sets.add(s);
                }
            }
            out.add(e);
        }
    }

    private static double finite(double v) {
        return (Double.isNaN(v) || Double.isInfinite(v)) ? 0 : v;
    }
}

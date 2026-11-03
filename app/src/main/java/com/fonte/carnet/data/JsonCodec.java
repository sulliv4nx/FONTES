package com.fonte.carnet.data;

import com.fonte.carnet.core.Exercise;
import com.fonte.carnet.core.ExerciseEntry;
import com.fonte.carnet.core.Library;
import com.fonte.carnet.core.Program;
import com.fonte.carnet.core.Session;
import com.fonte.carnet.core.SetEntry;
import com.fonte.carnet.core.Settings;
import com.fonte.carnet.core.Template;

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
        return o;
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

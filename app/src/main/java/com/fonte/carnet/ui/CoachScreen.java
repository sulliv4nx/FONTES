package com.fonte.carnet.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.CoachParser;
import com.fonte.carnet.core.Exercise;
import com.fonte.carnet.core.ExerciseEntry;
import com.fonte.carnet.core.Stats;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Programmation du coach : photo (lecture du texte) ou texte collé, puis séance prête à lancer. */
public class CoachScreen extends Screen {
    private static final int REQ_PHOTO = 31;
    private static final int REQ_PICK = 32;
    private static final String EXAMPLE = "A) Back squat 5x5 @ 100kg\n"
            + "B) Développé couché 4 × 8 à 70 kg\n"
            + "C) Rowing barre 4x10 60\n"
            + "D) Tractions 3 x max\n\n"
            + "WOD : AMRAP 12 min\n"
            + "10 wall balls\n"
            + "15 KB swings 24 kg\n"
            + "200 m run";

    private String text = "";
    private CoachParser.Result result;
    private final Set<Integer> excluded = new HashSet<>();
    private String sessionName = "Programmation du coach";

    @Override
    public String title() {
        return "Programmation du coach";
    }

    @Override
    public View build() {
        LinearLayout c = Ui.vbox(a);
        TextView intro = Ui.text(a, "Prends en photo la programmation de ton coach (tableau blanc, feuille, capture d'écran) "
                + "ou colle-la ici. L'appli repère les exercices, séries, répétitions et charges, ainsi que le WOD.", 14, Ui.MUTED);
        intro.setPadding(0, Ui.dp(4), 0, Ui.dp(8));
        c.addView(intro);

        TextView photo = Ui.button(a, "Prendre une photo", Ui.PRIMARY, v -> takePhoto());
        TextView pick = Ui.button(a, "Image de la galerie", Ui.SECONDARY, v -> pickImage());
        TextView paste = Ui.button(a, "Coller", Ui.SECONDARY, v -> paste());
        LinearLayout row = Ui.hbox(a);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, -2, 1.3f);
        p1.rightMargin = Ui.dp(8);
        row.addView(pick, p1);
        row.addView(paste, new LinearLayout.LayoutParams(0, -2, 1));
        c.addView(photo);
        c.addView(row, Ui.mlp(-1, -2, 0, 0, 0, 8));

        EditText area = Ui.area(a, "ex.\nSquat 5x5 100kg\nDéveloppé couché 4x8 @70\nTractions 3 x max", 7);
        area.setText(text);
        Ui.onText(area, t -> {
            text = t;
            result = null;
        });
        c.addView(Ui.labelled(a, "Programmation", area));
        LinearLayout actions = Ui.hbox(a);
        TextView analyze = Ui.button(a, "Analyser", Ui.PRIMARY, v -> analyze());
        TextView example = Ui.button(a, "Exemple", Ui.GHOST, v -> {
            text = EXAMPLE;
            result = null;
            a.refresh();
        });
        LinearLayout.LayoutParams pa = new LinearLayout.LayoutParams(0, -2, 1.6f);
        pa.rightMargin = Ui.dp(8);
        actions.addView(analyze, pa);
        actions.addView(example, new LinearLayout.LayoutParams(0, -2, 1));
        c.addView(actions);

        if (result != null) showResult(c);
        return Ui.page(a, c);
    }

    private void analyze() {
        if (text.trim().isEmpty()) {
            Ui.toast(a, "Colle ou scanne d'abord ta programmation.");
            return;
        }
        Map<Long, Stats.Record> recs = Stats.records(st.sessions);
        result = CoachParser.parse(text, CoachParser.libraryMatcher(st.exercises), id -> {
            Stats.Record r = recs.get(id);
            return r == null ? 0 : r.bestE1rm;
        }, st.settings.rounding);
        excluded.clear();
        Ui.hideKeyboard(a.getWindow().getDecorView());
        a.refresh();
    }

    private void showResult(LinearLayout c) {
        c.addView(Ui.section(a, "Séance détectée"));
        if (result.items.isEmpty()) {
            TextView t = Ui.text(a, "Aucun exercice reconnu. Écris une ligne par exercice, par exemple « Squat 5x5 100kg ».", 14, Ui.MUTED);
            t.setPadding(0, 0, 0, Ui.dp(8));
            c.addView(t);
        }
        for (int i = 0; i < result.items.size(); i++) {
            CoachParser.Item it = result.items.get(i);
            final int idx = i;
            LinearLayout card = Ui.card(a);
            LinearLayout line = Ui.hbox(a);
            CheckBox keep = new CheckBox(a);
            keep.setChecked(!excluded.contains(idx));
            keep.setOnCheckedChangeListener((b, on) -> {
                if (on) excluded.remove(idx);
                else excluded.add(idx);
            });
            line.addView(keep);
            LinearLayout txt = Ui.vbox(a);
            boolean known = it.exerciseId != 0;
            txt.addView(Ui.medium(a, known ? st.name(it.exerciseId) : it.name, 16, Ui.TEXT));
            TextView sum = Ui.text(a, it.summary() + (known ? "" : " · nouvel exercice"), 14, known ? Ui.ACCENT : Ui.MUTED);
            Ui.tabular(sum);
            txt.addView(sum);
            line.addView(txt, Ui.weight(1));
            card.addView(line);
            c.addView(card);
        }
        if (!result.ignored.isEmpty()) {
            StringBuilder sb = new StringBuilder("Lignes non utilisées : ");
            for (int i = 0; i < result.ignored.size(); i++) {
                if (i > 0) sb.append(" · ");
                sb.append(result.ignored.get(i));
            }
            TextView ig = Ui.text(a, sb.toString(), 12, Ui.FAINT);
            ig.setPadding(0, 0, 0, Ui.dp(8));
            c.addView(ig);
        }
        if (!result.items.isEmpty()) {
            EditText name = Ui.field(a, "Nom de la séance", Ui.TEXT_FIELD);
            name.setText(sessionName);
            Ui.onText(name, t -> sessionName = t.trim().isEmpty() ? "Programmation du coach" : t.trim());
            c.addView(Ui.labelled(a, "Nom de la séance", name));
            c.addView(Ui.button(a, "Démarrer la séance", Ui.PRIMARY, v -> {
                List<ExerciseEntry> plan = plan();
                if (plan.isEmpty()) {
                    Ui.toast(a, "Coche au moins un exercice.");
                    return;
                }
                a.startSession(sessionName, plan, 0, -1);
            }));
            c.addView(Ui.button(a, "Enregistrer comme modèle", Ui.GHOST, v -> {
                List<ExerciseEntry> plan = plan();
                if (plan.isEmpty()) return;
                st.saveTemplate(sessionName, plan);
                Ui.toast(a, "Modèle enregistré dans Séances types.");
            }));
        }
        if (result.wod != null) {
            c.addView(Ui.section(a, "WOD détecté"));
            LinearLayout card = Ui.card(a);
            card.addView(Ui.medium(a, result.wod.formatLabel(), 16, Ui.ACCENT));
            for (String l : result.wod.lines) card.addView(Ui.text(a, l, 15, Ui.TEXT));
            card.addView(Ui.button(a, "Enregistrer ce WOD", Ui.SECONDARY,
                    v -> a.show(WodEditScreen.prefilled(result.wod))));
            c.addView(card);
        }
    }

    /** Séance à partir des lignes cochées ; les exercices inconnus sont ajoutés à la bibliothèque. */
    private List<ExerciseEntry> plan() {
        List<ExerciseEntry> plan = new ArrayList<>();
        for (int i = 0; i < result.items.size(); i++) {
            if (excluded.contains(i)) continue;
            CoachParser.Item it = result.items.get(i);
            long id = it.exerciseId;
            if (id == 0) {
                String n = com.fonte.carnet.core.Fmt.cap(it.name);
                Exercise existing = st.findByName(n);
                id = existing != null ? existing.id : st.addExercise(n).id;
                it.exerciseId = id;
            }
            plan.add(it.toEntry(id));
        }
        return plan;
    }

    // ---------------------------------------------------------------- photo, galerie, presse-papiers

    private void takePhoto() {
        File f = PhotoProvider.file(a);
        if (f.exists() && !f.delete()) f.deleteOnExit();
        Uri out = PhotoProvider.uri();
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        i.putExtra(MediaStore.EXTRA_OUTPUT, out);
        i.setClipData(ClipData.newRawUri("photo", out));
        i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            a.startActivityForResult(i, REQ_PHOTO);
        } catch (ActivityNotFoundException | SecurityException e) {
            Ui.toast(a, "Appareil photo indisponible : choisis plutôt une image de la galerie.");
        }
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            a.startActivityForResult(i, REQ_PICK);
        } catch (ActivityNotFoundException e) {
            Ui.toast(a, "Aucune galerie disponible.");
        }
    }

    private void paste() {
        ClipboardManager cm = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip() || cm.getPrimaryClip() == null || cm.getPrimaryClip().getItemCount() == 0) {
            Ui.toast(a, "Le presse-papiers est vide.");
            return;
        }
        CharSequence t = cm.getPrimaryClip().getItemAt(0).coerceToText(a);
        if (t == null || t.toString().trim().isEmpty()) {
            Ui.toast(a, "Le presse-papiers ne contient pas de texte.");
            return;
        }
        text = text.trim().isEmpty() ? t.toString() : text + "\n" + t;
        result = null;
        a.refresh();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onActivityResult(int request, int res, Intent data) {
        if (request != REQ_PHOTO && request != REQ_PICK) return false;
        if (res != Activity.RESULT_OK) return true;
        Uri uri = request == REQ_PHOTO ? PhotoProvider.uri() : (data == null ? null : data.getData());
        if (uri == null || (request == REQ_PHOTO && PhotoProvider.file(a).length() == 0)) {
            Ui.toast(a, "Aucune image reçue.");
            return true;
        }
        ProgressDialog wait = new ProgressDialog(a);
        wait.setMessage("Lecture de la programmation…");
        wait.setCancelable(false);
        wait.show();
        Ocr.recognize(a, uri, (found, error) -> {
            wait.dismiss();
            if (found == null || found.trim().isEmpty()) {
                Ui.info(a, "Texte illisible", "Je n'ai pas réussi à lire de texte sur cette image"
                        + (error != null ? " (" + error + ")" : "") + ". Essaie avec une photo plus nette et bien éclairée, "
                        + "ou colle le texte.");
                return;
            }
            text = text.trim().isEmpty() ? found : text + "\n" + found;
            a.refresh();
            analyze();
        });
        return true;
    }
}

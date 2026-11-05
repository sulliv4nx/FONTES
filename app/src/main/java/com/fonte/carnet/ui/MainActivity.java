package com.fonte.carnet.ui;

import android.app.Activity;
import android.os.Build;
import android.graphics.Color;
import android.graphics.Insets;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.ExerciseEntry;
import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Program;
import com.fonte.carnet.core.Session;
import com.fonte.carnet.data.Store;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Activité unique : en-tête, contenu (pile d'écrans) et barre d'onglets. */
public class MainActivity extends Activity {
    private static final String TAG = "Fonte";
    private static final int REQ_EXPORT = 1;
    private static final int REQ_IMPORT = 2;

    public static final int TAB_HOME = 0;
    public static final int TAB_TRAINING = 1;
    public static final int TAB_NUTRITION = 2;
    public static final int TAB_BODY = 3;
    public static final int TAB_PROGRESS = 4;
    /** Anciens noms : la musculation est dans l'onglet Sport. */
    public static final int TAB_SESSION = TAB_TRAINING;
    /** Les programmes (Smolov Jr, 5/3/1) sont rangés dans l'onglet Muscu. */
    public static final int TAB_PROGRAMS = TAB_SESSION;
    private static final int TABS = 5;

    Store store;
    private FrameLayout content;
    private TextView titleView;
    private ImageView backButton;
    private ImageView actionButton;
    private LinearLayout bottomNavigation;
    private final LinearLayout[] tabButtons = new LinearLayout[TABS];
    private final Icon[] tabIcons = new Icon[TABS];
    private final TextView[] tabLabels = new TextView[TABS];
    private final List<Screen> stack = new ArrayList<>();
    private int tab = TAB_HOME;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean resumed;
    private long restEndAt;
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            onTick();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        configureSystemBars();
        Ui.init(this);
        store = Store.get(this);
        setContentView(buildRoot());
        goTab(TAB_HOME);
        handler.post(ticker);
        if (store.loadError != null) {
            Ui.info(this, "Données illisibles",
                    "Le fichier de données n'a pas pu être lu. Une copie a été mise de côté et l'appli repart de zéro. "
                            + "Si tu as une sauvegarde, importe-la depuis Réglages.");
            store.loadError = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        onTick();
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        store.flush();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(ticker);
        if (isFinishing()) {
            ClockRunner r = ClockRunner.active();
            if (r != null) r.stop();
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Recalcule les tailles et les marges après rotation ou changement de densité.
        Ui.init(this);
        Screen current = top();
        if (current != null) current.onHide();
        setContentView(buildRoot());
        render();
    }

    // ---------------------------------------------------------------- structure

    /** Dessine sous les barres système : les zones sûres sont gérées par des insets. */
    private void configureSystemBars() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);
    }

    private View buildRoot() {
        LinearLayout root = Ui.vbox(this);
        root.setBackgroundColor(Ui.BG);
        // La racine prend le focus initial : aucun champ ne s'active (ni le clavier) à l'ouverture.
        root.setFocusable(true);
        root.setFocusableInTouchMode(true);
        root.setDefaultFocusHighlightEnabled(false);

        LinearLayout header = Ui.hbox(this);
        header.setMinimumHeight(Ui.dp(60));
        header.setPadding(Ui.dp(8), Ui.dp(6), Ui.dp(8), Ui.dp(6));
        backButton = Ui.iconButton(this, Icon.BACK, Ui.TEXT, v -> handleBack());
        titleView = Ui.text(this, "", 20, Ui.TEXT);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        actionButton = Ui.iconButton(this, Icon.MORE, Ui.TEXT, v -> {
            Screen s = top();
            if (s != null) s.onAction();
        });
        backButton.setContentDescription("Retour");
        actionButton.setContentDescription("Options");
        header.addView(backButton);
        header.addView(titleView, Ui.weight(1));
        header.addView(actionButton);
        root.addView(header, Ui.lp(-1, -2));

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        View border = new View(this);
        border.setBackgroundColor(Ui.LINE);
        root.addView(border, Ui.lp(-1, 1));

        LinearLayout bar = Ui.hbox(this);
        bottomNavigation = bar;
        bar.setPadding(Ui.dp(6), Ui.dp(6), Ui.dp(6), Ui.dp(6));
        bar.setBackgroundColor(Ui.SURFACE);
        String[] labels = {"Accueil", "Sport", "Nutrition", "Corps", "Progrès"};
        int[] icons = {Icon.HOME, Icon.BARBELL, Icon.NUTRITION, Icon.BODY, Icon.TROPHY};
        for (int i = 0; i < TABS; i++) {
            LinearLayout t = Ui.vbox(this);
            t.setGravity(Gravity.CENTER);
            t.setMinimumHeight(Ui.dp(62));
            t.setPadding(Ui.dp(2), Ui.dp(7), Ui.dp(2), Ui.dp(5));
            t.setBackground(Ui.pressable(Ui.rect(0x00000000, 12), 12));
            tabButtons[i] = t;
            ImageView iv = new ImageView(this);
            tabIcons[i] = new Icon(icons[i], Ui.MUTED);
            iv.setImageDrawable(tabIcons[i]);
            t.addView(iv, Ui.lp(Ui.dp(24), Ui.dp(24)));
            tabLabels[i] = Ui.medium(this, labels[i], 11, Ui.MUTED);
            tabLabels[i].setPadding(0, Ui.dp(3), 0, 0);
            tabLabels[i].setSingleLine(true);
            tabLabels[i].setGravity(Gravity.CENTER);
            t.addView(tabLabels[i], Ui.lp(-1, -2));
            final int idx = i;
            t.setContentDescription(labels[i]);
            t.setOnClickListener(v -> goTab(idx));
            bar.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        }
        root.addView(bar, Ui.lp(-1, -2));

        // Clavier et barre de navigation Samsung ne recouvrent plus les champs ou onglets.
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int left, topInset, right, bottom;
            boolean keyboardVisible = false;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets system = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout());
                Insets ime = insets.getInsets(WindowInsets.Type.ime());
                left = system.left;
                topInset = system.top;
                right = system.right;
                bottom = Math.max(system.bottom, ime.bottom);
                keyboardVisible = insets.isVisible(WindowInsets.Type.ime());
            } else {
                left = insets.getSystemWindowInsetLeft();
                topInset = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            root.setPadding(left, topInset, right, bottom);
            bar.setVisibility(keyboardVisible ? View.GONE : View.VISIBLE);
            return insets;
        });
        root.requestApplyInsets();
        return root;
    }

    private Screen root(int t) {
        switch (t) {
            case TAB_TRAINING:
                return new TrainingScreen();
            case TAB_NUTRITION:
                return new NutritionScreen();
            case TAB_BODY:
                return new BodyScreen();
            case TAB_PROGRESS:
                return new ProgressScreen();
            default:
                return new DashboardScreen();
        }
    }

    private Screen top() {
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    /** Écran affiché. */
    public Screen currentScreen() {
        return top();
    }

    /** Revient au dernier écran de ce type dans la pile (affiché), ou null s'il n'y en a pas. */
    public Screen backTo(Class<? extends Screen> type) {
        for (int i = stack.size() - 1; i >= 0; i--) {
            if (!type.isInstance(stack.get(i))) continue;
            Screen old = top();
            if (old != null) old.onHide();
            while (stack.size() > i + 1) stack.remove(stack.size() - 1);
            render();
            return stack.get(i);
        }
        return null;
    }

    // ---------------------------------------------------------------- navigation

    public void goTab(int t) {
        Screen old = top();
        if (old != null) old.onHide();
        stack.clear();
        tab = t;
        Screen s = root(t);
        s.attach(this);
        stack.add(s);
        render();
    }

    public void show(Screen s) {
        Screen old = top();
        if (old != null) old.onHide();
        s.attach(this);
        stack.add(s);
        render();
    }

    /** Reconstruit l'écran affiché (après une modification des données). */
    public void refresh() {
        Screen s = top();
        if (s != null) s.onHide();
        render();
    }

    public void back() {
        if (stack.size() <= 1) {
            refresh();
            return;
        }
        Screen s = stack.remove(stack.size() - 1);
        s.onHide();
        render();
    }

    private void handleBack() {
        Screen s = top();
        if (s != null && s.onBack()) return;
        if (stack.size() > 1) back();
        else if (tab != TAB_HOME) goTab(TAB_HOME);
        else finish();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        handleBack();
    }

    private void render() {
        Screen s = top();
        if (s == null) return;
        content.removeAllViews();
        View v;
        try {
            v = s.build();
        } catch (RuntimeException e) {
            Log.e(TAG, "Erreur d'affichage", e);
            TextView err = Ui.text(this, "Oups, cet écran n'a pas pu s'afficher.\n" + e, 15, Ui.DANGER);
            err.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(16));
            v = err;
        }
        content.addView(v, new FrameLayout.LayoutParams(-1, -1));
        updateHeader();
        for (int i = 0; i < TABS; i++) {
            int color = i == tab ? Ui.ACCENT : Ui.MUTED;
            tabIcons[i].setColor(color);
            tabLabels[i].setTextColor(color);
            tabButtons[i].setSelected(i == tab);
            tabButtons[i].setBackground(Ui.pressable(
                    Ui.rect(i == tab ? Ui.ACCENT_SOFT : Color.TRANSPARENT, 12), 12));
        }
        refreshKeepScreenOn();
        safeTick(s);
    }

    /** Écran allumé pendant une séance ou un chrono. */
    public void refreshKeepScreenOn() {
        if (store.current != null || ClockRunner.running()) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void updateHeader() {
        Screen s = top();
        if (s == null) return;
        titleView.setText(s.title());
        boolean canGoBack = stack.size() > 1;
        backButton.setVisibility(canGoBack ? View.VISIBLE : View.GONE);
        titleView.setPadding(canGoBack ? Ui.dp(4) : Ui.dp(Ui.pad(this) - 8), 0, 0, 0);
        int icon = s.actionIcon();
        if (icon >= 0) {
            actionButton.setImageDrawable(new Icon(icon, Ui.TEXT));
            actionButton.setVisibility(View.VISIBLE);
        } else {
            actionButton.setVisibility(View.INVISIBLE);
        }
    }

    // ---------------------------------------------------------------- minuteur

    private void onTick() {
        if (restEndAt > 0 && System.currentTimeMillis() >= restEndAt) {
            restEndAt = 0;
            vibrate();
            if (resumed) Ui.toast(this, "Repos terminé, à toi !");
        }
        Screen s = top();
        if (s != null) safeTick(s);
    }

    private void safeTick(Screen s) {
        try {
            s.tick();
        } catch (RuntimeException e) {
            Log.e(TAG, "tick", e);
        }
    }

    public void startRest(int seconds) {
        restEndAt = System.currentTimeMillis() + seconds * 1000L;
        onTick();
    }

    public void addRest(int seconds) {
        if (restEndAt > 0) restEndAt = Math.max(System.currentTimeMillis() + 1000, restEndAt + seconds * 1000L);
        onTick();
    }

    public void stopRest() {
        restEndAt = 0;
        Screen s = top();
        if (s != null) safeTick(s);
    }

    /** Temps de repos restant en ms (0 si aucun minuteur). */
    public long restLeft() {
        return restEndAt > 0 ? Math.max(0, restEndAt - System.currentTimeMillis()) : 0;
    }

    @SuppressWarnings("deprecation")
    private void vibrate() {
        if (!store.settings.vibrate) return;
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            VibrationEffect effect = VibrationEffect.createWaveform(new long[]{0, 350, 150, 350, 150, 600}, -1);
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            v.vibrate(effect, attrs);
        } catch (RuntimeException e) {
            Log.e(TAG, "vibration", e);
        }
    }

    // ---------------------------------------------------------------- séances

    /** Démarre une séance (après confirmation si une autre est en cours). */
    public void startSession(String name, List<ExerciseEntry> plan, long programKey, int programDay) {
        Runnable go = () -> {
            stopRest();
            store.startSession(name, plan, programKey, programDay);
            openSession();
        };
        if (store.current == null) {
            go.run();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Une séance est déjà en cours")
                .setMessage("« " + store.current.name + " » n'est pas terminée. Si tu la remplaces, elle ne sera pas enregistrée.")
                .setPositiveButton("Voir la séance", (d, w) -> openSession())
                .setNeutralButton("Remplacer", (d, w) -> go.run())
                .setNegativeButton("Annuler", null)
                .show();
    }

    /** Affiche la séance en cours (onglet Sport). */
    public void openSession() {
        TrainingScreen.segment = TrainingScreen.MUSCU;
        goTab(TAB_TRAINING);
        if (store.current != null) show(new SessionScreen());
    }

    /** Ouvre l'onglet Sport sur un segment (Muscu, CrossFit, Renfo). */
    public void openTraining(int segment) {
        TrainingScreen.segment = segment;
        goTab(TAB_TRAINING);
    }

    public void startProgramDay(int index) {
        Program p = store.program;
        if (p == null || index < 0 || index >= p.days.size()) return;
        startSession(Store.programSessionName(p, index), p.days.get(index).exercises, p.startedAt, index);
    }

    public void showSummary(Session s) {
        if (s == null) {
            Ui.toast(this, "Séance vide : rien n'a été enregistré.");
            return;
        }
        StringBuilder m = new StringBuilder();
        m.append(Fmt.duration(s.durationMs()));
        if (s.doneSets() > 0) m.append(" · ").append(Fmt.plural(s.doneSets(), "série")).append(" · ").append(Fmt.tonnage(s.tonnage()));
        for (com.fonte.carnet.core.BodyLog.Activity c : store.cardio(s.id)) {
            m.append("\n").append(c.name).append(" : ").append(com.fonte.carnet.core.Cardio.summary(c));
        }
        if (!s.records.isEmpty()) {
            m.append("\n\n🏆 Records battus :");
            for (String r : s.records) m.append("\n• ").append(r);
        }
        if (store.programDay(s) != null && store.program.finished()) {
            m.append("\n\nProgramme terminé, bravo ! Pense à tester ou mettre à jour ton max.");
        }
        new AlertDialog.Builder(this).setTitle("Séance enregistrée").setMessage(m).setPositiveButton("OK", null).show();
    }

    // ---------------------------------------------------------------- sauvegarde

    public void exportData() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "paradoxe-training-assistant-sauvegarde-" + Fmt.isoDate(System.currentTimeMillis()) + ".json");
        try {
            startActivityForResult(i, REQ_EXPORT);
        } catch (ActivityNotFoundException e) {
            Ui.toast(this, "Aucune appli de fichiers disponible.");
        }
    }

    public void importData() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, REQ_IMPORT);
        } catch (ActivityNotFoundException e) {
            Ui.toast(this, "Aucune appli de fichiers disponible.");
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        Screen top = top();
        if (top != null && top.onActivityResult(request, result, data)) return;
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (request == REQ_EXPORT) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new IOException("fichier inaccessible");
                out.write(store.exportJson().getBytes(StandardCharsets.UTF_8));
                Ui.toast(this, "Sauvegarde exportée.");
            } catch (Exception e) {
                Ui.info(this, "Export impossible", String.valueOf(e.getMessage()));
            }
        } else if (request == REQ_IMPORT) {
            final String json;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new IOException("fichier inaccessible");
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] chunk = new byte[16384];
                int n;
                while ((n = in.read(chunk)) > 0) buf.write(chunk, 0, n);
                json = new String(buf.toByteArray(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                Ui.info(this, "Import impossible", String.valueOf(e.getMessage()));
                return;
            }
            Ui.confirm(this, "Importer cette sauvegarde ?",
                    "Toutes les données actuelles (séances, modèles, programme) seront remplacées par celles du fichier.",
                    "Remplacer", () -> {
                        try {
                            store.importJson(json);
                            stopRest();
                            goTab(TAB_HOME);
                            Ui.toast(this, "Données importées.");
                        } catch (Exception e) {
                            Ui.info(this, "Import impossible",
                                    "Ce fichier n'est pas une sauvegarde valide de l'appli.\n\n" + e.getMessage());
                        }
                    });
        }
    }
}

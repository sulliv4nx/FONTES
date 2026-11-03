package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.function.Consumer;

/** Palette et petites fabriques de vues (toute l'interface est construite en code). */
public final class Ui {
    private Ui() {
    }

    // Palette Fonte v1.1 : graphite, acier bleuté et turquoise lumineux.
    public static final int BG = 0xFF0B111B;
    public static final int SURFACE = 0xFF151E2B;
    public static final int SURFACE2 = 0xFF222E3E;
    public static final int LINE = 0xFF344253;
    public static final int TEXT = 0xFFF3F7FA;
    public static final int MUTED = 0xFFB6C3CF;
    public static final int FAINT = 0xFF8798AB;
    public static final int ACCENT = 0xFF69E6D2;
    public static final int ACCENT_SOFT = 0x2969E6D2;
    public static final int OK = 0xFF85DBA1;
    public static final int OK_SOFT = 0x2685DBA1;
    public static final int DANGER = 0xFFFF7885;

    public static final int PRIMARY = 0;
    public static final int SECONDARY = 1;
    public static final int GHOST = 2;
    public static final int DANGER_GHOST = 3;

    public static final int DECIMAL = 0;
    public static final int INTEGER = 1;
    public static final int TEXT_FIELD = 2;

    private static float density = 3f;

    public static void init(Context c) {
        density = c.getResources().getDisplayMetrics().density;
    }

    public static int dp(float v) {
        return Math.round(v * density);
    }

    // ---------------------------------------------------------------- fonds

    public static GradientDrawable rect(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    public static GradientDrawable outline(int fill, int stroke, float radiusDp) {
        GradientDrawable g = rect(fill, radiusDp);
        g.setStroke(dp(1.5f), stroke);
        return g;
    }

    /** Fond avec effet d'appui (ripple). */
    public static Drawable pressable(Drawable content, float radiusDp) {
        return new RippleDrawable(ColorStateList.valueOf(0x30FFFFFF), content, rect(0xFFFFFFFF, radiusDp));
    }

    // ---------------------------------------------------------------- textes

    public static TextView text(Context c, CharSequence s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setLineSpacing(0, 1.12f);
        return t;
    }

    public static TextView medium(Context c, CharSequence s, float sp, int color) {
        TextView t = text(c, s, sp, color);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    public static TextView heading(Context c, CharSequence s) {
        TextView t = text(c, s, 24, TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    /** Petit intitulé de section en capitales. */
    public static TextView section(Context c, String s) {
        TextView t = medium(c, s.toUpperCase(java.util.Locale.FRENCH), 12, MUTED);
        t.setLetterSpacing(0.08f);
        t.setLayoutParams(mlp(-1, -2, 4, 24, 4, 8));
        return t;
    }

    public static TextView badge(Context c, String s, int color, int bg) {
        TextView t = medium(c, s, 12, color);
        t.setBackground(rect(bg, 8));
        t.setPadding(dp(8), dp(3), dp(8), dp(3));
        return t;
    }

    /** Chiffres à chasse fixe (chronos, charges). */
    public static void tabular(TextView t) {
        t.setFontFeatureSettings("tnum");
    }

    // ---------------------------------------------------------------- conteneurs

    public static LinearLayout vbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout hbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = vbox(c);
        l.setBackground(outline(SURFACE, LINE, 16));
        l.setPadding(dp(16), dp(16), dp(16), dp(16));
        l.setLayoutParams(mlp(-1, -2, 0, 0, 0, 12));
        return l;
    }

    /** Carte cliquable. */
    public static LinearLayout clickCard(Context c, View.OnClickListener l) {
        LinearLayout card = card(c);
        card.setBackground(pressable(outline(SURFACE, LINE, 16), 16));
        card.setOnClickListener(l);
        return card;
    }

    /** Page défilante avec marges. Renvoie le conteneur où ajouter le contenu. */
    public static ScrollView page(Context c, LinearLayout content) {
        ScrollView sv = new ScrollView(c);
        sv.setFillViewport(true);
        sv.setClipToPadding(false);
        int screenDp = c.getResources().getConfiguration().screenWidthDp;
        int horizontalPad = screenDp < 360 ? 12 : 18;
        content.setPadding(dp(horizontalPad), dp(12), dp(horizontalPad), dp(28));
        if (screenDp >= 700) {
            android.widget.FrameLayout.LayoutParams params =
                    new android.widget.FrameLayout.LayoutParams(dp(680), -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            sv.addView(content, params);
        } else {
            sv.addView(content, new ViewGroup.LayoutParams(-1, -2));
        }
        return sv;
    }

    public static View space(Context c, float dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dp)));
        return v;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(mlp(-1, 1, 0, 8, 0, 8));
        return v;
    }

    /** Barre de progression simple. */
    public static View progress(Context c, int done, int total) {
        LinearLayout bar = hbox(c);
        bar.setBackground(rect(SURFACE2, 4));
        bar.setLayoutParams(mlp(-1, dp(8), 0, 10, 0, 6));
        View fill = new View(c);
        fill.setBackground(rect(ACCENT, 4));
        float ratio = total <= 0 ? 0 : Math.min(1f, done / (float) total);
        bar.setWeightSum(1f);
        bar.addView(fill, new LinearLayout.LayoutParams(0, -1, ratio));
        return bar;
    }

    // ---------------------------------------------------------------- layout params

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, -2, w);
    }

    /** Marges en dp. */
    public static LinearLayout.LayoutParams mlp(int w, int h, float l, float t, float r, float b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    // ---------------------------------------------------------------- contrôles

    public static TextView button(Context c, String label, int kind, View.OnClickListener l) {
        TextView b = medium(c, label, 16, TEXT);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(52));
        b.setPadding(dp(18), dp(12), dp(18), dp(12));
        int bg;
        switch (kind) {
            case PRIMARY:
                bg = ACCENT;
                b.setTextColor(BG);
                break;
            case SECONDARY:
                bg = SURFACE2;
                break;
            case DANGER_GHOST:
                bg = 0x00000000;
                b.setTextColor(DANGER);
                break;
            default:
                bg = 0x00000000;
                b.setTextColor(ACCENT);
                break;
        }
        b.setBackground(pressable(rect(bg, 14), 14));
        b.setOnClickListener(l);
        b.setLayoutParams(mlp(-1, -2, 0, 4, 0, 4));
        return b;
    }

    public static ImageView iconButton(Context c, int icon, int color, View.OnClickListener l) {
        ImageView v = new ImageView(c);
        v.setImageDrawable(new Icon(icon, color));
        v.setPadding(dp(12), dp(12), dp(12), dp(12));
        v.setBackground(pressable(rect(0x00000000, 24), 24));
        v.setOnClickListener(l);
        v.setLayoutParams(lp(dp(48), dp(48)));
        return v;
    }

    public static EditText field(Context c, String hint, int mode) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setTextColor(TEXT);
        e.setHintTextColor(FAINT);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        e.setMinHeight(dp(48));
        e.setBackground(outline(SURFACE2, LINE, 11));
        e.setPadding(dp(12), dp(11), dp(12), dp(11));
        e.setSingleLine(true);
        e.setSelectAllOnFocus(true);
        if (mode == DECIMAL) {
            e.setKeyListener(DigitsKeyListener.getInstance("0123456789.,"));
            e.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        } else if (mode == INTEGER) {
            e.setInputType(InputType.TYPE_CLASS_NUMBER);
        } else {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        tabular(e);
        return e;
    }

    /** Champ précédé de son libellé. */
    public static LinearLayout labelled(Context c, String label, View field) {
        LinearLayout box = vbox(c);
        TextView t = text(c, label, 13, MUTED);
        t.setPadding(dp(2), 0, 0, dp(6));
        box.addView(t);
        box.addView(field, lp(-1, -2));
        box.setLayoutParams(mlp(-1, -2, 0, 6, 0, 10));
        return box;
    }

    public static TextView chip(Context c, String label, boolean selected, View.OnClickListener l) {
        TextView t = medium(c, label, 14, selected ? BG : TEXT);
        t.setGravity(Gravity.CENTER);
        t.setMinHeight(dp(42));
        t.setPadding(dp(14), dp(10), dp(14), dp(10));
        t.setBackground(pressable(selected ? rect(ACCENT, 20) : outline(SURFACE, LINE, 20), 20));
        t.setOnClickListener(l);
        t.setLayoutParams(mlp(-2, -2, 0, 0, 8, 0));
        return t;
    }

    /** Rangée de puces défilant horizontalement. */
    public static HorizontalScrollView chipRow(Context c, View... chips) {
        HorizontalScrollView h = new HorizontalScrollView(c);
        h.setHorizontalScrollBarEnabled(false);
        LinearLayout row = hbox(c);
        for (View v : chips) row.addView(v);
        h.addView(row);
        h.setLayoutParams(mlp(-1, -2, 0, 0, 0, 8));
        return h;
    }

    /** Ligne « libellé … valeur ». */
    public static LinearLayout row(Context c, String label, String value) {
        LinearLayout r = hbox(c);
        r.setPadding(0, dp(4), 0, dp(4));
        TextView l = text(c, label, 15, MUTED);
        l.setMinWidth(0);
        l.setEllipsize(TextUtils.TruncateAt.END);
        l.setMaxLines(2);
        r.addView(l, weight(1));
        TextView v = medium(c, value, 15, TEXT);
        tabular(v);
        r.addView(v);
        return r;
    }

    /** Pavé de statistique (valeur en grand, libellé dessous). */
    public static LinearLayout stat(Context c, String value, String label) {
        LinearLayout box = vbox(c);
        box.setBackground(outline(SURFACE, LINE, 14));
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        TextView v = medium(c, value, 18, TEXT);
        tabular(v);
        v.setSingleLine(true);
        v.setEllipsize(TextUtils.TruncateAt.END);
        box.addView(v);
        box.addView(text(c, label, 12, MUTED));
        return box;
    }

    public static LinearLayout statRow(Context c, LinearLayout... stats) {
        // Sur petit écran, ne pas écraser trois valeurs dans des cartes illisibles.
        if (stats.length == 3 && c.getResources().getConfiguration().screenWidthDp < 380) {
            LinearLayout column = vbox(c);
            column.setLayoutParams(mlp(-1, -2, 0, 4, 0, 10));
            LinearLayout pair = hbox(c);
            pair.addView(stats[0], new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(0, -2, 1);
            second.leftMargin = dp(8);
            pair.addView(stats[1], second);
            column.addView(pair, lp(-1, -2));
            column.addView(stats[2], mlp(-1, -2, 0, 8, 0, 0));
            return column;
        }
        LinearLayout r = hbox(c);
        r.setLayoutParams(mlp(-1, -2, 0, 4, 0, 10));
        for (int i = 0; i < stats.length; i++) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) p.leftMargin = dp(8);
            r.addView(stats[i], p);
        }
        return r;
    }

    // ---------------------------------------------------------------- dialogues

    public static void toast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
    }

    public static void confirm(Context c, String title, String message, String yes, Runnable onYes) {
        new AlertDialog.Builder(c)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(yes, (d, w) -> onYes.run())
                .setNegativeButton("Annuler", null)
                .show();
    }

    public static void info(Context c, String title, String message) {
        new AlertDialog.Builder(c).setTitle(title).setMessage(message).setPositiveButton("OK", null).show();
    }

    /** Demande un texte (nom de séance, de modèle, d'exercice…). */
    public static void prompt(Context c, String title, String initial, String hint, Consumer<String> onOk) {
        LinearLayout box = vbox(c);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        EditText e = field(c, hint, TEXT_FIELD);
        if (initial != null) e.setText(initial);
        box.addView(e, lp(-1, -2));
        AlertDialog d = new AlertDialog.Builder(c)
                .setTitle(title)
                .setView(box)
                .setPositiveButton("OK", null)
                .setNegativeButton("Annuler", null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String s = e.getText().toString().trim();
            if (s.isEmpty()) {
                e.setError("Obligatoire");
                return;
            }
            d.dismiss();
            onOk.accept(s);
        }));
        if (d.getWindow() != null) {
            d.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        d.show();
        e.requestFocus();
    }

    /** Appelle f à chaque modification du texte. */
    public static void onText(EditText e, Consumer<String> f) {
        e.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                f.accept(s.toString());
            }
        });
    }

    public static void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
}

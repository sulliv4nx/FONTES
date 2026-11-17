package com.fonte.carnet.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;

import java.util.Calendar;
import java.util.Locale;

/** Date, heure et durée d'une séance faite plus tôt (ou mal datée). */
final class WhenSheet {
    private WhenSheet() {
    }

    interface Done {
        void on(long time, int minutes);
    }

    /**
     * @param minutes durée proposée en minutes, ou -1 pour ne pas la demander
     */
    static void show(MainActivity a, String title, long time, int minutes, Done done) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        LinearLayout box = Ui.vbox(a);
        TextView t = Ui.text(a, title, 19, Ui.TEXT);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        box.addView(t);
        TextView sub = Ui.text(a, "Pour une séance faite plus tôt et notée après coup.", 13, Ui.MUTED);
        sub.setPadding(0, Ui.dp(2), 0, Ui.dp(10));
        box.addView(sub);

        TextView dayValue = value(a);
        TextView hourValue = value(a);
        Runnable paint = () -> {
            dayValue.setText(Fmt.day(cal.getTimeInMillis()));
            hourValue.setText(String.format(Locale.FRENCH, "%dh%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
        };
        paint.run();

        box.addView(row(a, "Date", dayValue, v -> {
            DatePickerDialog dp = new DatePickerDialog(a, (picker, y, m, d) -> {
                cal.set(y, m, d);
                paint.run();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dp.getDatePicker().setMaxDate(System.currentTimeMillis());
            dp.show();
        }));
        LinearLayout quick = Ui.hbox(a);
        quick.setPadding(0, Ui.dp(2), 0, Ui.dp(6));
        String[] labels = {"Aujourd'hui", "Hier", "Avant-hier"};
        for (int i = 0; i < labels.length; i++) {
            int back = i;
            TextView chip = Ui.chip(a, labels[i], false, v -> {
                Calendar now = Calendar.getInstance();
                now.add(Calendar.DAY_OF_MONTH, -back);
                cal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
                paint.run();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            if (i > 0) lp.leftMargin = Ui.dp(6);
            quick.addView(chip, lp);
        }
        box.addView(quick);
        box.addView(row(a, "Heure de début", hourValue, v -> new TimePickerDialog(a, (picker, h, m) -> {
            cal.set(Calendar.HOUR_OF_DAY, h);
            cal.set(Calendar.MINUTE, m);
            paint.run();
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()));

        EditText dur = null;
        if (minutes >= 0) {
            dur = Ui.field(a, "60", Ui.INTEGER);
            dur.setText(String.valueOf(Math.max(1, minutes)));
            LinearLayout db = Ui.labelled(a, "Durée (minutes)", dur);
            db.setPadding(0, Ui.dp(8), 0, 0);
            box.addView(db);
        }

        TextView cancel = Ui.button(a, "Annuler", Ui.GHOST, null);
        TextView ok = Ui.button(a, "Enregistrer", Ui.PRIMARY, null);
        LinearLayout buttons = Ui.hbox(a);
        buttons.setPadding(0, Ui.dp(14), 0, 0);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, -2, 1);
        l1.rightMargin = Ui.dp(8);
        buttons.addView(cancel, l1);
        buttons.addView(ok, new LinearLayout.LayoutParams(0, -2, 2));
        box.addView(buttons);

        Dialog d = Ui.sheet(a, box);
        EditText durField = dur;
        cancel.setOnClickListener(v -> d.dismiss());
        ok.setOnClickListener(v -> {
            int m = -1;
            if (durField != null) {
                m = Fmt.parseInt(durField.getText().toString());
                if (m <= 0 || m > 600) {
                    durField.setError("Entre 1 et 600 min");
                    return;
                }
            }
            long when = cal.getTimeInMillis();
            if (when > System.currentTimeMillis() + 60_000L) {
                Ui.toast(a, "Cette date est dans le futur.");
                return;
            }
            if (durField != null) Ui.hideKeyboard(durField);
            d.dismiss();
            done.on(when, m);
        });
        d.show();
    }

    private static TextView value(MainActivity a) {
        TextView v = Ui.medium(a, "", 15, Ui.TEXT);
        v.setBackground(Ui.rect(Ui.SURFACE2, 8));
        v.setPadding(Ui.dp(10), Ui.dp(6), Ui.dp(10), Ui.dp(6));
        Ui.tabular(v);
        return v;
    }

    private static View row(MainActivity a, String label, TextView value, View.OnClickListener l) {
        LinearLayout row = Ui.hbox(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(Ui.dp(48));
        row.setBackground(Ui.pressable(Ui.rect(0x00000000, 10), 10));
        row.addView(Ui.text(a, label, 15, Ui.TEXT), Ui.weight(1));
        row.addView(value);
        row.setContentDescription(label);
        row.setOnClickListener(l);
        return row;
    }
}

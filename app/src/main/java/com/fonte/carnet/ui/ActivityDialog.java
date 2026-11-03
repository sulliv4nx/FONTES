package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fonte.carnet.core.Fmt;
import com.fonte.carnet.core.Nutrition;

/** Ajout d'une activité hors séance (course, vélo…) avec estimation des calories (MET). */
final class ActivityDialog {
    private ActivityDialog() {
    }

    static void show(MainActivity a, int day, Runnable onDone) {
        new AlertDialog.Builder(a)
                .setTitle("Quelle activité ?")
                .setItems(Nutrition.ACTIVITIES, (d, which) -> details(a, day, which, onDone))
                .setNegativeButton("Annuler", null)
                .show();
    }

    private static void details(MainActivity a, int day, int which, Runnable onDone) {
        String name = Nutrition.ACTIVITIES[which];
        double met = Nutrition.METS[which];
        double weight = a.store.weightForCalc();
        LinearLayout box = Ui.vbox(a);
        box.setPadding(Ui.dp(20), Ui.dp(4), Ui.dp(20), Ui.dp(4));
        EditText minutes = Ui.field(a, "30", Ui.INTEGER);
        minutes.setText("30");
        EditText kcal = Ui.field(a, "calculé", Ui.INTEGER);
        TextView live = Ui.medium(a, "", 15, Ui.ACCENT);
        Runnable update = () -> {
            int m = Fmt.parseInt(minutes.getText().toString());
            live.setText("≈ " + Nutrition.kcal(met, weight, m) + " kcal pour " + m + " min (" + Fmt.kg(weight) + " kg)");
        };
        Ui.onText(minutes, t -> update.run());
        update.run();
        box.addView(Ui.labelled(a, "Durée (minutes)", minutes));
        box.addView(live, Ui.mlp(-1, -2, 0, 0, 0, 10));
        box.addView(Ui.labelled(a, "Ou calories de ta montre (facultatif)", kcal));
        AlertDialog d = new AlertDialog.Builder(a)
                .setTitle(name)
                .setView(box)
                .setPositiveButton("Ajouter", null)
                .setNegativeButton("Annuler", null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int m = Fmt.parseInt(minutes.getText().toString());
            if (m <= 0 || m > 600) {
                minutes.setError("Entre 1 et 600 min");
                return;
            }
            int manual = Fmt.parseInt(kcal.getText().toString());
            int k = manual > 0 ? manual : Nutrition.kcal(met, weight, m);
            a.store.addActivity(name, m, k, day);
            Ui.hideKeyboard(minutes);
            d.dismiss();
            Ui.toast(a, name + " : " + k + " kcal ajoutées.");
            if (onDone != null) onDone.run();
        }));
        d.show();
    }
}

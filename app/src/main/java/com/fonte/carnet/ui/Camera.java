package com.fonte.carnet.ui;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;

/** Photo (appareil photo vers le fichier temporaire de PhotoProvider) ou image de la galerie. */
final class Camera {
    private Camera() {
    }

    /** Propose « Appareil photo » ou « Galerie », puis lance l'activité correspondante. */
    static void choose(MainActivity a, String title, int reqCamera, int reqGallery) {
        String[] items = {"Prendre une photo", "Choisir dans la galerie"};
        new AlertDialog.Builder(a)
                .setTitle(title)
                .setItems(items, (d, w) -> {
                    if (w == 0) capture(a, reqCamera);
                    else pick(a, reqGallery);
                })
                .show();
    }

    static void capture(MainActivity a, int request) {
        File f = PhotoProvider.file(a);
        if (f.exists() && !f.delete()) f.deleteOnExit();
        Uri out = PhotoProvider.uri();
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        i.putExtra(MediaStore.EXTRA_OUTPUT, out);
        i.setClipData(ClipData.newRawUri("photo", out));
        i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            a.startActivityForResult(i, request);
        } catch (ActivityNotFoundException | SecurityException e) {
            Ui.toast(a, "Appareil photo indisponible : choisis plutôt une image de la galerie.");
        }
    }

    static void pick(MainActivity a, int request) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            a.startActivityForResult(i, request);
        } catch (ActivityNotFoundException e) {
            Ui.toast(a, "Aucune galerie disponible.");
        }
    }

    /** Image reçue (photo prise ou image choisie), ou null. */
    static Uri result(MainActivity a, boolean fromCamera, Intent data) {
        if (fromCamera) return PhotoProvider.file(a).length() > 0 ? PhotoProvider.uri() : null;
        return data == null ? null : data.getData();
    }
}

package com.fonte.carnet.ui;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lecture d'un code-barres produit (EAN / UPC) sur une photo, sur le téléphone. */
final class Barcodes {
    private Barcodes() {
    }

    interface Callback {
        /** code non null en cas de succès. Appelé sur le fil principal. */
        void done(String code, String error);
    }

    private static final Pattern DIGITS = Pattern.compile("(\\d{8,14})");

    static void scan(Context c, Uri uri, Callback cb) {
        try {
            InputImage image = InputImage.fromFilePath(c, uri);
            BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
                    .build();
            BarcodeScanner scanner = BarcodeScanning.getClient(options);
            scanner.process(image)
                    .addOnSuccessListener(list -> {
                        close(scanner);
                        String code = null;
                        for (Barcode b : list) {
                            if (b.getRawValue() != null && !b.getRawValue().isEmpty()) {
                                code = b.getRawValue();
                                break;
                            }
                        }
                        if (code != null) cb.done(code, null);
                        else digitsFallback(c, uri, cb);
                    })
                    .addOnFailureListener(e -> {
                        close(scanner);
                        Log.e("Fonte", "code-barres", e);
                        digitsFallback(c, uri, cb);
                    });
        } catch (Exception e) {
            Log.e("Fonte", "code-barres", e);
            cb.done(null, e.getMessage());
        }
    }

    /** Si le code n'a pas pu être décodé, on cherche les chiffres imprimés sous le code-barres. */
    private static void digitsFallback(Context c, Uri uri, Callback cb) {
        Ocr.recognize(c, uri, (text, error) -> {
            String code = text == null ? null : digits(text);
            if (code != null) cb.done(code, null);
            else cb.done(null, "Aucun code-barres lisible sur la photo.");
        });
    }

    /** Suite de 8 à 14 chiffres (espaces entre groupes de chiffres ignorés). */
    static String digits(String text) {
        String joined = text.replaceAll("(?<=\\d)[ \\u00A0](?=\\d)", "");
        Matcher m = DIGITS.matcher(joined);
        String best = null;
        while (m.find()) {
            String d = m.group(1);
            if (d.length() == 13 || d.length() == 8 || d.length() == 12) return d;
            if (best == null) best = d;
        }
        return best;
    }

    private static void close(BarcodeScanner s) {
        try {
            s.close();
        } catch (Exception ignored) {
            // rien à faire
        }
    }
}

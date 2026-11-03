package com.fonte.carnet.ui;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

/** Lecture du texte d'une photo, sur le téléphone (ML Kit, modèle intégré : fonctionne hors ligne). */
final class Ocr {
    private Ocr() {
    }

    interface Callback {
        /** text non null en cas de succès, sinon error. Appelé sur le fil principal. */
        void done(String text, String error);
    }

    static void recognize(Context c, Uri uri, Callback cb) {
        try {
            InputImage image = InputImage.fromFilePath(c, uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image)
                    .addOnSuccessListener(result -> {
                        close(recognizer);
                        cb.done(result.getText(), null);
                    })
                    .addOnFailureListener(e -> {
                        close(recognizer);
                        Log.e("Fonte", "OCR", e);
                        cb.done(null, e.getMessage());
                    });
        } catch (Exception e) {
            Log.e("Fonte", "OCR", e);
            cb.done(null, e.getMessage());
        }
    }

    private static void close(TextRecognizer r) {
        try {
            r.close();
        } catch (Exception ignored) {
            // rien à faire
        }
    }
}

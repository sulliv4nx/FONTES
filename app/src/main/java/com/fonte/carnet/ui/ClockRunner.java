package com.fonte.carnet.ui;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.fonte.carnet.core.Clock;
import com.fonte.carnet.core.Settings;

/**
 * Fait tourner un chrono (WOD, minuteur, circuit de renfo) : temps réel, pauses, bips et vibrations.
 * Un seul chrono à la fois ; il continue si l'on change d'écran.
 */
final class ClockRunner {
    private static final String TAG = "Fonte";
    private static ClockRunner active;

    final Clock clock;
    final String wodId;
    final String routineId;
    /** Compteur de tours (AMRAP / For Time). */
    int rounds;
    boolean scoreSaved;

    private final Context app;
    private final Settings settings;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long base;
    private long pausedAt = -1;
    private int lastIndex = -1;
    private int lastSecond = -1;
    private boolean endSignaled;
    private Runnable listener;
    private ToneGenerator tone;
    private final Runnable loop = new Runnable() {
        @Override
        public void run() {
            step();
            if (active == ClockRunner.this && !clock.finished() && pausedAt < 0) handler.postDelayed(this, 100);
        }
    };

    private ClockRunner(Context c, Settings settings, Clock clock, String wodId, String routineId) {
        this.app = c.getApplicationContext();
        this.settings = settings;
        this.clock = clock;
        this.wodId = wodId;
        this.routineId = routineId;
    }

    static ClockRunner active() {
        return active;
    }

    /** Un chrono est lancé et pas encore terminé. */
    static boolean running() {
        return active != null && !active.clock.finished();
    }

    static ClockRunner start(Context c, Settings settings, Clock clock, String wodId, String routineId) {
        if (active != null) active.stop();
        ClockRunner r = new ClockRunner(c, settings, clock, wodId, routineId);
        active = r;
        r.base = SystemClock.elapsedRealtime();
        if (clock.phases.isEmpty() || !clock.phases.get(0).prep) r.beep(false);
        r.handler.post(r.loop);
        return r;
    }

    /** Temps actif (ms), pauses exclues. */
    long t() {
        return (pausedAt >= 0 ? pausedAt : SystemClock.elapsedRealtime()) - base;
    }

    boolean paused() {
        return pausedAt >= 0;
    }

    void pause() {
        if (pausedAt >= 0 || clock.finished()) return;
        pausedAt = SystemClock.elapsedRealtime();
        handler.removeCallbacks(loop);
        notifyListener();
    }

    void resume() {
        if (pausedAt < 0) return;
        base += SystemClock.elapsedRealtime() - pausedAt;
        pausedAt = -1;
        handler.removeCallbacks(loop);
        handler.post(loop);
    }

    void next() {
        clock.next(t());
        step();
        if (!clock.finished() && pausedAt < 0) {
            handler.removeCallbacks(loop);
            handler.post(loop);
        }
    }

    void previous() {
        clock.previous(t());
        endSignaled = false;
        step();
        if (pausedAt < 0) {
            handler.removeCallbacks(loop);
            handler.post(loop);
        }
    }

    void stop() {
        handler.removeCallbacks(loop);
        if (tone != null) {
            tone.release();
            tone = null;
        }
        listener = null;
        if (active == this) active = null;
    }

    void setListener(Runnable r) {
        listener = r;
    }

    private void notifyListener() {
        if (listener == null) return;
        try {
            listener.run();
        } catch (RuntimeException e) {
            Log.e(TAG, "chrono", e);
        }
    }

    private void step() {
        long t = t();
        clock.update(t);
        int index = clock.index();
        if (index != lastIndex) {
            if (lastIndex >= 0) {
                if (clock.finished()) signalEnd();
                else beep(false);
            }
            lastIndex = index;
            lastSecond = -1;
        }
        if (!clock.finished()) {
            int s = clock.secondsLeft(t);
            if (s != lastSecond) {
                if (s >= 1 && s <= 3) tick();
                lastSecond = s;
            }
        } else if (!endSignaled) {
            signalEnd();
        }
        notifyListener();
    }

    private void signalEnd() {
        if (endSignaled) return;
        endSignaled = true;
        sound(ToneGenerator.TONE_SUP_CONFIRM, 700);
        vibrate(new long[]{0, 400, 150, 400, 150, 700});
    }

    /** Bip court des 3 dernières secondes. */
    private void tick() {
        sound(ToneGenerator.TONE_PROP_BEEP, 120);
    }

    /** Bip de changement de phase (départ, travail, repos…). */
    private void beep(boolean quiet) {
        sound(ToneGenerator.TONE_PROP_BEEP2, 350);
        if (!quiet) vibrate(new long[]{0, 250});
    }

    private void sound(int toneType, int ms) {
        if (!settings.sounds) return;
        try {
            if (tone == null) tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 90);
            tone.startTone(toneType, ms);
        } catch (RuntimeException e) {
            Log.e(TAG, "son", e);
        }
    }

    @SuppressWarnings("deprecation")
    private void vibrate(long[] pattern) {
        if (!settings.vibrate) return;
        try {
            Vibrator v = (Vibrator) app.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            v.vibrate(VibrationEffect.createWaveform(pattern, -1), attrs);
        } catch (RuntimeException e) {
            Log.e(TAG, "vibration", e);
        }
    }
}

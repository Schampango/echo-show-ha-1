package com.burnfix.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

/**
 * Zeichnet abwechselnd Vollflaechen-Farben und ein bewegtes RGB-Muster.
 * Zwischen den Farben laeuft ein kurzer Bewegungsframe, und auch waehrend
 * der Farben wandert ein Balken durchs Bild, damit wirklich jeder Pixel
 * regelmaessig seinen Zustand aendert.
 */
public class RefreshView extends View {

    // Vollflaechen-Palette inkl. Voll-Weiss und Voll-Schwarz
    private static final int[] SOLIDS = {
            0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF,
            0xFF00FFFF, 0xFFFF00FF, 0xFFFFFF00, 0xFF000000, 0xFF808080
    };

    // Verweildauer je Farbe (ms), per Tap umschaltbar
    private static final int[] SPEEDS = {2000, 1000, 400, 120};

    private int speedIdx = 1;
    private int solidIdx = 0;
    private boolean noiseFrame = false;
    private long lastSwitch = 0L;
    private long phase = 0L;
    private boolean running = false;

    private final Paint paint = new Paint();

    public RefreshView(Context c) {
        super(c);
        setKeepScreenOn(true);
    }

    void start() {
        running = true;
        lastSwitch = SystemClock.uptimeMillis();
        postInvalidateOnAnimation();
    }

    void stop() {
        running = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            speedIdx = (speedIdx + 1) % SPEEDS.length;
            return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int w = getWidth();
        final int h = getHeight();
        final long now = SystemClock.uptimeMillis();

        if (now - lastSwitch >= SPEEDS[speedIdx]) {
            lastSwitch = now;
            if (!noiseFrame) {
                noiseFrame = true;                      // als naechstes ein Bewegungsframe
            } else {
                noiseFrame = false;
                solidIdx = (solidIdx + 1) % SOLIDS.length;
            }
        }

        if (noiseFrame) {
            drawMotion(canvas, w, h);
        } else {
            int c = SOLIDS[solidIdx];
            canvas.drawColor(c);
            // wandernder Kontrastbalken, damit auch waehrend der Farbe Pixel schalten
            paint.setColor(c == 0xFFFFFFFF ? 0xFF000000 : 0xFFFFFFFF);
            int bar = Math.max(4, h / 40);
            int span = h + bar;
            int y = (int) (((phase * (long) Math.max(1, h / 160)) % span)) - bar;
            canvas.drawRect(0, y, w, y + bar, paint);
        }

        phase++;
        if (running) postInvalidateOnAnimation();
    }

    private void drawMotion(Canvas canvas, int w, int h) {
        int cell = Math.max(8, w / 48);
        int off = (int) ((phase * 3) % (cell * 3L));
        for (int x = -cell * 3; x < w; x += cell) {
            int m = (((x + off) / cell) % 3 + 3) % 3;
            paint.setColor(m == 0 ? 0xFFFF0000 : (m == 1 ? 0xFF00FF00 : 0xFF0000FF));
            canvas.drawRect(x, 0, x + cell, h, paint);
        }
    }
}

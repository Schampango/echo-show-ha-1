package com.burnfix.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * BurnFix – Subpixel-/Retention-Refresher für LCD-Panels.
 *
 * Hält den Bildschirm an, setzt maximale Helligkeit und lässt eine
 * Vollflaechen-Animation (Farben + bewegtes Muster) endlos laufen.
 * Antippen wechselt die Geschwindigkeit, Zurueck-Taste beendet.
 */
public class MainActivity extends Activity {

    private RefreshView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        // Display nie abschalten
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Maximale Helligkeit erzwingen (wichtig fuers "Durchbewegen")
        WindowManager.LayoutParams lp = w.getAttributes();
        lp.screenBrightness = 1.0f;
        w.setAttributes(lp);

        view = new RefreshView(this);
        setContentView(view);

        hideSystemUi();

        Toast.makeText(this,
                "Tippen = Tempo wechseln  •  Zurueck = beenden",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    private void hideSystemUi() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (view != null) view.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (view != null) view.stop();
    }
}

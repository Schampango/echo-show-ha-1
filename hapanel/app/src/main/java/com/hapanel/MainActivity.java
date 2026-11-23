package com.hapanel;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int COL_BG     = 0xFF0D0D0D;
    private static final int COL_CARD   = 0xFF262626;
    private static final int COL_ON     = 0xFFF5C518; // amber = an
    private static final int COL_OFF    = 0xFF4A80C0; // blau = aus
    private static final int COL_TEXT   = 0xFFECECEC;

    private final ExecutorService io = Executors.newFixedThreadPool(4);
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final List<Tile> tiles = new ArrayList<>();

    private static WebConfigServer web;

    private Config cfg;
    private boolean polling = false;

    private static class Tile {
        String name, entity, iconKey;
        ImageView icon;
        boolean on;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildUi();
        startWeb();
    }

    private void startWeb() {
        if (web != null) return;
        final Config c = Config.load(this);
        try {
            WebConfigServer s = new WebConfigServer(this, c.webPort);
            s.onSaved = new Runnable() {
                @Override public void run() {
                    ui.post(new Runnable() {
                        @Override public void run() {
                            cfg = Config.load(MainActivity.this);
                            buildUi();
                        }
                    });
                }
            };
            s.start(fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            web = s;
            Logg.add("Webserver gestartet: http://" + localIp() + ":" + c.webPort);
            toast("Konfig im Browser: http://" + localIp() + ":" + c.webPort);
        } catch (Exception e) {
            Logg.add("Webserver-Start fehlgeschlagen: " + e.getMessage());
            toast("Webserver-Start fehlgeschlagen: " + e.getMessage());
        }
    }

    private String localIp() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> ifs =
                    java.net.NetworkInterface.getNetworkInterfaces();
            while (ifs.hasMoreElements()) {
                java.net.NetworkInterface nif = ifs.nextElement();
                if (!nif.isUp() || nif.isLoopback()) continue;
                java.util.Enumeration<java.net.InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress a = addrs.nextElement();
                    if (!a.isLoopbackAddress() && a instanceof java.net.Inet4Address) {
                        return a.getHostAddress();
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return "<IP>";
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) immersive();
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
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
        cfg = Config.load(this);
        buildUi();
        polling = true;
        ui.post(poll);
    }

    @Override
    protected void onPause() {
        super.onPause();
        polling = false;
        ui.removeCallbacks(poll);
    }

    // ---- UI aufbauen --------------------------------------------------------

    private GradientDrawable card() {
        GradientDrawable g = new GradientDrawable();
        g.setColor(COL_CARD);
        g.setCornerRadius(dp(14));
        return g;
    }

    private void buildUi() {
        if (cfg == null) cfg = Config.load(this);
        tiles.clear();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(COL_BG);
        root.setPadding(dp(8), dp(8), dp(8), dp(8));

        // --- linke Kachelflaeche (Grid) ---
        int cols = Math.max(1, cfg.columns);
        List<String[]> items = parseTiles(cfg.tilesText);
        int rows = Math.max(1, (int) Math.ceil(items.size() / (double) cols));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(cols);
        grid.setRowCount(rows);

        for (String[] it : items) {
            Tile t = new Tile();
            t.name = it[0];
            t.entity = it[1];
            t.iconKey = it[2];

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setBackground(card());
            cell.setPadding(dp(8), dp(10), dp(8), dp(10));

            ImageView icon = new ImageView(this);
            icon.setImageResource(Icons.res(t.iconKey));
            icon.setColorFilter(COL_OFF);
            LinearLayout.LayoutParams ip =
                    new LinearLayout.LayoutParams(dp(52), dp(52));
            icon.setLayoutParams(ip);

            TextView label = new TextView(this);
            label.setText(t.name);
            label.setTextColor(COL_TEXT);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(8);
            label.setLayoutParams(lp);

            cell.addView(icon);
            cell.addView(label);

            final Tile ft = t;
            cell.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { onTileTap(ft); }
            });

            t.icon = icon;
            tiles.add(t);

            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = 0;
            glp.height = 0;
            glp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            glp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            int m = dp(6);
            glp.setMargins(m, m, m, m);
            cell.setLayoutParams(glp);

            grid.addView(cell);
        }

        LinearLayout.LayoutParams gridLp =
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        grid.setLayoutParams(gridLp);
        root.addView(grid);

        // --- rechte Spalte: Rollo + Einstellungen ---
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rlp =
                new LinearLayout.LayoutParams(dp(150),
                        LinearLayout.LayoutParams.MATCH_PARENT);
        rlp.setMargins(dp(6), dp(6), dp(6), dp(6));
        right.setLayoutParams(rlp);

        LinearLayout rollo = new LinearLayout(this);
        rollo.setOrientation(LinearLayout.VERTICAL);
        rollo.setGravity(Gravity.CENTER);
        rollo.setBackground(card());
        rollo.setPadding(dp(8), dp(10), dp(8), dp(10));
        LinearLayout.LayoutParams rollolp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        rollo.setLayoutParams(rollolp);

        ImageView up = arrowView(false);
        up.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { rolloCmd(cfg.rolloUp, "Hoch"); }
        });
        ImageView stop = new ImageView(this);
        stop.setImageResource(R.drawable.ic_stop);
        stop.setColorFilter(COL_OFF);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(dp(70), dp(70));
        slp.topMargin = dp(10); slp.bottomMargin = dp(10);
        stop.setLayoutParams(slp);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { rolloCmd(cfg.rolloStop, "Stop"); }
        });
        ImageView down = arrowView(true);
        down.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { rolloCmd(cfg.rolloDown, "Runter"); }
        });

        TextView rolloLbl = new TextView(this);
        rolloLbl.setText("Rollo");
        rolloLbl.setTextColor(COL_TEXT);
        rolloLbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        rolloLbl.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lblp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lblp.topMargin = dp(8);
        rolloLbl.setLayoutParams(lblp);

        rollo.addView(up);
        rollo.addView(stop);
        rollo.addView(down);
        rollo.addView(rolloLbl);
        right.addView(rollo);

        root.addView(right);
        setContentView(root);
        immersive();
    }

    private ImageView arrowView(boolean down) {
        ImageView a = new ImageView(this);
        a.setImageResource(R.drawable.ic_arrow);
        a.setColorFilter(COL_OFF);
        if (down) a.setRotation(180f);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(76), dp(76));
        a.setLayoutParams(p);
        return a;
    }

    private List<String[]> parseTiles(String text) {
        List<String[]> out = new ArrayList<>();
        if (text == null) return out;
        for (String line : text.split("\\n")) {
            String s = line.trim();
            if (s.length() == 0) continue;
            String[] parts = s.split("\\|", 3);
            if (parts.length < 2) continue;
            String name = parts[0].trim();
            String entity = parts[1].trim();
            if (entity.length() == 0) continue;
            String icon = parts.length >= 3 ? parts[2].trim() : "bulb";
            if (icon.length() == 0) icon = "bulb";
            out.add(new String[]{name, entity, icon});
        }
        return out;
    }

    // ---- Aktionen -----------------------------------------------------------

    private void onTileTap(final Tile t) {
        io.submit(new Runnable() {
            @Override public void run() {
                boolean ok = Ha.toggle(cfg, t.entity);
                if (!ok) toast("HA nicht erreichbar / Fehler: " + t.name);
                String st = Ha.state(cfg, t.entity);
                applyState(t, st);
            }
        });
    }

    private void rolloCmd(final String command, final String label) {
        io.submit(new Runnable() {
            @Override public void run() {
                Logg.add("Rollo " + label + ": SSH " + cfg.sshUser + "@" + cfg.sshHost
                        + ":" + cfg.sshPort + "  cmd=\"" + command + "\""
                        + (cfg.sshKey != null && cfg.sshKey.trim().length() > 0 ? "  (Key-Auth)" : "  (Passwort-Auth)"));
                try {
                    String out = Ssh.run(cfg, command);
                    String tr = out == null ? "" : out.trim();
                    Logg.add("Rollo " + label + " OK" + (tr.length() > 0 ? ": " + tr : ""));
                    toast("Rollo: " + label);
                } catch (Exception e) {
                    Logg.add("Rollo " + label + " FEHLER: " + e.getClass().getSimpleName()
                            + ": " + e.getMessage());
                    toast("SSH-Fehler: " + e.getMessage());
                }
            }
        });
    }

    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (!polling) return;
            refreshAll();
            ui.postDelayed(this, 8000);
        }
    };

    private void refreshAll() {
        for (final Tile t : tiles) {
            io.submit(new Runnable() {
                @Override public void run() {
                    String st = Ha.state(cfg, t.entity);
                    applyState(t, st);
                }
            });
        }
    }

    private void applyState(final Tile t, final String st) {
        if (st == null) return;
        final boolean on = "on".equalsIgnoreCase(st.trim());
        ui.post(new Runnable() {
            @Override public void run() {
                t.on = on;
                t.icon.setColorFilter(on ? COL_ON : COL_OFF);
            }
        });
    }

    private void toast(final String msg) {
        ui.post(new Runnable() {
            @Override public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

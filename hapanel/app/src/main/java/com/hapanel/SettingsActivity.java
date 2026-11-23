package com.hapanel;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private static final int COL_BG   = 0xFF0D0D0D;
    private static final int COL_TEXT = 0xFFECECEC;
    private static final int COL_HINT = 0xFF888888;

    private EditText eUrl, eToken, eTiles, eCols;
    private EditText eHost, ePort, eUser, ePass, eKey;
    private EditText eUp, eStop, eDown;

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Config c = Config.load(this);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(COL_BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        sv.addView(root);

        root.addView(header("Home Assistant"));
        eUrl = field(root, "HA-URL (z.B. http://192.168.1.10:8123)", c.haUrl, InputType.TYPE_TEXT_VARIATION_URI);
        eToken = field(root, "Long-Lived Access Token", c.haToken, InputType.TYPE_CLASS_TEXT);

        root.addView(header("Kacheln (je Zeile: Name|entity_id)"));
        eTiles = multiField(root, "Esstisch|light.esstisch", c.tilesText);
        eCols = field(root, "Spalten", String.valueOf(c.columns), InputType.TYPE_CLASS_NUMBER);

        root.addView(header("Rollladen-Server (SSH)"));
        eHost = field(root, "Host/IP", c.sshHost, InputType.TYPE_TEXT_VARIATION_URI);
        ePort = field(root, "Port", String.valueOf(c.sshPort), InputType.TYPE_CLASS_NUMBER);
        eUser = field(root, "User", c.sshUser, InputType.TYPE_CLASS_TEXT);
        ePass = field(root, "Passwort (leer, wenn Key)", c.sshPass, InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);
        eKey = field(root, "Privater Key (Pfad, optional)", c.sshKey, InputType.TYPE_CLASS_TEXT);

        root.addView(header("Rollo-Kommandos"));
        eUp = field(root, "Hoch", c.rolloUp, InputType.TYPE_CLASS_TEXT);
        eStop = field(root, "Stop", c.rolloStop, InputType.TYPE_CLASS_TEXT);
        eDown = field(root, "Runter", c.rolloDown, InputType.TYPE_CLASS_TEXT);

        Button save = new Button(this);
        save.setText("Speichern");
        save.setAllCaps(false);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(20);
        save.setLayoutParams(blp);
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { save(); }
        });
        root.addView(save);

        setContentView(sv);
    }

    private TextView header(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextColor(0xFF6FA8DC);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(18);
        lp.bottomMargin = dp(4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private EditText field(LinearLayout parent, String hint, String value, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value == null ? "" : value);
        e.setInputType(inputType);
        e.setTextColor(COL_TEXT);
        e.setHintTextColor(COL_HINT);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        e.setLayoutParams(lp);
        parent.addView(e);
        return e;
    }

    private EditText multiField(LinearLayout parent, String hint, String value) {
        EditText e = field(parent, hint, value,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        e.setGravity(Gravity.TOP | Gravity.START);
        e.setMinLines(4);
        e.setSingleLine(false);
        return e;
    }

    private int parseInt(EditText e, int def) {
        try { return Integer.parseInt(e.getText().toString().trim()); }
        catch (Exception ex) { return def; }
    }

    private void save() {
        Config c = new Config();
        c.haUrl = eUrl.getText().toString().trim();
        c.haToken = eToken.getText().toString().trim();
        c.tilesText = eTiles.getText().toString();
        c.columns = Math.max(1, parseInt(eCols, 3));
        c.sshHost = eHost.getText().toString().trim();
        c.sshPort = parseInt(ePort, 22);
        c.sshUser = eUser.getText().toString().trim();
        c.sshPass = ePass.getText().toString();
        c.sshKey = eKey.getText().toString().trim();
        c.rolloUp = eUp.getText().toString();
        c.rolloStop = eStop.getText().toString();
        c.rolloDown = eDown.getText().toString();
        Config.save(this, c);
        Toast.makeText(this, "Gespeichert", Toast.LENGTH_SHORT).show();
        finish();
    }
}

package com.hapanel;

import android.content.Context;
import android.content.SharedPreferences;

/** Konfiguration, persistiert in SharedPreferences. */
public class Config {

    public static final String PREF = "hapanel";

    public String haUrl, haToken, tilesText;
    public String sshHost, sshUser, sshPass, sshKey;
    public String rolloUp, rolloStop, rolloDown;
    public int sshPort, columns;

    public static Config load(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        Config x = new Config();
        x.haUrl    = p.getString("ha_url", "http://192.168.1.10:8123");
        x.haToken  = p.getString("ha_token", "");
        x.tilesText = p.getString("tiles_text",
                "Esstisch|light.esstisch\n" +
                "Stehlampe|light.stehlampe\n" +
                "Esstisch|light.esstisch_2\n" +
                "Knixhult|light.knixhult\n" +
                "Stehlampe|light.stehlampe_2\n" +
                "Knixhult|light.knixhult_2");
        x.sshHost  = p.getString("ssh_host", "192.168.1.20");
        x.sshPort  = p.getInt("ssh_port", 22);
        x.sshUser  = p.getString("ssh_user", "root");
        x.sshPass  = p.getString("ssh_pass", "");
        x.sshKey   = p.getString("ssh_key", "");
        x.rolloUp   = p.getString("rollo_up", "rollo.sh up");
        x.rolloStop = p.getString("rollo_stop", "rollo.sh stop");
        x.rolloDown = p.getString("rollo_down", "rollo.sh down");
        x.columns  = p.getInt("columns", 3);
        return x;
    }

    public static void save(Context c, Config x) {
        SharedPreferences.Editor e = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit();
        e.putString("ha_url", x.haUrl);
        e.putString("ha_token", x.haToken);
        e.putString("tiles_text", x.tilesText);
        e.putString("ssh_host", x.sshHost);
        e.putInt("ssh_port", x.sshPort);
        e.putString("ssh_user", x.sshUser);
        e.putString("ssh_pass", x.sshPass);
        e.putString("ssh_key", x.sshKey);
        e.putString("rollo_up", x.rolloUp);
        e.putString("rollo_stop", x.rolloStop);
        e.putString("rollo_down", x.rolloDown);
        e.putInt("columns", x.columns);
        e.apply();
    }
}

package com.hapanel;

/** Auswaehlbare Kachel-Symbole. Key wird im Tile-Format als 3. Feld gespeichert. */
public class Icons {

    /** {key, Anzeigename} fuer das Dropdown im Web-Interface. */
    public static final String[][] OPTIONS = {
            {"bulb", "Lampe"},
            {"floorlamp", "Stehlampe"},
            {"plug", "Steckdose"},
            {"power", "Power"},
            {"fan", "Ventilator"},
            {"tv", "TV"},
    };

    public static int res(String key) {
        if (key == null) return R.drawable.ic_bulb;
        switch (key) {
            case "floorlamp": return R.drawable.ic_floorlamp;
            case "plug":      return R.drawable.ic_plug;
            case "power":     return R.drawable.ic_power;
            case "fan":       return R.drawable.ic_fan;
            case "tv":        return R.drawable.ic_tv;
            case "bulb":
            default:          return R.drawable.ic_bulb;
        }
    }
}

package com.hapanel;

import android.content.Context;
import android.util.Base64;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Eingebetteter HTTP-Server: liefert eine Konfig-Seite, ueber die sich die
 * App vom Browser aus einrichten laesst. Schreibt dieselben Prefs wie der
 * On-Device-Einstellungen-Screen. Optional Basic-Auth (webPass).
 */
public class WebConfigServer extends NanoHTTPD {

    private final Context ctx;
    public Runnable onSaved;

    public WebConfigServer(Context c, int port) {
        super(port);
        this.ctx = c.getApplicationContext();
    }

    @Override
    public Response serve(IHTTPSession session) {
        Config cfg = Config.load(ctx);

        if (cfg.webPass != null && cfg.webPass.length() > 0) {
            if (!authOk(session, cfg.webPass)) {
                Response r = newFixedLengthResponse(
                        Response.Status.UNAUTHORIZED, "text/plain", "Auth erforderlich");
                r.addHeader("WWW-Authenticate", "Basic realm=\"HA Panel\"");
                return r;
            }
        }

        boolean saved = false;
        if (Method.POST.equals(session.getMethod())) {
            try { session.parseBody(new HashMap<String, String>()); } catch (Exception ignore) {}
            Map<String, List<String>> p = session.getParameters();
            saveFrom(cfg, p);
            if (onSaved != null) { try { onSaved.run(); } catch (Exception ignore) {} }
            cfg = Config.load(ctx);
            saved = true;
        }

        return newFixedLengthResponse(
                Response.Status.OK, "text/html; charset=utf-8", page(cfg, saved));
    }

    private boolean authOk(IHTTPSession session, String pass) {
        try {
            String h = session.getHeaders().get("authorization");
            if (h == null || !h.toLowerCase().startsWith("basic ")) return false;
            String dec = new String(Base64.decode(h.substring(6).trim(), Base64.DEFAULT), "UTF-8");
            int c = dec.indexOf(':');
            String given = c >= 0 ? dec.substring(c + 1) : dec;
            return given.equals(pass);
        } catch (Exception e) {
            return false;
        }
    }

    private String first(Map<String, List<String>> p, String key) {
        List<String> l = p.get(key);
        return (l == null || l.isEmpty() || l.get(0) == null) ? "" : l.get(0);
    }

    private int toInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    /** Baut aus den Zeilen-Feldern (t_name/t_entity) das interne Name|entity-Format. */
    private String buildTiles(Map<String, List<String>> p, String fallback) {
        List<String> names = p.get("t_name");
        List<String> ents = p.get("t_entity");
        if (names == null) {
            // alte Textarea (Abwaertskompatibilitaet) oder unveraendert
            String tt = first(p, "tiles_text");
            return tt.length() > 0 ? tt : fallback;
        }
        StringBuilder tb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            String nm = names.get(i) == null ? "" : names.get(i).trim();
            String en = (ents != null && i < ents.size() && ents.get(i) != null)
                    ? ents.get(i).trim() : "";
            if (nm.length() == 0 && en.length() == 0) continue;
            if (tb.length() > 0) tb.append("\n");
            tb.append(nm).append("|").append(en);
        }
        return tb.toString();
    }

    private void saveFrom(Config old, Map<String, List<String>> p) {
        Config c = new Config();
        c.haUrl = first(p, "ha_url").trim();
        // Leere Secret-Felder = unveraendert lassen
        String tok = first(p, "ha_token");
        c.haToken = tok.length() > 0 ? tok.trim() : old.haToken;
        c.tilesText = buildTiles(p, old.tilesText);
        c.columns = Math.max(1, toInt(first(p, "columns"), 3));
        c.sshHost = first(p, "ssh_host").trim();
        c.sshPort = toInt(first(p, "ssh_port"), 22);
        c.sshUser = first(p, "ssh_user").trim();
        String sp = first(p, "ssh_pass");
        c.sshPass = sp.length() > 0 ? sp : old.sshPass;
        String keyIn = first(p, "ssh_key").trim();
        c.sshKey = keyIn.length() > 0 ? keyIn : old.sshKey;
        c.rolloUp = first(p, "rollo_up");
        c.rolloStop = first(p, "rollo_stop");
        c.rolloDown = first(p, "rollo_down");
        c.webPort = toInt(first(p, "web_port"), 8080);
        String wp = first(p, "web_pass");
        c.webPass = wp.length() > 0 ? wp : old.webPass;
        Config.save(ctx, c);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String page(Config c, boolean saved) {
        StringBuilder b = new StringBuilder();
        b.append("<!DOCTYPE html><html lang=\"de\"><head><meta charset=\"utf-8\">");
        b.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        b.append("<title>HA Panel - Konfiguration</title><style>");
        b.append("body{background:#0d0d0d;color:#ececec;font-family:system-ui,Arial,sans-serif;margin:0;padding:24px;}");
        b.append(".wrap{max-width:720px;margin:0 auto;}");
        b.append("h1{font-size:20px;} h2{font-size:15px;color:#6fa8dc;margin-top:24px;border-bottom:1px solid #333;padding-bottom:4px;}");
        b.append("label{display:block;margin:12px 0 4px;font-size:13px;color:#bbb;}");
        b.append("input,textarea{width:100%;box-sizing:border-box;background:#1e1e1e;border:1px solid #333;color:#ececec;");
        b.append("border-radius:8px;padding:10px;font-size:14px;font-family:inherit;}");
        b.append("textarea{min-height:120px;}");
        b.append("button{margin-top:20px;background:#3b6ea5;color:#fff;border:0;border-radius:8px;padding:12px 20px;font-size:15px;cursor:pointer;}");
        b.append(".ok{background:#244d2a;border:1px solid #3a7d44;padding:10px;border-radius:8px;margin-bottom:16px;}");
        b.append(".hint{color:#888;font-size:12px;}");
        b.append(".tile-row{display:flex;gap:8px;margin-top:8px;align-items:center;}");
        b.append(".tile-row input{flex:1;margin:0;}");
        b.append(".del{background:#5a2a2a;color:#fff;border:0;border-radius:8px;padding:10px 14px;margin:0;cursor:pointer;}");
        b.append(".addbtn{margin-top:10px;background:#2d4a2d;color:#fff;border:0;border-radius:8px;padding:8px 14px;cursor:pointer;}");
        b.append("</style></head><body><div class=\"wrap\">");
        b.append("<h1>HA Panel &ndash; Konfiguration</h1>");
        if (saved) b.append("<div class=\"ok\">Gespeichert. Das Dashboard aktualisiert sich automatisch.</div>");
        b.append("<form method=\"post\" action=\"/\">");

        b.append("<h2>Home Assistant</h2>");
        b.append("<label>HA-URL</label><input name=\"ha_url\" value=\"").append(esc(c.haUrl)).append("\" placeholder=\"http://192.168.1.10:8123\">");
        b.append("<label>Long-Lived Token <span class=\"hint\">(leer lassen = unver&auml;ndert)</span></label>");
        b.append("<input name=\"ha_token\" type=\"password\" value=\"\" placeholder=\"").append(c.haToken.length() > 0 ? "&bull;&bull;&bull; gesetzt" : "nicht gesetzt").append("\">");

        b.append("<h2>Kacheln <span class=\"hint\">(Name und entity_id je Kachel)</span></h2>");
        b.append("<div id=\"tiles\">");
        for (String line : c.tilesText.split("\\n")) {
            String s = line.trim();
            if (s.length() == 0) continue;
            int bar = s.indexOf('|');
            String nm = bar >= 0 ? s.substring(0, bar).trim() : s;
            String en = bar >= 0 ? s.substring(bar + 1).trim() : "";
            b.append("<div class=\"tile-row\">");
            b.append("<input name=\"t_name\" placeholder=\"Name\" value=\"").append(esc(nm)).append("\">");
            b.append("<input name=\"t_entity\" placeholder=\"entity_id\" value=\"").append(esc(en)).append("\">");
            b.append("<button type=\"button\" class=\"del\" onclick=\"this.parentNode.remove()\">✕</button>");
            b.append("</div>");
        }
        b.append("</div>");
        b.append("<button type=\"button\" class=\"addbtn\" onclick=\"addTile()\">+ Kachel</button>");
        b.append("<label>Spalten</label><input name=\"columns\" value=\"").append(c.columns).append("\">");

        b.append("<h2>Rollladen-Server (SSH)</h2>");
        b.append("<label>Host/IP</label><input name=\"ssh_host\" value=\"").append(esc(c.sshHost)).append("\">");
        b.append("<label>Port</label><input name=\"ssh_port\" value=\"").append(c.sshPort).append("\">");
        b.append("<label>User</label><input name=\"ssh_user\" value=\"").append(esc(c.sshUser)).append("\">");
        b.append("<label>Passwort <span class=\"hint\">(leer lassen = unver&auml;ndert)</span></label>");
        b.append("<input name=\"ssh_pass\" type=\"password\" value=\"\" placeholder=\"").append(c.sshPass.length() > 0 ? "&bull;&bull;&bull; gesetzt" : "nicht gesetzt").append("\">");
        b.append("<label>Privater Key <span class=\"hint\">(leer lassen = unver&auml;ndert; kompletten Inhalt -----BEGIN... einf&uuml;gen oder Pfad)</span></label>");
        b.append("<textarea name=\"ssh_key\" placeholder=\"").append(c.sshKey.length() > 0 ? "&bull;&bull;&bull; gesetzt" : "nicht gesetzt").append("\"></textarea>");

        b.append("<h2>Rollo-Kommandos</h2>");
        b.append("<label>Hoch</label><input name=\"rollo_up\" value=\"").append(esc(c.rolloUp)).append("\">");
        b.append("<label>Stop</label><input name=\"rollo_stop\" value=\"").append(esc(c.rolloStop)).append("\">");
        b.append("<label>Runter</label><input name=\"rollo_down\" value=\"").append(esc(c.rolloDown)).append("\">");

        b.append("<h2>Web-Interface</h2>");
        b.append("<label>Port <span class=\"hint\">(wirkt nach Neustart der App)</span></label><input name=\"web_port\" value=\"").append(c.webPort).append("\">");
        b.append("<label>Web-Passwort <span class=\"hint\">(leer lassen = unver&auml;ndert; setzen sch&uuml;tzt diese Seite per Basic-Auth)</span></label>");
        b.append("<input name=\"web_pass\" type=\"password\" value=\"\" placeholder=\"").append(c.webPass.length() > 0 ? "&bull;&bull;&bull; gesetzt" : "offen (kein Schutz)").append("\">");

        b.append("<br><button type=\"submit\">Speichern</button>");
        b.append("</form>");
        b.append("<script>function addTile(){var d=document.getElementById('tiles');");
        b.append("var r=document.createElement('div');r.className='tile-row';");
        b.append("r.innerHTML=\"<input name='t_name' placeholder='Name'>");
        b.append("<input name='t_entity' placeholder='entity_id'>");
        b.append("<button type='button' class='del' onclick='this.parentNode.remove()'>✕</button>\";");
        b.append("d.appendChild(r);}</script>");
        b.append("</div></body></html>");
        return b.toString();
    }
}

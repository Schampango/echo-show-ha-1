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

        String uri = session.getUri();
        if (uri != null && uri.startsWith("/log")) {
            if (Method.POST.equals(session.getMethod())) {
                try { session.parseBody(new HashMap<String, String>()); } catch (Exception ignore) {}
                Logg.clear();
            }
            return newFixedLengthResponse(
                    Response.Status.OK, "text/html; charset=utf-8", logPage());
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
        List<String> icons = p.get("t_icon");
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
            String ic = (icons != null && i < icons.size() && icons.get(i) != null)
                    ? icons.get(i).trim() : "bulb";
            if (ic.length() == 0) ic = "bulb";
            if (nm.length() == 0 && en.length() == 0) continue;
            if (tb.length() > 0) tb.append("\n");
            tb.append(nm).append("|").append(en).append("|").append(ic);
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
        boolean clearPass = p.get("clear_pass") != null;
        c.sshPass = clearPass ? "" : (sp.length() > 0 ? sp : old.sshPass);
        String keyIn = first(p, "ssh_key").trim();
        boolean clearKey = p.get("clear_key") != null;
        c.sshKey = clearKey ? "" : (keyIn.length() > 0 ? keyIn : old.sshKey);
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

    /** Inline-SVG fuer ein Icon (nur einfache Anfuehrungszeichen -> JS-sicher). */
    private String iconSvg(String k) {
        String open = "<svg viewBox='0 0 24 24' width='24' height='24'>";
        String close = "</svg>";
        if ("power".equals(k)) {
            return open
                    + "<path d='M12,3 L12,11' stroke='currentColor' stroke-width='2.2' fill='none' stroke-linecap='round'/>"
                    + "<path d='M7.5,6.5 A7,7 0 1 0 16.5,6.5' stroke='currentColor' stroke-width='2.2' fill='none' stroke-linecap='round'/>"
                    + close;
        }
        String d;
        switch (k) {
            case "floorlamp":
                d = "M9,3 L15,3 L17.5,9 L6.5,9 Z M11,9 L13,9 L13,19 L11,19 Z M7.5,19 L16.5,19 L16.5,21 L7.5,21 Z";
                break;
            case "plug":
                d = "M8,2 L10,2 L10,7 L8,7 Z M14,2 L16,2 L16,7 L14,7 Z M6,7 L18,7 L18,12 A6,6 0 0 1 6,12 Z M11,18 L13,18 L13,22 L11,22 Z";
                break;
            case "fan":
                d = "M12,12 L12,3 L15,7 Z M12,12 L21,12 L17,15 Z M12,12 L12,21 L9,17 Z M12,12 L3,12 L7,9 Z M10,12 A2,2 0 1 1 14,12 A2,2 0 1 1 10,12 Z";
                break;
            case "tv":
                d = "M4,5 L20,5 A1.5,1.5 0 0 1 21.5,6.5 L21.5,16 A1.5,1.5 0 0 1 20,17.5 L4,17.5 A1.5,1.5 0 0 1 2.5,16 L2.5,6.5 A1.5,1.5 0 0 1 4,5 Z M8,20 L16,20 L16,21.5 L8,21.5 Z";
                break;
            case "bulb":
            default:
                d = "M9,2 L15,2 A1,1 0 0 1 16,3 L16,4 A2,2 0 0 1 18,6 L18,19 A3,3 0 0 1 15,22 L9,22 A3,3 0 0 1 6,19 L6,6 A2,2 0 0 1 8,4 L8,3 A1,1 0 0 1 9,2 Z";
                break;
        }
        return open + "<path d='" + d + "' fill='currentColor'/>" + close;
    }

    /** Icon-Auswahl: anklickbare Symbole + verstecktes t_icon-Feld. */
    private String iconPicker(String selected) {
        StringBuilder o = new StringBuilder();
        o.append("<div class='iconpick'>");
        o.append("<input type='hidden' name='t_icon' value='").append(selected).append("'>");
        for (String[] opt : Icons.OPTIONS) {
            String k = opt[0];
            o.append("<span class='ico").append(k.equals(selected) ? " sel" : "")
                    .append("' data-k='").append(k).append("' title='").append(esc(opt[1]))
                    .append("' onclick='pick(this)'>").append(iconSvg(k)).append("</span>");
        }
        o.append("</div>");
        return o.toString();
    }

    private String logPage() {
        StringBuilder b = new StringBuilder();
        b.append("<!DOCTYPE html><html lang=\"de\"><head><meta charset=\"utf-8\">");
        b.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        b.append("<meta http-equiv=\"refresh\" content=\"3\">");
        b.append("<title>HA Panel - Log</title><style>");
        b.append("body{background:#0d0d0d;color:#ececec;font-family:system-ui,Arial,sans-serif;margin:0;padding:24px;}");
        b.append(".wrap{max-width:900px;margin:0 auto;}");
        b.append("a{color:#6fa8dc;} h1{font-size:20px;}");
        b.append("pre{background:#161616;border:1px solid #333;border-radius:8px;padding:12px;");
        b.append("white-space:pre-wrap;word-break:break-word;font-size:13px;line-height:1.5;max-height:70vh;overflow:auto;}");
        b.append("button{background:#5a2a2a;color:#fff;border:0;border-radius:8px;padding:8px 14px;cursor:pointer;}");
        b.append("</style></head><body><div class=\"wrap\">");
        b.append("<h1>Log <span style='font-size:13px;color:#888;'>(aktualisiert alle 3s)</span></h1>");
        b.append("<p><a href=\"/\">&larr; Zur&uuml;ck zur Konfiguration</a></p>");
        String d = Logg.dump();
        b.append("<pre>").append(esc(d.length() == 0 ? "(noch keine Eintraege)" : d)).append("</pre>");
        b.append("<form method=\"post\" action=\"/log\"><button type=\"submit\">Log leeren</button></form>");
        b.append("</div></body></html>");
        return b.toString();
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
        b.append("label.chk{display:flex;align-items:center;gap:8px;color:#bbb;font-size:13px;margin-top:8px;}");
        b.append("label.chk input{width:auto;}");
        b.append(".tile-row{margin-top:12px;border:1px solid #2a2a2a;border-radius:10px;padding:8px;}");
        b.append(".tr-top{display:flex;gap:8px;align-items:center;}");
        b.append(".tr-top input{flex:1;margin:0;}");
        b.append(".iconpick{display:flex;gap:6px;margin-top:8px;flex-wrap:wrap;}");
        b.append(".ico{width:38px;height:38px;display:flex;align-items:center;justify-content:center;border:2px solid #333;border-radius:8px;color:#9fb6cc;cursor:pointer;}");
        b.append(".ico.sel{border-color:#f5c518;color:#f5c518;background:#241f08;}");
        b.append(".del{background:#5a2a2a;color:#fff;border:0;border-radius:8px;padding:10px 14px;margin:0;cursor:pointer;}");
        b.append(".addbtn{margin-top:10px;background:#2d4a2d;color:#fff;border:0;border-radius:8px;padding:8px 14px;cursor:pointer;}");
        b.append("</style></head><body><div class=\"wrap\">");
        b.append("<h1>HA Panel &ndash; Konfiguration</h1>");
        b.append("<p><a href=\"/log\" style=\"color:#6fa8dc;\">&#9432; Log ansehen (SSH-/HA-Fehler)</a></p>");
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
            String[] parts = s.split("\\|", 3);
            String nm = parts.length > 0 ? parts[0].trim() : "";
            String en = parts.length > 1 ? parts[1].trim() : "";
            String ic = parts.length > 2 ? parts[2].trim() : "bulb";
            if (ic.length() == 0) ic = "bulb";
            b.append("<div class=\"tile-row\">");
            b.append("<div class=\"tr-top\">");
            b.append("<input name=\"t_name\" placeholder=\"Name\" value=\"").append(esc(nm)).append("\">");
            b.append("<input name=\"t_entity\" placeholder=\"entity_id\" value=\"").append(esc(en)).append("\">");
            b.append("<button type=\"button\" class=\"del\" onclick=\"this.closest('.tile-row').remove()\">✕</button>");
            b.append("</div>");
            b.append(iconPicker(ic));
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
        b.append("<label class=\"chk\"><input type=\"checkbox\" name=\"clear_pass\" value=\"1\"> Passwort entfernen</label>");
        b.append("<label>Privater Key <span class=\"hint\">(leer lassen = unver&auml;ndert; kompletten Inhalt -----BEGIN... einf&uuml;gen oder Pfad)</span></label>");
        b.append("<textarea name=\"ssh_key\" placeholder=\"").append(c.sshKey.length() > 0 ? "&bull;&bull;&bull; gesetzt" : "nicht gesetzt").append("\"></textarea>");
        b.append("<label class=\"chk\"><input type=\"checkbox\" name=\"clear_key\" value=\"1\"> Key entfernen (dann nur Passwort-Auth)</label>");

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
        b.append("<script>");
        b.append("function pick(el){var p=el.closest('.iconpick');");
        b.append("p.querySelector(\"input[name='t_icon']\").value=el.getAttribute('data-k');");
        b.append("var s=p.querySelectorAll('.ico');for(var i=0;i<s.length;i++){s[i].classList.remove('sel');}");
        b.append("el.classList.add('sel');}");
        b.append("function addTile(){var d=document.getElementById('tiles');");
        b.append("var r=document.createElement('div');r.className='tile-row';");
        b.append("r.innerHTML=\"<div class='tr-top'><input name='t_name' placeholder='Name'>");
        b.append("<input name='t_entity' placeholder='entity_id'>");
        b.append("<button type='button' class='del'>✕</button></div>");
        b.append(iconPicker("bulb"));
        b.append("\";");
        b.append("r.querySelector('.del').onclick=function(){r.remove();};");
        b.append("d.appendChild(r);}");
        b.append("</script>");
        b.append("</div></body></html>");
        return b.toString();
    }
}

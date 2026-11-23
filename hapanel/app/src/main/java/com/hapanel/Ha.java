package com.hapanel;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Minimaler Home-Assistant-REST-Client (nur HttpURLConnection, keine Libs). */
public class Ha {

    /** Schaltet eine Entity um (an/aus). true bei HTTP 2xx. */
    public static boolean toggle(Config c, String entity) {
        HttpURLConnection h = null;
        try {
            h = open(c, "/api/services/homeassistant/toggle", "POST");
            h.setDoOutput(true);
            byte[] body = ("{\"entity_id\":\"" + entity + "\"}").getBytes("UTF-8");
            OutputStream os = h.getOutputStream();
            os.write(body);
            os.close();
            int code = h.getResponseCode();
            drain(h);
            return code >= 200 && code < 300;
        } catch (Exception e) {
            return false;
        } finally {
            if (h != null) h.disconnect();
        }
    }

    /** Liefert den State-String ("on"/"off"/...) oder null bei Fehler. */
    public static String state(Config c, String entity) {
        HttpURLConnection h = null;
        try {
            h = open(c, "/api/states/" + entity, "GET");
            int code = h.getResponseCode();
            if (code < 200 || code >= 300) { drain(h); return null; }
            String s = read(h.getInputStream());
            JSONObject o = new JSONObject(s);
            return o.optString("state", "");
        } catch (Exception e) {
            return null;
        } finally {
            if (h != null) h.disconnect();
        }
    }

    private static HttpURLConnection open(Config c, String path, String method) throws IOException {
        String base = c.haUrl.trim().replaceAll("/+$", "");
        URL u = new URL(base + path);
        HttpURLConnection h = (HttpURLConnection) u.openConnection();
        h.setRequestMethod(method);
        h.setConnectTimeout(5000);
        h.setReadTimeout(5000);
        h.setRequestProperty("Authorization", "Bearer " + c.haToken);
        h.setRequestProperty("Content-Type", "application/json");
        return h;
    }

    private static String read(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        in.close();
        return bo.toString("UTF-8");
    }

    private static void drain(HttpURLConnection h) {
        try {
            InputStream e = h.getErrorStream();
            if (e != null) {
                byte[] buf = new byte[1024];
                while (e.read(buf) >= 0) { /* consume */ }
                e.close();
            }
        } catch (Exception ignore) {
        }
    }
}

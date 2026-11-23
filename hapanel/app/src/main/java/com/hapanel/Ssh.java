package com.hapanel;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Properties;

/** Fuehrt ein Kommando per SSH auf dem Rollladen-Server aus (JSch). */
public class Ssh {

    /** Gibt die (kombinierte) Ausgabe zurueck; wirft bei Verbindungsfehler. */
    public static String run(Config c, String command) throws Exception {
        JSch jsch = new JSch();
        String key = c.sshKey == null ? "" : c.sshKey.trim();
        boolean usingKey = key.length() > 0;
        if (usingKey) {
            // SSH-Passwort dient bei Key-Auth als (optionale) Passphrase
            byte[] passphrase = (c.sshPass != null && c.sshPass.length() > 0)
                    ? c.sshPass.getBytes("UTF-8") : null;
            if (key.startsWith("-----BEGIN")) {
                // eingefuegter Key-Inhalt
                jsch.addIdentity("rollo", key.getBytes("UTF-8"), null, passphrase);
            } else {
                // Pfad zu einer Key-Datei
                jsch.addIdentity(key, passphrase);
            }
        }
        Session s = jsch.getSession(c.sshUser, c.sshHost, c.sshPort);
        if (!usingKey && c.sshPass != null && c.sshPass.length() > 0) {
            s.setPassword(c.sshPass);
        }
        Properties cfg = new Properties();
        cfg.put("StrictHostKeyChecking", "no");
        s.setConfig(cfg);
        s.connect(8000);

        Logg.add("SSH verbunden: " + c.sshUser + "@" + c.sshHost + ":" + c.sshPort);

        ChannelExec ch = (ChannelExec) s.openChannel("exec");
        ch.setCommand(command);
        ch.setInputStream(null);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        ch.setErrStream(err);
        InputStream in = ch.getInputStream();
        ch.connect();

        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[1024];
        try {
            while (true) {
                while (in.available() > 0) {
                    int n = in.read(buf, 0, buf.length);
                    if (n < 0) break;
                    sb.append(new String(buf, 0, n, "UTF-8"));
                }
                if (ch.isClosed()) {
                    if (in.available() > 0) continue;
                    break;
                }
                Thread.sleep(100);
            }
        } finally {
            int exit = ch.getExitStatus();
            String errS = err.toString("UTF-8").trim();
            Logg.add("SSH exit=" + exit + (errS.length() > 0 ? "  stderr=" + errS : ""));
            ch.disconnect();
            s.disconnect();
            if (exit > 0) {
                throw new Exception("Kommando-Exit " + exit
                        + (errS.length() > 0 ? ": " + errS : ""));
            }
        }
        return sb.toString();
    }
}

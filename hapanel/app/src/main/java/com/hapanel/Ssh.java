package com.hapanel;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;
import java.util.Properties;

/** Fuehrt ein Kommando per SSH auf dem Rollladen-Server aus (JSch). */
public class Ssh {

    /** Gibt die (kombinierte) Ausgabe zurueck; wirft bei Verbindungsfehler. */
    public static String run(Config c, String command) throws Exception {
        JSch jsch = new JSch();
        if (c.sshKey != null && c.sshKey.trim().length() > 0) {
            jsch.addIdentity(c.sshKey.trim());
        }
        Session s = jsch.getSession(c.sshUser, c.sshHost, c.sshPort);
        if (c.sshPass != null && c.sshPass.length() > 0) {
            s.setPassword(c.sshPass);
        }
        Properties cfg = new Properties();
        cfg.put("StrictHostKeyChecking", "no");
        s.setConfig(cfg);
        s.connect(8000);

        ChannelExec ch = (ChannelExec) s.openChannel("exec");
        ch.setCommand(command);
        ch.setInputStream(null);
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
            ch.disconnect();
            s.disconnect();
        }
        return sb.toString();
    }
}

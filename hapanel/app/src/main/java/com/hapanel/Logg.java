package com.hapanel;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.Locale;

/** Einfacher Ring-Puffer-Logger, im Web-Interface unter /log sichtbar. */
public class Logg {

    private static final int MAX = 400;
    private static final Deque<String> LINES = new ArrayDeque<String>();
    private static final SimpleDateFormat TS =
            new SimpleDateFormat("HH:mm:ss", Locale.GERMANY);

    public static synchronized void add(String msg) {
        LINES.addLast(TS.format(new Date()) + "  " + msg);
        while (LINES.size() > MAX) LINES.removeFirst();
    }

    public static synchronized String dump() {
        StringBuilder b = new StringBuilder();
        for (String l : LINES) b.append(l).append("\n");
        return b.toString();
    }

    public static synchronized void clear() {
        LINES.clear();
    }
}

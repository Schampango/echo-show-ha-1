# HA Panel – Home-Assistant-Dashboard für den Echo Show 5

Native Android-App (abhängigkeitsarm, nur JSch für SSH): dunkles Kachel-Grid
zum Schalten von Home-Assistant-Entities plus eine Rollo-Spalte
(hoch / stop / runter), die ein Shellscript per SSH auf einem anderen Server
auslöst. Icon **gelb = an**, **blau = aus**. Alles über den
„Einstellungen"-Button im Gerät konfigurierbar – keine Secrets im Code.

Package `com.hapanel` · minSdk 21 · targetSdk 30 (läuft auf LineageOS 18.1).

## Bauen

```bash
cd ~/echo-show-ha-1/hapanel
export ANDROID_HOME=/opt/android-sdk
echo "sdk.dir=/opt/android-sdk" > local.properties
./gradlew assembleDebug
```

Erster Build lädt die JSch-Lib aus Maven Central – Internet nötig.
Ergebnis: `app/build/outputs/apk/debug/app-debug.apk`

## Installieren (Show 5 über WLAN)

```bash
adb connect 192.168.1.48:5555
adb -s 192.168.1.48:5555 install -r app/build/outputs/apk/debug/app-debug.apk
```

## Einrichten (im „Einstellungen"-Screen)

- **HA-URL**: z.B. `http://192.168.1.10:8123`
- **Token**: Home Assistant → Profil → *Long-Lived Access Tokens* → erstellen
- **Kacheln**: je Zeile `Anzeigename|entity_id`, z.B. `Stehlampe|light.stehlampe`.
  Entity-IDs findest du in HA unter *Entwicklerwerkzeuge → Zustände*.
- **Spalten**: Anzahl der Grid-Spalten (Screenshot = 3)
- **SSH**: Host/IP, Port, User und entweder **Passwort** **oder** einen
  **privaten Key**. Den Key am einfachsten als kompletten Inhalt
  (`-----BEGIN ... KEY-----` … `-----END ... KEY-----`) ins Key-Feld einfügen
  (empfohlen, keine Storage-Rechte nötig). Alternativ ein Dateipfad. Ist der Key
  mit Passphrase geschützt, kommt die Passphrase ins Passwort-Feld.
- **Rollo-Kommandos**: die Befehle, die per SSH ausgeführt werden, z.B.
  `/opt/rollo/rollo.sh up` / `... stop` / `... down`

## Konfiguration per Browser (Web-Interface)

Die App startet einen kleinen eingebauten Webserver. Beim App-Start zeigt ein
Toast die Adresse, standardmäßig:

```
http://<Show-IP>:8080
```

Dort öffnet sich dieselbe Konfigurationsseite (HA, Kacheln, SSH, Rollo) im
Browser eines beliebigen Geräts im Netz. „Speichern" schreibt die Einstellungen,
und das Dashboard auf dem Show aktualisiert sich automatisch.

- **Secrets** (HA-Token, SSH-Passwort): Felder bleiben im Browser leer —
  leer lassen = unverändert. So werden sie nicht an den Browser zurückgegeben.
- **Schutz**: Standardmäßig ist die Seite im LAN **ungeschützt**. Setze unter
  *Web-Konfiguration → Web-Passwort* ein Passwort, dann ist die Seite per
  HTTP-Basic-Auth geschützt. Ein geänderter Port greift nach App-Neustart.

⚠️ Das läuft über **unverschlüsseltes HTTP** im lokalen Netz — ok fürs Heimnetz,
aber nicht ins Internet exponieren.

## Funktionsweise

- Schalten: `POST /api/services/homeassistant/toggle` mit `entity_id`
- Status (Icon-Farbe): `GET /api/states/<entity_id>`, Poll alle 8 s
- Rollo: SSH-Exec des hinterlegten Kommandos (StrictHostKeyChecking aus)

## Als Standard-Launcher (optional, Kiosk)

Damit der Show direkt in dieses Panel bootet, kann HA Panel als Home-App
gesetzt werden (LineageOS: *Einstellungen → Apps → Standard-Apps → Startseite*),
oder per `cmd package set-home-activity com.hapanel/.MainActivity` via adb.

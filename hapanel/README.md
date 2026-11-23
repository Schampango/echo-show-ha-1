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
- **SSH**: Host/IP, Port, User und Passwort **oder** Pfad zu einem privaten Key
  (der Key muss auf dem Show liegen, z.B. `/sdcard/rollo_key`)
- **Rollo-Kommandos**: die Befehle, die per SSH ausgeführt werden, z.B.
  `/opt/rollo/rollo.sh up` / `... stop` / `... down`

## Funktionsweise

- Schalten: `POST /api/services/homeassistant/toggle` mit `entity_id`
- Status (Icon-Farbe): `GET /api/states/<entity_id>`, Poll alle 8 s
- Rollo: SSH-Exec des hinterlegten Kommandos (StrictHostKeyChecking aus)

## Als Standard-Launcher (optional, Kiosk)

Damit der Show direkt in dieses Panel bootet, kann HA Panel als Home-App
gesetzt werden (LineageOS: *Einstellungen → Apps → Standard-Apps → Startseite*),
oder per `cmd package set-home-activity com.hapanel/.MainActivity` via adb.

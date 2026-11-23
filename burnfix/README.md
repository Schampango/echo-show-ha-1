# BurnFix – LCD Subpixel-/Retention-Refresher (Android)

Kleine, abhängigkeitsfreie Android-App: Vollbild, maximale Helligkeit, Display
bleibt an, und es läuft endlos eine Animation aus Vollflächen-Farben
(Weiß/Schwarz/RGB/…) plus bewegtem RGB-Muster. Genau das, was Image Retention
auf IPS-LCDs (wie beim Echo Show 5) meist auflöst.

- **Tippen** = Tempo umschalten (2 s → 1 s → 0,4 s → 0,12 s pro Farbe)
- **Zurück-Taste** = beenden
- Für „über Nacht": mittleres Tempo wählen, laufen lassen.

Package: `com.burnfix.app` · minSdk 21 · targetSdk 30 (installiert sauber auf
LineageOS 18.1 / Android 11).

---

## Bauen (auf deinem T470)

Voraussetzung: ein JDK (17+) und das **Android SDK**. Wenn du Android Studio
hast, ist das SDK schon dabei. Ohne Studio reichen die *command-line tools*:

```bash
# SDK-Pfad bekannt machen (eine der beiden Varianten):
export ANDROID_HOME=$HOME/Android/Sdk          # oder wo dein SDK liegt
# ... oder eine local.properties anlegen:
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# benötigte SDK-Teile (einmalig), falls noch nicht vorhanden:
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Debug-APK bauen:
./gradlew assembleDebug
```

Ergebnis:

```
app/build/outputs/apk/debug/app-debug.apk
```

Die Debug-APK ist bereits mit dem Debug-Key signiert (v1+v2) und direkt
installierbar – kein extra Signieren nötig.

## Installieren

Echo im laufenden LineageOS, ADB-Debugging an (Einstellungen → Entwickler­optionen),
dann:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

App „BurnFix" starten, Tempo per Tap wählen, laufen lassen. Danach prüfen, ob die
Artefakte schwächer werden – wenn ja, wiederholen, bis sie weg sind.

## Alternative ohne Gradle (apktool-Route)

Falls du kein volles SDK installieren willst, lässt sich die App auch aus einem
apktool-Projekt bauen (apktool bringt aapt2 + smali mit) und mit
`uber-apk-signer` signieren. Sag Bescheid, dann lege ich dir diese Variante
zusätzlich bei.

## Release-APK (optional)

`./gradlew assembleRelease` erzeugt eine unsignierte Release-APK. Zum Installieren
musst du sie selbst signieren (eigener Keystore + `apksigner`), oder einfach die
Debug-APK nehmen – für diesen Zweck völlig ausreichend.

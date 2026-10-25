# TCL TV 0.2.0

Eigenständiger Android-TV-/Google-TV-Launcher für TCL-Fernseher, Paket
`de.tcltv.launcher`. TCL TV behält sein eigenes dunkles, tvOS-inspiriertes Design
und die Seiten Home, Filme, Serien, Apps, Live TV und Hintergrund.

## Bedienung mit der Fernbedienung

- LEFT/RIGHT wechselt innerhalb einer Reihe; am Reihenende bleibt der Fokus dort.
- Außerhalb des sichtbaren Bereichs liegende Karten werden automatisch eingeblendet.
- UP/DOWN wechselt zwischen Reihen und stellt die zuletzt benutzte Karte wieder her.
- OK öffnet eine Karte oder aktiviert einen Tab. BACK führt zur Home-Navigation zurück.
- Langdruck auf eine App unter Apps verwaltet Favoriten. Langdruck auf ein
  HDMI-Gerät öffnet dessen Namen- und Markenzuordnung.

Home enthält genau drei Reihen: Suche + HDMI-Geräte, Favoriten-Apps und
Weiterschauen. Filme, Serien, Live-TV-Programme und Weiterschauen bleiben Demodaten;
TCL TV liest keine privaten Wiedergabefortschritte von Streaming-Anbietern aus.

## Technische Änderungen in 0.2

- Topbar mit Platz für die weiße aktive Pill-Fläche, Fokusanimation und TV-Safe-Area.
- Gemeinsame D-Pad-Steuerung in `TvFocus`; keine Umsortierung der Karten beim Fokuswechsel.
- Bewusst beibehaltene Scroll-Reihen mit expliziten Fokuswegen und Sichtbarscrollen.
  Die zuvor beobachtete Reihenfolgeänderung durch `bringToFront()` entfällt.
- App-Erkennung über Leanback, normale Launcher, sichtbare installierte Pakete und
  bekannte Streaming-Pakete; Manifest-Abfragen, Dublettenfilter und Eigenfilter.
- App-Start bevorzugt Leanback und versucht bei Bedarf den normalen Launcher.
- HDMI-Deskriptoren werden zwischengespeichert und getrennt vom Verbindungszustand
  behandelt. Strukturereignisse werden gebündelt; reine Zustandsmeldungen bauen
  keine Karten neu auf. Vorhandene Karten behalten ihre Input-ID und ihren Fokus.
- CEC-Kinder ersetzen ihren physischen Eltern-Eingang in der Anzeige, um doppelte
  Karten zu verhindern. Verfügbare Labels und Hersteller-Metadaten liefern Hinweise;
  manuelle Namen und Marken bleiben möglich. Es werden keine privilegierten
  CEC-APIs benötigt.
- Geräte-Logos sind Vektoren. HDMI-Portnummern und Input-IDs erscheinen nicht auf
  den Karten. Ein Modellname wird nur bei passenden Modellhinweisen verwendet.

Der technische Abgleich und die übertragenen Lösungen aus TCL Home sind in
[docs/TCL-HOME-REFERENZ.md](docs/TCL-HOME-REFERENZ.md) dokumentiert.

## Bauen und prüfen

Voraussetzungen: JDK 17, Gradle 8.13, Android SDK 36 und Build Tools 36.0.0.
`minSdk` bleibt 26, `targetSdk` und `compileSdk` bleiben 36.

```sh
gradle :app:assembleDebug
gradle :app:testDebugUnitTest :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
Die bestehende GitHub Action baut bei Push auf `main`, bei Pull Requests und
manuell; sie stellt das Artefakt `TCL-TV-0.2-debug-apk` bereit.

Die Robolectric-Tests prüfen Fernbedienungswege, horizontales Scrollen,
Fokuserhalt bei HDMI-Änderungen, App-Erkennung und App-Start. Die Abnahme auf
physischer TCL-Hardware ist zusätzlich anhand von
[docs/TV-TESTPLAN.md](docs/TV-TESTPLAN.md) durchzuführen.

[Geräte-Logo-Quellen](THIRD_PARTY_NOTICES.md)

# Technischer Abgleich mit TCL Home

Referenz: [Trunda1997/-C9KHome](https://github.com/Trunda1997/-C9KHome),
Stand [`2caf77e9294273c06fd625983cb29ec1275b9acd`](https://github.com/Trunda1997/-C9KHome/tree/2caf77e9294273c06fd625983cb29ec1275b9acd).
Der Abgleich betrifft technische Lösungen, nicht die Gestaltung von TCL Home.

## Übertragen

- [`FocusNavigator.java`](https://github.com/Trunda1997/-C9KHome/blob/2caf77e9294273c06fd625983cb29ec1275b9acd/app/src/main/java/de/tcl/c9khome/FocusNavigator.java):
  explizite Fokusnachbarn, begrenzte Reihenenden und konsumierte Richtungstasten.
  `reveal()` wartet mit `post()` auf das Layout und prüft `hasFocus()`, bevor eine
  Karte eingeblendet wird. TCL TV verwendet dieses Prinzip in `TvFocus`, ergänzt
  um Platz für die Fokusvergrößerung und den zuletzt gewählten Fokus je Reihe.
- [`TvUi.java`](https://github.com/Trunda1997/-C9KHome/blob/2caf77e9294273c06fd625983cb29ec1275b9acd/app/src/main/java/de/tcl/c9khome/TvUi.java):
  Karten bleiben auch nach einem Eingabemoduswechsel fokussierbar
  (`setFocusableInTouchMode(true)`). Farben, Kartenflächen und Fokusanimationen
  bleiben die von TCL TV.
- [`LaunchableApps.java`](https://github.com/Trunda1997/-C9KHome/blob/2caf77e9294273c06fd625983cb29ec1275b9acd/app/src/main/java/de/tcl/c9khome/LaunchableApps.java):
  beide Launcher-Kategorien, eine Karte pro Paket, Eigenfilter und Prüfung der
  tatsächlich startbaren Activity. Übernommen sind die Prüfungen von Paket,
  Exportstatus, Aktivierung und benötigter Berechtigung. Auch nach einem Fehler
  beim Leanback-Start wird der normale Launcher separat versucht. TCL TV ergänzt
  bekannte Pakete und sichtbare installierte Apps.

## Geprüft und gezielt weiterentwickelt

[`HdmiRepository.java`](https://github.com/Trunda1997/-C9KHome/blob/2caf77e9294273c06fd625983cb29ec1275b9acd/app/src/main/java/de/tcl/c9khome/HdmiRepository.java)
und [`HdmiSection.java`](https://github.com/Trunda1997/-C9KHome/blob/2caf77e9294273c06fd625983cb29ec1275b9acd/app/src/main/java/de/tcl/c9khome/HdmiSection.java)
verwenden TvInputManager, Custom Label/Label/Input-ID, Marken-Overrides und
Passthrough-Öffnung. Diese Grundlage bleibt erhalten.

Die Referenz reagiert weiterhin auf jeden State-Callback mit einer Benachrichtigung
und ersetzt in `populate()` sämtliche Karten. Sie enthält an diesem Stand daher
keine robustere Lösung für den gemeldeten State-Refresh-/Fokusfehler. TCL TV 0.2
trennt Zustände von Deskriptoren, bündelt Strukturereignisse und aktualisiert
bestehende Karten anhand ihrer Input-ID. HDMI-Eltern mit CEC-Kindern werden nicht
doppelt dargestellt.

Die eigene Topbar von TCL TV erhält zusätzliche Höhe, flexible Tabbreiten und
ungeclippte Eltern. TCL-Home-Seitenstruktur, Branding, Versionen, Einstellungen
und weitere Medienfunktionen werden nicht übernommen.

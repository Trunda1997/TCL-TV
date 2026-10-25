# TCL TV 0.1

Eigenständiger Android-TV-Launcher für TCL TV. Dieses Projekt ist strikt getrennt von **TCL Home**. Aus TCL Home wurde ausschließlich die technische HDMI-Grundlogik adaptiert.

## Designbasis
- Neutraler dunkler Hintergrund ohne dekorative Wellen/Schweife
- Stark abgerundete, leicht transparente Kacheln mit dezentem Schatten
- Navigation: **Home · Filme · Serien · Apps · Live TV · Hintergrund**
- Home: **Suche + HDMI-Geräte**, darunter **Favoriten-Apps**, darunter **Weiterschauen**
- HDMI zeigt **Logo + Gerätename**, keine HDMI-Portnummer
- Filme und Serien enthalten Kategorien sowie Anbieter-Badges
- Hintergrund unterstützt neutrale Farben, Live-Hintergründe und eigene Bilder

## Stand 0.1
- Android-TV-Launcher/Home-Intent
- D-Pad/Fokusanimationen
- HDMI-Erkennung über `TvInputManager`
- HDMI öffnen über Passthrough-Input
- automatische Geräte-/Markenerkennung
- manuelle HDMI-Gerätebezeichnung und Logo-Zuordnung per Langdruck
- installierte TV-Apps erkennen und starten
- Favoritenverwaltung
- Home-, Filme-, Serien-, Apps-, Live-TV- und Hintergrund-Ansichten

Film-/Serien-Titel, Anbieter-Zuordnungen, Live-TV-Programme und „Weiterschauen“ sind in 0.1 zunächst UI-Demodaten. Echte Provider-Daten kommen in einer späteren Ausbaustufe.

## APK bauen
GitHub Actions baut bei jedem Push auf `main` automatisch eine Debug-APK.

APK-Ausgabe: `app/build/outputs/apk/debug/app-debug.apk`

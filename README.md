# TCL TV 0.1

Eigenständiger Android-TV-Launcher für TCL TV. Dieses Projekt ist getrennt von **TCL Home**. Aus TCL Home wurde ausschließlich die technische HDMI-Grundlogik adaptiert.

## Designbasis
- Neutraler dunkler Hintergrund ohne dekorative Wellen/Schweife
- Stark abgerundete, leicht transparente Kacheln
- Fokus: leichte Vergrößerung + Elevation
- Navigation: **Home · Filme · Serien · Apps · Live TV · Hintergrund**
- Home: **Suche + HDMI-Geräte**, darunter **Favoriten-Apps**, darunter **Weiterschauen**
- HDMI zeigt **Logo + Gerätename**, keine HDMI-Portnummer

Referenzbilder: `docs/home-reference.png` und `docs/background-reference.png`.

## Bereits umgesetzt
- Android-TV-Launcher/Home-Intent
- D-Pad/Fokusanimationen
- HDMI-Erkennung über `TvInputManager`
- HDMI öffnen über Passthrough-Input
- automatische Geräte-/Markenerkennung
- manuelle HDMI-Gerätebezeichnung und Logo-Zuordnung per Langdruck
- installierte TV-Apps erkennen und starten
- Favoriten per Langdruck in der Apps-Ansicht
- Home-, Filme-, Serien-, Apps-, Live-TV- und Hintergrund-Ansichten
- Kategorien bei Filme/Serien
- Anbieter-Badge auf Film-/Serien-Kacheln
- neutraler Hintergrund, mehrere Farbvarianten und dezenter Live-Hintergrund
- eigenes Hintergrundbild über Android-Dateiauswahl

## Noch Demo / nächste Ausbaustufe
Die Film-/Serien-Titel, Anbieter-Zuordnungen, Live-TV-Programme und die Home-Zeile „Weiterschauen“ sind in 0.1 **UI-Demodaten**. Für echte, aktuelle Verfügbarkeiten und appübergreifenden Wiedergabefortschritt braucht TCL TV später eine geeignete Inhalts-/Provider-Datenquelle oder unterstützte Integrationen.

## APK bauen
Das Repository enthält `.github/workflows/build-apk.yml`. Auf GitHub unter **Actions → Build TCL TV APK → Run workflow** kann eine Debug-APK gebaut werden.

Alternativ lokal mit Android SDK 36 und Gradle 8.13:

```bash
gradle :app:assembleDebug
```

APK-Ausgabe:
`app/build/outputs/apk/debug/app-debug.apk`

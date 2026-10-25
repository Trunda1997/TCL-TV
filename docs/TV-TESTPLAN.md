# TCL TV 0.2: Abnahme auf dem Fernseher

Diese Prüfung benötigt einen echten TCL Google TV mit Fernbedienung. Lokale
Robolectric-Tests ersetzen weder die Firmware- noch die CEC-Prüfung. Die folgenden
Hardware-Prüfungen sind im Entwicklungscontainer nicht ausgeführt worden.

1. Debug-APK über die vorhandene Installation installieren. Paketname und
   gespeicherte Hintergrund-/HDMI-Einstellungen müssen erhalten bleiben.
2. Alle sechs Tabs sowie Einstellungen mit LEFT/RIGHT fokussieren und mit OK
   öffnen. Die aktive weiße Pill-Fläche, Texte und Fokusvergrößerung müssen
   vollständig innerhalb des sichtbaren Bildschirmbereichs liegen.
3. Home: Suche und jedes HDMI-Gerät nacheinander mit RIGHT und LEFT besuchen.
   Die Reihenfolge muss auch bei mehrfachen Fokuswechseln gleich bleiben.
4. Mehr Apps als auf einen Bildschirm passen unter Apps und in Favoriten nutzen.
   Bis zur letzten App und zurück navigieren. Jede fokussierte Karte muss
   vollständig sichtbar sein. Am Reihenende darf der Fokus nicht entweichen.
5. UP/DOWN zwischen den drei Home-Reihen testen, auch nach horizontalem Scrollen.
   Die zuletzt gewählte Karte jeder Reihe muss wieder erreichbar sein.
6. Netflix, YouTube und weitere installierte Streaming-Apps mit OK starten und
   mit BACK/Home zurückkehren. Ohne Datenänderung muss die bisherige Karte
   fokussiert bleiben; Navigation muss unmittelbar weiter funktionieren.
7. App unter Apps per Langdruck als Favorit hinzufügen/entfernen; Home erneut
   öffnen. Zusätzlich Installation/Deinstallation während TCL TV im Hintergrund
   ist prüfen. TCL TV selbst darf nicht in der App-Liste erscheinen.
8. HDMI-Karte fokussieren, dann andere Geräte ein-/ausschalten und Kabel eines
   anderen Eingangs entfernen/anstecken. Fokus und bestehende Karten müssen
   erhalten bleiben. Bei Entfernung der fokussierten Karte muss eine vorhandene
   Nachbarkarte fokussiert werden. Häufige CEC-/State-Meldungen dürfen keinen
   sichtbaren Neuaufbau auslösen.
9. HDMI-Gerät umbenennen und Marke manuell ändern. Kartenname und Logo müssen
   aktualisiert werden, der Eingang weiterhin über seinen Passthrough geöffnet
   werden. Keine Portnummer darf als Kartenname erscheinen.
10. Filme, Serien, Apps, Live TV und Hintergrund vollständig mit LEFT/RIGHT,
    UP/DOWN, OK und BACK durchgehen. Auch nicht gefüllte App-Listen und eine
    leere HDMI-Geräteliste müssen bedienbar bleiben.

Gerätemodell, Android-/Firmware-Version, installierte App-Pakete und nicht
bestehende Prüfschritte bei der Abnahme notieren. CEC kann nur Daten erkennen,
die die jeweilige TCL-Firmware öffentlich bereitstellt; sonst manuell zuordnen.

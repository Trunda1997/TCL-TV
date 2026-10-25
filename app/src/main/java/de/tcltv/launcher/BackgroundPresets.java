package de.tcltv.launcher;

import android.graphics.Color;
import java.util.Arrays;
import java.util.List;

/** Muted presets; the surrounding cards always keep TCL TV's neutral surfaces. */
public final class BackgroundPresets {
    private BackgroundPresets() {}
    public static final class Preset {
        public final String id, name, group;
        public final int start, end;
        public final boolean live;
        public Preset(String id, String name, String group, int start, int end, boolean live) {
            this.id = id; this.name = name; this.group = group;
            this.start = start; this.end = end; this.live = live;
        }
    }
    private static Preset p(String id, String name, String group, String a, String b, boolean live) {
        return new Preset(id, name, group, Color.parseColor(a), Color.parseColor(b), live);
    }
    public static final List<Preset> ALL = Arrays.asList(
        p("neutral", "Anthrazit", "Empfohlen", "#202226", "#090A0C", false),
        p("graphite", "Graphit", "Empfohlen", "#303236", "#141518", false),
        p("midnight", "Nacht", "Empfohlen", "#172333", "#111216", false),
        p("warm", "Warmgrau", "Empfohlen", "#38342F", "#1B1B1B", false),
        p("color_black", "Schwarz", "Farben", "#000000", "#000000", false),
        p("color_anthracite", "Anthrazit", "Farben", "#202226", "#202226", false),
        p("color_graphite", "Graphit", "Farben", "#303236", "#303236", false),
        p("color_darkgray", "Dunkelgrau", "Farben", "#393B3E", "#393B3E", false),
        p("color_gray", "Mittelgrau", "Farben", "#66686A", "#66686A", false),
        p("color_midnight", "Nachtblau", "Farben", "#142133", "#142133", false),
        p("color_blue", "Dunkelblau", "Farben", "#223950", "#223950", false),
        p("color_petrol", "Petrol", "Farben", "#244244", "#244244", false),
        p("color_violet", "Dunkelviolett", "Farben", "#342B42", "#342B42", false),
        p("color_bordeaux", "Bordeaux", "Farben", "#452A32", "#452A32", false),
        p("color_green", "Dunkelgrün", "Farben", "#263D31", "#263D31", false),
        p("color_warmgray", "Warmgrau", "Farben", "#4C4741", "#4C4741", false),
        p("color_beige", "Beige / Grau", "Farben", "#77736A", "#77736A", false),
        p("color_slate", "Schiefer", "Farben", "#41474D", "#41474D", false),
        p("gradient_anthracite", "Anthrazit → Schwarz", "Verläufe", "#303237", "#030405", false),
        p("gradient_blue", "Dunkelblau → Graphit", "Verläufe", "#20364D", "#1E2024", false),
        p("gradient_night", "Grau → Nachtblau", "Verläufe", "#41454A", "#111D31", false),
        p("gradient_petrol", "Petrol → Anthrazit", "Verläufe", "#244B4D", "#1D2024", false),
        p("gradient_violet", "Violett → Schwarz", "Verläufe", "#3B2A4C", "#060609", false),
        p("gradient_grayblue", "Dunkelgrau → Blau", "Verläufe", "#2D3035", "#263C55", false),
        p("gradient_bordeaux", "Schwarz → Bordeaux", "Verläufe", "#050507", "#4A2733", false),
        p("live", "Langsamer Gradient", "Live", "#18212D", "#333D49", true),
        p("light", "Dezentes Licht", "Live", "#191C23", "#45413B", true),
        p("water", "Wasser", "Live", "#10292F", "#244750", true),
        p("clouds", "Wolken", "Live", "#263442", "#626D75", true),
        p("landscape", "Landschaft", "Live", "#253B40", "#303C30", true),
        p("city", "Stadt bei Nacht", "Live", "#151A25", "#333143", true),
        p("abstract", "Abstraktes Licht", "Live", "#242235", "#3D3448", true)
    );
    public static Preset find(String id) {
        for (Preset preset : ALL) if (preset.id.equals(id)) return preset;
        return ALL.get(0);
    }
}

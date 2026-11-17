package com.example.compass

import android.content.Context

enum class CompassStyle(val label: String, val blurb: String) {
    MODERN("Modern", "Clean dark dial with a red north. The default."),
    OPERATION("Operation", "Tactical pale-green display on dark olive, with a faint radar grid and a heads-up readout."),
    GOTHIC("Gothic", "Black stone and pewter, pointed arches, a blood-red north blade and blackletter lettering."),
    PIRATE("Pirate", "Riveted brass, rope trim, a weathered chart face and a fleur-de-lis north."),
    WESTERN("Old Western", "Stitched leather, aged paper, a sheriff-star north and saloon lettering."),
    PIXEL("Pixel Art", "A chunky 64 by 64 pixel dial with 16 colors. Letters stay upright, like an old RPG.");

    fun createRenderer(context: Context): StyleRenderer = when (this) {
        MODERN -> ModernRenderer(context)
        OPERATION -> OperationRenderer(context)
        GOTHIC -> GothicRenderer(context)
        PIRATE -> PirateRenderer(context)
        WESTERN -> WesternRenderer(context)
        PIXEL -> PixelRenderer(context)
    }

    /** Widget layout for this style (same views, different readout font and colours). */
    fun widgetLayout(): Int = when (this) {
        MODERN -> R.layout.widget_compass
        OPERATION -> R.layout.widget_compass_operation
        GOTHIC -> R.layout.widget_compass_gothic
        PIRATE -> R.layout.widget_compass_pirate
        WESTERN -> R.layout.widget_compass_western
        PIXEL -> R.layout.widget_compass_pixel
    }
}

object StylePrefs {
    const val PREFS = "settings"
    const val KEY = "style"

    fun get(context: Context): CompassStyle {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return CompassStyle.values().firstOrNull { it.name == name } ?: CompassStyle.MODERN
    }

    fun set(context: Context, style: CompassStyle) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, style.name).apply()
    }
}

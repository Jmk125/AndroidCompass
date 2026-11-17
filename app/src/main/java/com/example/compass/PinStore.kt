package com.example.compass

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Pin(val id: Long, val name: String, val lat: Double, val lon: Double, val color: Int)

/** Saved pins, kept in SharedPreferences as JSON so the app and both services see the same list. */
object PinStore {
    private const val PREFS = "pins"
    private const val KEY = "list"

    /** Arrow colours. Red comes last so new pins don't start out looking like the north marker. */
    val PALETTE: List<Pair<String, Int>> = listOf(
        "Yellow" to 0xFFFFD60A.toInt(),
        "Cyan" to 0xFF32D4E6.toInt(),
        "Green" to 0xFF4CD964.toInt(),
        "Magenta" to 0xFFFF4FD8.toInt(),
        "Orange" to 0xFFFF9F0A.toInt(),
        "Blue" to 0xFF4D8DFF.toInt(),
        "Purple" to 0xFFB07CFF.toInt(),
        "Lime" to 0xFFC6F135.toInt(),
        "Red" to 0xFFE02424.toInt(),
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): List<Pin> {
        val raw = prefs(context).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Pin(o.getLong("id"), o.getString("name"), o.getDouble("lat"), o.getDouble("lon"), o.getInt("color"))
            }
        }.getOrDefault(emptyList())
    }

    private fun save(context: Context, pins: List<Pin>) {
        val arr = JSONArray()
        pins.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("name", it.name)
                    .put("lat", it.lat).put("lon", it.lon).put("color", it.color)
            )
        }
        prefs(context).edit().putString(KEY, arr.toString()).apply()
    }

    fun add(context: Context, name: String, lat: Double, lon: Double): Pin {
        val pins = load(context)
        val color = PALETTE[pins.size % PALETTE.size].second
        val pin = Pin(System.currentTimeMillis(), name, lat, lon, color)
        save(context, pins + pin)
        return pin
    }

    fun update(context: Context, pin: Pin) = save(context, load(context).map { if (it.id == pin.id) pin else it })

    fun delete(context: Context, id: Long) = save(context, load(context).filter { it.id != id })
}

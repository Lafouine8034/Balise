package org.balise.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Place(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val savedAt: Long
)

/** Stockage local des lieux (aucune donnée ne quitte le téléphone). */
class PlaceStore(context: Context) {
    private val prefs = context.getSharedPreferences("places", Context.MODE_PRIVATE)

    fun load(): List<Place> {
        val raw = prefs.getString("list", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Place(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    latitude = o.getDouble("lat"),
                    longitude = o.getDouble("lon"),
                    accuracy = o.getDouble("acc").toFloat(),
                    savedAt = o.getLong("time")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(list: List<Place>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("lat", p.latitude)
                    .put("lon", p.longitude)
                    .put("acc", p.accuracy.toDouble())
                    .put("time", p.savedAt)
            )
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }
}

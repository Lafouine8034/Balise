package org.balise.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

// Extension pour créer le DataStore
private val Context.placeDataStore: DataStore<Preferences> by preferencesDataStore(name = "places_prefs")

class PlaceStore(private val context: Context) {

    companion object {
        private val PLACES_KEY = stringPreferencesKey("saved_places")
    }

    // Charger la liste des lieux
    fun load(): List<Place> {
        return runBlocking {
            try {
                val prefs = context.placeDataStore.data.first()
                val jsonStr = prefs[PLACES_KEY] ?: "[]"
                val array = JSONArray(jsonStr)
                val places = mutableListOf<Place>()
                
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    places.add(
                        Place(
                            id = obj.getLong("id"),
                            name = obj.getString("name"),
                            latitude = obj.getDouble("lat"),
                            longitude = obj.getDouble("lon"),
                            accuracy = obj.getDouble("acc").toFloat(),
                            savedAt = obj.getLong("savedAt")
                        )
                    )
                }
                places
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // Sauvegarder la liste des lieux
    fun save(places: List<Place>) {
        runBlocking {
            try {
                val array = JSONArray()
                for (place in places) {
                    val obj = JSONObject().apply {
                        put("id", place.id)
                        put("name", place.name)
                        put("lat", place.latitude)
                        put("lon", place.longitude)
                        put("acc", place.accuracy.toDouble())
                        put("savedAt", place.savedAt)
                    }
                    array.put(obj)
                }
                
                context.placeDataStore.edit { prefs ->
                    prefs[PLACES_KEY] = array.toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

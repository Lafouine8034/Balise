package org.balise.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom

/** Compteur qui force l'écran à se rafraîchir quand un SMS arrive. */
object Inbox {
    var version by mutableIntStateOf(0)
    fun changed() {
        version = version + 1
    }
}

data class PendingRequest(val id: String, val number: String, val label: String, val sentAt: Long)

data class IncomingRequest(val id: String, val from: String, val receivedAt: Long)

data class ReceivedPosition(
    val id: String,
    val from: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val receivedAt: Long
)

class MessageStore(context: Context) {
    private val prefs = context.getSharedPreferences("messages", Context.MODE_PRIVATE)

    private fun read(key: String): List<JSONObject> =
        try {
            val arr = JSONArray(prefs.getString(key, "[]") ?: "[]")
            (0 until arr.length()).map { arr.getJSONObject(it) }
        } catch (e: Exception) {
            emptyList()
        }

    private fun write(key: String, items: List<JSONObject>) {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun loadPending(): List<PendingRequest> = read("pending").mapNotNull {
        runCatching {
            PendingRequest(it.getString("id"), it.getString("number"), it.getString("label"), it.getLong("time"))
        }.getOrNull()
    }

    fun savePending(list: List<PendingRequest>) = write(
        "pending",
        list.take(20).map {
            JSONObject().put("id", it.id).put("number", it.number).put("label", it.label).put("time", it.sentAt)
        }
    )

    fun loadIncoming(): List<IncomingRequest> = read("incoming").mapNotNull {
        runCatching {
            IncomingRequest(it.getString("id"), it.getString("from"), it.getLong("time"))
        }.getOrNull()
    }

    fun saveIncoming(list: List<IncomingRequest>) = write(
        "incoming",
        list.take(20).map {
            JSONObject().put("id", it.id).put("from", it.from).put("time", it.receivedAt)
        }
    )

    fun loadReceived(): List<ReceivedPosition> = read("received").mapNotNull {
        runCatching {
            ReceivedPosition(
                it.getString("id"),
                it.getString("from"),
                it.getString("label"),
                it.getDouble("lat"),
                it.getDouble("lon"),
                it.getDouble("acc").toFloat(),
                it.getLong("time")
            )
        }.getOrNull()
    }

    fun saveReceived(list: List<ReceivedPosition>) = write(
        "received",
        list.take(50).map {
            JSONObject()
                .put("id", it.id)
                .put("from", it.from)
                .put("label", it.label)
                .put("lat", it.latitude)
                .put("lon", it.longitude)
                .put("acc", it.accuracy.toDouble())
                .put("time", it.receivedAt)
        }
    )
}

fun newId(): String {
    val chars = "abcdefghjkmnpqrstuvwxyz23456789"
    val random = SecureRandom()
    return (1..6).map { chars[random.nextInt(chars.length)] }.joinToString("")
}

/** Compare deux numéros en ignorant l'indicatif (+33, 0033, 0…). */
fun sameNumber(a: String, b: String): Boolean {
    val x = a.filter { it.isDigit() }.takeLast(9)
    val y = b.filter { it.isDigit() }.takeLast(9)
    return x.length >= 6 && x == y
}

package org.balise.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Member(val number: String, val pseudo: String)

data class GroupMessage(
    val id: Long,
    val fromNumber: String,
    val pseudo: String,
    val text: String,
    val lat: Double?,
    val lon: Double?,
    val acc: Float,
    val time: Long,
    val mine: Boolean
)

data class Group(
    val code: String,
    val name: String,
    val members: List<Member>,
    val messages: List<GroupMessage>
)

data class GroupInvite(
    val code: String,
    val name: String,
    val fromNumber: String,
    val fromPseudo: String,
    val numbers: List<String>,
    val time: Long
)

class GroupStore(context: Context) {
    private val prefs = context.getSharedPreferences("groups", Context.MODE_PRIVATE)

    fun pseudo(): String = prefs.getString("pseudo", "") ?: ""
    fun myNumber(): String = prefs.getString("number", "") ?: ""

    fun saveProfile(pseudo: String, number: String) {
        prefs.edit().putString("pseudo", pseudo).putString("number", number).apply()
    }

    fun loadGroups(): List<Group> {
        return try {
            val arr = JSONArray(prefs.getString("groups", "[]") ?: "[]")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val ms = o.getJSONArray("members")
                val mg = o.getJSONArray("msgs")
                Group(
                    o.getString("code"),
                    o.getString("name"),
                    (0 until ms.length()).map { j ->
                        val m = ms.getJSONObject(j)
                        Member(m.getString("n"), m.getString("p"))
                    },
                    (0 until mg.length()).map { j ->
                        val m = mg.getJSONObject(j)
                        GroupMessage(
                            m.getLong("id"),
                            m.getString("from"),
                            m.getString("pseudo"),
                            m.getString("text"),
                            if (m.has("lat")) m.getDouble("lat") else null,
                            if (m.has("lon")) m.getDouble("lon") else null,
                            m.optDouble("acc", 0.0).toFloat(),
                            m.getLong("time"),
                            m.getBoolean("mine")
                        )
                    }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveGroups(list: List<Group>) {
        val arr = JSONArray()
        list.forEach { g ->
            val ms = JSONArray()
            g.members.forEach { ms.put(JSONObject().put("n", it.number).put("p", it.pseudo)) }
            val mg = JSONArray()
            g.messages.take(200).forEach { m ->
                val o = JSONObject()
                    .put("id", m.id)
                    .put("from", m.fromNumber)
                    .put("pseudo", m.pseudo)
                    .put("text", m.text)
                    .put("acc", m.acc.toDouble())
                    .put("time", m.time)
                    .put("mine", m.mine)
                if (m.lat != null && m.lon != null) {
                    o.put("lat", m.lat)
                    o.put("lon", m.lon)
                }
                mg.put(o)
            }
            arr.put(JSONObject().put("code", g.code).put("name", g.name).put("members", ms).put("msgs", mg))
        }
        prefs.edit().putString("groups", arr.toString()).apply()
    }

    fun addGroup(group: Group) {
        saveGroups(listOf(group) + loadGroups().filter { it.code != group.code })
    }

    fun removeGroup(code: String) {
        saveGroups(loadGroups().filter { it.code != code })
    }

    fun addMessage(code: String, msg: GroupMessage) {
        saveGroups(loadGroups().map {
            if (it.code == code) it.copy(messages = (listOf(msg) + it.messages).take(200)) else it
        })
    }

    fun updateMemberPseudo(code: String, number: String, pseudo: String) {
        saveGroups(loadGroups().map { g ->
            if (g.code == code) {
                g.copy(members = g.members.map { if (sameNumber(it.number, number)) it.copy(pseudo = pseudo) else it })
            } else g
        })
    }

    fun removeMember(code: String, number: String) {
        saveGroups(loadGroups().map { g ->
            if (g.code == code) g.copy(members = g.members.filter { !sameNumber(it.number, number) }) else g
        })
    }

    fun loadInvites(): List<GroupInvite> {
        return try {
            val arr = JSONArray(prefs.getString("invites", "[]") ?: "[]")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val nums = o.getJSONArray("nums")
                GroupInvite(
                    o.getString("code"),
                    o.getString("name"),
                    o.getString("from"),
                    o.getString("fromPseudo"),
                    (0 until nums.length()).map { nums.getString(it) },
                    o.getLong("time")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveInvites(list: List<GroupInvite>) {
        val arr = JSONArray()
        list.take(20).forEach { inv ->
            val nums = JSONArray()
            inv.numbers.forEach { nums.put(it) }
            arr.put(
                JSONObject()
                    .put("code", inv.code)
                    .put("name", inv.name)
                    .put("from", inv.fromNumber)
                    .put("fromPseudo", inv.fromPseudo)
                    .put("nums", nums)
                    .put("time", inv.time)
            )
        }
        prefs.edit().putString("invites", arr.toString()).apply()
    }
}

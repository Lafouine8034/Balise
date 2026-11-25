package org.balise.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val bySender = messages.groupBy { it.originatingAddress ?: "" }
        for ((sender, parts) in bySender) {
            val body = parts.joinToString("") { it.messageBody ?: "" }
            SmsHandler.handle(context.applicationContext, sender, body)
        }
    }
}

/** Ne traite que les messages au format BALISE1. Tout le reste est ignoré et jamais conservé. */
object SmsHandler {
    private val reqRegex = Regex("""BALISE1 REQ ([a-z0-9]{6})""")
    private val posRegex = Regex("""BALISE1 POS ([a-z0-9]{6}) (-?\d{1,3}\.\d+) (-?\d{1,3}\.\d+) (\d+)""")
    private val grpRegex = Regex("""BALISE1 GRP ([a-z0-9]{6}) (\S+) ([^|]+)\|([0-9+,]*)""")
    private val msgRegex = Regex("""(?s)BALISE1 MSG ([a-z0-9]{6}) (\S+) (.+)""")
    private val gpsRegex = Regex("""BALISE1 GPS ([a-z0-9]{6}) (\S+) (-?\d{1,3}\.\d+) (-?\d{1,3}\.\d+) (\d+)""")
    private val lveRegex = Regex("""BALISE1 LVE ([a-z0-9]{6}) (\S+)""")
    private const val MAX_AGE_MS = 24L * 3600L * 1000L

    fun handle(context: Context, sender: String, body: String) {
        if (sender.isBlank()) return
        if (!body.contains("BALISE1")) return
        if (handleGroup(context, sender, body)) return

        val store = MessageStore(context)

        val pos = posRegex.find(body)
        if (pos != null) {
            val id = pos.groupValues[1]
            val lat = pos.groupValues[2].toDoubleOrNull()
            val lon = pos.groupValues[3].toDoubleOrNull()
            val acc = pos.groupValues[4].toIntOrNull()
            if (lat == null || lon == null || acc == null) return
            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return
            val now = System.currentTimeMillis()
            val pending = store.loadPending().firstOrNull {
                it.id == id && sameNumber(it.number, sender) && now - it.sentAt < MAX_AGE_MS
            } ?: return
            val label = pending.label.ifBlank { pending.number }
            store.saveReceived(
                listOf(ReceivedPosition(id, sender, label, lat, lon, acc.toFloat(), now)) + store.loadReceived()
            )
            store.savePending(store.loadPending().filter { it.id != id })
            Inbox.changed()
            showNotification(context, "Position reçue", "$label a partagé sa position.", 2)
            return
        }

        val req = reqRegex.find(body)
        if (req != null) {
            val id = req.groupValues[1]
            val list = store.loadIncoming()
            if (list.any { it.id == id && it.from == sender }) return
            store.saveIncoming(listOf(IncomingRequest(id, sender, System.currentTimeMillis())) + list)
            Inbox.changed()
            showNotification(context, "Demande de position", "$sender demande votre position. Touchez pour répondre.", 2)
        }
    }

    private fun handleGroup(context: Context, sender: String, body: String): Boolean {
        val gs = GroupStore(context)
        val now = System.currentTimeMillis()

        val grp = grpRegex.find(body)
        if (grp != null) {
            val code = grp.groupValues[1]
            if (gs.loadGroups().any { it.code == code }) return true
            val invites = gs.loadInvites()
            if (invites.any { it.code == code }) return true
            val numbers = grp.groupValues[4].split(",").filter { it.isNotBlank() }
            val invite = GroupInvite(code, grp.groupValues[3].trim(), sender, grp.groupValues[2], numbers, now)
            gs.saveInvites(listOf(invite) + invites)
            Inbox.changed()
            showNotification(
                context, "Invitation de groupe",
                "${invite.fromPseudo} vous invite dans le groupe « ${invite.name} ».", 3
            )
            return true
        }

        val gps = gpsRegex.find(body)
        if (gps != null) {
            val code = gps.groupValues[1]
            val group = gs.loadGroups().firstOrNull { it.code == code } ?: return true
            if (group.members.none { sameNumber(it.number, sender) }) return true
            val lat = gps.groupValues[3].toDoubleOrNull()
            val lon = gps.groupValues[4].toDoubleOrNull()
            val acc = gps.groupValues[5].toIntOrNull()
            if (lat == null || lon == null || acc == null) return true
            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return true
            val pseudo = gps.groupValues[2]
            gs.updateMemberPseudo(code, sender, pseudo)
            gs.addMessage(
                code,
                GroupMessage(now, sender, pseudo, "Position partagée", lat, lon, acc.toFloat(), now, false)
            )
            Inbox.changed()
            showNotification(context, group.name, "$pseudo a partagé sa position.", 3)
            return true
        }

        val msg = msgRegex.find(body)
        if (msg != null) {
            val code = msg.groupValues[1]
            val group = gs.loadGroups().firstOrNull { it.code == code } ?: return true
            if (group.members.none { sameNumber(it.number, sender) }) return true
            val pseudo = msg.groupValues[2]
            val text = msg.groupValues[3].trim()
            gs.updateMemberPseudo(code, sender, pseudo)
            gs.addMessage(code, GroupMessage(now, sender, pseudo, text, null, null, 0f, now, false))
            Inbox.changed()
            showNotification(context, group.name, "$pseudo : ${text.take(100)}", 3)
            return true
        }

        val lve = lveRegex.find(body)
        if (lve != null) {
            val code = lve.groupValues[1]
            val group = gs.loadGroups().firstOrNull { it.code == code } ?: return true
            if (group.members.none { sameNumber(it.number, sender) }) return true
            gs.removeMember(code, sender)
            gs.addMessage(
                code,
                GroupMessage(now, sender, lve.groupValues[2], "a quitté le groupe", null, null, 0f, now, false)
            )
            Inbox.changed()
            return true
        }
        return false
    }
}

fun showNotification(context: Context, title: String, text: String, tab: Int) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.createNotificationChannel(
        NotificationChannel("balise_requests", "Demandes et groupes", NotificationManager.IMPORTANCE_HIGH)
    )
    val open = Intent(context, MainActivity::class.java)
        .putExtra("tab", tab)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    val pi = PendingIntent.getActivity(
        context, tab, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val notification = Notification.Builder(context, "balise_requests")
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setContentTitle(title)
        .setContentText(text)
        .setContentIntent(pi)
        .setAutoCancel(true)
        .build()
    try {
        nm.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    } catch (e: SecurityException) {
        // notifications refusées : le contenu reste visible dans l'application
    }
}

@Suppress("DEPRECATION")
fun sendSms(context: Context, number: String, text: String): Boolean {
    return try {
        val sms: SmsManager? = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }
        if (sms == null) return false
        sms.sendMultipartTextMessage(number, null, sms.divideMessage(text), null, null)
        true
    } catch (e: Exception) {
        false
    }
}

fun smsPermissionsOk(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
        hasLocationPermission(context)

fun smsPermissionList(): Array<String> {
    val list = mutableListOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    if (Build.VERSION.SDK_INT >= 33) list.add(Manifest.permission.POST_NOTIFICATIONS)
    return list.toTypedArray()
}

fun formatTime(ms: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(ms))

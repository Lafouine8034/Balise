package org.balise.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun RequestsScreen(onGuide: (Place) -> Unit) {
    val context = LocalContext.current
    val store = remember { MessageStore(context) }
    val version = Inbox.version
    val incoming = remember(version) { store.loadIncoming() }
    val received = remember(version) { store.loadReceived() }

    var permissionsGranted by remember { mutableStateOf(smsPermissionsOk(context)) }
    var number by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsGranted = smsPermissionsOk(context) }

    fun sendRequest() {
        val digits = number.filter { it.isDigit() }
        if (digits.length < 6) {
            status = "Numéro invalide."
            return
        }
        val id = newId()
        val text = "BALISE1 REQ $id - Demande de position (appli Balise). Pas d'appli ? Repondez avec votre position."
        if (sendSms(context, number.trim(), text)) {
            store.savePending(
                listOf(PendingRequest(id, number.trim(), label.trim(), System.currentTimeMillis())) + store.loadPending()
            )
            status = "Demande envoyée à ${number.trim()}. La réponse apparaîtra ici."
            number = ""
            label = ""
        } else {
            status = "Envoi impossible (pas de carte SIM ou SMS non autorisés)."
        }
    }

    fun respond(req: IncomingRequest) {
        status = "Recherche de votre position…"
        requestPosition(context) { loc ->
            if (loc == null) {
                status = "Position introuvable. Sortez à l'air libre et réessayez."
            } else {
                val text = "BALISE1 POS ${req.id} " +
                    String.format(Locale.US, "%.6f %.6f", loc.latitude, loc.longitude) +
                    " ${loc.accuracy.toInt()} " + osmLink(loc.latitude, loc.longitude)
                if (sendSms(context, req.from, text)) {
                    store.saveIncoming(store.loadIncoming().filter { !(it.id == req.id && it.from == req.from) })
                    Inbox.changed()
                    status = "Position envoyée à ${req.from}."
                } else {
                    status = "Envoi impossible."
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Demandes de position", style = MaterialTheme.typography.headlineMedium)

        if (!permissionsGranted) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Pour envoyer et recevoir des demandes par SMS, Balise a besoin des autorisations " +
                            "SMS, localisation et notifications. Balise ne conserve que les messages qui " +
                            "commencent par BALISE1 : vos autres messages ne sont ni lus ni enregistrés. " +
                            "Les SMS peuvent être facturés selon votre forfait."
                    )
                    Button(onClick = { launcher.launch(smsPermissionList()) }) { Text("Autoriser") }
                }
            }
        }

        Text("Demander la position de quelqu'un", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = number,
            onValueChange = { number = it },
            label = { Text("Numéro de téléphone") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = label,
            onValueChange = { label = it },
            label = { Text("Nom (facultatif)") },
            singleLine = true
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = permissionsGranted,
            onClick = { sendRequest() }
        ) { Text("Envoyer la demande") }

        if (status.isNotEmpty()) Text(status)

        if (incoming.isNotEmpty()) {
            Text("Demandes reçues", style = MaterialTheme.typography.titleMedium)
            incoming.forEach { req ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(req.from, fontWeight = FontWeight.Bold)
                        Text("demande votre position (${formatTime(req.receivedAt)})")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = permissionsGranted,
                                onClick = { respond(req) }
                            ) { Text("Envoyer ma position") }
                            TextButton(onClick = {
                                store.saveIncoming(
                                    store.loadIncoming().filter { !(it.id == req.id && it.from == req.from) }
                                )
                                Inbox.changed()
                            }) { Text("Ignorer") }
                        }
                    }
                }
            }
        }

        if (received.isNotEmpty()) {
            Text("Positions reçues", style = MaterialTheme.typography.titleMedium)
            received.forEach { p ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(p.label, fontWeight = FontWeight.Bold)
                        Text(formatTime(p.receivedAt), style = MaterialTheme.typography.bodySmall)
                        Text(formatDecimal(p.latitude, p.longitude))
                        Text("Précision : ± ${p.accuracy.toInt()} m", style = MaterialTheme.typography.bodySmall)
                        val asPlace = Place(p.receivedAt, p.label, p.latitude, p.longitude, p.accuracy, p.receivedAt)
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onGuide(asPlace) }
                        ) { Text("Me guider (sans réseau)") }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { openMap(context, asPlace) }) { Text("Carte") }
                            TextButton(onClick = {
                                val places = PlaceStore(context)
                                places.save(listOf(asPlace) + places.load())
                                status = "Enregistré dans Lieux."
                            }) { Text("Enregistrer") }
                            TextButton(onClick = {
                                store.saveReceived(store.loadReceived().filter { it.id != p.id || it.from != p.from })
                                Inbox.changed()
                            }) { Text("Supprimer") }
                        }
                    }
                }
            }
        }

        if (incoming.isEmpty() && received.isEmpty()) {
            Text("Aucune demande ni position reçue pour le moment.")
        }
    }
}

private fun openMap(context: Context, place: Place) {
    val geo = Uri.parse(
        "geo:${place.latitude},${place.longitude}?q=${place.latitude},${place.longitude}(${Uri.encode(place.name)})"
    )
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, geo))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(osmLink(place.latitude, place.longitude))))
    }
}

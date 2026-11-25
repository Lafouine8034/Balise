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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun cleanPseudo(s: String): String = s.trim().replace(Regex("\\s+"), "_")

private fun cleanNumber(s: String): String = s.filter { it.isDigit() || it == '+' }

@Composable
fun GroupsScreen(onGuide: (Place) -> Unit) {
    val context = LocalContext.current
    val store = remember { GroupStore(context) }
    val version = Inbox.version
    var openCode by remember { mutableStateOf<String?>(null) }
    val groups = remember(version) { store.loadGroups() }
    val invites = remember(version) { store.loadInvites() }
    val profileOk = remember(version) { store.pseudo().isNotBlank() && store.myNumber().isNotBlank() }
    val opened = groups.firstOrNull { it.code == openCode }

    if (opened != null) {
        GroupChat(group = opened, store = store, onBack = { openCode = null }, onGuide = onGuide)
    } else {
        GroupList(
            store = store,
            groups = groups,
            invites = invites,
            profileOk = profileOk,
            onOpen = { openCode = it }
        )
    }
}

@Composable
private fun GroupList(
    store: GroupStore,
    groups: List<Group>,
    invites: List<GroupInvite>,
    profileOk: Boolean,
    onOpen: (String) -> Unit
) {
    val context = LocalContext.current
    var pseudo by remember { mutableStateOf(store.pseudo()) }
    var myNumber by remember { mutableStateOf(store.myNumber()) }
    var groupName by remember { mutableStateOf("") }
    var numbersText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var permissionsGranted by remember { mutableStateOf(smsPermissionsOk(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsGranted = smsPermissionsOk(context) }

    fun createGroup() {
        val name = groupName.trim().replace("|", " ")
        val me = store.myNumber()
        val numbers = numbersText.split(",", ";", "\n")
            .map { cleanNumber(it) }
            .filter { it.filter { c -> c.isDigit() }.length >= 6 && !sameNumber(it, me) }
            .distinctBy { it.filter { c -> c.isDigit() }.takeLast(9) }
        if (name.isBlank()) {
            status = "Donnez un nom au groupe."
            return
        }
        if (numbers.isEmpty()) {
            status = "Indiquez au moins un numéro (différent du vôtre)."
            return
        }
        if (numbers.size > 8) {
            status = "8 participants maximum, pour limiter le nombre de SMS."
            return
        }
        val code = newId()
        val text = "BALISE1 GRP $code ${cleanPseudo(store.pseudo())} $name|${numbers.joinToString(",")}"
        var sent = 0
        numbers.forEach { if (sendSms(context, it, text)) sent++ }
        if (sent == 0) {
            status = "Envoi impossible (pas de carte SIM ou SMS non autorisés)."
            return
        }
        store.addGroup(Group(code, name, numbers.map { Member(it, it) }, emptyList()))
        groupName = ""
        numbersText = ""
        status = "Groupe créé, invitations envoyées ($sent)."
        Inbox.changed()
    }

    fun accept(inv: GroupInvite) {
        val me = store.myNumber()
        val creator = Member(inv.fromNumber, inv.fromPseudo)
        val others = inv.numbers
            .filter { !sameNumber(it, me) && !sameNumber(it, inv.fromNumber) }
            .map { Member(it, it) }
        store.addGroup(Group(inv.code, inv.name, listOf(creator) + others, emptyList()))
        store.saveInvites(store.loadInvites().filter { it.code != inv.code })
        Inbox.changed()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Groupes plein air", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Discutez et partagez votre position avec votre groupe par SMS, sans internet. " +
                "La position n'est envoyée que lorsque vous appuyez sur le bouton. " +
                "Chaque message est envoyé à chaque participant : il peut être facturé selon votre forfait.",
            style = MaterialTheme.typography.bodySmall
        )

        if (!permissionsGranted) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Balise a besoin des autorisations SMS, localisation et notifications. " +
                            "Seuls les messages commençant par BALISE1 sont traités."
                    )
                    Button(onClick = { launcher.launch(smsPermissionList()) }) { Text("Autoriser") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mon profil", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = pseudo,
                    onValueChange = { pseudo = it },
                    label = { Text("Mon pseudo") },
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = myNumber,
                    onValueChange = { myNumber = it },
                    label = { Text("Mon numéro de téléphone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Button(onClick = {
                    store.saveProfile(cleanPseudo(pseudo), cleanNumber(myNumber))
                    status = "Profil enregistré."
                    Inbox.changed()
                }) { Text("Enregistrer le profil") }
                if (!profileOk) {
                    Text(
                        "Le pseudo et votre numéro sont nécessaires pour créer ou rejoindre un groupe " +
                            "(le numéro sert à ne pas vous envoyer vos propres messages).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (status.isNotEmpty()) Text(status)

        if (invites.isNotEmpty()) {
            Text("Invitations", style = MaterialTheme.typography.titleMedium)
            invites.forEach { inv ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(inv.name, fontWeight = FontWeight.Bold)
                        Text("Invité par ${inv.fromPseudo} (${inv.fromNumber})")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(enabled = profileOk, onClick = { accept(inv) }) { Text("Rejoindre") }
                            TextButton(onClick = {
                                store.saveInvites(store.loadInvites().filter { it.code != inv.code })
                                Inbox.changed()
                            }) { Text("Refuser") }
                        }
                    }
                }
            }
        }

        if (groups.isNotEmpty()) {
            Text("Mes groupes", style = MaterialTheme.typography.titleMedium)
            groups.forEach { g ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(g.name, fontWeight = FontWeight.Bold)
                        Text("${g.members.size + 1} participants", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { onOpen(g.code) }) { Text("Ouvrir") }
                    }
                }
            }
        }

        Text("Créer un groupe", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Nom du groupe (ex. Battue samedi)") },
            singleLine = true
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = numbersText,
            onValueChange = { numbersText = it },
            label = { Text("Numéros des participants, séparés par des virgules") }
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = permissionsGranted && profileOk,
            onClick = { createGroup() }
        ) { Text("Créer et inviter") }
    }
}

@Composable
private fun GroupChat(group: Group, store: GroupStore, onBack: () -> Unit, onGuide: (Place) -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var confirmLeave by remember { mutableStateOf(false) }

    fun broadcast(payload: String): Boolean {
        val me = store.myNumber()
        var ok = false
        group.members
            .filter { me.isBlank() || !sameNumber(it.number, me) }
            .forEach { if (sendSms(context, it.number, payload)) ok = true }
        return ok
    }

    fun sendText() {
        val t = text.trim()
        if (t.isEmpty()) return
        val pseudo = cleanPseudo(store.pseudo())
        if (broadcast("BALISE1 MSG ${group.code} $pseudo $t")) {
            val now = System.currentTimeMillis()
            store.addMessage(
                group.code,
                GroupMessage(now, store.myNumber(), pseudo, t, null, null, 0f, now, true)
            )
            text = ""
            status = ""
            Inbox.changed()
        } else {
            status = "Envoi impossible."
        }
    }

    fun sendPosition() {
        status = "Recherche de votre position…"
        requestPosition(context) { loc ->
            if (loc == null) {
                status = "Position introuvable. Sortez à l'air libre et réessayez."
            } else {
                val pseudo = cleanPseudo(store.pseudo())
                val payload = "BALISE1 GPS ${group.code} $pseudo " +
                    String.format(Locale.US, "%.6f %.6f", loc.latitude, loc.longitude) +
                    " ${loc.accuracy.toInt()}"
                if (broadcast(payload)) {
                    val now = System.currentTimeMillis()
                    store.addMessage(
                        group.code,
                        GroupMessage(
                            now, store.myNumber(), pseudo, "Position partagée",
                            loc.latitude, loc.longitude, loc.accuracy, now, true
                        )
                    )
                    status = "Position envoyée au groupe."
                    Inbox.changed()
                } else {
                    status = "Envoi impossible."
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBack) { Text("Retour") }
            TextButton(onClick = { confirmLeave = true }) { Text("Quitter le groupe") }
        }
        Text(group.name, style = MaterialTheme.typography.headlineSmall)
        Text(
            "Avec : " + group.members.joinToString { it.pseudo },
            style = MaterialTheme.typography.bodySmall
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = text,
            onValueChange = { text = it },
            label = { Text("Message") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { sendText() }) { Text("Envoyer") }
            OutlinedButton(onClick = { sendPosition() }) { Text("Partager ma position") }
        }
        if (status.isNotEmpty()) Text(status)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(group.messages) { m ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        val who = if (m.mine) "Moi" else m.pseudo
                        Text(
                            "$who — " + SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date(m.time)),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(m.text)
                        if (m.lat != null && m.lon != null) {
                            Text(formatDecimal(m.lat, m.lon))
                            Text("Précision : ± ${m.acc.toInt()} m", style = MaterialTheme.typography.bodySmall)
                            val place = Place(m.id, "$who (${group.name})", m.lat, m.lon, m.acc, m.time)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(onClick = { onGuide(place) }) { Text("Me guider") }
                                TextButton(onClick = { openMapAt(context, place) }) { Text("Carte") }
                                TextButton(onClick = {
                                    val ps = PlaceStore(context)
                                    ps.save(listOf(place) + ps.load())
                                    status = "Enregistré dans Lieux."
                                }) { Text("Enregistrer") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Quitter ce groupe ?") },
            text = { Text("Les autres participants seront prévenus et l'historique sera effacé sur ce téléphone.") },
            confirmButton = {
                TextButton(onClick = {
                    broadcast("BALISE1 LVE ${group.code} ${cleanPseudo(store.pseudo())}")
                    store.removeGroup(group.code)
                    confirmLeave = false
                    Inbox.changed()
                    onBack()
                }) { Text("Quitter") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) { Text("Annuler") }
            }
        )
    }
}

private fun openMapAt(context: Context, place: Place) {
    val geo = Uri.parse(
        "geo:${place.latitude},${place.longitude}?q=${place.latitude},${place.longitude}(${Uri.encode(place.name)})"
    )
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, geo))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(osmLink(place.latitude, place.longitude))))
    }
}

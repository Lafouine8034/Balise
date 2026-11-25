package org.balise.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun RequestsScreen(onGuide: (Place) -> Unit) {
    val context = LocalContext.current
    val profile = remember { ProfileStore(context) }
    var number by remember { mutableStateOf("") }
    val message = requestMessage(profile)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Demander une position", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Envoyez un lien à la personne. Elle le touche, partage sa position et vous répond par SMS " +
                "ou WhatsApp. Rien à installer de son côté.",
            style = MaterialTheme.typography.bodyMedium
        )
        if (profile.name.isBlank() || profile.number.isBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OrangeLight, contentColor = DeepBlue)
            ) {
                Text(
                    "Renseignez votre nom et votre numéro dans Profil (icône 👤 de l’accueil) : " +
                        "c’est à ce numéro que la réponse sera envoyée.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = number,
            onValueChange = { number = it },
            label = { Text("Numéro de la personne (facultatif)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Button(modifier = Modifier.fillMaxWidth(), onClick = { sendBySms(context, number, message) }) {
            Text("Envoyer par SMS")
        }
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { sendByWhatsApp(context, number, message) }) {
            Text("Envoyer par WhatsApp")
        }
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { shareText(context, message) }) {
            Text("Autre application…")
        }
        Text("Message prévu", style = MaterialTheme.typography.labelLarge)
        Text(message, style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(8.dp))
        ReceivedPositionCard(onGuide)
    }
}

@Composable
fun ReceivedPositionCard(onGuide: (Place) -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var parsed by remember { mutableStateOf<ParsedPosition?>(null) }
    var status by remember { mutableStateOf("") }

    fun read(raw: String) {
        val p = parsePosition(raw)
        parsed = p
        status = if (p == null) "Aucune position trouvée dans ce texte." else ""
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Vous avez reçu une position ?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Copiez le message reçu (WhatsApp, SMS…), puis collez-le ici pour être guidé jusqu’à la personne.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = text,
                onValueChange = { text = it; parsed = null },
                label = { Text("Message reçu") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val clip = readClipboard(context)
                    if (clip.isNullOrBlank()) {
                        status = "Le presse-papiers est vide."
                    } else {
                        text = clip
                        read(clip)
                    }
                }) { Text("Coller") }
                Button(onClick = { read(text) }) { Text("Lire la position") }
            }
            if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodySmall)

            parsed?.let { p ->
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nom (ex. Marie)") },
                    singleLine = true
                )
                Text(formatDecimal(p.lat, p.lon), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                p.accuracy?.let { Text("Précision : ± ${it.toInt()} m", style = MaterialTheme.typography.bodySmall) }
                val now = System.currentTimeMillis()
                val place = Place(now, label.trim().ifEmpty { "Position reçue" }, p.lat, p.lon, p.accuracy ?: 0f, now)
                Button(modifier = Modifier.fillMaxWidth(), onClick = { onGuide(place) }) {
                    Text("Me guider (sans réseau)")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { openPlaceOnMap(context, place) }) { Text("Carte") }
                    TextButton(onClick = {
                        val ps = PlaceStore(context)
                        ps.save(listOf(place) + ps.load())
                        status = "Enregistré dans Lieux."
                    }) { Text("Enregistrer dans Lieux") }
                }
            }
        }
    }
}

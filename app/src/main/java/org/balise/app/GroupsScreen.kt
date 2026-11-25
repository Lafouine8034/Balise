package org.balise.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GroupsScreen(onGuide: (Place) -> Unit) {
    val context = LocalContext.current
    val profile = remember { ProfileStore(context) }
    var activity by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    fun shareToGroup() {
        status = "Recherche de votre position…"
        requestPosition(context) { loc ->
            if (loc == null) {
                status = "Position introuvable. Sortez à l’air libre et réessayez."
            } else {
                status = ""
                val time = SimpleDateFormat("HH:mm", Locale.FRANCE).format(Date())
                val who = profile.name.ifBlank { "Moi" }
                val tag = if (activity.isBlank()) "" else "[${activity.trim()}] "
                sharePosition(context, "$tag$who – ma position à $time", loc.latitude, loc.longitude, loc.accuracy)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) shareToGroup() else status = "Permission de localisation refusée."
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Groupes plein air", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Chasse au trésor, battue, course d’orientation, randonnée : créez votre groupe dans la " +
                "messagerie que vous utilisez déjà (WhatsApp, Signal, SMS…). Balise s’occupe de la position.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "1. Partagez votre position dans le groupe avec le bouton ci-dessous.\n" +
                "2. Quand quelqu’un partage la sienne, copiez son message et collez-le plus bas.\n" +
                "3. Balise vous guide vers lui, même sans internet.",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = activity,
            onValueChange = { activity = it },
            label = { Text("Nom de l’activité (facultatif)") },
            singleLine = true
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (hasLocationPermission(context)) {
                    shareToGroup()
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        ) { Text("Partager ma position au groupe") }
        if (status.isNotEmpty()) Text(status)
        Text(
            "La position n’est jamais envoyée en continu : seulement quand vous appuyez. En zone sans " +
                "réseau mobile, aucun message ne passe.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        ReceivedPositionCard(onGuide)
    }
}

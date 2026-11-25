package org.balise.app


import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startTab = intent.getIntExtra("tab", 0)
        setContent {
            MaterialTheme {
                BaliseApp(startTab)
            }
        }
    }
}

@Composable
fun BaliseApp(startTab: Int = 0) {
    var tab by remember { mutableIntStateOf(startTab) }
    var guiding by remember { mutableStateOf<Place?>(null) }
    val labels = listOf("Position", "Lieux", "Demandes", "Groupes", "Urgence")
    Scaffold(
        bottomBar = {
            NavigationBar {
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index; guiding = null },
                        icon = {},
                        label = { Text(label, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            val target = guiding
            if (target != null) {
                GuideScreen(place = target, onClose = { guiding = null })
            } else when (tab) {
                0 -> WhereAmIScreen()
                1 -> PlacesScreen(onGuide = { guiding = it })
                2 -> RequestsScreen(onGuide = { guiding = it })
                3 -> GroupsScreen(onGuide = { guiding = it })
                else -> EmergencyScreen()
            }
        }
    }
}
@Composable
fun WhereAmIScreen() {
    val context = LocalContext.current
    var location by remember { mutableStateOf<Location?>(null) }
    var status by remember { mutableStateOf("") }

    fun locate() {
        status = "Recherche de la position…"
        requestPosition(context) { result ->
            location = result
            status = if (result == null) "Position introuvable. Sortez à l'air libre et réessayez." else ""
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) locate() else status = "Permission de localisation refusée."
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Balise", style = MaterialTheme.typography.headlineLarge)

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (hasLocationPermission(context)) {
                    locate()
                } else {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                }
            }
        ) { Text("Où suis-je ?") }

        if (status.isNotEmpty()) Text(status)

        location?.let { loc ->
            Text(formatDecimal(loc.latitude, loc.longitude), style = MaterialTheme.typography.titleLarge)
            Text("Précision : ± ${loc.accuracy.toInt()} m")
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { sharePosition(context, "Ma position", loc.latitude, loc.longitude, loc.accuracy) }
            ) { Text("Partager ma position") }
        }
    }
}

@Composable
fun PlacesScreen(onGuide: (Place) -> Unit) {
    val context = LocalContext.current
    val store = remember { PlaceStore(context) }
    var places by remember { mutableStateOf(store.load()) }
    var pending by remember { mutableStateOf<Location?>(null) }
    var name by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<Place?>(null) }

    fun locate() {
        status = "Recherche de la position…"
        requestPosition(context) { result ->
            if (result == null) {
                status = "Position introuvable. Sortez à l'air libre et réessayez."
            } else {
                pending = result
                status = ""
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) locate() else status = "Permission de localisation refusée."
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Lieux enregistrés", style = MaterialTheme.typography.headlineMedium)

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (hasLocationPermission(context)) {
                    locate()
                } else {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                }
            }
        ) { Text("Enregistrer ma position ici") }

        if (status.isNotEmpty()) Text(status)

        pending?.let { loc ->
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom du lieu (ex. Voiture)") },
                singleLine = true
            )
            Text("Précision : ± ${loc.accuracy.toInt()} m", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val now = System.currentTimeMillis()
                    val finalName = name.trim().ifEmpty {
                        "Lieu du " + SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date(now))
                    }
                    places = listOf(
                        Place(now, finalName, loc.latitude, loc.longitude, loc.accuracy, now)
                    ) + places
                    store.save(places)
                    pending = null
                    name = ""
                }) { Text("Enregistrer") }
                TextButton(onClick = {
                    pending = null
                    name = ""
                }) { Text("Annuler") }
            }
        }

        if (places.isEmpty() && pending == null) {
            Text("Aucun lieu enregistré pour le moment.")
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(places, key = { it.id }) { place ->
                PlaceCard(
                    place = place,
                    onGuide = { onGuide(place) },
                    onDelete = { toDelete = place }
                )
            }
        }
    }

    toDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Supprimer ce lieu ?") },
            text = { Text(p.name) },
            confirmButton = {
                TextButton(onClick = {
                    places = places.filter { it.id != p.id }
                    store.save(places)
                    toDelete = null
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Annuler") }
            }
        )
    }
}

@Composable
fun PlaceCard(place: Place, onGuide: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(place.savedAt)),
                style = MaterialTheme.typography.bodySmall
            )
            Text(formatDecimal(place.latitude, place.longitude))
            Text("Précision : ± ${place.accuracy.toInt()} m", style = MaterialTheme.typography.bodySmall)
            Button(onClick = onGuide, modifier = Modifier.fillMaxWidth()) { Text("Me guider (sans réseau)") }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { openInMaps(context, place) }) { Text("Ouvrir la carte") }
                TextButton(onClick = {
                    sharePosition(context, place.name, place.latitude, place.longitude, place.accuracy)
                }) { Text("Partager") }
                TextButton(onClick = onDelete) { Text("Supprimer") }
            }
        }
    }
}

private fun openInMaps(context: Context, place: Place) {
    val geo = Uri.parse(
        "geo:${place.latitude},${place.longitude}?q=${place.latitude},${place.longitude}(${Uri.encode(place.name)})"
    )
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, geo))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(osmLink(place.latitude, place.longitude)))
        )
    }
}

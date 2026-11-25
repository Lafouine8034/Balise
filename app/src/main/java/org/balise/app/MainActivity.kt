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


private val Navy = Color(0xFF102D50)
private val DeepNavy = Color(0xFF061B30)
private val CardBlue = Color(0xFF12385E)
private val Orange = Color(0xFFF28C28)
private val Pale = Color(0xFFEAF1FA)
private val Muted = Color(0xFFB7C7DA)
private val Success = Color(0xFF35D28A)

private val BaliseColors = darkColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    secondary = Color(0xFF4A91D9),
    onSecondary = Color.White,
    background = DeepNavy,
    onBackground = Pale,
    surface = Navy,
    onSurface = Pale,
    surfaceVariant = CardBlue,
    onSurfaceVariant = Muted
)


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestedTab = intent.getIntExtra("tab", 0)

        // Conservation des anciens accès par notifications
        val startTab = when (requestedTab) {
            2 -> 3
            3 -> 4
            else -> 0
        }

        setContent {
            MaterialTheme(colorScheme = BaliseColors) {
                Surface(color = DeepNavy) {
                    BaliseApp(startTab)
                }
            }
        }
    }
}

@Composable
fun BaliseApp(startTab: Int = 0) {
    var tab by remember { mutableIntStateOf(startTab) }
    var guiding by remember { mutableStateOf<Place?>(null) }

    val labels = listOf(
        "Accueil",
        "Position",
        "Lieux",
        "Demandes",
        "Groupes",
        "Urgences"
    )

    val symbols = listOf("⌂", "⌖", "▣", "•••", "♟", "✚")

    Scaffold(
        containerColor = DeepNavy,
        bottomBar = {
            NavigationBar(
                containerColor = Navy,
                contentColor = Pale
            ) {
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = {
                            tab = index
                            guiding = null
                        },
                        icon = {
                            Text(
                                symbols[index],
                                color = if (tab == index) Orange else Muted,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        label = {
                            Text(
                                label,
                                maxLines = 1,
                                color = if (tab == index) Orange else Muted,
                                fontSize = 10.sp
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            val target = guiding

            if (target != null) {
                GuideScreen(
                    place = target,
                    onClose = { guiding = null }
                )
            } else when (tab) {
                0 -> HomeScreen(onNavigate = { tab = it })
                1 -> WhereAmIScreen()
                2 -> PlacesScreen(onGuide = { guiding = it })
                3 -> RequestsScreen(onGuide = { guiding = it })
                4 -> GroupsScreen(onGuide = { guiding = it })
                else -> EmergencyScreen()
            }
        }
    }
}


@Composable
private fun HomeScreen(onNavigate: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "BALISE",
                    color = Pale,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )
                Text(
                    "Restez proches, où que vous soyez",
                    color = Muted,
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(CardBlue, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("♥", color = Orange, fontSize = 31.sp)
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0D2946)
            ),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("⌖", color = Orange, fontSize = 30.sp)

                Column(modifier = Modifier.weight(1f)) {
                    Text("Statut", color = Muted, fontSize = 12.sp)
                    Text(
                        "Partage de position à la demande",
                        color = Success,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = { onNavigate(1) },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Orange
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(
                        horizontal = 14.dp,
                        vertical = 10.dp
                    )
                ) {
                    Text(
                        "Partager",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeTile(
                "⌖",
                "Où suis-je ?",
                "Voir ma position\net la partager",
                Modifier.weight(1f)
            ) { onNavigate(1) }

            HomeTile(
                "▣",
                "Lieux",
                "Mes lieux enregistrés\net favoris",
                Modifier.weight(1f)
            ) { onNavigate(2) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeTile(
                "•••",
                "Demandes",
                "Demander la position\nd'un proche",
                Modifier.weight(1f)
            ) { onNavigate(3) }

            HomeTile(
                "✚",
                "Urgences",
                "Annuaire des secours\net numéros utiles",
                Modifier.weight(1f),
                highlight = true
            ) { onNavigate(5) }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(4) },
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardBlue
            )
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("♟", color = Orange, fontSize = 42.sp)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Groupes",
                        color = Pale,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Activités de plein air et partage de position en groupe",
                        color = Muted,
                        fontSize = 13.sp
                    )
                }

                Text("→", color = Pale, fontSize = 26.sp)
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0B2540)
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "⬡",
                    color = Color(0xFF4A91D9),
                    fontSize = 28.sp
                )

                Column {
                    Text(
                        "Balise protège votre vie privée.",
                        color = Pale,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        "Vos données de lieux restent sur votre appareil.",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun HomeTile(
    symbol: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(174.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) {
                Color(0xFF593719)
            } else {
                CardBlue
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                symbol,
                color = Orange,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    title,
                    color = Pale,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    description,
                    color = Muted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
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

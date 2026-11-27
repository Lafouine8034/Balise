package org.balise.app

import android.content.Context
import android.location.LocationManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun locationReady(context: Context): Boolean {
    if (!hasLocationPermission(context)) return false
    val m = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return m.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        m.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

@Composable
fun BaliseMain(startTab: Int) {
    var screen by remember { mutableStateOf<Int?>(if (startTab in 0..6) startTab else null) }
    var guiding by remember { mutableStateOf<Place?>(null) }

    BackHandler(enabled = screen != null) {
        if (guiding != null) guiding = null else screen = null
    }

    val current = screen
    Surface(modifier = Modifier.fillMaxSize(), color = Night, contentColor = Color.White) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            if (current == null) {
                HomeScreen(onOpen = { screen = it })
            } else {
                val target = guiding
                val titles = listOf("Où suis-je ?", "Lieux", "Demandes", "Groupes", "Urgence", "Profil", "À propos")
                SubHeader(
                    title = if (target != null) "Guidage" else titles[current],
                    backLabel = if (target != null) "← Retour" else "← Accueil",
                    onBack = { if (target != null) guiding = null else screen = null }
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (target != null) {
                        GuideScreen(place = target, onClose = { guiding = null })
                    } else {
                        when (current) {
                            0 -> WhereAmIScreen()
                            1 -> PlacesScreen(onGuide = { guiding = it })
                            2 -> RequestsScreen(onGuide = { guiding = it })
                            3 -> GroupsScreen(onGuide = { guiding = it })
                            4 -> EmergencyScreen()
                            5 -> ProfileScreen()
                            else -> AboutScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubHeader(title: String, backLabel: String, onBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().background(DeepBlue).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text(backLabel, color = Color.White) }
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Orange))
    }
}

@Composable
private fun RoundIcon(symbol: String, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(CardBlue)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { Text(symbol, fontSize = 20.sp) }
}

@Composable
private fun HomeScreen(onOpen: (Int) -> Unit) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(locationReady(context)) }
    var message by remember { mutableStateOf("") }

    fun quickShare() {
        message = "Recherche de votre position…"
        requestPosition(context) { loc ->
            ready = locationReady(context)
            if (loc == null) {
                message = "Position introuvable. Sortez à l’air libre et réessayez."
            } else {
                message = ""
                sharePosition(context, "Ma position", loc.latitude, loc.longitude, loc.accuracy)
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        ready = locationReady(context)
        if (granted.values.any { it }) quickShare() else message = "Permission de localisation refusée."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepBlue, Night)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RoundIcon("☰", "À propos") { onOpen(6) }
                RoundIcon("👤", "Mon profil") { onOpen(5) }
            }
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(150.dp).align(Alignment.TopCenter)
            )
        }
        Text(
            "BALISE",
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 6.sp
        )
        Text(
            "Restez proches, où que vous soyez",
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = Muted
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CardBlue, contentColor = Color.White),
            border = BorderStroke(1.dp, Line)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Statut", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Text(
                        if (ready) "● Localisation prête" else "● Localisation à activer",
                        color = if (ready) Color(0xFF4ADE80) else Orange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(
                    onClick = {
                        if (hasLocationPermission(context)) quickShare() else launcher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) { Text("Partager ma position", fontWeight = FontWeight.Bold) }
            }
        }
        if (message.isNotEmpty()) Text(message, color = Muted)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("📍", "Où suis-je ?", "Voir ma position et la partager", false, Modifier.weight(1f).height(176.dp)) { onOpen(0) }
            Tile("", "Lieux", "Mes lieux enregistrés", false, Modifier.weight(1f).height(176.dp), iconRes = R.drawable.ic_car) { onOpen(1) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("💬", "Demandes", "Demander la position d’un proche", false, Modifier.weight(1f).height(176.dp)) { onOpen(2) }
            Tile("🚨", "Urgences", "Annuaire des secours et numéros utiles", true, Modifier.weight(1f).height(176.dp)) { onOpen(4) }
        }
        Card(
            onClick = { onOpen(3) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBlue, contentColor = Color.White),
            border = BorderStroke(1.dp, Line)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("👥", fontSize = 40.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Groupes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Activités de plein air et partage de position en groupe", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Arrow(false)
            }
        }
        Card(
            onClick = { onOpen(6) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBlue, contentColor = Color.White),
            border = BorderStroke(1.dp, Line)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🛡️", fontSize = 26.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Balise protège votre vie privée.", fontWeight = FontWeight.SemiBold)
                    Text("Vos données restent sur votre appareil.", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Text("›", fontSize = 24.sp, color = Muted)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Arrow(highlight: Boolean) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (highlight) Orange else Color(0xFF1B3870)),
        contentAlignment = Alignment.Center
    ) {
        Text("→", color = if (highlight) DeepBlue else Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Tile(
    emoji: String,
    title: String,
    subtitle: String,
    highlight: Boolean,
    modifier: Modifier,
    iconRes: Int? = null,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) Color(0xFF3B2410) else CardBlue,
            contentColor = Color.White
        ),
        border = BorderStroke(if (highlight) 1.5.dp else 1.dp, if (highlight) Orange else Line)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (iconRes != null) {
                Image(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(48.dp))
            } else {
                Text(emoji, fontSize = 38.sp)
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.weight(1f))
                    Arrow(highlight)
                }
            }
        }
    }
}

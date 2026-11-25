package org.balise.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BaliseMain(startTab: Int) {
    var screen by remember { mutableStateOf<Int?>(if (startTab in 0..4) startTab else null) }
    var guiding by remember { mutableStateOf<Place?>(null) }

    BackHandler(enabled = screen != null) {
        if (guiding != null) guiding = null else screen = null
    }

    val current = screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (current == null) {
            HomeScreen(onOpen = { screen = it })
        } else {
            val target = guiding
            val titles = listOf("Où suis-je ?", "Lieux", "Demandes", "Groupes", "Urgence")
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
                        else -> EmergencyScreen()
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
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepBlue)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text(backLabel, color = Color.White) }
            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Orange)
        )
    }
}

@Composable
private fun HomeScreen(onOpen: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepBlue)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
            Text(
                "Balise",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Votre position, partagée quand il le faut.",
                color = OrangeLight,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Orange)
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile(
                    "📍", "Où suis-je ?", "Ma position exacte",
                    DeepBlue, Color.White,
                    Modifier.weight(1f).aspectRatio(1f)
                ) { onOpen(0) }
                Tile(
                    "🗺️", "Lieux", "Voiture, magasin…",
                    DeepBlue, Color.White,
                    Modifier.weight(1f).aspectRatio(1f)
                ) { onOpen(1) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile(
                    "📩", "Demandes", "Position par SMS",
                    DeepBlue, Color.White,
                    Modifier.weight(1f).aspectRatio(1f)
                ) { onOpen(2) }
                Tile(
                    "👥", "Groupes", "Chasse, battue, rando",
                    DeepBlue, Color.White,
                    Modifier.weight(1f).aspectRatio(1f)
                ) { onOpen(3) }
            }
            Tile(
                "🆘", "Urgence", "112 et annuaire de secours",
                Orange, DeepBlue,
                Modifier.fillMaxWidth(),
                wide = true
            ) { onOpen(4) }
        }
    }
}

@Composable
private fun Tile(
    emoji: String,
    title: String,
    subtitle: String,
    container: Color,
    content: Color,
    modifier: Modifier,
    wide: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, fontSize = 40.sp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(emoji, fontSize = 36.sp)
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

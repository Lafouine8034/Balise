package org.balise.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WhereAmIScreen()
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
                val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
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
            Text(formatDecimal(loc), style = MaterialTheme.typography.titleLarge)
            Text("Précision : ± ${loc.accuracy.toInt()} m")
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { shareLocation(context, loc) }
            ) { Text("Partager ma position") }
        }

        
    }
}

/** Demande une position unique, sans service Google (API Android standard). */
@android.annotation.SuppressLint("MissingPermission")
private fun requestPosition(context: Context, onResult: (Location?) -> Unit) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { manager.isProviderEnabled(it) }
    if (providers.isEmpty()) {
        onResult(null)
        return
    }
    val executor = ContextCompat.getMainExecutor(context)
    fun tryProvider(index: Int) {
        if (index >= providers.size) {
            onResult(null)
            return
        }
        LocationManagerCompat.getCurrentLocation(
            manager, providers[index], CancellationSignal(), executor
        ) { loc -> if (loc != null) onResult(loc) else tryProvider(index + 1) }
    }
    tryProvider(0)
}

private fun formatDecimal(loc: Location): String =
    String.format(Locale.US, "%.6f, %.6f", loc.latitude, loc.longitude)

private fun shareLocation(context: Context, loc: Location) {
    val coords = formatDecimal(loc)
    val link = String.format(
        Locale.US,
        "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=18/%.6f/%.6f",
        loc.latitude, loc.longitude, loc.latitude, loc.longitude
    )
    val text = "Ma position : $coords (précision ± ${loc.accuracy.toInt()} m)\n$link"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Partager ma position"))
}

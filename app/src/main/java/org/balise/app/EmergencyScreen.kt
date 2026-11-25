package org.balise.app

import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
fun reverseGeocode(context: Context, lat: Double, lon: Double, onResult: (String?) -> Unit) {
    val main = Handler(Looper.getMainLooper())
    if (!Geocoder.isPresent()) {
        onResult(null)
        return
    }
    val geocoder = Geocoder(context, Locale.FRANCE)
    if (Build.VERSION.SDK_INT >= 33) {
        geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) {
                val line = addresses.firstOrNull()?.getAddressLine(0)
                main.post { onResult(line) }
            }

            override fun onError(errorMessage: String?) {
                main.post { onResult(null) }
            }
        })
    } else {
        Thread {
            val line = try {
                geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()?.getAddressLine(0)
            } catch (e: Exception) {
                null
            }
            main.post { onResult(line) }
        }.start()
    }
}

@Composable
fun EmergencyScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    var loc by remember { mutableStateOf<Location?>(null) }
    var address by remember { mutableStateOf<String?>(null) }
    var addressDone by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var measuredAt by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    fun refresh() {
        status = "Recherche de la position…"
        address = null
        addressDone = false
        requestPosition(context) { result ->
            if (result == null) {
                status = "Position introuvable. Sortez à l'air libre et réessayez."
            } else {
                loc = result
                measuredAt = System.currentTimeMillis()
                status = ""
                reverseGeocode(context, result.latitude, result.longitude) { a ->
                    address = a
                    addressDone = true
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) refresh() else status = "Permission de localisation refusée."
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            onClick = {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
            }
        ) { Text("Appeler le 112", style = MaterialTheme.typography.titleLarge) }

        if (!hasLocationPermission(context) && loc == null) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            ) { Text("Autoriser la localisation") }
        }

        if (status.isNotEmpty()) Text(status)

        loc?.let { l ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Adresse approximative", style = MaterialTheme.typography.labelLarge)
                    Text(
                        when {
                            address != null -> address!!
                            addressDone -> "Adresse indisponible (pas de réseau ou aucune adresse à cet endroit)"
                            else -> "Recherche de l'adresse…"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Coordonnées GPS", style = MaterialTheme.typography.labelLarge)
                    Text(
                        formatDecimal(l.latitude, l.longitude),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Précision : ± ${l.accuracy.toInt()} m — mesurée à " +
                            SimpleDateFormat("HH:mm:ss", Locale.FRANCE).format(Date(measuredAt))
                    )
                    if (l.accuracy > 50f) {
                        Text(
                            "Position peu précise : décrivez aussi ce qui vous entoure (route, bâtiment, point de repère).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            val sentence = buildString {
                append("Je suis ")
                append(if (address != null) "près de $address. " else "à cet endroit. ")
                append("Mes coordonnées GPS sont ${formatDecimal(l.latitude, l.longitude)}, ")
                append("avec une précision d'environ ${l.accuracy.toInt()} mètres.")
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("À dire à l'opérateur", style = MaterialTheme.typography.labelLarge)
                    Text(sentence)
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { sharePosition(context, "Besoin d'aide, ma position", l.latitude, l.longitude, l.accuracy) }
            ) { Text("Envoyer ma position à un proche") }
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (hasLocationPermission(context)) {
                    refresh()
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
                ) { Text("Actualiser la position") }

        EmergencyDirectory()
    }
}

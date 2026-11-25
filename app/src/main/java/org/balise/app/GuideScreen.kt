package org.balise.app

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun GuideScreen(place: Place, onClose: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var current by remember { mutableStateOf<Location?>(null) }
    var azimuth by remember { mutableFloatStateOf(0f) }
    var hasCompass by remember { mutableStateOf(true) }

    // Garde l'écran allumé pendant le guidage
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Position en continu
    DisposableEffect(Unit) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                current = location
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        try {
            if (hasLocationPermission(context)) {
                for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                    if (manager.isProviderEnabled(p)) {
                        val last = manager.getLastKnownLocation(p)
                        if (last != null && current == null) current = last
                        manager.requestLocationUpdates(p, 1000L, 0f, listener)
                    }
                }
            }
        } catch (e: SecurityException) {
            // permission retirée : on reste sur « recherche »
        }
        onDispose { manager.removeUpdates(listener) }
    }

    // Boussole
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val r = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(r, event.values)
                // Téléphone plutôt vertical : on prend la direction de l'arrière de l'appareil
                val matrix = if (abs(r[8]) < 0.7f) {
                    val out = FloatArray(9)
                    SensorManager.remapCoordinateSystem(r, SensorManager.AXIS_X, SensorManager.AXIS_Z, out)
                    out
                } else r
                val o = FloatArray(3)
                SensorManager.getOrientation(matrix, o)
                val newAzimuth = Math.toDegrees(o[0].toDouble()).toFloat()
                azimuth = smoothAngle(azimuth, newAzimuth)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) {
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            hasCompass = false
        }
        onDispose { sm.unregisterListener(listener) }
    }

    val target = remember(place) {
        Location("balise").apply {
            latitude = place.latitude
            longitude = place.longitude
        }
    }
    val here = current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(place.name, style = MaterialTheme.typography.headlineMedium)

        if (here == null) {
            if (hasLocationPermission(context)) {
                Text("Recherche de votre position…")
            } else {
                Text("Autorisez la localisation pour Balise dans les paramètres du téléphone.")
            }
        } else {
            val distance = here.distanceTo(target)
            val bearing = here.bearingTo(target)
            val declination = GeomagneticField(
                here.latitude.toFloat(),
                here.longitude.toFloat(),
                here.altitude.toFloat(),
                System.currentTimeMillis()
            ).declination

            if (distance < 15f) {
                Text("Vous êtes arrivé", style = MaterialTheme.typography.headlineLarge)
            } else if (hasCompass) {
                Arrow(angle = bearing - (azimuth + declination))
            }

            Text(formatDistance(distance), style = MaterialTheme.typography.displayMedium)
            Text("Précision de votre position : ± ${here.accuracy.toInt()} m")

            if (!hasCompass) {
                Text("Boussole indisponible. Cap à suivre : ${bearing.roundToInt()}° (0° = nord)")
            } else {
                Text(
                    "La flèche indique la ligne droite. Si elle semble fausse, dessinez un 8 avec le téléphone pour calibrer la boussole.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        OutlinedButton(onClick = onClose) { Text("Arrêter le guidage") }
    }
}

@Composable
private fun Arrow(angle: Float) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.size(220.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            lineTo(w * 0.85f, h * 0.55f)
            lineTo(w * 0.6f, h * 0.55f)
            lineTo(w * 0.6f, h * 0.95f)
            lineTo(w * 0.4f, h * 0.95f)
            lineTo(w * 0.4f, h * 0.55f)
            lineTo(w * 0.15f, h * 0.55f)
            close()
        }
        rotate(degrees = angle, pivot = center) {
            drawPath(path = path, color = color)
        }
    }
}

private fun smoothAngle(old: Float, new: Float): Float {
    var d = new - old
    while (d > 180f) d -= 360f
    while (d < -180f) d += 360f
    return old + 0.2f * d
}

private fun formatDistance(meters: Float): String =
    if (meters < 1000f) "${meters.roundToInt()} m"
    else String.format(Locale.FRANCE, "%.1f km", meters / 1000f)

package org.balise.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import java.util.Locale

fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

/** Demande une position unique, sans service Google (API Android standard). */
@SuppressLint("MissingPermission")
fun requestPosition(context: Context, onResult: (Location?) -> Unit) {
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

fun formatDecimal(lat: Double, lon: Double): String =
    String.format(Locale.US, "%.6f, %.6f", lat, lon)

fun osmLink(lat: Double, lon: Double): String =
    String.format(
        Locale.US,
        "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=18/%.6f/%.6f",
        lat, lon, lat, lon
    )

fun sharePosition(context: Context, heading: String, lat: Double, lon: Double, accuracy: Float) {
    val text = "$heading : ${formatDecimal(lat, lon)} (précision ± ${accuracy.toInt()} m)\n${osmLink(lat, lon)}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Partager"))
}

package org.balise.app

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activité invisible lancée par le widget : récupère la position, l'enregistre
 * dans « Lieux » puis se ferme. Une activité visible peut lire la position sans
 * service d'arrière-plan ni permission spéciale.
 */
class QuickSaveActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            locateAndSave()
        } else {
            Toast.makeText(this, "Autorisez la localisation pour Balise.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasLocationPermission(this)) {
            locateAndSave()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun locateAndSave() {
        Toast.makeText(this, "Recherche de la position…", Toast.LENGTH_SHORT).show()
        requestPosition(this) { loc ->
            if (loc == null) {
                Toast.makeText(this, "Position introuvable. Sortez à l’air libre.", Toast.LENGTH_LONG).show()
            } else {
                val now = System.currentTimeMillis()
                val name = "Lieu du " + SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date(now))
                val store = PlaceStore(this)
                store.save(listOf(Place(now, name, loc.latitude, loc.longitude, loc.accuracy, now)) + store.load())
                Toast.makeText(this, "Enregistré : $name (± ${loc.accuracy.toInt()} m)", Toast.LENGTH_LONG).show()
                BaliseWidget.showSavedBriefly(this)
            }
            finish()
        }
    }
}

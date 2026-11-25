package org.balise.app

import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

object Links {
    // À adapter si votre pseudo GitHub est différent (en minuscules pour l'adresse de la page)
    const val WEB_BASE = "https://lafouine8034.github.io/Balise/"
    const val SOURCE = "https://github.com/Lafouine8034/Balise"
}

class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("profile", Context.MODE_PRIVATE)
    var name: String
        get() = prefs.getString("name", "") ?: ""
        set(value) { prefs.edit().putString("name", value).apply() }
    var number: String
        get() = prefs.getString("number", "") ?: ""
        set(value) { prefs.edit().putString("number", value).apply() }
}

/** Convertit un numéro en format international sans « + » (France par défaut). */
fun toInternational(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return when {
        raw.trim().startsWith("+") -> digits
        digits.startsWith("00") -> digits.drop(2)
        digits.startsWith("0") && digits.length == 10 -> "33" + digits.drop(1)
        else -> digits
    }
}

fun requestLink(profile: ProfileStore): String {
    val parts = mutableListOf<String>()
    val n = toInternational(profile.number)
    if (n.length >= 8) parts.add("n=$n")
    if (profile.name.isNotBlank()) parts.add("p=" + Uri.encode(profile.name.trim()))
    return if (parts.isEmpty()) Links.WEB_BASE else Links.WEB_BASE + "#" + parts.joinToString("&")
}

fun requestMessage(profile: ProfileStore): String {
    val intro = if (profile.name.isBlank()) "Peux-tu partager ta position avec moi ?" else "${profile.name.trim()} te demande ta position."
    return "$intro\nTouche ce lien, puis « Partager ma position » (rien à installer) :\n${requestLink(profile)}"
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Envoyer avec"))
}

fun sendBySms(context: Context, number: String, text: String) {
    val clean = number.filter { it.isDigit() || it == '+' }
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$clean")).putExtra("sms_body", text)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        shareText(context, text)
    }
}

fun sendByWhatsApp(context: Context, number: String, text: String) {
    val url = "https://wa.me/" + toInternational(number) + "?text=" + Uri.encode(text)
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        shareText(context, text)
    }
}

fun readClipboard(context: Context): String? {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = cm.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
}

fun openPlaceOnMap(context: Context, place: Place) {
    val geo = Uri.parse(
        "geo:${place.latitude},${place.longitude}?q=${place.latitude},${place.longitude}(${Uri.encode(place.name)})"
    )
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, geo))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(osmLink(place.latitude, place.longitude))))
    }
}

data class ParsedPosition(val lat: Double, val lon: Double, val accuracy: Float?)

/** Lit une position dans un message : lien OpenStreetMap de Balise, ou deux coordonnées « 50.123456, 3.123456 ». */
fun parsePosition(text: String): ParsedPosition? {
    val osm = Regex("""mlat=(-?\d{1,3}\.\d+)&(?:amp;)?mlon=(-?\d{1,3}\.\d+)""").find(text)
    val pair = Regex("""(-?\d{1,3}\.\d{3,})\s*[,;]\s*(-?\d{1,3}\.\d{3,})""").find(text)
    val (lat, lon) = when {
        osm != null -> osm.groupValues[1].toDouble() to osm.groupValues[2].toDouble()
        pair != null -> pair.groupValues[1].toDouble() to pair.groupValues[2].toDouble()
        else -> return null
    }
    if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
    val acc = Regex("""±\s*(\d+)\s*m""").find(text)?.groupValues?.get(1)?.toFloatOrNull()
    return ParsedPosition(lat, lon, acc)
}

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val store = remember { ProfileStore(context) }
    var name by remember { mutableStateOf(store.name) }
    var number by remember { mutableStateOf(store.number) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Mon profil", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Ces informations restent sur votre téléphone. Elles servent à écrire vos messages " +
                "et à indiquer à qui envoyer la réponse quand vous demandez une position.",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = name,
            onValueChange = { name = it; saved = false },
            label = { Text("Mon prénom ou pseudo") },
            singleLine = true
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = number,
            onValueChange = { number = it; saved = false },
            label = { Text("Mon numéro de téléphone") },
            supportingText = { Text("Format international conseillé : +33 6 12 34 56 78") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Button(onClick = {
            store.name = name.trim()
            store.number = number.trim()
            saved = true
        }) { Text("Enregistrer") }
        if (saved) Text("Profil enregistré.")
    }
}

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val version = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (e: Exception) {
        ""
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("À propos de Balise", style = MaterialTheme.typography.headlineMedium)
        if (version.isNotEmpty()) Text("Version $version", style = MaterialTheme.typography.bodySmall)
        Text("Vie privée", style = MaterialTheme.typography.titleMedium)
        Text(
            "Balise n’a ni compte, ni publicité, ni serveur. Vos positions, vos lieux et votre profil " +
                "restent sur votre téléphone. Une position n’est envoyée que lorsque vous appuyez vous-même " +
                "sur « envoyer » dans votre messagerie."
        )
        Text(
            "La page web que reçoivent vos proches ne conserve rien : leur position reste sur leur " +
                "téléphone jusqu’à ce qu’ils l’envoient."
        )
        Text(
            "Seule exception : l’adresse affichée sur l’écran Urgences est calculée par le service de " +
                "géocodage de votre téléphone (en général celui de Google), qui reçoit alors vos coordonnées."
        )
        Text("Autorisations", style = MaterialTheme.typography.titleMedium)
        Text("Localisation uniquement, pendant que l’application est ouverte. Balise ne lit ni vos SMS ni vos contacts.")
        Text(
            "Balise ne remplace pas les secours. En cas d’urgence, appelez le 112 (le 114 par SMS si vous ne pouvez pas parler).",
            style = MaterialTheme.typography.bodyMedium
        )
        Text("Logiciel libre sous licence GPL-3.0.")
        OutlinedButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Links.SOURCE)))
        }) { Text("Code source sur GitHub") }
    }
}

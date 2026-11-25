package org.balise.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class Contact(val number: String, val name: String, val detail: String)

private data class Section(val title: String, val urgent: Boolean, val contacts: List<Contact>)

private val directory = listOf(
    Section(
        "Urgences vitales — appel gratuit, 24 h/24", true,
        listOf(
            Contact("112", "Numéro d'urgence européen", "Tous les secours, partout en Europe"),
            Contact("15", "SAMU", "Urgence médicale, malaise, personne blessée"),
            Contact("17", "Police / gendarmerie", "Agression, cambriolage, danger, infraction"),
            Contact("18", "Pompiers", "Incendie, accident, secours à personne"),
            Contact("114", "Sourds et malentendants", "Par SMS, application ou internet, à la place du 15, 17 ou 18"),
            Contact("196", "Sauvetage en mer", "CROSS. Depuis le littoral ; en mer, canal 16 de la VHF"),
            Contact("191", "Sauvetage aéronautique", "Avion, hélicoptère, planeur en détresse"),
            Contact("197", "Alerte attentat / enlèvement", "Signalement d'un attentat ou d'un enlèvement")
        )
    ),
    Section(
        "Urgences sociales et soins — appel gratuit", true,
        listOf(
            Contact("115", "Samu social", "Personne à la rue, hébergement d'urgence"),
            Contact("119", "Enfance en danger", "Enfant maltraité ou en danger, 24 h/24"),
            Contact("116 000", "Enfants disparus", "Disparition ou fugue d'un enfant"),
            Contact("116 117", "Médecin de garde", "Soins sans urgence vitale quand les cabinets sont fermés")
        )
    ),
    Section(
        "Écoute et soutien", false,
        listOf(
            Contact("3114", "Prévention du suicide", "24 h/24, 7 j/7, pour vous ou pour un proche"),
            Contact("09 72 39 40 50", "SOS Amitié", "Écoute des personnes en détresse, 24 h/24, anonyme"),
            Contact(
                "3919", "Violences faites aux femmes",
                "Écoute anonyme et gratuite. En danger immédiat : 17, ou 114 par SMS"
            ),
            Contact("3018", "Cyberharcèlement", "Jeunes victimes de violences numériques, 7 j/7, 9 h-23 h"),
            Contact("3020", "Harcèlement scolaire", "Élèves, parents et professionnels"),
            Contact("3133", "Maltraitance envers un adulte", "Pour les victimes et les témoins"),
            Contact("116 006", "Aide aux victimes", "France Victimes, victimes d'infractions")
        )
    ),
    Section(
        "Addictions et santé", false,
        listOf(
            Contact("0 800 23 13 13", "Drogues Info Service", "Anonyme et gratuit, 8 h-2 h, 7 j/7"),
            Contact("0 980 980 930", "Alcool Info Service", "8 h-2 h, 7 j/7"),
            Contact("0 980 980 940", "Écoute Cannabis", "8 h-2 h, 7 j/7"),
            Contact("39 89", "Tabac Info Service", "Aide à l'arrêt, du lundi au samedi, 8 h-20 h"),
            Contact("09 74 75 13 13", "Joueurs Info Service", "Jeux d'argent, 8 h-2 h, 7 j/7"),
            Contact("0 800 840 800", "Sida Info Service", "VIH et infections sexuellement transmissibles"),
            Contact("32 37", "Pharmacie de garde", "Pharmacie ouverte la nuit, le dimanche et les jours fériés")
        )
    )
)

@Composable
fun EmergencyDirectory() {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Annuaire de secours",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            "Touchez un numéro pour l'afficher dans le clavier du téléphone. L'appel n'est jamais lancé tout seul.",
            style = MaterialTheme.typography.bodySmall
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OrangeLight, contentColor = DeepBlue)
        ) {
            Text(
                "Depuis octobre 2024, après un appel à un service d'urgence, vous pouvez être rappelé " +
                    "depuis le 0 800 112 112 : répondez à cet appel.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
        directory.forEach { section ->
            Text(
                section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            section.contacts.forEach { contact ->
                ContactRow(contact, section.urgent) { dial(context, contact.number) }
            }
        }
        Text(
            "Numéros relevés en novembre 2026 sur service-public.gouv.fr, santepubliquefrance.fr " +
                "et sante.fr. Numéros et horaires peuvent changer. En cas de doute, appelez le 112.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ContactRow(contact: Contact, urgent: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, fontWeight = FontWeight.Bold)
                Text(contact.detail, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(12.dp))
            Surface(
                color = if (urgent) Orange else DeepBlue,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    contact.number,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = if (urgent) DeepBlue else Color.White,
                    fontWeight = FontWeight.Bold,
                    softWrap = false
                )
            }
        }
    }
}

private fun dial(context: Context, number: String) {
    val digits = number.filter { it.isDigit() }
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")))
    } catch (e: ActivityNotFoundException) {
        // appareil sans application Téléphone (par exemple un Chromebook)
    }
}

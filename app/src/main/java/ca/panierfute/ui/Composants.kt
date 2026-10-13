package ca.panierfute.ui

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val FR: Locale = Locale.CANADA_FRENCH

fun argent(montant: Double): String = NumberFormat.getCurrencyInstance(FR).format(montant)

fun dateCourte(iso: String?): String = try {
    if (iso.isNullOrBlank()) "" else LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("EEEE d MMM", FR))
} catch (e: Exception) {
    iso ?: ""
}

fun majuscule(s: String): String = s.replaceFirstChar { it.titlecase(FR) }

/** Grand titre d'écran. */
@Composable
fun EnTete(titre: String, sousTitre: String? = null) {
    Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(titre, style = MaterialTheme.typography.headlineMedium)
        if (!sousTitre.isNullOrBlank()) {
            Text(
                sousTitre,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Carte de section avec titre. */
@Composable
fun Section(titre: String, modifier: Modifier = Modifier, contenu: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(titre, style = MaterialTheme.typography.titleMedium)
            contenu()
        }
    }
}

/** Petite pastille colorée (ex. « RABAIS »). */
@Composable
fun Pastille(texte: String, fond: Color, encre: Color) {
    Surface(color = fond, contentColor = encre, shape = RoundedCornerShape(6.dp)) {
        Text(
            texte,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun PastilleRabais() = Pastille(
    "RABAIS",
    MaterialTheme.colorScheme.tertiaryContainer,
    MaterialTheme.colorScheme.onTertiaryContainer
)

/** Ligne titre + interrupteur. */
@Composable
fun LigneInterrupteur(titre: String, detail: String? = null, actif: Boolean, changer: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titre, style = MaterialTheme.typography.bodyLarge)
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = actif, onCheckedChange = changer)
    }
}

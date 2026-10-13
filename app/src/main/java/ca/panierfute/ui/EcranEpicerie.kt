package ca.panierfute.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ca.panierfute.data.ArticleEpicerie
import ca.panierfute.data.Plan
import ca.panierfute.data.indexRayon

@Composable
fun EcranEpicerie(vm: AppViewModel) {
    val plan = vm.plan
    val ctx = LocalContext.current

    if (plan == null || plan.epicerie.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnTete("Épicerie")
            Section("Rien à acheter pour l'instant") {
                Text(
                    "La liste apparaîtra ici dès que ton plan de la semaine sera généré.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    val rayons = plan.epicerie
        .groupBy { it.rayon }
        .toList()
        .sortedBy { indexRayon(it.first) }
    val faits = plan.epicerie.count { it.index in vm.coches }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val sous = plan.circulaireFin?.let { "Prix de la circulaire valides jusqu'au ${dateCourte(it)}" }
            EnTete("Épicerie", sous)
        }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Total estimé", style = MaterialTheme.typography.labelLarge)
                            Text(argent(plan.totalEstime), style = MaterialTheme.typography.headlineMedium)
                        }
                        FilledTonalButton(onClick = { partager(ctx, plan, vm.reglages.marchand) }) {
                            Icon(Icons.Filled.Share, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Partager")
                        }
                    }
                    LinearProgressIndicator(
                        progress = { if (plan.epicerie.isEmpty()) 0f else faits.toFloat() / plan.epicerie.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$faits sur ${plan.epicerie.size} dans le panier · " +
                                "${plan.epicerie.count { it.enRabais }} en rabais",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        if (faits > 0) TextButton(onClick = { vm.decocherTout() }) { Text("Tout décocher") }
                    }
                }
            }
        }
        rayons.forEach { (rayon, articles) ->
            item {
                Text(
                    rayon,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column {
                        articles.forEachIndexed { i, a ->
                            if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            LigneArticle(a, a.index in vm.coches) { vm.basculer(a.index) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LigneArticle(a: ArticleEpicerie, coche: Boolean, basculer: () -> Unit) {
    val attenue = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().clickable { basculer() }.padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = coche, onCheckedChange = { basculer() })
        Column(Modifier.weight(1f)) {
            Text(
                a.article,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (coche) TextDecoration.LineThrough else null,
                color = if (coche) attenue else MaterialTheme.colorScheme.onSurface
            )
            val details = listOf(a.quantite, a.pour.joinToString(", ")).filter { it.isNotBlank() }
            if (details.isNotEmpty()) {
                Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = attenue)
            }
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp, end = 8.dp)) {
            val prix = a.cout ?: a.prixUnitaire
            if (prix != null) {
                Text(
                    (if (a.prixEstime) "≈ " else "") + argent(prix),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (a.enRabais) FontWeight.Bold else FontWeight.Normal
                )
            }
            if (a.enRabais) PastilleRabais()
        }
    }
}

/** Partage la liste en texte (Samsung Notes, Keep, texto…). */
private fun partager(ctx: android.content.Context, plan: Plan, marchand: String) {
    val texte = buildString {
        append("🛒 Épicerie — ").append(marchand)
        plan.circulaireFin?.let { append(" (rabais jusqu'au ").append(dateCourte(it)).append(")") }
        append("\n")
        plan.epicerie.groupBy { it.rayon }.toList().sortedBy { indexRayon(it.first) }.forEach { (rayon, articles) ->
            append("\n").append(rayon.uppercase(FR)).append("\n")
            articles.forEach { a ->
                append("☐ ").append(a.article)
                if (a.quantite.isNotBlank()) append(" — ").append(a.quantite)
                val prix = a.cout ?: a.prixUnitaire
                if (prix != null) append(" — ").append(if (a.prixEstime) "≈ " else "").append(argent(prix))
                if (a.enRabais) append(" 🏷️")
                append("\n")
            }
        }
        append("\nTotal estimé : ").append(argent(plan.totalEstime))
    }
    val envoi = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texte)
    }
    ctx.startActivity(Intent.createChooser(envoi, "Partager la liste d'épicerie"))
}

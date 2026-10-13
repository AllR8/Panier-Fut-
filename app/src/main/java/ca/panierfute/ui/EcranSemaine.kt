package ca.panierfute.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.panierfute.data.IngredientRepas
import ca.panierfute.data.ItemInventaire
import ca.panierfute.data.Plan
import ca.panierfute.data.Repas
import java.util.Locale

@Composable
fun EcranSemaine(vm: AppViewModel) {
    val plan = vm.plan
    var confirmer by remember { mutableStateOf(false) }

    fun demanderGeneration() {
        val recent = plan != null && System.currentTimeMillis() - plan.genereLe < 3L * 24 * 3600 * 1000
        if (recent) confirmer = true else vm.generer()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EnTete(
                "Ta semaine",
                "Rappel photos : ${vm.prochainRappel}\nGénération auto : ${vm.prochaineGeneration}"
            )
        }

        if (vm.generationEnCours || vm.attenteReseau) {
            item { CarteProgression(if (vm.attenteReseau) "En attente d'une connexion Internet…" else vm.etape) }
        }
        if (vm.erreur.isNotBlank() && !vm.generationEnCours) {
            item { CarteErreur(vm.erreur) }
        }

        if (plan == null) {
            item { CarteBienvenue(vm) }
        } else {
            item { CarteResume(plan) }
            plan.repas.groupBy { it.jour }.forEach { (jour, repasDuJour) ->
                item {
                    Text(
                        majuscule(jour),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(repasDuJour) { r -> CarteRepas(r) }
            }
            if (plan.inventaire.isNotEmpty()) item { CarteInventaire(plan.inventaire) }
            if (plan.remarques.isNotEmpty()) {
                item {
                    Section("Remarques") {
                        plan.remarques.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { demanderGeneration() },
                enabled = !vm.generationEnCours && !vm.attenteReseau,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (plan == null) "Générer mon plan maintenant" else "Régénérer le plan")
            }
        }
    }

    if (confirmer) {
        AlertDialog(
            onDismissRequest = { confirmer = false },
            title = { Text("Régénérer le plan ?") },
            text = {
                Text(
                    "Un plan a déjà été généré cette semaine. Un nouveau plan remplacera l'actuel " +
                        "(et la liste d'épicerie). Coût : moins d'un cent."
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmer = false; vm.generer() }) { Text("Régénérer") }
            },
            dismissButton = {
                TextButton(onClick = { confirmer = false }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun CarteProgression(etape: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Préparation de ton plan…", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (etape.isNotBlank()) Text(etape, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Tu peux fermer l'app : une notification t'avertira.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CarteErreur(message: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("La dernière génération a échoué", style = MaterialTheme.typography.titleSmall)
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CarteBienvenue(vm: AppViewModel) {
    val cleOk = vm.reglages.cleApi.isNotBlank()
    val nbPhotos = vm.photos.values.sumOf { it.size }
    Section("Bienvenue dans Panier Futé 🛒") {
        Text(
            "Chaque semaine, l'app lit la circulaire de ton ${vm.reglages.marchand}, regarde ce que tu as déjà " +
                "et te propose les repas et la liste d'épicerie qui profitent des rabais.",
            style = MaterialTheme.typography.bodyMedium
        )
        Etape(1, "Ajoute ta clé API DeepSeek", cleOk) { vm.onglet = Onglet.REGLAGES }
        Etape(2, "Prends en photo garde-manger, frigo et congélateur", nbPhotos > 0) { vm.onglet = Onglet.PHOTOS }
        Etape(3, "Génère ton plan (ou attends la génération automatique)", false) { }
    }
}

@Composable
private fun Etape(numero: Int, texte: String, faite: Boolean, action: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { action() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Pastille(
            if (faite) "✓" else numero.toString(),
            if (faite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            if (faite) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(Modifier.width(12.dp))
        Text(texte, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CarteResume(plan: Plan) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Semaine à partir du ${dateCourte(plan.debutPlan)}", style = MaterialTheme.typography.titleMedium)
            if (plan.resume.isNotBlank()) Text(plan.resume, style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()
            Chiffre("Épicerie estimée", "${argent(plan.totalEstime)} · ${plan.epicerie.size} articles")
            plan.economiesEstimees?.let { Chiffre("Économies grâce aux rabais", argent(it)) }
            if (plan.circulaireFin != null) Chiffre("Prix valides jusqu'au", dateCourte(plan.circulaireFin))
            Chiffre(
                "Coût de l'appel IA",
                String.format(Locale.CANADA_FRENCH, "%.2f ¢ US", plan.coutUsd * 100) +
                    " (${plan.nbPhotos} photo(s), ${plan.nbArticlesCirculaire} articles lus)"
            )
        }
    }
}

@Composable
private fun Chiffre(libelle: String, valeur: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(libelle, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(valeur, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CarteRepas(r: Repas) {
    var ouvert by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable { ouvert = !ouvert },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    majuscule(r.moment) + (if (r.tempsMin > 0) " · ${r.tempsMin} min" else ""),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                if (r.rabaisUtilises.isNotEmpty()) PastilleRabais()
                Icon(
                    if (ouvert) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (ouvert) "Réduire" else "Voir la recette"
                )
            }
            Text(r.nom, style = MaterialTheme.typography.titleMedium)
            if (r.description.isNotBlank()) {
                Text(r.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (ouvert) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ingrédients" + (if (r.portions > 0) " (${r.portions} portions)" else ""),
                    style = MaterialTheme.typography.titleSmall
                )
                r.ingredients.forEach { LigneIngredient(it) }
                if (r.etapes.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Préparation", style = MaterialTheme.typography.titleSmall)
                    r.etapes.forEachIndexed { i, e ->
                        Text("${i + 1}. $e", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun LigneIngredient(i: IngredientRepas) {
    val (etiquette, fond, encre) = when (i.source) {
        "rabais" -> Triple("rabais", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        "inventaire" -> Triple("en stock", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        "base" -> Triple("base", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        else -> Triple("à acheter", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            (if (i.quantite.isNotBlank()) i.quantite + " " else "") + i.nom,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Pastille(etiquette, fond, encre)
    }
}

@Composable
private fun CarteInventaire(items: List<ItemInventaire>) {
    var ouvert by remember { mutableStateOf(false) }
    Section("Ce que l'IA a vu chez toi (${items.size})") {
        TextButton(onClick = { ouvert = !ouvert }) { Text(if (ouvert) "Masquer" else "Afficher l'inventaire") }
        if (ouvert) {
            items.groupBy { it.zone.ifBlank { "autre" } }.forEach { (zone, liste) ->
                Text(majuscule(zone.replace("congelateur", "congélateur")), style = MaterialTheme.typography.titleSmall)
                liste.forEach { item ->
                    Text(
                        "• ${item.nom}" + (if (item.quantite.isNotBlank()) " — ${item.quantite}" else "") +
                            (if (item.utiliserVite) "  ⏰ à utiliser vite" else ""),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

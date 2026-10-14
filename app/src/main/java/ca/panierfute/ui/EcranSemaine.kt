@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package ca.panierfute.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
    var consignes by rememberSaveable { mutableStateOf("") }
    var garder by rememberSaveable { mutableStateOf(false) }
    val occupe = vm.generationEnCours || vm.attenteReseau
    val nouvellesPhotos = vm.photos.values.sumOf { it.size }

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

        if (occupe) {
            item { CarteProgression(if (vm.attenteReseau) "En attente d'une connexion Internet…" else vm.etape) }
        }
        if (vm.erreur.isNotBlank() && !vm.generationEnCours) {
            item { CarteErreur(vm.erreur) }
        }

        if (plan == null) {
            item { CarteBienvenue(vm) }
            item {
                Button(
                    onClick = { vm.generer() },
                    enabled = !occupe,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Générer mon plan maintenant")
                }
            }
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
            item { CarteInventaire(vm, plan) }
            if (plan.remarques.isNotEmpty()) {
                item {
                    Section("Remarques") {
                        plan.remarques.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
            item {
                CarteRegenerer(
                    consignes = consignes,
                    changerConsignes = { consignes = it },
                    garder = garder,
                    changerGarder = { garder = it },
                    occupe = occupe,
                    lancer = { confirmer = true }
                )
            }
        }
    }

    if (confirmer) {
        val source = if (nouvellesPhotos > 0) {
            "Tes $nouvellesPhotos nouvelle(s) photo(s) seront analysées."
        } else {
            "Pas de nouvelles photos : l'IA utilisera l'inventaire ci-dessus, avec tes corrections."
        }
        AlertDialog(
            onDismissRequest = { confirmer = false },
            title = { Text("Régénérer le plan ?") },
            text = {
                Text(
                    "$source Le nouveau plan remplacera l'actuel et la liste d'épicerie. " +
                        "Coût : moins d'un cent."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmer = false
                    vm.generer(consignes, garder)
                    consignes = ""
                    garder = false
                }) { Text("Régénérer") }
            },
            dismissButton = {
                TextButton(onClick = { confirmer = false }) { Text("Annuler") }
            }
        )
    }
}

/** Zone de régénération : précisions ponctuelles pour la nouvelle version du plan. */
@Composable
private fun CarteRegenerer(
    consignes: String,
    changerConsignes: (String) -> Unit,
    garder: Boolean,
    changerGarder: (Boolean) -> Unit,
    occupe: Boolean,
    lancer: () -> Unit
) {
    Section("Pas tout à fait ça ? Régénère le plan") {
        Text(
            "Dis à l'IA quoi changer. Elle repart de ton inventaire et de la circulaire.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = consignes,
            onValueChange = changerConsignes,
            label = { Text("Nouvelles préférences (facultatif)") },
            placeholder = { Text("ex. : plus végé, pas de porc, budget serré, garde le souper de samedi") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            Modifier.fillMaxWidth().clickable { changerGarder(!garder) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = garder, onCheckedChange = changerGarder)
            Text("Garder ces préférences pour les prochaines semaines", style = MaterialTheme.typography.bodyMedium)
        }
        Button(onClick = lancer, enabled = !occupe, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Régénérer le plan")
        }
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
            if (plan.consignes.isNotBlank()) {
                Text(
                    "Version demandée : « ${plan.consignes} »",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Chiffre("Épicerie estimée, taxes incluses", "${argent(plan.totalEstime)} · ${plan.epicerie.size} articles")
            if (plan.taxes > 0.0) Chiffre("dont TPS + TVQ", argent(plan.taxes))
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
                if (r.rabaisUtilises.isNotEmpty() || r.ingredients.any { it.source == "rabais" }) PastilleRabais()
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

private val ZONES = listOf(
    "garde-manger" to "🥫 Garde-manger",
    "frigo" to "🥬 Frigo",
    "congelateur" to "❄️ Congélateur"
)

/** Normalise la zone renvoyée par l'IA (« congélateur », « Frigo », etc.). */
private fun cleZone(zone: String): String {
    val z = zone.lowercase().replace("é", "e").trim()
    return when {
        z.startsWith("cong") -> "congelateur"
        z.startsWith("frig") || z.startsWith("refrig") -> "frigo"
        z.startsWith("garde") || z.startsWith("armoire") -> "garde-manger"
        else -> "autre"
    }
}

@Composable
private fun CarteInventaire(vm: AppViewModel, plan: Plan) {
    var ouvert by remember { mutableStateOf(false) }
    // index -1 = ajout d'un nouvel aliment
    var edition by remember { mutableStateOf<Pair<Int, ItemInventaire>?>(null) }
    val items = plan.inventaire

    Section("Ce que l'IA a vu chez toi (${items.size})") {
        Text(
            if (plan.inventaireCorrige) "✓ Corrigé par toi. Régénère le plan pour que les repas en tiennent compte."
            else "Une erreur ou un oubli ? Corrige ici, puis régénère le plan.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { ouvert = !ouvert }) { Text(if (ouvert) "Masquer" else "Afficher et modifier") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { edition = -1 to ItemInventaire("", "frigo", "", false) }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Ajouter")
            }
        }
        if (ouvert) {
            if (items.isEmpty()) {
                Text("Aucun aliment pour l'instant.", style = MaterialTheme.typography.bodyMedium)
            }
            val groupes = items.withIndex().groupBy { cleZone(it.value.zone) }
            (ZONES.map { it.first } + "autre").forEach { cle ->
                val liste = groupes[cle].orEmpty()
                if (liste.isNotEmpty()) {
                    Text(
                        ZONES.firstOrNull { it.first == cle }?.second ?: "Autre",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    liste.forEach { (index, item) ->
                        LigneAliment(
                            item = item,
                            editer = { edition = index to item },
                            retirer = { vm.retirerAliment(index) }
                        )
                    }
                }
            }
        }
    }

    val enCours = edition
    if (enCours != null) {
        val (index, item) = enCours
        DialogueAliment(
            initial = item,
            nouveau = index < 0,
            fermer = { edition = null },
            valider = { nouvel ->
                if (index < 0) {
                    vm.ajouterAliment(nouvel)
                    ouvert = true
                } else {
                    vm.remplacerAliment(index, nouvel)
                }
                edition = null
            }
        )
    }
}

@Composable
private fun LigneAliment(item: ItemInventaire, editer: () -> Unit, retirer: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { editer() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
            Text(item.nom, style = MaterialTheme.typography.bodyMedium)
            val details = listOfNotNull(
                item.quantite.ifBlank { null },
                if (item.utiliserVite) "⏰ à utiliser vite" else null
            )
            if (details.isNotEmpty()) {
                Text(
                    details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = editer) { Icon(Icons.Filled.Edit, contentDescription = "Modifier") }
        IconButton(onClick = retirer) { Icon(Icons.Filled.Delete, contentDescription = "Retirer") }
    }
}

@Composable
private fun DialogueAliment(
    initial: ItemInventaire,
    nouveau: Boolean,
    fermer: () -> Unit,
    valider: (ItemInventaire) -> Unit
) {
    var nom by remember(initial) { mutableStateOf(initial.nom) }
    var quantite by remember(initial) { mutableStateOf(initial.quantite) }
    var zone by remember(initial) {
        mutableStateOf(cleZone(initial.zone).let { if (it == "autre") "frigo" else it })
    }
    var vite by remember(initial) { mutableStateOf(initial.utiliserVite) }

    AlertDialog(
        onDismissRequest = fermer,
        title = { Text(if (nouveau) "Ajouter un aliment" else "Modifier l'aliment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Aliment") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantite,
                    onValueChange = { quantite = it },
                    label = { Text("Quantité (facultatif)") },
                    placeholder = { Text("ex. : 500 g, environ 4, ½ pot") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ZONES.forEach { (cle, titre) ->
                        FilterChip(selected = zone == cle, onClick = { zone = cle }, label = { Text(titre) })
                    }
                }
                LigneInterrupteur("À utiliser vite", null, vite) { vite = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { valider(ItemInventaire(nom.trim(), zone, quantite.trim(), vite)) },
                enabled = nom.isNotBlank()
            ) { Text(if (nouveau) "Ajouter" else "Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = fermer) { Text("Annuler") }
        }
    )
}

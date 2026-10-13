@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package ca.panierfute.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ca.panierfute.data.INGREDIENTS_BASE_DEFAUT

private val JOURS = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

private fun heure(h: Int, m: Int) = "%02d h %02d".format(h, m)

@Composable
fun EcranReglages(vm: AppViewModel) {
    val s = vm.reglages
    val ctx = LocalContext.current
    var montrerCle by remember { mutableStateOf(false) }
    var nouvelIngredient by remember { mutableStateOf("") }

    fun ajouterIngredients() {
        val nouveaux = nouvelIngredient.split(",", ";", "\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (nouveaux.isEmpty()) return
        vm.maj { e ->
            val existants = e.ingredientsBase.map { it.lowercase() }.toSet()
            e.copy(ingredientsBase = e.ingredientsBase + nouveaux.filter { it.lowercase() !in existants })
        }
        nouvelIngredient = ""
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EnTete("Réglages", "Tout est enregistré automatiquement.")

        // ---------- IA ----------
        Section("Intelligence artificielle (DeepSeek)") {
            OutlinedTextField(
                value = s.cleApi,
                onValueChange = { v -> vm.maj { it.copy(cleApi = v.trim()) } },
                label = { Text("Clé API DeepSeek (sk-…)") },
                singleLine = true,
                visualTransformation = if (montrerCle) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { montrerCle = !montrerCle }) { Text(if (montrerCle) "Cacher" else "Voir") }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Crée ta clé sur platform.deepseek.com (API keys) et ajoute quelques dollars de crédit : " +
                    "un plan coûte moins d'un cent.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = { vm.testerCle() }, enabled = !vm.testEnCours) { Text("Tester la clé") }
            }
            vm.resultatCle?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            OutlinedTextField(
                value = s.modele,
                onValueChange = { v -> vm.maj { it.copy(modele = v.trim()) } },
                label = { Text("Modèle") },
                singleLine = true,
                supportingText = { Text("deepseek-flash : le moins cher qui lit les images") },
                modifier = Modifier.fillMaxWidth()
            )
            LigneInterrupteur(
                "Mode réflexion",
                "Plans un peu mieux pensés, mais environ deux fois plus de jetons.",
                s.reflexion
            ) { v -> vm.maj { it.copy(reflexion = v) } }
        }

        // ---------- Épicier ----------
        Section("Mon épicier") {
            OutlinedTextField(
                value = s.marchand,
                onValueChange = { v -> vm.maj { it.copy(marchand = v) } },
                label = { Text("Bannière (nom sur Flipp)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = s.codePostal,
                onValueChange = { v -> vm.maj { it.copy(codePostal = v.uppercase()) } },
                label = { Text("Code postal de la succursale") },
                singleLine = true,
                supportingText = { Text("G1S 1T7 = Maxi, 955 boul. René-Lévesque O.") },
                modifier = Modifier.fillMaxWidth()
            )
            FilledTonalButton(onClick = { vm.testerCirculaire() }, enabled = !vm.testEnCours) {
                Text("Tester la circulaire (gratuit)")
            }
            vm.resultatCirculaire?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }

        // ---------- Ménage ----------
        Section("Mes repas") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Portions par repas", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = { vm.maj { it.copy(portions = (it.portions - 1).coerceAtLeast(1)) } },
                    modifier = Modifier.size(44.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) { Text("−") }
                Text(
                    s.portions.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                OutlinedButton(
                    onClick = { vm.maj { it.copy(portions = (it.portions + 1).coerceAtMost(12)) } },
                    modifier = Modifier.size(44.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) { Text("+") }
            }
            LigneInterrupteur("Soupers", null, s.soupers) { v -> vm.maj { it.copy(soupers = v) } }
            LigneInterrupteur("Dîners (lunchs)", "Souvent avec les restes du souper", s.diners) { v -> vm.maj { it.copy(diners = v) } }
            LigneInterrupteur("Déjeuners", null, s.dejeuners) { v -> vm.maj { it.copy(dejeuners = v) } }
            OutlinedTextField(
                value = s.preferences,
                onValueChange = { v -> vm.maj { it.copy(preferences = v) } },
                label = { Text("Préférences, allergies, restrictions") },
                placeholder = { Text("ex. : pas de fruits de mer, 2 soirs végé, on aime le épicé") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ---------- Ingrédients de base ----------
        Section("Ingrédients de base (${s.ingredientsBase.size})") {
            Text(
                "Toujours disponibles à la maison, même s'ils ne sont pas sur les photos. " +
                    "Ils ne seront jamais ajoutés à la liste d'épicerie. Touche un ingrédient pour le retirer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = nouvelIngredient,
                    onValueChange = { nouvelIngredient = it },
                    label = { Text("Ajouter (séparer par des virgules)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { ajouterIngredients() }),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { ajouterIngredients() }) { Icon(Icons.Filled.Add, contentDescription = "Ajouter") }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                s.ingredientsBase.forEach { ing ->
                    InputChip(
                        selected = false,
                        onClick = { vm.maj { it.copy(ingredientsBase = it.ingredientsBase - ing) } },
                        label = { Text(ing) },
                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Retirer", modifier = Modifier.size(16.dp)) }
                    )
                }
            }
            TextButton(onClick = { vm.maj { it.copy(ingredientsBase = INGREDIENTS_BASE_DEFAUT) } }) {
                Text("Rétablir la liste par défaut")
            }
        }

        // ---------- Horaire ----------
        Section("Horaire de la semaine") {
            Text("Rappel « prends tes photos »", style = MaterialTheme.typography.bodyLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                JOURS.forEachIndexed { i, nom ->
                    FilterChip(
                        selected = s.jourRappel == i + 1,
                        onClick = { vm.maj { it.copy(jourRappel = i + 1) } },
                        label = { Text(nom) }
                    )
                }
            }
            OutlinedButton(onClick = {
                TimePickerDialog(ctx, { _, h, m -> vm.maj { it.copy(heureRappel = h, minuteRappel = m) } },
                    s.heureRappel, s.minuteRappel, true).show()
            }) { Text("Heure du rappel : ${heure(s.heureRappel, s.minuteRappel)}") }

            Spacer(Modifier.height(4.dp))
            Text("Génération automatique du plan", style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = {
                TimePickerDialog(ctx, { _, h, m -> vm.maj { it.copy(heureGeneration = h, minuteGeneration = m) } },
                    s.heureGeneration, s.minuteGeneration, true).show()
            }) { Text("Heure de génération : ${heure(s.heureGeneration, s.minuteGeneration)}") }
            Text(
                "Prochain rappel : ${vm.prochainRappel}\nProchaine génération : ${vm.prochaineGeneration}\n" +
                    "Astuce : 00 h 30 tombe dans les heures creuses de DeepSeek (tarif réduit de moitié).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LigneInterrupteur(
                "Générer même sans photos",
                "Sinon, la génération automatique attend tes photos.",
                s.genererSansPhotos
            ) { v -> vm.maj { it.copy(genererSansPhotos = v) } }
        }

        // ---------- Samsung ----------
        Section("Fiabilité sur Galaxy") {
            Text(
                "Samsung met parfois les applis « en veille », ce qui retarde les notifications et la génération " +
                    "de nuit. Dans Paramètres › Batterie › Limites d'utilisation en arrière-plan, ajoute " +
                    "Panier Futé aux « Applications jamais en veille ».",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(onClick = {
                try {
                    ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                } catch (e: Exception) {
                    ctx.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }) { Text("Ouvrir les réglages de batterie") }
        }

        Spacer(Modifier.width(1.dp).height(24.dp))
    }
}

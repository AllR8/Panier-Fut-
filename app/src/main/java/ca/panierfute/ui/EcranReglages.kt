@file:OptIn(ExperimentalLayoutApi::class)

package ca.panierfute.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ca.panierfute.data.Epicerie
import ca.panierfute.data.Fournisseur
import ca.panierfute.data.Langue
import ca.panierfute.data.Region
import ca.panierfute.data.ingredientsBaseDefaut
import java.time.DayOfWeek

@Composable
fun EcranReglages(ui: EtatApp) {
    val t = ui.t
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MARGE, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EnTete(t.titreReglages, t.reglagesIntro)

        TitreSection(t.langueTitre)
        Segments(
            options = Langue.entries.map { it.nom },
            choisi = Langue.entries.indexOf(ui.langue),
            choisir = { ui.changerLangue(Langue.entries[it]) }
        )

        SectionEpicerie(ui)
        SectionIA(ui)
        SectionRepas(ui)
        SectionIngredients(ui)
        SectionHoraire(ui)

        TitreSection(t.fiabiliteTitre)
        val ctx = LocalContext.current
        Texte(t.fiabiliteTexte, doux = true)
        BoutonSecondaire(t.ouvrirReglagesBatterie, onClick = {
            try {
                ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e: Exception) {
                ctx.startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        })
        Spacer(Modifier.height(32.dp))
    }
}

// ---------------------------------------------------------------------------
// Mon épicerie : code postal, puis choix parmi les épiceries qui ont une circulaire
// ---------------------------------------------------------------------------

@Composable
private fun SectionEpicerie(ui: EtatApp) {
    val t = ui.t
    val r = ui.reglages
    var cp by rememberSaveable { mutableStateOf(Region.afficherCodePostal(r.codePostal)) }

    LaunchedEffect(Unit) {
        if (ui.epiceries.isEmpty() && !ui.rechercheEpiceries && ui.erreurEpiceries == null &&
            Region.codePostalValide(r.codePostal)
        ) ui.chercherEpiceries(r.codePostal)
    }

    TitreSection(t.epicerieTitre)
    Texte(t.epicerieAide, doux = true, style = P.t.petit)
    Row(verticalAlignment = Alignment.Top) {
        Champ(
            cp,
            { cp = it.uppercase() },
            t.codePostal,
            modifier = Modifier.weight(1f),
            exemple = t.exempleCodePostal,
            actions = KeyboardActions(onDone = { ui.chercherEpiceries(cp) })
        )
        Spacer(Modifier.width(10.dp))
        BoutonPrincipal(
            t.chercher,
            onClick = { ui.chercherEpiceries(cp) },
            actif = !ui.rechercheEpiceries,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    if (ui.rechercheEpiceries) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), color = P.c.encre, strokeWidth = 2.5.dp)
            Spacer(Modifier.width(10.dp))
            Texte(t.rechercheEnCours, doux = true)
        }
    }
    ui.erreurEpiceries?.let { BlocErreur(t.epicerieTitre, it) }

    val liste = ui.epiceries.filter { ui.toutesEpiceries || it.alimentation }
    val choisie = ui.epiceries.firstOrNull { it.id == r.marchandId && it.id > 0 }
        ?: ui.epiceries.firstOrNull { it.nom.equals(r.marchand, ignoreCase = true) }

    if (r.epicerieChoisie && choisie == null) {
        // Épicerie déjà choisie, liste pas encore chargée.
        Feuille(padding = PaddingValues(0.dp)) {
            LigneEpicerie(Epicerie(r.marchandId, r.marchand, r.marchandLogo, null, null, true), true, ui) {}
        }
    }
    if (liste.isNotEmpty()) {
        Texte(t.epiceriesTrouvees(liste.size), style = P.t.libelle)
        Feuille(padding = PaddingValues(0.dp)) {
            Column {
                liste.forEachIndexed { i, e ->
                    if (i > 0) Separateur(Modifier.padding(start = 76.dp))
                    LigneEpicerie(e, e == choisie, ui) { ui.choisirEpicerie(e) }
                }
            }
        }
    }
    if (ui.epiceries.isNotEmpty()) {
        LigneInterrupteur(t.afficherToutesEpiceries, t.afficherToutesAide, ui.toutesEpiceries) {
            ui.afficherToutesEpiceries(it)
        }
    }

    // Taxes de la région (modifiables)
    if (Region.codePostalValide(r.codePostal)) {
        val region = r.region
        var tauxTexte by remember(r.codePostal) {
            mutableStateOf(if (r.tauxTaxesPerso >= 0) pourcentage(r.tauxTaxesPerso * 100, r.langue) else "")
        }
        Feuille {
            Text(t.taxesTitre, style = P.t.corpsFort, color = P.c.encre)
            Texte(t.taxesAide(t.pourcent(region.tauxTaxes * 100), region.nom), doux = true, style = P.t.petit)
            Champ(
                tauxTexte,
                { v ->
                    tauxTexte = v
                    val n = v.replace(",", ".").replace("%", "").trim().toDoubleOrNull()
                    ui.maj { it.copy(tauxTaxesPerso = if (n == null || v.isBlank()) -1.0 else n.coerceIn(0.0, 30.0) / 100) }
                },
                t.tauxTaxes,
                exemple = pourcentage(region.tauxTaxes * 100, r.langue),
                clavier = KeyboardType.Decimal
            )
        }
    }

    // Caler l'horaire sur le jour où la circulaire change
    if (r.epicerieChoisie && r.jourDebutCirculaire in 1..7) {
        val debut = DayOfWeek.of(r.jourDebutCirculaire)
        val dejaAligne = r.jourRappel == debut.minus(1).value && r.heureGeneration == 0 && r.minuteGeneration == 30
        Feuille {
            Text(t.circulaireChangeLe(debut), style = P.t.corpsFort, color = P.c.encre)
            Texte(if (dejaAligne) t.horaireDejaAligne else t.alignerAide, doux = true, style = P.t.petit)
            if (!dejaAligne) BoutonSecondaire(t.alignerHoraire, onClick = { ui.alignerSurCirculaire() })
        }
    }
}

@Composable
private fun LigneEpicerie(e: Epicerie, choisie: Boolean, ui: EtatApp, choisir: () -> Unit) {
    val t = ui.t
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = choisir)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LogoEpicerie(e.logo, e.nom)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(e.nom, style = P.t.titreLigne, color = P.c.encre)
            if (e.debut != null && e.fin != null) {
                Text(t.circulaireCourte(e.debut, e.fin), style = P.t.petit, color = P.c.encreDouce)
            }
        }
        if (choisie) {
            Icon(
                Icons.Filled.Check,
                contentDescription = t.choisie,
                tint = P.c.surAction,
                modifier = Modifier.size(26.dp).clip(RoundedCornerShape(13.dp)).background(P.c.action).padding(4.dp)
            )
        }
    }
}

private fun pourcentage(v: Double, langue: Langue): String {
    val s = String.format(java.util.Locale.ROOT, "%.3f", v).trimEnd('0').trimEnd('.')
    return if (langue == Langue.EN) s else s.replace('.', ',')
}

// ---------------------------------------------------------------------------
// Intelligence artificielle : n'importe quel fournisseur
// ---------------------------------------------------------------------------

@Composable
private fun SectionIA(ui: EtatApp) {
    val t = ui.t
    val r = ui.reglages
    val f = r.fournisseur
    var montrerCle by remember { mutableStateOf(false) }

    TitreSection(t.iaTitre)
    Texte(t.iaAide, doux = true, style = P.t.petit)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Fournisseur.entries.forEach { option ->
            Puce(option.nom.ifBlank { t.fournisseurAutre }, option == f) { ui.changerFournisseur(option) }
        }
    }
    Feuille {
        if (f == Fournisseur.AUTRE) {
            Champ(
                r.urlPerso,
                { v -> ui.maj { it.copy(urlPerso = v.trim()) } },
                t.urlApi,
                exemple = "https://…/v1/chat/completions",
                clavier = KeyboardType.Uri,
                aide = t.urlApiAide
            )
        }
        Champ(
            r.cleApi,
            { v -> ui.maj { it.avecCle(v) } },
            t.cleApi(f.nom.ifBlank { t.fournisseurAutre }),
            clavier = KeyboardType.Password,
            masque = if (montrerCle) VisualTransformation.None else PasswordVisualTransformation(),
            fin = { BoutonTexte(if (montrerCle) t.cacher else t.voir, onClick = { montrerCle = !montrerCle }) },
            aide = if (f.siteCle.isNotBlank()) t.ouTrouverCle(f.siteCle) else null
        )
        Champ(
            r.modeles[f.code].orEmpty(),
            { v -> ui.maj { it.avecModele(v) } },
            t.modele,
            exemple = f.modeleDefaut.ifBlank { null },
            aide = t.modeleAide
        )
        if (f == Fournisseur.DEEPSEEK) {
            LigneInterrupteur(t.modeReflexion, t.modeReflexionAide, r.reflexion) { v -> ui.maj { it.copy(reflexion = v) } }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BoutonSecondaire(t.tester, onClick = { ui.testerCle() }, actif = !ui.testEnCours)
            if (ui.testEnCours) {
                Spacer(Modifier.width(12.dp))
                CircularProgressIndicator(Modifier.size(20.dp), color = P.c.encre, strokeWidth = 2.5.dp)
            }
        }
        ui.resultatCle?.let { Texte(it) }
    }
}

// ---------------------------------------------------------------------------
// Mes repas
// ---------------------------------------------------------------------------

@Composable
private fun SectionRepas(ui: EtatApp) {
    val t = ui.t
    val r = ui.reglages
    TitreSection(t.repasTitre)
    Feuille {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t.portions, style = P.t.corpsFort, color = P.c.encre, modifier = Modifier.weight(1f))
            BoutonSecondaire("−", onClick = { ui.maj { it.copy(portions = (it.portions - 1).coerceAtLeast(1)) } })
            Text(
                r.portions.toString(),
                style = P.t.titreSection,
                color = P.c.encre,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            BoutonSecondaire("+", onClick = { ui.maj { it.copy(portions = (it.portions + 1).coerceAtMost(12)) } })
        }
        Separateur()
        LigneInterrupteur(t.soupers, null, r.soupers) { v -> ui.maj { it.copy(soupers = v) } }
        LigneInterrupteur(t.diners, t.dinersAide, r.diners) { v -> ui.maj { it.copy(diners = v) } }
        LigneInterrupteur(t.dejeuners, null, r.dejeuners) { v -> ui.maj { it.copy(dejeuners = v) } }
        Separateur()
        Champ(
            r.preferences,
            { v -> ui.maj { it.copy(preferences = v) } },
            t.preferences,
            exemple = t.exemplePreferences,
            uneLigne = false,
            lignesMin = 2
        )
    }
}

// ---------------------------------------------------------------------------
// Ingrédients de base
// ---------------------------------------------------------------------------

@Composable
private fun SectionIngredients(ui: EtatApp) {
    val t = ui.t
    val r = ui.reglages
    var nouveau by remember { mutableStateOf("") }

    fun ajouter() {
        val ajouts = nouveau.split(",", ";", "\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (ajouts.isEmpty()) return
        ui.maj { e ->
            val existants = e.ingredientsBase.map { it.lowercase() }.toSet()
            e.copy(ingredientsBase = e.ingredientsBase + ajouts.filter { it.lowercase() !in existants })
        }
        nouveau = ""
    }

    TitreSection(t.ingredientsBaseTitre(r.ingredientsBase.size))
    Texte(t.ingredientsBaseAide, doux = true, style = P.t.petit)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Champ(
            nouveau,
            { nouveau = it },
            t.ajouterIngredients,
            modifier = Modifier.weight(1f),
            actions = KeyboardActions(onDone = { ajouter() })
        )
        IconButton(onClick = { ajouter() }) { Icon(Icons.Filled.Add, contentDescription = t.ajouter, tint = P.c.encre) }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        r.ingredientsBase.forEach { ing ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(P.c.carton)
                    .clickable { ui.maj { it.copy(ingredientsBase = it.ingredientsBase - ing) } }
                    .padding(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(ing, style = P.t.libelle, color = P.c.encre)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.Close, contentDescription = t.retirer, tint = P.c.encreDouce, modifier = Modifier.size(14.dp))
            }
        }
    }
    BoutonTexte(t.retablirListe, onClick = { ui.maj { it.copy(ingredientsBase = ingredientsBaseDefaut(it.langue)) } })
}

// ---------------------------------------------------------------------------
// Horaire
// ---------------------------------------------------------------------------

@Composable
private fun SectionHoraire(ui: EtatApp) {
    val t = ui.t
    val r = ui.reglages
    val ctx = LocalContext.current
    val format24 = ui.langue != Langue.EN

    TitreSection(t.horaireTitre)
    Feuille {
        Text(t.rappelPhotos, style = P.t.corpsFort, color = P.c.encre)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DayOfWeek.entries.forEach { jour ->
                Puce(t.jourAbrege(jour), r.jourRappel == jour.value) { ui.maj { it.copy(jourRappel = jour.value) } }
            }
        }
        BoutonSecondaire(t.heureRappel(t.heure(r.heureRappel, r.minuteRappel)), onClick = {
            TimePickerDialog(ctx, { _, h, m -> ui.maj { it.copy(heureRappel = h, minuteRappel = m) } },
                r.heureRappel, r.minuteRappel, format24).show()
        })
        Separateur()
        Text(t.generationAuto, style = P.t.corpsFort, color = P.c.encre)
        BoutonSecondaire(t.heureGeneration(t.heure(r.heureGeneration, r.minuteGeneration)), onClick = {
            TimePickerDialog(ctx, { _, h, m -> ui.maj { it.copy(heureGeneration = h, minuteGeneration = m) } },
                r.heureGeneration, r.minuteGeneration, format24).show()
        })
        Texte(t.prochainRappel(ui.prochainRappel), doux = true, style = P.t.petit)
        Texte(t.prochaineGeneration(ui.prochaineGeneration), doux = true, style = P.t.petit)
        Separateur()
        LigneInterrupteur(t.genererSansPhotos, t.genererSansPhotosAide, r.genererSansPhotos) { v ->
            ui.maj { it.copy(genererSansPhotos = v) }
        }
    }
}

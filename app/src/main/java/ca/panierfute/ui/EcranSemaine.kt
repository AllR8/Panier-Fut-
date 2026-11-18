@file:OptIn(ExperimentalLayoutApi::class)

package ca.panierfute.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ca.panierfute.R
import ca.panierfute.data.Endroit
import ca.panierfute.data.ItemInventaire
import ca.panierfute.data.Langue
import ca.panierfute.data.Plan
import ca.panierfute.data.Region
import ca.panierfute.data.Repas
import ca.panierfute.data.Source
import ca.panierfute.i18n.Textes
import java.time.LocalDate

val MARGE = 20.dp

@Composable
fun EcranSemaine(ui: EtatApp) {
    val plan = ui.plan
    val t = ui.t
    val r = ui.reglages
    var confirmer by remember { mutableStateOf(false) }
    var consignes by rememberSaveable { mutableStateOf("") }
    var garder by rememberSaveable { mutableStateOf(false) }
    val occupe = ui.generationEnCours || ui.attenteReseau
    val nouvellesPhotos = ui.photos.values.sumOf { it.size }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = MARGE, end = MARGE, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            EnTete(
                t.titreSemaine,
                if (r.epicerieChoisie) t.epicerieEtCodePostal(r.marchand, Region.afficherCodePostal(r.codePostal)) else null
            )
        }

        if (occupe) {
            item { BlocProgression(if (ui.attenteReseau) t.attenteReseau else ui.etape, t) }
        }
        if (ui.erreur.isNotBlank() && !ui.generationEnCours) {
            item { BlocErreur(t.generationEchouee, ui.erreur) }
        }

        if (plan == null) {
            item { Accueil(ui, occupe) }
        } else {
            item { BandeCirculaire(plan.circulaireDebut, plan.circulaireFin, ui.aujourdhui, t) }
            item { BlocTotal(plan, ui.langue, t) { ui.onglet = Onglet.EPICERIE } }
            if (plan.resume.isNotBlank() || plan.consignes.isNotBlank()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (plan.resume.isNotBlank()) Texte(plan.resume, doux = true)
                        if (plan.consignes.isNotBlank()) Texte(t.versionDemandee(plan.consignes), style = P.t.petit)
                    }
                }
            }

            item { TitreSection(t.titreRepas) }
            val jours = plan.repas
                .groupBy { it.date?.toString() ?: it.jour }
                .toList()
                .sortedBy { (_, liste) -> liste.first().date ?: LocalDate.MAX }
            items(jours, key = { it.first }) { (_, repas) ->
                Column {
                    JourDuPlan(repas.sortedBy { it.moment.ordinal }, ui.aujourdhui, t)
                    Separateur(Modifier.padding(top = 18.dp))
                }
            }

            item { BlocInventaire(ui, plan) }
            if (plan.remarques.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TitreSection(t.titreRemarques)
                        plan.remarques.forEach { rq ->
                            Row {
                                Box(
                                    Modifier.padding(top = 8.dp, end = 10.dp).size(5.dp)
                                        .background(P.c.encre, CircleShape)
                                )
                                Texte(rq)
                            }
                        }
                    }
                }
            }
            item {
                BlocRegenerer(
                    t = t,
                    consignes = consignes,
                    changerConsignes = { consignes = it },
                    garder = garder,
                    changerGarder = { garder = it },
                    occupe = occupe,
                    lancer = { confirmer = true }
                )
            }
            item { PiedDePage(plan, ui) }
        }
    }

    if (confirmer) {
        Dialogue(
            titre = t.regenererTitre,
            texte = t.regenererTexte(nouvellesPhotos),
            confirmer = t.regenerer,
            annuler = t.annuler,
            fermer = { confirmer = false },
            valider = {
                confirmer = false
                ui.generer(consignes, garder)
                consignes = ""
                garder = false
            }
        )
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun BlocProgression(etape: String, t: Textes) {
    Feuille {
        Text(t.preparationPlan, style = P.t.titreLigne, color = P.c.encre)
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = P.c.action,
            trackColor = P.c.trait
        )
        if (etape.isNotBlank()) Texte(etape)
        Texte(t.fermerAppPossible, doux = true, style = P.t.petit)
    }
}

@Composable
fun BlocErreur(titre: String, message: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .background(P.c.carton)
    ) {
        Box(Modifier.width(5.dp).fillMaxHeight().background(P.c.tomate))
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titre, style = P.t.titreLigne, color = P.c.tomate)
            Texte(message)
        }
    }
}

@Composable
private fun Accueil(ui: EtatApp, occupe: Boolean) {
    val t = ui.t
    val r = ui.reglages
    val nbPhotos = ui.photos.values.sumOf { it.size }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Texte(t.accueilIntro)
        Segments(
            options = Langue.entries.map { it.nom },
            choisi = Langue.entries.indexOf(ui.langue),
            choisir = { ui.changerLangue(Langue.entries[it]) }
        )
        Feuille(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
            EtapeAccueil(1, t.etapeEpicerie, if (r.epicerieChoisie) r.marchand else null, r.epicerieChoisie) {
                ui.onglet = Onglet.REGLAGES
            }
            Separateur()
            EtapeAccueil(2, t.etapeCle, if (r.iaConfiguree) r.fournisseur.nom.ifBlank { t.fournisseurAutre } else null, r.iaConfiguree) { ui.onglet = Onglet.REGLAGES }
            Separateur()
            EtapeAccueil(3, t.etapePhotos, if (nbPhotos > 0) t.nbPhotos(nbPhotos) else null, nbPhotos > 0) {
                ui.onglet = Onglet.PHOTOS
            }
        }
        BoutonPrincipal(
            t.genererPlan,
            onClick = { ui.generer() },
            actif = !occupe,
            modifier = Modifier.fillMaxWidth()
        )
        Texte(t.horaireResume(ui.prochainRappel, ui.prochaineGeneration), doux = true, style = P.t.petit)
    }
}

@Composable
private fun EtapeAccueil(numero: Int, texte: String, detail: String?, faite: Boolean, action: () -> Unit) {
    val c = P.c
    Row(
        Modifier.fillMaxWidth().clickable(onClick = action).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (faite) c.action else c.carton)
                .border(1.5.dp, if (faite) c.action else c.encre, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (faite) Icon(Icons.Filled.Check, contentDescription = null, tint = c.surAction, modifier = Modifier.size(18.dp))
            else Text(numero.toString(), style = P.t.libelle, fontWeight = FontWeight.SemiBold, color = c.encre)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(texte, style = P.t.corpsFort, color = c.encre)
            if (detail != null) Text(detail, style = P.t.petit, color = c.encreDouce)
        }
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = c.encreDouce)
    }
}

@Composable
private fun BlocTotal(plan: Plan, langue: Langue, t: Textes, ouvrirEpicerie: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = ouvrirEpicerie).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EtiquetteTotalAvecTrou(plan.totalEstime, langue)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(t.epicerieEstimee, style = P.t.corpsFort, color = P.c.encre)
            Text(t.articlesResume(plan.epicerie.size, plan.nbRabais), style = P.t.petit, color = P.c.encreDouce)
            plan.economiesEstimees?.takeIf { it > 0 }?.let {
                Text(t.economies(argent(it, langue)), style = P.t.petit, fontWeight = FontWeight.SemiBold, color = P.c.celeri)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Repas, jour par jour
// ---------------------------------------------------------------------------

@Composable
private fun JourDuPlan(repas: List<Repas>, aujourdhui: LocalDate, t: Textes) {
    val date = repas.first().date
    val estAujourdhui = date == aujourdhui
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.width(58.dp)) {
            if (date != null) {
                Text(date.dayOfMonth.toString(), style = P.t.chiffreJour, color = P.c.encre)
                Text(
                    if (estAujourdhui) t.aujourdhuiCourt else t.jourCourt(date),
                    style = P.t.libelle,
                    fontWeight = if (estAujourdhui) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (estAujourdhui) P.c.encre else P.c.encreDouce
                )
            } else {
                Text(repas.first().jour, style = P.t.libelle, color = P.c.encreDouce)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            repas.forEach { BlocRepas(it, t) }
        }
    }
}

@Composable
private fun BlocRepas(r: Repas, t: Textes) {
    var ouvert by rememberSaveable(r.nom) { mutableStateOf(false) }
    val c = P.c
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { ouvert = !ouvert }
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                t.momentEtDuree(r.moment, r.tempsMin),
                style = P.t.libelle,
                color = c.encreDouce,
                modifier = Modifier.weight(1f)
            )
            if (r.utiliseRabais) EtiquetteSpecial(t.special)
        }
        Text(r.nom, style = P.t.titreLigne, color = c.encre)
        if (r.description.isNotBlank()) {
            Text(
                r.description,
                style = P.t.corps,
                color = c.encreDouce,
                maxLines = if (ouvert) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (ouvert) Recette(r, t)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            Text(if (ouvert) t.masquerRecette else t.voirRecette, style = P.t.libelle, fontWeight = FontWeight.SemiBold, color = c.encre)
            Icon(
                if (ouvert) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = c.encre,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun Recette(r: Repas, t: Textes) {
    val c = P.c
    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(t.ingredientsPour(r.portions), style = P.t.corpsFort, color = c.encre)
        r.ingredients.forEach { i ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOf(i.quantite, i.nom).filter { it.isNotBlank() }.joinToString(" "),
                    style = P.t.corps,
                    color = c.encre,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                when (i.source) {
                    Source.RABAIS -> EtiquetteSpecial(t.sourceRabais)
                    Source.STOCK -> Text(t.sourceStock, style = P.t.libelle, color = c.celeri)
                    Source.BASE -> Text(t.sourceBase, style = P.t.libelle, color = c.encreDouce)
                    Source.ACHAT -> Text(t.sourceAchat, style = P.t.libelle, color = c.encre)
                }
            }
        }
        if (r.etapes.isNotEmpty()) {
            Text(t.preparation, style = P.t.corpsFort, color = c.encre, modifier = Modifier.padding(top = 8.dp))
            r.etapes.forEachIndexed { n, etape ->
                Row {
                    Text(
                        (n + 1).toString(),
                        style = P.t.titreLigne.copy(fontFamily = BarlowCondensed),
                        color = c.encre,
                        modifier = Modifier.width(22.dp)
                    )
                    Texte(etape, Modifier.weight(1f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Inventaire : ce que l'IA a vu, corrigeable
// ---------------------------------------------------------------------------

@Composable
private fun BlocInventaire(ui: EtatApp, plan: Plan) {
    val t = ui.t
    var ouvert by rememberSaveable { mutableStateOf(false) }
    var edition by remember { mutableStateOf<Pair<Int, ItemInventaire>?>(null) }
    val items = plan.inventaire

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TitreSection(t.titreInventaire(items.size)) {
            BoutonTexte(t.ajouter, onClick = { edition = -1 to ItemInventaire("", Endroit.FRIGO, "", false) })
        }
        Texte(if (plan.inventaireCorrige) t.inventaireCorrige else t.inventaireAide, doux = true, style = P.t.petit)
        if (!ouvert) {
            BoutonSecondaire(t.voirEtCorriger, onClick = { ouvert = true }, modifier = Modifier.fillMaxWidth())
        } else {
            Feuille(padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                if (items.isEmpty()) Texte(t.inventaireVide, doux = true)
                val groupes = items.withIndex().groupBy { it.value.endroit }
                Endroit.entries.forEach { endroit ->
                    val liste = groupes[endroit].orEmpty()
                    if (liste.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                            Icon(iconeEndroit(endroit), contentDescription = null, tint = P.c.encre, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(t.endroit(endroit), style = P.t.corpsFort, color = P.c.encre)
                        }
                        liste.forEach { (index, item) ->
                            LigneAliment(item, t, editer = { edition = index to item }, retirer = { ui.retirerAliment(index) })
                        }
                    }
                }
            }
            BoutonTexte(t.masquer, onClick = { ouvert = false })
        }
    }

    edition?.let { (index, item) ->
        DialogueAliment(
            initial = item,
            nouveau = index < 0,
            t = t,
            fermer = { edition = null },
            valider = { nouvel ->
                if (index < 0) {
                    ui.ajouterAliment(nouvel)
                    ouvert = true
                } else {
                    ui.remplacerAliment(index, nouvel)
                }
                edition = null
            }
        )
    }
}

@Composable
private fun LigneAliment(item: ItemInventaire, t: Textes, editer: () -> Unit, retirer: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = editer), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(start = 28.dp, top = 4.dp, bottom = 4.dp)) {
            Text(item.nom, style = P.t.corps, color = P.c.encre)
            if (item.quantite.isNotBlank() || item.utiliserVite) {
                Row {
                    if (item.quantite.isNotBlank()) {
                        Text(item.quantite, style = P.t.petit, color = P.c.encreDouce)
                        if (item.utiliserVite) Spacer(Modifier.width(10.dp))
                    }
                    if (item.utiliserVite) Text(t.utiliserVite, style = P.t.petit, fontWeight = FontWeight.SemiBold, color = P.c.tomate)
                }
            }
        }
        IconButton(onClick = editer) { Icon(Icons.Filled.Edit, contentDescription = t.modifier, tint = P.c.encreDouce) }
        IconButton(onClick = retirer) { Icon(Icons.Filled.Delete, contentDescription = t.retirer, tint = P.c.encreDouce) }
    }
}

@Composable
private fun DialogueAliment(
    initial: ItemInventaire,
    nouveau: Boolean,
    t: Textes,
    fermer: () -> Unit,
    valider: (ItemInventaire) -> Unit
) {
    var nom by remember(initial) { mutableStateOf(initial.nom) }
    var quantite by remember(initial) { mutableStateOf(initial.quantite) }
    var endroit by remember(initial) {
        mutableStateOf(if (initial.endroit == Endroit.AUTRE) Endroit.FRIGO else initial.endroit)
    }
    var vite by remember(initial) { mutableStateOf(initial.utiliserVite) }

    AlertDialog(
        onDismissRequest = fermer,
        containerColor = P.c.carton,
        shape = RoundedCornerShape(22.dp),
        title = { Text(if (nouveau) t.ajouterAliment else t.modifierAliment, style = P.t.titreSection, color = P.c.encre) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Champ(nom, { nom = it }, t.aliment)
                Champ(quantite, { quantite = it }, t.quantiteFacultative, exemple = t.exempleQuantite)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Endroit.GARDE_MANGER, Endroit.FRIGO, Endroit.CONGELATEUR).forEach { e ->
                        Puce(t.endroit(e), endroit == e) { endroit = e }
                    }
                }
                LigneInterrupteur(t.utiliserVite, null, vite) { vite = it }
            }
        },
        confirmButton = {
            BoutonTexte(
                if (nouveau) t.ajouter else t.enregistrer,
                onClick = { valider(ItemInventaire(nom.trim(), endroit, quantite.trim(), vite)) },
                actif = nom.isNotBlank()
            )
        },
        dismissButton = { BoutonTexte(t.annuler, onClick = fermer) }
    )
}

// ---------------------------------------------------------------------------
// Régénérer et pied de page
// ---------------------------------------------------------------------------

@Composable
private fun BlocRegenerer(
    t: Textes,
    consignes: String,
    changerConsignes: (String) -> Unit,
    garder: Boolean,
    changerGarder: (Boolean) -> Unit,
    occupe: Boolean,
    lancer: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TitreSection(t.titreRegenerer)
        Texte(t.regenererAide, doux = true, style = P.t.petit)
        Champ(
            consignes,
            changerConsignes,
            t.nouvellesPreferences,
            exemple = t.exempleConsignes,
            uneLigne = false,
            lignesMin = 2
        )
        Row(
            Modifier.fillMaxWidth().clickable { changerGarder(!garder) }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CaseACocher(garder, onClick = null)
            Spacer(Modifier.width(12.dp))
            Texte(t.garderPreferences)
        }
        BoutonPrincipal(
            t.regenerer,
            onClick = lancer,
            actif = !occupe,
            icone = painterResource(R.drawable.ic_semaine),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PiedDePage(plan: Plan, ui: EtatApp) {
    val t = ui.t
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Separateur(Modifier.padding(bottom = 8.dp))
        Texte(t.prochainRappel(ui.prochainRappel), doux = true, style = P.t.petit)
        Texte(t.prochaineGeneration(ui.prochaineGeneration), doux = true, style = P.t.petit)
        Texte(
            plan.coutUsd?.let { t.coutIA(plan.fournisseur, it * 100, plan.nbPhotos, plan.nbArticlesCirculaire) }
                ?: t.jetonsIA(plan.fournisseur, plan.jetonsEntree, plan.jetonsSortie, plan.nbPhotos, plan.nbArticlesCirculaire),
            doux = true,
            style = P.t.petit
        )
    }
}

/** Dialogue de confirmation aux couleurs de l'app. */
@Composable
fun Dialogue(
    titre: String,
    texte: String,
    confirmer: String,
    annuler: String,
    fermer: () -> Unit,
    valider: () -> Unit
) {
    AlertDialog(
        onDismissRequest = fermer,
        containerColor = P.c.carton,
        shape = RoundedCornerShape(22.dp),
        title = { Text(titre, style = P.t.titreSection, color = P.c.encre) },
        text = { Texte(texte) },
        confirmButton = { BoutonTexte(confirmer, onClick = valider) },
        dismissButton = { BoutonTexte(annuler, onClick = fermer) }
    )
}

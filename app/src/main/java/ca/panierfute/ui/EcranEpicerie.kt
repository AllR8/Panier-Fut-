package ca.panierfute.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ca.panierfute.data.ArticleEpicerie
import ca.panierfute.data.Langue
import ca.panierfute.data.Plan
import ca.panierfute.data.Rayon
import ca.panierfute.i18n.Textes

@Composable
fun EcranEpicerie(ui: EtatApp) {
    val plan = ui.plan
    val t = ui.t
    val langue = ui.langue
    val ctx = LocalContext.current

    if (plan == null || plan.epicerie.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = MARGE, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EnTete(t.titreEpicerie)
            Texte(t.epicerieVide, doux = true)
            BoutonSecondaire(t.allerASemaine, onClick = { ui.onglet = Onglet.SEMAINE })
        }
        return
    }

    val rayons = plan.epicerie.groupBy { it.rayon }.toList().sortedBy { it.first.ordinal }
    val faits = plan.epicerie.count { it.index in ui.coches }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = MARGE, end = MARGE, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { EnTete(t.titreEpicerie, plan.marchand) }
        item { BandeCirculaire(plan.circulaireDebut, plan.circulaireFin, ui.aujourdhui, t) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EtiquetteTotalAvecTrou(plan.totalEstime, langue)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(t.totalTaxesIncluses, style = P.t.corpsFort, color = P.c.encre)
                        Text(t.sousTotal(argent(plan.sousTotal, langue)), style = P.t.petit, color = P.c.encreDouce)
                        Text(
                            t.taxes(argent(plan.taxes, langue), plan.tauxTaxes * 100, plan.nbTaxables),
                            style = P.t.petit,
                            color = P.c.encreDouce
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(t.dansLePanier(faits, plan.epicerie.size), style = P.t.libelle, color = P.c.encre)
                        LinearProgressIndicator(
                            progress = { faits.toFloat() / plan.epicerie.size },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = P.c.action,
                            trackColor = P.c.trait,
                            drawStopIndicator = {}
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    BoutonSecondaire(
                        t.partager,
                        onClick = { partager(ctx, plan, t, langue) },
                        icone = rememberVectorPainter(Icons.Filled.Share)
                    )
                }
                if (faits > 0) BoutonTexte(t.toutDecocher, onClick = { ui.decocherTout() })
            }
        }
        rayons.forEach { (rayon, articles) ->
            item(key = rayon.code) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)) {
                        Text(t.rayon(rayon), style = P.t.titreSection, color = P.c.encre, modifier = Modifier.weight(1f))
                        Text(
                            articles.count { it.index in ui.coches }.toString() + "/" + articles.size,
                            style = P.t.libelle,
                            color = P.c.encreDouce
                        )
                    }
                    Feuille(padding = PaddingValues(0.dp)) {
                        Column {
                            articles.forEachIndexed { i, a ->
                                if (i > 0) Separateur(Modifier.padding(start = 58.dp))
                                LigneArticle(a, a.index in ui.coches, t, langue) { ui.basculer(a.index) }
                            }
                        }
                    }
                }
            }
        }
        item {
            Texte(t.prixEstimesNote, doux = true, style = P.t.petit, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun LigneArticle(a: ArticleEpicerie, coche: Boolean, t: Textes, langue: Langue, basculer: () -> Unit) {
    val c = P.c
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox, onClick = basculer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CaseACocher(coche, onClick = null)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                a.article,
                style = P.t.corpsFort,
                textDecoration = if (coche) TextDecoration.LineThrough else null,
                color = if (coche) c.encreDouce else c.encre
            )
            val details = t.quantitePour(a.quantite, a.pour)
            if (details.isNotBlank()) {
                Text(details, style = P.t.petit, color = c.encreDouce)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val prix = a.cout ?: a.prixUnitaire
            if (prix != null) {
                if (a.enRabais) {
                    Box(Modifier.alpha(if (coche) 0.45f else 1f)) { PrixSpecial(prix, langue) }
                } else {
                    Text(
                        (if (a.prixEstime) "≈ " else "") + argent(prix, langue),
                        style = P.t.corps,
                        color = c.encreDouce
                    )
                }
            } else if (a.enRabais) {
                EtiquetteSpecial(t.special)
            }
            if (a.taxable) Text(t.plusTaxes, style = P.t.petit, fontWeight = FontWeight.Medium, color = c.encreDouce)
        }
    }
}

/** Partage la liste en texte (Samsung Notes, Keep, texto…). */
private fun partager(ctx: Context, plan: Plan, t: Textes, langue: Langue) {
    val texte = buildString {
        append(t.titreEpicerie).append(", ").append(plan.marchand)
        if (plan.circulaireDebut != null && plan.circulaireFin != null) {
            append("\n").append(t.circulairePeriode(plan.circulaireDebut, plan.circulaireFin))
        }
        append("\n")
        plan.epicerie.groupBy { it.rayon }.toList().sortedBy { it.first.ordinal }.forEach { (rayon: Rayon, articles) ->
            append("\n").append(t.rayon(rayon)).append("\n")
            articles.forEach { a ->
                append("[ ] ").append(a.article)
                if (a.quantite.isNotBlank()) append(", ").append(a.quantite)
                val prix = a.cout ?: a.prixUnitaire
                if (prix != null) append(": ").append(if (a.prixEstime) "≈ " else "").append(argent(prix, langue))
                if (a.enRabais) append(" (").append(t.special).append(")")
                if (a.taxable) append(" ").append(t.plusTaxes)
                append("\n")
            }
        }
        append("\n").append(t.sousTotal(argent(plan.sousTotal, langue)))
        append("\n").append(t.taxes(argent(plan.taxes, langue), plan.tauxTaxes * 100, plan.nbTaxables))
        append("\n").append(t.totalTaxesIncluses).append(": ").append(argent(plan.totalEstime, langue))
    }
    val envoi = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texte)
    }
    ctx.startActivity(Intent.createChooser(envoi, t.partagerListe))
}

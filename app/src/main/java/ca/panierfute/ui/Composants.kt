package ca.panierfute.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ca.panierfute.R
import ca.panierfute.data.Endroit
import ca.panierfute.data.Langue
import ca.panierfute.data.Zone
import ca.panierfute.i18n.Textes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToLong

// ---------------------------------------------------------------------------
// Argent
// ---------------------------------------------------------------------------

/** Montant découpé pour l'affichage façon circulaire : dollars, cents, symbole. */
data class PartiesPrix(val entier: String, val cents: String, val symboleAvant: Boolean)

fun partiesPrix(montant: Double, langue: Langue): PartiesPrix {
    val totalCents = (abs(montant) * 100).roundToLong()
    return PartiesPrix(
        entier = (totalCents / 100).toString(),
        cents = (totalCents % 100).toString().padStart(2, '0'),
        // Français : « 12,50 $ ». Anglais et espagnol (Amérique du Nord) : « $12.50 ».
        symboleAvant = langue != Langue.FR
    )
}

/** Montant sur une ligne : « 12,50 $ » ou « $12.50 ». */
fun argent(montant: Double, langue: Langue): String {
    val p = partiesPrix(montant, langue)
    return if (p.symboleAvant) "$${p.entier}.${p.cents}" else "${p.entier},${p.cents} $"
}

/** Prix de circulaire : gros dollars, petits cents surélevés. */
fun prixAffiche(montant: Double, langue: Langue, tailleCents: TextUnit): AnnotatedString {
    val p = partiesPrix(montant, langue)
    val petit = SpanStyle(fontSize = tailleCents, baselineShift = BaselineShift(0.62f))
    return buildAnnotatedString {
        if (p.symboleAvant) withStyle(petit) { append("$") }
        append(p.entier)
        withStyle(petit) { append(if (p.symboleAvant) p.cents else ",${p.cents}") }
        if (!p.symboleAvant) withStyle(petit) { append(" $") }
    }
}

// ---------------------------------------------------------------------------
// Étiquettes
// ---------------------------------------------------------------------------

/** Étiquette suspendue : bout pointu à gauche, comme celles des tablettes. */
val FormeEtiquette = GenericShape { size, _ ->
    val pointe = size.height * 0.32f
    moveTo(pointe, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(pointe, size.height)
    lineTo(0f, size.height / 2f)
    close()
}

/** Le total de l'épicerie, en grande étiquette. */
@Composable
fun EtiquetteTotal(montant: Double, langue: Langue, modifier: Modifier = Modifier) {
    val c = P.c
    Box(
        modifier
            .clip(FormeEtiquette)
            .background(c.action)
            .padding(start = 30.dp, end = 18.dp, top = 8.dp, bottom = 6.dp)
    ) {
        Text(
            prixAffiche(montant, langue, P.t.prixHero.fontSize * 0.45f),
            style = P.t.prixHero,
            color = c.surAction
        )
    }
}

/** Le trou de l'étiquette, posé par-dessus (couleur du fond). */
@Composable
fun EtiquetteTotalAvecTrou(montant: Double, langue: Langue, modifier: Modifier = Modifier) {
    Box(modifier) {
        EtiquetteTotal(montant, langue)
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 11.dp)
                .size(9.dp)
                .background(P.c.papier, CircleShape)
        )
    }
}

/** Petite étiquette jaune : prix en spécial, ou simplement « spécial ». */
@Composable
fun EtiquetteSpecial(texte: AnnotatedString, modifier: Modifier = Modifier, style: TextStyle = P.t.libelle) {
    Box(
        modifier
            .clip(RoundedCornerShape(5.dp))
            .background(P.c.etiquette)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(texte, style = style, color = P.c.surEtiquette)
    }
}

@Composable
fun EtiquetteSpecial(texte: String, modifier: Modifier = Modifier) =
    EtiquetteSpecial(AnnotatedString(texte), modifier)

/** Prix d'un article en spécial, en étiquette jaune avec cents surélevés. */
@Composable
fun PrixSpecial(montant: Double, langue: Langue, modifier: Modifier = Modifier) {
    val style = P.t.titreSection.copy(fontWeight = FontWeight.ExtraBold, fontSize = P.t.titreSection.fontSize)
    EtiquetteSpecial(prixAffiche(montant, langue, style.fontSize * 0.55f), modifier, style)
}

// ---------------------------------------------------------------------------
// Titres et textes
// ---------------------------------------------------------------------------

@Composable
fun EnTete(titre: String, sousTitre: String? = null, fin: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(titre, style = P.t.titreEcran, color = P.c.encre)
            if (!sousTitre.isNullOrBlank()) {
                Text(sousTitre, style = P.t.corps, color = P.c.encreDouce, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (fin != null) fin()
    }
}

@Composable
fun TitreSection(texte: String, modifier: Modifier = Modifier, fin: (@Composable RowScope.() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(texte, style = P.t.titreSection, color = P.c.encre, modifier = Modifier.weight(1f))
        if (fin != null) fin()
    }
}

@Composable
fun Texte(texte: String, modifier: Modifier = Modifier, doux: Boolean = false, style: TextStyle = P.t.corps) {
    Text(texte, style = style, color = if (doux) P.c.encreDouce else P.c.encre, modifier = modifier)
}

@Composable
fun Separateur(modifier: Modifier = Modifier) = HorizontalDivider(modifier, thickness = 1.dp, color = P.c.trait)

/** Feuille blanche sur le fond : regroupe des réglages ou une liste. */
@Composable
fun Feuille(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(16.dp), contenu: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(P.c.carton)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { contenu() }
}

// ---------------------------------------------------------------------------
// Boutons et contrôles
// ---------------------------------------------------------------------------

private val FormeBouton = RoundedCornerShape(14.dp)

@Composable
fun BoutonPrincipal(
    texte: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actif: Boolean = true,
    icone: Painter? = null
) {
    Button(
        onClick = onClick,
        enabled = actif,
        shape = FormeBouton,
        colors = ButtonDefaults.buttonColors(
            containerColor = P.c.action,
            contentColor = P.c.surAction,
            disabledContainerColor = P.c.trait,
            disabledContentColor = P.c.encreDouce
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier.heightIn(min = 52.dp)
    ) {
        if (icone != null) {
            Icon(icone, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texte, style = P.t.bouton)
    }
}

@Composable
fun BoutonSecondaire(
    texte: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actif: Boolean = true,
    icone: Painter? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = actif,
        shape = FormeBouton,
        border = BorderStroke(1.5.dp, if (actif) P.c.encre else P.c.trait),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = P.c.encre, disabledContentColor = P.c.encreDouce),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        if (icone != null) {
            Icon(icone, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texte, style = P.t.bouton)
    }
}

@Composable
fun BoutonTexte(texte: String, onClick: () -> Unit, modifier: Modifier = Modifier, actif: Boolean = true) {
    TextButton(onClick = onClick, enabled = actif, modifier = modifier) {
        Text(texte, style = P.t.bouton, color = if (actif) P.c.encre else P.c.encreDouce)
    }
}

/** Case à cocher carrée, façon liste papier. */
@Composable
fun CaseACocher(coche: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val forme = RoundedCornerShape(6.dp)
    val base = modifier
        .size(26.dp)
        .clip(forme)
        .background(if (coche) P.c.action else P.c.carton)
        .border(2.dp, if (coche) P.c.action else P.c.encreDouce, forme)
    Box(
        if (onClick != null) base.clickable(role = Role.Checkbox, onClick = onClick) else base,
        contentAlignment = Alignment.Center
    ) {
        if (coche) Icon(Icons.Filled.Check, contentDescription = null, tint = P.c.surAction, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun Interrupteur(actif: Boolean, changer: (Boolean) -> Unit) {
    Switch(
        checked = actif,
        onCheckedChange = changer,
        colors = SwitchDefaults.colors(
            checkedTrackColor = P.c.action,
            checkedThumbColor = P.c.surAction,
            checkedBorderColor = P.c.action,
            uncheckedTrackColor = P.c.papier,
            uncheckedThumbColor = P.c.encreDouce,
            uncheckedBorderColor = P.c.encreDouce
        )
    )
}

@Composable
fun LigneInterrupteur(titre: String, detail: String? = null, actif: Boolean, changer: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Switch) { changer(!actif) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
            Text(titre, style = P.t.corpsFort, color = P.c.encre)
            if (detail != null) Text(detail, style = P.t.petit, color = P.c.encreDouce)
        }
        Spacer(Modifier.width(12.dp))
        Interrupteur(actif, changer)
    }
}

/** Choix exclusif sur une rangée (langue, endroit, jour). */
@Composable
fun Segments(options: List<String>, choisi: Int, choisir: (Int) -> Unit, modifier: Modifier = Modifier) {
    val forme = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(forme)
            .border(1.5.dp, P.c.encre, forme)
    ) {
        options.forEachIndexed { i, texte ->
            val actif = i == choisi
            Box(
                Modifier
                    .weight(1f)
                    .background(if (actif) P.c.action else P.c.carton)
                    .clickable(role = Role.RadioButton) { choisir(i) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(texte, style = P.t.bouton, color = if (actif) P.c.surAction else P.c.encre, maxLines = 1)
            }
        }
    }
}

/** Puce sélectionnable (jours, endroits). */
@Composable
fun Puce(texte: String, choisie: Boolean, onClick: () -> Unit) {
    val forme = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .clip(forme)
            .background(if (choisie) P.c.action else P.c.carton)
            .border(1.5.dp, if (choisie) P.c.action else P.c.trait, forme)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(texte, style = P.t.libelle, color = if (choisie) P.c.surAction else P.c.encre)
    }
}

/** Champ de saisie aux couleurs de l'app. */
@Composable
fun Champ(
    valeur: String,
    changer: (String) -> Unit,
    libelle: String,
    modifier: Modifier = Modifier,
    exemple: String? = null,
    uneLigne: Boolean = true,
    lignesMin: Int = 1,
    clavier: KeyboardType = KeyboardType.Text,
    actions: KeyboardActions = KeyboardActions.Default,
    masque: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    fin: (@Composable () -> Unit)? = null,
    aide: String? = null
) {
    OutlinedTextField(
        value = valeur,
        onValueChange = changer,
        label = { Text(libelle) },
        placeholder = exemple?.let { { Text(it, color = P.c.encreDouce) } },
        singleLine = uneLigne,
        minLines = lignesMin,
        keyboardOptions = KeyboardOptions(keyboardType = clavier),
        keyboardActions = actions,
        visualTransformation = masque,
        trailingIcon = fin,
        supportingText = aide?.let { { Text(it) } },
        shape = RoundedCornerShape(12.dp),
        textStyle = P.t.corps.copy(color = P.c.encre),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = P.c.encre,
            unfocusedBorderColor = P.c.trait,
            focusedLabelColor = P.c.encre,
            unfocusedLabelColor = P.c.encreDouce,
            cursorColor = P.c.encre,
            focusedContainerColor = P.c.carton,
            unfocusedContainerColor = P.c.carton
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// ---------------------------------------------------------------------------
// Icônes de l'app
// ---------------------------------------------------------------------------

@Composable
fun iconeZone(zone: Zone): Painter = painterResource(
    when (zone) {
        Zone.GARDE_MANGER -> R.drawable.ic_garde_manger
        Zone.FRIGO -> R.drawable.ic_frigo
        Zone.CONGELO -> R.drawable.ic_congelo
    }
)

@Composable
fun iconeEndroit(endroit: Endroit): Painter = painterResource(
    when (endroit) {
        Endroit.GARDE_MANGER -> R.drawable.ic_garde_manger
        Endroit.FRIGO -> R.drawable.ic_frigo
        Endroit.CONGELATEUR -> R.drawable.ic_congelo
        Endroit.AUTRE -> R.drawable.ic_etiquette
    }
)

// ---------------------------------------------------------------------------
// Logo d'épicerie (téléchargé une fois, sinon l'initiale)
// ---------------------------------------------------------------------------

private object CacheLogos {
    val images = ConcurrentHashMap<String, ImageBitmap>()
}

@Composable
fun LogoEpicerie(url: String, nom: String, taille: Dp = 44.dp) {
    val image by produceState(CacheLogos.images[url], url) {
        if (value == null && url.startsWith("https://")) {
            value = withContext(Dispatchers.IO) {
                try {
                    val octets = URL(url).openStream().use { it.readBytes() }
                    BitmapFactory.decodeByteArray(octets, 0, octets.size)?.asImageBitmap()
                        ?.also { CacheLogos.images[url] = it }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    val forme = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(taille)
            .clip(forme)
            .background(if (image != null) androidx.compose.ui.graphics.Color.White else P.c.papier)
            .border(1.dp, P.c.trait, forme),
        contentAlignment = Alignment.Center
    ) {
        val img = image
        if (img != null) {
            Image(img, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(5.dp))
        } else {
            Text(nom.trim().take(1).uppercase(), style = P.t.titreSection, color = P.c.encre)
        }
    }
}

// ---------------------------------------------------------------------------
// Bande de la circulaire : validité et meilleurs jours pour y aller
// ---------------------------------------------------------------------------

/** Les 3 premiers jours de la circulaire : les spéciaux y sont encore en tablette. */
fun finJoursIdeaux(debut: LocalDate, fin: LocalDate): LocalDate =
    debut.plusDays(2).let { if (it.isAfter(fin)) fin else it }

@Composable
fun BandeCirculaire(
    debut: LocalDate?,
    fin: LocalDate?,
    aujourdhui: LocalDate,
    t: Textes,
    modifier: Modifier = Modifier
) {
    val c = P.c
    if (debut == null || fin == null || fin.isBefore(debut)) {
        if (fin != null) Text(t.circulaireJusquau(fin), style = P.t.corpsFort, color = c.encre, modifier = modifier)
        return
    }
    val nbJours = (ChronoUnit.DAYS.between(debut, fin) + 1).toInt().coerceIn(1, 14)
    val idealFin = finJoursIdeaux(debut, fin)
    val conseil = when {
        aujourdhui.isBefore(debut) -> t.circulaireAVenir(debut, idealFin)
        !aujourdhui.isAfter(idealFin) -> t.circulaireIdeal(idealFin)
        !aujourdhui.isAfter(fin) -> t.circulaireRestant(ChronoUnit.DAYS.between(aujourdhui, fin).toInt() + 1, fin)
        else -> t.circulaireTerminee
    }
    val terminee = aujourdhui.isAfter(fin)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(t.circulairePeriode(debut, fin), style = P.t.titreLigne, color = c.encre)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0 until nbJours) {
                val jour = debut.plusDays(i.toLong())
                val passe = jour.isBefore(aujourdhui)
                val ideal = !jour.isAfter(idealFin)
                val estAujourdhui = jour == aujourdhui
                val forme = RoundedCornerShape(7.dp)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .alpha(if (passe) 0.4f else 1f)
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(forme)
                            .background(if (ideal) c.etiquette else c.carton)
                            .border(if (estAujourdhui) 2.5.dp else 1.dp, if (estAujourdhui) c.encre else c.trait, forme),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            t.initialeJour(jour),
                            style = P.t.libelle,
                            color = if (ideal) c.surEtiquette else c.encre
                        )
                    }
                    Text(
                        if (estAujourdhui) t.aujourdhuiCourt else jour.dayOfMonth.toString(),
                        style = P.t.petit,
                        fontWeight = if (estAujourdhui) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (estAujourdhui) c.encre else c.encreDouce,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            if (!terminee) {
                Box(
                    Modifier
                        .padding(top = 4.dp, end = 8.dp)
                        .size(11.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(c.etiquette)
                )
            }
            Text(conseil, style = P.t.corps, color = if (terminee) c.tomate else c.encre)
        }
    }
}

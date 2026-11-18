package ca.panierfute.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import ca.panierfute.R
import ca.panierfute.data.Photos
import ca.panierfute.data.Zone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun EcranPhotos(ui: EtatApp) {
    val ctx = LocalContext.current
    val t = ui.t

    // Gardés à travers une éventuelle recréation de l'activité pendant que l'appareil photo est ouvert
    var zoneCapture by rememberSaveable { mutableStateOf<String?>(null) }
    var cheminCapture by rememberSaveable { mutableStateOf<String?>(null) }
    var zoneGalerie by rememberSaveable { mutableStateOf<String?>(null) }

    val appareil = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val zone = Zone.depuisCle(zoneCapture)
        val fichier = cheminCapture?.let { File(it) }
        if (ok && zone != null && fichier != null) ui.ajouterCapture(zone, fichier) else fichier?.delete()
        zoneCapture = null
        cheminCapture = null
    }
    val galerie = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(Photos.MAX_PAR_ZONE)
    ) { uris ->
        val zone = Zone.depuisCle(zoneGalerie)
        if (zone != null && uris.isNotEmpty()) ui.ajouterGalerie(zone, uris)
        zoneGalerie = null
    }

    fun prendrePhoto(zone: Zone) {
        val f = Photos.fichierCapture(ctx)
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        zoneCapture = zone.cle
        cheminCapture = f.absolutePath
        try {
            appareil.launch(uri)
        } catch (e: Exception) {
            ui.message = t.aucunAppareilPhoto
        }
    }

    fun choisirGalerie(zone: Zone) {
        zoneGalerie = zone.cle
        try {
            galerie.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            ui.message = t.galerieImpossible
        }
    }

    val total = ui.photos.values.sumOf { it.size }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = MARGE, end = MARGE, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { EnTete(t.titrePhotos, t.photosIntro) }
        items(Zone.entries.toList()) { zone ->
            val liste = ui.photos[zone].orEmpty()
            val plein = liste.size >= Photos.MAX_PAR_ZONE
            Feuille {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(iconeZone(zone), contentDescription = null, tint = P.c.encre, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.zone(zone), style = P.t.titreSection, color = P.c.encre)
                        Text(t.conseilZone(zone), style = P.t.petit, color = P.c.encreDouce)
                    }
                    Text("${liste.size}/${Photos.MAX_PAR_ZONE}", style = P.t.libelle, color = P.c.encreDouce)
                }
                if (liste.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(liste) { f -> Miniature(f, t.supprimerPhoto) { ui.supprimerPhoto(f) } }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BoutonPrincipal(
                        t.prendrePhoto,
                        onClick = { prendrePhoto(zone) },
                        actif = !plein,
                        icone = painterResource(R.drawable.ic_camera),
                        modifier = Modifier.weight(1f)
                    )
                    BoutonSecondaire(
                        t.galerie,
                        onClick = { choisirGalerie(zone) },
                        actif = !plein,
                        icone = rememberVectorPainter(Icons.Filled.Add)
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 6.dp)) {
                Texte(
                    if (total == 0) t.photosAucune else t.photosPretes(total, ui.prochaineGeneration),
                    doux = true
                )
                BoutonSecondaire(
                    t.genererPlan,
                    onClick = { ui.generer(); ui.onglet = Onglet.SEMAINE },
                    actif = !ui.generationEnCours && !ui.attenteReseau,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun Miniature(fichier: File, description: String, supprimer: () -> Unit) {
    val image by produceState<ImageBitmap?>(null, fichier) {
        value = withContext(Dispatchers.IO) { Photos.miniature(fichier)?.asImageBitmap() }
    }
    Box(
        Modifier
            .size(width = 112.dp, height = 84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(P.c.trait)
    ) {
        image?.let {
            Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        IconButton(
            onClick = supprimer,
            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(30.dp)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = description,
                tint = Color.White,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.55f), CircleShape).padding(3.dp)
            )
        }
    }
}

package ca.panierfute.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
fun EcranPhotos(vm: AppViewModel) {
    val ctx = LocalContext.current

    // Gardés à travers une éventuelle recréation de l'activité pendant que l'appareil photo est ouvert
    var zoneCapture by rememberSaveable { mutableStateOf<String?>(null) }
    var cheminCapture by rememberSaveable { mutableStateOf<String?>(null) }
    var zoneGalerie by rememberSaveable { mutableStateOf<String?>(null) }

    val appareil = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val zone = Zone.depuisCle(zoneCapture)
        val fichier = cheminCapture?.let { File(it) }
        if (ok && zone != null && fichier != null) vm.ajouterCapture(zone, fichier) else fichier?.delete()
        zoneCapture = null
        cheminCapture = null
    }
    val galerie = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(Photos.MAX_PAR_ZONE)
    ) { uris ->
        val zone = Zone.depuisCle(zoneGalerie)
        if (zone != null && uris.isNotEmpty()) vm.ajouterGalerie(zone, uris)
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
            vm.message = "Aucune application d'appareil photo trouvée."
        }
    }

    fun choisirGalerie(zone: Zone) {
        zoneGalerie = zone.cle
        try {
            galerie.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            vm.message = "Impossible d'ouvrir la galerie."
        }
    }

    val total = vm.photos.values.sumOf { it.size }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EnTete(
                "Chez nous",
                "Photographie ce que tu as déjà : l'IA en tiendra compte pour éviter d'acheter en double " +
                    "et utiliser ce qui doit être mangé vite."
            )
        }
        items(Zone.entries.toList()) { zone ->
            val liste = vm.photos[zone].orEmpty()
            Section("${zone.emoji}  ${zone.titre}  (${liste.size}/${Photos.MAX_PAR_ZONE})") {
                Text(zone.conseil, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (liste.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(liste) { f -> Miniature(f) { vm.supprimerPhoto(f) } }
                    }
                }
                val plein = liste.size >= Photos.MAX_PAR_ZONE
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { prendrePhoto(zone) }, enabled = !plein) {
                        Icon(painterResource(R.drawable.ic_camera), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Photo")
                    }
                    OutlinedButton(onClick = { choisirGalerie(zone) }, enabled = !plein) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Galerie")
                    }
                }
            }
        }
        item {
            Section("Prêt ?") {
                Text(
                    if (total == 0) {
                        "Aucune photo pour l'instant. Sans photo, le plan se basera seulement sur tes ingrédients de base."
                    } else {
                        "$total photo(s) prête(s). Elles seront analysées à la génération automatique " +
                            "(${vm.prochaineGeneration}), ou tout de suite avec le bouton ci-dessous. " +
                            "Elles sont effacées une fois analysées."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { vm.generer(); vm.onglet = Onglet.SEMAINE },
                    enabled = !vm.generationEnCours && !vm.attenteReseau,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Générer mon plan maintenant")
                }
            }
        }
    }
}

@Composable
private fun Miniature(fichier: File, supprimer: () -> Unit) {
    val image by produceState<ImageBitmap?>(null, fichier) {
        value = withContext(Dispatchers.IO) { Photos.miniature(fichier)?.asImageBitmap() }
    }
    Box(
        Modifier
            .size(104.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val img = image
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        IconButton(
            onClick = supprimer,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Supprimer la photo",
                tint = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .padding(3.dp)
            )
        }
    }
}

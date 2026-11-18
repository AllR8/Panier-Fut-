package ca.panierfute.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.panierfute.R

@Composable
fun EcranPrincipal(ui: EtatApp) {
    val snackbar = remember { SnackbarHostState() }
    val msg = ui.message
    LaunchedEffect(msg) {
        if (msg != null) {
            ui.message = null
            snackbar.showSnackbar(msg)
        }
    }

    Scaffold(
        containerColor = P.c.papier,
        snackbarHost = {
            SnackbarHost(snackbar) { d ->
                Snackbar(
                    d,
                    shape = RoundedCornerShape(12.dp),
                    containerColor = P.c.action,
                    contentColor = P.c.surAction
                )
            }
        },
        bottomBar = { BarreOnglets(ui) }
    ) { marges ->
        Box(Modifier.padding(marges).fillMaxSize()) {
            when (ui.onglet) {
                Onglet.SEMAINE -> EcranSemaine(ui)
                Onglet.EPICERIE -> EcranEpicerie(ui)
                Onglet.PHOTOS -> EcranPhotos(ui)
                Onglet.REGLAGES -> EcranReglages(ui)
            }
        }
    }
}

/** Barre du bas : l'onglet actif est souligné d'un trait épais, comme un onglet de classeur. */
@Composable
private fun BarreOnglets(ui: EtatApp) {
    val c = P.c
    val t = ui.t
    Column(Modifier.background(c.carton).navigationBarsPadding()) {
        Separateur()
        Row(Modifier.fillMaxWidth().height(64.dp)) {
            Onglet.entries.forEach { o ->
                val actif = ui.onglet == o
                val (icone, titre) = when (o) {
                    Onglet.SEMAINE -> R.drawable.ic_semaine to t.ongletSemaine
                    Onglet.EPICERIE -> R.drawable.ic_panier to t.ongletEpicerie
                    Onglet.PHOTOS -> R.drawable.ic_camera to t.ongletPhotos
                    Onglet.REGLAGES -> R.drawable.ic_reglages to t.ongletReglages
                }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .semantics { selected = actif }
                        .clickable(role = Role.Tab) { ui.onglet = o },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .width(36.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(if (actif) c.encre else c.carton)
                    )
                    Spacer(Modifier.height(9.dp))
                    Icon(
                        painterResource(icone),
                        contentDescription = null,
                        tint = if (actif) c.encre else c.encreDouce,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        titre,
                        style = P.t.libelle,
                        fontWeight = if (actif) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (actif) c.encre else c.encreDouce,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}

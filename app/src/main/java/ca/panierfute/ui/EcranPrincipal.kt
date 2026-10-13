package ca.panierfute.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import ca.panierfute.R

@Composable
fun EcranPrincipal(vm: AppViewModel) {
    val snackbar = remember { SnackbarHostState() }
    val msg = vm.message
    LaunchedEffect(msg) {
        if (msg != null) {
            vm.message = null
            snackbar.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                Onglet.entries.forEach { o ->
                    NavigationBarItem(
                        selected = vm.onglet == o,
                        onClick = { vm.onglet = o },
                        icon = {
                            when (o) {
                                Onglet.SEMAINE -> Icon(Icons.Filled.DateRange, contentDescription = null)
                                Onglet.EPICERIE -> Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                                Onglet.PHOTOS -> Icon(painterResource(R.drawable.ic_camera), contentDescription = null)
                                Onglet.REGLAGES -> Icon(Icons.Filled.Settings, contentDescription = null)
                            }
                        },
                        label = { Text(o.titre) }
                    )
                }
            }
        }
    ) { marges ->
        Box(Modifier.padding(marges).fillMaxSize()) {
            when (vm.onglet) {
                Onglet.SEMAINE -> EcranSemaine(vm)
                Onglet.EPICERIE -> EcranEpicerie(vm)
                Onglet.PHOTOS -> EcranPhotos(vm)
                Onglet.REGLAGES -> EcranReglages(vm)
            }
        }
    }
}

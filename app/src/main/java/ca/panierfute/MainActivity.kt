package ca.panierfute

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import ca.panierfute.travail.Notifs
import ca.panierfute.ui.AppViewModel
import ca.panierfute.ui.EcranPrincipal
import ca.panierfute.ui.ThemePanier

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lireOnglet(intent)
        addOnNewIntentListener { nouvelle -> lireOnglet(nouvelle) }

        setContent {
            ThemePanier {
                val demandePermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        demandePermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                EcranPrincipal(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.rafraichir()
    }

    private fun lireOnglet(i: Intent?) {
        i?.getStringExtra(Notifs.EXTRA_ONGLET)?.let { vm.allerA(it) }
    }
}

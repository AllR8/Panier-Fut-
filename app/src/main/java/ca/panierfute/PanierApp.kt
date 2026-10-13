package ca.panierfute

import android.app.Application
import ca.panierfute.travail.Notifs
import ca.panierfute.travail.Planificateur
import kotlin.concurrent.thread

class PanierApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifs.creerCanal(this)
        // Vérifie que le rappel et la génération de la semaine sont bien planifiés.
        thread(name = "planification") {
            try {
                Planificateur.assurer(this, forcer = false)
            } catch (e: Exception) {
                // Ne jamais bloquer le démarrage de l'app pour ça.
            }
        }
    }
}

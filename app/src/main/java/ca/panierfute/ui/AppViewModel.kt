package ca.panierfute.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import ca.panierfute.data.DeepSeek
import ca.panierfute.data.EtatReglages
import ca.panierfute.data.ItemInventaire
import ca.panierfute.data.Photos
import ca.panierfute.data.Plan
import ca.panierfute.data.Reglages
import ca.panierfute.data.SourceCirculaire
import ca.panierfute.data.StockagePlan
import ca.panierfute.data.Zone
import ca.panierfute.travail.GenerationWorker
import ca.panierfute.travail.Planificateur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Onglet(val cle: String, val titre: String) {
    SEMAINE("semaine", "Semaine"),
    EPICERIE("epicerie", "Épicerie"),
    PHOTOS("photos", "Photos"),
    REGLAGES("reglages", "Réglages")
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Application get() = getApplication()
    private val stockage = Reglages(app)

    var onglet by mutableStateOf(Onglet.SEMAINE)
    var plan by mutableStateOf<Plan?>(null)
        private set
    var photos by mutableStateOf<Map<Zone, List<File>>>(emptyMap())
        private set
    var reglages by mutableStateOf(stockage.lireEtat())
        private set
    var coches by mutableStateOf<Set<Int>>(emptySet())
        private set

    var generationEnCours by mutableStateOf(false)

        private set
    var attenteReseau by mutableStateOf(false)
        private set
    var etape by mutableStateOf("")
        private set
    var erreur by mutableStateOf("")
        private set

    var prochainRappel by mutableStateOf("")

        private set
    var prochaineGeneration by mutableStateOf("")
        private set

    var resultatCle by mutableStateOf<String?>(null)

        private set
    var resultatCirculaire by mutableStateOf<String?>(null)
        private set
    var testEnCours by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    init {
        rafraichir()
        viewModelScope.launch {
            WorkManager.getInstance(app).getWorkInfosByTagFlow(Planificateur.TAG_GENERATION).collect { infos ->
                val actif = infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
                val etaitEnCours = generationEnCours
                generationEnCours = actif != null
                etape = actif?.progress?.getString(GenerationWorker.CLE_ETAPE) ?: ""
                attenteReseau = actif == null && infos.any {
                    it.state == WorkInfo.State.ENQUEUED && it.tags.contains(Planificateur.TAG_MANUEL)
                }
                if (etaitEnCours && actif == null) rafraichir()
            }
        }
    }

    fun allerA(cle: String) {
        Onglet.entries.firstOrNull { it.cle == cle }?.let { onglet = it }
    }

    /** Relit le plan, les photos et l'état depuis le stockage. */
    fun rafraichir() {
        viewModelScope.launch {
            val (p, ph) = withContext(Dispatchers.IO) { StockagePlan.charger(ctx) to Photos.parZone(ctx) }
            plan = p
            photos = ph
            coches = stockage.coches
            erreur = stockage.derniereErreur
            majHoraire()
        }
    }

    private fun majHoraire() {
        prochainRappel = Planificateur.formater(Planificateur.prochainRappel(stockage))
        prochaineGeneration = Planificateur.formater(Planificateur.prochaineGeneration(stockage))
    }

    // ---------- Génération ----------

    /**
     * Lance une génération manuelle.
     * [consignes] : précisions pour cette version seulement (ex. « plus végé cette semaine »).
     * [garder] : les ajoute aussi aux préférences permanentes.
     */
    fun generer(consignes: String = "", garder: Boolean = false) {
        if (reglages.cleApi.isBlank()) {
            message = "Ajoute d'abord ta clé API DeepSeek."
            onglet = Onglet.REGLAGES
            return
        }
        val texte = consignes.trim()
        if (garder && texte.isNotEmpty()) {
            maj { e ->
                val avant = e.preferences.trim()
                e.copy(preferences = if (avant.isEmpty()) texte else "$avant\n$texte")
            }
        }
        stockage.derniereErreur = ""
        erreur = ""
        Planificateur.lancerManuel(ctx, texte)
        message = "Génération lancée. Tu peux fermer l'app : tu recevras une notification."
    }

    // ---------- Inventaire (« ce que l'IA a vu chez toi ») ----------

    private fun modifierInventaire(transformation: (List<ItemInventaire>) -> List<ItemInventaire>) {
        val actuel = plan?.inventaire ?: return
        val nouveau = transformation(actuel)
        viewModelScope.launch {
            plan = withContext(Dispatchers.IO) {
                StockagePlan.majInventaire(ctx, nouveau)
                StockagePlan.charger(ctx)
            }
        }
    }

    fun ajouterAliment(item: ItemInventaire) = modifierInventaire { it + item }

    fun remplacerAliment(index: Int, item: ItemInventaire) = modifierInventaire { liste ->
        liste.mapIndexed { i, ancien -> if (i == index) item else ancien }
    }

    fun retirerAliment(index: Int) = modifierInventaire { liste ->
        liste.filterIndexed { i, _ -> i != index }
    }

    // ---------- Épicerie ----------

    fun basculer(index: Int) {
        val n = if (index in coches) coches - index else coches + index
        coches = n
        stockage.coches = n
    }

    fun decocherTout() {
        coches = emptySet()
        stockage.coches = emptySet()
    }

    // ---------- Photos ----------

    fun ajouterCapture(zone: Zone, fichier: File) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { Photos.ajouterCapture(ctx, zone, fichier) }
            if (!ok) message = "La photo n'a pas pu être enregistrée."
            photos = withContext(Dispatchers.IO) { Photos.parZone(ctx) }
        }
    }

    fun ajouterGalerie(zone: Zone, uris: List<Uri>) {
        viewModelScope.launch {
            val n = withContext(Dispatchers.IO) { Photos.ajouterDepuisGalerie(ctx, zone, uris) }
            if (n < uris.size) message = "$n photo(s) ajoutée(s) (maximum ${Photos.MAX_PAR_ZONE} par zone)."
            photos = withContext(Dispatchers.IO) { Photos.parZone(ctx) }
        }
    }

    fun supprimerPhoto(fichier: File) {
        viewModelScope.launch {
            photos = withContext(Dispatchers.IO) {
                Photos.supprimer(fichier)
                Photos.parZone(ctx)
            }
        }
    }

    // ---------- Réglages ----------

    fun maj(transformation: (EtatReglages) -> EtatReglages) {
        val avant = reglages
        val apres = transformation(avant)
        if (apres == avant) return
        reglages = apres
        stockage.ecrire(apres)
        if (!apres.memeHoraire(avant)) {
            majHoraire()
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    Planificateur.assurer(ctx, forcer = true)
                } catch (e: Exception) {
                    // sans conséquence : réessayé au prochain démarrage
                }
            }
        }
    }

    fun testerCle() {
        val cle = reglages.cleApi
        if (cle.isBlank()) {
            resultatCle = "Entre d'abord ta clé."
            return
        }
        testEnCours = true
        resultatCle = "Vérification…"
        viewModelScope.launch {
            resultatCle = try {
                "✓ " + withContext(Dispatchers.IO) { DeepSeek.solde(cle) }
            } catch (e: Exception) {
                "✗ " + (e.message ?: "Erreur inconnue")
            }
            testEnCours = false
        }
    }

    fun testerCirculaire() {
        testEnCours = true
        resultatCirculaire = "Lecture de la circulaire…"
        val r = reglages
        viewModelScope.launch {
            resultatCirculaire = try {
                val c = withContext(Dispatchers.IO) { SourceCirculaire.charger(r.codePostal, r.marchand) }
                val exemples = c.articles.take(4).joinToString("\n") { a ->
                    "• " + a.nom + (a.prix?.let { " — $it \$" } ?: "")
                }
                val periode = if (c.debut != null && c.fin != null) "du ${c.debut} au ${c.fin}" else ""
                "✓ ${c.articles.size} articles $periode\n$exemples"
            } catch (e: Exception) {
                "✗ " + (e.message ?: "Erreur inconnue")
            }
            testEnCours = false
        }
    }
}

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
import ca.panierfute.data.ClientIA
import ca.panierfute.data.Fournisseur
import ca.panierfute.data.Epicerie
import ca.panierfute.data.EtatReglages
import ca.panierfute.data.ItemInventaire
import ca.panierfute.data.Langue
import ca.panierfute.data.Photos
import ca.panierfute.data.Plan
import ca.panierfute.data.Region
import ca.panierfute.data.Reglages
import ca.panierfute.data.SourceCirculaire
import ca.panierfute.data.StockagePlan
import ca.panierfute.data.Zone
import ca.panierfute.data.ingredientsBaseDefaut
import ca.panierfute.i18n.Textes
import ca.panierfute.i18n.textes
import ca.panierfute.travail.GenerationWorker
import ca.panierfute.travail.Notifs
import ca.panierfute.travail.Planificateur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

class AppViewModel(app: Application) : AndroidViewModel(app), EtatApp {
    private val ctx: Application get() = getApplication()
    private val stockage = Reglages(app)

    override var onglet by mutableStateOf(Onglet.SEMAINE)
    override var message by mutableStateOf<String?>(null)

    override var reglages by mutableStateOf(stockage.lireEtat())
        private set
    override val langue: Langue get() = reglages.langue
    override val t: Textes get() = textes(reglages.langue)
    override val aujourdhui: LocalDate get() = LocalDate.now()

    override var plan by mutableStateOf<Plan?>(null)
        private set
    override var photos by mutableStateOf<Map<Zone, List<File>>>(emptyMap())
        private set
    override var coches by mutableStateOf<Set<Int>>(emptySet())
        private set

    override var generationEnCours by mutableStateOf(false)
        private set
    override var attenteReseau by mutableStateOf(false)
        private set
    override var etape by mutableStateOf("")
        private set
    override var erreur by mutableStateOf("")
        private set
    override var prochainRappel by mutableStateOf("")
        private set
    override var prochaineGeneration by mutableStateOf("")
        private set

    override var resultatCle by mutableStateOf<String?>(null)
        private set
    override var testEnCours by mutableStateOf(false)
        private set

    override var epiceries by mutableStateOf<List<Epicerie>>(emptyList())
        private set
    override var rechercheEpiceries by mutableStateOf(false)
        private set
    override var erreurEpiceries by mutableStateOf<String?>(null)
        private set
    override var toutesEpiceries by mutableStateOf(false)
        private set

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

    override fun allerA(cle: String) {
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
        prochainRappel = Planificateur.formater(Planificateur.prochainRappel(stockage), t)
        prochaineGeneration = Planificateur.formater(Planificateur.prochaineGeneration(stockage), t)
    }

    // ---------- Génération ----------

    /**
     * Lance une génération manuelle.
     * [consignes] : précisions pour cette version seulement (ex. « plus végé cette semaine »).
     * [garder] : les ajoute aussi aux préférences permanentes.
     */
    override fun generer(consignes: String, garder: Boolean) {
        if (!reglages.epicerieChoisie) {
            message = t.epicerieDAbord
            onglet = Onglet.REGLAGES
            return
        }
        if (!reglages.iaConfiguree) {
            message = t.cleDAbord
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
        message = t.generationLancee
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

    override fun ajouterAliment(item: ItemInventaire) = modifierInventaire { it + item }

    override fun remplacerAliment(index: Int, item: ItemInventaire) = modifierInventaire { liste ->
        liste.mapIndexed { i, ancien -> if (i == index) item else ancien }
    }

    override fun retirerAliment(index: Int) = modifierInventaire { liste ->
        liste.filterIndexed { i, _ -> i != index }
    }

    // ---------- Épicerie ----------

    override fun basculer(index: Int) {
        val n = if (index in coches) coches - index else coches + index
        coches = n
        stockage.coches = n
    }

    override fun decocherTout() {
        coches = emptySet()
        stockage.coches = emptySet()
    }

    // ---------- Photos ----------

    override fun ajouterCapture(zone: Zone, fichier: File) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { Photos.ajouterCapture(ctx, zone, fichier) }
            if (!ok) message = t.photoNonEnregistree
            photos = withContext(Dispatchers.IO) { Photos.parZone(ctx) }
        }
    }

    override fun ajouterGalerie(zone: Zone, uris: List<Uri>) {
        viewModelScope.launch {
            val n = withContext(Dispatchers.IO) { Photos.ajouterDepuisGalerie(ctx, zone, uris) }
            if (n < uris.size) message = t.photosAjoutees(n, Photos.MAX_PAR_ZONE)
            photos = withContext(Dispatchers.IO) { Photos.parZone(ctx) }
        }
    }

    override fun supprimerPhoto(fichier: File) {
        viewModelScope.launch {
            photos = withContext(Dispatchers.IO) {
                Photos.supprimer(fichier)
                Photos.parZone(ctx)
            }
        }
    }

    // ---------- Réglages ----------

    override fun maj(transformation: (EtatReglages) -> EtatReglages) {
        val avant = reglages
        val apres = transformation(avant)
        if (apres == avant) return
        reglages = apres
        stockage.ecrire(apres)
        if (!apres.memeHoraire(avant) || apres.langue != avant.langue) majHoraire()
        if (!apres.memeHoraire(avant)) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    Planificateur.assurer(ctx, forcer = true)
                } catch (e: Exception) {
                    // sans conséquence : réessayé au prochain démarrage
                }
            }
        }
    }

    override fun changerLangue(l: Langue) {
        val ancienne = reglages.langue
        if (l == ancienne) return
        maj { e ->
            // La liste d'ingrédients de base suit la langue tant qu'elle n'a pas été personnalisée.
            val base = if (e.ingredientsBase == ingredientsBaseDefaut(ancienne)) ingredientsBaseDefaut(l) else e.ingredientsBase
            e.copy(langue = l, ingredientsBase = base)
        }
        resultatCle = null
        Notifs.creerCanal(ctx)
        // Les épiceries sont relues : Flipp renvoie des circulaires dans la langue demandée.
        if (epiceries.isNotEmpty()) chercherEpiceries(reglages.codePostal)
    }

    override fun testerCle() {
        val config = reglages.configIA()
        if (!reglages.iaConfiguree) {
            resultatCle = t.entrerCleDAbord
            return
        }
        val tx = t
        testEnCours = true
        resultatCle = tx.verification
        viewModelScope.launch {
            resultatCle = try {
                withContext(Dispatchers.IO) { ClientIA.tester(config, tx) }
            } catch (e: Exception) {
                e.message ?: tx.erreurInconnue
            }
            testEnCours = false
        }
    }

    override fun changerFournisseur(f: Fournisseur) {
        maj { it.copy(fournisseur = f) }
        resultatCle = null
    }

    // ---------- Épicerie choisie ----------

    override fun chercherEpiceries(codePostal: String) {
        val tx = t
        val cp = Region.normaliserCodePostal(codePostal)
        if (!Region.codePostalValide(cp)) {
            erreurEpiceries = tx.erreurCodePostal
            return
        }
        if (cp != reglages.codePostal) {
            // Nouveau code postal : l'ancienne épicerie n'est peut-être plus dans le secteur.
            maj { it.copy(codePostal = cp, tauxTaxesPerso = -1.0) }
        }
        rechercheEpiceries = true
        erreurEpiceries = null
        val langueRecherche = reglages.langue
        viewModelScope.launch {
            try {
                val liste = withContext(Dispatchers.IO) { SourceCirculaire.listerEpiceries(cp, langueRecherche) }
                epiceries = liste
                if (liste.isEmpty()) erreurEpiceries = tx.aucuneEpicerieTrouvee
                // L'épicerie choisie ne publie plus de circulaire ici : on la garde, mais on le signale.
                val choisie = reglages
                if (choisie.epicerieChoisie && liste.isNotEmpty() &&
                    liste.none { it.id == choisie.marchandId || it.nom.equals(choisie.marchand, ignoreCase = true) }
                ) {
                    erreurEpiceries = tx.epicerieAbsente(choisie.marchand)
                }
                // Met à jour le jour de début de circulaire de l'épicerie choisie.
                liste.firstOrNull { it.id == choisie.marchandId && it.id > 0 }?.debut?.let { d ->
                    maj { it.copy(jourDebutCirculaire = d.dayOfWeek.value) }
                }
            } catch (e: Exception) {
                erreurEpiceries = tx.erreurRechercheEpiceries
            }
            rechercheEpiceries = false
        }
    }

    override fun choisirEpicerie(e: Epicerie) {
        maj {
            it.copy(
                marchand = e.nom,
                marchandId = e.id,
                marchandLogo = e.logo,
                jourDebutCirculaire = e.debut?.dayOfWeek?.value ?: 0
            )
        }
        erreurEpiceries = null
        message = t.epicerieChoisieMessage(e.nom)
    }

    override fun afficherToutesEpiceries(v: Boolean) {
        toutesEpiceries = v
    }

    /** Rappel la veille du début de la circulaire (19 h) et génération à 00 h 30 le premier jour. */
    override fun alignerSurCirculaire() {
        val debut = reglages.jourDebutCirculaire
        if (debut !in 1..7) return
        val veille = DayOfWeek.of(debut).minus(1).value
        maj { it.copy(jourRappel = veille, heureRappel = 19, minuteRappel = 0, heureGeneration = 0, minuteGeneration = 30) }
        message = t.horaireAligne(DayOfWeek.of(debut))
    }
}

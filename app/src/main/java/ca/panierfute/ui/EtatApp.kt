package ca.panierfute.ui

import android.net.Uri
import ca.panierfute.data.Epicerie
import ca.panierfute.data.EtatReglages
import ca.panierfute.data.Fournisseur
import ca.panierfute.data.ItemInventaire
import ca.panierfute.data.Langue
import ca.panierfute.data.Plan
import ca.panierfute.data.Zone
import ca.panierfute.i18n.Textes
import java.io.File
import java.time.LocalDate

enum class Onglet(val cle: String) { SEMAINE("semaine"), EPICERIE("epicerie"), PHOTOS("photos"), REGLAGES("reglages") }

/**
 * Tout ce que les écrans lisent et peuvent faire.
 * L'app utilise AppViewModel ; les captures d'écran automatiques utilisent une version de démonstration.
 */
interface EtatApp {
    var onglet: Onglet
    var message: String?

    val langue: Langue
    val t: Textes
    val aujourdhui: LocalDate

    val plan: Plan?
    val photos: Map<Zone, List<File>>
    val reglages: EtatReglages
    val coches: Set<Int>

    val generationEnCours: Boolean
    val attenteReseau: Boolean
    val etape: String
    val erreur: String
    val prochainRappel: String
    val prochaineGeneration: String

    val resultatCle: String?
    val testEnCours: Boolean

    val epiceries: List<Epicerie>
    val rechercheEpiceries: Boolean
    val erreurEpiceries: String?
    val toutesEpiceries: Boolean

    fun allerA(cle: String)
    fun generer(consignes: String = "", garder: Boolean = false)

    fun ajouterAliment(item: ItemInventaire)
    fun remplacerAliment(index: Int, item: ItemInventaire)
    fun retirerAliment(index: Int)

    fun basculer(index: Int)
    fun decocherTout()

    fun ajouterCapture(zone: Zone, fichier: File)
    fun ajouterGalerie(zone: Zone, uris: List<Uri>)
    fun supprimerPhoto(fichier: File)

    fun maj(transformation: (EtatReglages) -> EtatReglages)
    fun changerLangue(l: Langue)
    fun testerCle()
    fun changerFournisseur(f: Fournisseur)

    fun chercherEpiceries(codePostal: String)
    fun choisirEpicerie(e: Epicerie)
    fun afficherToutesEpiceries(v: Boolean)
    fun alignerSurCirculaire()
}

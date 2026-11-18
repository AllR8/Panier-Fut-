package ca.panierfute.i18n

import ca.panierfute.data.Endroit
import ca.panierfute.data.Langue
import ca.panierfute.data.Moment
import ca.panierfute.data.Plan
import ca.panierfute.data.Rayon
import ca.panierfute.data.Zone
import ca.panierfute.ui.argent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Textes de l'app dans la langue choisie (français, anglais ou espagnol). */
fun textes(langue: Langue): Textes = when (langue) {
    Langue.FR -> TextesFr
    Langue.EN -> TextesEn
    Langue.ES -> TextesEs
}

/**
 * Tous les textes visibles. Chaque langue implémente chaque entrée :
 * un oubli empêche la compilation.
 */
abstract class Textes(val langue: Langue) {

    // ----- Outils communs -----
    protected fun d(date: LocalDate, motif: String): String =
        date.format(DateTimeFormatter.ofPattern(motif, langue.locale))

    protected fun arg(montant: Double): String = argent(montant, langue)

    /** Pourcentage lisible : « 14,975 % » ou « 14.975% ». */
    fun pourcent(v: Double): String {
        val brut = String.format(Locale.ROOT, "%.3f", v).trimEnd('0').trimEnd('.')
        return if (langue == Langue.EN) "$brut%" else brut.replace('.', ',') + " %"
    }

    protected fun nomJour(j: DayOfWeek): String =
        j.getDisplayName(java.time.format.TextStyle.FULL, langue.locale)

    // ----- Dates -----
    abstract fun dateHeure(z: ZonedDateTime): String
    abstract fun heure(h: Int, m: Int): String
    abstract fun jourCourt(date: LocalDate): String
    abstract fun jourAbrege(j: DayOfWeek): String
    abstract fun initialeJour(date: LocalDate): String
    abstract val aujourdhuiCourt: String

    // ----- Onglets -----
    abstract val ongletSemaine: String
    abstract val ongletEpicerie: String
    abstract val ongletPhotos: String
    abstract val ongletReglages: String

    // ----- Communs -----
    abstract val ajouter: String
    abstract val retirer: String
    abstract val modifier: String
    abstract val enregistrer: String
    abstract val annuler: String
    abstract val masquer: String
    abstract val special: String
    abstract val genererPlan: String
    abstract fun prochainRappel(quand: String): String
    abstract fun prochaineGeneration(quand: String): String

    // ----- Circulaire -----
    abstract fun circulairePeriode(debut: LocalDate, fin: LocalDate): String
    abstract fun circulaireCourte(debut: LocalDate, fin: LocalDate): String
    abstract fun circulaireJusquau(fin: LocalDate): String
    abstract fun circulaireAVenir(debut: LocalDate, idealFin: LocalDate): String
    abstract fun circulaireIdeal(idealFin: LocalDate): String
    abstract fun circulaireRestant(jours: Int, fin: LocalDate): String
    abstract val circulaireTerminee: String

    // ----- Semaine -----
    abstract val titreSemaine: String
    abstract fun epicerieEtCodePostal(marchand: String, codePostal: String): String
    abstract val attenteReseau: String
    abstract val generationEchouee: String
    abstract fun versionDemandee(consignes: String): String
    abstract val titreRepas: String
    abstract val titreRemarques: String
    abstract val preparationPlan: String
    abstract val fermerAppPossible: String
    abstract val accueilIntro: String
    abstract val etapeEpicerie: String
    abstract val etapeCle: String
    abstract val etapePhotos: String
    abstract fun nbPhotos(n: Int): String
    abstract fun horaireResume(rappel: String, generation: String): String
    abstract val epicerieEstimee: String
    abstract fun articlesResume(n: Int, enSpecial: Int): String
    abstract fun economies(montant: String): String
    abstract fun moment(m: Moment): String
    fun momentEtDuree(m: Moment, minutes: Int): String = if (minutes > 0) "${moment(m)}, $minutes min" else moment(m)
    abstract val voirRecette: String
    abstract val masquerRecette: String
    abstract fun ingredientsPour(portions: Int): String
    abstract val sourceRabais: String
    abstract val sourceStock: String
    abstract val sourceBase: String
    abstract val sourceAchat: String
    abstract val preparation: String
    abstract fun titreInventaire(n: Int): String
    abstract val inventaireAide: String
    abstract val inventaireCorrige: String
    abstract val voirEtCorriger: String
    abstract val inventaireVide: String
    abstract fun endroit(e: Endroit): String
    abstract val utiliserVite: String
    abstract val ajouterAliment: String
    abstract val modifierAliment: String
    abstract val aliment: String
    abstract val quantiteFacultative: String
    abstract val exempleQuantite: String
    abstract val titreRegenerer: String
    abstract val regenererAide: String
    abstract val nouvellesPreferences: String
    abstract val exempleConsignes: String
    abstract val garderPreferences: String
    abstract val regenerer: String
    abstract val regenererTitre: String
    abstract fun regenererTexte(nouvellesPhotos: Int): String
    abstract fun coutIA(fournisseur: String, cents: Double, photos: Int, articles: Int): String
    abstract fun jetonsIA(fournisseur: String, entree: Int, sortie: Int, photos: Int, articles: Int): String

    // ----- Épicerie -----
    abstract val titreEpicerie: String
    abstract val epicerieVide: String
    abstract val allerASemaine: String
    abstract val totalTaxesIncluses: String
    abstract fun sousTotal(montant: String): String
    abstract fun taxes(montant: String, pourcentage: Double, nbTaxables: Int): String
    abstract fun dansLePanier(faits: Int, total: Int): String
    abstract val partager: String
    abstract val partagerListe: String
    abstract val toutDecocher: String
    abstract fun rayon(r: Rayon): String
    abstract val plusTaxes: String
    abstract val prixEstimesNote: String

    // ----- Photos -----
    abstract val titrePhotos: String
    abstract val photosIntro: String
    abstract fun zone(z: Zone): String
    abstract fun conseilZone(z: Zone): String
    abstract val prendrePhoto: String
    abstract val galerie: String
    abstract val supprimerPhoto: String
    abstract val photosAucune: String
    abstract fun photosPretes(n: Int, quand: String): String
    abstract val aucunAppareilPhoto: String
    abstract val galerieImpossible: String

    // ----- Réglages -----
    abstract val titreReglages: String
    abstract val reglagesIntro: String
    abstract val langueTitre: String
    abstract val epicerieTitre: String
    abstract val epicerieAide: String
    abstract val codePostal: String
    abstract val exempleCodePostal: String
    abstract val chercher: String
    abstract val rechercheEnCours: String
    abstract fun epiceriesTrouvees(n: Int): String
    abstract val choisie: String
    abstract val afficherToutesEpiceries: String
    abstract val afficherToutesAide: String
    abstract val taxesTitre: String
    abstract fun taxesAide(pourcentage: String, region: String): String
    abstract val tauxTaxes: String
    abstract fun circulaireChangeLe(jour: DayOfWeek): String
    abstract val alignerAide: String
    abstract val alignerHoraire: String
    abstract val horaireDejaAligne: String
    abstract val iaTitre: String
    abstract val iaAide: String
    abstract val fournisseurAutre: String
    abstract val urlApi: String
    abstract val urlApiAide: String
    abstract fun cleApi(fournisseur: String): String
    abstract fun ouTrouverCle(site: String): String
    abstract val voir: String
    abstract val cacher: String
    abstract val modele: String
    abstract val modeleAide: String
    abstract val modeReflexion: String
    abstract val modeReflexionAide: String
    abstract val tester: String
    abstract val repasTitre: String
    abstract val portions: String
    abstract val soupers: String
    abstract val diners: String
    abstract val dinersAide: String
    abstract val dejeuners: String
    abstract val preferences: String
    abstract val exemplePreferences: String
    abstract fun ingredientsBaseTitre(n: Int): String
    abstract val ingredientsBaseAide: String
    abstract val ajouterIngredients: String
    abstract val retablirListe: String
    abstract val horaireTitre: String
    abstract val rappelPhotos: String
    abstract fun heureRappel(h: String): String
    abstract val generationAuto: String
    abstract fun heureGeneration(h: String): String
    abstract val genererSansPhotos: String
    abstract val genererSansPhotosAide: String
    abstract val fiabiliteTitre: String
    abstract val fiabiliteTexte: String
    abstract val ouvrirReglagesBatterie: String

    // ----- Messages -----
    abstract val epicerieDAbord: String
    abstract val cleDAbord: String
    abstract val generationLancee: String
    abstract val photoNonEnregistree: String
    abstract fun photosAjoutees(n: Int, max: Int): String
    abstract val entrerCleDAbord: String
    abstract val verification: String
    abstract fun testReussi(modele: String, solde: String?): String
    abstract val erreurInconnue: String
    abstract val erreurCodePostal: String
    abstract val aucuneEpicerieTrouvee: String
    abstract fun epicerieAbsente(nom: String): String
    abstract val erreurRechercheEpiceries: String
    abstract fun epicerieChoisieMessage(nom: String): String
    abstract fun horaireAligne(jour: DayOfWeek): String

    // ----- Génération -----
    abstract val aucunePhotoSemaine: String
    abstract fun progresCirculaire(marchand: String): String
    abstract val progresDemande: String
    abstract fun progresPhotos(n: Int): String
    abstract val progresIA: String
    abstract val progresNouvelEssai: String
    abstract val progresNouvelEssaiLong: String
    abstract val progresEnregistrement: String

    // ----- Erreurs -----
    abstract val erreurAucuneEpicerie: String
    abstract fun erreurCirculaireIntrouvable(marchand: String, codePostal: String): String
    abstract fun erreurCirculaireVide(marchand: String): String
    abstract val erreurCleManquante: String
    abstract val erreurUrlManquante: String
    abstract val erreurIASansReponse: String
    abstract val erreurEnregistrement: String
    abstract fun erreurIA401(nom: String): String
    abstract fun erreurIA402(nom: String): String
    abstract fun erreurIA404(nom: String, modele: String): String
    abstract fun erreurIA429(nom: String): String
    abstract fun erreurIAServeur(nom: String, code: Int): String
    abstract fun erreurIAAutre(nom: String, code: Int, detail: String): String
    abstract fun diagnosticCoupee(jetons: Int): String
    abstract fun diagnosticIllisible(caracteres: Int): String
    abstract fun diagnosticReflexion(jetons: Int): String
    abstract val diagnosticEspaces: String
    abstract fun erreurPlanIncomplet(essais: Int, details: String): String

    // ----- Notifications -----
    abstract val notifCanalNom: String
    abstract val notifCanalDescription: String
    abstract val notifRappelTitre: String
    abstract fun notifRappelTexte(dejaPrises: Int, quand: String): String
    abstract val notifPlanTitre: String
    abstract fun notifPlanTexte(plan: Plan): String
    abstract val notifErreurTitre: String
    abstract val notifOuvrirPourPhotos: String
}

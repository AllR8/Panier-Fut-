package ca.panierfute.i18n

import ca.panierfute.data.Endroit
import ca.panierfute.data.Langue
import ca.panierfute.data.Moment
import ca.panierfute.data.Plan
import ca.panierfute.data.Rayon
import ca.panierfute.data.Zone
import ca.panierfute.ui.finJoursIdeaux
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Français du Québec, au tutoiement. */
object TextesFr : Textes(Langue.FR) {
    private fun pl(n: Int, un: String, plusieurs: String) = if (n <= 1) un else plusieurs

    /** « jeudi 19 novembre » */
    private fun jourEtDate(date: LocalDate) = d(date, "EEEE d MMMM")
    private fun jourEtDateCourt(date: LocalDate) = d(date, "EEEE d MMM")

    // ----- Dates -----
    override fun dateHeure(z: ZonedDateTime) =
        z.format(DateTimeFormatter.ofPattern("EEEE d MMM 'à' H 'h' mm", langue.locale))
    override fun heure(h: Int, m: Int) = "$h h " + m.toString().padStart(2, '0')
    override fun jourCourt(date: LocalDate) = d(date, "EEE")
    override fun jourAbrege(j: DayOfWeek) = when (j) {
        DayOfWeek.MONDAY -> "Lun"; DayOfWeek.TUESDAY -> "Mar"; DayOfWeek.WEDNESDAY -> "Mer"
        DayOfWeek.THURSDAY -> "Jeu"; DayOfWeek.FRIDAY -> "Ven"; DayOfWeek.SATURDAY -> "Sam"
        DayOfWeek.SUNDAY -> "Dim"
    }
    override fun initialeJour(date: LocalDate) = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "L"; DayOfWeek.TUESDAY -> "M"; DayOfWeek.WEDNESDAY -> "M"
        DayOfWeek.THURSDAY -> "J"; DayOfWeek.FRIDAY -> "V"; DayOfWeek.SATURDAY -> "S"
        DayOfWeek.SUNDAY -> "D"
    }
    override val aujourdhuiCourt = "auj."

    // ----- Onglets -----
    override val ongletSemaine = "Semaine"
    override val ongletEpicerie = "Épicerie"
    override val ongletPhotos = "Photos"
    override val ongletReglages = "Réglages"

    // ----- Communs -----
    override val ajouter = "Ajouter"
    override val retirer = "Retirer"
    override val modifier = "Modifier"
    override val enregistrer = "Enregistrer"
    override val annuler = "Annuler"
    override val masquer = "Masquer"
    override val special = "Spécial"
    override val genererPlan = "Générer mon plan maintenant"
    override fun prochainRappel(quand: String) = "Prochain rappel : $quand"
    override fun prochaineGeneration(quand: String) = "Prochaine génération : $quand"

    // ----- Circulaire -----
    override fun circulairePeriode(debut: LocalDate, fin: LocalDate) =
        if (debut.month == fin.month) "Spéciaux du ${d(debut, "EEEE d")} au ${jourEtDate(fin)}"
        else "Spéciaux du ${jourEtDate(debut)} au ${jourEtDate(fin)}"
    override fun circulaireCourte(debut: LocalDate, fin: LocalDate) =
        if (debut.month == fin.month) "Circulaire du ${debut.dayOfMonth} au ${d(fin, "d MMM")}"
        else "Circulaire du ${d(debut, "d MMM")} au ${d(fin, "d MMM")}"
    override fun circulaireJusquau(fin: LocalDate) = "Spéciaux valides jusqu'au ${jourEtDate(fin)}"
    override fun circulaireAVenir(debut: LocalDate, idealFin: LocalDate) =
        "Les spéciaux commencent ${jourEtDate(debut)}. Vas-y idéalement d'ici ${jourEtDate(idealFin)}, avant que les produits en spécial partent."
    override fun circulaireIdeal(idealFin: LocalDate) =
        "Vas-y idéalement d'ici ${jourEtDate(idealFin)} : les produits en spécial partent vite les premiers jours."
    override fun circulaireRestant(jours: Int, fin: LocalDate) =
        if (jours <= 1) "Dernier jour des spéciaux. Vas-y dès que possible."
        else "Encore $jours jours de spéciaux, jusqu'au ${jourEtDate(fin)}. Vas-y dès que possible : certains produits sont peut-être déjà partis."
    override val circulaireTerminee = "Cette circulaire est terminée. Régénère le plan pour profiter de la nouvelle."

    // ----- Semaine -----
    override val titreSemaine = "Ta semaine"
    override fun epicerieEtCodePostal(marchand: String, codePostal: String) = "$marchand, $codePostal"
    override val attenteReseau = "En attente d'une connexion Internet"
    override val generationEchouee = "La dernière génération a échoué"
    override fun versionDemandee(consignes: String) = "Version demandée : « $consignes »"
    override val titreRepas = "Au menu"
    override val titreRemarques = "À savoir"
    override val preparationPlan = "Préparation de ton plan"
    override val fermerAppPossible = "Tu peux fermer l'app : une notification t'avertira."
    override val accueilIntro =
        "Chaque semaine, Panier Futé lit la circulaire de ton épicerie, regarde ce que tu as déjà et te propose les repas et la liste d'épicerie qui profitent des spéciaux."
    override val etapeEpicerie = "Choisis ton épicerie"
    override val etapeCle = "Configure l'intelligence artificielle"
    override val etapePhotos = "Photographie ton garde-manger, ton frigo et ton congélateur"
    override fun nbPhotos(n: Int) = "$n " + pl(n, "photo", "photos")
    override fun horaireResume(rappel: String, generation: String) =
        "Prochain rappel : $rappel. Génération automatique : $generation."
    override val epicerieEstimee = "Épicerie estimée, taxes incluses"
    override fun articlesResume(n: Int, enSpecial: Int) =
        "$n " + pl(n, "article", "articles") + ", dont $enSpecial en spécial"
    override fun economies(montant: String) = "Économies grâce aux spéciaux : $montant"
    override fun moment(m: Moment) = when (m) {
        Moment.DEJEUNER -> "Déjeuner"
        Moment.DINER -> "Dîner"
        Moment.SOUPER -> "Souper"
    }
    override val voirRecette = "Voir la recette"
    override val masquerRecette = "Masquer la recette"
    override fun ingredientsPour(portions: Int) =
        if (portions > 0) "Ingrédients pour $portions " + pl(portions, "portion", "portions") else "Ingrédients"
    override val sourceRabais = "spécial"
    override val sourceStock = "en stock"
    override val sourceBase = "de base"
    override val sourceAchat = "à acheter"
    override val preparation = "Préparation"
    override fun titreInventaire(n: Int) = "Chez toi ($n)"
    override val inventaireAide =
        "Ce que l'IA a vu sur tes photos. Une erreur ou un oubli ? Corrige ici, puis régénère le plan."
    override val inventaireCorrige = "Corrigé par toi. Régénère le plan pour que les repas en tiennent compte."
    override val voirEtCorriger = "Voir et corriger"
    override val inventaireVide = "Aucun aliment pour l'instant."
    override fun endroit(e: Endroit) = when (e) {
        Endroit.GARDE_MANGER -> "Garde-manger"
        Endroit.FRIGO -> "Frigo"
        Endroit.CONGELATEUR -> "Congélateur"
        Endroit.AUTRE -> "Autre"
    }
    override val utiliserVite = "À utiliser vite"
    override val ajouterAliment = "Ajouter un aliment"
    override val modifierAliment = "Modifier l'aliment"
    override val aliment = "Aliment"
    override val quantiteFacultative = "Quantité (facultatif)"
    override val exempleQuantite = "Ex. : 500 g, environ 4, un demi-pot"
    override val titreRegenerer = "Pas tout à fait ça ?"
    override val regenererAide = "Dis à l'IA quoi changer. Elle repart de ton inventaire et de la circulaire."
    override val nouvellesPreferences = "Nouvelles préférences (facultatif)"
    override val exempleConsignes = "Ex. : plus végé, pas de porc, budget serré, garde le souper de samedi"
    override val garderPreferences = "Garder ces préférences pour les prochaines semaines"
    override val regenerer = "Régénérer le plan"
    override val regenererTitre = "Régénérer le plan ?"
    override fun regenererTexte(nouvellesPhotos: Int) =
        if (nouvellesPhotos > 0) {
            "${nbPhotos(nouvellesPhotos)} " + pl(nouvellesPhotos, "sera analysée", "seront analysées") +
                ". Le nouveau plan remplacera l'actuel et la liste d'épicerie."
        } else {
            "Sans nouvelles photos, l'IA repart de ton inventaire, avec tes corrections. Le nouveau plan remplacera l'actuel et la liste d'épicerie."
        }
    override fun coutIA(fournisseur: String, cents: Double, photos: Int, articles: Int) =
        "Analyse par $fournisseur : " + String.format(langue.locale, "%.2f", cents) + " ¢ US, " +
            nbPhotos(photos) + " et $articles articles de circulaire lus."
    override fun jetonsIA(fournisseur: String, entree: Int, sortie: Int, photos: Int, articles: Int) =
        "Analyse par $fournisseur : $entree jetons envoyés, $sortie reçus, " + nbPhotos(photos) +
            " et $articles articles de circulaire lus."

    // ----- Épicerie -----
    override val titreEpicerie = "Épicerie"
    override val epicerieVide = "Ta liste d'épicerie apparaîtra ici une fois le plan de la semaine généré."
    override val allerASemaine = "Aller à Ta semaine"
    override val totalTaxesIncluses = "Total estimé, taxes incluses"
    override fun sousTotal(montant: String) = "Sous-total : $montant"
    override fun taxes(montant: String, pourcentage: Double, nbTaxables: Int) =
        "Taxes à ${pourcent(pourcentage)} : $montant, sur $nbTaxables " + pl(nbTaxables, "article taxable", "articles taxables")
    override fun dansLePanier(faits: Int, total: Int) = "$faits sur $total dans le panier"
    override val partager = "Partager"
    override val partagerListe = "Partager la liste d'épicerie"
    override val toutDecocher = "Tout décocher"
    override fun rayon(r: Rayon) = when (r) {
        Rayon.FRUITS_LEGUMES -> "Fruits et légumes"
        Rayon.BOULANGERIE -> "Boulangerie"
        Rayon.VIANDES -> "Viandes et poissons"
        Rayon.LAITIERS -> "Produits laitiers et œufs"
        Rayon.EPICERIE -> "Garde-manger"
        Rayon.SURGELES -> "Surgelés"
        Rayon.AUTRES -> "Autres"
    }
    override val plusTaxes = "+ taxes"
    override val prixEstimesNote =
        "Les prix précédés de ≈ sont estimés par l'IA. Les taxes ne s'appliquent qu'aux articles taxables : la plupart des aliments de base en sont exemptés."

    // ----- Photos -----
    override val titrePhotos = "Photos"
    override val photosIntro =
        "Photographie ce que tu as déjà. L'IA s'en sert pour éviter les achats en double et utiliser ce qui doit être mangé vite."
    override fun zone(z: Zone) = when (z) {
        Zone.GARDE_MANGER -> "Garde-manger"
        Zone.FRIGO -> "Frigo"
        Zone.CONGELO -> "Congélateur"
    }
    override fun conseilZone(z: Zone) = when (z) {
        Zone.GARDE_MANGER -> "Ouvre les portes, une photo par tablette au besoin."
        Zone.FRIGO -> "Pense à la porte et aux tiroirs à légumes."
        Zone.CONGELO -> "Sors les sacs du fond si tu peux."
    }
    override val prendrePhoto = "Prendre une photo"
    override val galerie = "Galerie"
    override val supprimerPhoto = "Supprimer la photo"
    override val photosAucune =
        "Aucune photo pour l'instant. Sans photo, le plan se basera seulement sur tes ingrédients de base."
    override fun photosPretes(n: Int, quand: String) =
        "${nbPhotos(n)} " + pl(n, "prête", "prêtes") +
            ". Elles seront analysées à la génération automatique ($quand) ou dès que tu génères le plan, puis effacées."
    override val aucunAppareilPhoto = "Aucune application d'appareil photo trouvée."
    override val galerieImpossible = "Impossible d'ouvrir la galerie."

    // ----- Réglages -----
    override val titreReglages = "Réglages"
    override val reglagesIntro = "Tout est enregistré automatiquement."
    override val langueTitre = "Langue"
    override val epicerieTitre = "Mon épicerie"
    override val epicerieAide =
        "Entre ton code postal, puis choisis ton épicerie parmi celles qui publient une circulaire près de chez toi. Canada et États-Unis."
    override val codePostal = "Code postal"
    override val exempleCodePostal = "G1S 1T7"
    override val chercher = "Chercher"
    override val rechercheEnCours = "Recherche des circulaires"
    override fun epiceriesTrouvees(n: Int) = "$n " + pl(n, "épicerie avec une circulaire", "épiceries avec une circulaire")
    override val choisie = "Épicerie choisie"
    override val afficherToutesEpiceries = "Afficher les autres magasins"
    override val afficherToutesAide = "Grandes surfaces et pharmacies qui vendent aussi de l'alimentation"
    override val taxesTitre = "Taxes sur les articles taxables"
    override fun taxesAide(pourcentage: String, region: String) =
        "Taux de ta région : $pourcentage % ($region). Laisse vide pour l'utiliser, ou entre ton propre taux."
    override val tauxTaxes = "Taux personnalisé (%)"
    override fun circulaireChangeLe(jour: DayOfWeek) = "Ta circulaire change le ${nomJour(jour)}"
    override val alignerAide =
        "Pour profiter des spéciaux dès le premier jour : rappel des photos la veille à 19 h, plan généré à 0 h 30 le jour où la circulaire change."
    override val alignerHoraire = "Caler l'horaire sur la circulaire"
    override val horaireDejaAligne = "Ton horaire suit la circulaire : le plan est prêt dès le premier jour des spéciaux."
    override val iaTitre = "Intelligence artificielle"
    override val iaAide =
        "Choisis le service d'IA et colle ta clé d'API. DeepSeek est le moins cher. Le modèle doit pouvoir lire des images."
    override val fournisseurAutre = "Autre"
    override val urlApi = "Adresse de l'API"
    override val urlApiAide = "N'importe quelle API compatible OpenAI (chat completions)."
    override fun cleApi(fournisseur: String) = "Clé d'API $fournisseur"
    override fun ouTrouverCle(site: String) = "Crée ta clé sur $site."
    override val voir = "Voir"
    override val cacher = "Cacher"
    override val modele = "Modèle"
    override val modeleAide = "Laisse vide pour utiliser le modèle suggéré."
    override val modeReflexion = "Mode réflexion"
    override val modeReflexionAide = "Plans un peu mieux pensés, mais environ deux fois plus de jetons."
    override val tester = "Tester"
    override val repasTitre = "Mes repas"
    override val portions = "Portions par repas"
    override val soupers = "Soupers"
    override val diners = "Dîners"
    override val dinersAide = "Souvent avec les restes du souper"
    override val dejeuners = "Déjeuners"
    override val preferences = "Préférences, allergies, restrictions"
    override val exemplePreferences = "Ex. : pas de fruits de mer, deux soirs végé, on aime le piquant"
    override fun ingredientsBaseTitre(n: Int) = "Ingrédients de base ($n)"
    override val ingredientsBaseAide =
        "Toujours à la maison, même s'ils ne sont pas sur les photos. Ils ne seront jamais ajoutés à l'épicerie. Touche un ingrédient pour le retirer."
    override val ajouterIngredients = "Ajouter (séparés par des virgules)"
    override val retablirListe = "Rétablir la liste de départ"
    override val horaireTitre = "Horaire de la semaine"
    override val rappelPhotos = "Rappel des photos"
    override fun heureRappel(h: String) = "Rappel à $h"
    override val generationAuto = "Génération automatique du plan"
    override fun heureGeneration(h: String) = "Génération à $h"
    override val genererSansPhotos = "Générer même sans photos"
    override val genererSansPhotosAide = "Sinon, la génération automatique attend tes photos."
    override val fiabiliteTitre = "Notifications sur Samsung Galaxy"
    override val fiabiliteTexte =
        "Samsung met parfois les applis en veille, ce qui retarde le rappel et la génération de nuit. Dans Paramètres, Batterie, Limites d'utilisation en arrière-plan, ajoute Panier Futé aux applications jamais en veille."
    override val ouvrirReglagesBatterie = "Ouvrir les réglages de batterie"

    // ----- Messages -----
    override val epicerieDAbord = "Choisis d'abord ton épicerie."
    override val cleDAbord = "Configure d'abord l'intelligence artificielle."
    override val generationLancee = "Génération lancée. Tu peux fermer l'app : une notification t'avertira."
    override val photoNonEnregistree = "La photo n'a pas pu être enregistrée."
    override fun photosAjoutees(n: Int, max: Int) =
        "${nbPhotos(n)} " + pl(n, "ajoutée", "ajoutées") + ". Maximum de $max par endroit."
    override val entrerCleDAbord = "Entre d'abord ta clé d'API (et l'adresse de l'API pour « Autre »)."
    override val verification = "Vérification en cours"
    override fun testReussi(modele: String, solde: String?) =
        "Ça fonctionne : le modèle $modele répond et lit les images." + (solde?.let { " Solde : $it." } ?: "")
    override val erreurInconnue = "Erreur inconnue."
    override val erreurCodePostal = "Ce code postal n'est pas valide. Exemple : G1S 1T7 au Canada, 10001 aux États-Unis."
    override val aucuneEpicerieTrouvee = "Aucune circulaire trouvée pour ce code postal."
    override fun epicerieAbsente(nom: String) =
        "$nom n'a pas de circulaire en cours près de ce code postal. Choisis une autre épicerie."
    override val erreurRechercheEpiceries =
        "Impossible de lire la liste des épiceries. Vérifie ta connexion, puis réessaie."
    override fun epicerieChoisieMessage(nom: String) = "$nom choisie. Les prochains plans utiliseront sa circulaire."
    override fun horaireAligne(jour: DayOfWeek) =
        "Horaire calé : rappel la veille à 19 h, plan généré le ${nomJour(jour)} à 0 h 30."

    // ----- Génération -----
    override val aucunePhotoSemaine = "Aucune photo reçue cette semaine."
    override fun progresCirculaire(marchand: String) = "Lecture de la circulaire de $marchand"
    override val progresDemande = "Préparation de la demande"
    override fun progresPhotos(n: Int) = if (n <= 1) "Préparation de la photo" else "Préparation de $n photos"
    override val progresIA = "Analyse par l'IA, de 1 à 3 minutes"
    override val progresNouvelEssai = "Réponse incomplète, nouvel essai"
    override val progresNouvelEssaiLong = "Réponse trop longue, nouvel essai"
    override val progresEnregistrement = "Enregistrement du plan"

    // ----- Erreurs -----
    override val erreurAucuneEpicerie = "Choisis d'abord ton épicerie dans Réglages."
    override fun erreurCirculaireIntrouvable(marchand: String, codePostal: String) =
        "Aucune circulaire de $marchand trouvée près de $codePostal. Choisis une autre épicerie dans Réglages."
    override fun erreurCirculaireVide(marchand: String) =
        "La circulaire de $marchand est vide ou illisible pour le moment. Réessaie plus tard."
    override val erreurCleManquante = "Ajoute ta clé d'API dans Réglages, section Intelligence artificielle."
    override val erreurUrlManquante = "Indique l'adresse de l'API dans Réglages."
    override val erreurIASansReponse = "L'IA n'a rien répondu. Réessaie dans quelques minutes."
    override val erreurEnregistrement = "Le plan reçu n'a pas pu être enregistré."
    override fun erreurIA401(nom: String) = "$nom refuse la clé d'API. Vérifie-la dans Réglages."
    override fun erreurIA402(nom: String) = "Crédit insuffisant chez $nom. Recharge ton compte."
    override fun erreurIA404(nom: String, modele: String) =
        "$nom ne trouve pas le modèle « $modele ». Vérifie son nom dans Réglages."
    override fun erreurIA429(nom: String) = "Trop de requêtes chez $nom. Réessaie dans quelques minutes."
    override fun erreurIAServeur(nom: String, code: Int) = "Les serveurs de $nom ne répondent pas (erreur $code). Réessaie plus tard."
    override fun erreurIAAutre(nom: String, code: Int, detail: String) = "$nom a refusé la requête (erreur $code) : $detail"
    override fun diagnosticCoupee(jetons: Int) = "réponse coupée après $jetons jetons"
    override fun diagnosticIllisible(caracteres: Int) = "réponse illisible de $caracteres caractères"
    override fun diagnosticReflexion(jetons: Int) = "dont $jetons de réflexion"
    override val diagnosticEspaces = "surtout des espaces"
    override fun erreurPlanIncomplet(essais: Int, details: String) =
        "L'IA n'a pas renvoyé de plan complet après $essais " + pl(essais, "essai", "essais") +
            " ($details). Réessaie. Si ça recommence, choisis un autre modèle dans Réglages."

    // ----- Notifications -----
    override val notifCanalNom = "Épicerie de la semaine"
    override val notifCanalDescription = "Rappel des photos et plan de repas prêt"
    override val notifRappelTitre = "Photos du frigo, s'il te plaît"
    override fun notifRappelTexte(dejaPrises: Int, quand: String) =
        if (dejaPrises > 0) "${nbPhotos(dejaPrises)} " + pl(dejaPrises, "prête", "prêtes") + ". Ajoutes-en au besoin : le plan sera généré $quand."
        else "Prends en photo ton garde-manger, ton frigo et ton congélateur. Le plan sera généré $quand."
    override val notifPlanTitre = "Ton plan de la semaine est prêt"
    override fun notifPlanTexte(plan: Plan): String {
        val base = "${plan.repas.size} repas et ${plan.epicerie.size} articles, environ ${arg(plan.totalEstime)} taxes incluses."
        val debut = plan.circulaireDebut
        val fin = plan.circulaireFin ?: return base
        val ideal = if (debut != null) finJoursIdeaux(debut, fin) else null
        return if (ideal != null && !LocalDate.now().isAfter(ideal)) {
            "$base Vas-y idéalement d'ici ${jourEtDateCourt(ideal)}, les spéciaux partent vite."
        } else {
            "$base Spéciaux jusqu'au ${jourEtDateCourt(fin)} : vas-y dès que possible."
        }
    }
    override val notifErreurTitre = "Plan de la semaine non généré"
    override val notifOuvrirPourPhotos = "Ouvre l'app pour ajouter tes photos et générer le plan."
}

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

/** English (Canada / United States). */
object TextesEn : Textes(Langue.EN) {
    private fun pl(n: Int, one: String, many: String) = if (n == 1) one else many

    /** "Thursday, November 19" */
    private fun dayAndDate(date: LocalDate) = d(date, "EEEE, MMMM d")
    private fun dayAndDateShort(date: LocalDate) = d(date, "EEE, MMM d")

    // ----- Dates -----
    override fun dateHeure(z: ZonedDateTime) =
        z.format(DateTimeFormatter.ofPattern("EEEE, MMM d 'at' h:mm a", langue.locale))
    override fun heure(h: Int, m: Int): String {
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "$h12:" + m.toString().padStart(2, '0') + if (h < 12) " AM" else " PM"
    }
    override fun jourCourt(date: LocalDate) = d(date, "EEE")
    override fun jourAbrege(j: DayOfWeek) = when (j) {
        DayOfWeek.MONDAY -> "Mon"; DayOfWeek.TUESDAY -> "Tue"; DayOfWeek.WEDNESDAY -> "Wed"
        DayOfWeek.THURSDAY -> "Thu"; DayOfWeek.FRIDAY -> "Fri"; DayOfWeek.SATURDAY -> "Sat"
        DayOfWeek.SUNDAY -> "Sun"
    }
    override fun initialeJour(date: LocalDate) = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "M"; DayOfWeek.TUESDAY -> "T"; DayOfWeek.WEDNESDAY -> "W"
        DayOfWeek.THURSDAY -> "T"; DayOfWeek.FRIDAY -> "F"; DayOfWeek.SATURDAY -> "S"
        DayOfWeek.SUNDAY -> "S"
    }
    override val aujourdhuiCourt = "today"

    // ----- Onglets -----
    override val ongletSemaine = "Week"
    override val ongletEpicerie = "Groceries"
    override val ongletPhotos = "Photos"
    override val ongletReglages = "Settings"

    // ----- Communs -----
    override val ajouter = "Add"
    override val retirer = "Remove"
    override val modifier = "Edit"
    override val enregistrer = "Save"
    override val annuler = "Cancel"
    override val masquer = "Hide"
    override val special = "Sale"
    override val genererPlan = "Make my plan now"
    override fun prochainRappel(quand: String) = "Next reminder: $quand"
    override fun prochaineGeneration(quand: String) = "Next plan: $quand"

    // ----- Circulaire -----
    override fun circulairePeriode(debut: LocalDate, fin: LocalDate) =
        "Sale prices ${dayAndDate(debut)} to ${dayAndDate(fin)}"
    override fun circulaireCourte(debut: LocalDate, fin: LocalDate) =
        if (debut.month == fin.month) "Flyer ${d(debut, "MMM d")} to ${fin.dayOfMonth}"
        else "Flyer ${d(debut, "MMM d")} to ${d(fin, "MMM d")}"
    override fun circulaireJusquau(fin: LocalDate) = "Sale prices until ${dayAndDate(fin)}"
    override fun circulaireAVenir(debut: LocalDate, idealFin: LocalDate) =
        "Sale prices start ${dayAndDate(debut)}. Try to shop by ${dayAndDate(idealFin)}, before the sale items run out."
    override fun circulaireIdeal(idealFin: LocalDate) =
        "Try to shop by ${dayAndDate(idealFin)}: sale items sell out fast in the first few days."
    override fun circulaireRestant(jours: Int, fin: LocalDate) =
        if (jours <= 1) "Last day of sale prices. Go as soon as you can."
        else "$jours days of sale prices left, until ${dayAndDate(fin)}. Go as soon as you can: some items may already be gone."
    override val circulaireTerminee = "This flyer has ended. Make a new plan to use the new one."

    // ----- Semaine -----
    override val titreSemaine = "Your week"
    override fun epicerieEtCodePostal(marchand: String, codePostal: String) = "$marchand, $codePostal"
    override val attenteReseau = "Waiting for an internet connection"
    override val generationEchouee = "The last plan could not be made"
    override fun versionDemandee(consignes: String) = "You asked for: “$consignes”"
    override val titreRepas = "On the menu"
    override val titreRemarques = "Good to know"
    override val preparationPlan = "Making your plan"
    override val fermerAppPossible = "You can close the app: a notification will let you know."
    override val accueilIntro =
        "Each week, Panier Futé reads your grocery store's flyer, looks at what you already have, and suggests meals and a grocery list built around the sales."
    override val etapeEpicerie = "Choose your grocery store"
    override val etapeCle = "Set up the AI service"
    override val etapePhotos = "Take photos of your pantry, fridge and freezer"
    override fun nbPhotos(n: Int) = "$n " + pl(n, "photo", "photos")
    override fun horaireResume(rappel: String, generation: String) =
        "Next reminder: $rappel. Automatic plan: $generation."
    override val epicerieEstimee = "Estimated groceries, tax included"
    override fun articlesResume(n: Int, enSpecial: Int) =
        "$n " + pl(n, "item", "items") + ", $enSpecial on sale"
    override fun economies(montant: String) = "Saved with sale prices: $montant"
    override fun moment(m: Moment) = when (m) {
        Moment.DEJEUNER -> "Breakfast"
        Moment.DINER -> "Lunch"
        Moment.SOUPER -> "Dinner"
    }
    override val voirRecette = "Show recipe"
    override val masquerRecette = "Hide recipe"
    override fun ingredientsPour(portions: Int) =
        if (portions > 0) "Ingredients for $portions " + pl(portions, "serving", "servings") else "Ingredients"
    override val sourceRabais = "on sale"
    override val sourceStock = "in stock"
    override val sourceBase = "staple"
    override val sourceAchat = "to buy"
    override val preparation = "Steps"
    override fun titreInventaire(n: Int) = "At home ($n)"
    override val inventaireAide =
        "What the AI saw in your photos. Something wrong or missing? Fix it here, then make a new plan."
    override val inventaireCorrige = "Edited by you. Make a new plan so the meals use your changes."
    override val voirEtCorriger = "View and edit"
    override val inventaireVide = "No food items yet."
    override fun endroit(e: Endroit) = when (e) {
        Endroit.GARDE_MANGER -> "Pantry"
        Endroit.FRIGO -> "Fridge"
        Endroit.CONGELATEUR -> "Freezer"
        Endroit.AUTRE -> "Other"
    }
    override val utiliserVite = "Use soon"
    override val ajouterAliment = "Add a food item"
    override val modifierAliment = "Edit food item"
    override val aliment = "Food item"
    override val quantiteFacultative = "Quantity (optional)"
    override val exempleQuantite = "e.g. 500 g, about 4, half a jar"
    override val titreRegenerer = "Not quite right?"
    override val regenererAide = "Tell the AI what to change. It starts again from your inventory and the flyer."
    override val nouvellesPreferences = "New preferences (optional)"
    override val exempleConsignes = "e.g. more vegetarian, no pork, tight budget, keep Saturday's dinner"
    override val garderPreferences = "Keep these preferences for the coming weeks"
    override val regenerer = "Make a new plan"
    override val regenererTitre = "Make a new plan?"
    override fun regenererTexte(nouvellesPhotos: Int) =
        if (nouvellesPhotos > 0) {
            "Your ${nbPhotos(nouvellesPhotos)} will be analyzed. The new plan replaces the current one and its grocery list."
        } else {
            "With no new photos, the AI starts from your inventory, including your edits. The new plan replaces the current one and its grocery list."
        }
    override fun coutIA(fournisseur: String, cents: Double, photos: Int, articles: Int) =
        "Analyzed by $fournisseur: " + String.format(langue.locale, "%.2f", cents) + " US cents, " +
            nbPhotos(photos) + " and $articles flyer items read."
    override fun jetonsIA(fournisseur: String, entree: Int, sortie: Int, photos: Int, articles: Int) =
        "Analyzed by $fournisseur: $entree tokens sent, $sortie received, " + nbPhotos(photos) +
            " and $articles flyer items read."

    // ----- Épicerie -----
    override val titreEpicerie = "Groceries"
    override val epicerieVide = "Your grocery list will show up here once this week's plan is ready."
    override val allerASemaine = "Go to Your week"
    override val totalTaxesIncluses = "Estimated total, tax included"
    override fun sousTotal(montant: String) = "Subtotal: $montant"
    override fun taxes(montant: String, pourcentage: Double, nbTaxables: Int) =
        "Tax at ${pourcent(pourcentage)}: $montant, on $nbTaxables taxable " + pl(nbTaxables, "item", "items")
    override fun dansLePanier(faits: Int, total: Int) = "$faits of $total in the cart"
    override val motPour = "for"
    override val partager = "Share"
    override val partagerListe = "Share the grocery list"
    override val toutDecocher = "Uncheck all"
    override fun rayon(r: Rayon) = when (r) {
        Rayon.FRUITS_LEGUMES -> "Produce"
        Rayon.BOULANGERIE -> "Bakery"
        Rayon.VIANDES -> "Meat and fish"
        Rayon.LAITIERS -> "Dairy and eggs"
        Rayon.EPICERIE -> "Pantry"
        Rayon.SURGELES -> "Frozen"
        Rayon.AUTRES -> "Other"
    }
    override val plusTaxes = "+ tax"
    override val prixEstimesNote =
        "Prices marked ≈ are estimated by the AI. Tax applies only to taxable items: most basic groceries are exempt."

    // ----- Photos -----
    override val titrePhotos = "Photos"
    override val photosIntro =
        "Take photos of what you already have. The AI uses them to avoid buying doubles and to use up what needs eating soon."
    override fun zone(z: Zone) = when (z) {
        Zone.GARDE_MANGER -> "Pantry"
        Zone.FRIGO -> "Fridge"
        Zone.CONGELO -> "Freezer"
    }
    override fun conseilZone(z: Zone) = when (z) {
        Zone.GARDE_MANGER -> "Open the doors, one photo per shelf if needed."
        Zone.FRIGO -> "Include the door and the crisper drawers."
        Zone.CONGELO -> "Pull the bags at the back forward if you can."
    }
    override val prendrePhoto = "Take a photo"
    override val galerie = "Gallery"
    override val supprimerPhoto = "Delete photo"
    override val photosAucune = "No photos yet. Without photos, the plan only counts on your staples."
    override fun photosPretes(n: Int, quand: String) =
        "${nbPhotos(n)} ready. They'll be analyzed at the automatic plan ($quand) or as soon as you make a plan, then deleted."
    override val aucunAppareilPhoto = "No camera app found."
    override val galerieImpossible = "Can't open the gallery."

    // ----- Réglages -----
    override val titreReglages = "Settings"
    override val reglagesIntro = "Everything is saved automatically."
    override val langueTitre = "Language"
    override val epicerieTitre = "My grocery store"
    override val epicerieAide =
        "Enter your postal or ZIP code, then pick your store among those with a flyer near you. Canada and the United States."
    override val codePostal = "Postal or ZIP code"
    override val exempleCodePostal = "M5V 2T6"
    override val chercher = "Search"
    override val rechercheEnCours = "Looking for flyers"
    override fun epiceriesTrouvees(n: Int) = "$n " + pl(n, "store with a flyer", "stores with a flyer")
    override val choisie = "Selected store"
    override val afficherToutesEpiceries = "Show other stores"
    override val afficherToutesAide = "Big-box stores and pharmacies that also sell food"
    override val taxesTitre = "Tax on taxable items"
    override fun taxesAide(pourcentage: String, region: String) =
        "Your region's rate: $pourcentage ($region). Leave blank to use it, or enter your own rate."
    override val tauxTaxes = "Custom rate (%)"
    override fun circulaireChangeLe(jour: DayOfWeek) = "Your flyer changes on ${nomJour(jour)}"
    override val alignerAide =
        "To catch sales on day one: photo reminder the evening before at 7 PM, plan made at 12:30 AM the day the flyer changes."
    override val alignerHoraire = "Match my schedule to the flyer"
    override val horaireDejaAligne = "Your schedule follows the flyer: the plan is ready on the first day of the sale."
    override val iaTitre = "AI service"
    override val iaAide =
        "Choose the AI service and paste your API key. DeepSeek is the cheapest. The model must be able to read images."
    override val fournisseurAutre = "Other"
    override val urlApi = "API address"
    override val urlApiAide = "Any OpenAI-compatible API (chat completions)."
    override fun cleApi(fournisseur: String) = "$fournisseur API key"
    override fun ouTrouverCle(site: String) = "Create your key at $site."
    override val voir = "Show"
    override val cacher = "Hide"
    override val modele = "Model"
    override val modeleAide = "Leave blank to use the suggested model."
    override val modeReflexion = "Thinking mode"
    override val modeReflexionAide = "Slightly better plans, but about twice the tokens."
    override val tester = "Test"
    override val repasTitre = "My meals"
    override val portions = "Servings per meal"
    override val soupers = "Dinners"
    override val diners = "Lunches"
    override val dinersAide = "Often with dinner leftovers"
    override val dejeuners = "Breakfasts"
    override val preferences = "Preferences, allergies, restrictions"
    override val exemplePreferences = "e.g. no seafood, two vegetarian nights, we like it spicy"
    override fun ingredientsBaseTitre(n: Int) = "Staples ($n)"
    override val ingredientsBaseAide =
        "Always at home, even if they're not in the photos. They're never added to the grocery list. Tap one to remove it."
    override val ajouterIngredients = "Add (separated by commas)"
    override val retablirListe = "Restore the starting list"
    override val horaireTitre = "Weekly schedule"
    override val rappelPhotos = "Photo reminder"
    override fun heureRappel(h: String) = "Reminder at $h"
    override val generationAuto = "Automatic plan"
    override fun heureGeneration(h: String) = "Plan at $h"
    override val genererSansPhotos = "Make the plan even without photos"
    override val genererSansPhotosAide = "Otherwise, the automatic plan waits for your photos."
    override val fiabiliteTitre = "Notifications on Samsung Galaxy"
    override val fiabiliteTexte =
        "Samsung sometimes puts apps to sleep, which delays the reminder and the overnight plan. In Settings, Battery, Background usage limits, add Panier Futé to Never sleeping apps."
    override val ouvrirReglagesBatterie = "Open battery settings"

    // ----- Messages -----
    override val epicerieDAbord = "Choose your grocery store first."
    override val cleDAbord = "Set up the AI service first."
    override val generationLancee = "Plan started. You can close the app: a notification will let you know."
    override val photoNonEnregistree = "The photo couldn't be saved."
    override fun photosAjoutees(n: Int, max: Int) = "${nbPhotos(n)} added. Up to $max per spot."
    override val entrerCleDAbord = "Enter your API key first (and the API address for Other)."
    override val verification = "Checking"
    override fun testReussi(modele: String, solde: String?) =
        "It works: $modele answers and reads images." + (solde?.let { " Balance: $it." } ?: "")
    override val erreurInconnue = "Unknown error."
    override val erreurCodePostal = "This code isn't valid. Example: M5V 2T6 in Canada, 10001 in the United States."
    override val aucuneEpicerieTrouvee = "No flyers found for this code."
    override fun epicerieAbsente(nom: String) = "$nom has no current flyer near this code. Choose another store."
    override val erreurRechercheEpiceries = "Couldn't load the list of stores. Check your connection and try again."
    override fun epicerieChoisieMessage(nom: String) = "$nom selected. Your next plans will use its flyer."
    override fun horaireAligne(jour: DayOfWeek) =
        "Schedule updated: reminder the evening before at 7 PM, plan made on ${nomJour(jour)} at 12:30 AM."

    // ----- Génération -----
    override val aucunePhotoSemaine = "No photos this week."
    override fun progresCirculaire(marchand: String) = "Reading the $marchand flyer"
    override val progresDemande = "Preparing the request"
    override fun progresPhotos(n: Int) = if (n == 1) "Preparing the photo" else "Preparing $n photos"
    override val progresIA = "AI analysis, 1 to 3 minutes"
    override val progresNouvelEssai = "Incomplete answer, trying again"
    override val progresNouvelEssaiLong = "Answer too long, trying again"
    override val progresEnregistrement = "Saving the plan"

    // ----- Erreurs -----
    override val erreurAucuneEpicerie = "Choose your grocery store in Settings first."
    override fun erreurCirculaireIntrouvable(marchand: String, codePostal: String) =
        "No $marchand flyer found near $codePostal. Choose another store in Settings."
    override fun erreurCirculaireVide(marchand: String) =
        "The $marchand flyer is empty or unreadable right now. Try again later."
    override val erreurCleManquante = "Add your API key in Settings, under AI service."
    override val erreurUrlManquante = "Enter the API address in Settings."
    override val erreurIASansReponse = "The AI didn't answer. Try again in a few minutes."
    override val erreurEnregistrement = "The plan couldn't be saved."
    override fun erreurIA401(nom: String) = "$nom rejected the API key. Check it in Settings."
    override fun erreurIA402(nom: String) = "Not enough credit with $nom. Top up your account."
    override fun erreurIA404(nom: String, modele: String) = "$nom can't find the model “$modele”. Check its name in Settings."
    override fun erreurIA429(nom: String) = "Too many requests to $nom. Try again in a few minutes."
    override fun erreurIAServeur(nom: String, code: Int) = "$nom servers aren't responding (error $code). Try again later."
    override fun erreurIAAutre(nom: String, code: Int, detail: String) = "$nom rejected the request (error $code): $detail"
    override fun diagnosticCoupee(jetons: Int) = "answer cut off after $jetons tokens"
    override fun diagnosticIllisible(caracteres: Int) = "unreadable answer of $caracteres characters"
    override fun diagnosticReflexion(jetons: Int) = "including $jetons thinking tokens"
    override val diagnosticEspaces = "mostly blank space"
    override fun erreurPlanIncomplet(essais: Int, details: String) =
        "The AI didn't return a complete plan after $essais " + pl(essais, "try", "tries") +
            " ($details). Try again. If it keeps happening, choose another model in Settings."

    // ----- Notifications -----
    override val notifCanalNom = "Weekly groceries"
    override val notifCanalDescription = "Photo reminder and meal plan ready"
    override val notifRappelTitre = "Fridge photos, please"
    override fun notifRappelTexte(dejaPrises: Int, quand: String) =
        if (dejaPrises > 0) "${nbPhotos(dejaPrises)} ready. Add more if you like: the plan will be made $quand."
        else "Take photos of your pantry, fridge and freezer. The plan will be made $quand."
    override val notifPlanTitre = "Your weekly plan is ready"
    override fun notifPlanTexte(plan: Plan): String {
        val base = "${plan.repas.size} meals and ${plan.epicerie.size} items, about ${arg(plan.totalEstime)} with tax."
        val debut = plan.circulaireDebut
        val fin = plan.circulaireFin ?: return base
        val ideal = if (debut != null) finJoursIdeaux(debut, fin) else null
        return if (ideal != null && !LocalDate.now().isAfter(ideal)) {
            "$base Try to shop by ${dayAndDateShort(ideal)}: sale items go fast."
        } else {
            "$base Sale prices until ${dayAndDateShort(fin)}: go as soon as you can."
        }
    }
    override val notifErreurTitre = "Weekly plan not made"
    override val notifOuvrirPourPhotos = "Open the app to add your photos and make the plan."
}

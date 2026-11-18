package ca.panierfute

import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityOptionsCompat
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import ca.panierfute.data.ArticleEpicerie
import ca.panierfute.data.Endroit
import ca.panierfute.data.Epicerie
import ca.panierfute.data.EtatReglages
import ca.panierfute.data.Fournisseur
import ca.panierfute.data.IngredientRepas
import ca.panierfute.data.ItemInventaire
import ca.panierfute.data.Langue
import ca.panierfute.data.Moment
import ca.panierfute.data.Plan
import ca.panierfute.data.Rayon
import ca.panierfute.data.Repas
import ca.panierfute.data.Source
import ca.panierfute.data.Zone
import ca.panierfute.data.ingredientsBaseDefaut
import ca.panierfute.i18n.Textes
import ca.panierfute.i18n.textes
import ca.panierfute.ui.EcranPrincipal
import ca.panierfute.ui.EtatApp
import ca.panierfute.ui.Onglet
import ca.panierfute.ui.ThemePanier
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * Captures d'écran automatiques des écrans avec des données de démonstration.
 * Lancées par GitHub Actions : ./gradlew -Pcaptures recordPaparazziDebug
 */
class CapturesTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar"
    )

    private fun capture(nom: String, ui: EtatApp, sombre: Boolean = false, longue: Boolean = false) {
        paparazzi.unsafeUpdateConfig(
            deviceConfig = if (longue) DeviceConfig.PIXEL_5.copy(screenHeight = 7600) else DeviceConfig.PIXEL_5
        )
        paparazzi.snapshot(nom) {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides FauxRegistre) {
                ThemePanier(sombre = sombre) { EcranPrincipal(ui) }
            }
        }
    }

    @Test fun semaine() {
        capture("semaine_fr", Demo(Langue.FR))
        capture("semaine_fr_complete", Demo(Langue.FR), longue = true)
        capture("semaine_en", Demo(Langue.EN))
        capture("semaine_es", Demo(Langue.ES))
        capture("semaine_fr_sombre", Demo(Langue.FR), sombre = true)
    }

    @Test fun accueil() {
        capture("accueil_fr", Demo(Langue.FR, avecPlan = false))
        capture("accueil_es", Demo(Langue.ES, avecPlan = false))
    }

    @Test fun epicerie() {
        capture("epicerie_fr", Demo(Langue.FR, onglet = Onglet.EPICERIE))
        capture("epicerie_fr_complete", Demo(Langue.FR, onglet = Onglet.EPICERIE), longue = true)
        capture("epicerie_en_sombre", Demo(Langue.EN, onglet = Onglet.EPICERIE), sombre = true)
    }

    @Test fun photos() {
        capture("photos_fr", Demo(Langue.FR, onglet = Onglet.PHOTOS), longue = true)
    }

    @Test fun reglages() {
        capture("reglages_fr_complete", Demo(Langue.FR, onglet = Onglet.REGLAGES), longue = true)
        capture("reglages_en", Demo(Langue.EN, onglet = Onglet.REGLAGES))
    }
}

private object FauxRegistre : ActivityResultRegistryOwner {
    override val activityResultRegistry: ActivityResultRegistry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) = Unit
    }
}

/** État de démonstration : une semaine typique au Maxi de Québec. */
private class Demo(
    override val langue: Langue,
    avecPlan: Boolean = true,
    onglet: Onglet = Onglet.SEMAINE
) : EtatApp {
    override var onglet by mutableStateOf(onglet)
    override var message: String? = null
    override val t: Textes = textes(langue)
    override val aujourdhui: LocalDate = LocalDate.of(2026, 11, 20)

    override val reglages = EtatReglages(
        langue = langue,
        fournisseur = Fournisseur.DEEPSEEK,
        cles = mapOf("deepseek" to "sk-demo"),
        modeles = emptyMap(),
        urlPerso = "",
        reflexion = false,
        marchand = if (avecPlan) "Maxi" else "",
        marchandId = 2349,
        marchandLogo = "",
        jourDebutCirculaire = 4,
        codePostal = "G1S1T7",
        tauxTaxesPerso = -1.0,
        portions = 2,
        dejeuners = false,
        diners = true,
        soupers = true,
        preferences = "Pas de fruits de mer. On aime le piquant.",
        ingredientsBase = ingredientsBaseDefaut(langue),
        jourRappel = 5,
        heureRappel = 17,
        minuteRappel = 0,
        heureGeneration = 0,
        minuteGeneration = 30,
        genererSansPhotos = true
    )

    override val plan: Plan? = if (avecPlan) PLAN else null
    override val photos: Map<Zone, List<File>> = mapOf(
        Zone.GARDE_MANGER to emptyList(),
        Zone.FRIGO to listOf(File("demo1.jpg"), File("demo2.jpg")),
        Zone.CONGELO to emptyList()
    )
    override val coches: Set<Int> = setOf(0, 3)
    override val generationEnCours = false
    override val attenteReseau = false
    override val etape = ""
    override val erreur = ""
    override val prochainRappel: String =
        t.dateHeure(LocalDate.of(2026, 11, 25).atTime(19, 0).atZone(ZoneId.of("America/Toronto")))
    override val prochaineGeneration: String =
        t.dateHeure(LocalDate.of(2026, 11, 26).atTime(0, 30).atZone(ZoneId.of("America/Toronto")))
    override val resultatCle: String? = null
    override val testEnCours = false
    override val epiceries = listOf(
        Epicerie(2349, "Maxi", "", LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 25), true),
        Epicerie(1, "IGA", "", LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 25), true),
        Epicerie(2, "L'Inter-Marché", "", LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 25), true),
        Epicerie(3, "Metro", "", LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 25), true),
        Epicerie(4, "Super C", "", LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 25), true)
    )
    override val rechercheEpiceries = false
    override val erreurEpiceries: String? = null
    override val toutesEpiceries = false

    override fun allerA(cle: String) = Unit
    override fun generer(consignes: String, garder: Boolean) = Unit
    override fun ajouterAliment(item: ItemInventaire) = Unit
    override fun remplacerAliment(index: Int, item: ItemInventaire) = Unit
    override fun retirerAliment(index: Int) = Unit
    override fun basculer(index: Int) = Unit
    override fun decocherTout() = Unit
    override fun ajouterCapture(zone: Zone, fichier: File) = Unit
    override fun ajouterGalerie(zone: Zone, uris: List<Uri>) = Unit
    override fun supprimerPhoto(fichier: File) = Unit
    override fun maj(transformation: (EtatReglages) -> EtatReglages) = Unit
    override fun changerLangue(l: Langue) = Unit
    override fun testerCle() = Unit
    override fun changerFournisseur(f: Fournisseur) = Unit
    override fun chercherEpiceries(codePostal: String) = Unit
    override fun choisirEpicerie(e: Epicerie) = Unit
    override fun afficherToutesEpiceries(v: Boolean) = Unit
    override fun alignerSurCirculaire() = Unit
}

private fun jour(j: Int) = LocalDate.of(2026, 11, j)

private fun ing(nom: String, q: String, s: Source) = IngredientRepas(nom, q, s)

private val PLAN = Plan(
    genereLe = 0L,
    debutPlan = jour(20),
    circulaireDebut = jour(19),
    circulaireFin = jour(25),
    marchand = "Maxi",
    devise = "CAD",
    tauxTaxes = 0.14975,
    nbArticlesCirculaire = 312,
    nbPhotos = 7,
    coutUsd = 0.0042,
    jetonsEntree = 14800,
    jetonsSortie = 5200,
    fournisseur = "DeepSeek",
    resume = "Le poulet entier et les patates douces en spécial portent la semaine. Les épinards et le yogourt du frigo passent en premier.",
    repas = listOf(
        Repas(
            jour(20), "", Moment.SOUPER, "Poulet rôti au paprika fumé et patates douces",
            "Un poulet entier pour deux soupers : les restes vont dans les wraps de dimanche.", 2, 75,
            listOf(
                ing("poulet entier", "1,5 kg", Source.RABAIS),
                ing("patates douces", "3", Source.RABAIS),
                ing("oignon rouge", "1", Source.STOCK),
                ing("paprika fumé", "2 c. à thé", Source.BASE)
            ),
            listOf(
                "Préchauffe le four à 200 °C.",
                "Frotte le poulet avec l'huile, le paprika, le sel et le poivre.",
                "Coupe les patates douces et l'oignon en quartiers autour du poulet.",
                "Rôtis 70 minutes, jusqu'à 82 °C dans la cuisse."
            )
        ),
        Repas(
            jour(21), "", Moment.DINER, "Bols de riz aux épinards et œufs",
            "Rapide, avec les épinards à utiliser vite.", 2, 15,
            listOf(ing("épinards", "1 sac", Source.STOCK), ing("œufs", "4", Source.ACHAT)), listOf("Cuis le riz.", "Fais tomber les épinards.", "Ajoute les œufs au plat.")
        ),
        Repas(
            jour(21), "", Moment.SOUPER, "Chili de bœuf haché aux haricots noirs",
            "Une grosse casserole : la moitié va au congélateur.", 4, 40,
            listOf(ing("bœuf haché mi-maigre", "900 g", Source.RABAIS), ing("haricots noirs", "2 boîtes", Source.ACHAT)), listOf("Fais revenir le bœuf.", "Ajoute le reste et mijote 25 minutes.")
        ),
        Repas(
            jour(22), "", Moment.SOUPER, "Wraps au poulet, yogourt à l'ail et chou croquant",
            "Avec les restes du poulet de vendredi.", 2, 20,
            listOf(ing("tortillas de blé", "8", Source.RABAIS), ing("yogourt nature", "1 tasse", Source.STOCK)), listOf("Effiloche le poulet.", "Garnis les tortillas.")
        ),
        Repas(
            jour(23), "", Moment.SOUPER, "Saumon à l'érable, riz et brocoli",
            "Le brocoli en spécial cuit à la vapeur pendant le riz.", 2, 30,
            listOf(ing("filets de saumon", "2", Source.ACHAT), ing("brocoli", "1", Source.RABAIS)), listOf("Badigeonne le saumon de sirop d'érable.", "Cuis 12 minutes au four.")
        )
    ),
    epicerie = listOf(
        ArticleEpicerie(0, "Poulet entier frais, catégorie A", "1 (environ 1,5 kg)", Rayon.VIANDES, true, 9.87, 9.87, false, false, listOf("ven. souper", "dim. souper")),
        ArticleEpicerie(1, "Bœuf haché mi-maigre", "900 g", Rayon.VIANDES, true, 8.99, 8.99, false, false, listOf("sam. souper")),
        ArticleEpicerie(2, "Filets de saumon de l'Atlantique", "2", Rayon.VIANDES, false, 6.50, 13.00, true, false, listOf("lun. souper")),
        ArticleEpicerie(3, "Patates douces", "3", Rayon.FRUITS_LEGUMES, true, 0.99, 2.97, false, false, listOf("ven. souper")),
        ArticleEpicerie(4, "Brocoli", "1", Rayon.FRUITS_LEGUMES, true, 1.88, 1.88, false, false, listOf("lun. souper")),
        ArticleEpicerie(5, "Chou vert", "1", Rayon.FRUITS_LEGUMES, false, 2.49, 2.49, true, false, listOf("dim. souper")),
        ArticleEpicerie(6, "Tortillas de blé Dempster's", "1 paquet de 8", Rayon.BOULANGERIE, true, 2.99, 2.99, false, false, listOf("dim. souper")),
        ArticleEpicerie(7, "Œufs gros, douzaine", "1", Rayon.LAITIERS, false, 4.29, 4.29, true, false, listOf("sam. dîner")),
        ArticleEpicerie(8, "Haricots noirs en conserve", "2 boîtes", Rayon.EPICERIE, true, 1.25, 2.50, false, false, listOf("sam. souper")),
        ArticleEpicerie(9, "Eau pétillante citron", "12 canettes", Rayon.EPICERIE, true, 4.99, 4.99, false, true, listOf())
    ),
    inventaire = listOf(
        ItemInventaire("épinards", Endroit.FRIGO, "1 sac entamé", true),
        ItemInventaire("yogourt nature", Endroit.FRIGO, "500 g", false),
        ItemInventaire("riz basmati", Endroit.GARDE_MANGER, "environ 1 kg", false),
        ItemInventaire("petits pois surgelés", Endroit.CONGELATEUR, "1 sac", false)
    ),
    economiesEstimees = 18.40,
    remarques = listOf(
        "Le bœuf haché à 8,99 $ se congèle bien : un deuxième paquet vaut la peine.",
        "La photo du congélateur était floue ; vérifie l'inventaire."
    ),
    inventaireCorrige = false,
    consignes = ""
)

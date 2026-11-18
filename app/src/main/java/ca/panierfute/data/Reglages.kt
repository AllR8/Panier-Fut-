package ca.panierfute.data

import android.content.Context
import java.time.DayOfWeek

/** Ingrédients toujours disponibles à la maison, même s'ils n'apparaissent pas sur les photos. */
fun ingredientsBaseDefaut(langue: Langue): List<String> = when (langue) {
    Langue.FR -> INGREDIENTS_BASE_DEFAUT
    Langue.EN -> listOf(
        "All-purpose flour", "Salt", "Pepper", "Mayonnaise", "Dijon mustard", "Yellow mustard",
        "Ketchup", "Olive oil", "Canola oil", "Butter", "Sugar", "Brown sugar",
        "White vinegar", "Balsamic vinegar", "Soy sauce", "Worcestershire sauce", "Honey",
        "Maple syrup", "Chicken bouillon", "Beef bouillon", "Garlic powder", "Onion powder",
        "Paprika", "Cumin", "Dried oregano", "Dried thyme", "Dried basil", "Chili flakes",
        "Cinnamon", "Baking powder", "Baking soda", "Cornstarch", "Vanilla extract"
    )
    Langue.ES -> listOf(
        "Harina de trigo", "Sal", "Pimienta", "Mayonesa", "Mostaza de Dijon", "Mostaza amarilla",
        "Kétchup", "Aceite de oliva", "Aceite de canola", "Mantequilla", "Azúcar", "Azúcar morena",
        "Vinagre blanco", "Vinagre balsámico", "Salsa de soya", "Salsa inglesa", "Miel",
        "Jarabe de arce", "Caldo de pollo", "Caldo de res", "Ajo en polvo", "Cebolla en polvo",
        "Pimentón", "Comino", "Orégano seco", "Tomillo seco", "Albahaca seca", "Hojuelas de chile",
        "Canela", "Polvo para hornear", "Bicarbonato de sodio", "Fécula de maíz", "Extracto de vainilla"
    )
}

private val INGREDIENTS_BASE_DEFAUT = listOf(
    "Farine tout usage", "Sel", "Poivre", "Mayonnaise", "Moutarde de Dijon", "Moutarde jaune",
    "Ketchup", "Huile d'olive", "Huile de canola", "Beurre", "Sucre", "Cassonade",
    "Vinaigre blanc", "Vinaigre balsamique", "Sauce soya", "Sauce Worcestershire", "Miel",
    "Sirop d'érable", "Bouillon de poulet concentré", "Bouillon de bœuf concentré",
    "Ail en poudre", "Poudre d'oignon", "Paprika", "Cumin", "Origan séché", "Thym séché",
    "Basilic séché", "Flocons de piment", "Cannelle", "Poudre à pâte", "Bicarbonate de soude",
    "Fécule de maïs", "Extrait de vanille"
)

/** Copie immuable de tous les réglages, utilisée par l'interface. */
data class EtatReglages(
    val langue: Langue,
    val fournisseur: Fournisseur,
    val cles: Map<String, String>,      // une clé par fournisseur (code -> clé)
    val modeles: Map<String, String>,   // un modèle par fournisseur (vide = modèle suggéré)
    val urlPerso: String,               // adresse de l'API pour « Autre »
    val reflexion: Boolean,
    val marchand: String,         // nom de l'épicerie (ex. « Maxi ») ; vide = pas encore choisie
    val marchandId: Long,         // identifiant Flipp de l'épicerie (0 = inconnu)
    val marchandLogo: String,
    val jourDebutCirculaire: Int, // jour où la circulaire change (1 = lundi … 7 = dimanche ; 0 = inconnu)
    val codePostal: String,
    val tauxTaxesPerso: Double,   // < 0 : taux automatique selon la région
    val portions: Int,
    val dejeuners: Boolean,
    val diners: Boolean,
    val soupers: Boolean,
    val preferences: String,
    val ingredientsBase: List<String>,
    val jourRappel: Int,          // 1 = lundi … 7 = dimanche (java.time.DayOfWeek)
    val heureRappel: Int,
    val minuteRappel: Int,
    val heureGeneration: Int,
    val minuteGeneration: Int,
    val genererSansPhotos: Boolean
) {
    val region: Region get() = Region.depuisCodePostal(codePostal)

    /** Clé et modèle du fournisseur choisi. */
    val cleApi: String get() = cles[fournisseur.code].orEmpty()
    val modele: String get() = modeles[fournisseur.code]?.takeIf { it.isNotBlank() } ?: fournisseur.modeleDefaut
    val iaConfiguree: Boolean
        get() = modele.isNotBlank() && (cleApi.isNotBlank() || fournisseur == Fournisseur.AUTRE) &&
            (fournisseur != Fournisseur.AUTRE || urlPerso.isNotBlank())

    fun configIA() = ConfigIA(fournisseur, cleApi, modele, urlPerso, reflexion)

    fun avecCle(cle: String) = copy(cles = cles + (fournisseur.code to cle.trim()))
    fun avecModele(m: String) = copy(modeles = modeles + (fournisseur.code to m.trim()))

    /** Taux appliqué aux articles taxables : celui de l'utilisateur, sinon celui de la région. */
    val tauxTaxes: Double get() = if (tauxTaxesPerso >= 0) tauxTaxesPerso else region.tauxTaxes

    val epicerieChoisie: Boolean get() = marchand.isNotBlank() && codePostal.isNotBlank()

    fun memeHoraire(autre: EtatReglages): Boolean =
        jourRappel == autre.jourRappel && heureRappel == autre.heureRappel &&
            minuteRappel == autre.minuteRappel && heureGeneration == autre.heureGeneration &&
            minuteGeneration == autre.minuteGeneration
}

/** Réglages et petit état persistant, stockés dans les SharedPreferences privées de l'app. */
class Reglages(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("reglages", Context.MODE_PRIVATE)

    // --- Réglages modifiables ---
    var langue: Langue
        get() = Langue.depuisCode(p.getString("langue", null))
        set(v) { p.edit().putString("langue", v.code).apply() }

    var fournisseur: Fournisseur
        get() = Fournisseur.depuisCode(p.getString("fournisseur", null))
        set(v) { p.edit().putString("fournisseur", v.code).apply() }

    /** Clé d'un fournisseur (l'ancienne clé unique est celle de DeepSeek). */
    fun cle(f: Fournisseur): String =
        p.getString("cle_api_${f.code}", null) ?: if (f == Fournisseur.DEEPSEEK) p.getString("cle_api", "") ?: "" else ""

    fun modele(f: Fournisseur): String =
        p.getString("modele_${f.code}", null) ?: if (f == Fournisseur.DEEPSEEK) p.getString("modele", "") ?: "" else ""

    var urlPerso: String
        get() = p.getString("url_perso", "") ?: ""
        set(v) { p.edit().putString("url_perso", v.trim()).apply() }

    var reflexion: Boolean
        get() = p.getBoolean("reflexion", false)
        set(v) { p.edit().putBoolean("reflexion", v).apply() }

    var marchand: String
        get() = p.getString("marchand", "") ?: ""
        set(v) { p.edit().putString("marchand", v.trim()).apply() }

    var marchandId: Long
        get() = p.getLong("marchand_id", 0L)
        set(v) { p.edit().putLong("marchand_id", v).apply() }

    var marchandLogo: String
        get() = p.getString("marchand_logo", "") ?: ""
        set(v) { p.edit().putString("marchand_logo", v).apply() }

    var jourDebutCirculaire: Int
        get() = p.getInt("jour_debut_circulaire", 0)
        set(v) { p.edit().putInt("jour_debut_circulaire", v).apply() }

    var codePostal: String
        get() = p.getString("code_postal", "") ?: ""
        set(v) { p.edit().putString("code_postal", Region.normaliserCodePostal(v)).apply() }

    var tauxTaxesPerso: Double
        get() = p.getFloat("taux_taxes_perso", -1f).toDouble()
        set(v) { p.edit().putFloat("taux_taxes_perso", v.toFloat()).apply() }

    val region: Region get() = Region.depuisCodePostal(codePostal)
    val tauxTaxes: Double get() = if (tauxTaxesPerso >= 0) tauxTaxesPerso else region.tauxTaxes

    var portions: Int
        get() = p.getInt("portions", 2)
        set(v) { p.edit().putInt("portions", v.coerceIn(1, 12)).apply() }

    var dejeuners: Boolean
        get() = p.getBoolean("dejeuners", false)
        set(v) { p.edit().putBoolean("dejeuners", v).apply() }

    var diners: Boolean
        get() = p.getBoolean("diners", false)
        set(v) { p.edit().putBoolean("diners", v).apply() }

    var soupers: Boolean
        get() = p.getBoolean("soupers", true)
        set(v) { p.edit().putBoolean("soupers", v).apply() }

    var preferences: String
        get() = p.getString("preferences", "") ?: ""
        set(v) { p.edit().putString("preferences", v).apply() }

    var ingredientsBase: List<String>
        get() {
            val brut = p.getString("ingredients_base", null) ?: return ingredientsBaseDefaut(langue)
            return brut.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        }
        set(v) { p.edit().putString("ingredients_base", v.joinToString("\n")).apply() }

    var jourRappel: Int
        get() = p.getInt("jour_rappel", DayOfWeek.FRIDAY.value)
        set(v) { p.edit().putInt("jour_rappel", v.coerceIn(1, 7)).apply() }

    var heureRappel: Int
        get() = p.getInt("heure_rappel", 19)
        set(v) { p.edit().putInt("heure_rappel", v).apply() }

    var minuteRappel: Int
        get() = p.getInt("minute_rappel", 0)
        set(v) { p.edit().putInt("minute_rappel", v).apply() }

    // 00 h 30 : dans la nuit du vendredi au samedi, et en heures creuses chez DeepSeek
    // (pointe = 01-04 h et 06-10 h UTC, soit 21 h-00 h et 02-06 h à Québec en été).
    var heureGeneration: Int
        get() = p.getInt("heure_generation", 0)
        set(v) { p.edit().putInt("heure_generation", v).apply() }

    var minuteGeneration: Int
        get() = p.getInt("minute_generation", 30)
        set(v) { p.edit().putInt("minute_generation", v).apply() }

    var genererSansPhotos: Boolean
        get() = p.getBoolean("generer_sans_photos", true)
        set(v) { p.edit().putBoolean("generer_sans_photos", v).apply() }

    // --- État interne ---
    var derniereGenerationMs: Long
        get() = p.getLong("derniere_generation", 0L)
        set(v) { p.edit().putLong("derniere_generation", v).apply() }

    var dernierRappelMs: Long
        get() = p.getLong("dernier_rappel", 0L)
        set(v) { p.edit().putLong("dernier_rappel", v).apply() }

    var derniereErreur: String
        get() = p.getString("derniere_erreur", "") ?: ""
        set(v) { p.edit().putString("derniere_erreur", v).apply() }

    /** Noms des repas des dernières semaines, pour éviter les répétitions. */
    var repasRecents: List<String>
        get() = (p.getString("repas_recents", "") ?: "").split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        set(v) { p.edit().putString("repas_recents", v.joinToString("\n")).apply() }

    /** Articles cochés dans la liste d'épicerie courante (index). */
    var coches: Set<Int>
        get() = (p.getStringSet("coches", emptySet()) ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet()
        set(v) { p.edit().putStringSet("coches", v.map { it.toString() }.toSet()).apply() }

    fun lireEtat() = EtatReglages(
        langue = langue,
        fournisseur = fournisseur,
        cles = Fournisseur.entries.associate { it.code to cle(it) },
        modeles = Fournisseur.entries.associate { it.code to modele(it) },
        urlPerso = urlPerso,
        reflexion = reflexion,
        marchand = marchand,
        marchandId = marchandId,
        marchandLogo = marchandLogo,
        jourDebutCirculaire = jourDebutCirculaire,
        codePostal = codePostal,
        tauxTaxesPerso = tauxTaxesPerso,
        portions = portions,
        dejeuners = dejeuners,
        diners = diners,
        soupers = soupers,
        preferences = preferences,
        ingredientsBase = ingredientsBase,
        jourRappel = jourRappel,
        heureRappel = heureRappel,
        minuteRappel = minuteRappel,
        heureGeneration = heureGeneration,
        minuteGeneration = minuteGeneration,
        genererSansPhotos = genererSansPhotos
    )

    fun ecrire(e: EtatReglages) {
        val ed = p.edit()
        e.cles.forEach { (code, cle) -> ed.putString("cle_api_$code", cle.trim()) }
        e.modeles.forEach { (code, m) -> ed.putString("modele_$code", m.trim()) }
        ed
            .putString("langue", e.langue.code)
            .putString("fournisseur", e.fournisseur.code)
            .putString("url_perso", e.urlPerso.trim())
            .putBoolean("reflexion", e.reflexion)
            .putString("marchand", e.marchand.trim())
            .putLong("marchand_id", e.marchandId)
            .putString("marchand_logo", e.marchandLogo)
            .putInt("jour_debut_circulaire", e.jourDebutCirculaire)
            .putString("code_postal", Region.normaliserCodePostal(e.codePostal))
            .putFloat("taux_taxes_perso", e.tauxTaxesPerso.toFloat())
            .putInt("portions", e.portions.coerceIn(1, 12))
            .putBoolean("dejeuners", e.dejeuners)
            .putBoolean("diners", e.diners)
            .putBoolean("soupers", e.soupers)
            .putString("preferences", e.preferences)
            .putString("ingredients_base", e.ingredientsBase.joinToString("\n"))
            .putInt("jour_rappel", e.jourRappel.coerceIn(1, 7))
            .putInt("heure_rappel", e.heureRappel)
            .putInt("minute_rappel", e.minuteRappel)
            .putInt("heure_generation", e.heureGeneration)
            .putInt("minute_generation", e.minuteGeneration)
            .putBoolean("generer_sans_photos", e.genererSansPhotos)
            .apply()
    }
}

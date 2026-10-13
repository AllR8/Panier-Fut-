package ca.panierfute.data

import android.content.Context
import java.time.DayOfWeek

/** Ingrédients toujours disponibles à la maison, même s'ils n'apparaissent pas sur les photos. */
val INGREDIENTS_BASE_DEFAUT = listOf(
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
    val cleApi: String,
    val modele: String,
    val reflexion: Boolean,
    val marchand: String,
    val codePostal: String,
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
    fun memeHoraire(autre: EtatReglages): Boolean =
        jourRappel == autre.jourRappel && heureRappel == autre.heureRappel &&
            minuteRappel == autre.minuteRappel && heureGeneration == autre.heureGeneration &&
            minuteGeneration == autre.minuteGeneration
}

/** Réglages et petit état persistant, stockés dans les SharedPreferences privées de l'app. */
class Reglages(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("reglages", Context.MODE_PRIVATE)

    // --- Réglages modifiables ---
    var cleApi: String
        get() = p.getString("cle_api", "") ?: ""
        set(v) { p.edit().putString("cle_api", v.trim()).apply() }

    var modele: String
        get() = (p.getString("modele", null) ?: "").ifBlank { "deepseek-flash" }
        set(v) { p.edit().putString("modele", v.trim()).apply() }

    var reflexion: Boolean
        get() = p.getBoolean("reflexion", false)
        set(v) { p.edit().putBoolean("reflexion", v).apply() }

    var marchand: String
        get() = (p.getString("marchand", null) ?: "").ifBlank { "Maxi" }
        set(v) { p.edit().putString("marchand", v.trim()).apply() }

    var codePostal: String
        get() = (p.getString("code_postal", null) ?: "").ifBlank { "G1S1T7" }
        set(v) { p.edit().putString("code_postal", v.replace(" ", "").uppercase()).apply() }

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
            val brut = p.getString("ingredients_base", null) ?: return INGREDIENTS_BASE_DEFAUT
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
        cleApi = cleApi,
        modele = modele,
        reflexion = reflexion,
        marchand = marchand,
        codePostal = codePostal,
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
        p.edit()
            .putString("cle_api", e.cleApi.trim())
            .putString("modele", e.modele.trim())
            .putBoolean("reflexion", e.reflexion)
            .putString("marchand", e.marchand.trim())
            .putString("code_postal", e.codePostal.replace(" ", "").uppercase())
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

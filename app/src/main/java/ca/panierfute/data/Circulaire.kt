package ca.panierfute.data

import ca.panierfute.i18n.Textes
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.text.Collator
import java.time.LocalDate
import java.time.OffsetDateTime

data class ArticleCirculaire(
    val nom: String,
    val marque: String?,
    val prix: String?,
    val rabais: String?,
    val validJusquau: LocalDate?
)

data class Circulaire(
    val marchand: String,
    val debut: LocalDate?,
    val fin: LocalDate?,
    val titres: List<String>,
    val articles: List<ArticleCirculaire>
) {
    /** Version compacte (une ligne par article) envoyée à l'IA : peu de jetons. */
    fun enTexte(): String = buildString {
        for (a in articles) {
            append("- ").append(a.nom)
            if (a.marque != null && !a.nom.contains(a.marque, ignoreCase = true)) append(" [").append(a.marque).append("]")
            if (a.prix != null) append(" | ").append(a.prix).append(" \$")
            if (a.rabais != null) append(" | ").append(a.rabais)
            if (a.validJusquau != null && fin != null && a.validJusquau.isBefore(fin)) {
                append(" | until ").append(a.validJusquau)
            }
            append('\n')
        }
    }
}

/** Une épicerie qui publie une circulaire près d'un code postal. */
data class Epicerie(
    val id: Long,
    val nom: String,
    val logo: String,
    val debut: LocalDate?,      // circulaire en cours (ou la prochaine)
    val fin: LocalDate?,
    val alimentation: Boolean   // catégorie « Groceries » chez Flipp
)

/**
 * Lecture des circulaires via Flipp, le service qui héberge les circulaires
 * numériques des détaillants au Canada et aux États-Unis. API publique mais non
 * officielle : si Flipp la modifie, c'est ce fichier qu'il faudra ajuster.
 */
object SourceCirculaire {
    private const val BASE = "https://backflipp.wishabi.com/flipp"

    private data class Candidate(
        val id: Long,
        val titre: String,
        val marchandId: Long,
        val marchand: String,
        val logo: String,
        val alimentation: Boolean,
        val debut: OffsetDateTime?,
        val fin: OffsetDateTime?
    )

    /** Langue des circulaires : français au Québec si l'app est en français, sinon anglais. */
    private fun localeFlipp(codePostal: String, langue: Langue): String {
        val region = Region.depuisCodePostal(codePostal)
        return when {
            region.pays == "US" -> "en-us"
            langue == Langue.FR -> "fr-ca"
            else -> "en-ca"
        }
    }

    private fun lireListe(codePostal: String, langue: Langue): List<Candidate> {
        val cp = Region.normaliserCodePostal(codePostal)
        val url = "$BASE/flyers?locale=${localeFlipp(cp, langue)}&postal_code=${URLEncoder.encode(cp, "UTF-8")}"
        val maintenant = OffsetDateTime.now()
        return JSONObject(Http.get(url)).optJSONArray("flyers").objets()
            .map {
                Candidate(
                    id = it.optLong("id"),
                    titre = it.txt("name"),
                    marchandId = it.optLong("merchant_id"),
                    marchand = it.txt("merchant").trim(),
                    logo = it.txt("merchant_logo").replaceFirst("http://", "https://"),
                    alimentation = it.optJSONArray("categories").textes()
                        .any { c -> c.equals("Groceries", ignoreCase = true) },
                    debut = date(it.txt("valid_from")),
                    fin = date(it.txt("valid_to"))
                )
            }
            .filter { it.id > 0 && it.marchand.isNotEmpty() && (it.fin == null || it.fin.isAfter(maintenant)) }
    }

    /** Épiceries ayant une circulaire près de ce code postal (alimentation d'abord). */
    fun listerEpiceries(codePostal: String, langue: Langue): List<Epicerie> {
        val maintenant = OffsetDateTime.now()
        val collator = Collator.getInstance(langue.locale)
        return lireListe(codePostal, langue)
            .groupBy { if (it.marchandId > 0) it.marchandId.toString() else it.marchand.lowercase() }
            .values
            .map { flyers ->
                val alimentation = flyers.any { it.alimentation }
                val pertinents = flyers.filter { it.alimentation == alimentation }
                // La circulaire en cours, sinon la prochaine.
                val courante = pertinents
                    .filter { it.debut == null || !it.debut.isAfter(maintenant) }
                    .minByOrNull { it.fin ?: maintenant.plusYears(1) }
                    ?: pertinents.minByOrNull { it.debut ?: maintenant }
                val premier = flyers.first()
                Epicerie(
                    id = premier.marchandId,
                    nom = premier.marchand,
                    logo = flyers.firstOrNull { it.logo.isNotEmpty() }?.logo ?: "",
                    debut = courante?.debut?.toLocalDate(),
                    fin = courante?.fin?.toLocalDate(),
                    alimentation = alimentation
                )
            }
            .sortedWith(compareBy<Epicerie> { !it.alimentation }.thenComparator { a, b -> collator.compare(a.nom, b.nom) })
    }

    fun charger(r: EtatReglages, t: Textes): Circulaire {
        val marchand = r.marchand.trim()
        if (marchand.isEmpty() || r.codePostal.isBlank()) throw IOException(t.erreurAucuneEpicerie)
        val maintenant = OffsetDateTime.now()

        val candidates = lireListe(r.codePostal, r.langue).filter {
            if (r.marchandId > 0) it.marchandId == r.marchandId else it.marchand.equals(marchand, ignoreCase = true)
        }
        if (candidates.isEmpty()) {
            throw IOException(t.erreurCirculaireIntrouvable(marchand, Region.afficherCodePostal(r.codePostal)))
        }
        // Si l'épicerie a une circulaire d'alimentation, on ignore ses autres circulaires (jouets, etc.).
        val utiles = if (candidates.any { it.alimentation }) candidates.filter { it.alimentation } else candidates

        // Circulaires en vigueur maintenant ; si elles se terminent dans moins de 2 jours
        // et que la suivante est déjà publiée, on prend plutôt la suivante.
        val actives = utiles.filter { it.debut == null || !it.debut.isAfter(maintenant.plusHours(12)) }
        val aVenir = utiles.filter { it.debut != null && it.debut.isAfter(maintenant.plusHours(12)) }
        val finActives = actives.mapNotNull { it.fin }.maxOrNull()
        val retenues = when {
            actives.isNotEmpty() && (finActives == null || finActives.isAfter(maintenant.plusDays(2))) -> actives
            aVenir.isNotEmpty() -> {
                val premier = aVenir.mapNotNull { it.debut }.minOrNull()!!
                aVenir.filter { it.debut != null && !it.debut.isAfter(premier.plusDays(1)) }
            }
            else -> actives
        }

        val locale = localeFlipp(r.codePostal, r.langue)
        val vus = HashSet<String>()
        val articles = ArrayList<ArticleCirculaire>()
        for (c in retenues) {
            val detail = JSONObject(Http.get("$BASE/flyers/${c.id}?locale=$locale"))
            for (item in detail.optJSONArray("items").objets()) {
                val nom = item.txtOuNull("name") ?: continue
                val prix = item.txtOuNull("price") ?: item.txtOuNull("current_price")
                val rabais = item.txtOuNull("discount")
                val cleUnique = (nom + "|" + (prix ?: "")).lowercase()
                if (!vus.add(cleUnique)) continue
                articles.add(
                    ArticleCirculaire(
                        nom = nom.replace(Regex("\\s+"), " "),
                        marque = item.txtOuNull("brand"),
                        prix = prix,
                        rabais = rabais,
                        validJusquau = date(item.txt("valid_to"))?.toLocalDate()
                    )
                )
            }
        }
        if (articles.isEmpty()) throw IOException(t.erreurCirculaireVide(marchand))

        return Circulaire(
            marchand = marchand,
            debut = retenues.mapNotNull { it.debut }.minOrNull()?.toLocalDate(),
            fin = retenues.mapNotNull { it.fin }.maxOrNull()?.toLocalDate(),
            titres = retenues.map { it.titre },
            articles = articles
        )
    }

    private fun date(s: String): OffsetDateTime? =
        if (s.isBlank()) null else try { OffsetDateTime.parse(s) } catch (e: Exception) { null }
}

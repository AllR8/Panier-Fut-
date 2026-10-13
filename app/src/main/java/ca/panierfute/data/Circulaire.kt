package ca.panierfute.data

import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
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
            if (a.rabais != null) append(" | rabais ").append(a.rabais)
            if (a.validJusquau != null && fin != null && a.validJusquau.isBefore(fin)) {
                append(" | jusqu'au ").append(a.validJusquau)
            }
            append('\n')
        }
    }
}

/**
 * Lecture de la circulaire via Flipp, le service qui héberge les circulaires
 * numériques des épiciers canadiens (dont Maxi). API publique mais non officielle :
 * si Flipp la modifie, c'est ce fichier qu'il faudra ajuster.
 */
object SourceCirculaire {
    private const val BASE = "https://backflipp.wishabi.com/flipp"

    fun charger(codePostal: String, marchand: String): Circulaire {
        val cp = codePostal.replace(" ", "").uppercase()
        val cpUrl = URLEncoder.encode(cp, "UTF-8")
        val liste = JSONObject(Http.get("$BASE/flyers?locale=fr-ca&postal_code=$cpUrl"))
        val maintenant = OffsetDateTime.now()

        data class Candidate(val id: Long, val titre: String, val debut: OffsetDateTime?, val fin: OffsetDateTime?)

        val candidates = liste.optJSONArray("flyers").objets()
            .filter { it.txt("merchant").equals(marchand.trim(), ignoreCase = true) }
            .map {
                Candidate(
                    id = it.optLong("id"),
                    titre = it.txt("name"),
                    debut = date(it.txt("valid_from")),
                    fin = date(it.txt("valid_to"))
                )
            }
            .filter { it.id > 0 && (it.fin == null || it.fin.isAfter(maintenant)) }

        if (candidates.isEmpty()) {
            throw IOException("Aucune circulaire « $marchand » trouvée pour le code postal $cp.")
        }

        // Circulaires en vigueur maintenant ; si elles se terminent dans moins de 2 jours
        // et que la suivante est déjà publiée, on prend plutôt la suivante.
        val actives = candidates.filter { it.debut == null || !it.debut.isAfter(maintenant.plusHours(12)) }
        val aVenir = candidates.filter { it.debut != null && it.debut.isAfter(maintenant.plusHours(12)) }
        val finActives = actives.mapNotNull { it.fin }.maxOrNull()
        val retenues = when {
            actives.isNotEmpty() && (finActives == null || finActives.isAfter(maintenant.plusDays(2))) -> actives
            aVenir.isNotEmpty() -> {
                val premier = aVenir.mapNotNull { it.debut }.minOrNull()!!
                aVenir.filter { it.debut != null && !it.debut.isAfter(premier.plusDays(1)) }
            }
            else -> actives
        }

        val vus = HashSet<String>()
        val articles = ArrayList<ArticleCirculaire>()
        for (c in retenues) {
            val detail = JSONObject(Http.get("$BASE/flyers/${c.id}?locale=fr-ca"))
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
        if (articles.isEmpty()) throw IOException("La circulaire « $marchand » est vide ou illisible.")

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

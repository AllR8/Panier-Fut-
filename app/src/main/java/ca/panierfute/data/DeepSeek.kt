package ca.panierfute.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.ZoneOffset
import java.time.ZonedDateTime

data class ReponseIA(
    val contenu: String,
    val jetonsEntree: Int,
    val jetonsCache: Int,
    val jetonsSortie: Int,
    val coutUsd: Double
)

/**
 * Client minimal de l'API DeepSeek (format compatible OpenAI).
 * Modèle par défaut : deepseek-flash (DeepSeek-V4.1-Flash), qui accepte les images.
 */
object DeepSeek {
    private const val URL_CHAT = "https://api.deepseek.com/chat/completions"
    private const val URL_SOLDE = "https://api.deepseek.com/user/balance"

    // Tarifs deepseek-flash en USD par million de jetons, heures creuses (pointe = ×2).
    // Source : api-docs.deepseek.com, octobre 2026. Sert seulement à l'affichage du coût.
    private const val PRIX_CACHE = 0.003
    private const val PRIX_ENTREE = 0.15
    private const val PRIX_SORTIE = 0.60

    fun completer(
        cle: String,
        modele: String,
        systeme: String,
        contenuUtilisateur: JSONArray,
        reflexion: Boolean,
        maxJetons: Int = 16_000
    ): ReponseIA {
        val corps = JSONObject()
            .put("model", modele)
            .put(
                "messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systeme))
                    .put(JSONObject().put("role", "user").put("content", contenuUtilisateur))
            )
            .put("response_format", JSONObject().put("type", "json_object"))
            .put("max_tokens", maxJetons)
            .put("stream", false)
        if (!reflexion) {
            // Mode réflexion désactivé : moins de jetons de sortie, donc moins cher.
            corps.put("thinking", JSONObject().put("type", "disabled"))
            corps.put("temperature", 0.7)
        }

        val entetes = mapOf("Authorization" to "Bearer $cle")
        val texte = try {
            Http.postJson(URL_CHAT, corps.toString(), entetes, lectureMs = 8 * 60_000)
        } catch (e: ErreurHttp) {
            if (e.code != 400 && e.code != 422) throw IOException(messageErreur(e), e)
            // Paramètre optionnel refusé (mode json, réflexion…) : un essai avec la requête minimale.
            corps.remove("response_format")
            corps.remove("thinking")
            corps.remove("temperature")
            try {
                Http.postJson(URL_CHAT, corps.toString(), entetes, lectureMs = 8 * 60_000)
            } catch (e2: ErreurHttp) {
                throw IOException(messageErreur(e2), e2)
            }
        }

        val rep = JSONObject(texte)
        val choix = rep.optJSONArray("choices").objets().firstOrNull()
            ?: throw IOException("Réponse DeepSeek sans contenu.")
        if (choix.txt("finish_reason") == "length") {
            throw IOException("La réponse de l'IA a été coupée (trop longue). Réessaie.")
        }
        val contenu = choix.optJSONObject("message")?.txt("content") ?: ""

        val usage = rep.optJSONObject("usage") ?: JSONObject()
        val entree = usage.entier("prompt_tokens")
        val cache = usage.entier("prompt_cache_hit_tokens")
        val horsCache = usage.nombre("prompt_cache_miss_tokens")?.toInt() ?: (entree - cache)
        val sortie = usage.entier("completion_tokens")
        return ReponseIA(contenu, entree, cache, sortie, estimerCout(cache, horsCache, sortie))
    }

    /** Vérifie la clé et retourne le solde du compte (appel gratuit). */
    fun solde(cle: String): String {
        val texte = try {
            Http.get(URL_SOLDE, mapOf("Authorization" to "Bearer $cle"))
        } catch (e: ErreurHttp) {
            throw IOException(messageErreur(e), e)
        }
        val json = JSONObject(texte)
        val soldes = json.optJSONArray("balance_infos").objets()
            .joinToString(" · ") { "${it.txt("total_balance")} ${it.txt("currency")}" }
        val dispo = if (json.has("is_available") && !json.booleen("is_available")) " (solde insuffisant)" else ""
        return if (soldes.isBlank()) "Clé valide$dispo" else "Clé valide — solde : $soldes$dispo"
    }

    fun estimerCout(cache: Int, horsCache: Int, sortie: Int): Double {
        val h = ZonedDateTime.now(ZoneOffset.UTC).hour
        val pointe = h in 1..3 || h in 6..9
        val facteur = if (pointe) 2.0 else 1.0
        return facteur * (cache * PRIX_CACHE + horsCache * PRIX_ENTREE + sortie * PRIX_SORTIE) / 1_000_000.0
    }

    private fun messageErreur(e: ErreurHttp): String = when (e.code) {
        400 -> "Requête refusée par DeepSeek (400). Vérifie le nom du modèle dans Réglages. Détail : ${e.corps.take(200)}"
        401 -> "Clé API DeepSeek invalide (401). Vérifie-la dans Réglages."
        402 -> "Solde DeepSeek insuffisant (402). Recharge ton compte sur platform.deepseek.com."
        422 -> "Paramètres refusés par DeepSeek (422) : ${e.corps.take(200)}"
        429 -> "Trop de requêtes chez DeepSeek (429). Réessaie dans quelques minutes."
        in 500..599 -> "Serveurs DeepSeek indisponibles (${e.code}). Réessaie plus tard."
        else -> "Erreur DeepSeek ${e.code} : ${e.corps.take(200)}"
    }
}

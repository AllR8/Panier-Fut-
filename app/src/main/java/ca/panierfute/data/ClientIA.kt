package ca.panierfute.data

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import ca.panierfute.i18n.Textes
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.ZoneOffset
import java.time.ZonedDateTime

/** Format de requête de l'API. */
enum class FormatApi { OPENAI, ANTHROPIC }

/**
 * Fournisseurs d'IA offerts. Tous sauf Anthropic utilisent le format « chat completions »
 * d'OpenAI ; « Autre » accepte n'importe quelle API compatible (Groq, xAI, Together,
 * un serveur local, etc.). Les modèles par défaut sont des suggestions modifiables.
 */
enum class Fournisseur(
    val code: String,
    val nom: String,
    val url: String,
    val modeleDefaut: String,
    val format: FormatApi,
    val siteCle: String
) {
    DEEPSEEK("deepseek", "DeepSeek", "https://api.deepseek.com/chat/completions", "deepseek-flash", FormatApi.OPENAI, "platform.deepseek.com"),
    OPENAI("openai", "OpenAI", "https://api.openai.com/v1/chat/completions", "gpt-6-luna", FormatApi.OPENAI, "platform.openai.com"),
    ANTHROPIC("anthropic", "Claude", "https://api.anthropic.com/v1/messages", "claude-haiku-5-5", FormatApi.ANTHROPIC, "platform.claude.com"),
    GEMINI("gemini", "Gemini", "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", "gemini-3.8-flash", FormatApi.OPENAI, "aistudio.google.com"),
    MISTRAL("mistral", "Mistral", "https://api.mistral.ai/v1/chat/completions", "mistral-small-latest", FormatApi.OPENAI, "console.mistral.ai"),
    OPENROUTER("openrouter", "OpenRouter", "https://openrouter.ai/api/v1/chat/completions", "openrouter/auto", FormatApi.OPENAI, "openrouter.ai"),
    AUTRE("autre", "", "", "", FormatApi.OPENAI, "");

    companion object {
        fun depuisCode(code: String?): Fournisseur = entries.firstOrNull { it.code == code } ?: DEEPSEEK
    }
}

/** Ce qu'il faut pour appeler l'IA. */
data class ConfigIA(
    val fournisseur: Fournisseur,
    val cle: String,
    val modele: String,
    val urlPerso: String,
    val reflexion: Boolean
) {
    val url: String get() = if (fournisseur == Fournisseur.AUTRE) urlPerso.trim() else fournisseur.url
}

/** Morceau du message envoyé à l'IA : du texte ou une photo JPEG. */
sealed class PartieDemande {
    data class Texte(val texte: String) : PartieDemande()
    class Image(val jpeg: ByteArray) : PartieDemande()
}

data class ReponseIA(
    val contenu: String,
    val coupee: Boolean,          // la limite de jetons a été atteinte
    val jetonsEntree: Int,
    val jetonsSortie: Int,
    val jetonsReflexion: Int,
    val coutUsd: Double?          // connu seulement pour DeepSeek
)

/** Client minimal, sans dépendance, pour tous les fournisseurs. */
object ClientIA {

    fun completer(
        config: ConfigIA,
        systeme: String,
        parties: List<PartieDemande>,
        maxJetons: Int,
        formatJson: Boolean,
        t: Textes
    ): ReponseIA {
        if (config.cle.isBlank() && config.fournisseur != Fournisseur.AUTRE) throw IOException(t.erreurCleManquante)
        if (config.url.isBlank()) throw IOException(t.erreurUrlManquante)
        return when (config.fournisseur.format) {
            FormatApi.ANTHROPIC -> completerAnthropic(config, systeme, parties, maxJetons, t)
            FormatApi.OPENAI -> completerOpenAI(config, systeme, parties, maxJetons, formatJson, t)
        }
    }

    // ---------------------------------------------------------------------
    // Format OpenAI (DeepSeek, OpenAI, Gemini, Mistral, OpenRouter, autres)
    // ---------------------------------------------------------------------

    private fun completerOpenAI(
        config: ConfigIA,
        systeme: String,
        parties: List<PartieDemande>,
        maxJetons: Int,
        formatJson: Boolean,
        t: Textes
    ): ReponseIA {
        val f = config.fournisseur
        val contenu = JSONArray()
        for (p in parties) {
            when (p) {
                is PartieDemande.Texte -> contenu.put(JSONObject().put("type", "text").put("text", p.texte))
                is PartieDemande.Image -> contenu.put(
                    JSONObject().put("type", "image_url").put(
                        "image_url",
                        JSONObject().put("url", "data:image/jpeg;base64," + Base64.encodeToString(p.jpeg, Base64.NO_WRAP))
                    )
                )
            }
        }
        val corps = JSONObject()
            .put("model", config.modele)
            .put(
                "messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systeme))
                    .put(JSONObject().put("role", "user").put("content", contenu))
            )
            .put("stream", false)
        // OpenAI a remplacé max_tokens par max_completion_tokens ; les autres gardent max_tokens.
        var cleMax = if (f == Fournisseur.OPENAI) "max_completion_tokens" else "max_tokens"
        corps.put(cleMax, maxJetons)

        // Paramètres facultatifs : retirés si l'API les refuse.
        val facultatifs = mutableListOf<String>()
        fun facultatif(cle: String, valeur: Any) {
            corps.put(cle, valeur)
            facultatifs.add(cle)
        }
        if (formatJson) facultatif("response_format", JSONObject().put("type", "json_object"))
        when (f) {
            Fournisseur.DEEPSEEK -> {
                // La réflexion est ACTIVÉE par défaut chez DeepSeek : on la désactive
                // explicitement, sinon elle consomme des milliers de jetons de sortie.
                corps.put("thinking", JSONObject().put("type", if (config.reflexion) "enabled" else "disabled"))
                if (!config.reflexion) facultatif("temperature", 0.7)
            }
            Fournisseur.OPENAI, Fournisseur.GEMINI -> facultatif("reasoning_effort", "low")
            else -> facultatif("temperature", 0.7)
        }

        val entetes = mapOf("Authorization" to "Bearer ${config.cle}")
        var texte: String? = null
        var derniere: ErreurHttp? = null
        for (essai in 1..3) {
            try {
                texte = Http.postJson(config.url, corps.toString(), entetes, lectureMs = 8 * 60_000)
                break
            } catch (e: ErreurHttp) {
                derniere = e
                if (e.code != 400 && e.code != 422) break
                when {
                    facultatifs.isNotEmpty() -> {
                        facultatifs.forEach { corps.remove(it) }
                        facultatifs.clear()
                    }
                    e.corps.contains("max_tokens") || e.corps.contains("max_completion_tokens") -> {
                        // Mauvais nom de paramètre pour la limite : on essaie l'autre.
                        corps.remove(cleMax)
                        cleMax = if (cleMax == "max_tokens") "max_completion_tokens" else "max_tokens"
                        corps.put(cleMax, maxJetons)
                    }
                    else -> break
                }
            }
        }
        if (texte == null) throw IOException(messageErreur(derniere, config, t), derniere)

        val rep = JSONObject(texte)
        val choix = rep.optJSONArray("choices").objets().firstOrNull() ?: throw IOException(t.erreurIASansReponse)
        val coupee = choix.txt("finish_reason") == "length"
        val message = choix.optJSONObject("message") ?: JSONObject()
        val sortieTexte = when (val c = message.opt("content")) {
            is String -> c
            is JSONArray -> c.objets().joinToString("") { it.txt("text") }
            else -> ""
        }

        val usage = rep.optJSONObject("usage") ?: JSONObject()
        val entree = usage.entier("prompt_tokens")
        val sortie = usage.entier("completion_tokens")
        val reflexionJetons = usage.optJSONObject("completion_tokens_details")?.entier("reasoning_tokens") ?: 0
        val cout = if (f == Fournisseur.DEEPSEEK) {
            val cache = usage.entier("prompt_cache_hit_tokens")
            val horsCache = usage.nombre("prompt_cache_miss_tokens")?.toInt() ?: (entree - cache)
            coutDeepSeek(cache, horsCache, sortie)
        } else null
        return ReponseIA(sortieTexte, coupee, entree, sortie, reflexionJetons, cout)
    }

    // ---------------------------------------------------------------------
    // Format Anthropic (API Messages de Claude)
    // ---------------------------------------------------------------------

    private fun completerAnthropic(
        config: ConfigIA,
        systeme: String,
        parties: List<PartieDemande>,
        maxJetons: Int,
        t: Textes
    ): ReponseIA {
        val contenu = JSONArray()
        for (p in parties) {
            when (p) {
                is PartieDemande.Texte -> contenu.put(JSONObject().put("type", "text").put("text", p.texte))
                is PartieDemande.Image -> contenu.put(
                    JSONObject().put("type", "image").put(
                        "source",
                        JSONObject()
                            .put("type", "base64")
                            .put("media_type", "image/jpeg")
                            .put("data", Base64.encodeToString(p.jpeg, Base64.NO_WRAP))
                    )
                )
            }
        }
        val corps = JSONObject()
            .put("model", config.modele)
            .put("max_tokens", maxJetons)
            .put("system", systeme)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", contenu)))
        val entetes = mapOf(
            "x-api-key" to config.cle,
            "anthropic-version" to "2023-06-01"
        )
        val texte = try {
            Http.postJson(config.url, corps.toString(), entetes, lectureMs = 8 * 60_000)
        } catch (e: ErreurHttp) {
            throw IOException(messageErreur(e, config, t), e)
        }
        val rep = JSONObject(texte)
        val sortieTexte = rep.optJSONArray("content").objets()
            .filter { it.txt("type") == "text" }
            .joinToString("") { it.txt("text") }
        val usage = rep.optJSONObject("usage") ?: JSONObject()
        return ReponseIA(
            contenu = sortieTexte,
            coupee = rep.txt("stop_reason") == "max_tokens",
            jetonsEntree = usage.entier("input_tokens") + usage.entier("cache_read_input_tokens") +
                usage.entier("cache_creation_input_tokens"),
            jetonsSortie = usage.entier("output_tokens"),
            jetonsReflexion = 0,
            coutUsd = null
        )
    }

    // ---------------------------------------------------------------------
    // Test de la configuration
    // ---------------------------------------------------------------------

    /**
     * Vérifie la clé, le modèle et la lecture des images avec une toute petite requête
     * (une image blanche de 16 × 16 px). Coût : une fraction de cent.
     */
    fun tester(config: ConfigIA, t: Textes): String {
        val image = ByteArrayOutputStream().use { sortie ->
            val bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, sortie)
            bmp.recycle()
            sortie.toByteArray()
        }
        completer(
            config,
            "Reply with the single word OK.",
            listOf(PartieDemande.Texte("What color is this image? Reply OK."), PartieDemande.Image(image)),
            maxJetons = 256,
            formatJson = false,
            t = t
        )
        val solde = if (config.fournisseur == Fournisseur.DEEPSEEK) soldeDeepSeek(config.cle) else null
        return t.testReussi(config.modele, solde)
    }

    /** Solde du compte DeepSeek (appel gratuit), ou null s'il est illisible. */
    private fun soldeDeepSeek(cle: String): String? = try {
        val json = JSONObject(Http.get("https://api.deepseek.com/user/balance", mapOf("Authorization" to "Bearer $cle")))
        json.optJSONArray("balance_infos").objets()
            .joinToString(", ") { "${it.txt("total_balance")} ${it.txt("currency")}" }
            .ifBlank { null }
    } catch (e: Exception) {
        null
    }

    // ---------------------------------------------------------------------

    // Tarifs deepseek-flash en USD par million de jetons, heures creuses (pointe = ×2).
    // Source : api-docs.deepseek.com, octobre 2026. Sert seulement à l'affichage du coût.
    private const val PRIX_CACHE = 0.003
    private const val PRIX_ENTREE = 0.15
    private const val PRIX_SORTIE = 0.60

    private fun coutDeepSeek(cache: Int, horsCache: Int, sortie: Int): Double {
        val h = ZonedDateTime.now(ZoneOffset.UTC).hour
        val pointe = h in 1..3 || h in 6..9
        val facteur = if (pointe) 2.0 else 1.0
        return facteur * (cache * PRIX_CACHE + horsCache * PRIX_ENTREE + sortie * PRIX_SORTIE) / 1_000_000.0
    }

    private fun messageErreur(e: ErreurHttp?, config: ConfigIA, t: Textes): String {
        val nom = config.fournisseur.nom.ifBlank { t.fournisseurAutre }
        if (e == null) return t.erreurIASansReponse
        return when (e.code) {
            401, 403 -> t.erreurIA401(nom)
            402 -> t.erreurIA402(nom)
            404 -> t.erreurIA404(nom, config.modele)
            429 -> t.erreurIA429(nom)
            in 500..599 -> t.erreurIAServeur(nom, e.code)
            else -> t.erreurIAAutre(nom, e.code, e.corps.take(200))
        }
    }
}

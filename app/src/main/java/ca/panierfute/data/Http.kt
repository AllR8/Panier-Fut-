package ca.panierfute.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Erreur HTTP avec le code et le début du corps de la réponse. */
class ErreurHttp(val code: Int, val corps: String) : IOException("HTTP $code : ${corps.take(300)}")

/**
 * Petit client HTTP sans dépendance (HttpURLConnection).
 * Toutes les fonctions sont bloquantes : à appeler hors du fil principal.
 */
object Http {
    private const val AGENT = "Mozilla/5.0 (Linux; Android 14) PanierFute/1.0"

    fun get(url: String, entetes: Map<String, String> = emptyMap(), delaiMs: Int = 30_000): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "GET"
            c.connectTimeout = delaiMs
            c.readTimeout = delaiMs
            c.setRequestProperty("User-Agent", AGENT)
            c.setRequestProperty("Accept", "application/json")
            for ((cle, valeur) in entetes) c.setRequestProperty(cle, valeur)
            return lire(c)
        } finally {
            c.disconnect()
        }
    }

    fun postJson(
        url: String,
        corps: String,
        entetes: Map<String, String> = emptyMap(),
        lectureMs: Int = 60_000
    ): String {
        val octets = corps.toByteArray(Charsets.UTF_8)
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"
            c.connectTimeout = 30_000
            c.readTimeout = lectureMs
            c.doOutput = true
            c.setFixedLengthStreamingMode(octets.size)
            c.setRequestProperty("User-Agent", AGENT)
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            c.setRequestProperty("Accept", "application/json")
            for ((cle, valeur) in entetes) c.setRequestProperty(cle, valeur)
            c.outputStream.use { it.write(octets) }
            return lire(c)
        } finally {
            c.disconnect()
        }
    }

    private fun lire(c: HttpURLConnection): String {
        val code = c.responseCode
        val flux = if (code in 200..299) c.inputStream else c.errorStream
        val texte = flux?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        if (code !in 200..299) throw ErreurHttp(code, texte)
        return texte
    }
}

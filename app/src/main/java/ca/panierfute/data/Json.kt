package ca.panierfute.data

import org.json.JSONArray
import org.json.JSONObject

// Petites fonctions utilitaires pour lire du JSON de façon tolérante
// (l'IA peut renvoyer un nombre sous forme de texte, un champ manquant, etc.)

/** Texte d'un champ, ou [defaut] s'il est absent ou null. */
fun JSONObject.txt(cle: String, defaut: String = ""): String {
    val v = opt(cle)
    return if (v == null || v == JSONObject.NULL) defaut else v.toString().trim()
}

/** Texte d'un champ, ou null s'il est absent, null ou vide. */
fun JSONObject.txtOuNull(cle: String): String? {
    val t = txt(cle)
    return if (t.isEmpty() || t.equals("null", ignoreCase = true)) null else t
}

/** Nombre décimal d'un champ (accepte « 2,99 », « 2.99 $ », 2.99). */
fun JSONObject.nombre(cle: String): Double? {
    return when (val v = opt(cle)) {
        is Number -> v.toDouble()
        is String -> v.replace(",", ".").replace(Regex("[^0-9.]"), "").toDoubleOrNull()
        else -> null
    }
}

fun JSONObject.entier(cle: String, defaut: Int = 0): Int = nombre(cle)?.toInt() ?: defaut

fun JSONObject.booleen(cle: String): Boolean {
    return when (val v = opt(cle)) {
        is Boolean -> v
        is Number -> v.toInt() != 0
        is String -> v.equals("true", ignoreCase = true) || v.equals("oui", ignoreCase = true)
        else -> false
    }
}

/** Liste des objets d'un tableau (ignore les éléments qui ne sont pas des objets). */
fun JSONArray?.objets(): List<JSONObject> {
    if (this == null) return emptyList()
    val resultat = ArrayList<JSONObject>(length())
    for (i in 0 until length()) optJSONObject(i)?.let { resultat.add(it) }
    return resultat
}

/** Liste des textes non vides d'un tableau. */
fun JSONArray?.textes(): List<String> {
    if (this == null) return emptyList()
    val resultat = ArrayList<String>(length())
    for (i in 0 until length()) {
        val v = opt(i)
        if (v == null || v == JSONObject.NULL) continue
        val t = v.toString().trim()
        if (t.isNotEmpty()) resultat.add(t)
    }
    return resultat
}

/**
 * Extrait l'objet JSON d'une réponse de l'IA, même si elle l'a entouré
 * de ```json ... ``` ou de texte.
 */
fun extraireJson(texte: String): JSONObject {
    val t = texte.trim()
        .removePrefix("```json").removePrefix("```JSON").removePrefix("```")
        .removeSuffix("```")
        .trim()
    return try {
        JSONObject(t)
    } catch (e: Exception) {
        val debut = t.indexOf('{')
        val fin = t.lastIndexOf('}')
        if (debut >= 0 && fin > debut) JSONObject(t.substring(debut, fin + 1)) else throw e
    }
}

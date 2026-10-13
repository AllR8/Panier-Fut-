package ca.panierfute.data

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class ResultatGeneration {
    data class Succes(val plan: Plan) : ResultatGeneration()
    data class Ignore(val raison: String) : ResultatGeneration()
}

/**
 * Le cœur de l'app : UN SEUL appel à l'IA par génération.
 * Photos + ingrédients de base + circulaire → inventaire, repas de la semaine, liste d'épicerie.
 */
object GenerateurPlan {
    private val FR = Locale.CANADA_FRENCH
    private val FORMAT_JOUR = DateTimeFormatter.ofPattern("EEEE d MMM", FR)
    private val FORMAT_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM", FR)

    suspend fun generer(
        ctx: Context,
        manuel: Boolean,
        progres: suspend (String) -> Unit
    ): ResultatGeneration {
        val r = Reglages(ctx)
        if (r.cleApi.isBlank()) {
            throw IllegalStateException("Aucune clé API DeepSeek. Ajoute-la dans l'onglet Réglages.")
        }
        val photos = Photos.toutes(ctx)
        if (photos.isEmpty() && !manuel && !r.genererSansPhotos) {
            return ResultatGeneration.Ignore("Aucune photo reçue cette semaine.")
        }

        progres("Lecture de la circulaire ${r.marchand}…")
        val circulaire = withContext(Dispatchers.IO) { SourceCirculaire.charger(r.codePostal, r.marchand) }

        val maintenant = LocalDateTime.now()
        // Généré la nuit ou le matin : le plan commence aujourd'hui ; sinon, demain.
        val debut = if (maintenant.hour < 14) maintenant.toLocalDate() else maintenant.toLocalDate().plusDays(1)
        val jours = (0L until 7L).map { debut.plusDays(it).format(FORMAT_JOUR) }

        progres("Préparation de ${photos.size} photo(s)…")
        val contenu = withContext(Dispatchers.IO) { construireContenu(r, circulaire, photos, jours, maintenant) }
        val systeme = construireSysteme(jours, r.marchand)

        progres("Analyse par l'IA… (1 à 3 minutes)")
        var reponse = withContext(Dispatchers.IO) {
            DeepSeek.completer(r.cleApi, r.modele, systeme, contenu, r.reflexion)
        }
        var cout = reponse.coutUsd
        val premier = lireReponse(reponse.contenu)
        val json: JSONObject
        if (premier != null) {
            json = premier
        } else {
            // Rare : contenu vide ou JSON invalide. Un seul nouvel essai.
            progres("Réponse incomplète, nouvel essai…")
            reponse = withContext(Dispatchers.IO) {
                DeepSeek.completer(r.cleApi, r.modele, systeme, contenu, r.reflexion)
            }
            cout += reponse.coutUsd
            json = lireReponse(reponse.contenu)
                ?: throw IllegalStateException("L'IA n'a pas renvoyé de plan lisible. Réessaie plus tard.")
        }

        progres("Enregistrement du plan…")
        val meta = JSONObject()
            .put("genere_le", System.currentTimeMillis())
            .put("debut_plan", debut.toString())
            .put("circulaire_debut", circulaire.debut?.toString() ?: JSONObject.NULL)
            .put("circulaire_fin", circulaire.fin?.toString() ?: JSONObject.NULL)
            .put("nb_articles_circulaire", circulaire.articles.size)
            .put("nb_photos", photos.size)
            .put("cout_usd", cout)
            .put("jetons_entree", reponse.jetonsEntree)
            .put("jetons_sortie", reponse.jetonsSortie)
            .put("modele", r.modele)
        withContext(Dispatchers.IO) {
            StockagePlan.sauver(ctx, meta, json)
            Photos.viderTout(ctx) // les photos ont été analysées : on repart à neuf la semaine prochaine
        }

        val nouveauxRepas = json.optJSONArray("repas").objets().map { it.txt("nom") }.filter { it.isNotBlank() }
        r.repasRecents = (nouveauxRepas + r.repasRecents).distinct().take(28)
        r.derniereGenerationMs = System.currentTimeMillis()
        r.coches = emptySet()
        r.derniereErreur = ""

        val plan = StockagePlan.charger(ctx)
            ?: throw IllegalStateException("Le plan reçu n'a pas pu être enregistré.")
        return ResultatGeneration.Succes(plan)
    }

    private fun lireReponse(texte: String): JSONObject? {
        if (texte.isBlank()) return null
        return try {
            val j = extraireJson(texte)
            if (j.optJSONArray("repas") == null) null else j
        } catch (e: Exception) {
            null
        }
    }

    private fun construireContenu(
        r: Reglages,
        c: Circulaire,
        photos: List<Pair<Zone, java.io.File>>,
        jours: List<String>,
        maintenant: LocalDateTime
    ): JSONArray {
        val moments = buildList {
            if (r.dejeuners) add("déjeuner")
            if (r.diners) add("dîner")
            if (r.soupers || (!r.dejeuners && !r.diners)) add("souper")
        }
        val periode = when {
            c.debut != null && c.fin != null -> "valide du ${c.debut.format(FORMAT_DATE)} au ${c.fin.format(FORMAT_DATE)}"
            c.fin != null -> "valide jusqu'au ${c.fin.format(FORMAT_DATE)}"
            else -> "de la semaine"
        }

        val texte = buildString {
            appendLine("Date et heure : ${maintenant.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy, HH 'h' mm", FR))}.")
            appendLine("Jours à planifier (dans l'ordre) : ${jours.joinToString(" ; ")}.")
            appendLine("Repas à planifier chaque jour : ${moments.joinToString(", ")}.")
            appendLine("Portions par repas : ${r.portions}.")
            val pref = r.preferences.trim()
            appendLine("Préférences et contraintes du ménage : ${if (pref.isEmpty()) "aucune particulière" else pref}.")
            val recents = r.repasRecents.take(14)
            if (recents.isNotEmpty()) {
                appendLine("Repas des semaines précédentes (ne pas les répéter) : ${recents.joinToString(" ; ")}.")
            }
            appendLine()
            appendLine("INGRÉDIENTS DE BASE toujours disponibles (utilise-les librement ; ne les mets jamais dans l'épicerie ni dans l'inventaire) :")
            appendLine(r.ingredientsBase.joinToString(", "))
            appendLine()
            appendLine("CIRCULAIRE ${c.marchand.uppercase(FR)} (succursale du code postal ${r.codePostal}) $periode. L'épicerie sera faite avant la fin de la circulaire. Format : nom | prix | précisions.")
            append(c.enTexte())
            appendLine()
            if (photos.isEmpty()) {
                appendLine("PHOTOS : aucune cette semaine. Suppose qu'il n'y a que les ingrédients de base ; l'inventaire sera vide.")
            } else {
                appendLine("PHOTOS : ${photos.size} photo(s) suivent, chacune précédée de sa zone (garde-manger, frigo ou congélateur).")
            }
            appendLine()
            append("Produis maintenant le plan en json, exactement selon le schéma demandé.")
        }

        val parties = JSONArray()
        parties.put(JSONObject().put("type", "text").put("text", texte))
        photos.forEachIndexed { i, (zone, fichier) ->
            parties.put(JSONObject().put("type", "text").put("text", "Photo ${i + 1}/${photos.size} — ${zone.titre}"))
            val b64 = Base64.encodeToString(fichier.readBytes(), Base64.NO_WRAP)
            parties.put(
                JSONObject().put("type", "image_url")
                    .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$b64"))
            )
        }
        return parties
    }

    private fun construireSysteme(jours: List<String>, marchand: String): String = """
Tu es « Panier Futé », un planificateur de repas et d'épicerie économe pour un ménage de la ville de Québec. Tu écris en français québécois naturel et tu utilises les unités métriques (sauf si la circulaire indique des livres).

Tu reçois : des photos du garde-manger, du frigo et du congélateur ; la liste des ingrédients de base toujours disponibles ; les produits de la circulaire de l'épicier.

Ta mission, en une seule réponse :
1. INVENTAIRE — Liste ce que tu vois réellement sur les photos. N'invente rien. Si tu n'es pas certain d'un aliment, ajoute « (?) » à son nom. Marque utiliser_vite = true pour ce qui semble entamé, mûr ou périssable (légumes, restes, viande décongelée, produits laitiers ouverts). Ignore les ingrédients de base.
2. REPAS — Planifie les repas demandés pour chacun des jours. Priorités, dans l'ordre : (a) utiliser ce qui doit être mangé vite, (b) profiter des produits en rabais de la circulaire, (c) utiliser le reste de l'inventaire, (d) acheter le moins possible au prix régulier.
   - Repas réalistes : 40 minutes ou moins en semaine, un peu plus élaborés la fin de semaine.
   - Varie les protéines et les styles ; pas deux fois le même plat.
   - Prévois des restes intelligents quand c'est logique (cuire plus, réutiliser le lendemain) et dis-le dans la description.
   - Les quantités doivent correspondre au nombre de portions.
   - Étapes courtes et concrètes (4 à 7 étapes).
   - Le champ « jour » doit être exactement une de ces valeurs : ${jours.joinToString(" | ")}.
3. ÉPICERIE — Seulement ce qui manque pour réaliser les repas (rien de l'inventaire suffisant, aucun ingrédient de base).
   - Regroupe chaque article dans un de ces rayons : Fruits et légumes | Boulangerie | Viandes et poissons | Produits laitiers et œufs | Épicerie | Surgelés | Autres.
   - Si l'article est dans la circulaire : en_rabais = true, prix_unitaire = le prix de la circulaire, et nomme l'article comme dans la circulaire (en minuscules lisibles).
   - Sinon : en_rabais = false, estime un prix réaliste chez $marchand à Québec et mets prix_estime = true.
   - cout = coût total pour la quantité à acheter (prix_unitaire × nombre d'unités).
   - Un produit non périssable ou congelable en très bon rabais peut être acheté en plus pour faire des réserves : mentionne-le dans remarques.
4. Ignore les produits non alimentaires de la circulaire.

Réponds UNIQUEMENT avec un objet json valide, sans texte autour, selon ce schéma :
{
  "resume": "Deux phrases sur la stratégie de la semaine.",
  "inventaire": [
    {"nom": "poitrines de poulet", "zone": "congelateur", "quantite": "environ 4", "utiliser_vite": false}
  ],
  "repas": [
    {
      "jour": "${jours.first()}",
      "moment": "souper",
      "nom": "Nom appétissant du plat",
      "description": "Une ou deux phrases. Mentionne les restes prévus s'il y a lieu.",
      "portions": 2,
      "temps_min": 35,
      "ingredients": [
        {"nom": "carottes", "quantite": "4", "source": "rabais"},
        {"nom": "poitrines de poulet", "quantite": "2", "source": "inventaire"},
        {"nom": "paprika", "quantite": "1 c. à thé", "source": "base"},
        {"nom": "crème sure", "quantite": "125 ml", "source": "achat"}
      ],
      "etapes": ["Étape courte.", "Étape courte."],
      "rabais_utilises": ["CAROTTES, SAC DE 3 LB"]
    }
  ],
  "epicerie": [
    {"article": "Carottes, sac de 3 lb", "quantite": "1 sac", "rayon": "Fruits et légumes", "en_rabais": true, "prix_unitaire": 2.49, "cout": 2.49, "prix_estime": false, "pour": ["samedi souper", "mardi souper"]}
  ],
  "economies_estimees": 12.50,
  "remarques": ["Conseil ou mise en garde utile (photo floue, achat à stocker, etc.)."]
}
Valeurs permises pour « source » : inventaire, rabais, base, achat. Valeurs permises pour « zone » : garde-manger, frigo, congelateur.
""".trim()
}

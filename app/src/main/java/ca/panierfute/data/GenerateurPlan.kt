package ca.panierfute.data

import android.content.Context
import ca.panierfute.i18n.Textes
import ca.panierfute.i18n.textes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
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
    // Les consignes à l'IA sont en anglais ; ses réponses sont dans la langue de l'utilisateur.
    private val EN = Locale.ENGLISH
    private val FORMAT_JOUR = DateTimeFormatter.ofPattern("EEEE", EN)
    private val FORMAT_DATE = DateTimeFormatter.ofPattern("EEEE MMMM d", EN)

    suspend fun generer(
        ctx: Context,
        manuel: Boolean,
        consignes: String = "",
        progres: suspend (String) -> Unit
    ): ResultatGeneration {
        val r = Reglages(ctx)
        val e = r.lireEtat()
        val t = textes(e.langue)
        if (!e.iaConfiguree) throw IllegalStateException(t.erreurCleManquante)
        if (!e.epicerieChoisie) throw IllegalStateException(t.erreurAucuneEpicerie)
        val photos = Photos.toutes(ctx)
        if (photos.isEmpty() && !manuel && !e.genererSansPhotos) {
            return ResultatGeneration.Ignore(t.aucunePhotoSemaine)
        }

        // Régénération manuelle sans nouvelles photos : on réutilise l'inventaire du plan
        // actuel (avec les corrections de l'utilisateur) au lieu de repartir de zéro.
        val planActuel = if (manuel) {
            withContext(Dispatchers.IO) { StockagePlan.charger(ctx) }
                ?.takeIf { System.currentTimeMillis() - it.genereLe < 7L * 24 * 3600 * 1000 }
        } else null
        val inventaireConnu = if (photos.isEmpty()) planActuel?.inventaire?.takeIf { it.isNotEmpty() } else null
        val repasRemplaces = planActuel?.repas?.map { it.nom }.orEmpty()

        progres(t.progresCirculaire(e.marchand))
        val circulaire = withContext(Dispatchers.IO) { SourceCirculaire.charger(e, t) }

        val maintenant = LocalDateTime.now()
        // Généré la nuit ou le matin : le plan commence aujourd'hui ; sinon, demain.
        val debut = if (maintenant.hour < 14) maintenant.toLocalDate() else maintenant.toLocalDate().plusDays(1)
        val jours = (0L until 7L).map { debut.plusDays(it) }

        progres(if (photos.isEmpty()) t.progresDemande else t.progresPhotos(photos.size))
        val contenu = withContext(Dispatchers.IO) {
            construireContenu(e, r.repasRecents, circulaire, photos, jours, maintenant, inventaireConnu, consignes.trim(), repasRemplaces)
        }
        val systeme = construireSysteme(jours, e)

        progres(t.progresIA)
        // Au plus deux essais. Le 2e (rare) a plus d'espace, demande d'être plus bref et,
        // si le 1er a été coupé, se passe du mode JSON strict (qui peut boucler sur des espaces).
        val tentatives = ArrayList<ReponseIA>()
        var trouve: JSONObject? = null
        for (essai in 1..2) {
            val precedente = tentatives.lastOrNull()
            if (precedente != null) {
                progres(if (precedente.coupee) t.progresNouvelEssaiLong else t.progresNouvelEssai)
            }
            // On ne paie que les jetons réellement générés : une limite haute ne coûte rien de plus.
            val limite = when {
                e.fournisseur == Fournisseur.DEEPSEEK -> if (essai == 1 && !e.reflexion) 32_000 else 64_000
                else -> if (essai == 1) 16_000 else 32_000
            }
            val strict = precedente == null || !precedente.coupee
            val instructions = if (precedente == null) systeme else systeme + RAPPEL_CONCIS
            val rep = withContext(Dispatchers.IO) {
                ClientIA.completer(e.configIA(), instructions, contenu, limite, strict, t)
            }
            tentatives.add(rep)
            trouve = if (rep.coupee) null else lireReponse(rep.contenu)
            if (trouve != null) break
        }
        val json = trouve ?: throw IllegalStateException(diagnostic(tentatives, t))
        val cout = if (tentatives.all { it.coutUsd != null }) tentatives.sumOf { it.coutUsd ?: 0.0 } else -1.0

        progres(t.progresEnregistrement)
        if (inventaireConnu != null) {
            // L'inventaire confirmé par l'utilisateur reste la référence.
            val tableau = JSONArray()
            inventaireConnu.forEach { tableau.put(it.versJson()) }
            json.put("inventaire", tableau)
        }
        val meta = JSONObject()
            .put("genere_le", System.currentTimeMillis())
            .put("debut_plan", debut.toString())
            .put("circulaire_debut", circulaire.debut?.toString() ?: JSONObject.NULL)
            .put("circulaire_fin", circulaire.fin?.toString() ?: JSONObject.NULL)
            .put("marchand", e.marchand)
            .put("devise", e.region.devise)
            .put("taux_taxes", e.tauxTaxes)
            .put("langue", e.langue.code)
            .put("nb_articles_circulaire", circulaire.articles.size)
            .put("nb_photos", photos.size)
            .put("cout_usd", cout)
            .put("jetons_entree", tentatives.sumOf { it.jetonsEntree })
            .put("jetons_sortie", tentatives.sumOf { it.jetonsSortie })
            .put("fournisseur", e.fournisseur.nom.ifBlank { t.fournisseurAutre })
            .put("modele", e.modele)
            .put("consignes", consignes.trim())
            .put("inventaire_corrige", inventaireConnu != null && planActuel?.inventaireCorrige == true)
        withContext(Dispatchers.IO) {
            StockagePlan.sauver(ctx, meta, json)
            Photos.viderTout(ctx) // les photos ont été analysées : on repart à neuf la semaine prochaine
        }

        val nouveauxRepas = json.optJSONArray("repas").objets().map { it.txt("nom") }.filter { it.isNotBlank() }
        // Les repas remplacés par une régénération n'ont pas été mangés : on les retire de l'historique.
        r.repasRecents = (nouveauxRepas + r.repasRecents.filter { it !in repasRemplaces }).distinct().take(28)
        r.derniereGenerationMs = System.currentTimeMillis()
        r.coches = emptySet()
        r.derniereErreur = ""

        val plan = StockagePlan.charger(ctx) ?: throw IllegalStateException(t.erreurEnregistrement)
        return ResultatGeneration.Succes(plan)
    }

    private const val RAPPEL_CONCIS = "\n\nIMPORTANT: your previous answer was too long or unreadable. " +
        "Be brief: one-sentence descriptions, 3 to 5 short steps per meal, at most 30 inventory items."

    /** Message d'erreur précis, pour savoir ce qui s'est passé si les deux essais échouent. */
    private fun diagnostic(tentatives: List<ReponseIA>, t: Textes): String {
        val d = tentatives.lastOrNull() ?: return t.erreurIASansReponse
        val details = buildList {
            add(if (d.coupee) t.diagnosticCoupee(d.jetonsSortie) else t.diagnosticIllisible(d.contenu.length))
            if (d.jetonsReflexion > 0) add(t.diagnosticReflexion(d.jetonsReflexion))
            if (d.contenu.isNotEmpty() && d.contenu.trim().length < d.contenu.length / 10) add(t.diagnosticEspaces)
        }
        return t.erreurPlanIncomplet(tentatives.size, details.joinToString(", "))
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
        e: EtatReglages,
        repasRecents: List<String>,
        c: Circulaire,
        photos: List<Pair<Zone, java.io.File>>,
        jours: List<LocalDate>,
        maintenant: LocalDateTime,
        inventaireConnu: List<ItemInventaire>?,
        consignes: String,
        repasRemplaces: List<String>
    ): List<PartieDemande> {
        val moments = buildList {
            if (e.dejeuners) add("breakfast")
            if (e.diners) add("lunch")
            if (e.soupers || (!e.dejeuners && !e.diners)) add("dinner")
        }
        val periode = when {
            c.debut != null && c.fin != null -> "valid from ${c.debut.format(FORMAT_DATE)} to ${c.fin.format(FORMAT_DATE)}"
            c.fin != null -> "valid until ${c.fin.format(FORMAT_DATE)}"
            else -> "for this week"
        }

        val texte = buildString {
            appendLine("Now: ${maintenant.format(DateTimeFormatter.ofPattern("EEEE MMMM d yyyy, HH:mm", EN))}.")
            appendLine("Days to plan, in order: ${jours.joinToString("; ") { "${it} (${it.format(FORMAT_JOUR)})" }}.")
            appendLine("Meals to plan each day: ${moments.joinToString(", ")}.")
            appendLine("Servings per meal: ${e.portions}.")
            val pref = e.preferences.trim()
            appendLine("Household preferences and constraints: ${if (pref.isEmpty()) "none in particular" else pref}.")
            val recents = repasRecents.filter { it !in repasRemplaces }.take(14)
            if (recents.isNotEmpty()) {
                appendLine("Meals from previous weeks (do not repeat them): ${recents.joinToString("; ")}.")
            }
            if (repasRemplaces.isNotEmpty()) {
                appendLine("CURRENT PLAN FOR THIS WEEK, which the user wants replaced by a new version (keep one of these meals only if the instructions ask for it): ${repasRemplaces.joinToString("; ")}.")
            }
            if (consignes.isNotEmpty()) {
                appendLine()
                appendLine("USER INSTRUCTIONS FOR THIS VERSION (take priority over everything else, except allergies):")
                appendLine(consignes)
            }
            appendLine()
            appendLine("BASIC INGREDIENTS always at home (use them freely; never put them in the grocery list or the inventory):")
            appendLine(e.ingredientsBase.joinToString(", "))
            appendLine()
            appendLine("FLYER FROM ${c.marchand} (store near postal code ${Region.afficherCodePostal(e.codePostal)}, ${e.region.nom.ifBlank { "Canada" }}), $periode. Shopping will be done before the flyer ends. Format: name | price | details.")
            append(c.enTexte())
            appendLine()
            if (inventaireConnu != null) {
                appendLine("INVENTORY CONFIRMED AND CORRECTED BY THE USER (reliable: use it as is):")
                inventaireConnu.forEach { appendLine(it.enTexte()) }
                appendLine()
                appendLine("PHOTOS: no new photos. Rely on the inventory above and return \"inventaire\": [] (it is already known).")
            } else if (photos.isEmpty()) {
                appendLine("PHOTOS: none this week. Assume only the basic ingredients are available; the inventory will be empty.")
            } else {
                appendLine("PHOTOS: ${photos.size} photo(s) follow, each preceded by its location (pantry, fridge or freezer).")
            }
            appendLine()
            append("Now produce the plan as json, exactly following the requested schema.")
        }

        val parties = ArrayList<PartieDemande>()
        parties.add(PartieDemande.Texte(texte))
        photos.forEachIndexed { i, (zone, fichier) ->
            val lieu = when (zone) {
                Zone.GARDE_MANGER -> "Pantry"
                Zone.FRIGO -> "Fridge"
                Zone.CONGELO -> "Freezer"
            }
            parties.add(PartieDemande.Texte("Photo ${i + 1}/${photos.size}: $lieu"))
            parties.add(PartieDemande.Image(fichier.readBytes()))
        }
        return parties
    }

    private fun regleTaxes(region: Region): String = when (region.pays) {
        "CA" -> "true only for items subject to sales tax in ${region.nom}: soft drinks, sparkling water, candy and confectionery, chips and snack foods, alcohol, non-food products. Basic groceries (fruit, vegetables, meat, fish, eggs, bread, dairy, cereals, pasta, rice, canned goods, etc.) are zero-rated in Canada: taxable = false."
        "US" -> "true for items normally subject to sales tax in ${region.nom} (usually soft drinks, candy, prepared foods and non-food products; groceries are often exempt or taxed at a reduced rate)."
        else -> "true for soft drinks, candy, snack foods, alcohol and non-food products; false for basic groceries."
    }

    private fun construireSysteme(jours: List<LocalDate>, e: EtatReglages): String = """
You are "Panier Futé", a thrifty meal and grocery planner for one household. Write every user-facing text value (meal names, descriptions, steps, ingredient names and quantities, grocery articles and quantities, "pour", inventory names, "resume", "remarques") in ${e.langue.nomAnglais}. Use metric units unless the flyer uses pounds. Keep json keys and code values exactly as specified below.

You receive: photos of the pantry, fridge and freezer (or an inventory already confirmed by the user); the basic ingredients always at home; this week's flyer from the household's grocery store (${e.marchand}); sometimes one-off instructions from the user.

In a single answer:
1. INVENTORY: list only what you actually see in the photos. Never invent anything. If unsure about an item, add "(?)" to its name. Set utiliser_vite = true for what looks opened, ripe or perishable (vegetables, leftovers, thawed meat, opened dairy). Ignore basic ingredients. At most 40 items: group small things (e.g. assorted condiments).
2. MEALS: plan the requested meals for each day. Priorities, in order: (a) use what must be eaten soon, (b) take advantage of the flyer's sale items, (c) use the rest of the inventory, (d) buy as little as possible at regular price.
   - Realistic meals: 40 minutes or less on weekdays, a bit more elaborate on weekends.
   - Vary proteins and styles; never the same dish twice.
   - Plan smart leftovers when it makes sense (cook more, reuse the next day) and say so in the description.
   - Quantities must match the number of servings.
   - 3 to 6 steps, one sentence each.
   - "date" must be exactly one of: ${jours.joinToString(" | ")}. "moment" must be one of: breakfast, lunch, dinner.
3. GROCERY LIST: only what is missing to make the meals (nothing the inventory already covers, no basic ingredients).
   - "rayon" must be one of these codes: produce, bakery, meat_fish, dairy_eggs, pantry, frozen, other.
   - If the item is in the flyer: en_rabais = true, prix_unitaire = the flyer price, and name the article as in the flyer (in readable case, translated if needed).
   - Otherwise: en_rabais = false, estimate a realistic regular price at ${e.marchand} in ${e.region.nom.ifBlank { "Canada" }} and set prix_estime = true.
   - cout = total cost of the quantity to buy (prix_unitaire × number of units), before tax.
   - taxable = ${regleTaxes(e.region)}
   - A non-perishable or freezable item at a very good sale price may be bought extra to stock up: mention it in remarques.
   - "pour" lists the meals the item is for, e.g. the weekday and the meal.
4. Ignore non-food items in the flyer.

Be concise: no superfluous text. Answer ONLY with one valid json object, with no text around it, following this schema:
{
  "resume": "<two sentences on this week's strategy>",
  "inventaire": [
    {"nom": "<item>", "zone": "freezer", "quantite": "<about 4>", "utiliser_vite": false}
  ],
  "repas": [
    {
      "date": "${jours.first()}",
      "moment": "dinner",
      "nom": "<appetizing dish name>",
      "description": "<one sentence; mention planned leftovers if any>",
      "portions": ${e.portions},
      "temps_min": 35,
      "ingredients": [
        {"nom": "<ingredient>", "quantite": "<4>", "source": "sale"},
        {"nom": "<ingredient>", "quantite": "<2>", "source": "inventory"},
        {"nom": "<ingredient>", "quantite": "<1 tsp>", "source": "basic"},
        {"nom": "<ingredient>", "quantite": "<125 ml>", "source": "buy"}
      ],
      "etapes": ["<short step>", "<short step>"]
    }
  ],
  "epicerie": [
    {"article": "<article as in the flyer>", "quantite": "<1 bag>", "rayon": "produce", "en_rabais": true, "prix_unitaire": 2.49, "cout": 2.49, "prix_estime": false, "taxable": false, "pour": ["<weekday meal>"]}
  ],
  "economies_estimees": 12.50,
  "remarques": ["<useful tip or warning: blurry photo, item worth stocking up on, etc.>"]
}
Allowed values for "source": inventory, sale, basic, buy. Allowed values for "zone": pantry, fridge, freezer.
""".trim()
}

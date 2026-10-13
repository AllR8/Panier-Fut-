package ca.panierfute.data

import android.content.Context
import org.json.JSONObject
import java.io.File

data class IngredientRepas(val nom: String, val quantite: String, val source: String)

data class Repas(
    val jour: String,
    val moment: String,
    val nom: String,
    val description: String,
    val portions: Int,
    val tempsMin: Int,
    val ingredients: List<IngredientRepas>,
    val etapes: List<String>,
    val rabaisUtilises: List<String>
)

data class ArticleEpicerie(
    val index: Int,
    val article: String,
    val quantite: String,
    val rayon: String,
    val enRabais: Boolean,
    val prixUnitaire: Double?,
    val cout: Double?,
    val prixEstime: Boolean,
    val pour: List<String>
)

data class ItemInventaire(val nom: String, val zone: String, val quantite: String, val utiliserVite: Boolean)

data class Plan(
    val genereLe: Long,
    val debutPlan: String,
    val circulaireDebut: String?,
    val circulaireFin: String?,
    val nbArticlesCirculaire: Int,
    val nbPhotos: Int,
    val coutUsd: Double,
    val jetonsEntree: Int,
    val jetonsSortie: Int,
    val resume: String,
    val repas: List<Repas>,
    val epicerie: List<ArticleEpicerie>,
    val inventaire: List<ItemInventaire>,
    val economiesEstimees: Double?,
    val remarques: List<String>
) {
    val totalEstime: Double get() = epicerie.sumOf { it.cout ?: it.prixUnitaire ?: 0.0 }
}

/** Ordre des rayons (proche d'un parcours d'épicerie typique). */
val ORDRE_RAYONS = listOf(
    "Fruits et légumes", "Boulangerie", "Viandes et poissons", "Produits laitiers et œufs",
    "Épicerie", "Surgelés", "Autres"
)

fun indexRayon(rayon: String): Int {
    val i = ORDRE_RAYONS.indexOfFirst { it.equals(rayon.trim(), ignoreCase = true) }
    return if (i >= 0) i else ORDRE_RAYONS.size
}

/** Enregistre et relit le plan de la semaine (un seul fichier JSON). */
object StockagePlan {
    private fun fichier(ctx: Context) = File(ctx.filesDir, "plan.json")

    fun sauver(ctx: Context, meta: JSONObject, ia: JSONObject) {
        val tmp = File(ctx.filesDir, "plan.json.tmp")
        tmp.writeText(JSONObject().put("meta", meta).put("ia", ia).toString(), Charsets.UTF_8)
        val dest = fichier(ctx)
        if (!tmp.renameTo(dest)) {
            dest.writeText(tmp.readText(Charsets.UTF_8), Charsets.UTF_8)
            tmp.delete()
        }
    }

    fun charger(ctx: Context): Plan? {
        val f = fichier(ctx)
        if (!f.exists()) return null
        return try {
            analyser(JSONObject(f.readText(Charsets.UTF_8)))
        } catch (e: Exception) {
            null
        }
    }

    private fun analyser(racine: JSONObject): Plan {
        val meta = racine.optJSONObject("meta") ?: JSONObject()
        val ia = racine.optJSONObject("ia") ?: JSONObject()

        val repas = ia.optJSONArray("repas").objets().map { r ->
            Repas(
                jour = r.txt("jour"),
                moment = r.txt("moment", "souper"),
                nom = r.txt("nom", "Repas"),
                description = r.txt("description"),
                portions = r.entier("portions", 0),
                tempsMin = r.entier("temps_min", 0),
                ingredients = r.optJSONArray("ingredients").objets().map { i ->
                    IngredientRepas(i.txt("nom"), i.txt("quantite"), i.txt("source").lowercase())
                },
                etapes = r.optJSONArray("etapes").textes(),
                rabaisUtilises = r.optJSONArray("rabais_utilises").textes()
            )
        }

        val epicerie = ia.optJSONArray("epicerie").objets().mapIndexed { index, a ->
            ArticleEpicerie(
                index = index,
                article = a.txt("article", "?"),
                quantite = a.txt("quantite"),
                rayon = a.txt("rayon", "Autres").ifBlank { "Autres" },
                enRabais = a.booleen("en_rabais"),
                prixUnitaire = a.nombre("prix_unitaire") ?: a.nombre("prix"),
                cout = a.nombre("cout"),
                prixEstime = a.booleen("prix_estime"),
                pour = a.optJSONArray("pour").textes()
            )
        }

        val inventaire = ia.optJSONArray("inventaire").objets().map { i ->
            ItemInventaire(i.txt("nom"), i.txt("zone"), i.txt("quantite"), i.booleen("utiliser_vite"))
        }

        return Plan(
            genereLe = meta.optLong("genere_le"),
            debutPlan = meta.txt("debut_plan"),
            circulaireDebut = meta.txtOuNull("circulaire_debut"),
            circulaireFin = meta.txtOuNull("circulaire_fin"),
            nbArticlesCirculaire = meta.entier("nb_articles_circulaire"),
            nbPhotos = meta.entier("nb_photos"),
            coutUsd = meta.nombre("cout_usd") ?: 0.0,
            jetonsEntree = meta.entier("jetons_entree"),
            jetonsSortie = meta.entier("jetons_sortie"),
            resume = ia.txt("resume"),
            repas = repas,
            epicerie = epicerie,
            inventaire = inventaire,
            economiesEstimees = ia.nombre("economies_estimees"),
            remarques = ia.optJSONArray("remarques").textes()
        )
    }
}

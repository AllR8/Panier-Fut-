package ca.panierfute.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.Normalizer
import java.time.LocalDate

/** Taux par défaut des anciens plans (Québec : TPS 5 % + TVQ 9,975 %). */
private const val TAUX_TAXES_ANCIEN = 0.14975

/** Texte en minuscules sans accents, pour reconnaître les valeurs renvoyées par l'IA. */
private fun simplifier(s: String): String =
    Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

/** D'où vient un ingrédient d'une recette. */
enum class Source {
    STOCK, RABAIS, BASE, ACHAT;

    companion object {
        fun depuis(s: String): Source {
            val v = simplifier(s)
            return when {
                v.startsWith("inv") || v.startsWith("stock") || v.startsWith("home") -> STOCK
                v.startsWith("rabais") || v.startsWith("sale") || v.startsWith("special") || v.startsWith("promo") -> RABAIS
                v.startsWith("base") || v.startsWith("basic") || v.startsWith("staple") -> BASE
                else -> ACHAT
            }
        }
    }
}

/** Moment de la journée. */
enum class Moment(val code: String) {
    DEJEUNER("breakfast"), DINER("lunch"), SOUPER("dinner");

    companion object {
        fun depuis(s: String): Moment {
            val v = simplifier(s)
            return when {
                v.startsWith("breakfast") || v.startsWith("dejeuner") || v.startsWith("desayuno") -> DEJEUNER
                v.startsWith("lunch") || v.startsWith("diner") || v.startsWith("almuerzo") || v.startsWith("comida") -> DINER
                else -> SOUPER
            }
        }
    }
}

/** Rayons, dans l'ordre d'un parcours d'épicerie typique. */
enum class Rayon(val code: String) {
    FRUITS_LEGUMES("produce"),
    BOULANGERIE("bakery"),
    VIANDES("meat_fish"),
    LAITIERS("dairy_eggs"),
    EPICERIE("pantry"),
    SURGELES("frozen"),
    AUTRES("other");

    companion object {
        fun depuis(s: String): Rayon {
            val v = simplifier(s)
            entries.firstOrNull { it.code == v }?.let { return it }
            return when {
                // « Produits laitiers » avant « produce » : les deux commencent par « produ ».
                v.contains("laitier") || v.startsWith("dairy") || v.startsWith("lacteo") -> LAITIERS
                v.startsWith("fruit") || v.startsWith("produ") || v.contains("legume") -> FRUITS_LEGUMES
                v.startsWith("boulang") || v.startsWith("bake") || v.startsWith("panad") -> BOULANGERIE
                v.startsWith("viande") || v.startsWith("meat") || v.startsWith("carne") || v.contains("poisson") -> VIANDES
                v.startsWith("epicerie") || v.startsWith("pantry") || v.startsWith("despensa") || v.startsWith("grocery") -> EPICERIE
                v.startsWith("surgel") || v.startsWith("frozen") || v.startsWith("congel") -> SURGELES
                else -> AUTRES
            }
        }
    }
}

/** Endroit de la maison (inventaire). Codes compatibles avec les anciens plans. */
enum class Endroit(val code: String) {
    GARDE_MANGER("garde-manger"), FRIGO("frigo"), CONGELATEUR("congelateur"), AUTRE("autre");

    companion object {
        fun depuis(s: String): Endroit {
            val v = simplifier(s)
            return when {
                v.startsWith("garde") || v.startsWith("pantry") || v.startsWith("despensa") || v.startsWith("armoire") -> GARDE_MANGER
                v.startsWith("frig") || v.startsWith("fridge") || v.startsWith("refri") || v.startsWith("nevera") -> FRIGO
                v.startsWith("cong") || v.startsWith("freez") -> CONGELATEUR
                else -> AUTRE
            }
        }
    }
}

data class IngredientRepas(val nom: String, val quantite: String, val source: Source)

data class Repas(
    val date: LocalDate?,       // nouveaux plans : date ISO
    val jour: String,           // anciens plans : texte libre
    val moment: Moment,
    val nom: String,
    val description: String,
    val portions: Int,
    val tempsMin: Int,
    val ingredients: List<IngredientRepas>,
    val etapes: List<String>
) {
    val utiliseRabais: Boolean get() = ingredients.any { it.source == Source.RABAIS }
}

data class ArticleEpicerie(
    val index: Int,
    val article: String,
    val quantite: String,
    val rayon: Rayon,
    val enRabais: Boolean,
    val prixUnitaire: Double?,
    val cout: Double?,
    val prixEstime: Boolean,
    val taxable: Boolean,
    val pour: List<String>
) {
    val montant: Double get() = cout ?: prixUnitaire ?: 0.0
}

data class ItemInventaire(val nom: String, val endroit: Endroit, val quantite: String, val utiliserVite: Boolean) {
    fun versJson(): JSONObject = JSONObject()
        .put("nom", nom)
        .put("zone", endroit.code)
        .put("quantite", quantite)
        .put("utiliser_vite", utiliserVite)

    /** Ligne lisible envoyée à l'IA. */
    fun enTexte(): String = buildString {
        append("- ").append(nom)
        if (quantite.isNotBlank()) append(", ").append(quantite)
        append(" (").append(
            when (endroit) {
                Endroit.GARDE_MANGER -> "pantry"
                Endroit.FRIGO -> "fridge"
                Endroit.CONGELATEUR -> "freezer"
                Endroit.AUTRE -> "other"
            }
        ).append(")")
        if (utiliserVite) append(" [use soon]")
    }
}

data class Plan(
    val genereLe: Long,
    val debutPlan: LocalDate?,
    val circulaireDebut: LocalDate?,
    val circulaireFin: LocalDate?,
    val marchand: String,
    val devise: String,
    val tauxTaxes: Double,
    val nbArticlesCirculaire: Int,
    val nbPhotos: Int,
    val coutUsd: Double?,        // null si le fournisseur ne publie pas de tarif connu de l'app
    val jetonsEntree: Int,
    val jetonsSortie: Int,
    val fournisseur: String,
    val resume: String,
    val repas: List<Repas>,
    val epicerie: List<ArticleEpicerie>,
    val inventaire: List<ItemInventaire>,
    val economiesEstimees: Double?,
    val remarques: List<String>,
    val inventaireCorrige: Boolean,
    val consignes: String
) {
    val sousTotal: Double get() = epicerie.sumOf { it.montant }
    val taxes: Double get() = epicerie.filter { it.taxable }.sumOf { it.montant } * tauxTaxes
    /** Total estimé, taxes incluses. */
    val totalEstime: Double get() = sousTotal + taxes
    val nbTaxables: Int get() = epicerie.count { it.taxable }
    val nbRabais: Int get() = epicerie.count { it.enRabais }
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

    /** Remplace l'inventaire du plan courant (corrections de l'utilisateur). */
    fun majInventaire(ctx: Context, items: List<ItemInventaire>) {
        val f = fichier(ctx)
        if (!f.exists()) return
        val racine = JSONObject(f.readText(Charsets.UTF_8))
        val meta = racine.optJSONObject("meta") ?: JSONObject()
        val ia = racine.optJSONObject("ia") ?: JSONObject()
        val tableau = JSONArray()
        items.forEach { tableau.put(it.versJson()) }
        ia.put("inventaire", tableau)
        meta.put("inventaire_corrige", true)
        sauver(ctx, meta, ia)
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

    private fun dateIso(s: String?): LocalDate? =
        if (s.isNullOrBlank()) null else try { LocalDate.parse(s.trim().take(10)) } catch (e: Exception) { null }

    fun analyser(racine: JSONObject): Plan {
        val meta = racine.optJSONObject("meta") ?: JSONObject()
        val ia = racine.optJSONObject("ia") ?: JSONObject()

        val repas = ia.optJSONArray("repas").objets().map { r ->
            Repas(
                date = dateIso(r.txtOuNull("date")),
                jour = r.txt("jour"),
                moment = Moment.depuis(r.txt("moment", "dinner")),
                nom = r.txt("nom", "?"),
                description = r.txt("description"),
                portions = r.entier("portions", 0),
                tempsMin = r.entier("temps_min", 0),
                ingredients = r.optJSONArray("ingredients").objets().map { i ->
                    IngredientRepas(i.txt("nom"), i.txt("quantite"), Source.depuis(i.txt("source")))
                },
                etapes = r.optJSONArray("etapes").textes()
            )
        }

        val epicerie = ia.optJSONArray("epicerie").objets().mapIndexed { index, a ->
            ArticleEpicerie(
                index = index,
                article = a.txt("article", "?"),
                quantite = a.txt("quantite"),
                rayon = Rayon.depuis(a.txt("rayon", "other")),
                enRabais = a.booleen("en_rabais"),
                prixUnitaire = a.nombre("prix_unitaire") ?: a.nombre("prix"),
                cout = a.nombre("cout"),
                prixEstime = a.booleen("prix_estime"),
                taxable = a.booleen("taxable"),
                pour = a.optJSONArray("pour").textes()
            )
        }

        val inventaire = ia.optJSONArray("inventaire").objets().map { i ->
            ItemInventaire(i.txt("nom"), Endroit.depuis(i.txt("zone")), i.txt("quantite"), i.booleen("utiliser_vite"))
        }

        return Plan(
            genereLe = meta.optLong("genere_le"),
            debutPlan = dateIso(meta.txtOuNull("debut_plan")),
            circulaireDebut = dateIso(meta.txtOuNull("circulaire_debut")),
            circulaireFin = dateIso(meta.txtOuNull("circulaire_fin")),
            marchand = meta.txt("marchand", "Maxi"),
            devise = meta.txt("devise", "CAD"),
            tauxTaxes = meta.nombre("taux_taxes") ?: TAUX_TAXES_ANCIEN,
            nbArticlesCirculaire = meta.entier("nb_articles_circulaire"),
            nbPhotos = meta.entier("nb_photos"),
            coutUsd = (meta.opt("cout_usd") as? Number)?.toDouble()?.takeIf { it >= 0 },
            jetonsEntree = meta.entier("jetons_entree"),
            jetonsSortie = meta.entier("jetons_sortie"),
            fournisseur = meta.txt("fournisseur", "DeepSeek"),
            resume = ia.txt("resume"),
            repas = repas,
            epicerie = epicerie,
            inventaire = inventaire,
            economiesEstimees = ia.nombre("economies_estimees"),
            remarques = ia.optJSONArray("remarques").textes(),
            inventaireCorrige = meta.booleen("inventaire_corrige"),
            consignes = meta.txt("consignes")
        )
    }
}

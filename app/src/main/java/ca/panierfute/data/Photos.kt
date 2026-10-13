package ca.panierfute.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Les trois endroits à photographier. */
enum class Zone(val cle: String, val titre: String, val emoji: String, val conseil: String) {
    GARDE_MANGER("garde_manger", "Garde-manger", "🥫", "Ouvre les portes, une photo par tablette si besoin."),
    FRIGO("frigo", "Frigo", "🥬", "N'oublie pas la porte et les tiroirs à légumes."),
    CONGELO("congelo", "Congélateur", "❄️", "Sors les sacs du fond si tu peux.");

    companion object {
        fun depuisCle(cle: String?): Zone? = entries.firstOrNull { it.cle == cle }
    }
}

/**
 * Stockage des photos dans le dossier privé de l'app.
 * Chaque photo est redimensionnée (1280 px max) et recompressée en JPEG :
 * c'est suffisant pour que l'IA lise les étiquettes, et l'envoi reste léger.
 */
object Photos {
    const val MAX_PAR_ZONE = 5
    private const val COTE_MAX = 1280
    private const val QUALITE = 82

    private fun dossier(ctx: Context, zone: Zone): File =
        File(ctx.filesDir, "photos/${zone.cle}").apply { mkdirs() }

    fun lister(ctx: Context, zone: Zone): List<File> =
        dossier(ctx, zone).listFiles()
            ?.filter { it.isFile && it.name.endsWith(".jpg") }
            ?.sortedBy { it.name }
            ?: emptyList()

    fun toutes(ctx: Context): List<Pair<Zone, File>> =
        Zone.entries.flatMap { zone -> lister(ctx, zone).map { zone to it } }

    fun parZone(ctx: Context): Map<Zone, List<File>> =
        Zone.entries.associateWith { lister(ctx, it) }

    /** Fichier temporaire où l'appareil photo écrira la prise de vue. */
    fun fichierCapture(ctx: Context): File {
        val d = File(ctx.cacheDir, "captures").apply { mkdirs() }
        return File(d, "capture_${System.currentTimeMillis()}.jpg")
    }

    private fun nouveauFichier(ctx: Context, zone: Zone): File =
        File(dossier(ctx, zone), "${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")

    /** Ajoute la photo prise par l'appareil photo (puis supprime le fichier temporaire). */
    fun ajouterCapture(ctx: Context, zone: Zone, capture: File): Boolean {
        return try {
            if (!capture.exists() || capture.length() == 0L) return false
            compresser(ImageDecoder.createSource(capture), nouveauFichier(ctx, zone), COTE_MAX)
            true
        } catch (e: Exception) {
            false
        } finally {
            capture.delete()
        }
    }

    /** Ajoute des photos choisies dans la galerie. Retourne le nombre ajouté. */
    fun ajouterDepuisGalerie(ctx: Context, zone: Zone, uris: List<Uri>): Int {
        var ajoutees = 0
        for (uri in uris) {
            if (lister(ctx, zone).size >= MAX_PAR_ZONE) break
            try {
                compresser(ImageDecoder.createSource(ctx.contentResolver, uri), nouveauFichier(ctx, zone), COTE_MAX)
                ajoutees++
            } catch (e: Exception) {
                // image illisible : on l'ignore
            }
        }
        return ajoutees
    }

    fun supprimer(fichier: File) {
        fichier.delete()
    }

    fun viderTout(ctx: Context) {
        Zone.entries.forEach { zone -> lister(ctx, zone).forEach { it.delete() } }
    }

    /** Petite version pour l'affichage (null si illisible). */
    fun miniature(fichier: File, cote: Int = 320): Bitmap? = try {
        decoder(ImageDecoder.createSource(fichier), cote)
    } catch (e: Exception) {
        null
    }

    private fun compresser(source: ImageDecoder.Source, destination: File, cote: Int) {
        val bmp = decoder(source, cote)
        destination.parentFile?.mkdirs()
        FileOutputStream(destination).use { bmp.compress(Bitmap.CompressFormat.JPEG, QUALITE, it) }
        bmp.recycle()
    }

    // ImageDecoder applique automatiquement l'orientation EXIF (photos verticales bien tournées)
    // et lit aussi les HEIC du Galaxy.
    private fun decoder(source: ImageDecoder.Source, cote: Int): Bitmap =
        ImageDecoder.decodeBitmap(source) { decodeur, info, _ ->
            decodeur.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val l = info.size.width
            val h = info.size.height
            val echelle = min(1f, cote.toFloat() / max(l, h).toFloat())
            if (echelle < 1f) {
                decodeur.setTargetSize(
                    (l * echelle).roundToInt().coerceAtLeast(1),
                    (h * echelle).roundToInt().coerceAtLeast(1)
                )
            }
        }
}

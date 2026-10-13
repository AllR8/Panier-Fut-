package ca.panierfute.travail

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import ca.panierfute.data.Reglages
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Planification hebdomadaire :
 *  - rappel photos (par défaut vendredi 19 h) ;
 *  - génération automatique (par défaut samedi 00 h 30, donc dans la nuit du vendredi au samedi).
 * Chaque tâche est une tâche unique qui, en fin d'exécution, planifie celle de la semaine suivante.
 */
object Planificateur {
    const val TAG_RAPPEL = "rappel_photos"
    const val TAG_AUTO = "generation_auto"
    const val TAG_GENERATION = "generation"   // commun à l'auto et au manuel (suivi dans l'interface)
    const val TAG_MANUEL = "generation_manuelle"
    private const val NOM_MANUEL = "generation_manuelle"

    private val FORMAT = DateTimeFormatter.ofPattern("EEEE d MMM 'à' HH 'h' mm", Locale.CANADA_FRENCH)

    fun formater(t: ZonedDateTime): String = t.format(FORMAT)

    fun prochainRappel(r: Reglages, maintenant: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime =
        prochaineOccurrence(DayOfWeek.of(r.jourRappel), r.heureRappel, r.minuteRappel, maintenant)

    /** La génération suit le rappel : le lendemain si son heure est plus tôt que celle du rappel. */
    fun prochaineGeneration(r: Reglages, maintenant: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        val minutesRappel = r.heureRappel * 60 + r.minuteRappel
        val minutesGeneration = r.heureGeneration * 60 + r.minuteGeneration
        val jourRappel = DayOfWeek.of(r.jourRappel)
        val jour = if (minutesGeneration <= minutesRappel) jourRappel.plus(1) else jourRappel
        return prochaineOccurrence(jour, r.heureGeneration, r.minuteGeneration, maintenant)
    }

    private fun prochaineOccurrence(jour: DayOfWeek, h: Int, m: Int, maintenant: ZonedDateTime): ZonedDateTime {
        var t = maintenant.with(TemporalAdjusters.nextOrSame(jour))
            .withHour(h).withMinute(m).withSecond(0).withNano(0)
        if (!t.isAfter(maintenant.plusSeconds(30))) t = t.plusWeeks(1)
        return t
    }

    private fun reseau() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    private fun requete(tag: String, r: Reglages): OneTimeWorkRequest {
        val quand = if (tag == TAG_RAPPEL) prochainRappel(r) else prochaineGeneration(r)
        val delai = Duration.between(ZonedDateTime.now(), quand).toMillis().coerceAtLeast(0L)
        return if (tag == TAG_RAPPEL) {
            OneTimeWorkRequestBuilder<RappelWorker>()
                .setInitialDelay(delai, TimeUnit.MILLISECONDS)
                .addTag(TAG_RAPPEL)
                .build()
        } else {
            OneTimeWorkRequestBuilder<GenerationWorker>()
                .setInitialDelay(delai, TimeUnit.MILLISECONDS)
                .setConstraints(reseau())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .addTag(TAG_AUTO)
                .addTag(TAG_GENERATION)
                .build()
        }
    }

    private fun enAttente(info: WorkInfo) =
        info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.BLOCKED

    /**
     * S'assure que le rappel et la génération auto sont planifiés.
     * forcer = true : replanifie (après un changement d'horaire).
     * Bloquant : à appeler hors du fil principal.
     */
    fun assurer(ctx: Context, forcer: Boolean) {
        val wm = WorkManager.getInstance(ctx)
        val r = Reglages(ctx)
        for (tag in listOf(TAG_RAPPEL, TAG_AUTO)) {
            val infos = wm.getWorkInfosByTag(tag).get()
            val attente = infos.filter { enAttente(it) }
            val enCours = infos.any { it.state == WorkInfo.State.RUNNING }
            if (forcer) attente.forEach { wm.cancelWorkById(it.id) }
            if (forcer || (attente.isEmpty() && !enCours)) wm.enqueue(requete(tag, r))
        }
    }

    /** Appelé par une tâche à la fin de son exécution pour planifier la semaine suivante. */
    fun planifierSuivante(ctx: Context, tag: String, moi: UUID) {
        val wm = WorkManager.getInstance(ctx)
        val dejaPrevue = wm.getWorkInfosByTag(tag).get().any { it.id != moi && enAttente(it) }
        if (!dejaPrevue) wm.enqueue(requete(tag, Reglages(ctx)))
    }

    /** Génération lancée à la main (bouton « Générer maintenant »). */
    fun lancerManuel(ctx: Context) {
        val req = OneTimeWorkRequestBuilder<GenerationWorker>()
            .setInputData(workDataOf(GenerationWorker.CLE_MANUEL to true))
            .setConstraints(reseau())
            .addTag(TAG_GENERATION)
            .addTag(TAG_MANUEL)
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            NOM_MANUEL, androidx.work.ExistingWorkPolicy.KEEP, req
        )
    }
}

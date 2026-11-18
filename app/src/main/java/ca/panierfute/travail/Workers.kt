package ca.panierfute.travail

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ca.panierfute.data.ErreurHttp
import ca.panierfute.data.GenerateurPlan
import ca.panierfute.data.Photos
import ca.panierfute.data.Reglages
import ca.panierfute.data.ResultatGeneration
import ca.panierfute.i18n.textes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.max

/** Rappel du vendredi soir : « prends tes photos ». */
class RappelWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val r = Reglages(ctx)
        val t = textes(r.langue)
        r.dernierRappelMs = System.currentTimeMillis()
        val quand = Planificateur.formater(Planificateur.prochaineGeneration(r), t)
        val dejaPrises = Photos.toutes(ctx).size
        Notifs.rappelPhotos(ctx, t, t.notifRappelTexte(dejaPrises, quand))
        withContext(Dispatchers.IO) { Planificateur.planifierSuivante(ctx, Planificateur.TAG_RAPPEL, id) }
        return Result.success()
    }
}

/** Génération du plan (automatique la nuit, ou manuelle). */
class GenerationWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    companion object {
        const val CLE_MANUEL = "manuel"
        const val CLE_CONSIGNES = "consignes"
        const val CLE_ETAPE = "etape"
        private const val SEMAINE_MS = 7L * 24 * 3600 * 1000
    }

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val r = Reglages(ctx)
        val t = textes(r.langue)
        val manuel = inputData.getBoolean(CLE_MANUEL, false)
        val consignes = inputData.getString(CLE_CONSIGNES) ?: ""
        var replanifier = !manuel

        try {
            if (!manuel) {
                // Une seule génération automatique par semaine : si un plan a déjà été
                // généré depuis le dernier rappel (ex. à la main vendredi soir), on saute.
                val seuil = max(r.dernierRappelMs, System.currentTimeMillis() - SEMAINE_MS + 3600_000L)
                if (r.derniereGenerationMs > seuil) return Result.success()
            }

            val resultat = GenerateurPlan.generer(ctx, manuel, consignes) { etape ->
                setProgress(workDataOf(CLE_ETAPE to etape))
            }
            when (resultat) {
                is ResultatGeneration.Succes -> Notifs.planPret(ctx, t, t.notifPlanTexte(resultat.plan))
                is ResultatGeneration.Ignore -> Notifs.erreur(ctx, t, resultat.raison + " " + t.notifOuvrirPourPhotos)
            }
            return Result.success()
        } catch (e: CancellationException) {
            replanifier = false
            throw e
        } catch (e: Exception) {
            val http = e.cause as? ErreurHttp
            val reessayable = (e is IOException) && (http == null || http.code >= 500 || http.code == 429)
            if (!manuel && reessayable && runAttemptCount < 3) {
                replanifier = false   // la même tâche sera relancée plus tard par WorkManager
                return Result.retry()
            }
            val message = e.message ?: e.javaClass.simpleName
            r.derniereErreur = message
            Notifs.erreur(ctx, t, message)
            return Result.failure(workDataOf("erreur" to message))
        } finally {
            if (replanifier) {
                withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                    Planificateur.planifierSuivante(ctx, Planificateur.TAG_AUTO, id)
                }
            }
        }
    }
}

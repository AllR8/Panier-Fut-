package ca.panierfute.travail

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ca.panierfute.MainActivity
import ca.panierfute.R
import ca.panierfute.data.Reglages
import ca.panierfute.i18n.Textes
import ca.panierfute.i18n.textes

object Notifs {
    private const val CANAL = "panier_semaine"
    private const val ID_RAPPEL = 1
    private const val ID_PLAN = 2
    private const val ID_ERREUR = 3
    const val EXTRA_ONGLET = "onglet"

    /** Crée (ou renomme, après un changement de langue) le canal de notifications. */
    fun creerCanal(ctx: Context) {
        val t = textes(Reglages(ctx).langue)
        val canal = NotificationChannel(CANAL, t.notifCanalNom, NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = t.notifCanalDescription }
        ctx.getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)
    }

    fun rappelPhotos(ctx: Context, t: Textes, texte: String) =
        publier(ctx, ID_RAPPEL, t.notifRappelTitre, texte, "photos")

    fun planPret(ctx: Context, t: Textes, texte: String) {
        NotificationManagerCompat.from(ctx).cancel(ID_RAPPEL)
        publier(ctx, ID_PLAN, t.notifPlanTitre, texte, "semaine")
    }

    fun erreur(ctx: Context, t: Textes, texte: String) =
        publier(ctx, ID_ERREUR, t.notifErreurTitre, texte, "semaine")

    private fun intention(ctx: Context, onglet: String, code: Int): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ONGLET, onglet)
        }
        return PendingIntent.getActivity(
            ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    private fun publier(ctx: Context, id: Int, titre: String, texte: String, onglet: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val n = NotificationCompat.Builder(ctx, CANAL)
            .setSmallIcon(R.drawable.ic_notif)
            .setColor(0xFF173A2C.toInt())
            .setContentTitle(titre)
            .setContentText(texte)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texte))
            .setContentIntent(intention(ctx, onglet, id))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}

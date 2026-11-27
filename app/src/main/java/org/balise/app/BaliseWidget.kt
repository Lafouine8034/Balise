package org.balise.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews

class BaliseWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, defaultViews(context, appWidgetId))
        }
    }

    companion object {
        private const val CONFIRMATION_MS = 3000L

        /** Affichage normal : voiture + « Enregistrer », un appui lance l'enregistrement. */
        fun defaultViews(context: Context, appWidgetId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_balise)
            val intent = Intent(context, QuickSaveActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            return views
        }

        /** Affiche « ✓ Enregistré » quelques secondes, puis revient à l'affichage normal. */
        fun showSavedBriefly(context: Context) {
            val app = context.applicationContext
            try {
                val manager = AppWidgetManager.getInstance(app)
                val ids = manager.getAppWidgetIds(ComponentName(app, BaliseWidget::class.java))
                if (ids.isEmpty()) return
                val views = RemoteViews(app.packageName, R.layout.widget_balise)
                views.setTextViewText(R.id.widget_title, "✓ Enregistré")
                manager.partiallyUpdateAppWidget(ids, views)
                Handler(Looper.getMainLooper()).postDelayed({ reset(app) }, CONFIRMATION_MS)
            } catch (e: Exception) {
                // le widget n'est pas indispensable : l'enregistrement a déjà eu lieu
            }
        }

        private fun reset(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, BaliseWidget::class.java))
                for (id in ids) manager.updateAppWidget(id, defaultViews(context, id))
            } catch (e: Exception) {
                // sans gravité
            }
        }
    }
}

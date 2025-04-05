package com.example.ogz_pgz.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.ogz_pgz.R
import com.example.ogz_pgz.MainActivity


class OgzWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        for (widgetId in appWidgetIds) {
            val remoteViews = RemoteViews(context.packageName, R.layout.ogz_widget)


            val intentTab1 = Intent(context, OgzWidget::class.java).apply {
                action = ACTION_OPEN_TAB
                putExtra(EXTRA_TAB_INDEX, 0)
            }

            val pendingIntent1 = PendingIntent.getBroadcast(
                context,
                0,
                intentTab1,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            remoteViews.setOnClickPendingIntent(R.id.buttonGoOgz, pendingIntent1)


            val intentTab2 = Intent(context, OgzWidget::class.java).apply {
                action = ACTION_OPEN_TAB
                putExtra(EXTRA_TAB_INDEX, 1)
            }

            val pendingIntent2 = PendingIntent.getBroadcast(
                context,
                1,
                intentTab2,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            remoteViews.setOnClickPendingIntent(R.id.buttonGoPgz, pendingIntent2)

            appWidgetManager.updateAppWidget(widgetId, remoteViews)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if(intent?.action == ACTION_OPEN_TAB){
            val tabIndex = intent.getIntExtra(EXTRA_TAB_INDEX, 0)

            val startIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(EXTRA_TAB_INDEX, tabIndex)
            }

            context?.startActivity(startIntent)
        }
    }

    companion object{
        const val ACTION_OPEN_TAB = "com.example.ogz_pgz.widget.ACTION_OPEN_TAB"
        const val EXTRA_TAB_INDEX = "EXTRA_TAB_INDEX"
    }

}

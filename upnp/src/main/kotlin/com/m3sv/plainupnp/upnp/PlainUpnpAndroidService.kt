package com.m3sv.plainupnp.upnp

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.m3sv.plainupnp.core.eventbus.events.ExitApplication
import com.m3sv.plainupnp.core.eventbus.post
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class PlainUpnpAndroidService : Service() {

    @Inject
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            NotificationBuilder.ACTION_EXIT -> {
                stopForeground(true)
                finishApplication()
                stopSelf(startId)
            }

            START_SERVICE -> startForeground(
                NotificationBuilder.SERVER_NOTIFICATION,
                NotificationBuilder(this).buildNotification()
            )
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        finishApplication()
    }

    private fun finishApplication() {
        runBlocking { post(ExitApplication) }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, PlainUpnpAndroidService::class.java).apply {
                action = START_SERVICE
            }

            ContextCompat.startForegroundService(context, intent)
        }

        private const val START_SERVICE = "START_UPNP_SERVICE"
    }
}

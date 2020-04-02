package com.m3sv.plainupnp.upnp

import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.m3sv.plainupnp.common.ShutdownDispatcher
import com.m3sv.plainupnp.common.Shutdownable
import com.m3sv.plainupnp.upnp.cling.AndroidUpnpServiceConfiguration
import com.m3sv.plainupnp.upnp.cling.AndroidUpnpServiceImpl
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PlainUpnpAndroidService : AndroidUpnpServiceImpl(), Shutdownable {

    private lateinit var notificationManager: NotificationManagerCompat

    private lateinit var notificationBuilder: NotificationBuilder

    private val executor = Executors.newFixedThreadPool(64)

    private var mediaServer: MediaServer? = null

    override fun createConfiguration(): AndroidUpnpServiceConfiguration =
        object : AndroidUpnpServiceConfiguration() {
            override fun getRegistryMaintenanceIntervalMillis(): Int = 7000
            override fun getSyncProtocolExecutorService(): ExecutorService = executor
        }

    override fun onCreate() {
        super.onCreate()
        ShutdownDispatcher.addListener(this)
        notificationBuilder = NotificationBuilder(this)
        notificationManager = NotificationManagerCompat.from(this)

        val notification = notificationBuilder.buildNotification()
        notificationManager.notify(NotificationBuilder.SERVER_NOTIFICATION, notification)
        startForeground(
            NotificationBuilder.SERVER_NOTIFICATION,
            notification
        )
        mediaServer = MediaServer(this@PlainUpnpAndroidService).apply { start() }
    }

    override fun onDestroy() {
        mediaServer?.stop()
        ShutdownDispatcher.removeListener(this)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (NotificationBuilder.ACTION_EXIT == intent?.action) {
            ShutdownDispatcher.shutdown()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun shutdown() {
        stopForeground(true)
        stopSelf()
    }

    fun inject() {
        // TODO inject upnp manager here
    }
}

package com.m3sv.plainupnp.server

import com.m3sv.plainupnp.common.preferences.Preferences
import com.m3sv.plainupnp.common.preferences.PreferencesRepository
import com.m3sv.plainupnp.core.eventbus.events.ExitApplication
import com.m3sv.plainupnp.core.eventbus.subscribe
import com.m3sv.plainupnp.upnp.android.AndroidUpnpServiceImpl
import com.m3sv.plainupnp.upnp.server.MediaServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.fourthline.cling.UpnpService
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread

@Singleton
class ServerManagerImpl @Inject constructor(
    private val upnpService: UpnpService,
    private val mediaServer: MediaServer,
    private val preferencesRepository: PreferencesRepository
) : ServerManager {

    private val scope = CoroutineScope(Executors.newFixedThreadPool(4).asCoroutineDispatcher())

    private val serverState: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        scope.launch {
            subscribe<ExitApplication>()
                .flowOn(Dispatchers.Main)
                .collect { shutdown() }
        }

        scope.launch {
            preferencesRepository
                .preferences
                .collect { preferences ->
                    serverState.value = when (preferences.applicationMode) {
                        Preferences.ApplicationMode.STREAMING -> true
                        Preferences.ApplicationMode.PLAYER -> false
                        else -> false
                    }
                }
        }

        scope.launch {
            serverState.onEach {
                if (it) {
                    scope.launch {
                        (upnpService as AndroidUpnpServiceImpl).resume()
                        mediaServer.start()
                    }
                } else {
                    scope.launch {
                        (upnpService as AndroidUpnpServiceImpl).pause()
                        mediaServer.stop()
                    }
                }
            }.collect()
        }
    }

    override fun start() {
        (upnpService as AndroidUpnpServiceImpl).start()
    }

    override fun resume() {
        serverState.value = true
    }

    override fun pause() {
        serverState.value = false
    }

    override fun shutdown() {
        Timber.d("Finish media server")
        mediaServer.stop()
        Timber.d("Finished media server")

        thread {
            upnpService.shutdown()
            Timber.d("Shutdown upnpService")
        }
    }
}


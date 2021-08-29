package com.m3sv.selectcontentdirectory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m3sv.plainupnp.common.notification.ForegroundNotificationService
import com.m3sv.plainupnp.common.util.pass
import com.m3sv.plainupnp.data.upnp.DeviceDisplay
import com.m3sv.plainupnp.data.upnp.UpnpDevice
import com.m3sv.plainupnp.interfaces.LifecycleManager
import com.m3sv.plainupnp.interfaces.manageAppLifecycle
import com.m3sv.plainupnp.upnp.manager.Result
import com.m3sv.plainupnp.upnp.manager.UpnpManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SelectContentDirectoryViewModel @Inject constructor(
    application: Application,
    lifecycleManager: LifecycleManager,
    private val upnpManager: UpnpManager,
) : ViewModel() {

    init {
        when {
            lifecycleManager.isClosed || lifecycleManager.isFinishing -> pass
            else -> {
                ForegroundNotificationService.start(application)
                lifecycleManager.start()
                lifecycleManager.manageAppLifecycle()
            }
        }
    }

    val state: StateFlow<List<DeviceDisplay>> = upnpManager
        .contentDirectories
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf())

    suspend fun selectContentDirectory(upnpDevice: UpnpDevice): Result = upnpManager.selectContentDirectory(upnpDevice)
}

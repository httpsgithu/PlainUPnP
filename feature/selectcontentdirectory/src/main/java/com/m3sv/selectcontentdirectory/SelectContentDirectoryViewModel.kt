package com.m3sv.selectcontentdirectory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m3sv.plainupnp.data.upnp.DeviceDisplay
import com.m3sv.plainupnp.data.upnp.UpnpDevice
import com.m3sv.plainupnp.server.ServerManager
import com.m3sv.plainupnp.upnp.PlainUpnpAndroidService
import com.m3sv.plainupnp.upnp.manager.Result
import com.m3sv.plainupnp.upnp.manager.UpnpManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SelectContentDirectoryViewModel @Inject constructor(
    application: Application,
    serverManager: ServerManager,
    private val upnpManager: UpnpManager,
) : ViewModel() {

    init {
        PlainUpnpAndroidService.start(application)
        serverManager.start()
        serverManager.resume()
    }

    val state: StateFlow<List<DeviceDisplay>> = upnpManager
        .contentDirectories
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf())

    fun selectContentDirectoryAsync(upnpDevice: UpnpDevice): Deferred<Result> =
        upnpManager.selectContentDirectoryAsync(upnpDevice)
}

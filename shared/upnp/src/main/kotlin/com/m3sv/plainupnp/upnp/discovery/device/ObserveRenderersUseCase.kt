package com.m3sv.plainupnp.upnp.discovery.device

import com.m3sv.plainupnp.core.persistence.Database
import com.m3sv.plainupnp.data.upnp.DeviceDisplay
import com.m3sv.plainupnp.upnp.manager.UpnpManager
import com.m3sv.plainupnp.upnp.util.RENDERER_TYPE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveRenderersUseCase @Inject constructor(
    private val upnpManager: UpnpManager,
    private val database: Database,
) {
    private var foundCached: Boolean = false

    operator fun invoke(): Flow<DeviceDisplayBundle> = upnpManager
        .renderers
        .flowOn(Dispatchers.IO)
        .map { devices ->
            var deviceIndex = -1
            var deviceName: String? = null

            if (!foundCached) {
                deviceIndex = devices.indexOfFirst(::queryDatabaseForIdentity)
                deviceName = if (deviceIndex != -1) {
                    foundCached = true
                    devices[deviceIndex].device.friendlyName
                } else
                    null
            }

            DeviceDisplayBundle(
                devices,
                deviceIndex,
                deviceName
            )
        }

    private fun queryDatabaseForIdentity(deviceDisplay: DeviceDisplay): Boolean {
        val device = deviceDisplay.device
        return database
            .selectedDeviceQueries
            .selectDeviceByIdentity(RENDERER_TYPE, device.fullIdentity)
            .executeAsOneOrNull() != null
    }
}


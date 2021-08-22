package com.m3sv.plainupnp.upnp.actions.avtransport

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.fourthline.cling.controlpoint.ControlPoint
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.support.avtransport.callback.GetTransportInfo
import org.fourthline.cling.support.model.TransportInfo
import timber.log.Timber
import javax.inject.Inject

class GetTransportInfoAction @Inject constructor(private val controlPoint: ControlPoint) {

    fun getTransportInfo(service: Service<*, *>): Flow<TransportInfo> = callbackFlow {
        val tag = "AV"
        Timber.tag(tag).d("Get transport info")

        val action = object : GetTransportInfo(service) {
            override fun received(
                invocation: ActionInvocation<out Service<*, *>>?,
                transportInfo: TransportInfo,
            ) {
                Timber.tag(tag).d("Received transport info")
                trySendBlocking(transportInfo)
                close()
            }

            override fun failure(
                p0: ActionInvocation<out Service<*, *>>?,
                p1: UpnpResponse?,
                p2: String?,
            ) {
                error("Failed to get transport info")
            }
        }

        controlPoint.execute(action)
        awaitClose()
    }
}


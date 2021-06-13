package com.m3sv.plainupnp.upnp.actions.avtransport

import com.m3sv.plainupnp.upnp.actions.Action
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.fourthline.cling.controlpoint.ControlPoint
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.support.avtransport.callback.Play
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PlayAction @Inject constructor(controlPoint: ControlPoint) :
    Action<Unit, Boolean>(controlPoint) {

    fun play(
        service: Service<*, *>,
    ): Flow<Unit> {
        return callbackFlow {
            val tag = "AV"
            Timber.tag(tag).d("Play called")

            val action = object : Play(service) {
                override fun success(invocation: ActionInvocation<out Service<*, *>>?) {
                    Timber.tag(tag).d("Play success")
                    trySendBlocking(Unit)
                    close()
                }

                override fun failure(
                    p0: ActionInvocation<out Service<*, *>>?,
                    p1: UpnpResponse?,
                    p2: String?,
                ) {
                    error("Play failed")
                }
            }

            controlPoint.execute(action)

            awaitClose()
        }
    }

    override suspend fun invoke(
        service: Service<*, *>,
        vararg arguments: Unit,
    ): Boolean = suspendCoroutine { continuation ->
        val tag = "AV"
        Timber.tag(tag).d("Play called")

        val action = object : Play(service) {
            override fun success(invocation: ActionInvocation<out Service<*, *>>?) {
                Timber.tag(tag).d("Play success")
                continuation.resume(true)
            }

            override fun failure(
                p0: ActionInvocation<out Service<*, *>>?,
                p1: UpnpResponse?,
                p2: String?,
            ) {
                Timber.tag(tag).d("Play failed")
                continuation.resume(false)
            }
        }

        controlPoint.execute(action)
    }
}

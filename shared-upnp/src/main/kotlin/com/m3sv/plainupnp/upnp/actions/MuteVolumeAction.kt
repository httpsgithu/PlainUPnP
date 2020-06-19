package com.m3sv.plainupnp.upnp.actions

import com.m3sv.plainupnp.upnp.RendererServiceFinder
import org.fourthline.cling.UpnpService
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.support.renderingcontrol.callback.SetMute
import timber.log.Timber
import javax.inject.Inject

class MuteVolumeAction @Inject constructor(
    listener: UpnpService,
    serviceFinder: RendererServiceFinder
) : RenderingAction(listener, serviceFinder) {

    operator fun invoke(mute: Boolean) = executeRenderingAction {
        object : SetMute(this, mute) {
            override fun failure(
                invocation: ActionInvocation<out Service<*, *>>?,
                operation: UpnpResponse?,
                defaultMsg: String?
            ) {
                Timber.e(UpnpActionException("Failed to mute"))
            }
        }
    }
}
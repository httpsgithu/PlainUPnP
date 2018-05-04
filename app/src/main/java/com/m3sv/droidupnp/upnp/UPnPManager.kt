package com.m3sv.droidupnp.upnp

import com.m3sv.droidupnp.upnp.observers.ContentDirectoryDiscoveryObservable
import com.m3sv.droidupnp.upnp.observers.RendererDiscoveryObservable
import org.droidupnp.controller.upnp.IUPnPServiceController
import org.droidupnp.model.upnp.Factory
import java.util.*


class UPnPManager constructor(val controller: IUPnPServiceController, val factory: Factory) : Observable() {
    val rendererDiscoveryObservable = RendererDiscoveryObservable(controller.rendererDiscovery)
    val contentDirectoryDiscoveryObservable = ContentDirectoryDiscoveryObservable(controller.contentDirectoryDiscovery)
}
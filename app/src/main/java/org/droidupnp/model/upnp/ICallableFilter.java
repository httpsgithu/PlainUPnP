package org.droidupnp.model.upnp;

import com.m3sv.droidupnp.data.UpnpDevice;

import java.util.concurrent.Callable;

public interface ICallableFilter extends Callable<Boolean> {
    void setDevice(UpnpDevice device);
}

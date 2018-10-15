package org.droidupnp.model.upnp;

import com.m3sv.droidupnp.data.UpnpDevice;

public class CallableContentDirectoryFilter implements ICallableFilter {

    private UpnpDevice device;

    public void setDevice(UpnpDevice device) {
        this.device = device;
    }

    @Override
    public Boolean call() {
        return device.asService("ContentDirectory");
    }
}

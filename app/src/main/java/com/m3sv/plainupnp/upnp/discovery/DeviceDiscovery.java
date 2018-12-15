/**
 * Copyright (C) 2013 Aurélien Chabot <aurelien@chabot.fr>
 * <p>
 * This file is part of DroidUPNP.
 * <p>
 * DroidUPNP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * DroidUPNP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with DroidUPNP.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.m3sv.plainupnp.upnp.discovery;


import com.m3sv.plainupnp.data.upnp.UpnpDevice;
import com.m3sv.plainupnp.data.upnp.UpnpDeviceEvent;

import com.m3sv.plainupnp.upnp.ServiceListener;
import com.m3sv.plainupnp.upnp.UpnpServiceController;

import org.droidupnp.legacy.upnp.DeviceDiscoveryObserver;
import org.droidupnp.legacy.upnp.CallableFilter;
import org.droidupnp.legacy.upnp.RegistryListener;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import timber.log.Timber;

public abstract class DeviceDiscovery {

    protected boolean extendedInformation;

    protected final UpnpServiceController controller;

    private final BrowsingRegistryListener browsingRegistryListener;

    private final CopyOnWriteArrayList<DeviceDiscoveryObserver> observerList;

    public DeviceDiscovery(UpnpServiceController controller, boolean extendedInformation) {
        this.controller = controller;
        browsingRegistryListener = new BrowsingRegistryListener();
        this.extendedInformation = extendedInformation;
        observerList = new CopyOnWriteArrayList<>();
    }

    public DeviceDiscovery(UpnpServiceController controller) {
        this(controller, false);
    }

    public void resume(ServiceListener serviceListener) {
        serviceListener.addListener(browsingRegistryListener);
    }

    public void pause(ServiceListener serviceListener) {
        serviceListener.removeListener(browsingRegistryListener);
        serviceListener.clearListener();
    }

    public class BrowsingRegistryListener implements RegistryListener {

        @Override
        public void deviceAdded(final UpnpDevice device) {
            Timber.v("New device detected : " + device.getDisplayString());

            if (device.isFullyHydrated() && filter(device)) {
                if (isSelected(device)) {
                    Timber.i("Reselect device to refresh it");
                    select(device, true);
                }

                notifyAdded(device);
            }
        }

        @Override
        public void deviceRemoved(final UpnpDevice device) {
            Timber.v("Device removed : " + device.getFriendlyName());

            if (filter(device)) {
                if (isSelected(device)) {
                    Timber.i("Selected device have been removed");
                    removed(device);
                }

                notifyRemoved(device);
            }
        }
    }

    public boolean hasObserver(DeviceDiscoveryObserver o) {
        return observerList.contains(o);
    }

    public void addObserver(DeviceDiscoveryObserver o) {
        observerList.add(o);

        final Collection<UpnpDevice> upnpDevices = controller.getServiceListener()
                .getFilteredDeviceList(getCallableFilter());
        for (UpnpDevice d : upnpDevices)
            o.addedDevice(new UpnpDeviceEvent.Added(d));
    }

    public void removeObserver(DeviceDiscoveryObserver o) {
        observerList.remove(o);
    }

    public void notifyAdded(UpnpDevice device) {
        for (DeviceDiscoveryObserver o : observerList)
            o.addedDevice(new UpnpDeviceEvent.Added(device));
    }

    public void notifyRemoved(UpnpDevice device) {
        for (DeviceDiscoveryObserver o : observerList)
            o.removedDevice(new UpnpDeviceEvent.Removed(device));
    }

    /**
     * Filter device you want to add to this device list fragment
     *
     * @param device the device to test
     * @return add it or not
     * @throws Exception
     */
    protected boolean filter(UpnpDevice device) {
        CallableFilter filter = getCallableFilter();
        filter.setDevice(device);
        try {
            return filter.call();
        } catch (Exception e) {
            Timber.e(e);
        }
        return false;
    }

    /**
     * Get a callable device filter
     *
     * @return
     */
    protected abstract CallableFilter getCallableFilter();

    /**
     * Filter to know if device is selected
     *
     * @param d
     * @return
     */
    protected abstract boolean isSelected(UpnpDevice d);

    /**
     * Select a device
     *
     * @param device
     */
    protected abstract void select(UpnpDevice device);

    /**
     * Select a device
     *
     * @param device
     * @param force
     */
    protected abstract void select(UpnpDevice device, boolean force);

    /**
     * Callback when device removed
     *
     * @param d
     */
    protected abstract void removed(UpnpDevice d);
}
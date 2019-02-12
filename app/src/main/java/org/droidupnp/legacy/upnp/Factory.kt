/**
 * Copyright (C) 2013 Aurélien Chabot <aurelien></aurelien>@chabot.fr>
 *
 *
 * This file is part of DroidUPNP.
 *
 *
 * DroidUPNP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *
 * DroidUPNP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with DroidUPNP.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package org.droidupnp.legacy.upnp

import android.content.Context
import com.m3sv.plainupnp.upnp.ContentDirectoryCommand
import com.m3sv.plainupnp.upnp.RendererCommand
import com.m3sv.plainupnp.upnp.UpnpServiceController
import org.droidupnp.legacy.cling.UpnpRendererStateObservable

interface Factory {
    val upnpServiceController: UpnpServiceController

    fun createContentDirectoryCommand(): ContentDirectoryCommand?

    fun createRendererState(): UpnpRendererStateObservable

    fun createRendererCommand(rendererStateObservable: UpnpRendererStateObservable?): RendererCommand?
}

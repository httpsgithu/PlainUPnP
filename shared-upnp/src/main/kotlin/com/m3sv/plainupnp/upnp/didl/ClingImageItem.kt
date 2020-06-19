package com.m3sv.plainupnp.upnp.didl

import org.fourthline.cling.support.model.item.ImageItem

class ClingImageItem(item: ImageItem) : ClingDIDLItem(item) {

    override val dataType: String = "image/*"

}

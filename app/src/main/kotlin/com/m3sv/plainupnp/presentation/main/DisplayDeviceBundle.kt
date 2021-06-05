package com.m3sv.plainupnp.presentation.main

import com.m3sv.plainupnp.presentation.SpinnerItem

data class SpinnerItemsBundle(val devices: List<SpinnerItem>) {
    companion object {
        val empty = SpinnerItemsBundle(listOf())
    }
}

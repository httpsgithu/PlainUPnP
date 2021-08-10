package com.m3sv.plainupnp.util

import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.m3sv.plainupnp.core.eventbus.events.ExitApplication
import com.m3sv.plainupnp.core.eventbus.subscribe
import kotlinx.coroutines.flow.collect

fun ComponentActivity.subscribeForFinish() {
    lifecycleScope.launchWhenCreated { subscribe<ExitApplication>().collect { finishAffinity() } }
}

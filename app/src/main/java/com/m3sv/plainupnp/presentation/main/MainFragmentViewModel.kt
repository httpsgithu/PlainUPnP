package com.m3sv.plainupnp.presentation.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.m3sv.plainupnp.common.utils.disposeBy
import com.m3sv.plainupnp.presentation.base.BaseViewModel
import com.m3sv.plainupnp.upnp.ContentState
import com.m3sv.plainupnp.upnp.UpnpManager
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import javax.inject.Inject


class MainFragmentViewModel @Inject constructor(private val upnpManager: UpnpManager) :
        BaseViewModel(), UpnpManager by upnpManager {

    private val _serverContent = MutableLiveData<ContentState>()
    val serverContent: LiveData<ContentState> = _serverContent

    init {
        content.subscribeBy(onNext = _serverContent::postValue, onError = Timber::e).disposeBy(disposables)
    }
}
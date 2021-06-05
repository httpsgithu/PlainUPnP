package com.m3sv.plainupnp.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m3sv.plainupnp.ThemeManager
import com.m3sv.plainupnp.ThemeOption
import com.m3sv.plainupnp.common.preferences.PreferencesRepository
import com.m3sv.plainupnp.data.upnp.UpnpRendererState
import com.m3sv.plainupnp.presentation.SpinnerItem
import com.m3sv.plainupnp.upnp.didl.ClingContainer
import com.m3sv.plainupnp.upnp.didl.ClingDIDLObject
import com.m3sv.plainupnp.upnp.didl.ClingMedia
import com.m3sv.plainupnp.upnp.folder.Folder
import com.m3sv.plainupnp.upnp.manager.UpnpManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainViewState(
    val upnpRendererState: UpnpRendererState = UpnpRendererState.Empty,
    val spinnerItemsBundle: SpinnerItemsBundle = SpinnerItemsBundle.empty,
    val navigationStack: List<Folder> = listOf(),
    val activeTheme: ThemeOption = ThemeOption.System,
    val enableThumbnails: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    themeManager: ThemeManager,
    private val upnpManager: UpnpManager,
    private val volumeManager: BufferedVolumeManager,
) : ViewModel() {

    val volume = volumeManager
        .volumeFlow

    private val upnpState = upnpManager.upnpRendererState.onStart { emit(UpnpRendererState.Empty) }

    private val renderers = upnpManager.renderers
        .map { devices ->
            val items = devices.map { SpinnerItem(it.upnpDevice.friendlyName, it) }
            SpinnerItemsBundle(items)
        }

    private val navigationStack: Flow<List<Folder>> = upnpManager.navigationStack

    val finishActivityFlow: Flow<Unit> = navigationStack.filter { it.isEmpty() }.map { }

    val viewState: StateFlow<MainViewState> =
        combine(
            upnpState,
            renderers,
            navigationStack.filterNot { it.isEmpty() },
            themeManager.theme,
            preferencesRepository.preferences
        ) { upnpRendererState, spinnerItemsBundle, navigationStack, activeTheme, preferences ->
            MainViewState(
                upnpRendererState = upnpRendererState,
                spinnerItemsBundle = spinnerItemsBundle,
                navigationStack = navigationStack,
                activeTheme = activeTheme,
                enableThumbnails = preferences.enableThumbnails
            )
        }.stateIn(viewModelScope, SharingStarted.Lazily, MainViewState(activeTheme = themeManager.theme.value))

    fun itemClick(item: ClingDIDLObject) {
        when (item) {
            is ClingContainer -> navigateTo(item.id, item.title)
            is ClingMedia -> upnpManager.playItem(item.id)
            else -> error("Unknown cling item")
        }

    }

    fun moveTo(progress: Int) {
        viewModelScope.launch {
            upnpManager.seekTo(progress)
        }
    }

    fun selectRenderer(position: SpinnerItem) {
        upnpManager.selectRenderer(position)
    }

    fun playerButtonClick(button: PlayerButton) {
        viewModelScope.launch {
            when (button) {
                PlayerButton.PLAY -> upnpManager.togglePlayback()
                PlayerButton.PREVIOUS -> upnpManager.playPrevious()
                PlayerButton.NEXT -> upnpManager.playNext()
                PlayerButton.RAISE_VOLUME -> volumeManager.raiseVolume()
                PlayerButton.LOWER_VOLUME -> volumeManager.lowerVolume()
                PlayerButton.STOP -> upnpManager.stopPlayback()
            }
        }
    }

    fun navigateBack() {
        upnpManager.navigateBack()
    }

    fun navigateTo(folder: Folder) {
        upnpManager.navigateTo(folder)
    }

    private fun navigateTo(id: String, title: String) {
        upnpManager.navigateTo(id, title)
    }
}

package com.m3sv.plainupnp.presentation.base

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject


abstract class BaseFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    protected inline fun <reified T : ViewModel> getViewModel(activityScoped: Boolean = false): T =
        if (activityScoped) {
            ViewModelProvider(requireActivity(), viewModelFactory)
        } else {
            ViewModelProvider(this, viewModelFactory)
        }.get(T::class.java)

}

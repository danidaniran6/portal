package io.dee.portal.core.view.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import io.dee.portal.core.view.MainViewModel

@AndroidEntryPoint
abstract class BaseFragment : Fragment() {
    val mainViewModel by activityViewModels<MainViewModel> ()
    abstract fun bindVariables()
    abstract fun bindViews()
    abstract fun bindObservers()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindVariables()
        bindViews()
        bindObservers()

    }

}
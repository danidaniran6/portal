package io.dee.portal.core.base

import android.os.Bundle
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class BaseBottomSheet : BottomSheetDialogFragment() {
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
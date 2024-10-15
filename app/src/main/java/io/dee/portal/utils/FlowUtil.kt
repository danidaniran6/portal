package io.dee.portal.utils

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

fun <T> Fragment.flowCollect(
    flow: Flow<T>,
    lifecycle: Lifecycle.State = Lifecycle.State.RESUMED,
    result: (T) -> Unit
) {
    this.viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(lifecycle) {
            flow.collect {
                result.invoke(it)
            }
        }
    }
}

fun <T> Fragment.flowCollectLatest(
    flow: Flow<T>,
    lifecycle: Lifecycle.State = Lifecycle.State.RESUMED,
    result: (T) -> Unit
) {
    this.viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(lifecycle) {
            flow.collectLatest {
                result.invoke(it)
            }
        }
    }
}
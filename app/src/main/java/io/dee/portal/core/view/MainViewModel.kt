package io.dee.portal.core.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dee.portal.utils.NetworkStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject constructor() : ViewModel() {
    private val _connectivityStatus = MutableStateFlow(NetworkStatus.Unknown)
    val connectivityStatus = _connectivityStatus.asStateFlow()
    fun setConnectivityStatus(status: NetworkStatus) =viewModelScope.launch {
        _connectivityStatus.emit(status)
    }


}
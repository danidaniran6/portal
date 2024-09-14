package io.dee.portal.search_driver.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dee.portal.search_driver.data.repository.SearchDriverRepository
import io.dee.portal.search_driver.data.dto.Driver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchDriverViewModel
@Inject constructor(
    private val repository: SearchDriverRepository
) : ViewModel() {
    private val _uiState: MutableStateFlow<SearchDriverState> =
        MutableStateFlow(SearchDriverState.Loading)
    val uiState = _uiState.asStateFlow()
    fun onEvent(event: SearchDriverEvent) {
        when (event) {
            is SearchDriverEvent.GetDriver -> {
                getDriver()
            }
        }
    }

    private fun getDriver() = viewModelScope.launch {
        _uiState.value = repository.getDriver()
    }
}

sealed interface SearchDriverEvent {
    object GetDriver : SearchDriverEvent
}

sealed interface SearchDriverState {
    data object Loading : SearchDriverState
    data class Success(val driver: Driver) : SearchDriverState
    data class Error(val message: String) : SearchDriverState
}
package io.dee.portal.search_screen.data

import io.dee.portal.core.data.local.Location

data class SearchState(
    val isLoading: Boolean = false,
    val term: String = "",
    val searchedList: List<Location?> = emptyList()
)

sealed interface SearchUiState {
    object Loading : SearchUiState
    data class Error(val throwable: Throwable) : SearchUiState
    data class Success(
        val term: String, val searchedList: List<Location>
    ) : SearchUiState
}

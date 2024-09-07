package io.dee.portal.view.search_screen.data

import io.dee.portal.data.local.Location

data class SearchState(
    val isLoading: Boolean = false,
    val term: String = "",
    val searchedList: List<Location?> = emptyList()
)
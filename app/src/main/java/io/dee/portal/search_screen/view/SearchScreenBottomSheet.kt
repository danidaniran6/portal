package io.dee.portal.search_screen.view

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.dee.core.base.BaseBottomSheet
import io.dee.portal.R
import io.dee.portal.data.local.Location
import io.dee.portal.databinding.BottomSheetSearchBinding
import io.dee.portal.search_screen.data.SearchUiState
import kotlinx.coroutines.launch
import org.neshan.common.model.LatLng

class SearchScreenBottomSheet(
    private val userLocation: LatLng,
    private val currentLocationSelected: () -> Unit,
    private val onItemSearched: (Location) -> Unit
) : BaseBottomSheet() {
    private lateinit var binding: BottomSheetSearchBinding
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var searchAdapter: SearchedListAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private val searchRunnable = Runnable {
        viewModel.onEvent(
            SearchEvents.Search(
                binding.etSearch.text.toString(), userLocation.latitude, userLocation.longitude
            )
        )
    }
    private val searchHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        binding = BottomSheetSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun getTheme() = R.style.CustomBottomSheetDialogTheme

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomSheetBehavior = BottomSheetBehavior.from(view.parent as View)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }


    override fun bindVariables() {
        searchAdapter = SearchedListAdapter { selectedLocation ->
            viewModel.onEvent(SearchEvents.SaveLocation(selectedLocation))
            onItemSearched.invoke(selectedLocation)
            dismiss()
        }
    }

    override fun bindViews() {
        binding.apply {
            rcSearchResult.apply {
                this.itemAnimator = null
                this.layoutManager = LinearLayoutManager(requireContext())
                this.adapter = searchAdapter
            }

            etSearch.doAfterTextChanged {
                searchHandler.removeCallbacks(searchRunnable)
                searchHandler.postDelayed(searchRunnable, 800)
            }
            cvUserCurrentLocation.setOnClickListener {
                currentLocationSelected()
                dismiss()
            }
        }
    }

    override fun bindObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is SearchUiState.Error -> {}
                        is SearchUiState.Loading -> {}
                        is SearchUiState.Success -> {
                            searchAdapter.submitList(state.searchedList) {
                                binding.rcSearchResult.smoothScrollToPosition(0)
                            }
                        }
                    }
                }
            }
        }
    }
}
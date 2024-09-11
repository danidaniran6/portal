package io.dee.portal.search_driver.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.dee.portal.core.base.BaseBottomSheet
import io.dee.portal.databinding.BottomSheetSearchDriverBinding
import io.dee.portal.search_driver.data.dto.Driver
import kotlinx.coroutines.launch

class SearchDriverBottomSheet(
    private val onDriverFound: (driver: Driver) -> Unit
) : BaseBottomSheet() {

    private lateinit var binding: BottomSheetSearchDriverBinding
    private val viewModel: SearchDriverViewModel by viewModels()
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetSearchDriverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCancelable(false)
        bottomSheetBehavior = BottomSheetBehavior.from(view.parent as View)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetBehavior.isDraggable = false
        bottomSheetBehavior.isHideable = false
        viewModel.onEvent(SearchDriverEvent.GetDriver)
    }

    override fun bindVariables() = Unit

    override fun bindViews() = Unit

    override fun bindObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is SearchDriverState.Loading -> Unit

                        is SearchDriverState.Success -> {
                            Toast.makeText(requireContext(), "راننده پیدا شد", Toast.LENGTH_SHORT)
                                .show()
                            onDriverFound(state.driver)
                            dismiss()
                        }

                        is SearchDriverState.Error -> {
                            Toast.makeText(requireContext(), "راننده پیدا نشد", Toast.LENGTH_SHORT)
                                .show()
                            dismiss()
                        }
                    }
                }
            }
        }
    }
}
package io.dee.portal.map_screen.view

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.carto.graphics.Color
import com.carto.styles.LineEndType
import com.carto.styles.LineJoinType
import com.carto.styles.LineStyleBuilder
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dagger.hilt.android.AndroidEntryPoint
import io.dee.portal.BuildConfig
import io.dee.portal.R
import io.dee.portal.core.data.local.Location
import io.dee.portal.core.view.base.BaseFragment
import io.dee.portal.databinding.FragmentMapBinding
import io.dee.portal.map_screen.data.dto.DecodedSteps
import io.dee.portal.search_screen.view.SearchScreenBottomSheet
import io.dee.portal.utils.CustomDialog
import io.dee.portal.utils.LocationProviderState
import io.dee.portal.utils.MapShapesUtil
import io.dee.portal.utils.NavigationUtil.calculateBearing
import io.dee.portal.utils.flowCollect
import io.dee.portal.utils.toast
import org.neshan.common.model.LatLng
import org.neshan.mapsdk.MapView
import org.neshan.mapsdk.model.Marker
import org.neshan.mapsdk.model.Polyline


@AndroidEntryPoint
class MapFragment : BaseFragment() {
    private val TAG = MapFragment::class.java.name
    private val REQUEST_CODE = 100
    private lateinit var binding: FragmentMapBinding
    private val viewModel: MapViewModel by viewModels()

    private val stepsAdapter: StepsAdapter = StepsAdapter()
    private var isStepsOpen = false
    private var moveCameraToUserLocation: Boolean = true
    private var moveCameraToNavigatorLocation: Boolean = true
    private val backPressedCallback = object : OnBackPressedCallback(enabled = false) {
        override fun handleOnBackPressed() {
            if (viewModel.routingStatus.value == RoutingStatus.InProgress) {
                "routing in progress".toast(requireContext())
                showAlertDialog()
                return
            }
            isEnabled = false
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }
    private val backToRouteTimer = object : CountDownTimer(10000, 10) {
        override fun onTick(millisUntilFinished: Long) {
            binding.cvBackToRoute.visibility = View.VISIBLE
            moveCameraToNavigatorLocation = false
            val progress = 100 - (millisUntilFinished / 10000f * 100).toInt()
            binding.pbBackToRoute.setProgress(progress)
        }

        override fun onFinish() {
            backToTheRoute()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onPause() {
        super.onPause()

        viewModel.onEvent(MapEvents.StopLocationUpdates)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner, backPressedCallback
        )

        initMap()

    }

    private fun showAlertDialog() {
        CustomDialog.Builder()
            .setIcon(R.drawable.ic_round_warning_24, R.color.orange)
            .setTitle(getString(R.string.warning))
            .setDescription(getString(R.string.cancel_routing_warning))
            .setConfirmActionString(getString(R.string.yes))
            .setCancelActionString(getString(R.string.dismiss)).setConfirmCallback {
                viewModel.onEvent(MapEvents.CancelRouting)
                it.dismiss()
            }.build(requireContext()).show()

    }

    private fun initMap() {
        binding.map.apply {
            cachePath = requireContext().cacheDir
            cacheSize = 10
            settings.isMapRotationEnabled = true
            settings.maxTiltAngle = 90f
            settings.minTiltAngle = 30f
        }

    }

    override fun bindVariables() {
    }

    override fun bindViews() {
        binding.apply {
            binding.cvBackToRoute.setOnClickListener {
                backToTheRoute()
            }
            binding.cvNextStep.setOnClickListener {
                if (!isStepsOpen) openSteps()
                else closeSteps()
            }
            rcSteps.apply {
                this.layoutManager = LinearLayoutManager(requireContext())
                this.adapter = stepsAdapter
            }
            map.setOnMarkerClickListener {
                it.showInfoWindow()
                true
            }
            btnCancelRouting.setOnClickListener {
                viewModel.onEvent(MapEvents.CancelRouting)
            }
            btnSearchDriver.setOnClickListener {
                viewModel.onEvent(MapEvents.GetRoute)
            }
            btnMyLocation.setOnClickListener {
                viewModel.userLocation.value?.let {
                    binding.map.moveCamera(
                        LatLng(it.latitude, it.longitude), 0.25f
                    )
                }

            }
            llOriginLocation.setOnClickListener {
                SearchScreenBottomSheet(viewModel.userMarker.value?.latLng
                    ?: binding.map.cameraTargetPosition,
                    currentLocationSelected = {
                        viewModel.onEvent(MapEvents.SetOriginLocation(viewModel.userLocation.value))
                    },
                    onItemSearched = {
                        viewModel.onEvent(MapEvents.SetOriginLocation(it))
                    }).show(childFragmentManager, "")
            }
            llDestinationLocation.setOnClickListener {
                SearchScreenBottomSheet(binding.map.cameraTargetPosition,
                    currentLocationSelected = {
                        viewModel.onEvent(MapEvents.SetDestinationLocation(viewModel.userLocation.value))
                    },
                    onItemSearched = {
                        viewModel.onEvent(MapEvents.SetDestinationLocation(it))
                    }).show(childFragmentManager, "")
            }
            btnClearOriginLocation.setOnClickListener {
                viewModel.onEvent(MapEvents.ClearOriginLocation)
            }
            btnClearDestinationLocation.setOnClickListener {
                viewModel.onEvent(MapEvents.ClearDestinationLocation)
            }

            map.setOnMapLongClickListener {
                if (binding.isRouting == true) return@setOnMapLongClickListener
                viewModel.onEvent(
                    MapEvents.SetOriginDestinationLocation(
                        origin = viewModel.userLocation.value, destination = Location(
                            it.latitude, it.longitude, ""
                        )
                    )
                )

            }
            map.setOnCameraMoveStartListener { p0 ->
                when (p0) {

                    MapView.CameraMoveType.GESTURE -> {
                        if (viewModel.routingStatus.value == RoutingStatus.InProgress) {
                            backToRouteTimer.cancel()
                            backToRouteTimer.start()
                        }
                    }

                    MapView.CameraMoveType.DEVELOPER_ANIMATION -> {
                        p0
                    }

                    else -> {}
                }
            }
        }
    }

    private fun clearPolyLine() {
        if (viewModel.originToDestinationLine.value != null) {
            binding.map.removePolyline(viewModel.originToDestinationLine.value)
            viewModel.onEvent(MapEvents.SetOriginToDestinationLine(null))
        }
    }

    private fun clearRoutingLine() {
        if (viewModel.routePolyline.value != null) {
            binding.map.removePolyline(viewModel.routePolyline.value)
            viewModel.onEvent(MapEvents.SetRoutePolyline(null))
        }
    }


    override fun onResume() {
        super.onResume()

        Dexter.withContext(requireActivity())
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    viewModel.onEvent(MapEvents.StartLocationUpdates)
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    if (response.isPermanentlyDenied) {
                        openSettings()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?, token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()

    }


    private fun openSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts(
            "package", BuildConfig.APPLICATION_ID, null
        )
        intent.data = uri
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    override fun bindObservers() {


        viewModel.apply {
            originToDestinationLine.observe(viewLifecycleOwner) { line ->
                line?.let {
                    binding.map.addPolyline(it)
                    viewModel.originLocation.value?.let { origin ->
                        binding.map.moveCamera(origin.getLatLng(), .5f)
                    }
                }
                binding.apply {
                    btnSearchDriver.alpha = if (line == null) 0.15f else 1f
                    btnSearchDriver.isEnabled = line != null
                }
            }
            routePolyline.observe(viewLifecycleOwner) { line ->
                line?.let {
                    binding.map.addPolyline(it)
                }

            }
            userLocation.observe(viewLifecycleOwner) {
                (it == null).also {
                    binding.btnMyLocation.isEnabled = !it
                    binding.btnMyLocation.alpha = if (it) 0.15f else 1f
                }
                if (viewModel.routingStatus.value is RoutingStatus.InProgress) {
                    viewModel.onEvent(MapEvents.SetNavigatorLocation(it))
                    viewModel.onEvent(MapEvents.FindCurrentStep)
                } else onUpdateUserLocation(it)
            }
            navigatorLocation.observe(viewLifecycleOwner) {
                onUpdateNavigatorLocation(it)
            }
            flowCollect(flow = userLocationProvider) { state ->
                when (state) {
                    is LocationProviderState.Success -> {
                        viewModel.onEvent(MapEvents.SetUserLocation(state.location))
                    }

                    is LocationProviderState.Error -> {
                        val statusCode = try {
                            (state.exception as ApiException).statusCode
                        } catch (e: Exception) {
                            null
                        }
                        when (statusCode) {
                            LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                                Log.i(
                                    TAG,
                                    "Location settings are not satisfied. Attempting to upgrade " + "location settings "
                                )
                                try {
                                    val rae = state.exception as ResolvableApiException
                                    rae.startResolutionForResult(
                                        requireActivity(), REQUEST_CODE
                                    )
                                } catch (sie: IntentSender.SendIntentException) {
                                    Log.i(
                                        TAG, "PendingIntent unable to execute request."
                                    )
                                }
                            }

                            LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                                val errorMessage =
                                    "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                                Log.e(
                                    TAG, errorMessage
                                )
                                Toast.makeText(
                                    requireContext(), errorMessage, Toast.LENGTH_LONG
                                ).show()
                            }

                            else -> {}
                        }
                    }

                    else -> {}
                }

            }
            originLocation.observe(viewLifecycleOwner) {
                onUpdateOriginLocation(it)
                binding.isOriginFilled = it != null
                binding.origin = it
                if (destinationLocation.value != null) {
                    drawPolyline(it, destinationLocation.value)
                }
            }
            destinationLocation.observe(viewLifecycleOwner) {
                onUpdateDestinationLocation(it)
                binding.isDestinationFilled = it != null
                binding.destination = it
                if (originLocation.value != null) {
                    drawPolyline(origin = originLocation.value, it)
                }
            }
            originMarker.observe(viewLifecycleOwner) { marker ->
                marker?.let {
                    binding.map.addMarker(it)
                }
            }
            destinationMarker.observe(viewLifecycleOwner) { marker ->
                marker?.let {
                    binding.map.addMarker(it)
                }
            }
            userMarker.observe(viewLifecycleOwner) { marker ->
                marker?.let {
                    binding.map.addMarker(it)
                }
            }
            navigatorMarker.observe(viewLifecycleOwner) { marker ->
                marker?.let {
                    binding.map.addMarker(it)
                }
            }

            fetchRoutingState.observe(viewLifecycleOwner) { state ->
                when (state) {
                    is FetchRoutingState.Loading -> {
                        binding.fetchRoutingLoading = true
                        disableButtons()
                    }

                    is FetchRoutingState.Success -> {
                        clearPolyLine()
                        binding.fetchRoutingLoading = false
                        viewModel.onEvent(MapEvents.SetRoutingOverView(state.routeOverView))
                        viewModel.onEvent(MapEvents.SetRoutingSteps(state.routeSteps))
                        viewModel.onEvent(MapEvents.StartRouting)
                    }

                    is FetchRoutingState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        viewModel.onEvent(MapEvents.CancelRouting)
                        binding.fetchRoutingLoading = false
                        enableButtons()
                    }
                }
            }




            routingSteps.observe(viewLifecycleOwner) { steps ->
                stepsAdapter.submitList(steps) {
                    binding.rcSteps.scrollToPosition(0)
                }

                val polylineList = steps?.fold(arrayListOf<LatLng>()) { acc, decodedSteps ->
                    decodedSteps.decodedPolyline?.let {
                        acc.addAll(it)
                    }
                    return@fold acc
                }
                if (!polylineList.isNullOrEmpty()) {
                    val onMapPolyline = Polyline(
                        polylineList, getStepsLineStyle()
                    )
                    viewModel.onEvent(MapEvents.SetRoutePolyline(onMapPolyline))
                } else {
                    clearRoutingLine()
                }


            }
            routeNextStep.observe(viewLifecycleOwner) { step ->
                binding.apply {
                    cvNextStep.visibility = if (step == null) View.GONE else View.VISIBLE
                    step?.let {
                        nextStep.tvStepDistance.text = it.distance?.text ?: ""
                        nextStep.tvStepInstruction.text = it.instruction
                        getDrawable(
                            requireContext(),
                            DecodedSteps.ModifierEnum.getModifier(it.modifier).icon
                        ).also { nextStep.ivStepDirection.setImageDrawable(it) }
                    }
                }

            }
            routeCurrentStep.observe(viewLifecycleOwner) { step ->
                binding.apply {
                    currentStep.tvStepDistance.text = step?.distance?.text ?: ""
                    currentStep.tvStepInstruction.text = step?.instruction
                    getDrawable(
                        requireContext(), DecodedSteps.ModifierEnum.getModifier(step?.modifier).icon
                    ).also { currentStep.ivStepDirection.setImageDrawable(it) }
                }

                viewModel.onEvent(MapEvents.SnapToLine)
                step?.decodedPolyline?.let {
                    if (moveCameraToNavigatorLocation) {
                        val bearing = calculateBearing(it.first(), it.last())
                        binding.map.setBearing(bearing, 1f)
                        binding.map.setTilt(40f, 1f)
                        binding.map.setZoom(20f, 0.5f)
                        binding.map.moveCamera(
                            it.first(), 0.5f
                        )
                    }
                }
            }
            routingStatus.observe(viewLifecycleOwner) {
                when (it) {
                    is RoutingStatus.Start -> {
                        binding.isRouting = true
                        viewModel.onEvent(MapEvents.InProgressRouting)
                        viewModel.onEvent(MapEvents.SetUserLocation(viewModel.originLocation.value))
                        onUpdateUserLocation(null)
                        onUpdateOriginLocation(null)
                        onUpdateDestinationLocation(null)
                    }

                    is RoutingStatus.InProgress -> {
                        binding.isRouting = true
                        backPressedCallback.isEnabled = true
                        binding.cvBackToRoute.visibility = View.GONE
                        backToRouteTimer.cancel()
                        moveCameraToNavigatorLocation = true
                        viewModel.onEvent(MapEvents.SetRoutCurrentStep(viewModel.routeCurrentStep.value))
                    }

                    is RoutingStatus.Cancel -> {
                        backPressedCallback.isEnabled = false
                        binding.cvBackToRoute.visibility = View.GONE
                        backToRouteTimer.cancel()
                        binding.isRouting = false
                        enableButtons()
                        clearRoutingLine()
                        moveCameraToUserLocation = true
                        viewModel.onEvent(MapEvents.SetUserLocation(viewModel.userLocation.value))
                    }


                    is RoutingStatus.Finished -> {
                        backPressedCallback.isEnabled = false
                        binding.cvBackToRoute.visibility = View.GONE
                        backToRouteTimer.cancel()
                        binding.isRouting = false
                        enableButtons()
                        clearRoutingLine()
                        moveCameraToUserLocation = true
                        viewModel.onEvent(MapEvents.SetUserLocation(viewModel.userLocation.value))
                        getString(R.string.finish_routing).toast(requireContext())
                    }

                }

            }
        }

    }

    private fun closeSteps() {
        isStepsOpen = false
        binding.rcSteps.visibility = View.GONE

        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.constraintLayout2)
        constraintSet.clear(
            R.id.cvTop, ConstraintSet.BOTTOM
        )
        constraintSet.applyTo(binding.constraintLayout2)
    }

    private fun openSteps() {
        isStepsOpen = true
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.constraintLayout2)
        constraintSet.connect(
            R.id.cvTop, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 15
        )
        constraintSet.applyTo(binding.constraintLayout2)
        binding.rcSteps.visibility = View.VISIBLE
    }

    private fun disableButtons() {
        binding.apply {
            btnClearOriginLocation.isEnabled = false
            btnClearDestinationLocation.isEnabled = false
            llOriginLocation.isEnabled = false
            llDestinationLocation.isEnabled = false
        }
    }

    private fun enableButtons() {
        binding.apply {
            btnClearOriginLocation.isEnabled = true
            btnClearDestinationLocation.isEnabled = true
            llOriginLocation.isEnabled = true
            llDestinationLocation.isEnabled = true
        }
    }

    private fun mapSetPosition(overview: Boolean) {
        val centerFirstMarkerX = viewModel.originMarker.value!!.latLng.latitude
        val centerFirstMarkerY = viewModel.originMarker.value!!.latLng.longitude
        if (overview) {
            val centerFocalPositionX =
                (centerFirstMarkerX + viewModel.destinationMarker.value!!.latLng.latitude) / 2
            val centerFocalPositionY =
                (centerFirstMarkerY + viewModel.destinationMarker.value!!.latLng.longitude) / 2
            binding.map.moveCamera(LatLng(centerFocalPositionX, centerFocalPositionY), 0.5f)
        } else {
            binding.map.moveCamera(LatLng(centerFirstMarkerX, centerFirstMarkerY), 0.5f)


        }
    }


    private fun onUpdateUserLocation(loc: Location?) {
        if (loc == null) {
            if (viewModel.userMarker.value != null) {
                binding.map.removeMarker(viewModel.userMarker.value)
                viewModel.onEvent(MapEvents.SetUserMarker(null))
            }
            return
        }

        if (viewModel.userMarker.value != null) {
            viewModel.userMarker.value?.setLatLng(loc.getLatLng())
            return
        }
        val latLng = loc.getLatLng()
        val userMarkerStyle = MapShapesUtil.buildUserMarkerStyle(resources = resources)
        viewModel.onEvent(MapEvents.SetUserMarker(Marker(latLng, userMarkerStyle).apply {
            title = "User"

        }))
        if (moveCameraToUserLocation) {
            binding.map.setZoom(16f, 0.5f)
            binding.map.setBearing(loc.bearing, 0.5f)
            binding.map.setTilt(90f, 1f)
            binding.map.moveCamera(
                LatLng(latLng.latitude, latLng.longitude), 0.25f
            )
            moveCameraToUserLocation = false
        }
    }

    private fun onUpdateNavigatorLocation(loc: Location?) {
        if (loc == null) {
            if (viewModel.navigatorMarker.value != null) {
                binding.map.removeMarker(viewModel.navigatorMarker.value)
                viewModel.onEvent(MapEvents.SetNavigatorMarker(null))
            }
            return
        }
        if (viewModel.navigatorMarker.value != null) {
            viewModel.navigatorMarker.value?.setLatLng(loc.getLatLng())
            if (moveCameraToNavigatorLocation) {
                binding.map.moveCamera(
                    LatLng(loc.latitude, loc.longitude), 0.25f
                )
            }
            return
        }

        val latLng = loc.getLatLng()
        val navigatorMarkerStyle = MapShapesUtil.buildNavigationMarkerStyle(resources = resources)
        viewModel.onEvent(
            MapEvents.SetNavigatorMarker(Marker(
                latLng, navigatorMarkerStyle
            ).apply {
                title = "navigator"
            })
        )
        if (moveCameraToNavigatorLocation)
            binding.map.moveCamera(
                LatLng(latLng.latitude, latLng.longitude), 0.25f
            )
    }

    private fun onUpdateOriginLocation(loc: Location?) {
        if (viewModel.originMarker.value != null) {
            binding.map.removeMarker(viewModel.originMarker.value)
            viewModel.onEvent(MapEvents.SetOriginMarker(null))
        }

        if (loc == null) {
            clearPolyLine()
        }


        if (loc == null) return
        val location = loc.getLatLng()
        val marketStyle = MapShapesUtil.buildOriginMarkerStyle(resources = resources)
        viewModel.onEvent(MapEvents.SetOriginMarker(Marker(location, marketStyle).apply {
            title = "Origin"
        }))
        binding.map.moveCamera(
            LatLng(location.latitude, location.longitude), 0.25f
        )
    }

    private fun onUpdateDestinationLocation(loc: Location?) {

        if (viewModel.destinationMarker.value != null) {
            binding.map.removeMarker(viewModel.destinationMarker.value)
            viewModel.onEvent(MapEvents.SetDestinationMarker(null))
        }
        if (loc == null) {
            clearPolyLine()
        }
        if (loc == null) return
        val location = loc.getLatLng()

        val marketStyle = MapShapesUtil.buildDestinationMarkerStyle(resources = resources)

        viewModel.onEvent(MapEvents.SetDestinationMarker(Marker(location, marketStyle).apply {
            title = "Destination"
        }))
        binding.map.moveCamera(
            LatLng(location.latitude, location.longitude), 0.25f
        )
    }

    private fun drawPolyline(origin: Location?, destination: Location?) {
        if (origin == null) return
        if (destination == null) return
        if (viewModel.originToDestinationLine.value != null) {
            binding.map.removePolyline(viewModel.originToDestinationLine.value)
            viewModel.onEvent(MapEvents.SetOriginToDestinationLine(null))
        }
        val latLngs = ArrayList<LatLng>()
        latLngs.add(origin.getLatLng())
        latLngs.add(destination.getLatLng())
        val polyline = Polyline(latLngs, getLineStyle())
        viewModel.onEvent(MapEvents.SetOriginToDestinationLine(polyline))

    }

    private fun getLineStyle() = LineStyleBuilder().apply {
        color = Color(2, 119, 189, 190)
        width = 6f
        stretchFactor = 10f
        lineEndType = LineEndType.LINE_END_TYPE_ROUND
        lineJoinType = LineJoinType.LINE_JOIN_TYPE_ROUND

    }.buildStyle()


    private fun getStepsLineStyle() = LineStyleBuilder().apply {
        color = Color(2, 119, 189, 190)
        width = 20f
        stretchFactor = 10f
        lineEndType = LineEndType.LINE_END_TYPE_ROUND
        lineJoinType = LineJoinType.LINE_JOIN_TYPE_ROUND

    }.buildStyle()


    private fun backToTheRoute() {
        binding.cvBackToRoute.visibility = View.GONE
        binding.pbBackToRoute.progress = 0
        viewModel.onEvent(MapEvents.InProgressRouting)
    }


}


package io.dee.portal.map_screen.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.carto.graphics.Color
import com.carto.styles.AnimationStyle
import com.carto.styles.AnimationStyleBuilder
import com.carto.styles.AnimationType
import com.carto.styles.LineStyle
import com.carto.styles.LineStyleBuilder
import com.carto.styles.MarkerStyle
import com.carto.styles.MarkerStyleBuilder
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
import io.dee.portal.utils.LocationProviderState
import io.dee.portal.utils.NetworkStatus
import kotlinx.coroutines.launch
import org.neshan.common.model.LatLng
import org.neshan.mapsdk.internal.utils.BitmapUtils
import org.neshan.mapsdk.model.Marker
import org.neshan.mapsdk.model.Polyline
import org.neshan.mapsdk.style.NeshanMapStyle.NESHAN
import org.neshan.mapsdk.style.NeshanMapStyle.NESHAN_NIGHT
import kotlin.math.pow
import kotlin.math.sqrt


@AndroidEntryPoint
class MapFragment : BaseFragment() {
    private val TAG = MapFragment::class.java.name
    private val REQUEST_CODE = 100
    private lateinit var binding: FragmentMapBinding
    private val viewModel: MapViewModel by viewModels()

    private lateinit var stepsAdapter: StepsAdapter
    private var isStepsOpen = false
    private var shouldMoveCameraToUserLocation: Boolean = true

    private lateinit var sensorManager: SensorManager
    private val mySensorEventListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        }

        override fun onSensorChanged(event: SensorEvent) {


        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        binding.map.setZoom(15f, 0f)
        binding.map.cachePath = requireContext().cacheDir
        binding.map.cacheSize = 20


        binding.map.settings.isMapRotationEnabled = true


        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        viewModel.onEvent(MapEvents.StopLocationUpdates)
        sensorManager.unregisterListener(mySensorEventListener)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

    override fun bindVariables() {
        stepsAdapter = StepsAdapter(onStepSelected = { position ->
            if (viewModel.routingSteps.value == null) return@StepsAdapter
            val steps = viewModel.routingSteps.value!!.subList(0, position + 1)
            val decodedSteps = java.util.ArrayList<LatLng?>()
            steps.forEach {
                it.decodedPolyline?.let {
                    decodedSteps.addAll(it)
                }
            }
            val onMapPolyline = Polyline(
                decodedSteps, getStepsLineStyle()
            )
            clearRouteSteppedLine()
            closeSteps()
            viewModel.onEvent(MapEvents.SetRouteSteppedLine(onMapPolyline))
            viewModel.onEvent(MapEvents.SetRoutCurrentStep(stepsAdapter.currentList[position]))
        })
    }

    override fun bindViews() {
        binding.apply {
            binding.cvTop.setOnClickListener {
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
//            btnCancelRouting.setOnClickListener {
//                enableButtons()
//                clearRoutingLine()
//                clearRouteSteppedLine()
//                viewModel.onEvent(MapEvents.CancelRouting)
//                binding.isRouting = false
//                viewModel.onEvent(MapEvents.SetOriginLocation(viewModel.originLocation.value))
//            }
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

    private fun clearRouteSteppedLine() {
        if (viewModel.routeSteppedPolyline.value != null) {
            binding.map.removePolyline(viewModel.routeSteppedPolyline.value)
            viewModel.onEvent(MapEvents.SetRouteSteppedLine(null))
        }
    }


    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)?.also { accelerometer ->
            sensorManager.registerListener(
                mySensorEventListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        val systemUiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        binding.map.setMapStyle(
            when (systemUiMode) {
                Configuration.UI_MODE_NIGHT_YES -> NESHAN_NIGHT
                else -> NESHAN
            }
        )
        viewModel.onEvent(MapEvents.StartLocationUpdates)
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

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                mainViewModel.connectivityStatus.collect {
                    if (it == NetworkStatus.Connected) {
                        viewModel.onEvent(MapEvents.StartLocationUpdates)

                    }
                }
            }
        }
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

                mapSetPosition(false)
            }
            routeSteppedPolyline.observe(viewLifecycleOwner) { line ->
                line?.let {
                    binding.map.addPolyline(it)
                }
                mapSetPosition(false)
            }
            userLocation.observe(viewLifecycleOwner) {
                (it == null).also {
                    binding.btnMyLocation.isEnabled = !it
                    binding.btnMyLocation.alpha = if (it) 0.15f else 1f
                }
                if (binding.isRouting) {
                    val a = snapToLine(
                        it!!.getLatLng(),
                        viewModel.routingOverView.value!!
                    )
                    binding.map.moveCamera(a, 0.5f)
                    onUpdateUserLocation(Location(a))
                    val currentStep = findCurrentStep(
                        it!!.getLatLng(),
                        viewModel.routingSteps.value!!
                    )
                    viewModel.routingSteps.value!!.getOrNull(index = currentStep)?.let {
                        viewModel.onEvent(MapEvents.SetRoutCurrentStep(it))
                    }

                } else
                    onUpdateUserLocation(it)


            }

            viewLifecycleOwner.lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    userLocationProvider.collect { state ->
                        when (state) {
                            is LocationProviderState.Success -> {
                                viewModel.onEvent(MapEvents.SetUserLocation(state.location))

                            }

                            is LocationProviderState.Error -> {
                                val statusCode = (state.exception as ApiException).statusCode
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
                                }
                            }

                            else -> {}
                        }

                    }
                }
            }
            originLocation.observe(viewLifecycleOwner) {
                onUpdateOriginLocation(it)
                if (destinationLocation.value != null) {
                    drawPolyline(it, destinationLocation.value)
                }
            }
            destinationLocation.observe(viewLifecycleOwner) {
                onUpdateDestinationLocation(it)
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

            routingState.observe(viewLifecycleOwner) { state ->
                when (state) {
                    is RoutingState.Loading -> {
                        binding.routingInProgress = true
                        disableButtons()
                    }

                    is RoutingState.Success -> {
                        clearPolyLine()
                        binding.isRouting = true
                        binding.routingInProgress = false
                        val steps =
                            state.routeSteps.fold(mutableListOf<LatLng>()) { acc, decodedSteps ->
                                decodedSteps.decodedPolyline?.let {
                                    acc.addAll(it)
                                }
                                return@fold acc
                            }
                        viewModel.onEvent(MapEvents.SetRoutingOverView(steps))
                        viewModel.onEvent(MapEvents.SetRoutingSteps(state.routeSteps))
                    }

                    is RoutingState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT)
                            .show()
                        binding.isRouting = false
                        binding.routingInProgress = false
                        enableButtons()
                    }
                }
            }
            routingOverView.observe(viewLifecycleOwner) { overView ->
                overView?.let {
                    val onMapPolyline = Polyline(
                        it as java.util.ArrayList<LatLng>, getLineStyle()
                    )


                    viewModel.onEvent(MapEvents.SetRoutePolyline(onMapPolyline))
                }
            }
            routingSteps.observe(viewLifecycleOwner) { steps ->
                val list = steps?.filter { it != steps.firstOrNull() }
                stepsAdapter.submitList(list) {
                    binding.rcSteps.scrollToPosition(0)
                    viewModel.onEvent(MapEvents.SetRoutCurrentStep(list?.firstOrNull()))
                }
                steps?.let {
                    it.firstOrNull()?.let {
                        val bearing = calculateBearing(
                            it.decodedPolyline?.first()!!,
                            it.decodedPolyline.last()!!
                        )
                        binding.map.setZoom(20f, 0.5f)
                        binding.map.setBearing(bearing.toFloat(), 1f)
                        binding.map.setTilt(0f, 1f)
                        binding.map.moveCamera(
                            it.decodedPolyline.first()!!, 0.5f
                        )
                    }
                }
            }
            routCurrentStep.observe(viewLifecycleOwner) { step ->
                binding.stepsItemView.tvStepDistance.text = step?.distance?.text ?: ""
                binding.stepsItemView.tvStepInstruction.text = step?.instruction
                val drawable = getDrawable(
                    requireContext(), DecodedSteps.ModifierEnum.getModifier(step?.modifier).icon
                )
                binding.stepsItemView.ivStepDirection.setImageDrawable(drawable)
                step?.decodedPolyline?.let {
                    val bearing = calculateBearing(it.first(), it.last())
                    binding.map.setBearing(bearing.toFloat(), 1f)
                    binding.map.setTilt(0f, 1f)
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
        clearRouteSteppedLine()
        viewModel.onEvent(MapEvents.SetRoutCurrentStep(viewModel.routingSteps.value?.firstOrNull()))
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
//        val centerFirstMarkerX = viewModel.originMarker.value!!.latLng.latitude
//        val centerFirstMarkerY = viewModel.originMarker.value!!.latLng.longitude
//        if (overview) {
//            val centerFocalPositionX =
//                (centerFirstMarkerX + viewModel.destinationMarker.value!!.latLng.latitude) / 2
//            val centerFocalPositionY =
//                (centerFirstMarkerY + viewModel.destinationMarker.value!!.latLng.longitude) / 2
//            binding.map.moveCamera(LatLng(centerFocalPositionX, centerFocalPositionY), 0.5f)
//        } else {
//            binding.map.moveCamera(LatLng(centerFirstMarkerX, centerFirstMarkerY), 0.5f)
//
//
//        }
    }

    private fun calculateBearing(loc1: LatLng, loc2: LatLng): Float {
        val startLocation = android.location.Location("startLocation").apply {
            latitude = loc1.latitude
            longitude = loc1.longitude
        }

        val endLocation = android.location.Location("endLocation").apply {
            latitude = loc2.latitude
            longitude = loc2.longitude
        }

        return 360 - startLocation.bearingTo(endLocation)

    }


    private fun onUpdateUserLocation(loc: Location?) {
        if (loc == null) return
        if (viewModel.userMarker.value != null) {
            binding.map.removeMarker(viewModel.userMarker.value)
            viewModel.onEvent(MapEvents.SetUserMarker(null))
        }
        val latLng = loc.getLatLng()
        val userMarkerStyle = buildUserMarkerStyle()
        viewModel.onEvent(MapEvents.SetUserMarker(Marker(latLng, userMarkerStyle).apply {
            title = "User"

        }))


        if (shouldMoveCameraToUserLocation) {
            binding.map.moveCamera(
                LatLng(latLng.latitude, latLng.longitude), 0.25f
            )
            shouldMoveCameraToUserLocation = false
        }
    }

    private fun onUpdateOriginLocation(loc: Location?) {
        if (viewModel.originMarker.value != null) {
            binding.map.removeMarker(viewModel.originMarker.value)
            viewModel.onEvent(MapEvents.SetOriginMarker(null))
        }
        binding.isOriginFilled = loc != null
        if (loc == null) {
            clearPolyLine()
        }

//        binding.tvOriginLocation.text = loc?.getAddressOrLatLngString() ?: ""
        binding.origin = loc
        if (loc == null) return
        val location = loc.getLatLng()
        val marketStyle = buildOriginMarkerStyle()
        viewModel.onEvent(MapEvents.SetOriginMarker(Marker(location, marketStyle).apply {
            title = "Origin"
        }))
        binding.map.moveCamera(
            LatLng(location.latitude, location.longitude), 0.25f
        )
    }

    private fun onUpdateDestinationLocation(loc: Location?) {
        binding.isDestinationFilled = loc != null
        binding.destination = loc
        if (viewModel.destinationMarker.value != null) {
            binding.map.removeMarker(viewModel.destinationMarker.value)
            viewModel.onEvent(MapEvents.SetDestinationMarker(null))
        }
        if (loc == null) {
            clearPolyLine()
        }
        if (loc == null) return
        val location = loc.getLatLng()

        val marketStyle = buildDestinationMarkerStyle()

        viewModel.onEvent(MapEvents.SetDestinationMarker(Marker(location, marketStyle).apply {
            title = "Destination"
        }))
        binding.map.moveCamera(
            LatLng(location.latitude, location.longitude), 0.25f
        )
    }

    private fun getAnimationStyle(): AnimationStyle? {
        val animStBl = AnimationStyleBuilder()
        animStBl.fadeAnimationType = AnimationType.ANIMATION_TYPE_SMOOTHSTEP
        animStBl.sizeAnimationType = AnimationType.ANIMATION_TYPE_SPRING
        animStBl.phaseInDuration = 0.2f
        animStBl.phaseOutDuration = 0.2f
        return animStBl.buildStyle()
    }

    private fun buildUserMarkerStyle(): MarkerStyle {
        val style = MarkerStyleBuilder()
        style.size = 20f
        style.bitmap = BitmapUtils.createBitmapFromAndroidBitmap(
            BitmapFactory.decodeResource(
                resources, org.neshan.mapsdk.R.drawable.ic_marker
            )
        )
        style.animationStyle = getAnimationStyle()

        return style.buildStyle()
    }

    private fun buildOriginMarkerStyle(): MarkerStyle {
        val style = MarkerStyleBuilder()
        style.size = 20f
        style.bitmap = BitmapUtils.createBitmapFromAndroidBitmap(
            BitmapFactory.decodeResource(
                resources, org.neshan.mapsdk.R.drawable.ic_cluster_marker_blue
            )
        )
        style.animationStyle = getAnimationStyle()

        return style.buildStyle()
    }

    private fun buildDestinationMarkerStyle(): MarkerStyle {
        val style = MarkerStyleBuilder()
        style.size = 20f
        style.bitmap = BitmapUtils.createBitmapFromAndroidBitmap(
            BitmapFactory.decodeResource(
                resources, org.neshan.mapsdk.R.drawable.ic_cluster_marker_blue
            )
        )
        style.animationStyle = getAnimationStyle()

        return style.buildStyle()
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

    private fun getLineStyle(): LineStyle {
        val lineStyleBuilder = LineStyleBuilder()
        lineStyleBuilder.color = Color(2, 119, 189, 190)
        lineStyleBuilder.width = 4f
        lineStyleBuilder.stretchFactor = 0f
        return lineStyleBuilder.buildStyle()
    }

    private fun getStepsLineStyle(): LineStyle {
        val lineStyleBuilder = LineStyleBuilder()
        lineStyleBuilder.color = Color(R.color.red)
        lineStyleBuilder.width = 4f
        lineStyleBuilder.stretchFactor = 0f
        return lineStyleBuilder.buildStyle()
    }


    fun snapToLine(location: LatLng, polyline: List<LatLng>): LatLng {
        var closestPoint: LatLng? = null
        var minDistance = Double.MAX_VALUE

        for (i in 0 until polyline.size - 1) {
            val segmentStart = polyline[i]
            val segmentEnd = polyline[i + 1]

            val projectedPoint = getClosestPointOnSegment(location, segmentStart, segmentEnd)
            val distance = distanceBetween(location, projectedPoint)

            if (distance < minDistance) {
                minDistance = distance
                closestPoint = projectedPoint
            }
        }

        return closestPoint ?: location
    }

    private fun getClosestPointOnSegment(p: LatLng, a: LatLng, b: LatLng): LatLng {
        val apx = p.latitude - a.latitude
        val apy = p.longitude - a.longitude
        val abx = b.latitude - a.latitude
        val aby = b.longitude - a.longitude

        val ab2 = abx * abx + aby * aby
        val ap_ab = apx * abx + apy * aby
        val t = ap_ab / ab2

        val clampT = t.coerceIn(0.0, 1.0)

        return LatLng(
            a.latitude + clampT * abx,
            a.longitude + clampT * aby
        )
    }

    private fun distanceBetween(a: LatLng, b: LatLng): Double {
        val latDiff = a.latitude - b.latitude
        val lngDiff = a.longitude - b.longitude
        return sqrt(latDiff.pow(2) + lngDiff.pow(2))
    }

    fun findCurrentStep(
        currentLocation: LatLng,
        steps: List<DecodedSteps>,
        tolerance: Double = 0.0001
    ): Int {
        var closestStepIndex = -1
        var minDistance = Double.MAX_VALUE

        // Loop through each step and its list of LatLng points
        for (stepIndex in steps.indices) {
            val polyline = steps[stepIndex].decodedPolyline ?: return -1

            // Find the closest point on this step's polyline to the current location
            for (i in 0 until polyline.size - 1) {
                val segmentStart = polyline[i]
                val segmentEnd = polyline[i + 1]

                val closestPoint =
                    getClosestPointOnSegment(currentLocation, segmentStart, segmentEnd)
                val distance = distanceBetween(currentLocation, closestPoint)

                // If this is the closest step, update the closestStepIndex
                if (distance < minDistance && distance <= tolerance) {
                    minDistance = distance
                    closestStepIndex = stepIndex
                }
            }
        }

        return closestStepIndex // Returns the index of the current step
    }

}


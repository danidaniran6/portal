package io.dee.portal.map_screen.view

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.carto.graphics.Color
import com.carto.styles.LineStyle
import com.carto.styles.LineStyleBuilder
import com.carto.styles.MarkerStyle
import com.carto.styles.MarkerStyleBuilder
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.SettingsClient
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dagger.hilt.android.AndroidEntryPoint
import io.dee.portal.BuildConfig
import io.dee.portal.data.local.Location
import io.dee.portal.databinding.FragmentMapBinding
import io.dee.portal.search_driver.view.SearchDriverBottomSheet
import io.dee.portal.search_screen.view.SearchScreenBottomSheet
import org.neshan.common.model.LatLng
import org.neshan.mapsdk.internal.utils.BitmapUtils
import org.neshan.mapsdk.model.Marker
import org.neshan.mapsdk.model.Polyline
import org.neshan.mapsdk.style.NeshanMapStyle.NESHAN
import org.neshan.mapsdk.style.NeshanMapStyle.NESHAN_NIGHT
import java.text.DateFormat
import java.util.Date

@AndroidEntryPoint
class MapFragment : Fragment() {
    private val TAG = MapFragment::class.java.name
    private val REQUEST_CODE = 123
    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 3000
    private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 1000

    private lateinit var binding: FragmentMapBinding
    private val viewModel: MapViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    private lateinit var settingsClient: SettingsClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationSettingsRequest: LocationSettingsRequest
    private var lastUpdateTime: String? = null
    private var mRequestingLocationUpdates: Boolean? = null
    private var shouldMoveCameraToUserLocation: Boolean = true

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let {
                viewModel.onEvent(MapEvents.SetUserLocation(Location(it)))
                lastUpdateTime = DateFormat.getTimeInstance().format(Date())
            }

        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        binding.map.setZoom(15f, 0.25f)
        binding.map.cachePath = requireContext().cacheDir
        binding.map.cacheSize = 20

        val systemUiMode =
            resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK
        binding.map.setMapStyle(
            when (systemUiMode) {
                Configuration.UI_MODE_NIGHT_YES -> NESHAN_NIGHT
                else -> NESHAN
            }
        )

        return binding.root
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindObservers()
        bindViews()
    }

    private fun bindViews() {
        binding.apply {
            map.setOnMarkerClickListener {
                it.showInfoWindow()
                true
            }
            btnCancelRouting.setOnClickListener {
                clearRoutingLine()
                viewModel.onEvent(MapEvents.CancelRouting)
                binding.isRouting = false
                viewModel.onEvent(MapEvents.SetOriginLocation(viewModel.originLocation.value))
            }
            btnSearchDriver.setOnClickListener {
                SearchDriverBottomSheet(onDriverFound = {
                    viewModel.onEvent(MapEvents.UpdateDriver(it))
                }).show(childFragmentManager, "")
            }
            btnMyLocation.setOnClickListener {
                shouldMoveCameraToUserLocation = true
                viewModel.onEvent(MapEvents.SetUserLocation(viewModel.userLocation.value))
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

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onStart() {
        super.onStart()
        bindFused()

    }

    private fun startReceivingLocationUpdates() {

        Dexter.withContext(requireContext())
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    mRequestingLocationUpdates = true
                    startLocationUpdates()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    if (response.isPermanentlyDenied) {
                        // open device settings when the permission is
                        // denied permanently
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

    private fun bindFused() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        settingsClient = LocationServices.getSettingsClient(requireActivity())

        mRequestingLocationUpdates = false
        locationRequest = LocationRequest()
        locationRequest.numUpdates = 10
        locationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        locationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        locationSettingsRequest = builder.build()
        startReceivingLocationUpdates()

    }


    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener(requireActivity()) {
                fusedLocationClient.lastLocation.addOnSuccessListener(requireActivity()) { location ->
                    if (location != null) {
                        val loc = Location(location)
                        viewModel.onEvent(MapEvents.SetUserLocation(loc))
                    }
                }
                fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.myLooper()
                )

            }.addOnFailureListener { e ->
                val statusCode = (e as ApiException).statusCode
                when (statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        Log.i(
                            TAG,
                            "Location settings are not satisfied. Attempting to upgrade " + "location settings "
                        )
                        if (mRequestingLocationUpdates == true) {
                            try {
                                val rae = e as ResolvableApiException
                                rae.startResolutionForResult(requireActivity(), REQUEST_CODE)
                            } catch (sie: IntentSender.SendIntentException) {
                                Log.i(
                                    TAG, "PendingIntent unable to execute request."
                                )
                            }
                        }
                    }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val errorMessage =
                            "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                        Log.e(
                            TAG, errorMessage
                        )
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
                onUpdateUserLocation(viewModel.userLocation.value)
            }

    }


    fun bindObservers() {
        viewModel.apply {

            originToDestinationLine.observe(viewLifecycleOwner) { line ->
                line?.let {
                    binding.map.addPolyline(it)
                    viewModel.originLocation.value?.let { origin ->
                        binding.map.moveCamera(origin.getLatLng(), .5f)
                    }
                }
            }
            routePolyline.observe(viewLifecycleOwner) { line ->
                line?.let {
                    binding.map.addPolyline(it)
                }
                mapSetPosition(viewModel.destinationMarker.value != null)
            }
            userLocation.observe(viewLifecycleOwner) {
                onUpdateUserLocation(it)
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
            driver.observe(viewLifecycleOwner) { driverInfo ->
                driverInfo?.let {
                    viewModel.onEvent(MapEvents.GetRoute)
                    binding.tvDriverName.text = it.name
                    binding.tvDriverCar.text = it.car.getCarName()
                    binding.tvDriverPlateNumber.text = it.car.plateNumber.getPlateNumber()
                    binding.tvDriverPlateCityId.text = it.car.plateNumber.code
                }

            }
            routingState.observe(viewLifecycleOwner) { state ->
                when (state) {
                    is RoutingState.Loading -> {
                        binding.routingInProgress = true
                        disableButtons()
                        clearPolyLine()
                    }

                    is RoutingState.Success -> {
                        val onMapPolyline = Polyline(
                            state.routeOverviewPolylinePoints, getLineStyle()
                        )
                        viewModel.onEvent(MapEvents.SetRoutePolyline(onMapPolyline))
                        binding.isRouting = true
                        binding.routingInProgress = false
                        enableButtons()
                    }

                    is RoutingState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        binding.isRouting = false
                        binding.routingInProgress = false
                        enableButtons()
                    }
                }
            }
        }

    }

    private fun disableButtons() {
        binding.apply {
            btnClearOriginLocation.isEnabled = false
            btnClearDestinationLocation.isEnabled = false
            tvOriginLocation.isEnabled = false
            tvDestinationLocation.isEnabled = false
        }
    }

    private fun enableButtons() {
        binding.apply {
            btnClearOriginLocation.isEnabled = true
            btnClearDestinationLocation.isEnabled = true
            tvOriginLocation.isEnabled = true
            tvDestinationLocation.isEnabled = true
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
            binding.map.setZoom(14f, 0.5f)
        } else {
            binding.map.moveCamera(LatLng(centerFirstMarkerX, centerFirstMarkerY), 0.5f)
            binding.map.setZoom(14f, 0.5f)
        }
    }


    private fun onUpdateUserLocation(loc: Location?) {

        if (loc == null) return
        if (viewModel.userMarker.value != null) {
            binding.map.removeMarker(viewModel.userMarker.value)
            viewModel.onEvent(MapEvents.SetUserMarker(null))
        }
        val location = LatLng(loc.latitude, loc.longitude)
        val userMarkerStyle = buildUserMarkerStyle()
        viewModel.onEvent(MapEvents.SetUserMarker(Marker(location, userMarkerStyle).apply {
            title = "User"
        }))
        if (shouldMoveCameraToUserLocation) {
            binding.map.moveCamera(
                LatLng(location.latitude, location.longitude), 0.25f
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

    private fun buildUserMarkerStyle(): MarkerStyle {
        val style = MarkerStyleBuilder()
        style.size = 20f
        style.bitmap = BitmapUtils.createBitmapFromAndroidBitmap(
            BitmapFactory.decodeResource(
                resources, org.neshan.mapsdk.R.drawable.ic_marker
            )
        )

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
        return lineStyleBuilder.buildStyle()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback).addOnCompleteListener {
            Toast.makeText(requireContext(), "Location updates stopped!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(
                requireContext(), "Location updates failed to stop!", Toast.LENGTH_SHORT
            ).show()
        }.addOnSuccessListener {
            Toast.makeText(requireContext(), "Location updates stopped!", Toast.LENGTH_SHORT).show()
        }.addOnCanceledListener {
            Toast.makeText(requireContext(), "Location updates canceled!", Toast.LENGTH_SHORT)
                .show()
        }
    }

}


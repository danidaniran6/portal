package io.dee.portal.view.map_screen.view

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
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
import io.dee.portal.view.search_screen.view.SearchScreenBottomSheet
import org.neshan.common.model.LatLng
import org.neshan.mapsdk.internal.utils.BitmapUtils
import org.neshan.mapsdk.model.Marker
import org.neshan.mapsdk.model.Polyline
import java.text.DateFormat
import java.util.Date

@AndroidEntryPoint
class MapFragment : Fragment() {
    private val TAG = "MapFragment"
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
            viewModel.updateUserLocation(locationResult.lastLocation)
            lastUpdateTime = DateFormat.getTimeInstance().format(Date())
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
//
//        binding.map.settings.setNeshanLogoMargins(50, 50)
//        binding.map.isPoiEnabled = true
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
            btnMyLocation.setOnClickListener {
                shouldMoveCameraToUserLocation = true
                viewModel.updateUserLocation(viewModel.userLocation.value)
            }
            llOriginLocation.setOnClickListener {
                SearchScreenBottomSheet(binding.map.cameraTargetPosition,
                    currentLocationSelected = {
                        viewModel.updateOriginLocation(viewModel.userLocation.value)
                    },
                    onItemSearched = {
                        viewModel.updateOriginLocation(it)
                    }).show(childFragmentManager, "")
            }
            llDestinationLocation.setOnClickListener {
                SearchScreenBottomSheet(binding.map.cameraTargetPosition,
                    currentLocationSelected = {
                        viewModel.updateDestinationLocation(viewModel.userLocation.value)
                    },
                    onItemSearched = {
                        viewModel.updateDestinationLocation(it)
                    }).show(childFragmentManager, "")
            }
            btnClearOriginLocation.setOnClickListener {
                viewModel.updateOriginLocation(null)
                if (viewModel.originMarker.value != null) {
                    binding.map.removeMarker(viewModel.originMarker.value)
                    viewModel.updateUserMarker(null)
                }

            }
            btnClearDestinationLocation.setOnClickListener {
                viewModel.updateDestinationLocation(null)
                if (viewModel.destinationMarker.value != null) {
                    binding.map.removeMarker(viewModel.destinationMarker.value)
                    viewModel.updateDestinationMarker(null)
                }
            }

            map.setOnMapLongClickListener {
                viewModel.updateDestinationLocation(
                    Location(
                        it.latitude, it.longitude, ""
                    )
                )
            }

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
                    if (response.isPermanentlyDenied()) {
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
                        viewModel.updateUserLocation(location)
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
            userMarker.observe(viewLifecycleOwner) { marker ->
                marker?.let {
                    binding.map.addMarker(marker)
                }
            }
            originMarker.observe(viewLifecycleOwner) { marker ->
                marker?.let {
                    binding.map.addMarker(marker)
                }

            }
            destinationMarker.observe(viewLifecycleOwner) { marker ->
                marker?.let {
                    binding.map.addMarker(marker)
                }

            }

            userLocation.observe(viewLifecycleOwner) { location ->
                onUpdateUserLocation(location)
            }
            originLocation.observe(viewLifecycleOwner) { origin ->
                binding.isOriginFilled = origin != null
                binding.tvOriginLocation.text = origin?.getAddressOrLatLngString() ?: ""

                onUpdateOriginLocation(origin)
                if (destinationLocation.value != null) {
                    drawPolyline()
                }
            }
            destinationLocation.observe(viewLifecycleOwner) { destination ->
                binding.isDestinationFilled = destination != null
                binding.tvDestinationLocation.setText(destination?.getAddressOrLatLngString() ?: "")
                onUpdateDestinationLocation(destination)
                if (originLocation.value != null) {
                    drawPolyline()
                }

            }
        }
    }


    private fun onUpdateUserLocation(loc: Location?) {
        if (loc == null) return
        val location = LatLng(loc.latitude, loc.longitude)
        binding.map.setZoom(15f, 0.25f)
        if (viewModel.userMarker.value != null) {
            binding.map.removeMarker(viewModel.userMarker.value)
            viewModel.updateUserMarker(null)
        }
        val userMarkerStyle = buildUserMarkerStyle()
        viewModel.updateUserMarker(Marker(location, userMarkerStyle).apply {
            title = "User"
        })
        if (shouldMoveCameraToUserLocation) {
            binding.map.moveCamera(
                LatLng(location.latitude, location.longitude), 0.25f
            )
            shouldMoveCameraToUserLocation = false
        }
    }

    private fun onUpdateOriginLocation(loc: Location?) {
        if (loc == null) return
        val location = loc.getLatLng()
        if (viewModel.originMarker.value != null) {
            binding.map.removeMarker(viewModel.originMarker.value)
            viewModel.updateOriginMarker(null)
        }
        val marketStyle = buildOriginMarkerStyle()
        viewModel.updateOriginMarker(Marker(location, marketStyle).apply {
            title = "Origin"
        })
        binding.map.moveCamera(
            LatLng(location.latitude, location.longitude), 0.25f
        )
    }

    private fun onUpdateDestinationLocation(loc: Location?) {
        if (loc == null) return
        val location = loc.getLatLng()
        if (viewModel.destinationMarker.value != null) {
            binding.map.removeMarker(viewModel.destinationMarker.value)
            viewModel.updateDestinationMarker(null)
        }
        val marketStyle = buildDestinationMarkerStyle()
        viewModel.updateDestinationMarker(Marker(location, marketStyle).apply {
            title = "Destination"
        })
        binding.map.moveCamera(
            LatLng(location.latitude, location.longitude), 0.25f
        )
    }

    private fun buildUserMarkerStyle(): MarkerStyle {
        val style = MarkerStyleBuilder()
        style.size = 10f
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

    private fun drawPolyline() {
        if (viewModel.originLocation.value == null) return
        if (viewModel.destinationLocation.value == null) return
        val latLngs = ArrayList<LatLng>()
        latLngs.add(viewModel.originLocation.value!!.getLatLng())
        latLngs.add(viewModel.destinationLocation.value!!.getLatLng())
        val polyline = Polyline(latLngs, getLineStyle())
        binding.map.addPolyline(polyline)
        binding.map.moveCamera(viewModel.originLocation.value!!.getLatLng(), .5f)
    }

    fun getLineStyle(): LineStyle {
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
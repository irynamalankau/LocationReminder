package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var map: GoogleMap
    private var pointOfInterest: PointOfInterest? = null
    private var selectedLatLng: LatLng? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)


        //call this function after the user confirms on the selected location
        binding.saveLocationButton.setOnClickListener {
            onLocationSelected()
        }
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())


        return binding.root
    }


    private fun onLocationSelected() {

        if ((pointOfInterest == null) && (selectedLatLng == null)) {
            // Tell user that no location has been selected
            Toast.makeText(context, getString(R.string.toast_select_location), Toast.LENGTH_LONG)
                .show()
        } else {
            if (pointOfInterest != null) {
                _viewModel.selectedPOI.value = pointOfInterest
                _viewModel.reminderSelectedLocationStr.value = pointOfInterest!!.name
                _viewModel.latitude.value = pointOfInterest!!.latLng.latitude
                _viewModel.longitude.value = pointOfInterest!!.latLng.longitude
            } else {
                _viewModel.latitude.value = selectedLatLng!!.latitude
                _viewModel.longitude.value = selectedLatLng!!.longitude
                _viewModel.reminderSelectedLocationStr.value = getString(R.string.dropped_pin)
            }
            // Navigate back to the previous fragment to save the reminder
            _viewModel.navigationCommand.value = NavigationCommand.Back
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setMapStyle(map)
        setPoiClick(map)
        setMapLongClick(map)
        checkPermission()
    }

    @SuppressLint("MissingPermission")
    private fun checkPermission() {
        if (isPermissionGranted()) {
            enableLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }
    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        map.isMyLocationEnabled = true
        zoomToUserLocation()
    }


    @SuppressLint("MissingPermission")
    private fun zoomToUserLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener {
            if (it != null) {
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            it.latitude,
                            it.longitude
                        ), 15f
                    )
                )

            } else {
                Log.d(TAG, "map DIDN'T work in fusedlocation")
                map.moveCamera(CameraUpdateFactory.zoomIn())
            }
        }
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            // A Snippet is Additional text that's displayed below the title.
            map.clear()
            selectedLatLng = latLng
            pointOfInterest = null
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )

            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )

        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            map.clear()
            pointOfInterest = poi

            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )

            map.addCircle(
                CircleOptions()
                    .center(poi.latLng)
                    .radius(100.0)
                    .strokeColor(Color.argb(255, 255, 0, 0))
                    .fillColor(Color.argb(64, 255, 0, 0)).strokeWidth(2F)
            )

            poiMarker.showInfoWindow()
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    activity,
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    //PERMISSIONS

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check if location permissions are granted and if so enable the
        // location data layer.
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableLocation()
            } else {
                getAlertDialog()
            }
        }
    }

    private fun getAlertDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.location_required_explanation))
            .setPositiveButton("Ok") { dialog, _ ->
                run {
                    dialog.dismiss()
                }
            }

        builder.create().show()
    }

    companion object {
        private const val TAG = "SelectLocationFragment"
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

}





























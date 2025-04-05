package com.example.ogz_pgz

import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.location.Location
import android.location.LocationListener
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.ogz_pgz.R
import com.example.ogz_pgz.model.CoordinateConverter
import model.Coordinate
import model.GeodesicParameters
import model.PGZCalculator

class PgzFragment : Fragment() {

    private val pgzViewModel: PgzViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as MainApplication).repository)
    }
    private val mapViewModel: MapViewModel by activityViewModels()

    private lateinit var locationManager: LocationManager

    private lateinit var editTextPoint: EditText
    private lateinit var editTextDistance: EditText
    private lateinit var editTextElevation: EditText
    private lateinit var editTextAzimuth: EditText
    private lateinit var textViewResultCoordinate: TextView
    private lateinit var calculateButton: Button
    private lateinit var buttonGetCoordinates: ImageButton


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pgz, container, false)

        editTextPoint = view.findViewById(R.id.editTextPointPGZ)
        editTextDistance = view.findViewById(R.id.editTextDistancePGZ)
        editTextElevation = view.findViewById(R.id.editTextElevationPGZ)
        editTextAzimuth = view.findViewById(R.id.editTextAzimuthPGZ)
        textViewResultCoordinate = view.findViewById(R.id.textViewResultCoordinate)
        calculateButton = view.findViewById(R.id.buttonCalculatePGZ)
        buttonGetCoordinates = view.findViewById(R.id.imageButtonGetCoordinatesPGZ)

        MainActivity.setupMask(editTextPoint)
        MainActivity.setupAngleMask(editTextElevation)
        MainActivity.setupAngleMask(editTextAzimuth)


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        allObserve()

        getParams()

        setOnClickListeners()

        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

    }

    private fun setParams(){
        pgzViewModel.setParams(
            editTextPoint.text.toString(),
            editTextDistance.text.toString(),
            editTextElevation.text.toString(),
            editTextAzimuth.text.toString()
        )

        pgzViewModel.setResult(
            textViewResultCoordinate.text.toString()
        )

        mapViewModel.updateMapPoints(CoordinateConverter.parseLatLon(editTextPoint.text.toString()), CoordinateConverter.parseLatLon(textViewResultCoordinate.text.toString().removePrefix("Coordinate: ")))
    }

    private fun allObserve(){
        pgzViewModel.coordinate.observe(viewLifecycleOwner, Observer { newValue -> editTextPoint.setText(newValue) })
        pgzViewModel.distance.observe(viewLifecycleOwner, Observer { newValue -> editTextDistance.setText(newValue) })
        pgzViewModel.elevation.observe(viewLifecycleOwner, Observer { newValue -> editTextElevation.setText(newValue) })
        pgzViewModel.azimuth.observe(viewLifecycleOwner, Observer { newValue -> editTextAzimuth.setText(newValue) })
        pgzViewModel.resCoordinate.observe(viewLifecycleOwner, Observer { newValue -> textViewResultCoordinate.text = newValue })
    }

    private fun setOnClickListeners(){
        calculateButton.setOnClickListener {
            textViewResultCoordinate.text = "Coordinate:"

            val coordinate = CoordinateConverter.parseCoordinate(editTextPoint.text.toString())

            val distance = editTextDistance.text.toString().toDoubleOrNull() ?: 0.0
            val elevation = CoordinateConverter.parseAngle(editTextElevation.text.toString())
            val azimuth = CoordinateConverter.parseAngle(editTextAzimuth.text.toString())

            val geodesicParameters = GeodesicParameters(distance = distance, azimuth = azimuth, elevationAngle = elevation, distanceIncline = 0.0)

            val resCoordinate = PGZCalculator.calculate(coordinate, geodesicParameters)

            val resCoordinateStr = CoordinateConverter.coordinateToString(resCoordinate)

            val rcParse = CoordinateConverter.parseCoordinate(resCoordinateStr)

            pgzViewModel.saveCalculation(coordinate, geodesicParameters, rcParse)

            textViewResultCoordinate.text = "${textViewResultCoordinate.text} ${resCoordinateStr}"

            setParams()
        }

        buttonGetCoordinates.setOnClickListener {
            requestLocationPermissionsAndGetLocation()
            mapViewModel.updatePoint1(CoordinateConverter.parseLatLon(editTextPoint.text.toString()))
        }
    }

    private fun getParams(){
        editTextPoint.setText(pgzViewModel.coordinate.value)
        editTextDistance.setText(pgzViewModel.distance.value)
        editTextElevation.setText(pgzViewModel.elevation.value)
        editTextAzimuth.setText(pgzViewModel.azimuth.value)
        textViewResultCoordinate.setText(pgzViewModel.resCoordinate.value)
    }


    private fun requestLocationPermissionsAndGetLocation() {
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCoarseLocationPermission && hasFineLocationPermission) {
            getCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getCurrentLocation() {
        try {
            // Проверяем разрешения еще раз перед запросом локации
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            // Проверяем, включен ли GPS
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // Здесь можно показать диалог с просьбой включить GPS
                return
            }

            // Запрашиваем последнее известное местоположение
            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastKnownLocation != null) {
                val coordinate = Coordinate(latitude = lastKnownLocation.latitude, longitude = lastKnownLocation.longitude, altitude = 0.0)
                pgzViewModel.setCoordinate(CoordinateConverter.coordinateToString(coordinate))
            }

            // Запрашиваем актуальное местоположение
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000, // минимальное время между обновлениями в мс
                10f,  // минимальное расстояние между обновлениями в метрах
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        val coordinate = Coordinate(latitude = location.latitude, longitude = location.longitude, altitude = 0.0)
                        pgzViewModel.setCoordinate(CoordinateConverter.coordinateToString(coordinate))
                        locationManager.removeUpdates(this)
                    }

                }
            )
        } catch (e: Exception) {
            Log.e("PgzFragment", "Error getting location: ${e.message}")
        }
    }



    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

        fun newInstance(param1: String, param2: String) =
            PgzFragment().apply {

            }
    }
}
package com.example.ogz_pgz

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
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
import com.example.ogz_pgz.model.OGZCalculator

class OgzFragment : Fragment() {

    private val ogzViewModel: OgzViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as MainApplication).repository)
    }

    private val mapViewModel: MapViewModel by activityViewModels()

    private lateinit var locationManager: LocationManager

    private lateinit var editTextFirstPointOGZ: EditText
    private lateinit var editTextSecondPointOGZ: EditText
    private lateinit var textViewDistanceTopo: TextView
    private lateinit var textViewDistanceLong: TextView
    private lateinit var textViewElevationAngle: TextView
    private lateinit var textViewAzimuth: TextView
    private lateinit var calculateButton: Button
    private lateinit var buttonGetCoordinates: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ogz, container, false)

        editTextFirstPointOGZ = view.findViewById(R.id.editTextFirstPointOGZ)
        editTextSecondPointOGZ = view.findViewById(R.id.editTextSecondPointOGZ)
        textViewDistanceTopo = view.findViewById(R.id.textViewDistanceTopoOGZ)
        textViewDistanceLong = view.findViewById(R.id.textViewDistanceLongOGZ)
        textViewElevationAngle = view.findViewById(R.id.textViewElevationAngle)
        textViewAzimuth = view.findViewById(R.id.textViewAzimuth)
        calculateButton = view.findViewById(R.id.buttonCaulculateOGZ)
        buttonGetCoordinates = view.findViewById(R.id.imageButtonGetCoordinatesOGZ)

        MainActivity.setupMask(editTextFirstPointOGZ)
        MainActivity.setupMask(editTextSecondPointOGZ)


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
        ogzViewModel.setCoordinates(
            editTextFirstPointOGZ.text.toString(),
            editTextSecondPointOGZ.text.toString()
        )

        ogzViewModel.setGeoParams(
            textViewDistanceTopo.text.toString(),
            textViewDistanceLong.text.toString(),
            textViewElevationAngle.text.toString(),
            textViewAzimuth.text.toString()
        )

        mapViewModel.updateMapPoints(CoordinateConverter.parseLatLon(editTextFirstPointOGZ.text.toString()), CoordinateConverter.parseLatLon(editTextSecondPointOGZ.text.toString()))
    }

    private fun allObserve(){
        ogzViewModel.coordinate1.observe(viewLifecycleOwner, Observer { newValue -> editTextFirstPointOGZ.setText(newValue) })
        ogzViewModel.coordinate2.observe(viewLifecycleOwner, Observer { newValue -> editTextSecondPointOGZ.setText(newValue) })
        ogzViewModel.distanceTopo.observe(viewLifecycleOwner, Observer { newValue -> textViewDistanceTopo.text = newValue })
        ogzViewModel.distanceLong.observe(viewLifecycleOwner, Observer { newValue -> textViewDistanceLong.text = newValue })
        ogzViewModel.elevationAngle.observe(viewLifecycleOwner, Observer { newValue -> textViewElevationAngle.text = newValue })
        ogzViewModel.azimuth.observe(viewLifecycleOwner, Observer { newValue -> textViewAzimuth.text = newValue })
    }

    private fun setOnClickListeners(){
        calculateButton.setOnClickListener {
            textViewDistanceTopo.text = "Distance topo, m"
            textViewDistanceLong.text = "Distance long, m"
            textViewElevationAngle.text = "Elevation angle, degree"
            textViewAzimuth.text = "Azimuth, degree"

            val coordinate1 = CoordinateConverter.parseCoordinate(editTextFirstPointOGZ.text.toString())
            val coordinate2 = CoordinateConverter.parseCoordinate(editTextSecondPointOGZ.text.toString())

            val geodesicParameters = OGZCalculator.calculate(coordinate1, coordinate2)

            ogzViewModel.saveCalculation(coordinate1, coordinate2, geodesicParameters)

            textViewDistanceTopo.text = "${textViewDistanceTopo.text}: ${geodesicParameters.distance}"
            textViewDistanceLong.text = "${textViewDistanceLong.text}: ${geodesicParameters.distanceIncline}"
            textViewElevationAngle.text = "${textViewElevationAngle.text}: ${CoordinateConverter.angleToDms(geodesicParameters.elevationAngle*100)}"
            textViewAzimuth.text = "${textViewAzimuth.text}: ${CoordinateConverter.angleToDms(geodesicParameters.azimuth)}"

            setParams()
        }

        buttonGetCoordinates.setOnClickListener {
            requestLocationPermissionsAndGetLocation()
            mapViewModel.updatePoint1(CoordinateConverter.parseLatLon(editTextFirstPointOGZ.text.toString()))
        }
    }

    private fun getParams(){
        editTextFirstPointOGZ.setText(ogzViewModel.coordinate1.value)
        editTextSecondPointOGZ.setText(ogzViewModel.coordinate2.value)
        textViewDistanceTopo.setText(ogzViewModel.distanceTopo.value)
        textViewDistanceLong.setText(ogzViewModel.distanceLong.value)
        textViewElevationAngle.setText(ogzViewModel.elevationAngle.value)
        textViewAzimuth.setText(ogzViewModel.azimuth.value)
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
                ogzViewModel.setFirstCoordinate(CoordinateConverter.coordinateToString(coordinate))
            }

            // Запрашиваем актуальное местоположение
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000, // минимальное время между обновлениями в мс
                10f,  // минимальное расстояние между обновлениями в метрах
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        val coordinate = Coordinate(latitude = location.latitude, longitude = location.longitude, altitude = 0.0)
                        ogzViewModel.setFirstCoordinate(CoordinateConverter.coordinateToString(coordinate))
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

    }

}
package com.example.ogz_pgz

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.location.Location
import android.location.LocationListener
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.ogz_pgz.OgzFragment.Companion
import com.example.ogz_pgz.R
import com.example.ogz_pgz.model.CoordinateConverter
import com.example.ogz_pgz.model.VincentyCalculator
import model.Coordinate
import model.GeodesicParameters

class PgzFragment : Fragment() {

    private val pgzViewModel: PgzViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as MainApplication).repository)
    }
    private val mapViewModel: MapViewModel by activityViewModels()

    private val locationManager by lazy {
        requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private lateinit var locationListener: LocationListener

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

        mapViewModel.updateMapPoints(CoordinateConverter.fromDMS(editTextPoint.text.toString()), CoordinateConverter.fromDMS(textViewResultCoordinate.text.toString().removePrefix("Coordinate: ")))
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

            val coordinate = CoordinateConverter.fromDMS(editTextPoint.text.toString())

            val distance = editTextDistance.text.toString().toDoubleOrNull() ?: 0.0
            val elevation = CoordinateConverter.angleDMSToDouble(editTextElevation.text.toString())
            val azimuth = CoordinateConverter.angleDMSToDouble(editTextAzimuth.text.toString())

            val geodesicParameters = GeodesicParameters(distance = distance, azimuth = azimuth, elevationAngle = elevation, distanceIncline = 0.0)

            val resCoordinate = VincentyCalculator().calculatePGZ(coordinate, geodesicParameters)

            val resCoordinateStr = CoordinateConverter.toDMS(resCoordinate)

            val rcParse = CoordinateConverter.fromDMS(resCoordinateStr)

            pgzViewModel.saveCalculation(coordinate, geodesicParameters, rcParse)

            textViewResultCoordinate.text = "${textViewResultCoordinate.text} ${resCoordinateStr}"

            setParams()
        }

        buttonGetCoordinates.setOnClickListener {
            requestLocationPermissionsAndGetLocation()
            mapViewModel.updatePoint1(CoordinateConverter.fromDMS(editTextPoint.text.toString()))
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
            // Проверяем разрешения еще раз
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("PgzFragment", "Нет разрешений на местоположение")
                return
            }

            // Проверяем, включен ли GPS
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Log.d("PgzFragment", "GPS и сетевые провайдеры отключены")
                showEnableLocationDialog()
                return
            }

            // Пробуем разные провайдеры для получения последнего известного местоположения
            var lastLocation: Location? = null

            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    Log.d("PgzFragment", "Last GPS location: $lastLocation")
                }
            } catch (e: Exception) {
                Log.e("PgzFragment", "Error getting GPS location: ${e.message}")
            }

            if (lastLocation == null) {
                try {
                    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        Log.d("PgzFragment", "Last Network location: $lastLocation")
                    }
                } catch (e: Exception) {
                    Log.e("PgzFragment", "Error getting Network location: ${e.message}")
                }
            }

            if (lastLocation == null) {
                try {
                    lastLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                    Log.d("PgzFragment", "Last Passive location: $lastLocation")
                } catch (e: Exception) {
                    Log.e("PgzFragment", "Error getting Passive location: ${e.message}")
                }
            }

            // Если есть последнее известное местоположение, используем его
            if (lastLocation != null) {
                Log.d("PgzFragment", "Используем последнее известное местоположение: Lat=${lastLocation.latitude}, Lon=${lastLocation.longitude}")
                val coordinate = Coordinate(
                    latitude = lastLocation.latitude,
                    longitude = lastLocation.longitude,
                    altitude = lastLocation.altitude
                )
                pgzViewModel.setCoordinate(CoordinateConverter.toDMS(coordinate))
            } else {
                Log.d("PgzFragment", "Нет последнего известного местоположения, ждем обновлений")
            }

            // Настраиваем слушатель для получения актуальных данных
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    Log.d("PgzFragment", "Получено обновление местоположения: Lat=${location.latitude}, Lon=${location.longitude}")
                    val coordinate = Coordinate(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = location.altitude
                    )
                    pgzViewModel.setCoordinate(CoordinateConverter.toDMS(coordinate))

                    // Удаляем слушатель, если больше не нужны обновления
                    locationManager.removeUpdates(this)
                }

                override fun onProviderDisabled(provider: String) {
                    Log.d("PgzFragment", "Провайдер отключен: $provider")
                }

                override fun onProviderEnabled(provider: String) {
                    Log.d("PgzFragment", "Провайдер включен: $provider")
                }

                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
                    Log.d("PgzFragment", "Статус провайдера изменен: $provider, статус: $status")
                }
            }

            // Регистрируем слушатель для GPS и сетевого провайдера
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000, // 1 секунда
                    1f,   // 1 метр
                    locationListener
                )
                Log.d("PgzFragment", "Запрошены обновления от GPS")
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000,
                    1f,
                    locationListener
                )
                Log.d("PgzFragment", "Запрошены обновления от сети")
            }

            // Установка таймаута для получения местоположения
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    locationManager.removeUpdates(locationListener)
                    Log.d("PgzFragment", "Таймаут получения местоположения")
                } catch (e: Exception) {
                    Log.e("PgzFragment", "Ошибка при удалении слушателя: ${e.message}")
                }
            }, 30000) // 30 секунд таймаут

        } catch (e: Exception) {
            Log.e("PgzFragment", "Общая ошибка получения местоположения: ${e.message}", e)
        }
    }

    private fun showEnableLocationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Включите GPS")
            .setMessage("Для определения местоположения необходимо включить GPS. Хотите перейти в настройки?")
            .setPositiveButton("Настройки") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            if (::locationListener.isInitialized) {
                locationManager.removeUpdates(locationListener)
            }
        } catch (e: Exception) {
            Log.e("PgzFragment", "Error removing location updates: ${e.message}")
        }
    }

    // Обработка результата запроса разрешений
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PgzFragment.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                getCurrentLocation()
            } else {
                // Показать информацию о том, что без разрешений функция не работает
                Toast.makeText(
                    requireContext(),
                    "Для работы приложения необходимо разрешение на определение местоположения",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }



    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

        fun newInstance(param1: String, param2: String) =
            PgzFragment().apply {

            }
    }
}
package com.example.ogz_pgz

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
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
import com.example.ogz_pgz.R
import com.example.ogz_pgz.model.CoordinateConverter
import com.example.ogz_pgz.model.VincentyCalculator
import model.Coordinate

class OgzFragment : Fragment() {

    private val ogzViewModel: OgzViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as MainApplication).repository)
    }

    private val locationManager by lazy {
        requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private lateinit var locationListener: LocationListener

    private val mapViewModel: MapViewModel by activityViewModels()


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

        mapViewModel.updateMapPoints(CoordinateConverter.fromDMS(editTextFirstPointOGZ.text.toString()), CoordinateConverter.fromDMS(editTextSecondPointOGZ.text.toString()))
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

            val coordinate1 = CoordinateConverter.fromDMS(editTextFirstPointOGZ.text.toString())
            val coordinate2 = CoordinateConverter.fromDMS(editTextSecondPointOGZ.text.toString())

            val geodesicParameters = VincentyCalculator().calculateOGZ(coordinate1, coordinate2)

            ogzViewModel.saveCalculation(coordinate1, coordinate2, geodesicParameters)

            textViewDistanceTopo.text = "${textViewDistanceTopo.text}: ${geodesicParameters.distance}"
            textViewDistanceLong.text = "${textViewDistanceLong.text}: ${geodesicParameters.distanceIncline}"
            textViewElevationAngle.text = "${textViewElevationAngle.text}: ${CoordinateConverter.doubleToAngleDMS(geodesicParameters.elevationAngle*100)}"
            textViewAzimuth.text = "${textViewAzimuth.text}: ${CoordinateConverter.doubleToAngleDMS(geodesicParameters.azimuth)}"

            setParams()
        }

        buttonGetCoordinates.setOnClickListener {
            requestLocationPermissionsAndGetLocation()
            mapViewModel.updatePoint1(CoordinateConverter.fromDMS(editTextFirstPointOGZ.text.toString()))
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
                ogzViewModel.setFirstCoordinate(CoordinateConverter.toDMS(coordinate))
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
                    ogzViewModel.setFirstCoordinate(CoordinateConverter.toDMS(coordinate))

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
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
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

    }

}
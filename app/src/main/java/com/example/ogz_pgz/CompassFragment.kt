package com.example.ogz_pgz

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.ogz_pgz.R

class CompassFragment : DialogFragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private var lastAccelerometer = FloatArray(3)
    private var lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false

    private var rotationMatrix = FloatArray(9)
    private var orientation = FloatArray(3)
    private var currentDegree = 0f

    private lateinit var compassImageView: ImageView
    private lateinit var degreeTextView: TextView
    private lateinit var closeButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_compass, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация датчиков
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // Инициализация UI элементов
        compassImageView = view.findViewById(R.id.compass_image)
        degreeTextView = view.findViewById(R.id.degree_text)
        closeButton = view.findViewById(R.id.close_button)

        closeButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()

        // Регистрация слушателей датчиков
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()

        // Отмена регистрации слушателей датчиков при паузе
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
            lastAccelerometerSet = true
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
            lastMagnetometerSet = true
        }

        if (lastAccelerometerSet && lastMagnetometerSet) {
            SensorManager.getRotationMatrix(
                rotationMatrix, null, lastAccelerometer, lastMagnetometer
            )

            SensorManager.getOrientation(rotationMatrix, orientation)

            // Преобразование радиан в градусы
            val degree = Math.toDegrees(orientation[0].toDouble()).toFloat()

            // Преобразование в диапазон [0, 360)
            val adjustedDegree = (degree + 360) % 360

            // Плавное вращение компаса
            val rotation = -adjustedDegree
            compassImageView.rotation = rotation

            // Отображение азимута
            val displayDegree = adjustedDegree.toInt()
            val cardinal = getCardinalDirection(displayDegree)
            degreeTextView.text = "$displayDegree° $cardinal"

            currentDegree = rotation
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Не требуется реализация для этого примера
    }

    private fun getCardinalDirection(angle: Int): String {
        return when (angle) {
            in 0..22, in 338..360 -> "С"
            in 23..67 -> "СВ"
            in 68..112 -> "В"
            in 113..157 -> "ЮВ"
            in 158..202 -> "Ю"
            in 203..247 -> "ЮЗ"
            in 248..292 -> "З"
            in 293..337 -> "СЗ"
            else -> ""
        }
    }

    override fun onStart() {
        super.onStart()

        // Настройка диалога на полный экран или нужный размер
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    companion object {
        const val TAG = "CompassDialogFragment"

        fun newInstance(): CompassFragment {
            return CompassFragment()
        }
    }
}
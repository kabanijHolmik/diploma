package com.example.ogz_pgz

import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ogz_pgz.R
import com.example.ogz_pgz.repository.GeodesicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : DialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var repository: GeodesicRepository
    private lateinit var buttonCloseHistory: ImageButton
    private lateinit var buttonDeleteHistory: AppCompatImageView
    private lateinit var buttonShareHistory: AppCompatImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = (requireActivity().application as MainApplication).repository

        recyclerView = view.findViewById(R.id.recyclerViewHistory)
        historyAdapter = HistoryAdapter(requireContext())
        recyclerView.adapter = historyAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        observeHistory()

        buttonShareHistory = view.findViewById(R.id.imageViewShareHistory)
        buttonCloseHistory = view.findViewById(R.id.btnCloseHistory)
        buttonDeleteHistory = view.findViewById(R.id.imageViewGarbageTruck)

        buttonCloseHistory.setOnClickListener {
            dismiss()
        }

        buttonDeleteHistory.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        buttonShareHistory.setOnClickListener {
            shareHistoryAsCSV()
        }


    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Удаление истории")
            .setMessage("Вы уверены, что хотите удалить всю историю вычислений?")
            .setPositiveButton("Да") { _, _ ->
                repository.clearAllData()
                Toast.makeText(context, "История удалена", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun observeHistory() {
        // Используем lifecycleScope для сбора Flow данных из репозитория
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.allCalculations.collect { calculationsList ->
                    historyAdapter.setData(calculationsList)
                }
            }
        }
    }

    private fun shareHistoryAsCSV() {
        lifecycleScope.launch {
            try {
                // Собираем значения из Flow (получаем последний список)
                val calculations = repository.allCalculations.first()

                val fileName = "geodesic_history_${System.currentTimeMillis()}.csv"
                val file = File(requireContext().cacheDir, fileName)

                withContext(Dispatchers.IO) {
                    // Создаем CSV с заголовками
                    val csvWriter = FileWriter(file)

                    // Добавляем заголовки в соответствии с вашей структурой данных
                    csvWriter.append("Дата,Тип расчета,")
                    csvWriter.append("Широта 1,Долгота 1,Высота 1,")
                    csvWriter.append("Широта 2,Долгота 2,Высота 2,")
                    csvWriter.append("Расстояние,Азимут,Угол возвышения,Наклонное расстояние\n")

                    // Записываем данные в файл
                    calculations.forEach { calcWithRelations ->
                        // Дата и тип расчета
                        val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                            .format(Date(calcWithRelations.calculation.timestamp))
                        csvWriter.append("\"$date\",")
                        csvWriter.append("\"${calcWithRelations.calculation.calculationType}\",")

                        // Координаты первой точки
                        csvWriter.append("\"${calcWithRelations.coordinate1.latitude}\",")
                        csvWriter.append("\"${calcWithRelations.coordinate1.longitude}\",")
                        csvWriter.append("\"${calcWithRelations.coordinate1.altitude}\",")

                        // Координаты второй точки
                        csvWriter.append("\"${calcWithRelations.coordinate2.latitude}\",")
                        csvWriter.append("\"${calcWithRelations.coordinate2.longitude}\",")
                        csvWriter.append("\"${calcWithRelations.coordinate2.altitude}\",")

                        // Параметры
                        csvWriter.append("\"${calcWithRelations.parameters.distance}\",")
                        csvWriter.append("\"${calcWithRelations.parameters.azimuth}\",")
                        csvWriter.append("\"${calcWithRelations.parameters.elevationAngle}\",")
                        csvWriter.append("\"${calcWithRelations.parameters.distanceIncline}\"\n")
                    }

                    csvWriter.flush()
                    csvWriter.close()
                }

                // Создаем intent для отправки файла
                val intent = Intent(Intent.ACTION_SEND)
                val contentUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )

                intent.type = "text/csv"
                intent.putExtra(Intent.EXTRA_SUBJECT, "История геодезических расчетов")
                intent.putExtra(Intent.EXTRA_STREAM, contentUri)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // Запускаем выбор приложения для отправки
                startActivity(Intent.createChooser(intent, "Поделиться историей расчетов"))

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Ошибка при экспорте данных: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e(TAG, "Error exporting data", e)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    companion object {
        const val TAG = "HistoryFragment"
    }
}
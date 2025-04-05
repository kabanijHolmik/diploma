package com.example.ogz_pgz

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ogz_pgz.R
import com.example.ogz_pgz.entities.CalculationWithRelations
import com.example.ogz_pgz.entities.toDomain
import com.example.ogz_pgz.model.CoordinateConverter

class HistoryAdapter(private val context: Context) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private var calculationsList: List<CalculationWithRelations> = emptyList()

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewOperationName: TextView = itemView.findViewById(R.id.textViewOperationName)
        val textViewCoordinate1: TextView = itemView.findViewById(R.id.textViewCoordinate1)
        val textViewCoordinate2: TextView = itemView.findViewById(R.id.textViewCoordinate2)
        val textViewDistanceTopoHistory: TextView = itemView.findViewById(R.id.textViewDistanceTopoHistory)
        val textViewElevationAngleHistory: TextView = itemView.findViewById(R.id.textViewElevationAngleHistory)
        val textViewDistanceLongHistory: TextView = itemView.findViewById(R.id.textViewDistanceLongHistory)
        val textViewAzimuthHistory: TextView = itemView.findViewById(R.id.textViewAzimuthHistory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_recycler_item, parent, false)
        return HistoryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val currentItem = calculationsList[position]

        // Установка типа операции
        holder.textViewOperationName.text = currentItem.calculation.calculationType

        // Форматирование координат
        holder.textViewCoordinate1.text = CoordinateConverter.formatCoordinate(currentItem.coordinate1.toDomain())
        holder.textViewCoordinate2.text = CoordinateConverter.formatCoordinate(currentItem.coordinate2.toDomain())

        // Установка параметров
        holder.textViewDistanceTopoHistory.text = "Distance topo, m: ${formatDouble(currentItem.parameters.distance)}"
        holder.textViewElevationAngleHistory.text = "Elevation angle, °: ${CoordinateConverter.angleToDms(currentItem.parameters.elevationAngle)}"
        holder.textViewDistanceLongHistory.text = "Distance long, m: ${formatDouble(currentItem.parameters.distanceIncline)}"
        holder.textViewAzimuthHistory.text = "Azimuth, °: ${CoordinateConverter.angleToDms(currentItem.parameters.azimuth)}"
    }

    override fun getItemCount(): Int {
        return calculationsList.size
    }

    fun setData(calculations: List<CalculationWithRelations>) {
        this.calculationsList = calculations
        notifyDataSetChanged()
    }

    private fun formatDouble(value: Double): String {
        return String.format("%.3f", value)
    }
}
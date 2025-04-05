package com.example.ogz_pgz.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "calculations",
    foreignKeys = [
        ForeignKey(
            entity = CoordinateEntity::class,
            parentColumns = ["id"],
            childColumns = ["coordinate1Id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CoordinateEntity::class,
            parentColumns = ["id"],
            childColumns = ["coordinate2Id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GeodesicParametersEntity::class,
            parentColumns = ["id"],
            childColumns = ["parametersId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("coordinate1Id"),
        Index("coordinate2Id"),
        Index("parametersId")
    ]
)
data class CalculationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val calculationType: String, // "ОГЗ" или "ПГЗ"
    val coordinate1Id: Long,
    val coordinate2Id: Long,
    val parametersId: Long,
    val timestamp: Long = System.currentTimeMillis()
)
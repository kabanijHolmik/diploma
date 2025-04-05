package com.example.ogz_pgz.entities

import androidx.room.Embedded
import androidx.room.Relation

data class CalculationWithRelations(
    @Embedded val calculation: CalculationEntity,
    @Relation(
        parentColumn = "coordinate1Id",
        entityColumn = "id"
    )
    val coordinate1: CoordinateEntity,

    @Relation(
        parentColumn = "coordinate2Id",
        entityColumn = "id"
    )
    val coordinate2: CoordinateEntity,

    @Relation(
        parentColumn = "parametersId",
        entityColumn = "id"
    )
    val parameters: GeodesicParametersEntity
)
package com.maksimowiczm.foodyou.photo

import kotlinx.serialization.Serializable

@Serializable
data class PhotoFoodResult(
    val alimento: String,
    val kcal: Int,
    val proteinas: Double,
    val grasas: Double,
    val carbos: Double,
    val error: String? = null
)

interface PhotoAnalyzer {
    suspend fun analyze(imageBytes: ByteArray, fileName: String = "food.jpg"): Result<PhotoFoodResult>
}

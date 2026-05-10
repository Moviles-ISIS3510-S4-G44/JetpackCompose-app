package com.university.marketplace.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.university.marketplace.domain.Listing
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "listings")
data class ListingEntity(
    @PrimaryKey val id: String,
    val sellerId: String,
    val categoryId: String,
    val title: String,
    val description: String,
    val price: Double,
    val condition: String,
    val imagesJson: String,
    val status: String,
    val latitude: Double?,
    val longitude: Double?,
    val locationName: String?,
    val embedding: FloatArray?,
    val updatedAt: Long = System.currentTimeMillis()
)

fun ListingEntity.toDomain(): Listing {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val type = Types.newParameterizedType(List::class.java, String::class.java)
    val adapter = moshi.adapter<List<String>>(type)
    val images = adapter.fromJson(imagesJson) ?: emptyList()

    return Listing(
        id = id,
        sellerId = sellerId,
        categoryId = categoryId,
        title = title,
        description = description,
        price = price,
        condition = condition,
        images = images,
        status = status,
        latitude = latitude,
        longitude = longitude,
        locationName = locationName
    )
}

fun Listing.toEntity(embedding: FloatArray? = null): ListingEntity {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val type = Types.newParameterizedType(List::class.java, String::class.java)
    val adapter = moshi.adapter<List<String>>(type)
    val imagesJson = adapter.toJson(images)

    return ListingEntity(
        id = id,
        sellerId = sellerId,
        categoryId = categoryId,
        title = title,
        description = description,
        price = price,
        condition = condition,
        imagesJson = imagesJson,
        status = status,
        latitude = latitude,
        longitude = longitude,
        locationName = locationName,
        embedding = embedding
    )
}

class Converters {
    @TypeConverter
    fun fromFloatArray(value: FloatArray?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toFloatArray(value: String?): FloatArray? {
        return value?.split(",")?.map { it.toFloat() }?.toFloatArray()
    }
}

package com.university.marketplace.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.university.marketplace.domain.Listing
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
    val embedding: ByteArray?,
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ListingEntity) return false
        if (id != other.id) return false
        if (sellerId != other.sellerId) return false
        if (categoryId != other.categoryId) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (price != other.price) return false
        if (condition != other.condition) return false
        if (imagesJson != other.imagesJson) return false
        if (status != other.status) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false
        if (updatedAt != other.updatedAt) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sellerId.hashCode()
        result = 31 * result + categoryId.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + price.hashCode()
        result = 31 * result + condition.hashCode()
        result = 31 * result + imagesJson.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + (latitude?.hashCode() ?: 0)
        result = 31 * result + (longitude?.hashCode() ?: 0)
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}

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
        longitude = longitude
    )
}

fun Listing.toEntity(embedding: FloatArray? = null): ListingEntity {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val type = Types.newParameterizedType(List::class.java, String::class.java)
    val adapter = moshi.adapter<List<String>>(type)
    val imagesJson = adapter.toJson(images)

    // Convertir FloatArray a ByteArray usando el converter
    val embeddingBytes = Converters().fromFloatArray(embedding)

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
        embedding = embeddingBytes
    )
}

class Converters {
    @TypeConverter
    fun fromFloatArray(value: FloatArray?): ByteArray? {
        if (value == null) return null
        val buffer = ByteBuffer.allocate(value.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        value.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    @TypeConverter
    fun toFloatArray(value: ByteArray?): FloatArray? {
        if (value == null) return null
        val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
        val floats = FloatArray(value.size / 4)
        for (i in floats.indices) {
            floats[i] = buffer.getFloat(i * 4)
        }
        return floats
    }
}

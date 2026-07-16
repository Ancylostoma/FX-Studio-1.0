package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class CatalogVariant(
    val name: String,
    val price: Double
)

@Entity(tableName = "catalog_items")
data class CatalogItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val category: String,
    val variantsString: String, // Format: "name1:price1|name2:price2"
    val imageBytes: ByteArray? = null
) {
    fun getVariants(): List<CatalogVariant> {
        if (variantsString.isBlank()) return emptyList()
        return try {
            variantsString.split("|").mapNotNull {
                val parts = it.split(":")
                if (parts.size >= 2) {
                    val name = parts.subList(0, parts.size - 1).joinToString(":")
                    val price = parts.last().toDoubleOrNull() ?: 0.0
                    CatalogVariant(name, price)
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        fun createVariantsString(variants: List<CatalogVariant>): String {
            return variants.joinToString("|") { "${it.name}:${it.price}" }
        }
    }
}

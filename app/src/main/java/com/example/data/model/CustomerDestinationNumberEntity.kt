package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customer_destination_numbers",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("customerId"),
        Index("numberValue")
    ]
)
data class CustomerDestinationNumberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val dataType: CustomerDataType,
    val label: String, // Contoh: "HP utama", "HP istri", "Rumah", "Toko"
    val numberValue: String, // String agar 0 di depan tidak hilang
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

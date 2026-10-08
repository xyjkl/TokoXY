package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("transactionId"),
        Index("productId"),
        Index("destinationNumberSnapshot")
    ]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val productId: Long? = null,
    val productNameSnapshot: String,
    val categorySnapshot: String,
    val quantity: Int = 1,
    val costPrice: Long, // Harga modal snapshot saat transaksi
    val sellPrice: Long, // Harga jual snapshot saat transaksi
    val subtotal: Long, // sellPrice * quantity
    val grossProfit: Long, // (sellPrice - costPrice) * quantity
    val requiredCustomerDataType: CustomerDataType = CustomerDataType.NONE,
    val destinationLabelSnapshot: String? = null, // e.g. "HP utama", "Rumah"
    val destinationNumberSnapshot: String? = null // e.g. "081234567890"
)

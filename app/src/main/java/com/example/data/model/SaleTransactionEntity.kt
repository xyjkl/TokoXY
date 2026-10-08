package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_transactions",
    indices = [
        Index("customerId"),
        Index("createdAt")
    ]
)
data class SaleTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long? = null,
    val customerNameSnapshot: String, // Snapshot nama pelanggan saat transaksi dibuat
    val totalAmount: Long, // Total harga jual (omzet)
    val totalCost: Long, // Total harga modal
    val grossProfit: Long, // Laba kotor (totalAmount - totalCost)
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

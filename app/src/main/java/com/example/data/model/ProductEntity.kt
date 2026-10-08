package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val productType: ProductType = ProductType.DIGITAL,
    val costPrice: Long, // Harga modal dalam Rupiah (bilangan bulat)
    val defaultSellPrice: Long, // Harga jual default dalam Rupiah
    val requiredCustomerDataType: CustomerDataType = CustomerDataType.NONE,
    val isActive: Boolean = true,
    // Field persiapan untuk migrasi barang fisik & stok masa depan
    val stock: Int? = null,
    val unit: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

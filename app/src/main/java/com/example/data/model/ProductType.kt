package com.example.data.model

enum class ProductType(val label: String) {
    DIGITAL("Produk Digital"),
    FISIK("Barang Fisik"); // Ready for future physical products migration

    companion object {
        fun fromString(value: String): ProductType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: DIGITAL
        }
    }
}

package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class CustomerWithNumbers(
    @Embedded
    val customer: CustomerEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "customerId"
    )
    val numbers: List<CustomerDestinationNumberEntity> = emptyList()
) {
    /**
     * Mengambil nomor yang cocok dengan CustomerDataType yang diminta.
     */
    fun getNumbersForType(type: CustomerDataType): List<CustomerDestinationNumberEntity> {
        return numbers.filter { it.dataType == type }
    }

    /**
     * Mengambil nomor default untuk tipe tertentu, jika ada.
     */
    fun getDefaultNumberForType(type: CustomerDataType): CustomerDestinationNumberEntity? {
        val forType = getNumbersForType(type)
        return forType.find { it.isDefault } ?: if (forType.size == 1) forType.first() else null
    }
}

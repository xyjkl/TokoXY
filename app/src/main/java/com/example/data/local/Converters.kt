package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.CustomerDataType
import com.example.data.model.ProductType

class Converters {
    @TypeConverter
    fun fromCustomerDataType(value: CustomerDataType?): String {
        return value?.name ?: CustomerDataType.NONE.name
    }

    @TypeConverter
    fun toCustomerDataType(value: String?): CustomerDataType {
        return value?.let { CustomerDataType.fromString(it) } ?: CustomerDataType.NONE
    }

    @TypeConverter
    fun fromProductType(value: ProductType?): String {
        return value?.name ?: ProductType.DIGITAL.name
    }

    @TypeConverter
    fun toProductType(value: String?): ProductType {
        return value?.let { ProductType.fromString(it) } ?: ProductType.DIGITAL
    }
}

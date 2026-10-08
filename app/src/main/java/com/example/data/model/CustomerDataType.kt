package com.example.data.model

enum class CustomerDataType(val label: String, val inputHint: String) {
    NOMOR_HP("Nomor HP", "Contoh: 081234567890"),
    NOMOR_METER_PLN("Nomor Meter PLN", "Contoh: 12345678901 (11-12 digit)"),
    ID_PELANGGAN_PLN("ID Pelanggan PLN", "Contoh: 512345678912 (12 digit)"),
    NONE("Tidak Perlu", "");

    companion object {
        fun fromString(value: String): CustomerDataType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}

package com.example.data.model

enum class PaperWidthProfile(
    val id: String,
    val displayName: String,
    val paperWidthMm: Float,
    val printerDpi: Int,
    val printableWidthMm: Float,
    val charsPerLine: Int
) {
    PROFILE_58MM(
        id = "58mm",
        displayName = "58 mm (Standar MP-58N)",
        paperWidthMm = 58f,
        printerDpi = 203,
        printableWidthMm = 48f,
        charsPerLine = 32
    ),
    PROFILE_80MM(
        id = "80mm",
        displayName = "80 mm (Printer Besar)",
        paperWidthMm = 80f,
        printerDpi = 203,
        printableWidthMm = 72f,
        charsPerLine = 48
    );

    companion object {
        fun fromId(id: String?): PaperWidthProfile {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: PROFILE_58MM
        }
    }
}

data class PrinterConfig(
    val selectedDeviceAddress: String? = null,
    val selectedDeviceName: String? = null,
    val paperWidthProfile: PaperWidthProfile = PaperWidthProfile.PROFILE_58MM,
    val storeName: String = "TokoKu",
    val storeAddress: String = "",
    val storePhone: String = "",
    val receiptFooter: String = "Terima kasih atas kunjungan Anda",
    val charsPerLineOverride: Int = 32
)

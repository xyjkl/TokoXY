package com.example.ui.components

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatRupiah(amount: Long): String {
    val localeId = Locale("in", "ID")
    val formatter = NumberFormat.getCurrencyInstance(localeId)
    formatter.maximumFractionDigits = 0
    formatter.minimumFractionDigits = 0
    return formatter.format(amount)
        .replace("Rp", "Rp ")
        .replace(",00", "")
}

fun formatTanggalWaktu(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Calendar.getInstance()
    val txCal = Calendar.getInstance().apply { time = date }

    val timeFormat = SimpleDateFormat("HH:mm", Locale("in", "ID"))
    val timeStr = timeFormat.format(date)

    return if (now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)
    ) {
        "Hari ini, $timeStr"
    } else if (now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) - txCal.get(Calendar.DAY_OF_YEAR) == 1
    ) {
        "Kemarin, $timeStr"
    } else {
        val fullFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("in", "ID"))
        fullFormat.format(date)
    }
}

fun formatTanggalPendek(timestamp: Long): String {
    val format = SimpleDateFormat("dd MMM yyyy", Locale("in", "ID"))
    return format.format(Date(timestamp))
}

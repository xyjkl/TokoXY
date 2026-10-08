package com.example.util

import com.example.data.model.PrinterConfig
import com.example.data.model.TransactionWithItems
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptFormatter {

    /**
     * Format Rupiah tanpa desimal, misalnya Rp 12.000
     */
    fun formatRupiah(amount: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        return format.format(amount).replace(",00", "")
    }

    /**
     * Sanitasi teks pengguna agar tidak merusak formatting ESC/POS DantSu
     * DantSu menggunakan tag seperti [C], [L], [R], <b>, <u> dll.
     * Karakter kurung siku atau tag yang tidak sengaja dimasukkan pengguna perlu dinetralisir.
     */
    fun sanitize(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return text
            .replace("[", "(")
            .replace("]", ")")
            .replace("<", "{")
            .replace(">", "}")
            .replace("\n", " ")
            .replace("\r", "")
            .trim()
    }

    /**
     * Format tanggal dan waktu
     */
    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }

    /**
     * Memotong string panjang menjadi baris-baris sesuai batas karakter (word-wrap atau char-wrap)
     */
    fun wrapText(text: String, maxWidth: Int): List<String> {
        val sanitized = sanitize(text)
        if (sanitized.length <= maxWidth) return listOf(sanitized)

        val words = sanitized.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            if (word.length > maxWidth) {
                // Kata sangat panjang, pecah paksa
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder()
                }
                var start = 0
                while (start < word.length) {
                    val end = minOf(start + maxWidth, word.length)
                    lines.add(word.substring(start, end))
                    start += maxWidth
                }
            } else if (currentLine.length + word.length + (if (currentLine.isEmpty()) 0 else 1) <= maxWidth) {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            } else {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    /**
     * Membentuk struk transaksi pelanggan
     * PERINGATAN KEAMANAN & PRIVASI: Harga modal (totalCost, costPrice) dan estimasi laba
     * TIDAK BOLEH dicetak ke struk pelanggan!
     */
    fun formatTransactionReceipt(
        txWithItems: TransactionWithItems,
        config: PrinterConfig
    ): String {
        val tx = txWithItems.transaction
        val items = txWithItems.items
        val lineWidth = config.charsPerLineOverride.coerceAtLeast(24)

        val sb = StringBuilder()

        // Header Toko (Centered)
        sb.append("[C]<b><font size='big'>").append(sanitize(config.storeName)).append("</font></b>\n")
        if (config.storeAddress.isNotBlank()) {
            val addressLines = wrapText(config.storeAddress, lineWidth)
            for (line in addressLines) {
                sb.append("[C]").append(line).append("\n")
            }
        }
        if (config.storePhone.isNotBlank()) {
            sb.append("[C]Telp: ").append(sanitize(config.storePhone)).append("\n")
        }

        // Garis Pembatas
        val divider = "-".repeat(lineWidth)
        sb.append("[C]").append(divider).append("\n")

        // Info Transaksi
        sb.append("[L]No: ").append(sanitize(tx.invoiceNumber)).append("\n")
        sb.append("[L]Tanggal: ").append(formatDateTime(tx.createdAt)).append("\n")
        val customerName = if (tx.customerNameSnapshot.isNotBlank()) tx.customerNameSnapshot else "Pembeli Umum"
        sb.append("[L]Pelanggan: ").append(sanitize(customerName)).append("\n")

        sb.append("[C]").append(divider).append("\n")

        // Daftar Item Produk
        for (item in items) {
            // Nama produk (bisa dibungkus jika panjang)
            val productName = sanitize(item.productNameSnapshot)
            val nameLines = wrapText(productName, lineWidth)
            for (line in nameLines) {
                sb.append("[L]<b>").append(line).append("</b>\n")
            }

            // Label tujuan jika ada (misal "HP utama", "Meter Rumah")
            if (!item.destinationLabelSnapshot.isNullOrBlank()) {
                val labelLines = wrapText(item.destinationLabelSnapshot, lineWidth)
                for (l in labelLines) {
                    sb.append("[L] ").append(l).append("\n")
                }
            }

            // Nomor tujuan jika ada (misal "081234567890", pertahankan 0 di depan)
            if (!item.destinationNumberSnapshot.isNullOrBlank()) {
                sb.append("[L] ").append(sanitize(item.destinationNumberSnapshot)).append("\n")
            }

            // Baris Harga: "1 x Rp 12.000" di kiri, Subtotal di kanan
            val leftCol = "${item.quantity} x ${formatRupiah(item.sellPrice)}"
            val rightCol = formatRupiah(item.subtotal)
            sb.append("[L]").append(leftCol)
            sb.append("[R]").append(rightCol).append("\n")

            sb.append("\n") // jeda kecil antar item
        }

        sb.append("[C]").append(divider).append("\n")

        // Total Penjualan
        sb.append("[L]<b>TOTAL</b>[R]<b>").append(formatRupiah(tx.totalAmount)).append("</b>\n")
        sb.append("[C]").append(divider).append("\n")

        // Footer
        if (config.receiptFooter.isNotBlank()) {
            val footerLines = wrapText(config.receiptFooter, lineWidth)
            for (line in footerLines) {
                sb.append("[C]").append(line).append("\n")
            }
        } else {
            sb.append("[C]Terima kasih\n")
        }

        // Paper Feed secukupnya
        sb.append("\n\n\n")

        return sb.toString()
    }

    /**
     * Struk untuk "Tes Cetak"
     */
    fun formatTestReceipt(config: PrinterConfig): String {
        val lineWidth = config.charsPerLineOverride.coerceAtLeast(24)
        val sb = StringBuilder()

        sb.append("[C]<b><font size='big'>TES PRINTER TOKOKU</font></b>\n")
        sb.append("[C]Profil: ").append(config.paperWidthProfile.displayName).append("\n")
        sb.append("[C]Lebar Karakter: ").append(lineWidth).append(" cpl\n")

        val divider = "-".repeat(lineWidth)
        sb.append("[C]").append(divider).append("\n")

        sb.append("[L]Status: Berhasil Terhubung\n")
        sb.append("[L]Uji Huruf: ABCDEFGHIJKLMNOPQRSTUVWXYZ\n")
        sb.append("[L]Uji Huruf Kecil: abcdefghijklmnopqrstuvwxyz\n")
        sb.append("[L]Uji Angka: 0123456789 (0 di awal: 081234)\n")
        sb.append("[L]Contoh Rupiah: Rp 150.000\n")

        sb.append("[C]").append(divider).append("\n")
        sb.append("[L]Uji Word-wrap Nama Produk Panjang:\n")
        val longProductLines = wrapText("Paket Data Telkomsel Internet Bulanan Super Seru Max 100GB", lineWidth)
        for (l in longProductLines) {
            sb.append("[L]<b>").append(l).append("</b>\n")
        }
        sb.append("[L]1 x Rp 150.000[R]Rp 150.000\n")

        sb.append("[C]").append(divider).append("\n")
        sb.append("[C]<b>Tes Cetak Berhasil!</b>\n")
        sb.append("[C]Printer MP-58N Siap Digunakan\n")
        sb.append("\n\n\n")

        return sb.toString()
    }
}

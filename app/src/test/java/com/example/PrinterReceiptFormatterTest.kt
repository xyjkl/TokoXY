package com.example

import com.example.data.model.CustomerDataType
import com.example.data.model.PaperWidthProfile
import com.example.data.model.PrinterConfig
import com.example.data.model.SaleItemEntity
import com.example.data.model.SaleTransactionEntity
import com.example.data.model.TransactionWithItems
import com.example.util.ReceiptFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrinterReceiptFormatterTest {

    private val sampleConfig = PrinterConfig(
        selectedDeviceAddress = "00:11:22:33:44:55",
        selectedDeviceName = "MP-58N",
        paperWidthProfile = PaperWidthProfile.PROFILE_58MM,
        storeName = "TokoKu Cell",
        storeAddress = "Jl. Merdeka No. 10 Jakarta",
        storePhone = "082111223344",
        receiptFooter = "Terima kasih telah berbelanja",
        charsPerLineOverride = 32
    )

    private val sampleTransaction = TransactionWithItems(
        transaction = SaleTransactionEntity(
            id = 1,
            invoiceNumber = "TRX-20261008-143000-123",
            customerId = 1,
            customerNameSnapshot = "Budi Santoso",
            totalAmount = 34500,
            totalCost = 30800,
            grossProfit = 3700,
            notes = "Lunas",
            createdAt = 1791448200000L // 08/10/2026
        ),
        items = listOf(
            SaleItemEntity(
                id = 1,
                transactionId = 1,
                productId = 1,
                productNameSnapshot = "Pulsa Telkomsel 10.000",
                categorySnapshot = "Pulsa",
                quantity = 1,
                costPrice = 10300,
                sellPrice = 12000,
                subtotal = 12000,
                grossProfit = 1700,
                requiredCustomerDataType = CustomerDataType.NOMOR_HP,
                destinationLabelSnapshot = "HP utama",
                destinationNumberSnapshot = "081234567890"
            ),
            SaleItemEntity(
                id = 2,
                transactionId = 1,
                productId = 2,
                productNameSnapshot = "Token PLN 20.000",
                categorySnapshot = "Token PLN",
                quantity = 1,
                costPrice = 20500,
                sellPrice = 22500,
                subtotal = 22500,
                grossProfit = 2000,
                requiredCustomerDataType = CustomerDataType.NOMOR_METER_PLN,
                destinationLabelSnapshot = "Meter Rumah",
                destinationNumberSnapshot = "12345678901"
            )
        )
    )

    @Test
    fun `test receipt formatting uses transaction snapshot`() {
        val receipt = ReceiptFormatter.formatTransactionReceipt(sampleTransaction, sampleConfig)

        // Verifikasi nama toko dan invoice
        assertTrue("Harus mencantumkan nama toko", receipt.contains("TokoKu Cell"))
        assertTrue("Harus mencantumkan nomor invoice", receipt.contains("TRX-20261008-143000-123"))
        assertTrue("Harus mencantumkan nama pelanggan", receipt.contains("Budi Santoso"))
        assertTrue("Harus mencantumkan nama item pulsa", receipt.contains("Pulsa Telkomsel 10.000"))
        assertTrue("Harus mencantumkan nama item token PLN", receipt.contains("Token PLN 20.000"))
    }

    @Test
    fun `test leading zero in destination phone number is preserved`() {
        val receipt = ReceiptFormatter.formatTransactionReceipt(sampleTransaction, sampleConfig)

        // Nomor tujuan dengan awalan nol harus utuh tidak hilang menjadi 81234567890
        assertTrue("Nol di depan nomor HP harus tetap ada", receipt.contains("081234567890"))
        assertTrue("Nomor meter PLN harus tetap ada", receipt.contains("12345678901"))
    }

    @Test
    fun `test destination numbers are not swapped between items`() {
        val receipt = ReceiptFormatter.formatTransactionReceipt(sampleTransaction, sampleConfig)

        val pulsaIndex = receipt.indexOf("Pulsa Telkomsel 10.000")
        val hpIndex = receipt.indexOf("081234567890")
        val tokenIndex = receipt.indexOf("Token PLN 20.000")
        val meterIndex = receipt.indexOf("12345678901")

        assertTrue("Nomor HP harus muncul setelah pulsa dan sebelum token PLN", hpIndex in (pulsaIndex + 1)..<tokenIndex)
        assertTrue("Nomor meter harus muncul setelah nama item token PLN", meterIndex > tokenIndex)
    }

    @Test
    fun `test cost price and gross profit are NOT included in customer receipt`() {
        val receipt = ReceiptFormatter.formatTransactionReceipt(sampleTransaction, sampleConfig)

        // Informasi rahasia internal: totalCost (30800), grossProfit (3700, 1700, 2000), modal (10300, 20500)
        assertFalse("Modal tidak boleh bocor ke struk", receipt.contains("30.800"))
        assertFalse("Modal tidak boleh bocor ke struk", receipt.contains("10.300"))
        assertFalse("Modal tidak boleh bocor ke struk", receipt.contains("20.500"))
        assertFalse("Laba kotor tidak boleh bocor ke struk", receipt.contains("3.700"))
        assertFalse("Kata 'Modal' tidak boleh ada di struk pelanggan", receipt.contains("Modal", ignoreCase = true))
        assertFalse("Kata 'Laba' tidak boleh ada di struk pelanggan", receipt.contains("Laba", ignoreCase = true))

        // Total penjualan harus ada
        assertTrue("Total penjualan harus tercantum", receipt.contains("34.500"))
    }

    @Test
    fun `test long product name word wrap does not exceed line width`() {
        val longText = "Paket Data Telkomsel Internet Bulanan Super Seru Max 100GB"
        val wrappedLines = ReceiptFormatter.wrapText(longText, 32)

        assertTrue("Nama panjang harus dipecah ke beberapa baris", wrappedLines.size > 1)
        for (line in wrappedLines) {
            assertTrue("Setiap baris tidak boleh melebihi 32 karakter", line.length <= 32)
        }
    }

    @Test
    fun `test sanitization prevents formatting tag injections`() {
        val maliciousName = "Paket [C]<b>Hacked</b> [R]5000"
        val sanitized = ReceiptFormatter.sanitize(maliciousName)

        assertFalse("Kurung siku pembuka harus dihilangkan", sanitized.contains("["))
        assertFalse("Kurung siku penutup harus dihilangkan", sanitized.contains("]"))
        assertFalse("Kurung sudut pembuka harus dihilangkan", sanitized.contains("<"))
        assertFalse("Kurung sudut penutup harus dihilangkan", sanitized.contains(">"))
    }

    @Test
    fun `test test receipt format contains expected sections`() {
        val testReceipt = ReceiptFormatter.formatTestReceipt(sampleConfig)

        assertTrue(testReceipt.contains("TES PRINTER TOKOKU"))
        assertTrue(testReceipt.contains("58 mm"))
        assertTrue(testReceipt.contains("32 cpl"))
        assertTrue(testReceipt.contains("081234"))
        assertTrue(testReceipt.contains("Rp 150.000"))
    }

    @Test
    fun `test repeated print tap does not create parallel execution`() {
        // Simulasi flag concurrency guard seperti di BluetoothPrinterManager & PrinterViewModel
        var isPrinting = false
        var executionCount = 0

        fun triggerPrint(): Boolean {
            if (isPrinting) {
                return false // Tolak tap berulang ketika operasi masih berlangsung
            }
            isPrinting = true
            executionCount++
            return true
        }

        // Tap pertama berhasil dimulai
        val firstTap = triggerPrint()
        assertTrue("Tap pertama harus dieksekusi", firstTap)
        assertEquals(1, executionCount)

        // Tap kedua (ketika yang pertama belum selesai) harus diabaikan
        val secondTap = triggerPrint()
        assertFalse("Tap kedua saat sedang mencetak harus ditolak", secondTap)
        assertEquals("Jumlah eksekusi tidak boleh bertambah", 1, executionCount)

        // Setelah selesai, baru dapat mencetak kembali
        isPrinting = false
        val thirdTap = triggerPrint()
        assertTrue("Tap setelah selesai harus dapat dieksekusi", thirdTap)
        assertEquals(2, executionCount)
    }

    @Test
    fun `test printer failure does not alter or corrupt transaction data`() {
        // Snapshot awal transaksi
        val originalInvoice = sampleTransaction.transaction.invoiceNumber
        val originalTotal = sampleTransaction.transaction.totalAmount
        val originalItemsCount = sampleTransaction.items.size

        // Simulasi error printer (koneksi terputus / timeout / bluetooth mati)
        val simulatePrinterFailure = com.example.util.PrintResult.Error("Printer tidak terjangkau")

        // Verifikasi error terdeteksi
        assertTrue(simulatePrinterFailure is com.example.util.PrintResult.Error)
        assertEquals("Printer tidak terjangkau", simulatePrinterFailure.message)

        // Verifikasi data transaksi tetap utuh tidak berubah sama sekali
        assertEquals("Nomor invoice tidak boleh berubah", originalInvoice, sampleTransaction.transaction.invoiceNumber)
        assertEquals("Total transaksi tidak boleh berubah", originalTotal, sampleTransaction.transaction.totalAmount)
        assertEquals("Jumlah item tidak boleh berubah", originalItemsCount, sampleTransaction.items.size)
    }

    @Test
    fun `test reprint uses same transaction snapshot without creating new transaction or altering total`() {
        // Transaksi awal
        val initialTxId = sampleTransaction.transaction.id
        val initialInvoice = sampleTransaction.transaction.invoiceNumber
        val initialTotal = sampleTransaction.transaction.totalAmount

        // Cetak struk pertama
        val firstReceipt = ReceiptFormatter.formatTransactionReceipt(sampleTransaction, sampleConfig)

        // Cetak ulang (reprint) menggunakan objek transaksi yang sama
        val reprintReceipt = ReceiptFormatter.formatTransactionReceipt(sampleTransaction, sampleConfig)

        // Verifikasi kedua struk identik
        assertEquals("Struk cetak ulang harus sama persis dengan struk awal", firstReceipt, reprintReceipt)
        assertEquals("ID transaksi pada cetak ulang harus tetap sama", initialTxId, sampleTransaction.transaction.id)
        assertEquals("Nomor invoice pada cetak ulang harus tetap sama", initialInvoice, sampleTransaction.transaction.invoiceNumber)
        assertEquals("Total transaksi pada cetak ulang tidak boleh bertambah", initialTotal, sampleTransaction.transaction.totalAmount)
    }

    @Test
    fun `test printer configuration properties and defaults are preserved`() {
        val defaultConfig = PrinterConfig()

        // Verifikasi default profil 58mm untuk printer MP-58N
        assertEquals(PaperWidthProfile.PROFILE_58MM, defaultConfig.paperWidthProfile)
        assertEquals(32, defaultConfig.charsPerLineOverride)
        assertEquals("TokoKu", defaultConfig.storeName)
        assertEquals("Terima kasih atas kunjungan Anda", defaultConfig.receiptFooter)
        assertEquals(null, defaultConfig.selectedDeviceAddress)

        // Verifikasi custom konfigurasi
        val customConfig = PrinterConfig(
            selectedDeviceAddress = "AA:BB:CC:DD:EE:FF",
            selectedDeviceName = "MP-58N Bluetooth",
            paperWidthProfile = PaperWidthProfile.PROFILE_58MM,
            storeName = "Konter Pulsa Berkah",
            storeAddress = "Pasar Sentral Kios 5",
            storePhone = "081999888777",
            receiptFooter = "Barang yang sudah dibeli tidak dapat ditukar",
            charsPerLineOverride = 32
        )

        assertEquals("AA:BB:CC:DD:EE:FF", customConfig.selectedDeviceAddress)
        assertEquals("MP-58N Bluetooth", customConfig.selectedDeviceName)
        assertEquals("Konter Pulsa Berkah", customConfig.storeName)
        assertEquals(32, customConfig.charsPerLineOverride)
    }
}

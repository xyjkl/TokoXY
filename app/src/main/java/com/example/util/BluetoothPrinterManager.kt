package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.example.data.model.PrinterConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

data class BluetoothDeviceInfo(
    val name: String,
    val address: String
)

sealed interface PrintResult {
    data class Success(val message: String = "Data struk berhasil dikirim ke printer") : PrintResult
    data class Error(val message: String) : PrintResult
}

class BluetoothPrinterManager(
    private val context: Context
) {
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    // Mutex untuk memastikan hanya 1 operasi pencetakan yang berjalan dalam 1 waktu
    private val printMutex = Mutex()

    /**
     * Memeriksa apakah bluetooth hardware tersedia pada perangkat
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }

    /**
     * Memeriksa apakah bluetooth saat ini aktif (ON)
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Memeriksa apakah permission bluetooth yang dibutuhkan sudah diberikan
     */
    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connectGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            val scanGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            connectGranted && scanGranted
        } else {
            true // Pada API < 31, BLUETOOTH & BLUETOOTH_ADMIN adalah normal permissions di manifest
        }
    }

    /**
     * Mengambil daftar seluruh perangkat bluetooth yang sudah dipasangkan (paired devices).
     * Tidak membatasi nama agar perangkat printer dengan nama apapun dapat dipilih.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceInfo> {
        if (!isBluetoothAvailable() || !isBluetoothEnabled() || !hasBluetoothPermission()) {
            return emptyList()
        }
        val bondedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        return bondedDevices?.map { device ->
            val name = try {
                device.name ?: "Perangkat Tanpa Nama"
            } catch (e: Exception) {
                "Perangkat Bluetooth"
            }
            BluetoothDeviceInfo(
                name = name,
                address = device.address
            )
        } ?: emptyList()
    }

    /**
     * Mengirim teks ESC/POS ke printer menggunakan koneksi Bluetooth RFCOMM/SPP
     */
    suspend fun printReceipt(
        formattedText: String,
        config: PrinterConfig
    ): PrintResult = withContext(Dispatchers.IO) {
        val targetAddress = config.selectedDeviceAddress
        if (targetAddress.isNullOrBlank()) {
            return@withContext PrintResult.Error("Belum ada printer yang dipilih. Silakan atur printer terlebih dahulu.")
        }

        if (!isBluetoothAvailable()) {
            return@withContext PrintResult.Error("Bluetooth tidak didukung pada perangkat ini.")
        }

        if (!isBluetoothEnabled()) {
            return@withContext PrintResult.Error("Bluetooth dalam keadaan mati. Harap aktifkan Bluetooth.")
        }

        if (!hasBluetoothPermission()) {
            return@withContext PrintResult.Error("Izin Bluetooth belum diberikan.")
        }

        // Cegah klik ganda / eksekusi paralel menggunakan Mutex
        if (printMutex.isLocked) {
            return@withContext PrintResult.Error("Sedang memproses pencetakan lain. Harap tunggu.")
        }

        printMutex.withLock {
            var bluetoothConnection: BluetoothConnection? = null
            try {
                // Cari BluetoothConnection dari DantSu berdasarkan address
                val pairedPrinters = BluetoothPrintersConnections().list
                bluetoothConnection = pairedPrinters?.find { it.device?.address.equals(targetAddress, ignoreCase = true) }

                if (bluetoothConnection == null) {
                    // Coba buat koneksi langsung dari BluetoothAdapter jika tidak ada di list
                    @SuppressLint("MissingPermission")
                    val device = bluetoothAdapter?.getRemoteDevice(targetAddress)
                    if (device != null) {
                        bluetoothConnection = BluetoothConnection(device)
                    }
                }

                if (bluetoothConnection == null) {
                    return@withLock PrintResult.Error("Printer dengan alamat $targetAddress tidak ditemukan di daftar perangkat terpasang.")
                }

                val profile = config.paperWidthProfile
                val dpi = profile.printerDpi
                val widthMm = profile.printableWidthMm
                val chars = config.charsPerLineOverride

                // Inisialisasi ESC/POS Printer
                // Catatan teknis: DantSu EscPosPrinter(connection, dpi, widthMm, charsPerLine)
                val printer = EscPosPrinter(
                    bluetoothConnection,
                    dpi,
                    widthMm,
                    chars
                )

                // Kirim cetak ke printer
                // mmFeedPaper: 15mm feed di akhir
                printer.printFormattedText(formattedText, 15f)

                PrintResult.Success("Data struk berhasil dikirim ke printer ${config.selectedDeviceName ?: targetAddress}")
            } catch (e: IOException) {
                PrintResult.Error("Gagal menghubungkan ke printer: ${e.localizedMessage ?: "Koneksi terputus atau printer mati"}")
            } catch (e: Exception) {
                PrintResult.Error("Gagal mencetak: ${e.localizedMessage ?: e.javaClass.simpleName}")
            } finally {
                // Selalu disconnect dan bersihkan resource socket
                try {
                    bluetoothConnection?.disconnect()
                } catch (_: Exception) {}
            }
        }
    }
}

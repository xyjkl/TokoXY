package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.PaperWidthProfile
import com.example.data.model.PrinterConfig
import com.example.data.model.TransactionWithItems
import com.example.data.repository.PrinterSettingsRepository
import com.example.util.BluetoothDeviceInfo
import com.example.util.BluetoothPrinterManager
import com.example.util.PrintResult
import com.example.util.ReceiptFormatter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PrintUiEvent {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : PrintUiEvent
}

class PrinterViewModel(
    private val printerManager: BluetoothPrinterManager,
    private val settingsRepository: PrinterSettingsRepository
) : ViewModel() {

    val printerConfig: StateFlow<PrinterConfig> = settingsRepository.printerConfigFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            PrinterConfig()
        )

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = _pairedDevices.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _lastPrintStatus = MutableStateFlow<String?>(null)
    val lastPrintStatus: StateFlow<String?> = _lastPrintStatus.asStateFlow()

    private val _events = MutableSharedFlow<PrintUiEvent>()
    val events: SharedFlow<PrintUiEvent> = _events.asSharedFlow()

    init {
        refreshBluetoothState()
    }

    fun refreshBluetoothState() {
        val perm = printerManager.hasBluetoothPermission()
        _hasPermission.value = perm
        val enabled = printerManager.isBluetoothEnabled()
        _isBluetoothEnabled.value = enabled
        if (perm && enabled) {
            _pairedDevices.value = printerManager.getPairedDevices()
        } else {
            _pairedDevices.value = emptyList()
        }
    }

    fun selectPrinter(name: String, address: String) {
        viewModelScope.launch {
            settingsRepository.saveSelectedPrinter(name, address)
            _events.emit(PrintUiEvent.ShowSnackbar("Printer $name ($address) dipilih"))
        }
    }

    fun clearPrinter() {
        viewModelScope.launch {
            settingsRepository.clearSelectedPrinter()
            _events.emit(PrintUiEvent.ShowSnackbar("Pilihan printer dihapus"))
        }
    }

    fun updatePaperProfile(profile: PaperWidthProfile) {
        viewModelScope.launch {
            settingsRepository.updatePaperProfile(profile)
        }
    }

    fun updateCharsPerLine(chars: Int) {
        viewModelScope.launch {
            settingsRepository.updateCharsPerLine(chars)
        }
    }

    fun updateStoreInfo(
        storeName: String,
        storeAddress: String,
        storePhone: String,
        receiptFooter: String
    ) {
        viewModelScope.launch {
            settingsRepository.updateStoreInfo(
                storeName = storeName.trim(),
                storeAddress = storeAddress.trim(),
                storePhone = storePhone.trim(),
                receiptFooter = receiptFooter.trim()
            )
            _events.emit(PrintUiEvent.ShowSnackbar("Pengaturan toko tersimpan"))
        }
    }

    /**
     * Mencetak struk transaksi (manual dipicu pengguna).
     * Mencegah klik berulang saat masih mencetak.
     */
    fun printTransactionReceipt(txWithItems: TransactionWithItems) {
        if (_isPrinting.value) return

        val config = printerConfig.value
        if (config.selectedDeviceAddress.isNullOrBlank()) {
            viewModelScope.launch {
                _events.emit(PrintUiEvent.ShowSnackbar("Belum ada printer yang dipilih. Atur di menu Printer.", isError = true))
            }
            return
        }

        val formattedText = ReceiptFormatter.formatTransactionReceipt(txWithItems, config)

        _isPrinting.value = true
        _lastPrintStatus.value = "Menghubungkan ke ${config.selectedDeviceName ?: "printer"}..."

        viewModelScope.launch {
            try {
                val result = printerManager.printReceipt(formattedText, config)
                when (result) {
                    is PrintResult.Success -> {
                        _lastPrintStatus.value = result.message
                        _events.emit(PrintUiEvent.ShowSnackbar(result.message, isError = false))
                    }
                    is PrintResult.Error -> {
                        _lastPrintStatus.value = result.message
                        _events.emit(PrintUiEvent.ShowSnackbar(result.message, isError = true))
                    }
                }
            } finally {
                _isPrinting.value = false
            }
        }
    }

    /**
     * Menguji printer dengan teks uji coba tanpa menyimpan transaksi penjualan apa pun
     */
    fun printTestReceipt() {
        if (_isPrinting.value) return

        val config = printerConfig.value
        if (config.selectedDeviceAddress.isNullOrBlank()) {
            viewModelScope.launch {
                _events.emit(PrintUiEvent.ShowSnackbar("Pilih printer terlebih dahulu untuk tes cetak", isError = true))
            }
            return
        }

        val testText = ReceiptFormatter.formatTestReceipt(config)

        _isPrinting.value = true
        _lastPrintStatus.value = "Mengirim tes cetak ke ${config.selectedDeviceName ?: "printer"}..."

        viewModelScope.launch {
            try {
                val result = printerManager.printReceipt(testText, config)
                when (result) {
                    is PrintResult.Success -> {
                        _lastPrintStatus.value = "Tes cetak berhasil dikirim!"
                        _events.emit(PrintUiEvent.ShowSnackbar("Tes cetak berhasil dikirim ke printer!", isError = false))
                    }
                    is PrintResult.Error -> {
                        _lastPrintStatus.value = result.message
                        _events.emit(PrintUiEvent.ShowSnackbar(result.message, isError = true))
                    }
                }
            } finally {
                _isPrinting.value = false
            }
        }
    }
}

class PrinterViewModelFactory(
    private val printerManager: BluetoothPrinterManager,
    private val settingsRepository: PrinterSettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PrinterViewModel::class.java)) {
            return PrinterViewModel(printerManager, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

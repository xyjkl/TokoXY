package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SalesSummary
import com.example.data.model.CustomerDataType
import com.example.data.model.CustomerDestinationNumberEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.CustomerWithNumbers
import com.example.data.model.ProductEntity
import com.example.data.model.ProductType
import com.example.data.model.TransactionWithItems
import com.example.data.repository.TokoKuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.coroutines.cancellation.CancellationException

enum class AppNavTab(val label: String) {
    HOME("Beranda"),
    SALES("Penjualan"),
    HISTORY("Riwayat"),
    PRODUCTS("Produk"),
    CUSTOMERS("Pelanggan")
}

enum class DateFilterType(val label: String) {
    HARI_INI("Hari Ini"),
    KEMARIN("Kemarin"),
    TUJUH_HARI("7 Hari Terakhir"),
    BULAN_INI("Bulan Ini"),
    SEMUA("Semua")
}

class TokoKuViewModel(
    private val repository: TokoKuRepository
) : ViewModel() {

    // --- NAVIGATION ---
    private val _currentTab = MutableStateFlow(AppNavTab.HOME)
    val currentTab: StateFlow<AppNavTab> = _currentTab.asStateFlow()

    fun navigateTo(tab: AppNavTab) {
        _currentTab.value = tab
    }

    // --- INITIALIZATION ---
    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
            refreshTodaySummary()
        }
        viewModelScope.launch {
            repository.allCustomersWithNumbers.collect { customerList ->
                val current = _selectedCustomer.value
                if (current != null) {
                    val updated = customerList.find { it.customer.id == current.customer.id }
                    if (updated != null) {
                        _selectedCustomer.value = updated
                        syncCartWithUpdatedCustomer(updated)
                    }
                }
            }
        }
    }

    // --- PRODUK STATE ---
    val allActiveProducts: StateFlow<List<ProductEntity>> = repository.allActiveProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    private val _productCategoryFilter = MutableStateFlow<String?>(null)
    val productCategoryFilter: StateFlow<String?> = _productCategoryFilter.asStateFlow()

    private val _productOnlyActiveFilter = MutableStateFlow(false)
    val productOnlyActiveFilter: StateFlow<Boolean> = _productOnlyActiveFilter.asStateFlow()

    fun setProductSearchQuery(query: String) {
        _productSearchQuery.value = query
    }

    fun setProductCategoryFilter(category: String?) {
        _productCategoryFilter.value = category
    }

    fun setProductOnlyActiveFilter(onlyActive: Boolean) {
        _productOnlyActiveFilter.value = onlyActive
    }

    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.saveProduct(product)
        }
    }

    fun toggleProductActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleProductActive(id, isActive)
        }
    }

    // --- PELANGGAN STATE ---
    val allCustomers: StateFlow<List<CustomerWithNumbers>> = repository.allCustomersWithNumbers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery: StateFlow<String> = _customerSearchQuery.asStateFlow()

    fun setCustomerSearchQuery(query: String) {
        _customerSearchQuery.value = query
    }

    fun saveCustomer(
        customer: CustomerEntity,
        numbers: List<CustomerDestinationNumberEntity> = emptyList(),
        onSuccess: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val id = repository.saveCustomer(customer, numbers)
            onSuccess?.invoke(id)
        }
    }

    fun selectCustomerById(customerId: Long) {
        viewModelScope.launch {
            val cust = repository.getCustomerById(customerId).filterNotNull().first()
            selectCustomer(cust)
        }
    }

    fun addCustomerDestinationNumber(number: CustomerDestinationNumberEntity) {
        viewModelScope.launch {
            repository.addDestinationNumber(number)
        }
    }

    fun updateCustomerDestinationNumber(number: CustomerDestinationNumberEntity) {
        viewModelScope.launch {
            repository.updateDestinationNumber(number)
        }
    }

    fun deleteCustomerDestinationNumber(id: Long) {
        viewModelScope.launch {
            repository.deleteDestinationNumber(id)
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    // --- ALUR PENJUALAN & AUTOFILL (SALES FLOW) ---
    private val _selectedCustomer = MutableStateFlow<CustomerWithNumbers?>(null)
    val selectedCustomer: StateFlow<CustomerWithNumbers?> = _selectedCustomer.asStateFlow()

    private val _manualCustomerName = MutableStateFlow("")
    val manualCustomerName: StateFlow<String> = _manualCustomerName.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _saleNotes = MutableStateFlow("")
    val saleNotes: StateFlow<String> = _saleNotes.asStateFlow()

    private val _isSavingSale = MutableStateFlow(false)
    val isSavingSale: StateFlow<Boolean> = _isSavingSale.asStateFlow()

    private val _saleErrorMessage = MutableStateFlow<String?>(null)
    val saleErrorMessage: StateFlow<String?> = _saleErrorMessage.asStateFlow()

    fun clearSaleErrorMessage() {
        _saleErrorMessage.value = null
    }

    private val _saleSuccessTransaction = MutableStateFlow<TransactionWithItems?>(null)
    val saleSuccessTransaction: StateFlow<TransactionWithItems?> = _saleSuccessTransaction.asStateFlow()

    fun selectCustomer(customer: CustomerWithNumbers?) {
        _selectedCustomer.value = customer
        if (customer != null) {
            _manualCustomerName.value = customer.customer.name
        }
        // Aturan: Jika pelanggan diganti, evaluasi ulang nomor tujuan seluruh item di keranjang
        // agar nomor milik pelanggan sebelumnya tidak tertinggal!
        reEvaluateCartWithNewCustomer(customer)
    }

    fun setManualCustomerName(name: String) {
        _manualCustomerName.value = name
        if (_selectedCustomer.value != null && _selectedCustomer.value?.customer?.name != name) {
            _selectedCustomer.value = null
            reEvaluateCartWithNewCustomer(null)
        }
    }

    fun setSaleNotes(notes: String) {
        _saleNotes.value = notes
    }

    /**
     * Menambahkan produk ke keranjang penjualan dengan aturan autofill:
     * 1. Jenis data NONE -> tidak memerlukan nomor tujuan.
     * 2. Tepat 1 nomor cocok -> isi otomatis nomor tersebut.
     * 3. Beberapa nomor cocok dan ada yang ditandai Utama (isDefault) -> gunakan nomor utama.
     * 4. Beberapa nomor cocok TANPA nomor utama -> jangan pilih otomatis secara sepihak,
     *    biarkan selectedNumber = null agar pengguna wajib memilih nomor tujuan.
     * 5. Belum ada nomor cocok -> sediakan input manual.
     */
    fun addProductToCart(product: ProductEntity) {
        val customer = _selectedCustomer.value
        val reqType = product.requiredCustomerDataType

        val newCartItem = if (reqType == CustomerDataType.NONE) {
            CartItem(product = product)
        } else if (customer != null) {
            val matchingNumbers = customer.getNumbersForType(reqType)
            when {
                matchingNumbers.size == 1 -> {
                    CartItem(
                        product = product,
                        selectedNumber = matchingNumbers.first(),
                        isManualInput = false
                    )
                }
                matchingNumbers.size > 1 -> {
                    val defaultNum = matchingNumbers.find { it.isDefault }
                    if (defaultNum != null) {
                        CartItem(
                            product = product,
                            selectedNumber = defaultNum,
                            isManualInput = false
                        )
                    } else {
                        // Beberapa nomor tanpa nomor utama -> biarkan belum terpilih, tampilkan pilihan
                        CartItem(
                            product = product,
                            selectedNumber = null,
                            isManualInput = false,
                            manualNumberLabel = reqType.label
                        )
                    }
                }
                else -> {
                    // Belum ada nomor yang sesuai untuk jenis data ini -> input manual
                    CartItem(
                        product = product,
                        selectedNumber = null,
                        isManualInput = true,
                        manualNumberLabel = reqType.label
                    )
                }
            }
        } else {
            // Belum ada pelanggan terpilih
            CartItem(
                product = product,
                selectedNumber = null,
                isManualInput = true,
                manualNumberLabel = reqType.label
            )
        }

        _cartItems.value = _cartItems.value + newCartItem
    }

    private fun reEvaluateCartWithNewCustomer(customer: CustomerWithNumbers?) {
        _cartItems.value = _cartItems.value.map { item ->
            val reqType = item.product.requiredCustomerDataType
            if (reqType == CustomerDataType.NONE) {
                item
            } else if (customer != null) {
                val matchingNumbers = customer.getNumbersForType(reqType)
                when {
                    matchingNumbers.size == 1 -> {
                        item.copy(
                            selectedNumber = matchingNumbers.first(),
                            isManualInput = false,
                            manualNumberValue = "",
                            saveToCustomer = false
                        )
                    }
                    matchingNumbers.size > 1 -> {
                        val defaultNum = matchingNumbers.find { it.isDefault }
                        if (defaultNum != null) {
                            item.copy(
                                selectedNumber = defaultNum,
                                isManualInput = false,
                                manualNumberValue = "",
                                saveToCustomer = false
                            )
                        } else {
                            // Beberapa nomor tanpa nomor utama -> wajibkan pemilihan
                            item.copy(
                                selectedNumber = null,
                                isManualInput = false,
                                manualNumberValue = "",
                                saveToCustomer = false
                            )
                        }
                    }
                    else -> {
                        // Tidak ada nomor yang cocok pada pelanggan baru
                        item.copy(
                            selectedNumber = null,
                            isManualInput = true,
                            manualNumberValue = "",
                            saveToCustomer = false
                        )
                    }
                }
            } else {
                // Pelanggan dihapus/dikosongkan -> bersihkan nomor pelanggan lama
                item.copy(
                    selectedNumber = null,
                    isManualInput = true,
                    manualNumberValue = ""
                )
            }
        }
    }

    private fun syncCartWithUpdatedCustomer(customer: CustomerWithNumbers) {
        _cartItems.value = _cartItems.value.map { item ->
            val reqType = item.product.requiredCustomerDataType
            if (reqType == CustomerDataType.NONE) {
                item
            } else if (item.selectedNumber != null) {
                val updatedNum = customer.numbers.find { it.id == item.selectedNumber.id }
                if (updatedNum != null) {
                    item.copy(selectedNumber = updatedNum)
                } else {
                    val matching = customer.getNumbersForType(reqType)
                    if (matching.size == 1) {
                        item.copy(selectedNumber = matching.first(), isManualInput = false)
                    } else if (matching.size > 1) {
                        val def = matching.find { it.isDefault }
                        item.copy(selectedNumber = def, isManualInput = false)
                    } else {
                        item.copy(selectedNumber = null, isManualInput = true, manualNumberLabel = reqType.label)
                    }
                }
            } else if (item.isManualInput && item.manualNumberValue.isBlank()) {
                val matching = customer.getNumbersForType(reqType)
                if (matching.size == 1) {
                    item.copy(
                        selectedNumber = matching.first(),
                        isManualInput = false,
                        manualNumberValue = "",
                        saveToCustomer = false
                    )
                } else if (matching.size > 1) {
                    val def = matching.find { it.isDefault }
                    if (def != null) {
                        item.copy(
                            selectedNumber = def,
                            isManualInput = false,
                            manualNumberValue = "",
                            saveToCustomer = false
                        )
                    } else {
                        item
                    }
                } else {
                    item
                }
            } else {
                item
            }
        }
    }

    fun removeCartItem(itemId: String) {
        _cartItems.value = _cartItems.value.filterNot { it.itemId == itemId }
    }

    fun updateCartItemQuantity(itemId: String, quantity: Int) {
        if (quantity <= 0) return
        _cartItems.value = _cartItems.value.map {
            if (it.itemId == itemId) it.copy(quantity = quantity) else it
        }
    }

    fun updateCartItemSellPrice(itemId: String, sellPrice: Long) {
        if (sellPrice < 0) return
        _cartItems.value = _cartItems.value.map {
            if (it.itemId == itemId) it.copy(sellPrice = sellPrice) else it
        }
    }

    fun selectNumberForCartItem(itemId: String, number: CustomerDestinationNumberEntity) {
        _cartItems.value = _cartItems.value.map {
            if (it.itemId == itemId) {
                it.copy(
                    selectedNumber = number,
                    isManualInput = false,
                    manualNumberValue = "",
                    saveToCustomer = false
                )
            } else it
        }
    }

    fun setManualInputForCartItem(
        itemId: String,
        numberValue: String,
        label: String = "Nomor Baru",
        saveToCustomer: Boolean = false
    ) {
        _cartItems.value = _cartItems.value.map {
            if (it.itemId == itemId) {
                it.copy(
                    selectedNumber = null,
                    isManualInput = true,
                    manualNumberValue = numberValue,
                    manualNumberLabel = label,
                    saveToCustomer = saveToCustomer
                )
            } else it
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _selectedCustomer.value = null
        _manualCustomerName.value = ""
        _saleNotes.value = ""
        _saleSuccessTransaction.value = null
    }

    fun resetSaleSuccessState() {
        _saleSuccessTransaction.value = null
    }

    /**
     * Menyimpan transaksi penjualan ke database:
     * - Cegah klik ganda dengan `isSavingSale`
     * - Snapshot seluruh data
     * - Simpan nomor baru ke data pelanggan jika opsi dicentang
     */
    fun saveSaleTransaction(onComplete: (Long) -> Unit = {}) {
        if (_isSavingSale.value) return
        val currentItems = _cartItems.value
        if (currentItems.isEmpty()) return

        // Validasi kelengkapan nomor tujuan yang diperlukan
        val invalidItem = currentItems.find { !it.isValid }
        if (invalidItem != null) return

        _isSavingSale.value = true
        _saleErrorMessage.value = null

        viewModelScope.launch {
            try {
                val customer = _selectedCustomer.value
                val customerNameManual = _manualCustomerName.value

                val saleItemEntities = currentItems.map { it.toSaleItemEntity() }

                // Identifikasi nomor baru yang diminta untuk disimpan ke pelanggan
                val newNumbersToSave = mutableListOf<CustomerDestinationNumberEntity>()
                if (customer != null) {
                    for (item in currentItems) {
                        if (item.isManualInput && item.saveToCustomer && item.manualNumberValue.isNotBlank()) {
                            newNumbersToSave.add(
                                CustomerDestinationNumberEntity(
                                    customerId = customer.customer.id,
                                    dataType = item.product.requiredCustomerDataType,
                                    label = item.manualNumberLabel.ifBlank { "Nomor Baru" },
                                    numberValue = item.manualNumberValue.trim(),
                                    isDefault = false
                                )
                            )
                        }
                    }
                }

                val txId = repository.createSaleTransaction(
                    customer = customer,
                    customerNameManual = customerNameManual,
                    items = saleItemEntities,
                    notes = _saleNotes.value,
                    newNumbersToSave = newNumbersToSave
                )

                // Baca hasil transaksi tepat satu kali
                val txWithItems = repository.getTransactionById(txId).filterNotNull().first()
                _saleSuccessTransaction.value = txWithItems
                _cartItems.value = emptyList()
                _selectedCustomer.value = null
                _manualCustomerName.value = ""
                _saleNotes.value = ""
                refreshTodaySummary()
                onComplete(txId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _saleErrorMessage.value = e.message ?: "Gagal menyimpan transaksi penjualan"
            } finally {
                _isSavingSale.value = false
            }
        }
    }

    // --- RIWAYAT & RINGKASAN PENJUALAN ---
    val allTransactions: StateFlow<List<TransactionWithItems>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _historyDateFilter = MutableStateFlow(DateFilterType.HARI_INI)
    val historyDateFilter: StateFlow<DateFilterType> = _historyDateFilter.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    private val _todaySummary = MutableStateFlow(SalesSummary(0, 0, 0))
    val todaySummary: StateFlow<SalesSummary> = _todaySummary.asStateFlow()

    private val _filteredSummary = MutableStateFlow(SalesSummary(0, 0, 0))
    val filteredSummary: StateFlow<SalesSummary> = _filteredSummary.asStateFlow()

    fun setHistoryDateFilter(filter: DateFilterType) {
        _historyDateFilter.value = filter
        updateFilteredSummary()
    }

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    private fun getTodayDateRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    private fun getDateRangeForFilter(filter: DateFilterType): Pair<Long, Long>? {
        val cal = Calendar.getInstance()
        val nowEnd = cal.timeInMillis

        return when (filter) {
            DateFilterType.HARI_INI -> getTodayDateRange()
            DateFilterType.KEMARIN -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            DateFilterType.TUJUH_HARI -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, nowEnd)
            }
            DateFilterType.BULAN_INI -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, nowEnd)
            }
            DateFilterType.SEMUA -> null
        }
    }

    fun refreshTodaySummary() {
        viewModelScope.launch {
            val (start, end) = getTodayDateRange()
            _todaySummary.value = repository.getDailySummary(start, end)
            updateFilteredSummary()
        }
    }

    private fun updateFilteredSummary() {
        viewModelScope.launch {
            val range = getDateRangeForFilter(_historyDateFilter.value)
            if (range != null) {
                _filteredSummary.value = repository.getDailySummary(range.first, range.second)
            } else {
                _filteredSummary.value = repository.getDailySummary(0, Long.MAX_VALUE)
            }
        }
    }

    fun getTransactionsForCustomer(customerId: Long) = repository.getTransactionsForCustomer(customerId)
}

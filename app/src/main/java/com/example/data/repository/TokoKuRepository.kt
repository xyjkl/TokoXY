package com.example.data.repository

import com.example.data.local.CustomerDao
import com.example.data.local.ProductDao
import com.example.data.local.SaleTransactionDao
import com.example.data.local.SalesSummary
import com.example.data.model.CustomerDataType
import com.example.data.model.CustomerDestinationNumberEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.CustomerWithNumbers
import com.example.data.model.ProductEntity
import com.example.data.model.ProductType
import com.example.data.model.SaleItemEntity
import com.example.data.model.SaleTransactionEntity
import com.example.data.model.TransactionWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TokoKuRepository(
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val saleTransactionDao: SaleTransactionDao
) {

    // --- PRODUK ---
    val allActiveProducts: Flow<List<ProductEntity>> = productDao.getAllActiveProducts()
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

    fun getProductById(id: Long): Flow<ProductEntity?> = productDao.getProductById(id)

    suspend fun saveProduct(product: ProductEntity): Long = withContext(Dispatchers.IO) {
        if (product.id == 0L) {
            productDao.insertProduct(product)
        } else {
            productDao.updateProduct(product)
            product.id
        }
    }

    suspend fun toggleProductActive(id: Long, isActive: Boolean) = withContext(Dispatchers.IO) {
        productDao.setProductActive(id, isActive)
    }

    // --- PELANGGAN ---
    val allCustomersWithNumbers: Flow<List<CustomerWithNumbers>> = customerDao.getAllCustomersWithNumbers()

    fun getCustomerById(id: Long): Flow<CustomerWithNumbers?> = customerDao.getCustomerWithNumbersById(id)

    suspend fun saveCustomer(
        customer: CustomerEntity,
        numbers: List<CustomerDestinationNumberEntity> = emptyList()
    ): Long = withContext(Dispatchers.IO) {
        customerDao.saveCustomerWithNumbers(customer, numbers)
    }

    suspend fun addDestinationNumber(number: CustomerDestinationNumberEntity): Long = withContext(Dispatchers.IO) {
        customerDao.addDestinationNumberWithDefaultCheck(number)
    }

    suspend fun updateDestinationNumber(number: CustomerDestinationNumberEntity) = withContext(Dispatchers.IO) {
        if (number.isDefault) {
            customerDao.clearDefaultsForDataType(number.customerId, number.dataType)
        }
        customerDao.updateDestinationNumber(number)
    }

    suspend fun deleteDestinationNumber(id: Long) = withContext(Dispatchers.IO) {
        customerDao.deleteDestinationNumberById(id)
    }

    suspend fun deleteCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        customerDao.deleteCustomer(customer)
    }

    // --- TRANSAKSI PENJUALAN ---
    val allTransactions: Flow<List<TransactionWithItems>> = saleTransactionDao.getAllTransactionsWithItems()

    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<TransactionWithItems>> {
        return saleTransactionDao.getTransactionsByDateRange(startTime, endTime)
    }

    fun getTransactionById(id: Long): Flow<TransactionWithItems?> {
        return saleTransactionDao.getTransactionWithItemsById(id)
    }

    fun getTransactionsForCustomer(customerId: Long): Flow<List<TransactionWithItems>> {
        return saleTransactionDao.getTransactionsForCustomer(customerId)
    }

    suspend fun getDailySummary(startTime: Long, endTime: Long): SalesSummary = withContext(Dispatchers.IO) {
        val count = saleTransactionDao.getTransactionCountBetween(startTime, endTime)
        val sales = saleTransactionDao.getTotalSalesBetween(startTime, endTime)
        val profit = saleTransactionDao.getTotalGrossProfitBetween(startTime, endTime)
        SalesSummary(
            transactionCount = count,
            totalSales = sales,
            totalGrossProfit = profit
        )
    }

    suspend fun createSaleTransaction(
        customer: CustomerWithNumbers?,
        customerNameManual: String?,
        items: List<SaleItemEntity>,
        notes: String?,
        newNumbersToSave: List<CustomerDestinationNumberEntity>
    ): Long = withContext(Dispatchers.IO) {
        val totalAmount = items.sumOf { it.subtotal }
        val totalCost = items.sumOf { it.costPrice * it.quantity }
        val grossProfit = totalAmount - totalCost

        val invoiceNumber = generateInvoiceNumber()
        val customerNameSnapshot = when {
            customer != null -> customer.customer.name
            !customerNameManual.isNullOrBlank() -> customerNameManual.trim()
            else -> "Umum / Walk-in"
        }

        val transaction = SaleTransactionEntity(
            invoiceNumber = invoiceNumber,
            customerId = customer?.customer?.id,
            customerNameSnapshot = customerNameSnapshot,
            totalAmount = totalAmount,
            totalCost = totalCost,
            grossProfit = grossProfit,
            notes = notes?.takeIf { it.isNotBlank() }
        )

        saleTransactionDao.insertSaleWithItems(
            transaction = transaction,
            items = items,
            newNumbersToSaveToCustomer = newNumbersToSave,
            customerDao = customerDao
        )
    }

    private fun generateInvoiceNumber(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
        val randomSuffix = (100..999).random()
        return "TRX-${dateFormat.format(Date())}-$randomSuffix"
    }

    // --- SEED INITIAL SAMPLE DATA (FIRST LAUNCH) ---
    suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        val productCount = productDao.getProductCount()
        if (productCount == 0) {
            seedInitialProducts()
        }

        val customerCount = customerDao.getCustomerCount()
        if (customerCount == 0) {
            seedInitialCustomers()
        }
    }

    private suspend fun seedInitialProducts() {
        val initialProducts = listOf(
            // Pulsa Telkomsel
            ProductEntity(
                name = "Pulsa Telkomsel 5.000",
                category = "Pulsa",
                productType = ProductType.DIGITAL,
                costPrice = 5400,
                defaultSellPrice = 7000,
                requiredCustomerDataType = CustomerDataType.NOMOR_HP
            ),
            ProductEntity(
                name = "Pulsa Telkomsel 10.000",
                category = "Pulsa",
                productType = ProductType.DIGITAL,
                costPrice = 10300,
                defaultSellPrice = 12000,
                requiredCustomerDataType = CustomerDataType.NOMOR_HP
            ),
            ProductEntity(
                name = "Pulsa Telkomsel 25.000",
                category = "Pulsa",
                productType = ProductType.DIGITAL,
                costPrice = 24800,
                defaultSellPrice = 27000,
                requiredCustomerDataType = CustomerDataType.NOMOR_HP
            ),
            ProductEntity(
                name = "Pulsa Telkomsel 50.000",
                category = "Pulsa",
                productType = ProductType.DIGITAL,
                costPrice = 49500,
                defaultSellPrice = 52000,
                requiredCustomerDataType = CustomerDataType.NOMOR_HP
            ),
            // Pulsa Indosat
            ProductEntity(
                name = "Pulsa Indosat 10.000",
                category = "Pulsa",
                productType = ProductType.DIGITAL,
                costPrice = 10250,
                defaultSellPrice = 12000,
                requiredCustomerDataType = CustomerDataType.NOMOR_HP
            ),
            ProductEntity(
                name = "Pulsa Indosat 25.000",
                category = "Pulsa",
                productType = ProductType.DIGITAL,
                costPrice = 24800,
                defaultSellPrice = 27000,
                requiredCustomerDataType = CustomerDataType.NOMOR_HP
            ),
            // Paket Data
            ProductEntity(
                name = "Paket Data Telkomsel MAX 10GB 30 Hari",
                category = "Paket Data",
                productType = ProductType.DIGITAL,
                costPrice = 48000,
                defaultSellPrice = 53000,
                requiredCustomerDataType = CustomerDataType.NOMOR_HP
            ),
            ProductEntity(
                name = "Paket Data Indosat Freedom 15GB 30 Hari",
                category = "Paket Data",
                productType = ProductType.DIGITAL,
                costPrice = 45000,
                defaultSellPrice = 50000,
                requiredCustomerDataType = CustomerDataType.NOMOR_HP
            ),
            // Token PLN
            ProductEntity(
                name = "Token Listrik PLN 20.000",
                category = "Token PLN",
                productType = ProductType.DIGITAL,
                costPrice = 20500,
                defaultSellPrice = 22500,
                requiredCustomerDataType = CustomerDataType.NOMOR_METER_PLN
            ),
            ProductEntity(
                name = "Token Listrik PLN 50.000",
                category = "Token PLN",
                productType = ProductType.DIGITAL,
                costPrice = 50500,
                defaultSellPrice = 52500,
                requiredCustomerDataType = CustomerDataType.NOMOR_METER_PLN
            ),
            ProductEntity(
                name = "Token Listrik PLN 100.000",
                category = "Token PLN",
                productType = ProductType.DIGITAL,
                costPrice = 100500,
                defaultSellPrice = 102500,
                requiredCustomerDataType = CustomerDataType.NOMOR_METER_PLN
            ),
            // Tagihan PLN Pascabayar
            ProductEntity(
                name = "Tagihan Listrik PLN Pascabayar",
                category = "Tagihan PLN",
                productType = ProductType.DIGITAL,
                costPrice = 2500,
                defaultSellPrice = 5000,
                requiredCustomerDataType = CustomerDataType.ID_PELANGGAN_PLN
            ),
            // Voucher Game / Lainnya
            ProductEntity(
                name = "Voucher Google Play Rp 20.000",
                category = "Voucher Game",
                productType = ProductType.DIGITAL,
                costPrice = 20000,
                defaultSellPrice = 22000,
                requiredCustomerDataType = CustomerDataType.NONE
            )
        )
        productDao.insertProducts(initialProducts)
    }

    private suspend fun seedInitialCustomers() {
        // Customer 1: Budi Santoso (memiliki 2 nomor HP dan 2 nomor meter PLN)
        val budiId = customerDao.insertCustomer(
            CustomerEntity(name = "Budi Santoso", notes = "Pelanggan setia samping toko")
        )
        customerDao.insertDestinationNumbers(
            listOf(
                CustomerDestinationNumberEntity(
                    customerId = budiId,
                    dataType = CustomerDataType.NOMOR_HP,
                    label = "HP utama",
                    numberValue = "081234567890",
                    isDefault = true
                ),
                CustomerDestinationNumberEntity(
                    customerId = budiId,
                    dataType = CustomerDataType.NOMOR_HP,
                    label = "HP istri",
                    numberValue = "085678901234",
                    isDefault = false
                ),
                CustomerDestinationNumberEntity(
                    customerId = budiId,
                    dataType = CustomerDataType.NOMOR_METER_PLN,
                    label = "Rumah",
                    numberValue = "12345678901",
                    isDefault = true
                ),
                CustomerDestinationNumberEntity(
                    customerId = budiId,
                    dataType = CustomerDataType.NOMOR_METER_PLN,
                    label = "Toko",
                    numberValue = "98765432109",
                    isDefault = false
                )
            )
        )

        // Customer 2: Siti Rahma (memiliki 1 nomor HP dan 1 ID PLN)
        val sitiId = customerDao.insertCustomer(
            CustomerEntity(name = "Siti Rahma", notes = "Langganan token awal bulan")
        )
        customerDao.insertDestinationNumbers(
            listOf(
                CustomerDestinationNumberEntity(
                    customerId = sitiId,
                    dataType = CustomerDataType.NOMOR_HP,
                    label = "Pribadi",
                    numberValue = "087811223344",
                    isDefault = true
                ),
                CustomerDestinationNumberEntity(
                    customerId = sitiId,
                    dataType = CustomerDataType.ID_PELANGGAN_PLN,
                    label = "Rumah Kontrakan",
                    numberValue = "512345678912",
                    isDefault = true
                )
            )
        )

        // Customer 3: Ahmad Fauzi (hanya nomor HP)
        val ahmadId = customerDao.insertCustomer(
            CustomerEntity(name = "Ahmad Fauzi", notes = "Karyawan kantor seberang")
        )
        customerDao.insertDestinationNumbers(
            listOf(
                CustomerDestinationNumberEntity(
                    customerId = ahmadId,
                    dataType = CustomerDataType.NOMOR_HP,
                    label = "WhatsApp",
                    numberValue = "089699887766",
                    isDefault = true
                )
            )
        )
    }
}

package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.CustomerDestinationNumberEntity
import com.example.data.model.SaleItemEntity
import com.example.data.model.SaleTransactionEntity
import com.example.data.model.TransactionWithItems
import kotlinx.coroutines.flow.Flow

data class SalesSummary(
    val transactionCount: Int,
    val totalSales: Long,
    val totalGrossProfit: Long
)

@Dao
interface SaleTransactionDao {

    @Transaction
    @Query("SELECT * FROM sale_transactions ORDER BY createdAt DESC")
    fun getAllTransactionsWithItems(): Flow<List<TransactionWithItems>>

    @Transaction
    @Query("SELECT * FROM sale_transactions WHERE createdAt BETWEEN :startTime AND :endTime ORDER BY createdAt DESC")
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<TransactionWithItems>>

    @Transaction
    @Query("SELECT * FROM sale_transactions WHERE id = :id LIMIT 1")
    fun getTransactionWithItemsById(id: Long): Flow<TransactionWithItems?>

    @Transaction
    @Query("SELECT * FROM sale_transactions WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<TransactionWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: SaleTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM sale_transactions")
    suspend fun getTotalTransactionCount(): Int

    @Query("SELECT COUNT(*) FROM sale_transactions WHERE createdAt BETWEEN :startTime AND :endTime")
    suspend fun getTransactionCountBetween(startTime: Long, endTime: Long): Int

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM sale_transactions WHERE createdAt BETWEEN :startTime AND :endTime")
    suspend fun getTotalSalesBetween(startTime: Long, endTime: Long): Long

    @Query("SELECT COALESCE(SUM(grossProfit), 0) FROM sale_transactions WHERE createdAt BETWEEN :startTime AND :endTime")
    suspend fun getTotalGrossProfitBetween(startTime: Long, endTime: Long): Long

    /**
     * Menyimpan transaksi penjualan beserta seluruh item dan opsi penyimpanan nomor baru ke data pelanggan
     * secara atomik dalam satu transaksi database.
     */
    @Transaction
    suspend fun insertSaleWithItems(
        transaction: SaleTransactionEntity,
        items: List<SaleItemEntity>,
        newNumbersToSaveToCustomer: List<CustomerDestinationNumberEntity> = emptyList(),
        customerDao: CustomerDao? = null
    ): Long {
        val txId = insertTransaction(transaction)
        val itemsWithTxId = items.map { it.copy(transactionId = txId) }
        insertSaleItems(itemsWithTxId)

        // Simpan nomor baru ke pelanggan jika ada
        if (customerDao != null && newNumbersToSaveToCustomer.isNotEmpty()) {
            for (num in newNumbersToSaveToCustomer) {
                customerDao.addDestinationNumberWithDefaultCheck(num)
            }
        }

        return txId
    }
}

package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.CustomerDataType
import com.example.data.model.CustomerDestinationNumberEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.CustomerWithNumbers
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Transaction
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomersWithNumbers(): Flow<List<CustomerWithNumbers>>

    @Transaction
    @Query("SELECT * FROM customers WHERE id = :customerId LIMIT 1")
    fun getCustomerWithNumbersById(customerId: Long): Flow<CustomerWithNumbers?>

    @Transaction
    @Query("SELECT * FROM customers WHERE id = :customerId LIMIT 1")
    suspend fun getCustomerWithNumbersByIdDirect(customerId: Long): CustomerWithNumbers?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDestinationNumber(number: CustomerDestinationNumberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDestinationNumbers(numbers: List<CustomerDestinationNumberEntity>): List<Long>

    @Update
    suspend fun updateDestinationNumber(number: CustomerDestinationNumberEntity)

    @Delete
    suspend fun deleteDestinationNumber(number: CustomerDestinationNumberEntity)

    @Query("DELETE FROM customer_destination_numbers WHERE id = :id")
    suspend fun deleteDestinationNumberById(id: Long)

    @Query("UPDATE customer_destination_numbers SET isDefault = 0 WHERE customerId = :customerId AND dataType = :dataType")
    suspend fun clearDefaultsForDataType(customerId: Long, dataType: CustomerDataType)

    @Query("SELECT COUNT(*) FROM customer_destination_numbers WHERE customerId = :customerId AND dataType = :dataType")
    suspend fun countNumbersForDataType(customerId: Long, dataType: CustomerDataType): Int

    @Transaction
    suspend fun saveCustomerWithNumbers(
        customer: CustomerEntity,
        numbers: List<CustomerDestinationNumberEntity>
    ): Long {
        val customerId = if (customer.id == 0L) {
            insertCustomer(customer)
        } else {
            updateCustomer(customer)
            customer.id
        }

        // Simpan nomor dengan aturan 1 default per dataType
        for (num in numbers) {
            if (num.isDefault) {
                clearDefaultsForDataType(customerId, num.dataType)
            }
            val numToSave = num.copy(customerId = customerId)
            if (numToSave.id == 0L) {
                insertDestinationNumber(numToSave)
            } else {
                updateDestinationNumber(numToSave)
            }
        }
        return customerId
    }

    @Transaction
    suspend fun addDestinationNumberWithDefaultCheck(
        number: CustomerDestinationNumberEntity
    ): Long {
        // Cek jika nomor pertama dari tipenya, otomatis jadikan default jika belum diset
        val existingCount = countNumbersForDataType(number.customerId, number.dataType)
        val shouldBeDefault = number.isDefault || existingCount == 0

        if (shouldBeDefault) {
            clearDefaultsForDataType(number.customerId, number.dataType)
        }

        return insertDestinationNumber(number.copy(isDefault = shouldBeDefault))
    }

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun getCustomerCount(): Int
}

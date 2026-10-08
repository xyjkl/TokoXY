package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.CustomerDestinationNumberEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SaleItemEntity
import com.example.data.model.SaleTransactionEntity

@Database(
    entities = [
        ProductEntity::class,
        CustomerEntity::class,
        CustomerDestinationNumberEntity::class,
        SaleTransactionEntity::class,
        SaleItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TokoKuDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun saleTransactionDao(): SaleTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: TokoKuDatabase? = null

        fun getDatabase(context: Context): TokoKuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TokoKuDatabase::class.java,
                    "tokoku_database"
                )
                    .fallbackToDestructiveMigration(false) // Safe for future explicit migrations
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

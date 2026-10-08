package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class TransactionWithItems(
    @Embedded
    val transaction: SaleTransactionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val items: List<SaleItemEntity> = emptyList()
)

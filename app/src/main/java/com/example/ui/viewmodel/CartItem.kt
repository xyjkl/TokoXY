package com.example.ui.viewmodel

import com.example.data.model.CustomerDataType
import com.example.data.model.CustomerDestinationNumberEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SaleItemEntity
import java.util.UUID

data class CartItem(
    val itemId: String = UUID.randomUUID().toString(),
    val product: ProductEntity,
    val quantity: Int = 1,
    val sellPrice: Long = product.defaultSellPrice,
    val costPrice: Long = product.costPrice,
    // Autofill & Destination number state
    val selectedNumber: CustomerDestinationNumberEntity? = null,
    val manualNumberValue: String = "",
    val manualNumberLabel: String = "Nomor Baru",
    val saveToCustomer: Boolean = false,
    val isManualInput: Boolean = false
) {
    val subtotal: Long get() = sellPrice * quantity.coerceAtLeast(1)
    val grossProfit: Long get() = (sellPrice - costPrice) * quantity.coerceAtLeast(1)

    val effectiveNumberValue: String
        get() = if (isManualInput || selectedNumber == null) manualNumberValue.trim() else selectedNumber.numberValue

    val effectiveLabel: String
        get() = if (isManualInput || selectedNumber == null) manualNumberLabel.trim() else selectedNumber.label

    val isValid: Boolean
        get() {
            if (product.requiredCustomerDataType == CustomerDataType.NONE) return true
            return effectiveNumberValue.isNotBlank()
        }

    fun toSaleItemEntity(transactionId: Long = 0): SaleItemEntity {
        val reqType = product.requiredCustomerDataType
        return SaleItemEntity(
            transactionId = transactionId,
            productId = product.id,
            productNameSnapshot = product.name,
            categorySnapshot = product.category,
            quantity = quantity.coerceAtLeast(1),
            costPrice = costPrice,
            sellPrice = sellPrice,
            subtotal = subtotal,
            grossProfit = grossProfit,
            requiredCustomerDataType = reqType,
            destinationLabelSnapshot = if (reqType != CustomerDataType.NONE) effectiveLabel else null,
            destinationNumberSnapshot = if (reqType != CustomerDataType.NONE) effectiveNumberValue else null
        )
    }
}

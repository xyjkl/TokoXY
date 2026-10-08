package com.example

import com.example.data.model.CustomerDataType
import com.example.data.model.CustomerDestinationNumberEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.CustomerWithNumbers
import com.example.data.model.ProductEntity
import com.example.data.model.ProductType
import com.example.data.model.SaleItemEntity
import com.example.ui.components.formatRupiah
import com.example.ui.viewmodel.CartItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TokoKuLogicTest {

    private val pulsaProduk = ProductEntity(
        id = 1,
        name = "Pulsa Telkomsel 10.000",
        category = "Pulsa",
        productType = ProductType.DIGITAL,
        costPrice = 10300,
        defaultSellPrice = 12000,
        requiredCustomerDataType = CustomerDataType.NOMOR_HP
    )

    private val tokenPlnProduk = ProductEntity(
        id = 2,
        name = "Token Listrik PLN 20.000",
        category = "Token PLN",
        productType = ProductType.DIGITAL,
        costPrice = 20500,
        defaultSellPrice = 22500,
        requiredCustomerDataType = CustomerDataType.NOMOR_METER_PLN
    )

    private val budi = CustomerWithNumbers(
        customer = CustomerEntity(id = 1, name = "Budi"),
        numbers = listOf(
            CustomerDestinationNumberEntity(
                id = 10,
                customerId = 1,
                dataType = CustomerDataType.NOMOR_HP,
                label = "HP utama",
                numberValue = "081234567890",
                isDefault = true
            ),
            CustomerDestinationNumberEntity(
                id = 11,
                customerId = 1,
                dataType = CustomerDataType.NOMOR_HP,
                label = "HP istri",
                numberValue = "085678901234",
                isDefault = false
            ),
            CustomerDestinationNumberEntity(
                id = 12,
                customerId = 1,
                dataType = CustomerDataType.NOMOR_METER_PLN,
                label = "Rumah",
                numberValue = "12345678901",
                isDefault = true
            ),
            CustomerDestinationNumberEntity(
                id = 13,
                customerId = 1,
                dataType = CustomerDataType.NOMOR_METER_PLN,
                label = "Toko",
                numberValue = "98765432109",
                isDefault = false
            )
        )
    )

    private val sitiSatuHp = CustomerWithNumbers(
        customer = CustomerEntity(id = 2, name = "Siti"),
        numbers = listOf(
            CustomerDestinationNumberEntity(
                id = 20,
                customerId = 2,
                dataType = CustomerDataType.NOMOR_HP,
                label = "Pribadi",
                numberValue = "087811223344",
                isDefault = true
            )
        )
    )

    @Test
    fun `skenario 1 - pelanggan dengan satu nomor HP membeli pulsa terisi otomatis`() {
        val matchingNumbers = sitiSatuHp.getNumbersForType(CustomerDataType.NOMOR_HP)
        assertEquals(1, matchingNumbers.size)

        val cartItem = CartItem(
            product = pulsaProduk,
            selectedNumber = matchingNumbers.first()
        )

        assertEquals("087811223344", cartItem.effectiveNumberValue)
        assertEquals("Pribadi", cartItem.effectiveLabel)
        assertTrue(cartItem.isValid)
    }

    @Test
    fun `skenario 2 - pelanggan dengan dua nomor meter membeli token PLN dapat memilih rumah atau toko`() {
        val meterNumbers = budi.getNumbersForType(CustomerDataType.NOMOR_METER_PLN)
        assertEquals(2, meterNumbers.size)

        val rumahMeter = meterNumbers.find { it.label == "Rumah" }
        val tokoMeter = meterNumbers.find { it.label == "Toko" }
        assertNotNull(rumahMeter)
        assertNotNull(tokoMeter)

        // Pilih Rumah
        val cartRumah = CartItem(product = tokenPlnProduk, selectedNumber = rumahMeter)
        assertEquals("12345678901", cartRumah.effectiveNumberValue)

        // Ganti pilih Toko
        val cartToko = CartItem(product = tokenPlnProduk, selectedNumber = tokoMeter)
        assertEquals("98765432109", cartToko.effectiveNumberValue)
    }

    @Test
    fun `skenario 3 - pelanggan memiliki nomor default terisi otomatis dan dapat diganti`() {
        val defaultHp = budi.getDefaultNumberForType(CustomerDataType.NOMOR_HP)
        assertNotNull(defaultHp)
        assertEquals("081234567890", defaultHp?.numberValue)
        assertEquals("HP utama", defaultHp?.label)

        val cartItem = CartItem(product = pulsaProduk, selectedNumber = defaultHp)
        assertEquals("081234567890", cartItem.effectiveNumberValue)

        // Ganti ke HP istri
        val hpIstri = budi.getNumbersForType(CustomerDataType.NOMOR_HP).find { it.label == "HP istri" }
        val switchedCartItem = cartItem.copy(selectedNumber = hpIstri)
        assertEquals("085678901234", switchedCartItem.effectiveNumberValue)
    }

    @Test
    fun `skenario 4 - pelanggan belum memiliki nomor yang diperlukan mendukung input manual dan simpan`() {
        // Siti belum punya nomor meter PLN
        val meterNumbers = sitiSatuHp.getNumbersForType(CustomerDataType.NOMOR_METER_PLN)
        assertTrue(meterNumbers.isEmpty())

        val manualCartItem = CartItem(
            product = tokenPlnProduk,
            selectedNumber = null,
            isManualInput = true,
            manualNumberValue = "54321678901",
            manualNumberLabel = "Rumah Baru",
            saveToCustomer = true
        )

        assertEquals("54321678901", manualCartItem.effectiveNumberValue)
        assertEquals("Rumah Baru", manualCartItem.effectiveLabel)
        assertTrue(manualCartItem.saveToCustomer)
        assertTrue(manualCartItem.isValid)
    }

    @Test
    fun `skenario 5 - satu transaksi berisi pulsa dan token PLN menggunakan jenis dan nomor yang tepat`() {
        val itemPulsa = CartItem(
            product = pulsaProduk,
            selectedNumber = budi.getDefaultNumberForType(CustomerDataType.NOMOR_HP)
        )
        val itemPln = CartItem(
            product = tokenPlnProduk,
            selectedNumber = budi.getDefaultNumberForType(CustomerDataType.NOMOR_METER_PLN)
        )

        val entityPulsa = itemPulsa.toSaleItemEntity(transactionId = 100)
        val entityPln = itemPln.toSaleItemEntity(transactionId = 100)

        assertEquals(CustomerDataType.NOMOR_HP, entityPulsa.requiredCustomerDataType)
        assertEquals("081234567890", entityPulsa.destinationNumberSnapshot)

        assertEquals(CustomerDataType.NOMOR_METER_PLN, entityPln.requiredCustomerDataType)
        assertEquals("12345678901", entityPln.destinationNumberSnapshot)
    }

    @Test
    fun `skenario 6 - produk yang sama dibeli untuk dua nomor berbeda tersimpan sebagai dua baris terpisah`() {
        val item1 = CartItem(
            product = pulsaProduk,
            selectedNumber = budi.getNumbersForType(CustomerDataType.NOMOR_HP).find { it.label == "HP utama" }
        )
        val item2 = CartItem(
            product = pulsaProduk,
            selectedNumber = budi.getNumbersForType(CustomerDataType.NOMOR_HP).find { it.label == "HP istri" }
        )

        val entity1 = item1.toSaleItemEntity(1)
        val entity2 = item2.toSaleItemEntity(1)

        assertEquals(entity1.productNameSnapshot, entity2.productNameSnapshot)
        assertEquals("081234567890", entity1.destinationNumberSnapshot)
        assertEquals("085678901234", entity2.destinationNumberSnapshot)
        assertTrue(entity1.destinationNumberSnapshot != entity2.destinationNumberSnapshot)
    }

    @Test
    fun `skenario 8 - snapshot transaksi tidak terpengaruh jika master produk atau pelanggan diubah`() {
        val originalItem = CartItem(
            product = pulsaProduk,
            sellPrice = 12000,
            costPrice = 10300,
            selectedNumber = budi.getDefaultNumberForType(CustomerDataType.NOMOR_HP)
        )
        val snapshot = originalItem.toSaleItemEntity(transactionId = 1)

        // Master produk mengalami kenaikan harga di masa depan
        val updatedMasterProduct = pulsaProduk.copy(
            costPrice = 11500,
            defaultSellPrice = 14000
        )

        // Snapshot transaksi lama tetap menyimpan harga saat transaksi dibuat
        assertEquals(12000L, snapshot.sellPrice)
        assertEquals(10300L, snapshot.costPrice)
        assertEquals(1700L, snapshot.grossProfit)
        assertEquals("081234567890", snapshot.destinationNumberSnapshot)
    }

    @Test
    fun `format Rupiah valid untuk nominal Long`() {
        assertEquals("Rp 12.000", formatRupiah(12000L))
        assertEquals("Rp 100.500", formatRupiah(100500L))
        assertEquals("Rp 0", formatRupiah(0L))
    }

    @Test
    fun `skenario 9 - pelanggan dengan beberapa nomor tanpa nomor default tidak memilih otomatis dan validitas false`() {
        // Buat data pelanggan dengan 2 nomor HP tanpa flag isDefault
        val custNoDefault = CustomerWithNumbers(
            customer = CustomerEntity(id = 50, name = "Rudi"),
            numbers = listOf(
                CustomerDestinationNumberEntity(
                    id = 501,
                    customerId = 50,
                    dataType = CustomerDataType.NOMOR_HP,
                    label = "HP 1",
                    numberValue = "0811111111",
                    isDefault = false
                ),
                CustomerDestinationNumberEntity(
                    id = 502,
                    customerId = 50,
                    dataType = CustomerDataType.NOMOR_HP,
                    label = "HP 2",
                    numberValue = "0822222222",
                    isDefault = false
                )
            )
        )

        val numbers = custNoDefault.getNumbersForType(CustomerDataType.NOMOR_HP)
        assertEquals(2, numbers.size)
        val defaultNum = numbers.find { it.isDefault }
        assertNull(defaultNum)

        // Item keranjang awal sebelum pemilihan nomor
        val unselectedCartItem = CartItem(
            product = pulsaProduk,
            selectedNumber = defaultNum, // null
            isManualInput = false
        )

        assertFalse(unselectedCartItem.isValid)
        assertEquals("", unselectedCartItem.effectiveNumberValue)

        // Setelah pengguna memilih salah satu nomor secara sadar
        val chosenCartItem = unselectedCartItem.copy(selectedNumber = numbers[1])
        assertTrue(chosenCartItem.isValid)
        assertEquals("0822222222", chosenCartItem.effectiveNumberValue)
        assertEquals("HP 2", chosenCartItem.effectiveLabel)
    }

    @Test
    fun `skenario 10 - produk tanpa nomor tujuan selalu valid tanpa nomor`() {
        val nonNumberProduct = ProductEntity(
            id = 99,
            name = "Voucher Game 100",
            category = "Game",
            productType = ProductType.DIGITAL,
            costPrice = 90000,
            defaultSellPrice = 100000,
            requiredCustomerDataType = CustomerDataType.NONE
        )

        val item = CartItem(product = nonNumberProduct)
        assertTrue(item.isValid)
        val entity = item.toSaleItemEntity(1)
        assertNull(entity.destinationNumberSnapshot)
        assertNull(entity.destinationLabelSnapshot)
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CustomerDataType
import com.example.data.model.CustomerDestinationNumberEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.CustomerWithNumbers
import com.example.data.model.TransactionWithItems
import com.example.ui.components.DataTypeBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatRupiah
import com.example.ui.components.formatTanggalWaktu
import com.example.ui.viewmodel.TokoKuViewModel

@Composable
fun CustomerScreen(
    viewModel: TokoKuViewModel,
    onSelectCustomerForSale: (CustomerWithNumbers) -> Unit,
    onSelectTransactionDetail: (TransactionWithItems) -> Unit,
    modifier: Modifier = Modifier
) {
    val allCustomers by viewModel.allCustomers.collectAsState()
    val searchQuery by viewModel.customerSearchQuery.collectAsState()

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerDetailToView by remember { mutableStateOf<CustomerWithNumbers?>(null) }

    // Dialog Tambah Nomor Baru untuk pelanggan tertentu
    var customerForNewNumber by remember { mutableStateOf<CustomerWithNumbers?>(null) }
    var numberToEdit by remember { mutableStateOf<CustomerDestinationNumberEntity?>(null) }

    val filteredCustomers = allCustomers.filter { custWithNumbers ->
        val nameMatches = custWithNumbers.customer.name.contains(searchQuery, ignoreCase = true)
        val noteMatches = custWithNumbers.customer.notes?.contains(searchQuery, ignoreCase = true) == true
        val numberMatches = custWithNumbers.numbers.any { num ->
            num.numberValue.contains(searchQuery) || num.label.contains(searchQuery, ignoreCase = true)
        }
        nameMatches || noteMatches || numberMatches
    }

    // Dialog Tambah/Edit Data Dasar Pelanggan
    if (showAddCustomerDialog || customerToEdit != null) {
        CustomerFormDialog(
            customer = customerToEdit,
            onSave = { customer ->
                viewModel.saveCustomer(customer)
                showAddCustomerDialog = false
                customerToEdit = null
            },
            onDismiss = {
                showAddCustomerDialog = false
                customerToEdit = null
            }
        )
    }

    // Dialog Tambah / Edit Nomor Tujuan
    if (customerForNewNumber != null || numberToEdit != null) {
        DestinationNumberFormDialog(
            customerId = customerForNewNumber?.customer?.id ?: numberToEdit?.customerId ?: 0L,
            number = numberToEdit,
            onSave = { entity ->
                if (entity.id == 0L) {
                    viewModel.addCustomerDestinationNumber(entity)
                } else {
                    viewModel.updateCustomerDestinationNumber(entity)
                }
                customerForNewNumber = null
                numberToEdit = null
            },
            onDismiss = {
                customerForNewNumber = null
                numberToEdit = null
            }
        )
    }

    // Dialog Detail Pelanggan (termasuk riwayat pembelian)
    customerDetailToView?.let { custWithNums ->
        val updatedCust = allCustomers.find { it.customer.id == custWithNums.customer.id } ?: custWithNums
        CustomerDetailDialog(
            customerWithNumbers = updatedCust,
            viewModel = viewModel,
            onEditCustomer = {
                customerDetailToView = null
                customerToEdit = updatedCust.customer
            },
            onAddNewNumber = {
                customerForNewNumber = updatedCust
            },
            onEditNumber = { num ->
                numberToEdit = num
            },
            onDeleteNumber = { numId ->
                viewModel.deleteCustomerDestinationNumber(numId)
            },
            onStartSale = {
                customerDetailToView = null
                onSelectCustomerForSale(updatedCust)
            },
            onSelectTransaction = onSelectTransactionDetail,
            onDismiss = { customerDetailToView = null }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    customerToEdit = null
                    showAddCustomerDialog = true
                },
                modifier = Modifier.testTag("add_customer_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Tambah Pelanggan")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Data Pelanggan",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Kelola nomor HP, nomor meter & ID PLN",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            customerToEdit = null
                            showAddCustomerDialog = true
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah")
                    }
                }
            }

            // Kolom Pencarian (Berdasarkan nama ATAU nomor yang tersimpan)
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setCustomerSearchQuery(it) },
                    placeholder = { Text("Cari nama atau nomor HP / meter PLN...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.setCustomerSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus")
                            }
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Counter
            item {
                Text(
                    text = "Total: ${filteredCustomers.size} pelanggan",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Daftar Pelanggan
            if (filteredCustomers.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Group,
                        title = "Pelanggan Tidak Ditemukan",
                        message = if (searchQuery.isNotEmpty()) {
                            "Tidak ada pelanggan dengan nama atau nomor \"$searchQuery\"."
                        } else {
                            "Belum ada pelanggan tersimpan. Tambah pelanggan baru untuk transaksi cepat."
                        },
                        buttonText = "Tambah Pelanggan Baru",
                        onButtonClick = {
                            customerToEdit = null
                            showAddCustomerDialog = true
                        }
                    )
                }
            } else {
                items(filteredCustomers, key = { it.customer.id }) { custWithNumbers ->
                    CustomerCard(
                        customerWithNumbers = custWithNumbers,
                        onClick = { customerDetailToView = custWithNumbers },
                        onQuickSale = { onSelectCustomerForSale(custWithNumbers) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
fun CustomerCard(
    customerWithNumbers: CustomerWithNumbers,
    onClick: () -> Unit,
    onQuickSale: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cust = customerWithNumbers.customer
    val numbers = customerWithNumbers.numbers

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cust.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = cust.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!cust.notes.isNullOrBlank()) {
                            Text(
                                text = cust.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Button(
                    onClick = onQuickSale,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Jual")
                }
            }

            if (numbers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))

                var isExpanded by remember { mutableStateOf(false) }
                val displayedNumbers = if (isExpanded) numbers else numbers.take(2)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayedNumbers.forEach { num ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    DataTypeBadge(dataType = num.dataType)
                                    if (num.isDefault) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                            Text(
                                                text = "Utama",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = num.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = num.numberValue,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (numbers.size > 2) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { isExpanded = !isExpanded }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isExpanded) "Sembunyikan nomor tambahan" else "+ ${numbers.size - 2} nomor lainnya (klik untuk melihat)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog Tambah / Edit Data Pelanggan
 */
@Composable
fun CustomerFormDialog(
    customer: CustomerEntity?,
    onSave: (CustomerEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val isEdit = customer != null
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Edit Pelanggan" else "Tambah Pelanggan Baru",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Pelanggan *") },
                    placeholder = { Text("Contoh: Budi Santoso") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Pelanggan (Opsional)") },
                    placeholder = { Text("Contoh: Samping pos satpam, langganan token") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val entity = CustomerEntity(
                            id = customer?.id ?: 0L,
                            name = name.trim(),
                            notes = notes.trim().ifBlank { null },
                            createdAt = customer?.createdAt ?: System.currentTimeMillis()
                        )
                        onSave(entity)
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isEdit) "Simpan" else "Tambah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

/**
 * Dialog Tambah / Edit Nomor Tujuan Pelanggan
 */
@Composable
fun DestinationNumberFormDialog(
    customerId: Long,
    number: CustomerDestinationNumberEntity?,
    onSave: (CustomerDestinationNumberEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val isEdit = number != null
    var selectedType by remember { mutableStateOf(number?.dataType ?: CustomerDataType.NOMOR_HP) }
    var label by remember { mutableStateOf(number?.label ?: "HP utama") }
    var numberValue by remember { mutableStateOf(number?.numberValue ?: "") }
    var isDefault by remember { mutableStateOf(number?.isDefault ?: false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEdit) "Edit Nomor Tujuan" else "Tambah Nomor Tujuan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Jenis Data:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        CustomerDataType.NOMOR_HP,
                        CustomerDataType.NOMOR_METER_PLN,
                        CustomerDataType.ID_PELANGGAN_PLN
                    ).forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                if (type == CustomerDataType.NOMOR_HP && label == "Rumah") label = "HP utama"
                                if (type == CustomerDataType.NOMOR_METER_PLN && label == "HP utama") label = "Rumah"
                            },
                            label = { Text(type.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = numberValue,
                    onValueChange = { numberValue = it },
                    label = { Text("Nilai Nomor *") },
                    placeholder = { Text(selectedType.inputHint) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label Nomor *") },
                    placeholder = { Text("Misal: HP utama, HP istri, Rumah, Toko") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isDefault = !isDefault }
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                    Column {
                        Text(
                            text = "Jadikan Nomor Default",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Hanya 1 nomor default untuk setiap jenis data",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (numberValue.isNotBlank() && label.isNotBlank()) {
                                val entity = CustomerDestinationNumberEntity(
                                    id = number?.id ?: 0L,
                                    customerId = customerId,
                                    dataType = selectedType,
                                    label = label.trim(),
                                    numberValue = numberValue.trim(),
                                    isDefault = isDefault
                                )
                                onSave(entity)
                            }
                        },
                        enabled = numberValue.isNotBlank() && label.isNotBlank(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

/**
 * Dialog Halaman Detail Pelanggan beserta Riwayat Pembeliannya
 */
@Composable
fun CustomerDetailDialog(
    customerWithNumbers: CustomerWithNumbers,
    viewModel: TokoKuViewModel,
    onEditCustomer: () -> Unit,
    onAddNewNumber: () -> Unit,
    onEditNumber: (CustomerDestinationNumberEntity) -> Unit,
    onDeleteNumber: (Long) -> Unit,
    onStartSale: () -> Unit,
    onSelectTransaction: (TransactionWithItems) -> Unit,
    onDismiss: () -> Unit
) {
    val cust = customerWithNumbers.customer
    val numbers = customerWithNumbers.numbers

    val customerTransactionsFlow = remember(cust.id) {
        viewModel.getTransactionsForCustomer(cust.id)
    }
    val customerTransactions by customerTransactionsFlow.collectAsState(initial = emptyList())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cust.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = cust.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (!cust.notes.isNullOrBlank()) {
                                Text(
                                    text = cust.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row {
                        IconButton(onClick = onEditCustomer) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Pelanggan")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tombol Mulai Penjualan Cepat
                Button(
                    onClick = onStartSale,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mulai Transaksi untuk ${cust.name}")
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Bagian Nomor Tujuan Tersimpan
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Nomor Tujuan Tersimpan (${numbers.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onAddNewNumber) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Tambah Nomor")
                            }
                        }
                    }

                    if (numbers.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Belum ada nomor HP / meter PLN tersimpan.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    } else {
                        items(numbers) { num ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            DataTypeBadge(dataType = num.dataType)
                                            if (num.isDefault) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = "Utama",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${num.label}: ${num.numberValue}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = { onEditNumber(num) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { onDeleteNumber(num.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bagian Riwayat Pembelian Pelanggan
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Riwayat Pembelian (${customerTransactions.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (customerTransactions.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Belum ada transaksi untuk pelanggan ini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    } else {
                        items(customerTransactions) { txWithItems ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectTransaction(txWithItems) }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = formatTanggalWaktu(txWithItems.transaction.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatRupiah(txWithItems.transaction.totalAmount),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val itemNames = txWithItems.items.joinToString(", ") { "${it.quantity}x ${it.productNameSnapshot}" }
                                    Text(
                                        text = itemNames,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

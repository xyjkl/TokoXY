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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CustomerDataType
import com.example.data.model.TransactionWithItems
import com.example.ui.components.DataTypeBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MetricSummaryCard
import com.example.ui.components.formatRupiah
import com.example.ui.components.formatTanggalWaktu
import com.example.ui.viewmodel.DateFilterType
import com.example.ui.viewmodel.TokoKuViewModel
import java.util.Calendar

@Composable
fun HistoryScreen(
    viewModel: TokoKuViewModel,
    onNavigateToSales: () -> Unit,
    printerViewModel: com.example.ui.viewmodel.PrinterViewModel? = null,
    onNavigateToPrinter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val dateFilter by viewModel.historyDateFilter.collectAsState()
    val searchQuery by viewModel.historySearchQuery.collectAsState()
    val summary by viewModel.filteredSummary.collectAsState()

    var selectedTransaction by remember { mutableStateOf<TransactionWithItems?>(null) }

    // Filter transaksi berdasarkan tanggal dan query pencarian (nama pelanggan atau nomor tujuan)
    val filteredTransactions = remember(allTransactions, dateFilter, searchQuery) {
        val now = Calendar.getInstance()
        allTransactions.filter { txWithItems ->
            val txTime = txWithItems.transaction.createdAt
            val txCal = Calendar.getInstance().apply { timeInMillis = txTime }

            // Filter Tanggal
            val dateMatches = when (dateFilter) {
                DateFilterType.HARI_INI -> {
                    now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                            now.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)
                }
                DateFilterType.KEMARIN -> {
                    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                    yesterday.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                            yesterday.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)
                }
                DateFilterType.TUJUH_HARI -> {
                    val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
                    txTime >= sevenDaysAgo.timeInMillis
                }
                DateFilterType.BULAN_INI -> {
                    now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                            now.get(Calendar.MONTH) == txCal.get(Calendar.MONTH)
                }
                DateFilterType.SEMUA -> true
            }

            // Filter Pencarian (Nama pelanggan atau Nomor tujuan yang tersimpan di snapshot item)
            val searchMatches = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim()
                txWithItems.transaction.customerNameSnapshot.contains(q, ignoreCase = true) ||
                        txWithItems.transaction.invoiceNumber.contains(q, ignoreCase = true) ||
                        txWithItems.items.any { item ->
                            item.destinationNumberSnapshot?.contains(q) == true ||
                                    item.productNameSnapshot.contains(q, ignoreCase = true)
                        }
            }

            dateMatches && searchMatches
        }
    }

    // Dialog Detail Transaksi (Snapshot View)
    selectedTransaction?.let { tx ->
        TransactionDetailDialog(
            transactionWithItems = tx,
            onDismiss = { selectedTransaction = null },
            printerViewModel = printerViewModel,
            onNavigateToPrinter = onNavigateToPrinter
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column {
                Text(
                    text = "Riwayat Penjualan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Laporan transaksi, omzet, dan estimasi laba kotor",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Kolom Pencarian
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setHistorySearchQuery(it) },
                placeholder = { Text("Cari pelanggan, nomor, atau invoice") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.setHistorySearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Hapus")
                        }
                    }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Filter Chip Bar Tanggal
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(DateFilterType.entries) { filter ->
                    val isSelected = dateFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setHistoryDateFilter(filter) },
                        label = { Text(filter.label) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Ringkasan Periode / Filter Terpilih
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Ringkasan: ${dateFilter.label}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricSummaryCard(
                            title = "Transaksi",
                            value = "${filteredTransactions.size}",
                            icon = Icons.Default.ReceiptLong,
                            iconColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        )
                        MetricSummaryCard(
                            title = "Total Omzet",
                            value = formatRupiah(filteredTransactions.sumOf { it.transaction.totalAmount }),
                            icon = Icons.Default.PointOfSale,
                            iconColor = Color(0xFF00796B),
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1.3f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Estimasi Laba Kotor Ringkasan
                    val currentProfit = filteredTransactions.sumOf { it.transaction.grossProfit }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF8E1),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Estimasi Laba Kotor",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFBF360C)
                                    )
                                }
                                Text(
                                    text = "*Belum dikurangi operasional toko",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF8D6E63)
                                )
                            }
                            Text(
                                text = formatRupiah(currentProfit),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }
        }

        // Daftar Transaksi
        if (filteredTransactions.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.Receipt,
                    title = "Tidak Ada Transaksi",
                    message = if (searchQuery.isNotEmpty()) {
                        "Tidak ditemukan transaksi dengan kata kunci \"$searchQuery\"."
                    } else {
                        "Belum ada transaksi pada periode ${dateFilter.label}."
                    },
                    buttonText = "Catat Penjualan Baru",
                    onButtonClick = onNavigateToSales
                )
            }
        } else {
            items(filteredTransactions, key = { it.transaction.id }) { txWithItems ->
                HistoryItemCard(
                    transactionWithItems = txWithItems,
                    onClick = { selectedTransaction = txWithItems }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun HistoryItemCard(
    transactionWithItems: TransactionWithItems,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tx = transactionWithItems.transaction
    val items = transactionWithItems.items

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tx.customerNameSnapshot,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = "Tercatat",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = formatRupiah(tx.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Rincian nomor tujuan & nama item
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${item.quantity}x ${item.productNameSnapshot}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        if (!item.destinationNumberSnapshot.isNullOrBlank()) {
                            Text(
                                text = "Tujuan: ${item.destinationLabelSnapshot?.let { "$it — " } ?: ""}${item.destinationNumberSnapshot}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = formatRupiah(item.subtotal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTanggalWaktu(tx.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Laba: ${formatRupiah(tx.grossProfit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE65100),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Dialog Detail Transaksi (Struk Snapshot Lengkap)
 */
@Composable
fun TransactionDetailDialog(
    transactionWithItems: TransactionWithItems,
    onDismiss: () -> Unit,
    printerViewModel: com.example.ui.viewmodel.PrinterViewModel? = null,
    onNavigateToPrinter: () -> Unit = {}
) {
    val tx = transactionWithItems.transaction
    val items = transactionWithItems.items

    val printerConfig by (printerViewModel?.printerConfig?.collectAsState() ?: remember { mutableStateOf(null) })
    val isPrinting by (printerViewModel?.isPrinting?.collectAsState() ?: remember { mutableStateOf(false) })
    val hasSelectedPrinter = !printerConfig?.selectedDeviceAddress.isNullOrBlank()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 660.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Detail Transaksi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tx.invoiceNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        // Info Pelanggan & Waktu
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pelanggan", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(tx.customerNameSnapshot, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Waktu Transaksi", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatTanggalWaktu(tx.createdAt), style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (!tx.notes.isNullOrBlank()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Catatan", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(tx.notes, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Rincian Item (Snapshot):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    items(items) { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.productNameSnapshot,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = item.categorySnapshot,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = formatRupiah(item.subtotal),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (item.requiredCustomerDataType != CustomerDataType.NONE) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        DataTypeBadge(dataType = item.requiredCustomerDataType)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${item.destinationLabelSnapshot?.let { "$it — " } ?: ""}${item.destinationNumberSnapshot ?: "-"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Harga: ${formatRupiah(item.sellPrice)} × ${item.quantity}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "Modal: ${formatRupiah(item.costPrice)} | Laba: ${formatRupiah(item.grossProfit)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Penjualan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatRupiah(tx.totalAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Modal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = formatRupiah(tx.totalCost),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Estimasi Laba Kotor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatRupiah(tx.grossProfit),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Section Cetak Ulang Struk
                if (printerViewModel != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (hasSelectedPrinter)
                                            "Printer: ${printerConfig?.selectedDeviceName ?: "MP-58N"}"
                                        else "Belum ada printer dipilih",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (!hasSelectedPrinter) {
                                    TextButton(
                                        onClick = {
                                            onDismiss()
                                            onNavigateToPrinter()
                                        }
                                    ) {
                                        Text("Atur Printer", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (hasSelectedPrinter) {
                                        printerViewModel.printTransactionReceipt(transactionWithItems)
                                    } else {
                                        onDismiss()
                                        onNavigateToPrinter()
                                    }
                                },
                                enabled = !isPrinting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reprint_receipt_history_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                if (isPrinting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mengirim ke Printer...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (hasSelectedPrinter) "Cetak Ulang Struk" else "Pilih Printer Dahulu")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

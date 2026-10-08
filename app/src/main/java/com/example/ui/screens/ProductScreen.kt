@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CustomerDataType
import com.example.data.model.ProductEntity
import com.example.data.model.ProductType
import com.example.ui.components.DataTypeBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatRupiah
import com.example.ui.viewmodel.TokoKuViewModel

@Composable
fun ProductScreen(
    viewModel: TokoKuViewModel,
    modifier: Modifier = Modifier
) {
    val allProducts by viewModel.allProducts.collectAsState()
    val searchQuery by viewModel.productSearchQuery.collectAsState()
    val categoryFilter by viewModel.productCategoryFilter.collectAsState()
    val onlyActiveFilter by viewModel.productOnlyActiveFilter.collectAsState()

    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var showProductForm by remember { mutableStateOf(false) }

    val categories = remember(allProducts) {
        listOf("Semua") + allProducts.map { it.category }.distinct()
    }

    val filteredProducts = allProducts.filter { p ->
        val matchesSearch = p.name.contains(searchQuery, ignoreCase = true) ||
                p.category.contains(searchQuery, ignoreCase = true)
        val matchesCategory = categoryFilter == null || categoryFilter == "Semua" || p.category == categoryFilter
        val matchesActive = !onlyActiveFilter || p.isActive
        matchesSearch && matchesCategory && matchesActive
    }

    // Formulir Tambah/Edit Produk (Halaman Layar Penuh)
    if (showProductForm) {
        ProductFormFullScreenDialog(
            product = productToEdit,
            onSave = { product ->
                viewModel.saveProduct(product)
                showProductForm = false
                productToEdit = null
            },
            onDismiss = {
                showProductForm = false
                productToEdit = null
            }
        )
    }

    // Menggunakan Layout tanpa FAB yang bertumpuk dengan kartu produk.
    // Tombol "+ Tambah" di header adalah aksi tambah utama yang jelas.
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Katalog
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Katalog Produk",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Kelola pulsa, data, dan token PLN",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            productToEdit = null
                            showProductForm = true
                        },
                        modifier = Modifier.testTag("add_product_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Kolom Pencarian Satu Baris
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setProductSearchQuery(it) },
                    placeholder = { Text("Cari produk atau kategori...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.setProductSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus")
                            }
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Filter Kategori Horizontal Scrollable
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = (categoryFilter == null && cat == "Semua") || categoryFilter == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.setProductCategoryFilter(if (cat == "Semua") null else cat)
                            },
                            label = { Text(cat, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Baris Info & Toggle Filter "Hanya Aktif"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Produk: ${filteredProducts.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { viewModel.setProductOnlyActiveFilter(!onlyActiveFilter) }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Filter: Hanya Aktif",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = onlyActiveFilter,
                            onCheckedChange = { viewModel.setProductOnlyActiveFilter(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // Daftar Kartu Produk
            if (filteredProducts.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Inventory2,
                        title = "Produk Tidak Ditemukan",
                        message = if (searchQuery.isNotEmpty()) {
                            "Tidak ada produk dengan kata kunci \"$searchQuery\"."
                        } else {
                            "Belum ada produk pada kategori ini."
                        },
                        buttonText = "Tambah Produk Baru",
                        onButtonClick = {
                            productToEdit = null
                            showProductForm = true
                        }
                    )
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductItemCard(
                        product = product,
                        onEdit = {
                            productToEdit = product
                            showProductForm = true
                        },
                        onToggleActive = { isActive ->
                            viewModel.toggleProductActive(product.id, isActive)
                        }
                    )
                }
            }

            // Ruang bawah yang cukup agar item terakhir tidak tertutup navigasi
            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
fun ProductItemCard(
    product: ProductEntity,
    onEdit: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val profit = product.defaultSellPrice - product.costPrice

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (product.isActive) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Baris: Nama & Kategori di kiri, Switch Status di kanan
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (product.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = product.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (product.requiredCustomerDataType != CustomerDataType.NONE) {
                            Spacer(modifier = Modifier.width(6.dp))
                            DataTypeBadge(dataType = product.requiredCustomerDataType)
                        }
                    }
                }

                // Switch Status Produk dengan Label Jelas "Aktif" / "Nonaktif"
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = if (product.isActive) "Aktif" else "Nonaktif",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (product.isActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                    )
                    Switch(
                        checked = product.isActive,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(10.dp))

            // Rincian Harga & Margin
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Harga Jual",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatRupiah(product.defaultSellPrice),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (product.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }

                Column {
                    Text(
                        text = "Harga Modal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatRupiah(product.costPrice),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Laba / Item",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatRupiah(profit),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE65100)
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Produk",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Formulir Tambah / Edit Produk Layar Penuh (Full-Screen Form)
 * Nyaman untuk ponsel, urutan isian tepat, sticky bottom action bar,
 * validasi jelas, dan keyboard-aware.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormFullScreenDialog(
    product: ProductEntity?,
    onSave: (ProductEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val isEdit = product != null

    // Isian Formulir
    var name by remember { mutableStateOf(product?.name ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "Pulsa") }
    var customCategory by remember { mutableStateOf("") }
    var isCustomCategoryMode by remember { mutableStateOf(false) }

    var costPriceStr by remember { mutableStateOf(product?.costPrice?.toString() ?: "") }
    var defaultSellPriceStr by remember { mutableStateOf(product?.defaultSellPrice?.toString() ?: "") }
    var requiredDataType by remember { mutableStateOf(product?.requiredCustomerDataType ?: CustomerDataType.NOMOR_HP) }
    var isActive by remember { mutableStateOf(product?.isActive ?: true) }
    var isSaving by remember { mutableStateOf(false) }

    val presetCategories = listOf("Pulsa", "Paket Data", "Token PLN", "Tagihan PLN", "Voucher Game", "Lainnya")

    val costPrice = costPriceStr.toLongOrNull() ?: 0L
    val sellPrice = defaultSellPriceStr.toLongOrNull() ?: 0L
    val effectiveCategory = if (isCustomCategoryMode) customCategory.trim() else category.trim()

    val nameValid = name.isNotBlank()
    val categoryValid = effectiveCategory.isNotBlank()
    val costPriceValid = costPriceStr.isNotBlank() && costPrice >= 0
    val sellPriceValid = defaultSellPriceStr.isNotBlank() && sellPrice >= 0
    val isFormValid = nameValid && categoryValid && costPriceValid && sellPriceValid

    val estimatedProfit = sellPrice - costPrice

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (isEdit) "Edit Produk" else "Tambah Produk Baru",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    // Tombol Simpan Menetap di Bawah (Sticky Bottom Bar)
                    Surface(
                        tonalElevation = 3.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Batal")
                            }

                            Button(
                                onClick = {
                                    if (isFormValid && !isSaving) {
                                        isSaving = true
                                        val entity = ProductEntity(
                                            id = product?.id ?: 0L,
                                            name = name.trim(),
                                            category = effectiveCategory,
                                            productType = ProductType.DIGITAL,
                                            costPrice = costPrice,
                                            defaultSellPrice = sellPrice,
                                            requiredCustomerDataType = requiredDataType,
                                            isActive = isActive,
                                            createdAt = product?.createdAt ?: System.currentTimeMillis()
                                        )
                                        onSave(entity)
                                    }
                                },
                                enabled = isFormValid && !isSaving,
                                modifier = Modifier
                                    .weight(1.8f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = if (isEdit) "Simpan Perubahan" else "Tambah Produk",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lengkapi detail produk digital toko Anda:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 1. NAMA PRODUK
                    item {
                        Column {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("1. Nama Produk *") },
                                placeholder = { Text("Contoh: Pulsa Telkomsel 10.000") },
                                singleLine = true,
                                isError = name.isBlank() && isSaving,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            if (name.isBlank()) {
                                Text(
                                    text = "Nama produk wajib diisi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                )
                            }
                        }
                    }

                    // 2. KATEGORI PRODUK
                    item {
                        Column {
                            Text(
                                text = "2. Kategori Produk *",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                presetCategories.forEach { cat ->
                                    val isSelected = !isCustomCategoryMode && category == cat
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            isCustomCategoryMode = false
                                            category = cat
                                            // Otomatis sarankan tipe data tujuan yang sesuai
                                            when (cat) {
                                                "Pulsa", "Paket Data" -> requiredDataType = CustomerDataType.NOMOR_HP
                                                "Token PLN" -> requiredDataType = CustomerDataType.NOMOR_METER_PLN
                                                "Tagihan PLN" -> requiredDataType = CustomerDataType.ID_PELANGGAN_PLN
                                                "Voucher Game" -> requiredDataType = CustomerDataType.NONE
                                            }
                                        },
                                        label = { Text(cat) }
                                    )
                                }
                                FilterChip(
                                    selected = isCustomCategoryMode,
                                    onClick = { isCustomCategoryMode = true },
                                    label = { Text("Kategori Baru...") }
                                )
                            }

                            if (isCustomCategoryMode) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = customCategory,
                                    onValueChange = { customCategory = it },
                                    label = { Text("Ketik Nama Kategori Baru") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // 3 & 4. HARGA MODAL & HARGA JUAL
                    item {
                        Column {
                            Text(
                                text = "3 & 4. Penetapan Harga (Rupiah) *",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = costPriceStr,
                                    onValueChange = { costPriceStr = it.filter { c -> c.isDigit() } },
                                    label = { Text("Harga Modal (Rp) *") },
                                    placeholder = { Text("10300") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = defaultSellPriceStr,
                                    onValueChange = { defaultSellPriceStr = it.filter { c -> c.isDigit() } },
                                    label = { Text("Harga Jual (Rp) *") },
                                    placeholder = { Text("12000") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // Ringkasan Margin Per Item setelah harga terisi
                            Spacer(modifier = Modifier.height(10.dp))
                            if (costPriceStr.isNotBlank() && defaultSellPriceStr.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (estimatedProfit >= 0) Color(0xFFFFF8E1) else Color(0xFFFFEBEE),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Ringkasan Laba Kotor Per Item:",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (estimatedProfit >= 0) Color(0xFFBF360C) else MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = formatRupiah(estimatedProfit),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (estimatedProfit >= 0) Color(0xFFE65100) else MaterialTheme.colorScheme.error
                                            )
                                        }
                                        if (estimatedProfit < 0) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Perhatian: Harga jual lebih rendah dari harga modal!",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. JENIS DATA PELANGGAN YANG DIPERLUKAN
                    item {
                        Column {
                            Text(
                                text = "5. Jenis Data Pelanggan yang Diperlukan *",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Digunakan untuk autofill nomor tujuan transaksi secara otomatis",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                CustomerDataType.entries.forEach { type ->
                                    val isSelected = requiredDataType == type
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { requiredDataType = type }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Inventory2,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = type.label,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (type == CustomerDataType.NONE) {
                                                    Text(
                                                        text = "Untuk produk voucher atau transaksi tanpa nomor tujuan",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. STATUS AKTIF
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "6. Status Produk: ${if (isActive) "Aktif" else "Nonaktif"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "Produk aktif langsung tampil pada pilihan transaksi",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isActive,
                                    onCheckedChange = { isActive = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

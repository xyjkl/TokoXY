package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.TokoKuDatabase
import com.example.data.model.TransactionWithItems
import com.example.data.repository.TokoKuRepository
import com.example.ui.screens.CustomerScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProductScreen
import com.example.ui.screens.SalesScreen
import com.example.ui.screens.TransactionDetailDialog
import com.example.ui.theme.TokoKuTheme
import com.example.ui.viewmodel.AppNavTab
import com.example.ui.viewmodel.TokoKuViewModel
import com.example.ui.viewmodel.TokoKuViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TokoKuTheme {
                TokoKuApp()
            }
        }
    }
}

@Composable
fun TokoKuApp() {
    val context = LocalContext.current
    val database = remember { TokoKuDatabase.getDatabase(context) }
    val repository = remember {
        TokoKuRepository(
            productDao = database.productDao(),
            customerDao = database.customerDao(),
            saleTransactionDao = database.saleTransactionDao()
        )
    }
    val factory = remember { TokoKuViewModelFactory(repository) }
    val viewModel: TokoKuViewModel = viewModel(factory = factory)

    val currentTab by viewModel.currentTab.collectAsState()

    // Dialog Detail Transaksi global (bisa dibuka dari Beranda, Riwayat, atau Detail Pelanggan)
    var selectedTransactionDetail by remember { mutableStateOf<TransactionWithItems?>(null) }

    selectedTransactionDetail?.let { txWithItems ->
        TransactionDetailDialog(
            transactionWithItems = txWithItems,
            onDismiss = { selectedTransactionDetail = null }
        )
    }

    // Tangani tombol Back jika sedang berada di tab sekunder agar kembali ke Beranda
    BackHandler(enabled = currentTab != AppNavTab.HOME) {
        viewModel.navigateTo(AppNavTab.HOME)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                // Tab Beranda
                NavigationBarItem(
                    selected = currentTab == AppNavTab.HOME,
                    onClick = { viewModel.navigateTo(AppNavTab.HOME) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppNavTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Beranda"
                        )
                    },
                    label = { Text("Beranda") }
                )

                // Tab Penjualan
                NavigationBarItem(
                    selected = currentTab == AppNavTab.SALES,
                    onClick = { viewModel.navigateTo(AppNavTab.SALES) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppNavTab.SALES) Icons.Filled.PointOfSale else Icons.Outlined.PointOfSale,
                            contentDescription = "Penjualan"
                        )
                    },
                    label = { Text("Penjualan") }
                )

                // Tab Riwayat
                NavigationBarItem(
                    selected = currentTab == AppNavTab.HISTORY,
                    onClick = { viewModel.navigateTo(AppNavTab.HISTORY) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppNavTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "Riwayat"
                        )
                    },
                    label = { Text("Riwayat") }
                )

                // Tab Produk
                NavigationBarItem(
                    selected = currentTab == AppNavTab.PRODUCTS,
                    onClick = { viewModel.navigateTo(AppNavTab.PRODUCTS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppNavTab.PRODUCTS) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                            contentDescription = "Produk"
                        )
                    },
                    label = { Text("Produk") }
                )

                // Tab Pelanggan
                NavigationBarItem(
                    selected = currentTab == AppNavTab.CUSTOMERS,
                    onClick = { viewModel.navigateTo(AppNavTab.CUSTOMERS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppNavTab.CUSTOMERS) Icons.Filled.Group else Icons.Outlined.Group,
                            contentDescription = "Pelanggan"
                        )
                    },
                    label = { Text("Pelanggan") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { tab ->
                when (tab) {
                    AppNavTab.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSales = { viewModel.navigateTo(AppNavTab.SALES) },
                        onNavigateToHistory = { viewModel.navigateTo(AppNavTab.HISTORY) },
                        onNavigateToProducts = { viewModel.navigateTo(AppNavTab.PRODUCTS) },
                        onNavigateToCustomers = { viewModel.navigateTo(AppNavTab.CUSTOMERS) },
                        onSelectTransaction = { tx -> selectedTransactionDetail = tx }
                    )

                    AppNavTab.SALES -> SalesScreen(
                        viewModel = viewModel,
                        onNavigateToHistory = { viewModel.navigateTo(AppNavTab.HISTORY) }
                    )

                    AppNavTab.HISTORY -> HistoryScreen(
                        viewModel = viewModel,
                        onNavigateToSales = { viewModel.navigateTo(AppNavTab.SALES) }
                    )

                    AppNavTab.PRODUCTS -> ProductScreen(
                        viewModel = viewModel
                    )

                    AppNavTab.CUSTOMERS -> CustomerScreen(
                        viewModel = viewModel,
                        onSelectCustomerForSale = { customer ->
                            viewModel.selectCustomer(customer)
                            viewModel.navigateTo(AppNavTab.SALES)
                        },
                        onSelectTransactionDetail = { tx -> selectedTransactionDetail = tx }
                    )
                }
            }
        }
    }
}

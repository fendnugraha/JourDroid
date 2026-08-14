package com.example.jourdroid.ui.app.transaction

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.data.JournalData
import com.example.jourdroid.data.UserData
import com.example.jourdroid.ui.component.PrintJournalReceiptDialog
import com.example.jourdroid.ui.component.SlideUpModal
import com.example.jourdroid.utils.DateUtils
import com.example.jourdroid.utils.FormatterUtils.formatNumberWithDots
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    user: UserData
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var journals by remember { mutableStateOf<List<JournalData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedJournalForPrint by remember { mutableStateOf<JournalData?>(null) }
    var autoShowPrinterSelection by remember { mutableStateOf(false) }
    var isModalOpen by remember { mutableStateOf(false) }

    // Filter states
    var searchTerm by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("all") }
    var accountFilter by remember { mutableStateOf("all") }
    var showAccountFilterDialog by remember { mutableStateOf(false) }

    val userWarehouseId = user.warehouseId ?: 0
    val userName = user.name ?: "Admin"
    val userWarehouseName = user.warehouse?.name ?: "Tanpa Gudang"

    // Refresh function
    val refreshJournals = suspend {
        try {
            isLoading = true
            errorMessage = null
            val apiService = ApiClient.getApiService(context)

            val today = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DateUtils.getTodayJakartaFormat()
            } else {
                "2026-07-09"
            }

            val response = apiService.getJournalByWarehouse(
                warehouse = userWarehouseId,
                startDate = today,
                endDate = today
            )
            if (response.success) {
                journals = response.data
            } else {
                errorMessage = response.message
            }
        } catch (e: Exception) {
            errorMessage = "Gagal memuat data: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshJournals()
    }

    // Bluetooth Permissions
    var hasBluetoothPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.all { it }
        hasBluetoothPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Izin Bluetooth diperlukan untuk cetak struk.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasBluetoothPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
            )
        }
    }

    // ─── FILTER LOGIC (Matching JS useMemo structure) ───
    val filteredTransactions = remember(journals, accountFilter, categoryFilter, searchTerm) {
        if (journals.isEmpty()) {
            emptyList()
        } else {
            journals.filter { journal ->
                val credId = journal.effectiveCredId
                val debtId = journal.effectiveDebtId

                val accountFilterNum = accountFilter.toIntOrNull()
                val matchAccount = accountFilter != "all" && accountFilterNum != null &&
                        (credId == accountFilterNum || debtId == accountFilterNum)

                val matchCategory = categoryFilter != "all" && journal.trxType == categoryFilter

                val query = searchTerm.trim().lowercase()
                val matchSearchTerm = if (query.isEmpty()) {
                    false
                } else {
                    val debtName = (journal.debt?.accName ?: "").lowercase()
                    val credName = (journal.cred?.accName ?: "").lowercase()
                    val description = (journal.description ?: "").lowercase()
                    val idStr = journal.id.toString().lowercase()
                    val invoice = (journal.invoice ?: "").lowercase()
                    val amountStr = journal.amount.toString().lowercase()
                    val transactionProductMatches = (journal.transaction ?: emptyList()).any { t ->
                        (t.product?.name ?: "").lowercase().contains(query)
                    }

                    debtName.contains(query) || credName.contains(query) ||
                            description.contains(query) || idStr.contains(query) ||
                            invoice.contains(query) || amountStr.contains(query) ||
                            transactionProductMatches
                }

                if (accountFilter != "all" && categoryFilter != "all" && query.isNotEmpty()) {
                    matchAccount && matchCategory && matchSearchTerm
                } else if (categoryFilter != "all") {
                    matchCategory
                } else if (accountFilter != "all") {
                    matchAccount
                } else if (query.isNotEmpty()) {
                    matchSearchTerm
                } else {
                    true
                }
            }
        }
    }

    // Distinct categories for filter chips
    val availableCategories = remember(journals) {
        val categories = journals.mapNotNull { it.trxType }.filter { it.isNotBlank() }.distinct().toMutableList()
        categories
    }

    val totalAmount = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.amount.toLong() }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.surface
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                "Finance Activity", 
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = userWarehouseName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { isModalOpen = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Jurnal")
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { scope.launch { refreshJournals() } },
                modifier = Modifier.padding(innerPadding)
            ) {
                val scrollState = rememberScrollState()
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // ─── FINANCE APP VIBE SUMMARY CARD ───
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.SwapHoriz,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Total Transactions",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "${filteredTransactions.size} items",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Rp " + formatNumberWithDots(totalAmount),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Filtered journal volume today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ─── SEARCH & FILTER SECTION ───
                    OutlinedTextField(
                        value = searchTerm,
                        onValueChange = { searchTerm = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by description, account, invoice...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (searchTerm.isNotEmpty()) {
                                IconButton(onClick = { searchTerm = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Filter Chips & Account Filter Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // All Chip
                        FilterChip(
                            selected = categoryFilter == "all",
                            onClick = { categoryFilter = "all" },
                            label = { Text("All Categories") },
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Dynamic Category Chips
                        availableCategories.forEach { category ->
                            FilterChip(
                                selected = categoryFilter == category,
                                onClick = {
                                    categoryFilter = if (categoryFilter == category) "all" else category
                                },
                                label = { Text(category) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Account Filter Button
                        FilterChip(
                            selected = accountFilter != "all",
                            onClick = { showAccountFilterDialog = true },
                            label = {
                                Text(if (accountFilter == "all") "Filter Account" else "Account: #$accountFilter")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Account filter",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                if (accountFilter != "all") {
                                    IconButton(
                                        onClick = { accountFilter = "all" },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Reset Account Filter")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ─── JOURNAL LIST SECTION HEADER ───
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Transactions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )

                        if (searchTerm.isNotEmpty() || categoryFilter != "all" || accountFilter != "all") {
                            TextButton(
                                onClick = {
                                    searchTerm = ""
                                    categoryFilter = "all"
                                    accountFilter = "all"
                                }
                            ) {
                                Text("Reset Filters", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 800.dp)
                    ) {
                        when {
                            isLoading && journals.isEmpty() -> {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                            errorMessage != null -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                                    Button(onClick = { scope.launch { refreshJournals() } }, modifier = Modifier.padding(top = 8.dp)) {
                                        Text("Try Again")
                                    }
                                }
                            }
                            filteredTransactions.isEmpty() -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (journals.isEmpty()) "No transactions today" else "No transactions match your filter",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            else -> {
                                JournalTable(
                                    journals = filteredTransactions,
                                    onJournalClick = { selectedJournalForPrint = it }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp)) // Extra space for FAB
                }
            }
        }
    }

    // Account filter dialog
    if (showAccountFilterDialog) {
        var tempAccountInput by remember { mutableStateOf(if (accountFilter == "all") "" else accountFilter) }
        AlertDialog(
            onDismissRequest = { showAccountFilterDialog = false },
            title = { Text("Filter by Account ID") },
            text = {
                Column {
                    Text(
                        "Enter account ID to filter credit or debit accounts:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempAccountInput,
                        onValueChange = { tempAccountInput = it },
                        label = { Text("Account ID") },
                        placeholder = { Text("e.g. 101") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        accountFilter = if (tempAccountInput.isBlank()) "all" else tempAccountInput.trim()
                        showAccountFilterDialog = false
                    }
                ) {
                    Text("Apply Filter")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        accountFilter = "all"
                        showAccountFilterDialog = false
                    }
                ) {
                    Text("Clear Filter")
                }
            }
        )
    }

    if (selectedJournalForPrint != null) {
        PrintJournalReceiptDialog(
            journal = selectedJournalForPrint!!,
            agentName = userName,
            warehouseName = userWarehouseName,
            startWithPrinterSelection = autoShowPrinterSelection,
            onDismiss = { 
                selectedJournalForPrint = null
                autoShowPrinterSelection = false
            }
        )
    }

    SlideUpModal(
        visible = isModalOpen,
        title = "Tambah Transaksi Jurnal",
        onClose = { isModalOpen = false }
    ) {
        CreateMutationFromHq(
            onSuccess = { journal ->
                isModalOpen = false
                scope.launch { refreshJournals() }
                if (journal != null) {
                    scope.launch {
                        kotlinx.coroutines.delay(300)
                        selectedJournalForPrint = journal
                        autoShowPrinterSelection = true
                    }
                }
            }
        )
    }
}

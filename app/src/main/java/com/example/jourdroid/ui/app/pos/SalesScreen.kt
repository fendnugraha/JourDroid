package com.example.jourdroid.ui.app.pos

import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.data.SalesData
import com.example.jourdroid.data.SalesSummaryItem
import com.example.jourdroid.data.UserData
import com.example.jourdroid.ui.component.PrintPosReceiptDialog
import com.example.jourdroid.utils.DateUtils
import com.example.jourdroid.utils.FormatterUtils.formatRupiah
import com.example.jourdroid.utils.FormatterUtils.formatShortDate
import com.example.jourdroid.utils.FormatterUtils.formatTimeOnly
import com.example.jourdroid.utils.ShareUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    user: UserData
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var txJournals by remember { mutableStateOf(emptyList<SalesData>()) }
    var summaryItems by remember { mutableStateOf(emptyList<SalesSummaryItem>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPosOpen by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedSaleForPrint by remember { mutableStateOf<SalesData?>(null) }
    
    var inventoryView by remember { mutableStateOf<ComposeView?>(null) }

    val userWarehouseId = user.warehouseId ?: 1 // 🟢 Changed default to 1 to match PosScreen
    val userWarehouseName = user.warehouse?.name ?: "Tanpa Gudang"

    val refreshTxJournals = suspend {
        try {
            isLoading = true
            errorMessage = null
            val apiService = ApiClient.getApiService(context)

            val today = DateUtils.getTodayJakartaFormat()
            android.util.Log.d("SalesScreen", "Fetching TX for Warehouse: $userWarehouseId on $today")

            val response = apiService.getTxByWarehouse(
                warehouse = userWarehouseId,
                startDate = today,
                endDate = today
            )
            if (response.success) {
                val apiList = response.data?.list ?: emptyList()
                summaryItems = response.data?.summary ?: emptyList()
                
                if (apiList.isNotEmpty()) {
                    // Group by invoice to show transaction-level cards
                    txJournals = apiList.groupBy { it.invoice }.map { (invoice, items) ->
                        val first = items.first()
                        val totalAmount = items.sumOf { 
                            val p = it.price?.toDoubleOrNull() ?: 0.0
                            val q = if (it.quantity < 0) -it.quantity else it.quantity
                            p * q.toDouble()
                        }.toLong()

                        SalesData(
                            id = first.id,
                            invoice = invoice,
                            dateIssued = first.dateIssued,
                            amount = totalAmount,
                            status = first.status,
                            items = items.map { item ->
                                com.example.jourdroid.data.SalesItem(
                                    id = item.id,
                                    productName = item.product?.name ?: "Unknown",
                                    quantity = if (item.quantity < 0) -item.quantity else item.quantity,
                                    price = item.price?.toDoubleOrNull()?.toLong() ?: 0L,
                                    subtotal = (item.price?.toDoubleOrNull() ?: 0.0).toLong() * 
                                            (if (item.quantity < 0) -item.quantity else item.quantity)
                                )
                            }
                        )
                    }.sortedByDescending { it.id }
                } else {
                    txJournals = response.transactions ?: emptyList()
                }
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
        refreshTxJournals()
    }

    if (isPosOpen) {
        PosScreen(
            user = user,
            onBack = { 
                isPosOpen = false
                scope.launch { refreshTxJournals() }
            },
            onSuccess = {
                scope.launch { refreshTxJournals() }
            }
        )
    } else {
        val gradient = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                MaterialTheme.colorScheme.surface
            )
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Sales Activity", fontWeight = FontWeight.Bold)
                            Text(
                                text = userWarehouseName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        if (selectedTab == 1 && summaryItems.isNotEmpty()) {
                            IconButton(onClick = {
                                try {
                                    val view = inventoryView // 🟢 Use the compact inventory view for sharing
                                    if (view != null && view.width > 0 && view.height > 0) {
                                        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                                        val canvas = android.graphics.Canvas(bitmap)
                                        canvas.drawColor(android.graphics.Color.WHITE)
                                        view.draw(canvas)
                                        
                                        val uri = ShareUtils.saveBitmapToCache(context, bitmap)
                                        if (uri != null) {
                                            ShareUtils.shareImage(context, uri)
                                        }
                                    } else {
                                        Toast.makeText(context, "Gagal menangkap laporan inventory", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share Inventory Report")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { isPosOpen = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add POS Transaction")
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(gradient)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Activity") },
                        icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Summary") },
                        icon = { Icon(Icons.Default.BarChart, null) }
                    )
                }

                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { scope.launch { refreshTxJournals() } },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (errorMessage != null && txJournals.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { scope.launch { refreshTxJournals() } }) {
                                    Text("Coba Lagi")
                                }
                            }
                        }
                    } else if (txJournals.isEmpty() && summaryItems.isEmpty() && !isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text("Belum ada transaksi hari ini", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    } else {
                        if (selectedTab == 0) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(txJournals) { sale ->
                                    SalesRow(
                                        sale = sale,
                                        onPrintClick = { selectedSaleForPrint = sale }
                                    )
                                }
                            }
                        } else {
                            // Summary Tab with Shareable Ticket
                            Box(modifier = Modifier.fillMaxSize()) {
                                // 1. Visible Detailed Summary (for User)
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    SalesSummaryTicket(
                                        warehouseName = userWarehouseName,
                                        summaryItems = summaryItems,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                // 2. Hidden Compact Inventory Report (for Sharing)
                                // We use a tiny size but wrapContentHeight to ensure all items are rendered for capture
                                AndroidView(
                                    factory = { ctx ->
                                        ComposeView(ctx).apply {
                                            setContent {
                                                InventoryReportTicket(
                                                    warehouseName = userWarehouseName,
                                                    summaryItems = summaryItems
                                                )
                                            }
                                        }
                                    },
                                    update = { inventoryView = it },
                                    modifier = Modifier
                                        .width(360.dp)
                                        .wrapContentHeight()
                                        .alpha(0f) // Keep it invisible but active
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedSaleForPrint != null) {
        PrintPosReceiptDialog(
            salesData = selectedSaleForPrint!!,
            agentName = user.name ?: "Admin",
            warehouseName = userWarehouseName,
            onDismiss = { selectedSaleForPrint = null }
        )
    }
}

@Composable
fun InventoryReportTicket(
    warehouseName: String,
    summaryItems: List<SalesSummaryItem>
) {
    val today = remember { DateUtils.getTodayJakartaFormat() }
    
    Column(
        modifier = Modifier
            .width(360.dp)
            .background(Color.White)
            .padding(24.dp)
    ) {
        Text(
            text = "INVENTORY SALES REPORT",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )
        Text(
            text = warehouseName,
            style = MaterialTheme.typography.bodySmall,
            color = Color.DarkGray
        )
        Text(
            text = "Date: $today",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Compact Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3F4F6))
                .padding(8.dp)
        ) {
            Text("Product Name", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text("Qty Sold", Modifier.width(60.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End)
        }
        
        // Compact Table Rows
        summaryItems.forEach { item ->
            val qty = item.quantity?.toDoubleOrNull() ?: 0.0
            val absQty = if (qty < 0) -qty else qty
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.product?.name ?: "Unknown",
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    color = Color.Black
                )
                Text(
                    text = absQty.toInt().toString(),
                    modifier = Modifier.width(60.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    color = Color.Black
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Generated by JourDroid Inventory System",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun SalesSummaryTicket(
    warehouseName: String,
    summaryItems: List<SalesSummaryItem>,
    modifier: Modifier = Modifier
) {
    val today = remember { DateUtils.getTodayJakartaFormat() }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "DAILY SALES SUMMARY",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = warehouseName,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = today,
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SummaryTableHeader()
        
        Spacer(modifier = Modifier.height(8.dp))
        
        summaryItems.forEach { item ->
            SummaryTableRow(item = item)
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val grandTotal = summaryItems.sumOf { (it.totalPrice?.toDoubleOrNull() ?: 0.0).toLong() }
        val grandTotalAbs = if (grandTotal < 0) -grandTotal else grandTotal
        
        val totalCost = summaryItems.sumOf { (it.totalCost?.toDoubleOrNull() ?: 0.0).toLong() }
        val totalCostAbs = if (totalCost < 0) -totalCost else totalCost
        val grandProfit = grandTotalAbs - totalCostAbs
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL REVENUE", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text(formatRupiah(grandTotalAbs), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL COST", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text(formatRupiah(totalCostAbs), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GRAND PROFIT", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    Text(
                        formatRupiah(grandProfit),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "JourDroid - Realtime Business Insight",
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SummaryTableHeader() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Product", Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            Text("Qty", Modifier.weight(0.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
            Text("Total Sales", Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
        }
    }
}

@Composable
fun SummaryTableRow(item: SalesSummaryItem) {
    val qty = item.quantity?.toDoubleOrNull() ?: 0.0
    val absQty = if (qty < 0) -qty else qty
    val totalPrice = item.totalPrice?.toDoubleOrNull() ?: 0.0
    val absTotalPrice = if (totalPrice < 0) -totalPrice else totalPrice
    
    val totalCost = item.totalCost?.toDoubleOrNull() ?: 0.0
    val absTotalCost = if (totalCost < 0) -totalCost else totalCost
    val profit = absTotalPrice - absTotalCost

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1.5f)) {
                Text(
                    text = item.product?.name ?: "Unknown Product",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.product?.category ?: "-"} • Cost: ${formatRupiah(absTotalCost.toLong())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = absQty.toInt().toString(),
                Modifier.weight(0.5f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    text = formatRupiah(absTotalPrice.toLong()),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "+${formatRupiah(profit.toLong())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SalesRow(
    sale: SalesData,
    onPrintClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sale.invoice ?: "INV-${sale.id}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${formatShortDate(sale.dateIssued)} • ${formatTimeOnly(sale.dateIssued)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatRupiah(sale.amount),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onPrintClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Print,
                            contentDescription = "Print Receipt",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Surface(
                    color = if (sale.status == 1) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (sale.status == 1) "Success" else "Pending",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (sale.status == 1) Color(0xFF2E7D32) else Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

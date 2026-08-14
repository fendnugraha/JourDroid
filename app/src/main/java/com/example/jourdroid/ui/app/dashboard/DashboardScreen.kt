package com.example.jourdroid.ui.app.dashboard

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.data.CashBankBalanceItem
import com.example.jourdroid.data.UserData
import com.example.jourdroid.utils.DateUtils
import com.example.jourdroid.utils.FormatterUtils.formatRupiah
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: UserData
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var balanceData by remember { mutableStateOf<CashBankBalanceItem?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val userWarehouseId = user.warehouseId ?: 0

    val refreshData = suspend {
        try {
            isLoading = true
            errorMessage = null
            val apiService = ApiClient.getApiService(context)

            val today = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DateUtils.getTodayJakartaFormat()
            } else {
                "2026-07-09"
            }

            val response = apiService.getCashBankBalance(
                warehouse = userWarehouseId,
                endDate = today
            )
            if (response.success) {
                balanceData = response.data
            } else {
                errorMessage = response.message
            }
        } catch (e: Exception) {
            errorMessage = "Gagal memuat data: ${e.localizedMessage}"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
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
                        Text(
                            "Dashboard", 
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                        ) 
                    },
                    actions = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { scope.launch { refreshData() } },
                modifier = Modifier.padding(innerPadding)
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        isLoading && balanceData == null -> {
                            CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                        }
                        errorMessage != null -> {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                                Button(onClick = { scope.launch { refreshData() } }, modifier = Modifier.padding(top = 8.dp)) {
                                    Text("Coba Lagi")
                                }
                            }
                        }
                        balanceData == null || balanceData?.chartOfAccounts?.isEmpty() == true -> {
                            Text(
                                text = "Tidak ada data saldo",
                                modifier = Modifier.padding(32.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        else -> {
                            // Summary Cards
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SummaryCard(
                                    title = "Total Cash",
                                    amount = balanceData?.sumtotalCash ?: 0L,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                SummaryCard(
                                    title = "Total Bank",
                                    amount = balanceData?.sumtotalBank ?: 0L,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Text(
                                text = "Cash & Bank Details",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
                            )
                            
                            CashBankBalance(
                                accounts = balanceData!!.chartOfAccounts,
                                modifier = Modifier.heightIn(max = 1000.dp) // Large enough to show all
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Dashboard Page",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: Long,
    modifier: Modifier = Modifier,
    color: Color
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatRupiah(amount),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 14.sp
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

package com.example.jourdroid.ui.app.delivery

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
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
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.data.DeliveryItem
import com.example.jourdroid.data.UserData
import com.example.jourdroid.ui.component.PrintDeliveryReceiptDialog
import com.example.jourdroid.utils.FormatterUtils.formatRupiah
import com.example.jourdroid.utils.FormatterUtils.formatShortDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryScreen(
    user: UserData
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var deliveries by remember { mutableStateOf<List<DeliveryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var selectedDeliveryForPrint by remember { mutableStateOf<DeliveryItem?>(null) }

    val refreshDeliveries = suspend {
        try {
            isLoading = true
            errorMessage = null
            val apiService = ApiClient.getApiService(context)
            val response = apiService.getDeliveries()
            
            if (response.success) {
                // Show all deliveries for printing history
                deliveries = response.deliveries.sortedByDescending { it.createdAt }
            } else {
                errorMessage = response.message
            }
        } catch (e: Exception) {
            errorMessage = "Gagal memuat data delivery: ${e.localizedMessage}"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshDeliveries()
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
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
                            "Data Pengiriman", 
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                        ) 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { scope.launch { refreshDeliveries() } },
                modifier = Modifier.padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    if (isLoading && deliveries.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (errorMessage != null && deliveries.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                                Button(onClick = { scope.launch { refreshDeliveries() } }) {
                                    Text("Retry")
                                }
                            }
                        }
                    } else if (deliveries.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Belum ada riwayat pengiriman.", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(deliveries) { item ->
                                DeliveryDataCard(
                                    item = item,
                                    onPrintClick = { selectedDeliveryForPrint = item }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (selectedDeliveryForPrint != null) {
        PrintDeliveryReceiptDialog(
            delivery = selectedDeliveryForPrint!!,
            onDismiss = { selectedDeliveryForPrint = null }
        )
    }
}

@Composable
fun DeliveryDataCard(
    item: DeliveryItem,
    onPrintClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.invoice ?: "INV-####",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = formatShortDate(item.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Surface(
                    color = when(item.status.lowercase()) {
                        "delivered" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        "pending" -> Color(0xFFFFA000).copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = when(item.status.lowercase()) {
                                "delivered" -> Color(0xFF4CAF50)
                                "pending" -> Color(0xFFFFA000)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dari", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.sourceAccount?.warehouse?.name ?: "-", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(horizontal = 8.dp).size(16.dp))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Ke", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.destinationAccount?.warehouse?.name ?: "-", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatRupiah(item.amount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    )
                }
                
                OutlinedButton(
                    onClick = onPrintClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Print Struk")
                }
            }
        }
    }
}

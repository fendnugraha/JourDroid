package com.example.jourdroid.ui.app.delivery

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.data.DeliveryItem
import com.example.jourdroid.data.UserData
import com.example.jourdroid.ui.component.NotificationBadge
import com.example.jourdroid.ui.component.PrintDeliveryReceiptDialog
import com.example.jourdroid.ui.component.ProfileAvatar
import com.example.jourdroid.utils.FormatterUtils.formatRupiah
import com.example.jourdroid.utils.FormatterUtils.formatShortDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryScreen(
    user: UserData,
    unreadNotificationCount: Int = 0,
    onNavigateToNotifications: () -> Unit = {}
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
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
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
                            "Pengiriman", 
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                        ) 
                    },
                    navigationIcon = {
                        ProfileAvatar(user = user)
                    },
                    actions = {
                        NotificationBadge(
                            unreadCount = unreadNotificationCount,
                            onClick = onNavigateToNotifications
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
                        .padding(horizontal = 20.dp)
                ) {
                    if (isLoading && deliveries.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 3.dp)
                        }
                    } else if (errorMessage != null && deliveries.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Info, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.height(16.dp))
                                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                                TextButton(onClick = { scope.launch { refreshDeliveries() } }) {
                                    Text("Retry")
                                }
                            }
                        }
                    } else if (deliveries.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                Spacer(Modifier.height(16.dp))
                                Text(text = "Belum ada riwayat pengiriman.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.invoice ?: "INV-####",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                    Text(
                        text = formatShortDate(item.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Surface(
                    color = when(item.status.lowercase()) {
                        "delivered" -> Color(0xFF10B981).copy(alpha = 0.1f)
                        "pending" -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = item.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = when(item.status.lowercase()) {
                                "delivered" -> Color(0xFF10B981)
                                "pending" -> Color(0xFFF59E0B)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("DARI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(item.sourceAccount?.warehouse?.name ?: "-", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("KE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(item.destinationAccount?.warehouse?.name ?: "-", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("TOTAL NILAI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatRupiah(item.amount),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    )
                }
                
                Button(
                    onClick = onPrintClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PRINT", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

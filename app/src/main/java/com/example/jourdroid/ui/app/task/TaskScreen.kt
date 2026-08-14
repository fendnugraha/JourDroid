package com.example.jourdroid.ui.app.task

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.data.DeliveryItem
import com.example.jourdroid.data.UserData
import com.example.jourdroid.utils.FormatterUtils.formatRupiah
import com.example.jourdroid.utils.LocationHelper
import com.example.jourdroid.utils.LocationUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    user: UserData
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var deliveries by remember { mutableStateOf<List<DeliveryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Dialog for confirmation
    var showCompleteConfirm by remember { mutableStateOf<DeliveryItem?>(null) }
    
    // User current location for ETA
    var currentUserLocation by remember { mutableStateOf<android.location.Location?>(null) }

    val refreshDeliveries = suspend {
        try {
            isLoading = true
            errorMessage = null
            
            // Try to update location for ETA
            currentUserLocation = LocationHelper.getCurrentLocation(context)
            
            val apiService = ApiClient.getApiService(context)
            val response = apiService.getDeliveries()
            
            if (response.success) {
                deliveries = response.deliveries
            } else {
                errorMessage = response.message
            }
        } catch (e: Exception) {
            errorMessage = "Gagal memuat data tugas: ${e.localizedMessage}"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            scope.launch { refreshDeliveries() }
        } else {
            Toast.makeText(context, "Izin lokasi diperlukan untuk tracking tugas", Toast.LENGTH_LONG).show()
        }
    }

    val updateDeliveryStatus = { item: DeliveryItem, action: String ->
        scope.launch {
            try {
                isLoading = true
                
                // Fetch current GPS before action
                val location = LocationHelper.getCurrentLocation(context)
                val lat = location?.latitude
                val lng = location?.longitude
                
                android.util.Log.d("TASK_ACTION", "Sending Location: $lat, $lng for action: $action")
                
                val apiService = ApiClient.getApiService(context)
                val response = when (action) {
                    "process" -> apiService.processDelivery(item.id, lat, lng)
                    "complete" -> apiService.completeDelivery(item.id, lat, lng)
                    "cancel" -> apiService.cancelDelivery(item.id)
                    else -> throw IllegalArgumentException("Unknown action")
                }
                
                if (response.isSuccessful) {
                    Toast.makeText(context, "Berhasil: $action", Toast.LENGTH_SHORT).show()
                    refreshDeliveries()
                } else {
                    Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
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
                            "Delivery Task", 
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
                    // ─── STATISTICS HEADER ───
                    TaskStatsHeader(deliveries = deliveries)

                    Spacer(modifier = Modifier.height(12.dp))

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
                            Text(text = "Tidak ada tugas aktif hari ini.", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(deliveries) { item ->
                                TaskItemCard(
                                    item = item,
                                    currentLocation = currentUserLocation,
                                    onAction = { action -> 
                                        if (action == "complete") {
                                            showCompleteConfirm = item
                                        } else {
                                            updateDeliveryStatus(item, action) 
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 🟢 CONFIRMATION DIALOG
    if (showCompleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showCompleteConfirm = null },
            title = { Text("Konfirmasi Penerimaan") },
            text = { Text("Pastikan uang sudah dihitung dan lengkap sebelum konfirmasi.") },
            confirmButton = {
                Button(
                    onClick = {
                        val item = showCompleteConfirm!!
                        showCompleteConfirm = null
                        updateDeliveryStatus(item, "complete")
                    }
                ) {
                    Text("Sudah Lengkap")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteConfirm = null }) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun TaskStatsHeader(deliveries: List<DeliveryItem>) {
    val pendingCount = deliveries.count { it.status.lowercase() == "pending" }
    val inTransitCount = deliveries.count { it.status.lowercase() == "in_transit" || it.status.lowercase() == "processing" }
    val deliveredCount = deliveries.count { it.status.lowercase() == "delivered" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            count = pendingCount, 
            label = "Pending", 
            color = Color(0xFFFFA000), 
            modifier = Modifier.weight(1f)
        )
        StatCard(
            count = inTransitCount, 
            label = "Proses", 
            color = MaterialTheme.colorScheme.primary, 
            modifier = Modifier.weight(1f)
        )
        StatCard(
            count = deliveredCount, 
            label = "Selesai", 
            color = Color(0xFF4CAF50), 
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(count: Int, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = color.copy(alpha = 0.8f)
                )
            )
        }
    }
}

@Composable
fun TaskItemCard(
    item: DeliveryItem,
    currentLocation: android.location.Location?,
    onAction: (String) -> Unit
) {
    val context = LocalContext.current
    
    // Calculate Distance & ETA
    val destLat = item.destinationAccount?.warehouse?.latitude?.toDoubleOrNull()
    val destLng = item.destinationAccount?.warehouse?.longitude?.toDoubleOrNull()
    
    val distanceInfo = remember(currentLocation, destLat, destLng) {
        if (currentLocation != null && destLat != null && destLng != null) {
            val dist = LocationUtils.calculateDistance(
                currentLocation.latitude, currentLocation.longitude,
                destLat, destLng
            )
            val eta = LocationUtils.estimateETA(dist)
            Pair(LocationUtils.formatDistance(dist), eta)
        } else null
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Invoice & Priority Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.invoice ?: "INV-####",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Priority Badge (Compact)
                val priority = item.priority?.lowercase() ?: "low"
                val priorityColor = when (priority) {
                    "high" -> Color(0xFFFF9800)
                    "urgent" -> Color(0xFFF44336)
                    else -> MaterialTheme.colorScheme.outline
                }
                
                if (priority != "low") {
                    Surface(
                        color = priorityColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, priorityColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = priority.uppercase(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = priorityColor,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Compact Route info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.sourceAccount?.warehouse?.name ?: "Origin",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                )
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(horizontal = 8.dp).size(12.dp), tint = MaterialTheme.colorScheme.outline)
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.destinationAccount?.warehouse?.name ?: "Destination",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                
                // Map Navigation Button
                if (destLat != null && destLng != null) {
                    IconButton(
                        onClick = {
                            val uri = "google.navigation:q=$destLat,$destLng"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                            intent.setPackage("com.google.android.apps.maps")
                            context.startActivity(intent)
                        },
                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = "Maps", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // ─── STATUS STEPPER (Compact) ───
            DeliveryStatusStepper(status = item.status.lowercase())

            Spacer(modifier = Modifier.height(8.dp))

            // Distance / ETA small text
            distanceInfo?.let { (dist, eta) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$dist • Estimasi $eta menit",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Footer: Amount & Action
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "NILAI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatRupiah(item.amount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    )
                }
                
                // Action Button (Compact)
                when (item.status.lowercase()) {
                    "pending" -> {
                        Button(
                            onClick = { onAction("process") },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                        ) {
                            Text("PROSES", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    "processing", "in_transit" -> {
                        Button(
                            onClick = { onAction("complete") },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("TERIMA", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    "delivered" -> {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DeliveryStatusStepper(status: String) {
    val step = when (status) {
        "pending" -> 1
        "processing", "in_transit" -> 2
        "delivered" -> 3
        else -> 1
    }

    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Step 1
        StepperNode(icon = Icons.Default.Inventory2, isActive = true, activeColor = activeColor)
        
        // Line 1
        StepperLine(isActive = step >= 2, color = activeColor, inactiveColor = inactiveColor, modifier = Modifier.weight(1f))
        
        // Step 2
        StepperNode(icon = Icons.Default.LocalShipping, isActive = step >= 2, activeColor = activeColor, inactiveColor = inactiveColor)
        
        // Line 2
        StepperLine(isActive = step >= 3, color = activeColor, inactiveColor = inactiveColor, modifier = Modifier.weight(1f))
        
        // Step 3
        StepperNode(icon = Icons.Default.Home, isActive = step >= 3, activeColor = activeColor, inactiveColor = inactiveColor)
    }
}

@Composable
fun StepperNode(icon: androidx.compose.ui.graphics.vector.ImageVector, isActive: Boolean, activeColor: Color, inactiveColor: Color = Color.LightGray) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(if (isActive) activeColor else inactiveColor.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (isActive) Color.White else inactiveColor
        )
    }
}

@Composable
fun StepperLine(isActive: Boolean, color: Color, inactiveColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(3.dp)
            .background(if (isActive) color else inactiveColor.copy(alpha = 0.2f), CircleShape)
    )
}

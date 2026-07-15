package com.example.jourdroid.ui.app.delivery

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.data.JournalData
import com.example.jourdroid.data.UserData
import com.example.jourdroid.utils.DateUtils
import com.example.jourdroid.utils.FormatterUtils.formatRupiah
import com.example.jourdroid.utils.FormatterUtils.formatTimeOnly
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryScreen(
    user: UserData
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var deliveryJournals by remember { mutableStateOf<List<JournalData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var journalToUpdate by remember { mutableStateOf<JournalData?>(null) }

    val userWarehouseId = user.role?.warehouseId ?: 0

    val refreshDeliveries = suspend {
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
                deliveryJournals = response.data.filter { it.status == 0 }
            } else {
                errorMessage = response.message
            }
        } catch (e: Exception) {
            errorMessage = "Gagal memuat data delivery: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    val updateStatus = { journal: JournalData ->
        scope.launch {
            try {
                isLoading = true
                val apiService = ApiClient.getApiService(context)
                apiService.updateJournalStatus(journal.id, 1)
                Toast.makeText(context, "Delivery ID ${journal.id} Berhasil Diselesaikan!", Toast.LENGTH_SHORT).show()
                refreshDeliveries()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal update status: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
                journalToUpdate = null
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshDeliveries()
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
                    actions = {
                        IconButton(onClick = { scope.launch { refreshDeliveries() } }, enabled = !isLoading) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading && deliveryJournals.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (errorMessage != null && deliveryJournals.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                    }
                } else if (deliveryJournals.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No active delivery tasks for today.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(deliveryJournals) { journal ->
                            DeliveryTaskCard(
                                journal = journal,
                                onClick = { journalToUpdate = journal }
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Prompt
    if (journalToUpdate != null) {
        AlertDialog(
            onDismissRequest = { journalToUpdate = null },
            title = { Text("Confirm Delivery") },
            text = { Text("Are you sure this item (Invoice: ${journalToUpdate?.invoice}) has been delivered?") },
            confirmButton = {
                Button(
                    onClick = { updateStatus(journalToUpdate!!) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Yes, Delivered")
                }
            },
            dismissButton = {
                TextButton(onClick = { journalToUpdate = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun DeliveryTaskCard(
    journal: JournalData,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Invoice & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = journal.invoice ?: "INV-####",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Text(
                    text = formatTimeOnly(journal.dateIssued),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Route Section
            Row(verticalAlignment = Alignment.Top) {
                // Left dots/line visual
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(30.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                )
                            )
                    )
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right labels
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "From: ${journal.cred?.warehouse?.name ?: "Headquarter"}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "To: ${journal.debt?.warehouse?.name ?: "Destination"}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Amount and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatRupiah(journal.amount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check, 
                            contentDescription = null, 
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tap to Complete",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

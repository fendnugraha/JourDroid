package com.example.jourdroid.ui.app.dashboard

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
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
import com.example.jourdroid.data.DailyDashboardData
import com.example.jourdroid.data.UserData
import com.example.jourdroid.ui.component.PrintReportDialog
import com.example.jourdroid.utils.DateUtils
import com.example.jourdroid.utils.FormatterUtils.formatRupiah
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    user: UserData
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var reportData by remember { mutableStateOf<DailyDashboardData?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPrintDialog by remember { mutableStateOf(false) }
    val openingCash = 9000000

    val userWarehouseId = user.warehouseId ?: 0

    val fetchReport = suspend {
        try {
            isLoading = true
            errorMessage = null
            val apiService = ApiClient.getApiService(context)

            val today = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DateUtils.getTodayJakartaFormat()
            } else {
                "2026-07-20"
            }

            val response = apiService.getDailyDashboard(
                warehouse = userWarehouseId,
                startDate = today,
                endDate = today
            )
            
            Log.d("ReportScreen", "Response: $response")

            if (response.success == true) {
                reportData = response.data
            } else {
                errorMessage = response.message
            }
        } catch (e: Exception) {
            errorMessage = "Gagal memuat laporan: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        fetchReport()
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
                        Text(
                            "Daily Overview", 
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        ) 
                    },
                    actions = {
                        IconButton(onClick = { scope.launch { fetchReport() } }, enabled = !isLoading) {
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                }
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    TextButton(onClick = { scope.launch { fetchReport() } }) {
                        Text("Retry")
                    }
                }
            } else if (reportData != null) {
                val data = reportData!!
                
                // Formulas
                val totalRevenue = data.totalFee + data.totalCash + 
                                  (data.totalCashDeposit?.total ?: 0) + 
                                  (data.totalAccessories?.total ?: 0) + 
                                  (data.totalVoucher?.total ?: 0) + 
                                  data.totalExpense
                
                val totalDisetor = if (data.totalCash > openingCash) {
                    totalRevenue - openingCash
                } else {
                    totalRevenue
                }

                val showWarning = totalRevenue < openingCash

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Quick Summary Header
                    HeaderSummaryCard(totalDisetor.toLong())

                    if (showWarning) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Warning: Total Pendapatan is below Opening Cash!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Detailed Breakdown Card
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            SectionHeader("Income Sources", Icons.Default.TrendingUp, Color(0xFF4CAF50))
                            
                            ReportRow("Uang Tunai", data.totalCash.toLong())
                            ReportRow("Voucher", (data.totalVoucher?.total ?: 0).toLong())
                            ReportRow("Accessories", (data.totalAccessories?.total ?: 0).toLong())
                            ReportRow("Deposit", (data.totalCashDeposit?.total ?: 0).toLong())
                            ReportRow("Koreksi", data.totalCorrection.toLong())
                            ReportRow("Fee Jasa", data.totalFee.toLong())

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                            
                            SectionHeader("Deductions", Icons.Default.TrendingDown, Color(0xFFF44336))
                            ReportRow("Biaya Operasional", data.totalExpense.toLong(), color = Color.Red)
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp)
                            
                            ReportRow(
                                label = "Total Pendapatan", 
                                value = totalRevenue.toLong(), 
                                color = if (totalRevenue >= 0) Color(0xFF00897B) else Color.Red,
                                isBold = true,
                                large = true
                            )
                        }
                    }
                    
                    // Final Settlement Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "TOTAL UANG DISETOR", 
                                style = MaterialTheme.typography.labelLarge, 
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatRupiah(totalDisetor.toLong()),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold, 
                                    color = Color(0xFF00796B),
                                    letterSpacing = (-0.5).sp
                                ),
                                modifier = Modifier.align(Alignment.End)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Based on total cash vs opening balance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Button(
                        onClick = { showPrintDialog = true },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("PRINT DAILY REPORT", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    if (showPrintDialog && reportData != null) {
        PrintReportDialog(
            data = reportData!!,
            warehouseName = user.warehouse?.name ?: "Gudang Utama",
            onDismiss = { showPrintDialog = false }
        )
    }
}

@Composable
fun HeaderSummaryCard(amount: Long) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Final Balance", 
                style = MaterialTheme.typography.labelMedium, 
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Text(
                text = formatRupiah(amount),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = (-1).sp
                )
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color) {
    Row(
        modifier = Modifier.padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = iconColor,
                letterSpacing = 1.sp
            )
        )
    }
}

@Composable
fun ReportRow(
    label: String, 
    value: Long, 
    color: Color = Color.Unspecified, 
    isBold: Boolean = false,
    large: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label, 
            style = (if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium).copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                color = if (large) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = formatRupiah(value), 
            style = (if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium).copy(
                fontWeight = FontWeight.ExtraBold,
                color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color
            )
        )
    }
}

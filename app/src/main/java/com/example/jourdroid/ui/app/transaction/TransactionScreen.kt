package com.example.jourdroid.ui.app.transaction

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.data.JournalData
import com.example.jourdroid.data.UserData
import com.example.jourdroid.ui.component.PrintJournalReceiptDialog
import com.example.jourdroid.ui.component.SlideUpModal
import com.example.jourdroid.utils.DateUtils
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

    val userWarehouseId = user.role?.warehouseId ?: 0
    val userName = user.name
    val userWarehouseName = user.role?.warehouse?.name ?: "Tanpa Gudang"

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
                            "Activity", 
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                        ) 
                    },
                    actions = {
                        IconButton(onClick = { scope.launch { refreshJournals() } }, enabled = !isLoading) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    },
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
            val scrollState = rememberScrollState()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // ─── JOURNAL LIST SECTION ───
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Daily Transactions",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = "Today's journal mutations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 800.dp)
                ) {
                    when {
                        isLoading -> {
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
                        journals.isEmpty() -> {
                            Text(
                                text = "No transactions today",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        else -> {
                            JournalTable(
                                journals = journals,
                                onJournalClick = { selectedJournalForPrint = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp)) // Extra space for FAB
            }
        }
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

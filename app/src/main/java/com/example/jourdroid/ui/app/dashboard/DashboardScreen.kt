package com.example.jourdroid.ui.app.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.jourdroid.data.UserData
import com.example.jourdroid.data.JournalData
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.ui.app.transaction.JournalTable
import com.example.jourdroid.ui.component.PrintJournalReceiptDialog
import com.example.jourdroid.utils.BluetoothPrinterManager
import com.example.jourdroid.utils.DateUtils
import com.example.jourdroid.utils.FormatterUtils.formatRupiah

import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    user: UserData,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val printerManager = remember { BluetoothPrinterManager(context) }

    val userName = user.name
    val userEmail = user.email
    val userRole = user.role?.role ?: "No Role"
    val userWarehouseName = user.role?.warehouse?.name ?: "Tanpa Gudang"

    // 🟢 SINKRON: Tipe data menggunakan List<JournalData> sesuai data class barumu
    var journals by remember { mutableStateOf<List<JournalData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedJournalForPrint by remember { mutableStateOf<JournalData?>(null) }

    val userWarehouseId = user.role?.warehouseId ?: 0

    // Fungsi untuk ambil data dari API
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

    // Ambil data jurnal saat pertama kali buka
    LaunchedEffect(Unit) {
        refreshJournals()
    }

    // State Izin Bluetooth (Android 12+)
    var hasBluetoothPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBluetoothPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Izin Bluetooth ditolak. Tidak bisa cetak struk.", Toast.LENGTH_LONG).show()
        }
    }

    // Auto-request permission saat pertama buka halaman jika di Android 12 ke atas
    LaunchedEffect(Unit) {
        if (!hasBluetoothPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    // Menggunakan ScrollState agar UI aman di layar HP ukuran kecil
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ─── CARD PROFIL USER ───
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Selamat Datang, $userName", style = MaterialTheme.typography.headlineSmall)
                Text(text = userEmail, style = MaterialTheme.typography.bodyMedium)
                Text(text = "Role: $userRole", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Warehouse: $userWarehouseName", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── KONDISIONAL DAFTAR JURNAL / MUTASI ───
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "--- DAFTAR MUTASI ---",
                style = MaterialTheme.typography.labelLarge
            )
            TextButton(
                onClick = { scope.launch { refreshJournals() } },
                enabled = !isLoading
            ) {
                Text("Refresh")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
            errorMessage != null -> {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            else -> {
                // Dioper ke komponen JournalTable pembaca JournalData milikmu
                Box(modifier = Modifier.heightIn(max = 450.dp)) {
                    JournalTable(
                        journals = journals,
                        onJournalClick = { selectedJournalForPrint = it }
                    )
                }
            }
        }

        // ─── DIALOG PREVIEW CETAK ───
        if (selectedJournalForPrint != null) {
            PrintJournalReceiptDialog(
                journal = selectedJournalForPrint!!,
                agentName = userName,
                warehouseName = userWarehouseName,
                onDismiss = { selectedJournalForPrint = null }
            )
        }

        // ─── TOMBOL LOGOUT ───
        Button(
            onClick = {
                if (!isLoggingOut) {
                    isLoggingOut = true
                    scope.launch {
                        try {
                            val apiService = ApiClient.getApiService(context)
                            val response = apiService.logout()
                            if (!response.isSuccessful) {
                                Toast.makeText(context, "Sesi server berakhir atau error (${response.code()})", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            // Abaikan error jaringan saat logout, tetap bersihkan data lokal
                        } finally {
                            isLoggingOut = false
                            onLogoutClick() // Selalu panggil logout lokal
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoggingOut
        ) {
            if (isLoggingOut) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onError,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Logout / Keluar")
            }
        }
    }
}
package com.example.jourdroid.ui.app.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
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
import com.example.jourdroid.utils.BluetoothPrinterManager
import com.example.jourdroid.utils.DateUtils

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
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val userWarehouseId = user.role?.warehouseId ?: 0

    // Ambil data jurnal dari API Laravel
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            errorMessage = null
            val apiService = ApiClient.getApiService(context)

            val today = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DateUtils.getTodayJakartaFormat()
            } else {
                "2026-07-09" // Menyesuaikan fallback date yang valid
            }

            // Tembak API-nya! (Token otomatis diurus oleh ApiClient interceptor)
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
            errorMessage = "Gagal memuat data dari server: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
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
        Text(
            text = "--- DAFTAR MUTASI JURNAL HARI INI ---",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.Start)
        )
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
                Box(modifier = Modifier.heightIn(max = 250.dp)) {
                    JournalTable(journals = journals)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── BAGIAN SISTEM PRINTER THERMAL ───
        Text(text = "--- PRINTER THERMAL ---", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        if (!hasBluetoothPermission) {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }) {
                Text("Izinkan Akses Bluetooth")
            }
        } else {
            val pairedPrinters = remember(hasBluetoothPermission) { printerManager.getPairedPrinters() }

            if (pairedPrinters.isEmpty()) {
                Text(
                    text = "Tidak ada printer thermal yang terikat. Silakan pasangkan dahulu lewat menu Bluetooth pengaturan HP.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            } else {
                pairedPrinters.forEach { device ->
                    @SuppressLint("MissingPermission")
                    Button(
                        onClick = {
                            Toast.makeText(context, "Menghubungkan ke ${device.name}...", Toast.LENGTH_SHORT).show()
                            val success = printerManager.connectToPrinter(device)
                            if (success) {
                                Toast.makeText(context, "Koneksi Sukses! Mencetak...", Toast.LENGTH_SHORT).show()
                                val strukText = """
                                    ================================
                                             JOURDROID SHOP         
                                    ================================
                                    Kasir  : $userName
                                    Gudang : $userWarehouseName
                                    --------------------------------
                                    Item 1          Rp 50.000
                                    Item 2          Rp 25.000
                                    --------------------------------
                                    Total           Rp 75.000
                                    ================================
                                       Terima Kasih Telah Belanja   
                                    ================================
                                """.trimIndent()

                                printerManager.printText(strukText)
                                printerManager.disconnect()
                            } else {
                                Toast.makeText(context, "Gagal koneksi ke Printer.", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        @SuppressLint("MissingPermission")
                        Text("Cetak Struk via ${device.name}")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ─── TOMBOL LOGOUT ───
        Button(
            onClick = {
                scope.launch {
                    try {
                        val apiService = ApiClient.getApiService(context)
                        val response = apiService.logout()
                        if (response.isSuccessful) {
                            onLogoutClick()
                        } else {
                            // Jika 405 atau error lain, tetap logout di local untuk keamanan user
                            Toast.makeText(context, "Logout server gagal (${response.code()}), membersihkan sesi lokal...", Toast.LENGTH_SHORT).show()
                            onLogoutClick()
                        }
                    } catch (e: Exception) {
                        onLogoutClick()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout / Keluar")
        }
    }
}
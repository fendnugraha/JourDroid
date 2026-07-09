package com.example.jourdroid.ui.app.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.jourdroid.data.UserData
import com.example.jourdroid.utils.BluetoothPrinterManager

@Composable
fun DashboardScreen(
    user: UserData,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val printerManager = remember { BluetoothPrinterManager(context) }

    val userName = user.name
    val userEmail = user.email
    val userRole = user.role?.role ?: "No Role"
    val userWarehouseName = user.role?.warehouse?.name

    // State untuk memantau apakah izin Bluetooth sudah diberikan atau belum
    var hasBluetoothPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Android 11 kebawah otomatis true jika sudah ada di Manifest
            }
        )
    }

    // Launcher untuk memunculkan pop-up izin bawaan Android (Seperti di JS/React Native)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBluetoothPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Izin Bluetooth ditolak. Tidak bisa cetak struk.", Toast.LENGTH_LONG).show()
        }
    }

    // Otomatis minta izin saat Dashboard pertama kali terbuka (Mirip useEffect kosong [] di React)
    LaunchedEffect(Unit) {
        if (!hasBluetoothPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Card Profil ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Selamat Datang, $userName", style = MaterialTheme.typography.headlineSmall)
                Text(text = userEmail, style = MaterialTheme.typography.bodyMedium)
                Text(text = "Role: ${userRole}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Warehouse: ${userWarehouseName}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "--- PRINTER THERMAL ---", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // Cek kondisi izin terlebih dahulu sebelum merender list printer
        if (!hasBluetoothPermission) {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }) {
                Text("Izinkan Akses Bluetooth")
            }
        } else {
            // Ambil daftar printer hanya jika IZIN SUDAH DIBERIKAN
            val pairedPrinters = remember(hasBluetoothPermission) { printerManager.getPairedPrinters() }

            if (pairedPrinters.isEmpty()) {
                Text(
                    text = "Tidak ada printer thermal yang tersambung. Silakan pairing dulu di pengaturan Bluetooth HP.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
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

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { onLogoutClick() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout / Keluar")
        }
    }
}
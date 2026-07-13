package com.example.jourdroid.ui.component

import android.bluetooth.BluetoothDevice
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.jourdroid.data.JournalData
import com.example.jourdroid.utils.BluetoothPrinterManager
import com.example.jourdroid.utils.FormatterUtils.formatRupiah
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PrintJournalReceiptDialog(
    journal: JournalData,
    agentName: String,
    warehouseName: String,
    startWithPrinterSelection: Boolean = false, // Added parameter
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val printerManager = remember { BluetoothPrinterManager(context) }

    var showBluetoothPrinters by remember { mutableStateOf(startWithPrinterSelection) }
    var bluetoothDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var isPrinting by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var printSuccessMsg by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showBluetoothPrinters) {
        if (showBluetoothPrinters) {
            bluetoothDevices = printerManager.getPairedPrinters()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            bluetoothDevices = printerManager.getPairedPrinters()
            showBluetoothPrinters = true
        } else {
            Toast.makeText(context, "Bluetooth permissions required", Toast.LENGTH_SHORT).show()
        }
    }

    val receiptText = """
        ================================
                 JOURDROID SHOP         
        ================================
        No. Journal : ${journal.id}
        Tanggal     : ${journal.dateIssued}
        User Name   : $agentName
        Tujuan      : ${journal.debt?.warehouse?.name ?: "Cabang"}
        --------------------------------
        JUMLAH: Rp ${formatRupiah(journal.amount)}
        --------------------------------
        ================================
           Hitung sebelum diterima   
        ================================
    """.trimIndent()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cetak Resi Jurnal",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Text("X")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (printSuccessMsg != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "✓", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = printSuccessMsg ?: "", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (errorMsg != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = errorMsg ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (!showBluetoothPrinters) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "PREVIEW RESI THERMAL",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF8CC600)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = receiptText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(receiptText))
                                Toast.makeText(context, "Resi disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Salin")
                        }

                        Button(
                            onClick = {
                                bluetoothDevices = printerManager.getPairedPrinters()
                                showBluetoothPrinters = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D5CFF))
                        ) {
                            Text("Pilih Printer")
                        }
                    }
                } else {
                    Text(
                        text = "Pilih Bluetooth Thermal Printer",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (bluetoothDevices.isEmpty()) {
                        Text(
                            text = "Tidak ada perangkat Bluetooth yang dipairing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            bluetoothDevices.forEach { device ->
                                val isSelected = selectedDevice == device
                                Surface(
                                    selected = isSelected,
                                    onClick = { selectedDevice = device },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("⎙", color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            @SuppressLint("MissingPermission")
                                            val devName = device.name ?: "Unknown Printer"
                                            @SuppressLint("MissingPermission")
                                            val devAddress = device.address
                                            Text(text = devName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text(text = devAddress, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                        if (isSelected) {
                                            Text("✓", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedDevice != null) {
                        Button(
                            onClick = {
                                isTestingConnection = true
                                errorMsg = null
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        printerManager.testConnection(selectedDevice!!)
                                    }
                                    isTestingConnection = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Koneksi ke printer berhasil!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        errorMsg = "Koneksi gagal: ${result.exceptionOrNull()?.message}"
                                    }
                                }
                            },
                            enabled = !isTestingConnection && !isPrinting,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Text("Test Koneksi")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showBluetoothPrinters = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Kembali")
                        }

                        Button(
                            onClick = {
                                val dev = selectedDevice
                                if (dev != null) {
                                    isPrinting = true
                                    errorMsg = null
                                    scope.launch {
                                        val success = withContext(Dispatchers.IO) {
                                            val connected = printerManager.connectToPrinter(dev)
                                            if (connected) {
                                                val bytes = printerManager.generateReceiptBytes(journal, agentName, warehouseName)
                                                printerManager.printBytes(bytes)
                                                printerManager.disconnect()
                                                true
                                            } else false
                                        }
                                        isPrinting = false
                                        if (success) {
                                            printSuccessMsg = "Resi berhasil dicetak!"
                                        } else {
                                            errorMsg = "Gagal mencetak. Pastikan printer menyala."
                                        }
                                    }
                                }
                            },
                            enabled = selectedDevice != null && !isPrinting && !isTestingConnection,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            if (isPrinting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Text("Cetak")
                            }
                        }
                    }
                }
            }
        }
    }
}

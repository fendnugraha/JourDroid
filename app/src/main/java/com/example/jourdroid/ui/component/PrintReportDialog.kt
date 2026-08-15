package com.example.jourdroid.ui.component

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.jourdroid.data.DailyDashboardData
import com.example.jourdroid.utils.BluetoothPrinterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PrintReportDialog(
    data: DailyDashboardData,
    warehouseName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val printerManager = remember { BluetoothPrinterManager(context) }

    var bluetoothDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var isPrinting by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var personalNote by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        bluetoothDevices = printerManager.getPairedPrinters()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Cetak Laporan Harian",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "Pilih Bluetooth Printer",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (bluetoothDevices.isEmpty()) {
                    Text(
                        text = "Tidak ada printer Bluetooth yang ditemukan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bluetoothDevices.forEach { device ->
                            val isSelected = selectedDevice == device
                            Surface(
                                onClick = { selectedDevice = device },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("⎙", color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    @SuppressLint("MissingPermission")
                                    val name = device.name ?: "Unknown Printer"
                                    Text(text = name, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Personal Note Field
                OutlinedTextField(
                    value = personalNote,
                    onValueChange = { personalNote = it },
                    label = { Text("Pesan Personal (Opsional)") },
                    placeholder = { Text("Contoh: Setoran sudah diserahkan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Batal")
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
                                            val bytes = printerManager.generateReportReceiptBytes(data, warehouseName, personalNote)
                                            printerManager.printBytes(bytes)
                                            printerManager.disconnect()
                                            true
                                        } else false
                                    }
                                    isPrinting = false
                                    if (success) {
                                        Toast.makeText(context, "Laporan dicetak!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        errorMsg = "Gagal mencetak. Cek koneksi printer."
                                    }
                                }
                            }
                        },
                        enabled = selectedDevice != null && !isPrinting,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isPrinting) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        else Text("Cetak")
                    }
                }
            }
        }
    }
}

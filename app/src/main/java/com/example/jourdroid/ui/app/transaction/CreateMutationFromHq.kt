package com.example.jourdroid.ui.app.transaction

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.data.AccountItem
import com.example.jourdroid.data.JournalData
import com.example.jourdroid.data.WarehouseItem
import com.example.jourdroid.utils.DateUtils
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMutationFromHq(
    onSuccess: (JournalData?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var warehouses by remember { mutableStateOf<List<WarehouseItem>>(emptyList()) }
    var allAccounts by remember { mutableStateOf<List<AccountItem>>(emptyList()) }

    var selectedWarehouse by remember { mutableStateOf<WarehouseItem?>(null) }
    var fromAccount by remember { mutableStateOf<AccountItem?>(null) }
    var toAccount by remember { mutableStateOf<AccountItem?>(null) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dateIssued by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var autoPrint by remember { mutableStateOf(true) }

    // Dropdown expanded states
    var warehouseExpanded by remember { mutableStateOf(false) }
    var fromAccountExpanded by remember { mutableStateOf(false) }
    var toAccountExpanded by remember { mutableStateOf(false) }

    // Initial data fetch
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            val apiService = ApiClient.getApiService(context)
            
            // Set default date to today with time in Jakarta timezone
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                dateIssued = DateUtils.getNowJakartaFormat()
            } else {
                val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
                dateIssued = String.format(Locale.US, "%04d-%02d-%02d %02d:%02d:%02d", 
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))
            }

            val warehouseResponse = apiService.getAllWarehouses()
            if (warehouseResponse.success) {
                warehouses = warehouseResponse.warehouses.filter { w: WarehouseItem -> w.id != 1 }
            }

            val accountResponse = apiService.getAllAccounts()
            if (accountResponse.success) {
                allAccounts = accountResponse.accounts
                if (allAccounts.isEmpty()) {
                    Toast.makeText(context, "Data akun kosong dari server", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Gagal ambil akun: ${accountResponse.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal memuat data: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            isLoading = false
        }
    }

    // Filter accounts
    val fromAccounts = remember(allAccounts) {
        allAccounts.filter { it.warehouseId == 1 }
    }
    
    val toAccounts = remember(allAccounts, selectedWarehouse) {
        val wh = selectedWarehouse
        if (wh == null) emptyList()
        else allAccounts.filter { it.warehouseId == wh.id }
    }

    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val datePart = if (dateIssued.contains(" ")) dateIssued.split(" ")[0] else dateIssued
            dateIssued = String.format(Locale.US, "%s %02d:%02d:00", datePart, hourOfDay, minute)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val datePart = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            val timePart = if (dateIssued.contains(" ")) dateIssued.split(" ")[1] else "00:00:00"
            dateIssued = "$datePart $timePart"
            timePickerDialog.show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Informasi Mutasi HQ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (allAccounts.isEmpty() && !isLoading) {
                Text(
                    text = "⚠️ Akun tidak ditemukan. Pastikan data akun sudah tersedia.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Date Picker
            OutlinedTextField(
                value = dateIssued,
                onValueChange = { },
                label = { Text("Tanggal") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.EditCalendar, contentDescription = "Pilih Tanggal")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Warehouse Selection
            ExposedDropdownMenuBox(
                expanded = warehouseExpanded,
                onExpandedChange = { warehouseExpanded = !warehouseExpanded }
            ) {
                OutlinedTextField(
                    value = selectedWarehouse?.name ?: "Pilih Warehouse Tujuan",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Warehouse Tujuan") },
                    leadingIcon = { Icon(Icons.Default.Warehouse, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = warehouseExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                ExposedDropdownMenu(
                    expanded = warehouseExpanded,
                    onDismissRequest = { warehouseExpanded = false }
                ) {
                    warehouses.forEach { warehouse ->
                        DropdownMenuItem(
                            text = { Text(warehouse.name) },
                            onClick = {
                                selectedWarehouse = warehouse
                                toAccount = null // Reset toAccount when warehouse changes
                                warehouseExpanded = false
                            }
                        )
                    }
                }
            }

            // From Account (HQ)
            ExposedDropdownMenuBox(
                expanded = fromAccountExpanded,
                onExpandedChange = { fromAccountExpanded = !fromAccountExpanded }
            ) {
                OutlinedTextField(
                    value = fromAccount?.accName ?: "Pilih Akun Sumber (HQ)",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Akun Sumber (HQ)") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromAccountExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                ExposedDropdownMenu(
                    expanded = fromAccountExpanded,
                    onDismissRequest = { fromAccountExpanded = false }
                ) {
                    fromAccounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text("${account.accName} (${account.accCode})") },
                            onClick = {
                                fromAccount = account
                                fromAccountExpanded = false
                            }
                        )
                    }
                }
            }

            // To Account
            ExposedDropdownMenuBox(
                expanded = toAccountExpanded,
                onExpandedChange = { toAccountExpanded = !toAccountExpanded }
            ) {
                OutlinedTextField(
                    value = toAccount?.accName ?: "Pilih Akun Tujuan",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Akun Tujuan") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toAccountExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                    enabled = selectedWarehouse != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                if (selectedWarehouse != null) {
                    ExposedDropdownMenu(
                        expanded = toAccountExpanded,
                        onDismissRequest = { toAccountExpanded = false }
                    ) {
                        toAccounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text("${account.accName} (${account.accCode})") },
                                onClick = {
                                    toAccount = account
                                    toAccountExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                label = { Text("Jumlah (Rp)") },
                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Keterangan") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Auto Print Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Print, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Cetak Struk Otomatis",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                }
                Switch(
                    checked = autoPrint,
                    onCheckedChange = { autoPrint = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = {
                    if (dateIssued.isEmpty() || fromAccount == null || toAccount == null || amount.isEmpty()) {
                        Toast.makeText(context, "Mohon lengkapi semua field", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSubmitting = true
                    scope.launch {
                        try {
                            val apiService = ApiClient.getApiService(context)
                            val response = apiService.createMutation(
                                dateIssued = dateIssued,
                                debtCode = toAccount!!.id,   // Debt = To Account
                                credCode = fromAccount!!.id, // Cred = From Account
                                isConfirmed = 1,             // 🟢 Send as 1 (True)
                                amount = amount.toInt(),
                                feeAmount = 0,
                                trxType = "Mutasi Kas",
                                description = description
                            )
                            
                            if (response.isSuccessful) {
                                val createResponse = response.body()
                                val createdJournal = createResponse?.data
                                Toast.makeText(context, "Mutasi berhasil dibuat", Toast.LENGTH_SHORT).show()
                                
                                if (autoPrint) {
                                    if (createdJournal != null) {
                                        onSuccess(createdJournal)
                                    } else {
                                        Toast.makeText(context, "Berhasil, tapi data cetak tidak diterima dari server", Toast.LENGTH_LONG).show()
                                        onSuccess(null)
                                    }
                                } else {
                                    onSuccess(null)
                                }
                            } else {
                                val errorBody = response.errorBody()?.string()
                                Toast.makeText(context, "Gagal: ${response.code()} $errorBody", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal membuat mutasi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isSubmitting,
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Buat Mutasi", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

package com.example.jourdroid.ui.app.attendance

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.data.AttendanceData
import com.example.jourdroid.data.UserData
import com.example.jourdroid.data.WarehouseItem
import com.example.jourdroid.utils.FormatterUtils
import com.example.jourdroid.utils.ImageUtils
import com.example.jourdroid.utils.LocationHelper
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val FILE_PROVIDER_AUTHORITY = "com.example.jourdroid.fileprovider"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    user: UserData,
    onSuccess: (AttendanceData) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiService = remember { ApiClient.getApiService(context) }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    
    var currentLocation by remember { mutableStateOf<android.location.Location?>(null) }
    var nearestWarehouse by remember { mutableStateOf<WarehouseItem?>(null) }
    
    var isLoadingLocation by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedPhotoUri = photoUri
            Log.d("AttendanceScreen", "Photo taken successfully and set to capturedPhotoUri: $capturedPhotoUri")
        } else {
            Log.w("AttendanceScreen", "Photo taking failed or cancelled")
            photoUri = null
            capturedPhotoUri = null
            tempPhotoFile = null
        }
    }

    val launchCamera = {
        try {
            val file = createTempFile(context)
            tempPhotoFile = file
            val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
            photoUri = uri
            // capturedPhotoUri is NOT set here to avoid Coil trying to load an empty file
            Log.d("AttendanceScreen", "Launching camera with URI: $uri")
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Log.e("AttendanceScreen", "Failed to launch camera", e)
            Toast.makeText(context, "Gagal membuka kamera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    val syncLocation = {
        scope.launch {
            isLoadingLocation = true
            errorMessage = null
            try {
                val loc = LocationHelper.getCurrentLocation(context)
                if (loc != null) {
                    currentLocation = loc
                    Log.d("AttendanceScreen", "Current Location: ${loc.latitude}, ${loc.longitude} (Accuracy: ${loc.accuracy})")
                    val response = apiService.getNearestWarehouse(loc.latitude, loc.longitude)
                    Log.d("AttendanceScreen", "API Response: found=${response.found}, message=${response.message}")
                    if (response.found) {
                        nearestWarehouse = response.warehouse
                        Log.d("AttendanceScreen", "Warehouse found: ${nearestWarehouse?.name} (Distance: ${nearestWarehouse?.distance}m)")
                    } else {
                        nearestWarehouse = null
                        errorMessage = response.message ?: "Di luar jangkauan lokasi absen. Pastikan Anda berada di area Warehouse."
                    }
                } else {
                    errorMessage = "Gagal mendapatkan GPS. Pastikan lokasi aktif dan izin diberikan."
                }
            } catch (e: Exception) {
                Log.e("AttendanceScreen", "syncLocation Error", e)
                errorMessage = "Error lokasi: ${e.localizedMessage}"
            } finally {
                isLoadingLocation = false
            }
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            syncLocation()
        } else {
            Toast.makeText(context, "Izin lokasi diperlukan untuk absensi", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        requestPermissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        ))
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-100).dp)
                .background(Color(0xFF6366F1).copy(alpha = 0.1f), CircleShape)
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Presensi Kehadiran", fontWeight = FontWeight.Bold, color = Color.White) },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = Color(0xFFF87171))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF6366F1).copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                (user.name ?: "U").take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF818CF8)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Halo, ${user.name}", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Siap untuk absen hari ini?", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (nearestWarehouse != null) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFF59E0B).copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    null,
                                    tint = if (nearestWarehouse != null) Color(0xFF34D399) else Color(0xFFFBBF24),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("LOKASI SAAT INI", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(
                                    nearestWarehouse?.name ?: if (isLoadingLocation) "Mencari lokasi..." else "Di Luar Jangkauan",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (nearestWarehouse != null && nearestWarehouse?.distance != null) {
                                    Text(
                                        "Jarak: ${nearestWarehouse?.distance} m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF34D399)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { syncLocation() },
                                enabled = !isLoadingLocation
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (currentLocation != null) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.5f))
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Navigation, null, tint = Color(0xFF6366F1), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Sinyal GPS:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Surface(
                                    color = if (currentLocation!!.accuracy <= 50) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFF59E0B).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (currentLocation!!.accuracy <= 50) Color(0xFF34D399).copy(alpha = 0.2f) else Color(0xFFFBBF24).copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        "± ${currentLocation!!.accuracy.toInt()} m",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentLocation!!.accuracy <= 50) Color(0xFF34D399) else Color(0xFFFBBF24)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MyLocation, null, tint = Color(0xFF6366F1), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Koordinat:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Text(
                                    "${"%.6f".format(currentLocation!!.latitude)}, ${"%.6f".format(currentLocation!!.longitude)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "FOTO ABSEN HARI INI",
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF020617).copy(alpha = 0.4f))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color(0xFF334155).copy(alpha = 0.1f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (capturedPhotoUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(capturedPhotoUri),
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .clickable { 
                                    capturedPhotoUri = null
                                    launchCamera() 
                                },
                            color = Color(0xFFF43F5E).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, null, tint = Color(0xFFFB7185), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Foto Ulang", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFB7185))
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFF6366F1).copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, null, tint = Color(0xFF818CF8), modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Ambil Foto Selfie", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Wajah harus terlihat jelas", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Button(
                                onClick = {
                                    if (currentLocation == null) {
                                        Toast.makeText(context, "Aktifkan GPS terlebih dahulu", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    launchCamera()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Icon(Icons.Default.Camera, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Buka Kamera")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoTile(
                        icon = Icons.Default.CalendarToday,
                        label = "Tanggal",
                        value = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date()) },
                        modifier = Modifier.weight(1f)
                    )
                    InfoTile(
                        icon = Icons.Default.Schedule,
                        label = "Jam Absen",
                        value = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()) },
                        modifier = Modifier.weight(1f)
                    )
                }

                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f))
                        ) {
                            Text(
                                it,
                                modifier = Modifier.padding(16.dp),
                                color = Color(0xFFF87171),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (capturedPhotoUri == null || tempPhotoFile == null) return@Button
                        
                        scope.launch {
                            try {
                                isSubmitting = true
                                // 🟢 1. Check status first before submission
                                val statusResponse = apiService.getUserCheckedInStatus()
                                if (statusResponse.hasCheckedIn) {
                                    Toast.makeText(context, "Anda sudah melakukan absen hari ini.", Toast.LENGTH_LONG).show()
                                    // Fallback data if already checked in
                                    onSuccess(AttendanceData(
                                        id = null,
                                        userId = user.id,
                                        warehouseId = user.warehouseId,
                                        photo = null,
                                        timeIn = "--:--",
                                        date = "Today",
                                        note = null,
                                        longitude = null,
                                        latitude = null,
                                        approvalStatus = "Already Checked In",
                                        warehouseName = user.warehouse?.name ?: "Warehouse"
                                    ))
                                    return@launch
                                }

                                // 🟢 2. Proceed with submission if not checked in
                                val compressedFile = ImageUtils.compressImage(context, capturedPhotoUri!!, 300) // 🟢 Reduced to 300KB for faster upload
                                val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                val photoPart = MultipartBody.Part.createFormData("photo", compressedFile.name, requestFile)
                                
                                val lat = currentLocation!!.latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                                val lng = currentLocation!!.longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                                val warehouseId = (nearestWarehouse?.id ?: 0).toString().toRequestBody("text/plain".toMediaTypeOrNull())
                                val role = (user.role?.toString() ?: "Staff").toRequestBody("text/plain".toMediaTypeOrNull())
                                val timeIn = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()).toRequestBody("text/plain".toMediaTypeOrNull())

                                val response = apiService.createAttendance(
                                    photo = photoPart,
                                    latitude = lat,
                                    longitude = lng,
                                    warehouseId = warehouseId,
                                    role = role,
                                    timeIn = timeIn
                                )

                                if (response.isSuccessful) {
                                    val attendanceData = response.body()?.data?.copy(
                                        warehouseName = nearestWarehouse?.name
                                    ) ?: AttendanceData(
                                        id = null,
                                        userId = user.id,
                                        warehouseId = nearestWarehouse?.id,
                                        photo = null,
                                        timeIn = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                        note = null,
                                        longitude = currentLocation!!.longitude.toString(),
                                        latitude = currentLocation!!.latitude.toString(),
                                        approvalStatus = "Pending",
                                        warehouseName = nearestWarehouse?.name
                                    )
                                    onSuccess(attendanceData)
                                } else {
                                    errorMessage = "Absensi gagal: ${response.code()}"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Error: ${e.localizedMessage}"
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isSubmitting && capturedPhotoUri != null && nearestWarehouse != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1),
                        disabledContainerColor = Color(0xFF1E293B)
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Kirim Absen Masuk", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun InfoTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1E293B).copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

private fun createTempFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = File(context.externalCacheDir, "attendance")
    if (!storageDir.exists()) storageDir.mkdirs()
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
}

package com.example.jourdroid.ui.component

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.data.UserData
import com.example.jourdroid.utils.LocationHelper
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AttendanceFormDialog(
    user: UserData,
    visible: Boolean,
    onClose: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<android.location.Location?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Get location when form opens
    LaunchedEffect(visible) {
        if (visible) {
            currentLocation = LocationHelper.getCurrentLocation(context)
        }
    }

    SlideUpModal(
        visible = visible,
        title = "Check-in Kehadiran",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Upload Foto Selfie & GPS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Photo Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                        Text("Belum ada foto", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pilih Foto")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // GPS Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = if (currentLocation != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Lokasi Presensi", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (currentLocation != null) "${currentLocation?.latitude}, ${currentLocation?.longitude}" else "Mencari lokasi...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (selectedImageUri == null) {
                        Toast.makeText(context, "Harap pilih foto terlebih dahulu", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (currentLocation == null) {
                        Toast.makeText(context, "Lokasi tidak ditemukan", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        try {
                            isSubmitting = true
                            val apiService = ApiClient.getApiService(context)
                            
                            val file = getFileFromUri(context, selectedImageUri!!)
                            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                            val photoPart = MultipartBody.Part.createFormData("photo", file.name, requestFile)
                            
                            val lat = currentLocation!!.latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                            val lng = currentLocation!!.longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                            val warehouseId = (user.warehouseId ?: 0).toString().toRequestBody("text/plain".toMediaTypeOrNull())
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
                                Toast.makeText(context, "Absensi berhasil!", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } else {
                                Toast.makeText(context, "Absensi gagal: ${response.code()}", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting && selectedImageUri != null
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Kirim Absensi", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getFileFromUri(context: android.content.Context, uri: Uri): File {
    val contentResolver = context.contentResolver
    val inputStream = contentResolver.openInputStream(uri)
    val file = File(context.cacheDir, "attendance_photo_${System.currentTimeMillis()}.jpg")
    val outputStream = FileOutputStream(file)
    inputStream?.use { input ->
        outputStream.use { output ->
            input.copyTo(output)
        }
    }
    return file
}

package com.example.jourdroid.ui.app.attendance

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.jourdroid.data.AttendanceData
import com.example.jourdroid.ui.component.AttendanceTicket
import com.example.jourdroid.utils.FormatterUtils
import com.example.jourdroid.utils.ShareUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceSuccessScreen(
    userName: String,
    attendanceData: AttendanceData,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    var ticketView by remember { mutableStateOf<ComposeView?>(null) }
    
    val displayTime = remember { 
        FormatterUtils.formatTimeOnly(attendanceData.timeIn?.toString())
    }
    val displayDate = remember {
        FormatterUtils.formatLongDate(attendanceData.date?.toString())
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
    )

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(gradient),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Bukti Presensi", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // The Ticket component wrapped in an AndroidView for reliable capture
            AndroidView(
                factory = { ctx ->
                    ComposeView(ctx).apply {
                        setContent {
                            AttendanceTicket(
                                userName = userName,
                                warehouseName = attendanceData.warehouseName ?: "Gudang Tidak Diketahui",
                                data = attendanceData,
                                displayTime = displayTime,
                                displayDate = displayDate
                            )
                        }
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .width(320.dp)
                    .wrapContentHeight(),
                update = { ticketView = it }
            )

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Share Button
                OutlinedButton(
                    onClick = {
                        try {
                            val view = ticketView
                            if (view == null || view.width <= 0 || view.height <= 0) {
                                Toast.makeText(context, "Gagal mengambil gambar. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            
                            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            view.draw(canvas)
                            
                            val uri = ShareUtils.saveBitmapToCache(context, bitmap)
                            if (uri != null) {
                                ShareUtils.shareImage(context, uri)
                            } else {
                                Toast.makeText(context, "Gagal menyimpan gambar ke cache", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Bagikan")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bagikan", fontWeight = FontWeight.Bold)
                }

                // Done Button
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Selesai")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Selesai", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Terima kasih sudah absen hari ini!",
                color = Color.Gray,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

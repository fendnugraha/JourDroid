package com.example.jourdroid.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jourdroid.data.AttendanceData

@Composable
fun AttendanceTicket(
    userName: String,
    warehouseName: String,
    data: AttendanceData,
    displayTime: String,
    displayDate: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val photoUrl = data.photo // This might be a partial path or full URL

    Column(
        modifier = modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(primaryColor, primaryColor.copy(alpha = 0.8f))
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ABSENSI BERHASIL",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Photo Section (New)
        if (!photoUrl.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.LightGray.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(2.dp, primaryColor.copy(alpha = 0.3f))
            ) {
                val url = if (photoUrl.startsWith("http")) photoUrl else "https://sandbox.three-komunika.com/storage/$photoUrl"
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .allowHardware(false) // 🟢 Disable hardware bitmaps for safe sharing capture
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto Absensi",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }

        // Info Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TicketInfoItem("NAMA PENGGUNA", userName)
            TicketInfoItem("GUDANG", warehouseName)
            TicketInfoItem("JAM MASUK", displayTime)
            TicketInfoItem("TANGGAL", displayDate)
            
            val statusStr = when (val s = data.approvalStatus) {
                "1", 1, 1.0 -> "Disetujui"
                "Late", "late" -> "Terlambat"
                else -> s?.toString() ?: "Menunggu"
            }
            TicketInfoItem("STATUS", statusStr, valueColor = if (statusStr == "Disetujui") Color(0xFF4CAF50) else primaryColor)

            Spacer(modifier = Modifier.height(8.dp))
            
            // Divider with perforated effect
            DashedDivider(color = Color.LightGray.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(8.dp))

            // Location Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lokasi Terverifikasi GPS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "Lat: ${data.latitude ?: "-"}\nLng: ${data.longitude ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "JourDroid - Smart Workforce Management",
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TicketInfoItem(label: String, value: String, valueColor: Color = Color.Black) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DashedDivider(color: Color) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, 0.5f),
            end = Offset(size.width, 0.5f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }
}

package com.example.jourdroid.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jourdroid.data.UserData

@Composable
fun ProfileAvatar(
    user: UserData,
    modifier: Modifier = Modifier
) {
    val photoUrl = user.contact?.contactPhotoUrl?.takeIf { it.isNotBlank() } ?: user.contact?.photo?.takeIf { it.isNotBlank() }
    
    Surface(
        modifier = modifier
            .padding(start = 16.dp)
            .size(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        if (!photoUrl.isNullOrEmpty()) {
            val url = when {
                photoUrl.startsWith("http") -> photoUrl
                photoUrl.startsWith("/") -> "https://sandbox.three-komunika.com$photoUrl"
                photoUrl.startsWith("storage/") -> "https://sandbox.three-komunika.com/$photoUrl"
                photoUrl.startsWith("contact/") -> "https://sandbox.three-komunika.com/storage/$photoUrl"
                else -> "https://sandbox.three-komunika.com/storage/$photoUrl"
            }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .allowHardware(false)
                    .build(),
                contentDescription = "Profile",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val name = user.contact?.name ?: user.name ?: "U"
            Box(contentAlignment = Alignment.Center, modifier = Modifier.background(MaterialTheme.colorScheme.primary)) {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun CompactPageTitle(user: UserData, title: String) {
    val name = user.contact?.name ?: user.name ?: "User"
    val firstName = name.split(" ").firstOrNull() ?: name

    Column(verticalArrangement = Arrangement.Center, modifier = Modifier.padding(start = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp
            )
        )
        Text(
            text = firstName,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                letterSpacing = (-0.3).sp
            )
        )
    }
}

@Composable
fun NotificationBadge(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.padding(end = 12.dp)
    ) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            fontSize = 8.sp
                        )
                    }
                }
            }
        ) {
            Icon(
                imageVector = if (unreadCount > 0) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                contentDescription = "Notifications",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

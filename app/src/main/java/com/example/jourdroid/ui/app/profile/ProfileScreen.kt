package com.example.jourdroid.ui.app.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.data.UserData
import com.example.jourdroid.ui.component.AttendanceFormDialog
import com.example.jourdroid.ui.component.CompactPageTitle
import com.example.jourdroid.ui.component.NotificationBadge
import com.example.jourdroid.ui.component.ProfileAvatar
import com.example.jourdroid.utils.DateUtils
import com.example.jourdroid.utils.FormatterUtils.formatLongDate
import com.example.jourdroid.utils.FormatterUtils.formatRupiah
import com.example.jourdroid.utils.FormatterUtils.formatShortDate
import com.example.jourdroid.utils.ImageUtils
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: UserData,
    unreadNotificationCount: Int = 0,
    onNavigateToNotifications: () -> Unit = {},
    onLogoutClick: () -> Unit,
    onUserUpdate: (UserData) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoggingOut by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    val userRole = user.role?.toString() ?: ""
    val isAdmin = listOf("Administrator", "Super Admin").any { it.equals(userRole, ignoreCase = true) }
    
    var isCheckedIn by remember { mutableStateOf(user.hasCheckedIn ?: false) }
    var showAttendanceForm by remember { mutableStateOf(false) }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    isUploadingPhoto = true
                    val compressedFile = ImageUtils.compressImage(context, it, 300)
                    val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val photoPart = MultipartBody.Part.createFormData("photo", compressedFile.name, requestFile)
                    
                    // Preparation of other required fields from current user data
                    val name = (user.contact?.name ?: user.name ?: "User").toRequestBody("text/plain".toMediaTypeOrNull())
                    val phone = (user.contact?.phone ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
                    val address = (user.contact?.address ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
                    val telegramId = (user.contact?.telegramChatId ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
                    
                    val apiService = ApiClient.getApiService(context)
                    val contactId = user.contact?.id ?: user.contactId ?: 0
                    
                    if (contactId == 0) {
                        Toast.makeText(context, "ID Kontak tidak ditemukan", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val response = apiService.updateContactPhoto(
                        id = contactId,
                        photo = photoPart,
                        name = name,
                        phone = phone,
                        address = address,
                        telegramChatId = telegramId,
                        method = "PUT"
                    )
                    
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Foto profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        
                        // 🟢 Use the updated contact data from response body directly
                        val updateBody = response.body()
                        if (updateBody != null) {
                            val updatedUser = user.copy(contact = updateBody.data)
                            onUserUpdate(updatedUser)
                        } else {
                            // Fallback to refresh if body is null
                            val profileResponse = apiService.getUserProfile()
                            profileResponse.user?.let { updatedUser -> onUserUpdate(updatedUser) }
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(context, "Gagal mengunggah: ${response.code()} $errorBody", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingPhoto = false
                }
            }
        }
    }

    val contact = user.contact
    val employee = user.contact?.employee
    val warehouse = user.warehouse
    val warning = employee?.warningActive
    val roleName = user.role?.toString() ?: "Staff"
    val displayName = contact?.name ?: user.name ?: "User"
    val userPhotoUrl = user.contact?.contactPhotoUrl?.takeIf { it.isNotBlank() } ?: user.contact?.photo?.takeIf { it.isNotBlank() }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.surface
        )
    )

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoggingOut) showLogoutDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFFF1F2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    "Keluar dari Akun?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    "Anda akan keluar dari akun ${displayName}. Sesi saat ini akan diakhiri.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isLoggingOut) {
                            isLoggingOut = true
                            scope.launch {
                                try {
                                    ApiClient.getApiService(context).logout()
                                } catch (_: Exception) {
                                } finally {
                                    isLoggingOut = false
                                    showLogoutDialog = false
                                    onLogoutClick()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE11D48),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoggingOut
                ) {
                    if (isLoggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Keluar", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoggingOut
                ) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        CompactPageTitle(user = user, title = "Profile")
                    },
                    navigationIcon = {
                        ProfileAvatar(user = user)
                    },
                    actions = {
                        NotificationBadge(
                            unreadCount = unreadNotificationCount,
                            onClick = onNavigateToNotifications
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ─── 1. HERO USER PROFILE CARD ───
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Avatar with Upload functionality
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Surface(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clickable { photoPickerLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    shadowElevation = 4.dp,
                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isUploadingPhoto) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else if (!userPhotoUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(userPhotoUrl)
                                                    .crossfade(true)
                                                    .allowHardware(false)
                                                    .build(),
                                                contentDescription = "Foto Profil",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = (displayName.ifBlank { "U" }).take(1).uppercase(),
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black
                                                )
                                            )
                                        }
                                    }
                                }
                                
                                // Camera Icon Badge
                                Surface(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .offset(x = 4.dp, y = 4.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface)
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Ubah Foto",
                                        modifier = Modifier.padding(4.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }

                            // Name & Badges
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    maxLines = 2
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Role Badge
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Shield,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = roleName,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }
                                    }

                                    // Active Status Badge
                                    if (user.isActive == 1) {
                                        Surface(
                                            color = Color(0xFFE8F5E9),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(Color(0xFF4CAF50), CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "Aktif",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF2E7D32)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                // Email
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Email,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = user.email ?: "-",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    if (user.emailVerifiedAt != null) {
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = "Verified",
                                            modifier = Modifier.size(14.dp),
                                            tint = Color(0xFF0EA5E9)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Attendance Status Widget
                        if (!isAdmin) {
                            Surface(
                                color = if (isCheckedIn) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isCheckedIn) Color(0xFFBBF7D0) else Color(0xFFFDE68A)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isCheckedIn) {
                                    scope.launch {
                                        try {
                                            val apiService = ApiClient.getApiService(context)
                                            val statusResponse = apiService.getUserCheckedInStatus()
                                            isCheckedIn = statusResponse.hasCheckedIn
                                            
                                            // 🟢 Refresh full user data to update warehouse_id etc
                                            val profileResponse = apiService.getUserProfile()
                                            profileResponse.user?.let { onUserUpdate(it) }
                                            
                                            if (!isCheckedIn) showAttendanceForm = true
                                        } catch (e: Exception) {
                                            showAttendanceForm = true
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                if (isCheckedIn) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                                RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isCheckedIn) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isCheckedIn) Color(0xFF16A34A) else Color(0xFFD97706)
                                        )
                                    }
                                    Column {
                                        Text(
                                            "Status Absensi Hari Ini",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (isCheckedIn) "Sudah Check-in" else "Belum Check-in",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCheckedIn) Color(0xFF15803D) else Color(0xFFB45309)
                                            )
                                        )
                                    }
                                }
                                    if (!isCheckedIn) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AttendanceFormDialog(
                    user = user,
                    visible = showAttendanceForm,
                    onClose = { showAttendanceForm = false },
                    onSuccess = {
                        showAttendanceForm = false
                        isCheckedIn = true
                        // 🟢 Refresh full user data after attendance success
                        scope.launch {
                            try {
                                val profileResponse = ApiClient.getApiService(context).getUserProfile()
                                profileResponse.user?.let { onUserUpdate(it) }
                            } catch (_: Exception) {}
                        }
                    }
                )

                // ─── 2. ACTIVE SANCTION / WARNING (SP) ───
                AnimatedVisibility(visible = warning != null) {
                    warning?.let { w ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "SANKSI AKTIF: ${w.level ?: "-"}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "s/d ${formatShortDate(w.expiredDate)}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Alasan: ${w.reason ?: "-"}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onErrorContainer)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ─── 3. EMPLOYMENT DATA ───
                ProfileSectionHeader(
                    title = "Data Kepegawaian",
                    icon = Icons.Default.Badge
                )
                InfoSectionCard {
                    InfoRowItem(
                        icon = Icons.Default.Cake,
                        label = "Tempat, Tgl Lahir",
                        value = "${employee?.placeOfBirth ?: "-"}, ${formatLongDate(employee?.birthDate)}"
                    )
                    ProfileDivider()
                    InfoRowItem(
                        icon = Icons.Default.AccountBalance,
                        label = "Agama",
                        value = employee?.religion?.capitalize() ?: "-"
                    )
                    ProfileDivider()
                    InfoRowItem(
                        icon = Icons.Default.Favorite,
                        label = "Status Pernikahan",
                        value = employee?.maritalStatus?.capitalize() ?: "-"
                    )
                    ProfileDivider()
                    InfoRowItem(
                        icon = Icons.Default.CalendarToday,
                        label = "Tanggal Bergabung",
                        value = "${formatShortDate(employee?.hireDate)} (${DateUtils.calculateWorkDuration(employee?.hireDate)})"
                    )
                    ProfileDivider()
                    InfoRowItem(
                        icon = Icons.Default.WorkOutline,
                        label = "Tipe Pekerjaan",
                        value = employee?.employmentType?.replace("_", " ")?.uppercase() ?: "-"
                    )
                    ProfileDivider()
                    InfoRowItem(
                        icon = Icons.Default.Payments,
                        label = "Gaji Pokok",
                        value = formatRupiah(employee?.baseSalary),
                        isValueBold = true,
                        valueColor = Color(0xFF15803D)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ─── 4. ASSIGNED BRANCH / WAREHOUSE ───
                ProfileSectionHeader(
                    title = "Cabang Penugasan",
                    icon = Icons.Default.Storefront
                )
                InfoSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Nama Outlet",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = warehouse?.name ?: "-",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "KODE: ${warehouse?.code ?: "-"}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    ProfileDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    InfoRowItem(
                        icon = Icons.Default.Schedule,
                        label = "Jam Operasional",
                        value = "Buka Pukul ${warehouse?.openingTime ?: "-"} WIB"
                    )

                    ProfileDivider()

                    InfoRowItem(
                        icon = Icons.Default.LocationOn,
                        label = "Alamat Lengkap",
                        value = warehouse?.address ?: "-"
                    )
                }

                // ─── 5. RECEIVABLES (HUTANG PIUTANG) ───
                val empReceivable = contact?.employeeReceivablesSum?.total ?: 0.0
                val instReceivable = contact?.installmentReceivablesSum?.total ?: 0.0
                val totalReceivables = empReceivable + instReceivable
                
                if (totalReceivables > 0) {
                    Spacer(modifier = Modifier.height(20.dp))

                    ProfileSectionHeader(
                        title = "Hutang Piutang",
                        icon = Icons.Default.Wallet
                    )
                    InfoSectionCard {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total Sisa Piutang",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    formatRupiah(totalReceivables.toLong()),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        InfoRowItem(
                            icon = Icons.Default.Money,
                            label = "Piutang Kasbon / Langsung",
                            value = formatRupiah(empReceivable.toLong())
                        )
                        ProfileDivider()
                        InfoRowItem(
                            icon = Icons.Default.AccountBalanceWallet,
                            label = "Piutang Cicilan",
                            value = formatRupiah(instReceivable.toLong())
                        )
                    }
                }

                // ─── 6. ACCOUNT SYSTEM DETAILS ───
                ProfileSectionHeader(
                    title = "Detail Akun Sistem",
                    icon = Icons.Default.Info
                )
                InfoSectionCard {
                    SystemInfoRow(Icons.Default.Key, "User ID", "#${user.id}")
                    ProfileDivider()
                    SystemInfoRow(Icons.Default.Fingerprint, "Contact ID", "#${user.contactId ?: "-"}")
                    ProfileDivider()
                    SystemInfoRow(Icons.Default.CalendarMonth, "Terdaftar", formatShortDate(user.createdAt))
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ─── 7. LOGOUT BUTTON ───
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    enabled = !isLoggingOut
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "KELUAR AKUN",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun ProfileSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
fun InfoSectionCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            content()
        }
    }
}

@Composable
fun InfoRowItem(
    icon: ImageVector,
    label: String,
    value: String,
    isValueBold: Boolean = false,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isValueBold) FontWeight.Bold else FontWeight.Medium,
                    color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
fun SystemInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun ProfileDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

private fun String.capitalize() = this.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
}

package com.example.jourdroid.ui.app.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.data.CashBankBalanceItem
import com.example.jourdroid.data.UserData
import com.example.jourdroid.ui.component.CompactPageTitle
import com.example.jourdroid.ui.component.NotificationBadge
import com.example.jourdroid.ui.component.ProfileAvatar
import com.example.jourdroid.utils.DateUtils
import com.example.jourdroid.utils.FormatterUtils.formatRupiah
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: UserData,
    hasCheckedIn: Boolean,
    unreadNotificationCount: Int = 0,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAttendance: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var balanceData by remember { mutableStateOf<CashBankBalanceItem?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val userWarehouseId = user.warehouseId ?: 0
    val userRole = user.role?.toString() ?: ""
    val isAdmin = listOf("Administrator", "Super Admin").any { it.equals(userRole, ignoreCase = true) }

    val pagerState = rememberPagerState(pageCount = { 2 })

    val refreshData = suspend {
        try {
            isLoading = true
            errorMessage = null
            val apiService = ApiClient.getApiService(context)

            val today = DateUtils.getTodayJakartaFormat()

            val response = apiService.getCashBankBalance(
                warehouse = userWarehouseId,
                endDate = today
            )
            if (response.success) {
                balanceData = response.data
            } else {
                errorMessage = response.message
            }
        } catch (e: Exception) {
            errorMessage = "Gagal memuat data: ${e.localizedMessage}"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.surface
        )
    )

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
                        CompactPageTitle(user = user, title = "Dashboard")
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
            floatingActionButton = {
                if (!isAdmin && !hasCheckedIn) {
                    ExtendedFloatingActionButton(
                        onClick = onNavigateToAttendance,
                        icon = { Icon(Icons.Default.Fingerprint, null) },
                        text = { Text("Check-in") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { scope.launch { refreshData() } },
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Hero Summary Section (Finance App Vibe)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "Cash",
                            amount = balanceData?.sumtotalCash ?: 0L,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary,
                            icon = Icons.Default.Wallet
                        )
                        SummaryCard(
                            title = "Bank",
                            amount = balanceData?.sumtotalBank ?: 0L,
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF0EA5E9),
                            icon = Icons.Default.AccountBalance
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Tab Switching Section
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            if (pagerState.currentPage < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = pagerState.currentPage == 0,
                            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                            text = { Text("Balance", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = pagerState.currentPage == 1,
                            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                            text = { Text("Report", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.PieChart, null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Content Pager - Using a Box to fix height issues in vertical scroll
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 500.dp)) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.Top
                        ) { page ->
                            when (page) {
                                0 -> {
                                    if (balanceData != null) {
                                        CashBankBalance(accounts = balanceData!!.chartOfAccounts)
                                    } else if (!isLoading) {
                                        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                            Text("No balance data", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                                1 -> {
                                    ReportContent(user = user)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: Long,
    modifier: Modifier = Modifier,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = color
                    )
                }
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = formatRupiah(amount),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

package com.example.jourdroid.ui.app.pos

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBoxValue.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.data.*
import com.example.jourdroid.ui.component.PrintPosReceiptDialog
import com.example.jourdroid.ui.component.SlideUpModal
import com.example.jourdroid.utils.FormatterUtils.formatRupiah
import kotlinx.coroutines.launch

data class CartEntry(
    val quantity: Int,
    val customPrice: Long? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    user: UserData,
    onBack: () -> Unit,
    onSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiService = remember { ApiClient.getApiService(context) }

    var products by remember { mutableStateOf<List<ProductItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf("all", "Voucher & SP", "Accessories", "Kabel Data", "Charger", "Earphone")
    var selectedCategory by remember { mutableStateOf("Voucher & SP") }

    // Cart State: Map of Product ID to CartEntry
    var cartMap by remember { mutableStateOf<Map<Int, CartEntry>>(emptyMap()) }
    var isCartModalOpen by remember { mutableStateOf(false) }
    
    // Edit Price State
    var editingProduct by remember { mutableStateOf<Pair<ProductItem, CartEntry>?>(null) }
    
    // Checkout Result
    var salesResult by remember { mutableStateOf<SalesData?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val userWarehouseId = user.warehouseId ?: 1
    val userWarehouseName = user.warehouse?.name ?: "Store"

    val refreshProducts = suspend {
        try {
            isLoading = true
            errorMessage = null
            val response = apiService.getAllProducts()
            if (response.success) {
                products = response.products
            } else {
                errorMessage = response.message
            }
        } catch (e: Exception) {
            errorMessage = "Gagal memuat produk: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshProducts()
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { product ->
            val matchesSearch = if (searchQuery.isBlank()) true 
                               else product.name.contains(searchQuery, ignoreCase = true) || product.code?.contains(searchQuery, ignoreCase = true) == true
            val matchesCategory = if (selectedCategory == "all") true 
                                 else product.category?.equals(selectedCategory, ignoreCase = true) == true
            matchesSearch && matchesCategory
        }
    }

    val totalAmount = remember(cartMap, products) {
        cartMap.entries.sumOf { (id, entry) ->
            val product = products.find { it.id == id }
            val unitPrice = entry.customPrice ?: product?.priceLong ?: 0L
            unitPrice * entry.quantity
        }
    }

    val totalItems = remember(cartMap) { cartMap.values.sumOf { it.quantity } }

    val handleCheckout = {
        scope.launch {
            try {
                isSubmitting = true
                val cartItems = cartMap.filter { it.value.quantity > 0 }.map { (id, entry) ->
                    val product = products.find { it.id == id }!!
                    val unitPrice = entry.customPrice ?: product.priceLong
                    CartItem(productId = id, quantity = entry.quantity, price = unitPrice)
                }

                if (cartItems.isEmpty()) {
                    Toast.makeText(context, "Keranjang kosong", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val request = TransactionRequest(
                    cart = cartItems,
                    transactionType = "Sales",
                    warehouseId = userWarehouseId
                )

                val response = apiService.submitTransaction(request)
                if (response.success) {
                    // Reset Cart
                    val finalItems = cartMap.filter { it.value.quantity > 0 }.map { (id, entry) ->
                        val product = products.find { it.id == id }!!
                        val unitPrice = entry.customPrice ?: product.priceLong
                        SalesItem(
                            id = id,
                            productName = product.name,
                            quantity = entry.quantity,
                            price = unitPrice,
                            subtotal = unitPrice * entry.quantity
                        )
                    }

                    salesResult = response.data ?: SalesData(
                        id = 0,
                        invoice = response.invoice,
                        dateIssued = "Baru Saja",
                        amount = totalAmount,
                        status = 1,
                        items = finalItems
                    )

                    cartMap = emptyMap()
                    isCartModalOpen = false
                    onSuccess() // 🟢 Trigger refresh in background
                    Toast.makeText(context, "Transaksi Berhasil!", Toast.LENGTH_SHORT).show()
                } else {
                    val msg = response.message ?: "Server error"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
                } catch (e: Exception) {
                    val errorMsg = if (e.localizedMessage?.contains("500") == true) 
                        "Kesalahan Server (500). Hubungi Admin."
                    else "Checkout Gagal: ${e.localizedMessage}"
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                } finally {
                isSubmitting = false
            }
        }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), MaterialTheme.colorScheme.surface)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Point of Sales", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(gradient)) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    placeholder = { Text("Cari produk atau barcode...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) } },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Category Filter
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { 
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ) 
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = Color.Transparent,
                                borderWidth = 1.dp
                            )
                        )
                    }
                }

                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { scope.launch { refreshProducts() } },
                    modifier = Modifier.weight(1f)
                ) {
                    if (errorMessage != null && products.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                                Button(onClick = { scope.launch { refreshProducts() } }) { Text("Coba Lagi") }
                            }
                        }
                    } else if (filteredProducts.isEmpty() && !isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Produk tidak ditemukan", color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(150.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredProducts) { product ->
                                ProductCard(
                                    product = product,
                                    quantity = cartMap[product.id]?.quantity ?: 0,
                                    onAdd = { 
                                        val current = cartMap[product.id]
                                        cartMap = cartMap.toMutableMap().apply { 
                                            put(product.id, current?.copy(quantity = current.quantity + 1) ?: CartEntry(1)) 
                                        } 
                                    },
                                    onRemove = {
                                        val current = cartMap[product.id] ?: return@ProductCard
                                        cartMap = cartMap.toMutableMap().apply {
                                            if (current.quantity == 1) remove(product.id) 
                                            else put(product.id, current.copy(quantity = current.quantity - 1))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (cartMap.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 12.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                            .clickable { isCartModalOpen = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        BadgedBox(
                            badge = { 
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ) { Text(totalItems.toString()) } 
                            }
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart, 
                                null, 
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "Total Pembayaran", 
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), 
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                formatRupiah(totalAmount), 
                                color = MaterialTheme.colorScheme.onPrimary, 
                                fontWeight = FontWeight.ExtraBold, 
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Cart Details Modal
    SlideUpModal(
        visible = isCartModalOpen,
        title = "Keranjang Belanja",
        onClose = { isCartModalOpen = false }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val cartItems = cartMap.filter { it.value.quantity > 0 }.map { (id, entry) ->
                val product = products.find { it.id == id }!!
                product to entry
            }

            if (cartItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Keranjang Anda kosong")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${cartItems.size} Produk terpilih", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { cartMap = emptyMap(); isCartModalOpen = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear all")
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    cartItems.forEach { (product, entry) ->
                        CartRow(
                            product = product, 
                            entry = entry,
                            onQuantityChange = { newQty ->
                                cartMap = cartMap.toMutableMap().apply {
                                    put(product.id, entry.copy(quantity = newQty))
                                }
                            },
                            onRemove = { 
                                cartMap = cartMap.toMutableMap().apply { remove(product.id) }
                                if (cartMap.isEmpty()) isCartModalOpen = false
                            },
                            onEditPrice = { editingProduct = product to entry }
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp), 
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Pembayaran", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(formatRupiah(totalAmount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { handleCheckout() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("Bayar Sekarang", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Edit Price Dialog
    if (editingProduct != null) {
        val (product, entry) = editingProduct!!
        var tempPrice by remember { mutableStateOf((entry.customPrice ?: product.priceLong).toString()) }
        
        AlertDialog(
            onDismissRequest = { editingProduct = null },
            title = { 
                Text(
                    "Ubah Harga Manual", 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempPrice,
                        onValueChange = { if (it.all { char -> char.isDigit() }) tempPrice = it },
                        label = { Text("Harga Baru") },
                        prefix = { Text("Rp ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Harga default: ${formatRupiah(product.priceLong)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPrice = tempPrice.toLongOrNull()
                        cartMap = cartMap.toMutableMap().apply {
                            put(product.id, entry.copy(customPrice = if (newPrice == product.priceLong) null else newPrice))
                        }
                        editingProduct = null
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Update Harga")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingProduct = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Receipt Dialog
    if (salesResult != null) {
        PrintPosReceiptDialog(
            salesData = salesResult!!,
            agentName = user.name ?: "Admin",
            warehouseName = userWarehouseName,
            onDismiss = { salesResult = null }
        )
    }
}

@Composable
fun ProductCard(
    product: ProductItem,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Product Icon / Image Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(product.category?.lowercase()) {
                        "charger" -> Icons.Default.BatteryChargingFull
                        "earphone" -> Icons.Default.Headset
                        "kabel data" -> Icons.Default.Usb
                        "accessories" -> Icons.Default.Extension
                        else -> Icons.Default.Inventory2
                    },
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                
                // Stock Badge (Overlay)
                Surface(
                    color = if (product.stock > 0) Color(0xFF4CAF50).copy(alpha = 0.9f) else MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 0.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "Stok: ${product.stock}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatRupiah(product.priceLong),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))
            
            if (quantity == 0) {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tambah", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Text(quantity.toString(), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyLarge)
                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CartRow(
    product: ProductItem,
    entry: CartEntry,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    onEditPrice: () -> Unit
) {
    val unitPrice = entry.customPrice ?: product.priceLong

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Product Icon (Modern Square)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(product.category?.lowercase()) {
                        "charger" -> Icons.Default.BatteryChargingFull
                        "earphone" -> Icons.Default.Headset
                        "kabel data" -> Icons.Default.Usb
                        "accessories" -> Icons.Default.Extension
                        else -> Icons.Default.Inventory2
                    },
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 2. Info & Adjuster Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = formatRupiah(unitPrice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Modern +/- Adjuster
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = { if (entry.quantity > 1) onQuantityChange(entry.quantity - 1) else onRemove() },
                        modifier = Modifier
                            .size(32.dp) // 👈 Diperbesar sedikit dari 14.dp agar proporsional
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp), // 👈 Ukuran icon yang pas untuk tombol 28.dp
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = entry.quantity.toString(),
                        style = MaterialTheme.typography.titleMedium, // 👈 Dibuat sedikit lebih tegas
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { onQuantityChange(entry.quantity + 1) },
                        modifier = Modifier
                            .size(32.dp) // 👈 Samakan ukuran 28.dp
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 3. Subtotal & Quick Actions
            Column(horizontalAlignment = Alignment.End) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close, 
                        null, 
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = formatRupiah(unitPrice * entry.quantity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (entry.customPrice != null) {
                    Text(
                        "Custom Price",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.clickable { onEditPrice() }
                    )
                } else {
                    TextButton(
                        onClick = onEditPrice,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(20.dp)
                    ) {
                        Text("Edit Price", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

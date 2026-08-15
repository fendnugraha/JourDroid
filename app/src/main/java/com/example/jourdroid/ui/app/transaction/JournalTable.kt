package com.example.jourdroid.ui.app.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jourdroid.data.JournalData
import com.example.jourdroid.utils.FormatterUtils.formatNumberWithDots
import com.example.jourdroid.utils.FormatterUtils.formatShortDate
import com.example.jourdroid.utils.FormatterUtils.formatTimeOnly

@Composable
fun JournalTable(
    journals: List<JournalData>,
    warehouseCashId: Int,
    warehouseId: Int
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(journals.size) { index ->
            val journal = journals[index]
            JournalItem(
                journal = journal,
                warehouseCashId = warehouseCashId,
                warehouseId = warehouseId
            )
        }
    }
}

@Composable
fun JournalItem(
    journal: JournalData,
    warehouseCashId: Int,
    warehouseId: Int
) {
    // Logic matching JS: isInflow means debt_id is the warehouse cash account
    val isInflow = journal.effectiveDebtId == warehouseCashId
    // Web logic: !isInflow -> Green/Plus, isInflow -> Red/Minus
    val isPositive = !isInflow
    
    val categoryIcon = when {
        journal.trxType?.contains("Sales", ignoreCase = true) == true -> Icons.Default.ShoppingCart
        journal.trxType?.contains("Transfer", ignoreCase = true) == true -> Icons.Default.SwapHoriz
        journal.trxType?.contains("Expense", ignoreCase = true) == true -> Icons.Default.Payments
        journal.trxType == "Mutasi Kas" -> Icons.Default.SwapHoriz
        else -> Icons.Default.ReceiptLong
    }

    val iconBgColor = when {
        isPositive -> Color(0xFFE8F5E9)
        else -> Color(0xFFFFEBEE)
    }
    
    val iconTintColor = when {
        isPositive -> Color(0xFF2E7D32)
        else -> Color(0xFFC62828)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Middle Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = journal.description ?: "Transaction #${journal.id}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.1.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = journal.trxType ?: "Journal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = "${formatTimeOnly(journal.dateIssued)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                
                // Advanced Settle Channel Display (Matching JS logic)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (journal.trxType == "Mutasi Kas") {
                        val credGroup = journal.cred?.accountGroup ?: "Unknown"
                        val debtGroup = journal.debt?.accountGroup ?: "Unknown"
                        
                        val credWhName = journal.cred?.warehouse?.name?.replace(Regex("(?i)^konter\\s*"), "") ?: ""
                        val debtWhName = journal.debt?.warehouse?.name?.replace(Regex("(?i)^konter\\s*"), "") ?: ""
                        
                        val displayCred = if (journal.cred?.warehouseId != warehouseId && credWhName.isNotEmpty()) {
                            "$credGroup ($credWhName)"
                        } else credGroup
                        
                        val displayDebt = if (journal.debt?.warehouseId != warehouseId && debtWhName.isNotEmpty()) {
                            "$debtGroup ($debtWhName)"
                        } else debtGroup

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "$displayCred → $displayDebt",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (credGroup != debtGroup) {
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.Warning, null, modifier = Modifier.size(12.dp), tint = Color(0xFFF59E0B))
                            }
                        }
                    } else {
                        // Regular transaction: Show the account that is NOT the warehouse cash
                        val settleAccount = if (journal.effectiveCredId == warehouseCashId) {
                            journal.debt?.accountGroup ?: "Internal"
                        } else {
                            journal.cred?.accountGroup ?: "Internal"
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CreditCard, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = settleAccount,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Right Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isPositive) "+" else "-") + " " + formatNumberWithDots(journal.amount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                )
                
                if (journal.feeAmount > 0) {
                    Text(
                        text = "Fee: ${formatNumberWithDots(journal.feeAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

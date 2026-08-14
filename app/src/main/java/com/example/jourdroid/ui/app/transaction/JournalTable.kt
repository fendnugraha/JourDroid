package com.example.jourdroid.ui.app.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    onJournalClick: (JournalData) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(journals.size) { index ->
            val journal = journals[index]
            JournalItem(journal, onClick = { onJournalClick(journal) })
        }
    }
}

@Composable
fun JournalItem(journal: JournalData, onClick: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = journal.description ?: "No Description",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${formatShortDate(journal.dateIssued)} ${formatTimeOnly(journal.dateIssued)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Hapus") }, onClick = { onClick(); showMenu = false })
                    }
                }
            }

            // Badges
            Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Type badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Tag, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(journal.trxType ?: "Uncategorized", style = MaterialTheme.typography.labelSmall)
                }

                // Channel badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Tag, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = (journal.cred?.accountGroup ?: "Cash") + " → " + (journal.debt?.accountGroup ?: "Cash"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nominal", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text(
                        text = "+ " + formatNumberWithDots(journal.amount),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (journal.feeAmount > 0) {
                        Text(
                            text = "Fee: " + formatNumberWithDots(journal.feeAmount),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}


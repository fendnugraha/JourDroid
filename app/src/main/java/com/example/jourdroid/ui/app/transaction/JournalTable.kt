package com.example.jourdroid.ui.app.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jourdroid.data.JournalData
import com.example.jourdroid.utils.FormatterUtils.formatRupiah
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
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "ID ${journal.id}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = formatTimeOnly(journal.dateIssued),
                    style = MaterialTheme.typography.bodySmall
                )
            }
//            Text(modifier = Modifier.padding(top = 4.dp), text = if (journal.status == 0) "On Delivery" else "Delivered", style = MaterialTheme.typography.bodySmall)
            // 🟢 Menggunakan Triple(A, B, C) untuk menampung 3 data
            val (statusIcon, statusText, statusColor) = if (journal.status == 0) {
                Triple(Icons.Default.PedalBike, "On Delivery", MaterialTheme.colorScheme.primary)
            } else {
                Triple(Icons.Default.CheckCircle, "Delivered", MaterialTheme.colorScheme.secondary)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = statusColor // 💡 Bagus juga kalau warna ikon ikut statusColor supaya serasi!
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall, // ⚠️ Pastikan tambahkan koma di sini
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
//                    modifier = Modifier.padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warehouse,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = journal.debt?.warehouse?.name ?: "Cabang",
                        style = MaterialTheme.typography.bodySmall
                        // 🔴 Hapus Modifier.weight di sini
                    )
                }

                    Text(
                        text = formatRupiah(journal.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

            }
        }
    }
}


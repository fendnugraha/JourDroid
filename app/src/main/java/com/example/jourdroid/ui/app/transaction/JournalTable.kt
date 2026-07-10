package com.example.jourdroid.ui.app.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
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


@Composable
fun JournalTable(
    journals: List<JournalData>,
    onJournalClick: (JournalData) -> Unit
) {
    val filteredJournals = journals.filter { it.trxType == "Mutasi Kas" && it.cred?.warehouseId == 1 && it.cred.accountId == 1 }
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(filteredJournals.size) { index ->
            val journal = filteredJournals[index]
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
                    text = "No. Journal: ${journal.id}, Status: ${if (journal.status == 0) "On Delivery" else "Delivered"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Tanggal: ${journal.dateIssued}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom // 🟢 Tetap "items-end" untuk menyamakan rata bawah
            ) {
                // 🟢 Column ini mengambil sisa ruang horizontal di kiri (Flex: 1)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Invoice: ${journal.invoice}",
                        style = MaterialTheme.typography.bodyMedium
                        // 🔴 Hapus Modifier.weight di sini
                    )
                    Text(
                        text = "Tujuan: ${journal.debt?.warehouse?.name ?: "Cabang"}",
                        style = MaterialTheme.typography.bodyMedium
                        // 🔴 Hapus Modifier.weight di sini
                    )
                }

                // 🟢 Teks Rupiah akan otomatis mepet ke kanan secara rapi
                Text(
                    text = formatRupiah(journal.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }
    }
}


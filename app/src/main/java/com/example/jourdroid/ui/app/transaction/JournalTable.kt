package com.example.jourdroid.ui.app.transaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.jourdroid.data.JournalData
import com.example.jourdroid.utils.FormatterUtils.formatRupiah


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
            Text(text = "Journal ID: ${journal.id}", style = MaterialTheme.typography.titleSmall)
            Text(text = "Date: ${journal.dateIssued}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Amount: Rp ${formatRupiah(journal.amount)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}


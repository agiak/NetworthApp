package com.agcoding.networkapp.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.shared.ui.model.EntryUiModel
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

@Composable
fun RecentEntriesSection(
    entries: List<EntryUiModel>,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.section_recent_entries),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onShowAll) {
                Text(text = stringResource(R.string.btn_show_all))
                Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null)
            }
        }
        entries.forEach { entry ->
            EntryHistoryItem(entry = entry)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecentEntriesSectionPreview() {
    NetWorthTheme {
        RecentEntriesSection(
            entries = listOf(
                EntryUiModel(1L, "20 Mar 2026", "€16,500"),
                EntryUiModel(2L, "15 Feb 2026", "€15,800"),
                EntryUiModel(3L, "15 Jan 2026", "€15,100")
            ),
            onShowAll = {}
        )
    }
}

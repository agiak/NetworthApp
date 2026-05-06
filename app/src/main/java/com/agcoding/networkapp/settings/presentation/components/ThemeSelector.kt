package com.agcoding.networkapp.settings.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.settings.domain.model.AppTheme
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

@Composable
fun ThemeSelector(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            AppTheme.entries.forEach { theme ->
                val label = when (theme) {
                    AppTheme.DARK -> stringResource(R.string.theme_dark)
                    AppTheme.LIGHT -> stringResource(R.string.theme_light)
                    AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.RadioButton) { onThemeSelected(theme) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedTheme == theme, onClick = { onThemeSelected(theme) })
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ThemeSelectorPreview() {
    NetWorthTheme {
        ThemeSelector(selectedTheme = AppTheme.SYSTEM, onThemeSelected = {})
    }
}

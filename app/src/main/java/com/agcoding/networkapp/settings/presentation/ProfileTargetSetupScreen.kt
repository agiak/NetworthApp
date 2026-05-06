package com.agcoding.networkapp.settings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.shared.ui.theme.DarkBackground
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun ProfileTargetSetupScreen(
    onComplete: () -> Unit,
    viewModel: ProfileTargetSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onComplete()
    }

    ProfileTargetSetupContent(
        uiState = uiState,
        onTargetChange = viewModel::onTargetChange,
        onSave = viewModel::onSave
    )
}

@Composable
private fun ProfileTargetSetupContent(
    uiState: ProfileTargetSetupUiState,
    onTargetChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = stringResource(R.string.title_setup_target),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.desc_setup_target),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(48.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.label_target_amount).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = uiState.target,
                    onValueChange = onTargetChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = PositiveGreen) },
                    placeholder = { Text("100000", color = Color.LightGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PositiveGreen,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                        cursorColor = PositiveGreen
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkBackground,
                    contentColor = Color.White
                ),
                enabled = uiState.target.isNotBlank() && !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = stringResource(R.string.btn_continue),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileTargetSetupPreview() {
    NetWorthTheme {
        ProfileTargetSetupContent(
            uiState = ProfileTargetSetupUiState(target = "100000"),
            onTargetChange = {},
            onSave = {}
        )
    }
}

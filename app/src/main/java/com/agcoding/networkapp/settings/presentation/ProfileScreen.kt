package com.agcoding.networkapp.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isSetup: Boolean,
    onComplete: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onComplete()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!isSetup) {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_edit_profile), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { onBack?.invoke() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSetup) {
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = stringResource(R.string.title_setup_profile),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.desc_setup_profile),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(48.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(PositiveGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.name.take(1).uppercase(),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            ProfileTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.label_name),
                icon = Icons.Default.Person,
                placeholder = "John Doe"
            )

            Spacer(modifier = Modifier.height(20.dp))

            ProfileTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = stringResource(R.string.label_email),
                icon = Icons.Default.Email,
                placeholder = "john@example.com",
                keyboardType = KeyboardType.Email
            )

            if (!isSetup) {
                Spacer(modifier = Modifier.height(20.dp))
                ProfileTextField(
                    value = uiState.target,
                    onValueChange = viewModel::onTargetChange,
                    label = stringResource(R.string.label_target_amount),
                    icon = Icons.Default.Star,
                    placeholder = "100000",
                    keyboardType = KeyboardType.Number
                )
                if (uiState.trackingSince.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    TrackingSinceRow(date = uiState.trackingSince)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.onSave(isSetup) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                ),
                enabled = uiState.name.isNotBlank() && !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (isSetup) stringResource(R.string.btn_continue) else stringResource(R.string.btn_save),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(icon, contentDescription = null, tint = PositiveGreen) },
            placeholder = { Text(placeholder, color = Color.LightGray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PositiveGreen,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                cursorColor = PositiveGreen
            )
        )
    }
}

@Composable
private fun TrackingSinceRow(date: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.label_tracking_since_profile).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = date,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray) },
            readOnly = true,
            enabled = false,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = Color.LightGray.copy(alpha = 0.3f),
                disabledLeadingIconColor = Color.Gray,
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
    }
}

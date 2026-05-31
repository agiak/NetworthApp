package com.agcoding.networkapp.account.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme
import com.agcoding.networkapp.shared.ui.utils.formatWithSeparator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateAccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalAppColorScheme.current

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onNavigateBack()
    }

    if (uiState.isCreated) {
        AccountCreatedContent(
            accountName     = uiState.name.trim(),
            startingBalance = uiState.startingBalance,
            colorHex        = uiState.colorHex,
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_create_account), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.onIntent(CreateAccountIntent.UpdateName(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label_account_name)) },
                    placeholder = { Text(stringResource(R.string.hint_account_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = colors.actionPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        cursorColor          = colors.actionPrimary,
                    ),
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.startingBalance,
                    onValueChange = { viewModel.onIntent(CreateAccountIntent.UpdateStartingBalance(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label_starting_balance)) },
                    placeholder = { Text("0") },
                    leadingIcon = {
                        Text(
                            text = "€",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.actionPrimary,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = colors.actionPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        cursorColor          = colors.actionPrimary,
                    ),
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.label_account_color),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Account.PRESET_COLORS.forEach { hex ->
                        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { colors.actionPrimary }
                        val isSelected = hex == uiState.colorHex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { viewModel.onIntent(CreateAccountIntent.SelectColor(hex)) }
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick  = { viewModel.onIntent(CreateAccountIntent.Create) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled  = uiState.canCreate && !uiState.isCreating,
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = colors.actionPrimary,
                        contentColor           = colors.actionContent,
                        disabledContainerColor = colors.actionPrimary.copy(alpha = 0.38f),
                        disabledContentColor   = colors.actionContent.copy(alpha = 0.38f),
                    ),
                ) {
                    if (uiState.isCreating) {
                        CircularProgressIndicator(color = colors.actionContent, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.btn_create_account), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AccountCreatedContent(
    accountName: String,
    startingBalance: String,
    colorHex: String,
) {
    val colors = LocalAppColorScheme.current
    val accentColor = remember(colorHex) {
        try { Color(android.graphics.Color.parseColor(colorHex)) }
        catch (e: Exception) { android.graphics.Color.parseColor("#7DD3A0").let { Color(it) } }
    }

    val circleScale   = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }
    val contentAlpha  = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            // nwPop: 0 → overshoot 1.12 at 60% → settle 1.0
            circleScale.animateTo(
                targetValue   = 1f,
                animationSpec = keyframes {
                    durationMillis = 450
                    0f    at 0   with LinearEasing
                    1.12f at 270 with LinearEasing
                    1f    at 450
                }
            )
        }
        kotlinx.coroutines.delay(180)
        checkProgress.animateTo(
            targetValue   = 1f,
            animationSpec = tween(durationMillis = 520, easing = LinearOutSlowInEasing)
        )
        contentAlpha.animateTo(
            targetValue   = 1f,
            animationSpec = tween(durationMillis = 350)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundPrimary),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Animated circle + checkmark
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(140.dp)) {
                scale(circleScale.value) {
                    drawCircle(color = accentColor)
                }
            }
            androidx.compose.foundation.Canvas(modifier = Modifier.size(140.dp)) {
                val strokeWidth = 10.dp.toPx()
                val checkPath = Path().apply {
                    val w = size.width; val h = size.height
                    moveTo(w * 0.27f, h * 0.51f)
                    lineTo(w * 0.44f, h * 0.68f)
                    lineTo(w * 0.73f, h * 0.34f)
                }
                val measurer = PathMeasure()
                measurer.setPath(checkPath, false)
                val animatedPath = Path()
                measurer.getSegment(0f, measurer.length * checkProgress.value, animatedPath, true)
                drawPath(
                    path  = animatedPath,
                    color = Color.White,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Column(
            modifier = Modifier.alpha(contentAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text      = stringResource(R.string.label_account_created_upper),
                style     = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
                color     = colors.statusSuccess,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = accountName,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = colors.contentPrimary,
            )
            Spacer(Modifier.height(6.dp))
            val formattedBalance = if (startingBalance.isEmpty()) "€0"
                                   else "€${formatWithSeparator(startingBalance)}"
            Text(
                text = stringResource(R.string.label_starting_at, formattedBalance),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.contentSecondary,
            )
        }
    }
}

package com.lyx.sms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.lyx.sms.SmsUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SmsUiState,
    biometricAvailable: Boolean,
    snackbarHostState: SnackbarHostState,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onSave: (
        serverUrl: String,
        token: String,
        biometricEnabled: Boolean
    ) -> Unit,
    onPurgeAll: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var serverUrl by rememberSaveable(uiState.settings.serverUrl) { mutableStateOf(uiState.settings.serverUrl) }
    var token by rememberSaveable(uiState.settings.token) { mutableStateOf(uiState.settings.token) }
    var biometricEnabled by rememberSaveable(uiState.settings.biometricEnabled) { mutableStateOf(uiState.settings.biometricEnabled) }
    var showTokenPlain by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onPurgeAll()
                    }
                ) {
                    Text("\u786e\u8ba4\u5220\u9664")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("\u53d6\u6d88")
                }
            },
            title = { Text("\u5220\u9664\u5168\u90e8\u4fe1\u606f") },
            text = { Text("\u8fd9\u4f1a\u5220\u9664\u670d\u52a1\u5668\u4e0a\u7684\u5168\u90e8\u77ed\u4fe1\u8bb0\u5f55\u3002") }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(if (uiState.settings.isConfigured) "\u670d\u52a1\u5668\u8bbe\u7f6e" else "\u9996\u6b21\u8bbe\u7f6e")
                    },
                    navigationIcon = {
                        if (canGoBack) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "\u8fd4\u56de"
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "\u8fde\u63a5\u77ed\u4fe1\u670d\u52a1\u5668",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "\u53ef\u586b\u5199\u57df\u540d\u3001\u9879\u76ee\u8def\u5f84\u6216\u5b8c\u6574\u63a5\u53e3\u5730\u5740\u3002",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = serverUrl,
                                onValueChange = { serverUrl = it },
                                label = { Text("\u670d\u52a1\u5668\u5730\u5740") },
                                placeholder = { Text("\u8bf7\u8f93\u5165\u57df\u540d\u6216\u63a5\u53e3\u5730\u5740") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = token,
                                onValueChange = { token = it },
                                label = { Text("\u8bbf\u95ee\u4ee4\u724c") },
                                placeholder = { Text("\u8bf7\u8f93\u5165\u8bbf\u95ee\u4ee4\u724c") },
                                singleLine = true,
                                visualTransformation = if (showTokenPlain) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showTokenPlain = !showTokenPlain }) {
                                        Icon(
                                            imageVector = if (showTokenPlain) {
                                                Icons.Outlined.VisibilityOff
                                            } else {
                                                Icons.Outlined.Visibility
                                            },
                                            contentDescription = if (showTokenPlain) {
                                                "\u9690\u85cf\u4ee4\u724c"
                                            } else {
                                                "\u663e\u793a\u4ee4\u724c"
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "\u5e94\u7528\u5b89\u5168",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            SettingSwitchRow(
                                title = "\u542f\u7528\u542f\u52a8\u65f6\u7cfb\u7edf\u9a8c\u8bc1",
                                description = when {
                                    biometricAvailable -> "\u6253\u5f00\u5e94\u7528\u65f6\uff0c\u4f7f\u7528\u6307\u7eb9\u3001\u9762\u5bb9\u6216\u9501\u5c4f\u5bc6\u7801\u5b8c\u6210\u9a8c\u8bc1"
                                    biometricEnabled -> "\u5f53\u524d\u8bbe\u5907\u6682\u65f6\u65e0\u6cd5\u4f7f\u7528\u7cfb\u7edf\u9a8c\u8bc1\uff0c\u53ef\u5148\u5173\u95ed\u6b64\u9009\u9879"
                                    else -> "\u5f53\u524d\u8bbe\u5907\u672a\u542f\u7528\u53ef\u7528\u7684\u7cfb\u7edf\u9a8c\u8bc1"
                                },
                                checked = biometricEnabled,
                                onCheckedChange = { biometricEnabled = it },
                                enabled = biometricAvailable || biometricEnabled,
                                icon = Icons.Outlined.Fingerprint
                            )
                        }
                    }
                }

                if (uiState.settings.isConfigured) {
                    item {
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.88f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "\u5371\u9669\u64cd\u4f5c",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "\u8c03\u7528\u63a5\u53e3\u5220\u9664\u670d\u52a1\u5668\u4e0a\u7684\u5168\u90e8\u77ed\u4fe1\u8bb0\u5f55\u3002",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedButton(
                                    onClick = { showDeleteDialog = true },
                                    enabled = !uiState.isPurging
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteSweep,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(if (uiState.isPurging) "\u5220\u9664\u4e2d..." else "\u5220\u9664\u5168\u90e8\u4fe1\u606f")
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            val trimmedServer = serverUrl.trim()
                            val trimmedToken = token.trim()

                            val validationMessage = when {
                                trimmedServer.isBlank() -> "\u8bf7\u8f93\u5165\u670d\u52a1\u5668\u5730\u5740\u3002"
                                trimmedToken.isBlank() -> "\u8bf7\u8f93\u5165\u8bbf\u95ee\u4ee4\u724c\u3002"
                                else -> null
                            }

                            if (validationMessage != null) {
                                scope.launch { snackbarHostState.showSnackbar(validationMessage) }
                            } else {
                                onSave(trimmedServer, trimmedToken, biometricEnabled)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Save,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(if (uiState.settings.isConfigured) "\u4fdd\u5b58\u8bbe\u7f6e" else "\u4fdd\u5b58\u5e76\u7ee7\u7eed")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

package com.lyx.sms.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyx.sms.SmsUiState
import com.lyx.sms.model.SmsMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    uiState: SmsUiState,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    val background = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.82f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("\u6211\u7684\u4fe1\u606f", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = uiState.lastSyncedLabel.ifBlank { "\u5df2\u663e\u793a\u6700\u65b030\u6761\u4fe1\u606f" },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onRefresh) {
                            if (uiState.isLoadingMessages) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "\u5237\u65b0"
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "\u8bbe\u7f6e"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            when {
                uiState.visibleMessages.isEmpty() && uiState.isLoadingMessages -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.visibleMessages.isEmpty() -> {
                    EmptyMessageState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 14.dp,
                            bottom = 28.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.visibleMessages,
                            key = { _, item -> item.id }
                        ) { index, message ->
                            val previous = uiState.visibleMessages.getOrNull(index - 1)
                            val showDayDivider = previous == null || previous.dayLabel != message.dayLabel
                            MessageBubble(
                                message = message,
                                showDayDivider = showDayDivider
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.canLoadOlder,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 72.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 6.dp,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "\u62d6\u52a8\u5230\u9876\u90e8\u52a0\u8f7d\u66f4\u65e930\u6761",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: SmsMessage,
    showDayDivider: Boolean
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val linkColor = MaterialTheme.colorScheme.primary
    val annotatedContent = remember(message.content, linkColor) {
        buildMessageAnnotatedString(message.content, linkColor)
    }
    val verificationCode = remember(message.content) {
        extractVerificationCode(message.content)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        if (showDayDivider) {
            Text(
                text = message.dayLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
        ) {
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text(message.displayDevice) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Sms,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            Text(
                text = message.displayPhone,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = message.fullDateTimeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(0.88f),
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomEnd = 22.dp,
                bottomStart = 8.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                ClickableText(
                    text = annotatedContent,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                    ),
                    onClick = { offset ->
                        val annotations = annotatedContent.getStringAnnotations(
                            start = offset,
                            end = offset
                        )
                        if (annotations.isNotEmpty()) {
                            val annotation = annotations.first()
                            when (annotation.tag) {
                                "url" -> openUrl(context, annotation.item)
                                "phone" -> openDialer(context, annotation.item)
                            }
                        }
                    }
                )

                if (verificationCode != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(
                                androidx.compose.ui.text.buildAnnotatedString {
                                    append(verificationCode)
                                }
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("\u590d\u5236\u9a8c\u8bc1\u7801 $verificationCode")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMessageState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Sms,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "\u6682\u65e0\u4fe1\u606f",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "\u4fdd\u5b58\u670d\u52a1\u5668\u8bbe\u7f6e\u540e\u53ef\u5237\u65b0\u67e5\u770b\u3002",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "\u6700\u65b0\u6d88\u606f\u663e\u793a\u5728\u5e95\u90e8\uff0c\u62d6\u5230\u9876\u90e8\u53ef\u7ee7\u7eed\u7ffb\u9875\u3002",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun openUrl(context: Context, rawUrl: String) {
    val normalized = if (rawUrl.startsWith("http://", ignoreCase = true) ||
        rawUrl.startsWith("https://", ignoreCase = true)
    ) {
        rawUrl
    } else {
        "http://$rawUrl"
    }

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching { context.startActivity(intent) }
}

private fun openDialer(context: Context, rawPhone: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$rawPhone")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching { context.startActivity(intent) }
}

package com.lyx.sms.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lyx.sms.MessageListScrollEvent
import com.lyx.sms.SmsAppViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Composable
fun SmsAppContent(
    viewModel: SmsAppViewModel,
    biometricAvailable: Boolean,
    requestBiometric: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val currentVisibleCount by rememberUpdatedState(uiState.visibleMessages.size)
    val currentShouldLockOnBackground = rememberUpdatedState(
        uiState.isSettingsLoaded && uiState.settings.requiresUnlock
    )
    val lifecycleOwner = LocalLifecycleOwner.current
    var showSettings by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.isSettingsLoaded, uiState.settings.isConfigured) {
        if (uiState.isSettingsLoaded) {
            showSettings = !uiState.settings.isConfigured
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && currentShouldLockOnBackground.value) {
                viewModel.lockApp()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.scrollEvents.collect { event ->
            withFrameNanos { }
            when (event) {
                MessageListScrollEvent.ToBottom -> {
                    if (currentVisibleCount > 0) {
                        listState.scrollToItem(currentVisibleCount - 1)
                    }
                }

                is MessageListScrollEvent.RestoreAfterPrepend -> {
                    listState.scrollToItem(
                        index = event.addedCount,
                        scrollOffset = event.scrollOffset
                    )
                }
            }
        }
    }

    LaunchedEffect(listState, uiState.canLoadOlder) {
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.isScrollInProgress
            )
        }.map { (index, offset, isScrolling) ->
            isScrolling && index == 0 && offset == 0
        }.distinctUntilChanged()
            .filter { it && uiState.canLoadOlder }
            .collect {
                viewModel.loadOlderPage(listState.firstVisibleItemScrollOffset)
            }
    }

    if (!uiState.isSettingsLoaded) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val requiresSetup = !uiState.settings.isConfigured
    val requiresUnlock = !requiresSetup && !uiState.isUnlocked

    if (!requiresSetup && showSettings) {
        BackHandler { showSettings = false }
    }

    when {
        requiresSetup || showSettings -> SettingsScreen(
            uiState = uiState,
            biometricAvailable = biometricAvailable,
            snackbarHostState = snackbarHostState,
            canGoBack = !requiresSetup,
            onBack = { showSettings = false },
            onSave = { serverUrl, token, biometricEnabled ->
                viewModel.saveSettings(
                    serverUrl = serverUrl,
                    token = token,
                    biometricEnabled = biometricEnabled
                ) {
                    showSettings = false
                }
            },
            onPurgeAll = { viewModel.purgeAllMessages() }
        )

        requiresUnlock -> UnlockScreen(
            uiState = uiState,
            biometricAvailable = biometricAvailable,
            snackbarHostState = snackbarHostState,
            onUnlockWithBiometric = {
                requestBiometric(
                    { viewModel.unlockWithBiometric() },
                    { viewModel.showMessage(it) }
                )
            }
        )

        else -> MessageListScreen(
            uiState = uiState,
            listState = listState,
            snackbarHostState = snackbarHostState,
            onOpenSettings = { showSettings = true },
            onRefresh = { viewModel.refreshMessages(resetToLatest = true) }
        )
    }
}

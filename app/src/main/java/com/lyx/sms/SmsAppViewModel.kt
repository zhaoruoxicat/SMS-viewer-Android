package com.lyx.sms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lyx.sms.data.AppSettingsRepository
import com.lyx.sms.data.SmsApiClient
import com.lyx.sms.model.AppSettings
import com.lyx.sms.model.SmsMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SmsAppViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = AppSettingsRepository(application)
    private val apiClient = SmsApiClient()

    private val _settings = MutableStateFlow<AppSettings?>(null)
    private val _allMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    private val _loadedPages = MutableStateFlow(1)
    private val _isUnlocked = MutableStateFlow(true)
    private val _isLoadingMessages = MutableStateFlow(false)
    private val _isPurging = MutableStateFlow(false)
    private val _lastSyncedLabel = MutableStateFlow("")

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    private val _scrollEvents = MutableSharedFlow<MessageListScrollEvent>(extraBufferCapacity = 4)
    val scrollEvents = _scrollEvents.asSharedFlow()

    private var lastLoadedConfigKey: String? = null
    private var lockInitialized = false

    val settings: StateFlow<AppSettings?> = _settings.asStateFlow()

    private val baseUiState = combine(
        settings,
        _allMessages,
        _loadedPages,
        _isUnlocked
    ) { settings, allMessages, loadedPages, isUnlocked ->
        val visibleCount = (loadedPages * PAGE_SIZE).coerceAtMost(allMessages.size)
        val visibleMessages = if (visibleCount == 0) emptyList() else allMessages.takeLast(visibleCount)
        val resolvedSettings = settings ?: AppSettings()

        SmsUiState(
            settings = resolvedSettings,
            allMessages = allMessages,
            visibleMessages = visibleMessages,
            canLoadOlder = visibleMessages.size < allMessages.size,
            isUnlocked = isUnlocked,
            isSettingsLoaded = settings != null
        )
    }

    val uiState: StateFlow<SmsUiState> = combine(
        baseUiState,
        _isLoadingMessages,
        _isPurging,
        _lastSyncedLabel
    ) { baseState, isLoadingMessages, isPurging, lastSyncedLabel ->
        baseState.copy(
            isLoadingMessages = isLoadingMessages,
            isPurging = isPurging,
            lastSyncedLabel = lastSyncedLabel
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SmsUiState()
    )

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { currentSettings ->
                _settings.value = currentSettings

                if (!lockInitialized) {
                    _isUnlocked.value = !currentSettings.requiresUnlock
                    lockInitialized = true
                } else if (!currentSettings.requiresUnlock) {
                    _isUnlocked.value = true
                }

                if (!currentSettings.isConfigured) {
                    lastLoadedConfigKey = null
                    _allMessages.value = emptyList()
                    _loadedPages.value = 1
                    _lastSyncedLabel.value = ""
                    return@collect
                }

                if (_isUnlocked.value && currentSettings.configKey != lastLoadedConfigKey) {
                    refreshMessages(resetToLatest = true)
                }
            }
        }
    }

    fun refreshMessages(resetToLatest: Boolean = true) {
        val currentSettings = settings.value ?: return
        if (!currentSettings.isConfigured) {
            _snackbarMessages.tryEmit("\u8bf7\u5148\u8bbe\u7f6e\u670d\u52a1\u5668\u5730\u5740\u548c\u8bbf\u95ee\u4ee4\u724c\u3002")
            return
        }
        if (!_isUnlocked.value || _isLoadingMessages.value) return

        viewModelScope.launch {
            _isLoadingMessages.value = true
            try {
                val messages = apiClient.fetchMessages(
                    serverUrl = currentSettings.serverUrl,
                    token = currentSettings.token
                ).sortedWith(compareBy<SmsMessage>({ it.receivedAtMillis }, { it.id }))

                _allMessages.value = messages
                _loadedPages.value = 1
                _lastSyncedLabel.value = "\u540c\u6b65\u4e8e ${currentTimestampLabel()}"
                lastLoadedConfigKey = currentSettings.configKey

                if (resetToLatest && messages.isNotEmpty()) {
                    _scrollEvents.tryEmit(MessageListScrollEvent.ToBottom)
                }
            } catch (error: Exception) {
                _snackbarMessages.emit(error.message ?: "\u52a0\u8f7d\u4fe1\u606f\u5931\u8d25\u3002")
            } finally {
                _isLoadingMessages.value = false
            }
        }
    }

    fun loadOlderPage(firstVisibleItemScrollOffset: Int) {
        val currentMessages = _allMessages.value
        val currentlyVisible = (_loadedPages.value * PAGE_SIZE).coerceAtMost(currentMessages.size)
        if (currentlyVisible >= currentMessages.size) return

        val nextVisibleCount = (currentlyVisible + PAGE_SIZE).coerceAtMost(currentMessages.size)
        val addedCount = nextVisibleCount - currentlyVisible
        if (addedCount <= 0) return

        _loadedPages.value = ((nextVisibleCount - 1) / PAGE_SIZE) + 1
        _scrollEvents.tryEmit(
            MessageListScrollEvent.RestoreAfterPrepend(
                addedCount = addedCount,
                scrollOffset = firstVisibleItemScrollOffset
            )
        )
    }

    fun saveSettings(
        serverUrl: String,
        token: String,
        biometricEnabled: Boolean,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentSettings = settings.value ?: AppSettings()
            val updated = currentSettings.copy(
                serverUrl = serverUrl.trim(),
                token = token.trim(),
                biometricEnabled = biometricEnabled
            )

            settingsRepository.saveSettings(updated)
            _isUnlocked.value = true
            _snackbarMessages.emit("\u8bbe\u7f6e\u5df2\u4fdd\u5b58\u3002")
            onSaved()
        }
    }

    fun unlockWithBiometric() {
        _isUnlocked.value = true
        _snackbarMessages.tryEmit("\u5df2\u901a\u8fc7\u7cfb\u7edf\u9a8c\u8bc1\u3002")
        refreshMessages(resetToLatest = true)
    }

    fun lockApp() {
        val currentSettings = settings.value ?: return
        if (currentSettings.requiresUnlock) {
            _isUnlocked.value = false
        }
    }

    fun purgeAllMessages() {
        val currentSettings = settings.value ?: return
        if (!currentSettings.isConfigured) {
            _snackbarMessages.tryEmit("\u8bf7\u5148\u5b8c\u6210\u670d\u52a1\u5668\u8bbe\u7f6e\u3002")
            return
        }
        if (_isPurging.value) return

        viewModelScope.launch {
            _isPurging.value = true
            try {
                val result = apiClient.purgeAll(
                    serverUrl = currentSettings.serverUrl,
                    token = currentSettings.token
                )
                _allMessages.value = emptyList()
                _loadedPages.value = 1
                _lastSyncedLabel.value = "\u670d\u52a1\u5668\u5df2\u6e05\u7a7a ${result.beforeTotal} \u6761\u4fe1\u606f"
                _snackbarMessages.emit("\u5df2\u5220\u9664\u5168\u90e8\u4fe1\u606f\u3002")
            } catch (error: Exception) {
                _snackbarMessages.emit(error.message ?: "\u5220\u9664\u5168\u90e8\u4fe1\u606f\u5931\u8d25\u3002")
            } finally {
                _isPurging.value = false
            }
        }
    }

    fun showMessage(message: String) {
        _snackbarMessages.tryEmit(message)
    }

    private fun currentTimestampLabel(): String = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))

    companion object {
        const val PAGE_SIZE: Int = 30
    }
}

data class SmsUiState(
    val settings: AppSettings = AppSettings(),
    val allMessages: List<SmsMessage> = emptyList(),
    val visibleMessages: List<SmsMessage> = emptyList(),
    val canLoadOlder: Boolean = false,
    val isUnlocked: Boolean = true,
    val isSettingsLoaded: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val isPurging: Boolean = false,
    val lastSyncedLabel: String = ""
)

sealed interface MessageListScrollEvent {
    data object ToBottom : MessageListScrollEvent

    data class RestoreAfterPrepend(
        val addedCount: Int,
        val scrollOffset: Int
    ) : MessageListScrollEvent
}

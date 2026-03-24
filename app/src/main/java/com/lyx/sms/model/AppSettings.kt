package com.lyx.sms.model

data class AppSettings(
    val serverUrl: String = "",
    val token: String = "",
    val biometricEnabled: Boolean = false
) {
    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && token.isNotBlank()

    val requiresUnlock: Boolean
        get() = biometricEnabled

    val configKey: String
        get() = "${serverUrl.trim()}|${token.trim()}"
}

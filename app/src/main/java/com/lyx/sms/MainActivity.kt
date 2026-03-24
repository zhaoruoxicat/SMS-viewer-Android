package com.lyx.sms

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lyx.sms.ui.SmsAppContent
import com.lyx.sms.ui.theme.MySmsTheme

class MainActivity : AppCompatActivity() {
    private val systemAuthenticators: Int =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MySmsTheme {
                val viewModel = viewModel<SmsAppViewModel>()
                val biometricAvailable = remember { isBiometricAvailable() }

                SmsAppContent(
                    viewModel = viewModel,
                    biometricAvailable = biometricAvailable,
                    requestBiometric = { onSuccess, onError ->
                        requestBiometricAuth(onSuccess, onError)
                    }
                )
            }
        }
    }

    private fun isBiometricAvailable(): Boolean {
        return when (BiometricManager.from(this).canAuthenticate(systemAuthenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS,
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> true
            else -> false
        }
    }

    private fun requestBiometricAuth(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isBiometricAvailable()) {
            onError("\u5f53\u524d\u8bbe\u5907\u6682\u65f6\u65e0\u6cd5\u4f7f\u7528\u7cfb\u7edf\u9a8c\u8bc1\u3002")
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    onError("\u7cfb\u7edf\u9a8c\u8bc1\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5\u3002")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_name))
            .setSubtitle("\u9a8c\u8bc1\u8eab\u4efd\u540e\u8fdb\u5165\u5e94\u7528")
            .setAllowedAuthenticators(systemAuthenticators)
            .build()

        prompt.authenticate(promptInfo)
    }
}

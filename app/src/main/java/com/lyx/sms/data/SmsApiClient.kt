package com.lyx.sms.data

import com.lyx.sms.model.PurgeResult
import com.lyx.sms.model.SmsMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SmsApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()
) {
    suspend fun fetchMessages(serverUrl: String, token: String): List<SmsMessage> = withContext(Dispatchers.IO) {
        val trimmedToken = token.trim()
        val request = Request.Builder()
            .url(buildApiUrl(serverUrl, "list", trimmedToken))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $trimmedToken")
            .header("X-API-Token", trimmedToken)
            .header("X-Token", trimmedToken)
            .get()
            .build()

        val root = execute(request)
        val records = root.optJSONArray("records") ?: JSONArray()

        buildList {
            for (index in 0 until records.length()) {
                val item = records.optJSONObject(index) ?: continue
                add(
                    SmsMessage(
                        id = item.optLong("id", index.toLong()),
                        phone = item.optString("phone"),
                        content = item.optString("content"),
                        receivedAt = item.optString("received_at"),
                        device = item.optString("device")
                    )
                )
            }
        }
    }

    suspend fun purgeAll(serverUrl: String, token: String): PurgeResult = withContext(Dispatchers.IO) {
        val trimmedToken = token.trim()
        val request = Request.Builder()
            .url(buildApiUrl(serverUrl, "purge_all", trimmedToken))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $trimmedToken")
            .header("X-API-Token", trimmedToken)
            .header("X-Token", trimmedToken)
            .post(ByteArray(0).toRequestBody())
            .build()

        val root = execute(request)
        PurgeResult(
            beforeTotal = root.optInt("before_total", 0),
            remainingTotal = root.optInt("remaining_total", 0),
            deleteMode = root.optString("delete_mode", "UNKNOWN")
        )
    }

    private fun buildApiUrl(serverUrl: String, action: String, token: String): HttpUrl {
        val normalized = normalizeServerUrl(serverUrl)
        val url = normalized.toHttpUrlOrNull()
            ?: throw IOException("\u670d\u52a1\u5668\u5730\u5740\u683c\u5f0f\u4e0d\u6b63\u786e\uff0c\u8bf7\u586b\u5199\u5b8c\u6574\u57df\u540d\u6216\u9879\u76ee\u8def\u5f84\u3002")

        return url.newBuilder()
            .setQueryParameter("action", action)
            .setQueryParameter("token", token)
            .build()
    }

    private fun normalizeServerUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            throw IOException("\u670d\u52a1\u5668\u5730\u5740\u4e0d\u80fd\u4e3a\u7a7a\u3002")
        }

        val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            val scheme = if (looksLikeLocalAddress(trimmed)) "http://" else "https://"
            scheme + trimmed
        }

        return when {
            withScheme.endsWith(".php", ignoreCase = true) -> withScheme
            withScheme.endsWith("/appapi/index.php", ignoreCase = true) -> withScheme
            withScheme.endsWith("/appapi/", ignoreCase = true) -> withScheme + "index.php"
            withScheme.endsWith("/appapi", ignoreCase = true) -> "$withScheme/index.php"
            else -> withScheme.trimEnd('/') + "/appapi/index.php"
        }
    }

    private fun looksLikeLocalAddress(value: String): Boolean {
        val lower = value.lowercase()
        if (lower.startsWith("localhost")) return true
        if (lower.startsWith("10.") || lower.startsWith("192.168.") || lower.startsWith("127.")) return true
        if (Regex("^172\\.(1[6-9]|2\\d|3[0-1])\\.").containsMatchIn(lower)) return true
        return Regex("^\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d+)?(?:/.*)?$").matches(lower)
    }

    private fun execute(request: Request): JSONObject {
        client.newCall(request).execute().use { response ->
            val bodyText = response.body?.string().orEmpty()
            val payload = bodyText.toJsonObjectOrNull()

            if (!response.isSuccessful) {
                throw IOException(payload.extractMessage(response.code))
            }

            val json = payload ?: throw IOException("\u670d\u52a1\u5668\u8fd4\u56de\u4e86\u65e0\u6cd5\u8bc6\u522b\u7684\u6570\u636e\u3002")
            if (!json.optBoolean("success", true)) {
                throw IOException(json.extractMessage(response.code))
            }
            return json
        }
    }

    private fun String.toJsonObjectOrNull(): JSONObject? = runCatching {
        JSONObject(this)
    }.getOrNull()

    private fun JSONObject?.extractMessage(code: Int): String {
        if (this == null) return "\u8bf7\u6c42\u5931\u8d25\uff0c\u72b6\u6001\u7801 $code\u3002"

        val message = optString("message").ifBlank {
            optString("error").ifBlank { "\u8bf7\u6c42\u5931\u8d25\uff0c\u72b6\u6001\u7801 $code\u3002" }
        }
        return message
    }
}

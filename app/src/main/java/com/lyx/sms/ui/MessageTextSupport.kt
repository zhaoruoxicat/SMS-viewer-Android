package com.lyx.sms.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

private val clickablePattern = Regex(
    pattern = "((?:https?://|www\\.)[^\\s]+)|(?<!\\d)(1[3-9]\\d{9})(?!\\d)",
    options = setOf(RegexOption.IGNORE_CASE)
)

private val otpKeywordPattern = Regex("(验证码|\\bcode\\b)", RegexOption.IGNORE_CASE)
private val otpPattern = Regex("(?<!\\d)(\\d{4,8})(?!\\d)")

fun buildMessageAnnotatedString(
    content: String,
    linkColor: Color
): AnnotatedString = buildAnnotatedString {
    var cursor = 0

    clickablePattern.findAll(content).forEach { match ->
        if (match.range.first > cursor) {
            append(content.substring(cursor, match.range.first))
        }

        val matchedText = match.value
        val tag = if (matchedText.startsWith("http", ignoreCase = true) || matchedText.startsWith("www.", ignoreCase = true)) {
            "url"
        } else {
            "phone"
        }

        pushStringAnnotation(tag = tag, annotation = matchedText)
        pushStyle(
            SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.None,
                fontWeight = FontWeight.SemiBold
            )
        )
        append(matchedText)
        pop()
        pop()
        cursor = match.range.last + 1
    }

    if (cursor < content.length) {
        append(content.substring(cursor))
    }
}

fun extractVerificationCode(content: String): String? {
    if (!otpKeywordPattern.containsMatchIn(content)) return null
    return otpPattern.find(content)?.groupValues?.getOrNull(1)
}

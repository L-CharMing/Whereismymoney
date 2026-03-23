package com.example.whereismymoney.capture

import com.example.whereismymoney.data.model.CaptureCandidate
import com.example.whereismymoney.data.model.CaptureMode
import java.math.BigDecimal
import java.time.LocalDateTime

private val supportedPackages = setOf(
    "com.tencent.mm",
    "com.eg.android.AlipayGphone"
)

data class PaymentCaptureResult(
    val accepted: Boolean,
    val candidate: CaptureCandidate?,
    val reason: String
)

class PaymentCaptureManager {
    fun parseAccessibilityPayload(packageName: String, visibleTexts: List<String>): PaymentCaptureResult {
        if (packageName !in supportedPackages) {
            return PaymentCaptureResult(false, null, "Unsupported package: $packageName")
        }
        val joined = visibleTexts.joinToString(" ")
        val amount = amountPattern.find(joined)?.groupValues?.get(1)?.toBigDecimalOrNull()
            ?: return PaymentCaptureResult(false, null, "Amount not found")
        val merchant = visibleTexts.firstOrNull { it.length in 2..24 && it.none(Char::isDigit) } ?: "未知商户"
        val candidate = CaptureCandidate(
            title = if (packageName == "com.tencent.mm") "微信支付" else "支付宝支付",
            merchant = merchant,
            amount = amount,
            detectedAt = LocalDateTime.now(),
            rawText = joined,
            source = CaptureMode.ACCESSIBILITY
        )
        return PaymentCaptureResult(true, candidate, "Accessibility capture candidate built")
    }

    fun buildWirelessAdbHints(): List<String> {
        return listOf(
            "在 Android 11+ 打开开发者选项和无线调试。",
            "在同一局域网内，用 adb pair 和 adb connect 与手机配对。",
            "通过 adb shell dumpsys window / uiautomator dump 辅助观察支付成功页结构。",
            "不要依赖无线 ADB 作为长期后台能力，它更适合开发调试和高级用户模式。"
        )
    }

    companion object {
        private val amountPattern = Regex("(?:¥|￥)([0-9]+(?:\\.[0-9]{1,2})?)")
    }
}

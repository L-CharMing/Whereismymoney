package com.example.whereismymoney.capture

import com.example.whereismymoney.data.model.BillRecord
import com.example.whereismymoney.data.model.CaptureCandidate
import com.example.whereismymoney.data.model.CaptureMode
import com.example.whereismymoney.data.model.CaptureSettings
import java.time.Duration
import java.time.LocalDateTime

private val defaultSupportedPackages = setOf(
    "com.tencent.mm",
    "com.eg.android.AlipayGphone"
)

data class PaymentCaptureResult(
    val accepted: Boolean,
    val candidate: CaptureCandidate?,
    val reason: String
)

class PaymentCaptureManager {
    fun parseAccessibilityPayload(
        packageName: String,
        visibleTexts: List<String>,
        settings: CaptureSettings
    ): PaymentCaptureResult {
        val allowedPackages = settings.allowedPackageNames.ifEmpty { defaultSupportedPackages.toList() }.toSet()
        if (packageName !in allowedPackages) {
            return PaymentCaptureResult(false, null, "Package not in whitelist: $packageName")
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

    fun isDuplicate(candidate: CaptureCandidate, records: List<BillRecord>, settings: CaptureSettings): Boolean {
        val timeWindow = settings.dedupeWindowMinutes.coerceAtLeast(1).toLong()
        return records.any { record ->
            record.amount.compareTo(candidate.amount) == 0 &&
                record.merchant.equals(candidate.merchant, ignoreCase = true) &&
                Duration.between(record.occurredAt, candidate.detectedAt).abs() <= Duration.ofMinutes(timeWindow)
        }
    }

    fun buildWirelessAdbHints(): List<String> {
        return listOf(
            "默认以无障碍作为支付识别主链路。",
            "无线 ADB 仅作为高级功能开放给有能力的用户。",
            "在 Android 11+ 打开开发者选项和无线调试。",
            "在同一局域网内，用 adb pair 和 adb connect 与手机配对。",
            "通过 adb shell dumpsys window / uiautomator dump 辅助观察支付成功页结构。"
        )
    }

    companion object {
        private val amountPattern = Regex("(?:¥|￥)([0-9]+(?:\\.[0-9]{1,2})?)")
    }
}

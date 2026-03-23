package com.example.whereismymoney.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.whereismymoney.capture.PaymentCaptureManager

class PaymentAccessibilityService : AccessibilityService() {
    private val captureManager = PaymentCaptureManager()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        val root = rootInActiveWindow ?: return
        val visibleTexts = mutableListOf<String>()
        collectTexts(root, visibleTexts)
        captureManager.parseAccessibilityPayload(packageName, visibleTexts)
        // 下一步：
        // 1. 把 candidate 发送到 repository / view model 持久化层
        // 2. 做去重（金额 + 时间窗口 + 商户）
        // 3. 若命中 ASK_EVERY_TIME，则弹出本地确认 UI
    }

    override fun onInterrupt() = Unit

    private fun collectTexts(node: AccessibilityNodeInfo?, output: MutableList<String>) {
        if (node == null) return
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let(output::add)
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let(output::add)
        for (index in 0 until node.childCount) {
            collectTexts(node.getChild(index), output)
        }
    }
}

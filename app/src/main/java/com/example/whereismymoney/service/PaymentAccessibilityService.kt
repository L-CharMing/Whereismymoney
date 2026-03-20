package com.example.whereismymoney.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class PaymentAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Prototype hook:
        // 1. detect known payment success screens or transaction detail pages
        // 2. extract visible text and package name
        // 3. hand data to a repository / WorkManager pipeline for rule matching + AI classification
    }

    override fun onInterrupt() = Unit
}

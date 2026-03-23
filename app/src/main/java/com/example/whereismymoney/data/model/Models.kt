package com.example.whereismymoney.data.model

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

enum class CaptureMode {
    ACCESSIBILITY,
    ADB,
    MANUAL
}

enum class RecordRuleAction {
    ALWAYS_RECORD,
    IGNORE,
    ASK_EVERY_TIME
}

data class ExpenseCategory(
    val id: String,
    val name: String,
    val monthlyBudgetHint: BigDecimal? = null
)

data class BillingRule(
    val id: String,
    val matcher: String,
    val action: RecordRuleAction,
    val categoryId: String? = null,
    val notes: String = ""
)

data class BillRecord(
    val id: String,
    val title: String,
    val amount: BigDecimal,
    val merchant: String,
    val occurredAt: LocalDateTime,
    val source: CaptureMode,
    val categoryId: String?,
    val autoCaptured: Boolean,
    val needsReview: Boolean,
    val rawText: String
)

data class MonthlyCategorySummary(
    val month: YearMonth,
    val categoryName: String,
    val totalAmount: BigDecimal,
    val percentOfMonth: Float
)

data class CaptureSettings(
    val useAccessibilityService: Boolean,
    val useAdbBridge: Boolean,
    val enableNotificationMirror: Boolean,
    val aiEndpoint: String,
    val aiApiKeyPlaceholder: String,
    val reviewUnknownBills: Boolean,
    val allowedPackageNames: List<String>,
    val dedupeWindowMinutes: Int
)

data class CaptureCandidate(
    val title: String,
    val merchant: String,
    val amount: BigDecimal,
    val detectedAt: LocalDateTime,
    val rawText: String,
    val source: CaptureMode
)

data class MonthlyInsight(
    val month: YearMonth,
    val topCategory: String,
    val totalSpent: BigDecimal,
    val biggestExpenseDay: LocalDate,
    val suggestion: String
)

data class LedgerSnapshot(
    val categories: List<ExpenseCategory>,
    val rules: List<BillingRule>,
    val records: List<BillRecord>,
    val settings: CaptureSettings
)

package com.example.whereismymoney.data.repository

import com.example.whereismymoney.ai.StubBillClassifier
import com.example.whereismymoney.data.model.BillRecord
import com.example.whereismymoney.data.model.BillingRule
import com.example.whereismymoney.data.model.CaptureCandidate
import com.example.whereismymoney.data.model.CaptureMode
import com.example.whereismymoney.data.model.CaptureSettings
import com.example.whereismymoney.data.model.ExpenseCategory
import com.example.whereismymoney.data.model.LedgerSnapshot
import com.example.whereismymoney.data.model.MonthlyCategorySummary
import com.example.whereismymoney.data.model.MonthlyInsight
import com.example.whereismymoney.data.model.RecordRuleAction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class InMemoryLedgerRepository(
    val categories: List<ExpenseCategory>,
    val rules: List<BillingRule>,
    val records: List<BillRecord>,
    val settings: CaptureSettings
) {
    fun shouldRecord(candidate: CaptureCandidate): RecordRuleAction {
        val hit = rules.firstOrNull { rule ->
            candidate.merchant.contains(rule.matcher, ignoreCase = true) ||
                candidate.rawText.contains(rule.matcher, ignoreCase = true)
        }
        return hit?.action ?: if (settings.reviewUnknownBills) {
            RecordRuleAction.ASK_EVERY_TIME
        } else {
            RecordRuleAction.ALWAYS_RECORD
        }
    }

    fun classify(candidate: CaptureCandidate): BillRecord {
        val result = StubBillClassifier().classify(candidate)
        return BillRecord(
            id = UUID.randomUUID().toString(),
            title = result.suggestedTitle,
            amount = candidate.amount,
            merchant = candidate.merchant,
            occurredAt = candidate.detectedAt,
            source = candidate.source,
            categoryId = result.categoryId,
            autoCaptured = candidate.source != CaptureMode.MANUAL,
            needsReview = result.confidence < 0.7f,
            rawText = candidate.rawText
        )
    }

    fun addManualRecord(title: String, merchant: String, amount: BigDecimal, categoryId: String?): BillRecord {
        return BillRecord(
            id = UUID.randomUUID().toString(),
            title = title,
            amount = amount,
            merchant = merchant,
            occurredAt = LocalDateTime.now(),
            source = CaptureMode.MANUAL,
            categoryId = categoryId,
            autoCaptured = false,
            needsReview = false,
            rawText = "$merchant $title $amount"
        )
    }

    fun updateRecord(recordId: String, title: String, merchant: String, amount: BigDecimal, categoryId: String?): List<BillRecord> {
        return records.map { record ->
            if (record.id == recordId) {
                record.copy(
                    title = title.ifBlank { record.title },
                    merchant = merchant.ifBlank { record.merchant },
                    amount = amount,
                    categoryId = categoryId,
                    rawText = "$merchant $title $amount"
                )
            } else {
                record
            }
        }
    }

    fun monthlySummary(month: YearMonth): List<MonthlyCategorySummary> {
        val monthRecords = records.filter { YearMonth.from(it.occurredAt) == month }
        val total = monthRecords.fold(BigDecimal.ZERO) { acc, record -> acc + record.amount }
        return monthRecords
            .groupBy { it.categoryId ?: "other" }
            .map { (categoryId, grouped) ->
                val amount = grouped.fold(BigDecimal.ZERO) { acc, record -> acc + record.amount }
                MonthlyCategorySummary(
                    month = month,
                    categoryName = categories.firstOrNull { it.id == categoryId }?.name ?: "其他",
                    totalAmount = amount,
                    percentOfMonth = if (total.compareTo(BigDecimal.ZERO) == 0) 0f else amount
                        .divide(total, 4, RoundingMode.HALF_UP)
                        .toFloat()
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    fun monthlyInsight(month: YearMonth): MonthlyInsight {
        val monthRecords = records.filter { YearMonth.from(it.occurredAt) == month }
        val topCategory = monthlySummary(month).firstOrNull()?.categoryName ?: "暂无数据"
        val total = monthRecords.fold(BigDecimal.ZERO) { acc, record -> acc + record.amount }
        val biggestExpenseDay = monthRecords
            .groupBy { it.occurredAt.toLocalDate() }
            .maxByOrNull { (_, values) -> values.fold(BigDecimal.ZERO) { acc, record -> acc + record.amount } }
            ?.key ?: month.atDay(1)
        return MonthlyInsight(
            month = month,
            topCategory = topCategory,
            totalSpent = total,
            biggestExpenseDay = biggestExpenseDay,
            suggestion = "如果本月继续在 $topCategory 上高频支出，可以设置预算提醒或将固定商户加入自动规则。"
        )
    }

    fun toSnapshot(): LedgerSnapshot = LedgerSnapshot(categories, rules, records, settings)

    companion object {
        fun fromSnapshot(snapshot: LedgerSnapshot): InMemoryLedgerRepository {
            return InMemoryLedgerRepository(
                categories = snapshot.categories,
                rules = snapshot.rules,
                records = snapshot.records,
                settings = snapshot.settings
            )
        }

        fun demo(): LedgerSnapshot {
            val categories = listOf(
                ExpenseCategory("food", "餐饮"),
                ExpenseCategory("transport", "交通"),
                ExpenseCategory("groceries", "日用杂货"),
                ExpenseCategory("housing", "居住"),
                ExpenseCategory("other", "其他")
            )
            val rules = listOf(
                BillingRule("1", "Starbucks", RecordRuleAction.ALWAYS_RECORD, "food", "咖啡始终记录"),
                BillingRule("2", "公司报销", RecordRuleAction.IGNORE, null, "报销型付款不进个人账本"),
                BillingRule("3", "Transfer", RecordRuleAction.ASK_EVERY_TIME, null, "转账需二次确认")
            )
            val now = LocalDateTime.now()
            val records = listOf(
                BillRecord("1", "早餐", BigDecimal("18.00"), "便利店", now.minusDays(2), CaptureMode.ACCESSIBILITY, "food", true, false, "早餐付款成功"),
                BillRecord("2", "地铁", BigDecimal("6.00"), "Metro", now.minusDays(2), CaptureMode.ADB, "transport", true, false, "地铁乘车码支付"),
                BillRecord("3", "超市补货", BigDecimal("88.50"), "Supermarket", now.minusDays(1), CaptureMode.MANUAL, "groceries", false, false, "周末采购")
            )
            val settings = CaptureSettings(
                useAccessibilityService = true,
                useAdbBridge = false,
                enableNotificationMirror = true,
                aiEndpoint = "https://api.example.com/v1/classify",
                aiApiKeyPlaceholder = "Paste-your-secret-here",
                reviewUnknownBills = true
            )
            return LedgerSnapshot(categories, rules, records, settings)
        }
    }
}

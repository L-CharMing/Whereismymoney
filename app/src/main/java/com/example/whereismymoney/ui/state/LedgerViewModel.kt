package com.example.whereismymoney.ui.state

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.whereismymoney.capture.PaymentCaptureManager
import com.example.whereismymoney.data.local.JsonLedgerStorage
import com.example.whereismymoney.data.model.BillRecord
import com.example.whereismymoney.data.model.BillingRule
import com.example.whereismymoney.data.model.CaptureCandidate
import com.example.whereismymoney.data.model.CaptureSettings
import com.example.whereismymoney.data.model.ExpenseCategory
import com.example.whereismymoney.data.model.LedgerSnapshot
import com.example.whereismymoney.data.model.MonthlyCategorySummary
import com.example.whereismymoney.data.model.RecordRuleAction
import com.example.whereismymoney.data.repository.InMemoryLedgerRepository
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

class LedgerViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = JsonLedgerStorage(application)
    private val captureManager = PaymentCaptureManager()

    var uiState by mutableStateOf(LedgerUiState())
        private set

    init {
        val snapshot = storage.loadOrDefault(InMemoryLedgerRepository.demo())
        uiState = snapshot.toUiState(captureManager.buildWirelessAdbHints())
    }

    fun addManualRecord(title: String, merchant: String, amountInput: String, categoryId: String?) {
        val amount = amountInput.toBigDecimalOrNull() ?: return
        val repository = currentRepository()
        val record = repository.addManualRecord(
            title = title.ifBlank { merchant },
            merchant = merchant.ifBlank { "手动录入" },
            amount = amount,
            categoryId = categoryId
        )
        persist(currentSnapshot().copy(records = listOf(record) + uiState.allRecords))
    }

    fun updateRecord(recordId: String, title: String, merchant: String, amountInput: String, categoryId: String?) {
        val amount = amountInput.toBigDecimalOrNull() ?: return
        val updatedRecords = currentRepository().updateRecord(recordId, title, merchant, amount, categoryId)
        persist(currentSnapshot().copy(records = updatedRecords))
    }

    fun deleteRecord(recordId: String) {
        val updatedRecords = currentRepository().deleteRecord(recordId)
        persist(currentSnapshot().copy(records = updatedRecords))
    }

    fun addRule(matcher: String, action: RecordRuleAction, categoryId: String?, notes: String) {
        if (matcher.isBlank()) return
        val newRule = BillingRule(
            id = UUID.randomUUID().toString(),
            matcher = matcher,
            action = action,
            categoryId = categoryId,
            notes = notes
        )
        persist(currentSnapshot().copy(rules = listOf(newRule) + uiState.rules))
    }

    fun importCandidate(candidate: CaptureCandidate) {
        if (captureManager.isDuplicate(candidate, uiState.allRecords, uiState.settings)) {
            uiState = uiState.copy(exportMessage = "检测到重复账单，已跳过自动入账。")
            return
        }
        val repository = currentRepository()
        val action = repository.shouldRecord(candidate)
        when (action) {
            RecordRuleAction.IGNORE -> return
            RecordRuleAction.ASK_EVERY_TIME,
            RecordRuleAction.ALWAYS_RECORD -> {
                val record = repository.classify(candidate)
                persist(currentSnapshot().copy(records = listOf(record) + uiState.allRecords))
            }
        }
    }

    fun updateSettings(transform: (CaptureSettings) -> CaptureSettings) {
        persist(currentSnapshot().copy(settings = transform(uiState.settings)))
    }

    fun updateSearchQuery(query: String) {
        uiState = currentSnapshot().toUiState(
            wirelessAdbHints = captureManager.buildWirelessAdbHints(),
            selectedMonth = uiState.selectedMonth,
            searchQuery = query,
            exportMessage = uiState.exportMessage
        )
    }

    fun selectPreviousMonth() {
        uiState = currentSnapshot().toUiState(
            wirelessAdbHints = captureManager.buildWirelessAdbHints(),
            selectedMonth = uiState.selectedMonth.minusMonths(1),
            searchQuery = uiState.searchQuery,
            exportMessage = uiState.exportMessage
        )
    }

    fun selectNextMonth() {
        uiState = currentSnapshot().toUiState(
            wirelessAdbHints = captureManager.buildWirelessAdbHints(),
            selectedMonth = uiState.selectedMonth.plusMonths(1),
            searchQuery = uiState.searchQuery,
            exportMessage = uiState.exportMessage
        )
    }

    fun exportSelectedMonthCsv() {
        val exportDir = getApplication<Application>().filesDir.resolve("exports")
        exportDir.mkdirs()
        val fileName = "ledger-${uiState.selectedMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))}.csv"
        val target = exportDir.resolve(fileName)
        val csv = buildString {
            appendLine("id,title,merchant,amount,occurredAt,source,categoryId,autoCaptured,needsReview")
            uiState.filteredRecords.forEach { record ->
                appendLine(
                    listOf(
                        record.id.csvEscape(),
                        record.title.csvEscape(),
                        record.merchant.csvEscape(),
                        record.amount.toPlainString().csvEscape(),
                        record.occurredAt.toString().csvEscape(),
                        record.source.name.csvEscape(),
                        (record.categoryId ?: "").csvEscape(),
                        record.autoCaptured.toString().csvEscape(),
                        record.needsReview.toString().csvEscape()
                    ).joinToString(",")
                )
            }
        }
        target.writeText(csv)
        uiState = uiState.copy(exportMessage = "已导出 CSV：${target.absolutePath}")
    }

    fun clearExportMessage() {
        uiState = uiState.copy(exportMessage = null)
    }

    private fun persist(snapshot: LedgerSnapshot) {
        storage.save(snapshot)
        uiState = snapshot.toUiState(
            wirelessAdbHints = captureManager.buildWirelessAdbHints(),
            selectedMonth = uiState.selectedMonth,
            searchQuery = uiState.searchQuery,
            exportMessage = uiState.exportMessage
        )
    }

    private fun currentRepository(): InMemoryLedgerRepository = InMemoryLedgerRepository.fromSnapshot(currentSnapshot())

    private fun currentSnapshot(): LedgerSnapshot = LedgerSnapshot(
        categories = uiState.categories,
        rules = uiState.rules,
        records = uiState.allRecords,
        settings = uiState.settings
    )
}

data class LedgerUiState(
    val categories: List<ExpenseCategory> = emptyList(),
    val rules: List<BillingRule> = emptyList(),
    val allRecords: List<BillRecord> = emptyList(),
    val filteredRecords: List<BillRecord> = emptyList(),
    val settings: CaptureSettings = CaptureSettings(
        useAccessibilityService = true,
        useAdbBridge = false,
        enableNotificationMirror = true,
        aiEndpoint = "",
        aiApiKeyPlaceholder = "",
        reviewUnknownBills = true,
        allowedPackageNames = listOf("com.tencent.mm", "com.eg.android.AlipayGphone"),
        dedupeWindowMinutes = 2
    ),
    val selectedMonth: YearMonth = YearMonth.now(),
    val searchQuery: String = "",
    val thisMonthTotal: BigDecimal = BigDecimal.ZERO,
    val thisMonthTopCategory: String = "暂无数据",
    val thisMonthSuggestions: String = "",
    val monthlyBreakdown: List<MonthlyCategorySummary> = emptyList(),
    val wirelessAdbHints: List<String> = emptyList(),
    val exportMessage: String? = null
)

private fun LedgerSnapshot.toUiState(
    wirelessAdbHints: List<String>,
    selectedMonth: YearMonth = YearMonth.now(),
    searchQuery: String = "",
    exportMessage: String? = null
): LedgerUiState {
    val repository = InMemoryLedgerRepository.fromSnapshot(this)
    val insight = repository.monthlyInsight(selectedMonth)
    return LedgerUiState(
        categories = categories,
        rules = rules,
        allRecords = records.sortedByDescending { it.occurredAt },
        filteredRecords = repository.recordsForMonth(selectedMonth, searchQuery),
        settings = settings,
        selectedMonth = selectedMonth,
        searchQuery = searchQuery,
        thisMonthTotal = insight.totalSpent,
        thisMonthTopCategory = insight.topCategory,
        thisMonthSuggestions = insight.suggestion,
        monthlyBreakdown = repository.monthlySummary(selectedMonth),
        wirelessAdbHints = wirelessAdbHints,
        exportMessage = exportMessage
    )
}

private fun String.csvEscape(): String {
    val escaped = replace("\"", "\"\"")
    return "\"$escaped\""
}

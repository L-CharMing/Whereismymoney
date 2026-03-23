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
        persist(currentSnapshot().copy(records = listOf(record) + uiState.records))
    }

    fun updateRecord(recordId: String, title: String, merchant: String, amountInput: String, categoryId: String?) {
        val amount = amountInput.toBigDecimalOrNull() ?: return
        val updatedRecords = currentRepository().updateRecord(recordId, title, merchant, amount, categoryId)
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
        val repository = currentRepository()
        val action = repository.shouldRecord(candidate)
        when (action) {
            RecordRuleAction.IGNORE -> return
            RecordRuleAction.ASK_EVERY_TIME,
            RecordRuleAction.ALWAYS_RECORD -> {
                val record = repository.classify(candidate)
                persist(currentSnapshot().copy(records = listOf(record) + uiState.records))
            }
        }
    }

    fun updateSettings(transform: (CaptureSettings) -> CaptureSettings) {
        persist(currentSnapshot().copy(settings = transform(uiState.settings)))
    }

    private fun persist(snapshot: LedgerSnapshot) {
        storage.save(snapshot)
        uiState = snapshot.toUiState(captureManager.buildWirelessAdbHints())
    }

    private fun currentRepository(): InMemoryLedgerRepository = InMemoryLedgerRepository.fromSnapshot(currentSnapshot())

    private fun currentSnapshot(): LedgerSnapshot = LedgerSnapshot(
        categories = uiState.categories,
        rules = uiState.rules,
        records = uiState.records,
        settings = uiState.settings
    )
}

data class LedgerUiState(
    val categories: List<ExpenseCategory> = emptyList(),
    val rules: List<BillingRule> = emptyList(),
    val records: List<BillRecord> = emptyList(),
    val settings: CaptureSettings = CaptureSettings(
        useAccessibilityService = true,
        useAdbBridge = false,
        enableNotificationMirror = true,
        aiEndpoint = "",
        aiApiKeyPlaceholder = "",
        reviewUnknownBills = true
    ),
    val thisMonthTotal: BigDecimal = BigDecimal.ZERO,
    val thisMonthTopCategory: String = "暂无数据",
    val thisMonthSuggestions: String = "",
    val monthlyBreakdown: List<MonthlyCategorySummary> = emptyList(),
    val wirelessAdbHints: List<String> = emptyList()
)

private fun LedgerSnapshot.toUiState(wirelessAdbHints: List<String>): LedgerUiState {
    val repository = InMemoryLedgerRepository.fromSnapshot(this)
    val month = java.time.YearMonth.now()
    val insight = repository.monthlyInsight(month)
    return LedgerUiState(
        categories = categories,
        rules = rules,
        records = records.sortedByDescending { it.occurredAt },
        settings = settings,
        thisMonthTotal = insight.totalSpent,
        thisMonthTopCategory = insight.topCategory,
        thisMonthSuggestions = insight.suggestion,
        monthlyBreakdown = repository.monthlySummary(month),
        wirelessAdbHints = wirelessAdbHints
    )
}

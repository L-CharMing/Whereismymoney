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
import com.example.whereismymoney.data.model.ProductCostRecord
import com.example.whereismymoney.data.model.RecordRuleAction
import com.example.whereismymoney.data.repository.InMemoryLedgerRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
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

    fun addManualRecord(title: String, merchant: String, amountInput: String, categoryId: String?, occurredDateInput: String? = null) {
        val amount = amountInput.toBigDecimalOrNull() ?: return
        val repository = currentRepository()
        val occurredAt = occurredDateInput?.let { runCatching { LocalDate.parse(it).atStartOfDay() }.getOrNull() } ?: LocalDateTime.now()
        val record = repository.addManualRecord(
            title = title.ifBlank { "手动记录" },
            merchant = merchant.ifBlank { "" },
            amount = amount,
            categoryId = categoryId,
            occurredAt = occurredAt
        )
        persist(currentSnapshot().copy(records = listOf(record) + uiState.allRecords))
    }

    fun updateRecord(recordId: String, title: String, amountInput: String) {
        val amount = amountInput.toBigDecimalOrNull() ?: return
        val oldRecord = uiState.allRecords.firstOrNull { it.id == recordId } ?: return
        val updated = currentRepository().updateRecord(
            recordId = recordId,
            title = title,
            merchant = oldRecord.merchant,
            amount = amount,
            categoryId = oldRecord.categoryId
        )
        persist(currentSnapshot().copy(records = updated))
    }

    fun deleteRecord(recordId: String) {
        val updated = currentRepository().deleteRecord(recordId)
        persist(currentSnapshot().copy(records = updated))
    }

    fun addProductCost(name: String, totalPriceInput: String, purchaseDateInput: String) {
        val totalPrice = totalPriceInput.toBigDecimalOrNull() ?: return
        val purchaseDate = runCatching { LocalDate.parse(purchaseDateInput) }.getOrNull() ?: return
        if (name.isBlank()) return
        val record = ProductCostRecord(
            id = UUID.randomUUID().toString(),
            name = name,
            totalPrice = totalPrice,
            purchaseDate = purchaseDate,
            createdAt = LocalDateTime.now()
        )
        persist(currentSnapshot().copy(productCosts = listOf(record) + uiState.productCosts))
    }



    fun updateProductCost(recordId: String, name: String, totalPriceInput: String, purchaseDateInput: String) {
        val totalPrice = totalPriceInput.toBigDecimalOrNull() ?: return
        val purchaseDate = runCatching { LocalDate.parse(purchaseDateInput) }.getOrNull() ?: return
        val updated = uiState.productCosts.map { item ->
            if (item.id == recordId) item.copy(name = name.ifBlank { item.name }, totalPrice = totalPrice, purchaseDate = purchaseDate) else item
        }
        persist(currentSnapshot().copy(productCosts = updated))
    }

    fun deleteProductCost(recordId: String) {
        persist(currentSnapshot().copy(productCosts = uiState.productCosts.filterNot { it.id == recordId }))
    }

    fun importRecordToProduct(recordId: String) {
        val record = uiState.allRecords.firstOrNull { it.id == recordId } ?: return
        addProductCost(
            name = record.title,
            totalPriceInput = record.amount.toPlainString(),
            purchaseDateInput = record.occurredAt.toLocalDate().toString()
        )
    }

    fun addAiVoiceBill(voiceText: String) {
        if (uiState.settings.aiEndpoint.isBlank() || uiState.settings.aiApiKeyPlaceholder.isBlank()) {
            uiState = uiState.copy(exportMessage = "请先在设置页配置 AI Endpoint 和 Token。")
            return
        }
        val amount = Regex("([0-9]+(?:\\.[0-9]{1,2})?)").find(voiceText)?.groupValues?.get(1)?.toBigDecimalOrNull()
        val title = voiceText.replace(Regex("[0-9.]+"), "").trim().ifBlank { "AI语音记账" }
        if (amount == null) {
            uiState = uiState.copy(exportMessage = "AI语音记账示例：未识别到金额，请在文本里包含数字金额。")
            return
        }
        addManualRecord(title = title, merchant = "AI语音", amountInput = amount.toPlainString(), categoryId = null)
    }

    fun addRule(matcher: String, action: RecordRuleAction, categoryId: String?, notes: String) {
        if (matcher.isBlank()) return
        val newRule = BillingRule(UUID.randomUUID().toString(), matcher, action, categoryId, notes)
        persist(currentSnapshot().copy(rules = listOf(newRule) + uiState.rules))
    }

    fun importCandidate(candidate: CaptureCandidate) {
        if (captureManager.isDuplicate(candidate, uiState.allRecords, uiState.settings)) {
            uiState = uiState.copy(exportMessage = "检测到重复账单，已跳过自动入账。")
            return
        }
        val repository = currentRepository()
        when (repository.shouldRecord(candidate)) {
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
        uiState = currentSnapshot().toUiState(captureManager.buildWirelessAdbHints(), uiState.selectedMonth, query, uiState.exportMessage)
    }

    fun selectPreviousMonth() {
        uiState = currentSnapshot().toUiState(captureManager.buildWirelessAdbHints(), uiState.selectedMonth.minusMonths(1), uiState.searchQuery, uiState.exportMessage)
    }

    fun selectNextMonth() {
        uiState = currentSnapshot().toUiState(captureManager.buildWirelessAdbHints(), uiState.selectedMonth.plusMonths(1), uiState.searchQuery, uiState.exportMessage)
    }

    fun selectMonth(month: YearMonth) {
        uiState = currentSnapshot().toUiState(captureManager.buildWirelessAdbHints(), month, uiState.searchQuery, uiState.exportMessage)
    }

    fun exportSelectedMonthCsv() {
        val exportDir = getApplication<Application>().filesDir.resolve("exports").apply { mkdirs() }
        val fileName = "ledger-${uiState.selectedMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))}.csv"
        val target = exportDir.resolve(fileName)
        val csv = buildString {
            appendLine("id,title,amount,occurredAt,source")
            uiState.filteredRecords.forEach { record ->
                appendLine(listOf(record.id.csvEscape(), record.title.csvEscape(), record.amount.toPlainString().csvEscape(), record.occurredAt.toString().csvEscape(), record.source.name.csvEscape()).joinToString(","))
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
        uiState = snapshot.toUiState(captureManager.buildWirelessAdbHints(), uiState.selectedMonth, uiState.searchQuery, uiState.exportMessage)
    }

    private fun currentRepository(): InMemoryLedgerRepository = InMemoryLedgerRepository.fromSnapshot(currentSnapshot())

    private fun currentSnapshot(): LedgerSnapshot = LedgerSnapshot(
        categories = uiState.categories,
        rules = uiState.rules,
        records = uiState.allRecords,
        settings = uiState.settings,
        productCosts = uiState.productCosts
    )
}

data class LedgerUiState(
    val categories: List<ExpenseCategory> = emptyList(),
    val rules: List<BillingRule> = emptyList(),
    val allRecords: List<BillRecord> = emptyList(),
    val filteredRecords: List<BillRecord> = emptyList(),
    val productCosts: List<ProductCostRecord> = emptyList(),
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
        productCosts = productCosts.sortedByDescending { it.createdAt },
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

private fun String.csvEscape(): String = "\"${replace("\"", "\"\"")}\""

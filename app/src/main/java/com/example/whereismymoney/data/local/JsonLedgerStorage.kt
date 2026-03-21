package com.example.whereismymoney.data.local

import android.content.Context
import com.example.whereismymoney.data.model.BillRecord
import com.example.whereismymoney.data.model.BillingRule
import com.example.whereismymoney.data.model.CaptureMode
import com.example.whereismymoney.data.model.CaptureSettings
import com.example.whereismymoney.data.model.ExpenseCategory
import com.example.whereismymoney.data.model.LedgerSnapshot
import com.example.whereismymoney.data.model.RecordRuleAction
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDateTime

class JsonLedgerStorage(private val context: Context) {
    private val fileName = "ledger_snapshot.json"

    fun loadOrDefault(defaultSnapshot: LedgerSnapshot): LedgerSnapshot {
        val file = context.getFileStreamPath(fileName)
        if (!file.exists()) {
            save(defaultSnapshot)
            return defaultSnapshot
        }
        val content = file.readText()
        return runCatching { fromJson(JSONObject(content)) }.getOrElse {
            save(defaultSnapshot)
            defaultSnapshot
        }
    }

    fun save(snapshot: LedgerSnapshot) {
        context.openFileOutput(fileName, Context.MODE_PRIVATE).bufferedWriter().use { writer ->
            writer.write(toJson(snapshot).toString())
        }
    }

    private fun toJson(snapshot: LedgerSnapshot): JSONObject {
        return JSONObject()
            .put("categories", JSONArray().apply {
                snapshot.categories.forEach { category ->
                    put(
                        JSONObject()
                            .put("id", category.id)
                            .put("name", category.name)
                            .put("monthlyBudgetHint", category.monthlyBudgetHint?.toPlainString())
                    )
                }
            })
            .put("rules", JSONArray().apply {
                snapshot.rules.forEach { rule ->
                    put(
                        JSONObject()
                            .put("id", rule.id)
                            .put("matcher", rule.matcher)
                            .put("action", rule.action.name)
                            .put("categoryId", rule.categoryId)
                            .put("notes", rule.notes)
                    )
                }
            })
            .put("records", JSONArray().apply {
                snapshot.records.forEach { record ->
                    put(
                        JSONObject()
                            .put("id", record.id)
                            .put("title", record.title)
                            .put("amount", record.amount.toPlainString())
                            .put("merchant", record.merchant)
                            .put("occurredAt", record.occurredAt.toString())
                            .put("source", record.source.name)
                            .put("categoryId", record.categoryId)
                            .put("autoCaptured", record.autoCaptured)
                            .put("needsReview", record.needsReview)
                            .put("rawText", record.rawText)
                    )
                }
            })
            .put(
                "settings",
                JSONObject()
                    .put("useAccessibilityService", snapshot.settings.useAccessibilityService)
                    .put("useAdbBridge", snapshot.settings.useAdbBridge)
                    .put("enableNotificationMirror", snapshot.settings.enableNotificationMirror)
                    .put("aiEndpoint", snapshot.settings.aiEndpoint)
                    .put("aiApiKeyPlaceholder", snapshot.settings.aiApiKeyPlaceholder)
                    .put("reviewUnknownBills", snapshot.settings.reviewUnknownBills)
            )
    }

    private fun fromJson(root: JSONObject): LedgerSnapshot {
        val categories = root.getJSONArray("categories").toList { json ->
            ExpenseCategory(
                id = json.getString("id"),
                name = json.getString("name"),
                monthlyBudgetHint = json.optString("monthlyBudgetHint")
                    .takeIf { it.isNotBlank() && it != "null" }
                    ?.let(::BigDecimal)
            )
        }
        val rules = root.getJSONArray("rules").toList { json ->
            BillingRule(
                id = json.getString("id"),
                matcher = json.getString("matcher"),
                action = RecordRuleAction.valueOf(json.getString("action")),
                categoryId = json.optString("categoryId").takeIf { it.isNotBlank() && it != "null" },
                notes = json.optString("notes")
            )
        }
        val records = root.getJSONArray("records").toList { json ->
            BillRecord(
                id = json.getString("id"),
                title = json.getString("title"),
                amount = BigDecimal(json.getString("amount")),
                merchant = json.getString("merchant"),
                occurredAt = LocalDateTime.parse(json.getString("occurredAt")),
                source = CaptureMode.valueOf(json.getString("source")),
                categoryId = json.optString("categoryId").takeIf { it.isNotBlank() && it != "null" },
                autoCaptured = json.getBoolean("autoCaptured"),
                needsReview = json.getBoolean("needsReview"),
                rawText = json.optString("rawText")
            )
        }
        val settingsJson = root.getJSONObject("settings")
        val settings = CaptureSettings(
            useAccessibilityService = settingsJson.optBoolean("useAccessibilityService", true),
            useAdbBridge = settingsJson.optBoolean("useAdbBridge", false),
            enableNotificationMirror = settingsJson.optBoolean("enableNotificationMirror", true),
            aiEndpoint = settingsJson.optString("aiEndpoint"),
            aiApiKeyPlaceholder = settingsJson.optString("aiApiKeyPlaceholder"),
            reviewUnknownBills = settingsJson.optBoolean("reviewUnknownBills", true)
        )
        return LedgerSnapshot(categories, rules, records, settings)
    }

    private fun <T> JSONArray.toList(mapper: (JSONObject) -> T): List<T> {
        return buildList {
            for (index in 0 until length()) {
                add(mapper(getJSONObject(index)))
            }
        }
    }
}

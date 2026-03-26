package com.example.whereismymoney.ai

import com.example.whereismymoney.data.model.BillRecord
import com.example.whereismymoney.data.model.CaptureCandidate

interface BillClassifier {
    fun classify(candidate: CaptureCandidate): ClassificationResult
}

data class ClassificationResult(
    val categoryId: String?,
    val confidence: Float,
    val reason: String,
    val suggestedTitle: String
)

class StubBillClassifier : BillClassifier {
    override fun classify(candidate: CaptureCandidate): ClassificationResult {
        val normalized = candidate.rawText.lowercase()
        val categoryId = when {
            listOf("coffee", "restaurant", "奶茶", "早餐").any(normalized::contains) -> "food"
            listOf("metro", "taxi", "地铁", "滴滴").any(normalized::contains) -> "transport"
            listOf("supermarket", "超市", "grocery").any(normalized::contains) -> "groceries"
            else -> "other"
        }
        return ClassificationResult(
            categoryId = categoryId,
            confidence = if (categoryId == "other") 0.42f else 0.83f,
            reason = "Prototype classifier using local keyword rules; replace with remote LLM/API later.",
            suggestedTitle = candidate.title.ifBlank { candidate.merchant }
        )
    }
}

class RemoteClassifierConfig(
    val endpoint: String,
    val apiKey: String,
    val modelName: String
)

interface RemoteClassifierGateway {
    fun classify(candidate: CaptureCandidate, config: RemoteClassifierConfig): ClassificationResult
}

class RemoteClassifierGatewayStub : RemoteClassifierGateway {
    override fun classify(candidate: CaptureCandidate, config: RemoteClassifierConfig): ClassificationResult {
        return ClassificationResult(
            categoryId = null,
            confidence = 0f,
            reason = "Connect your own LLM endpoint at ${config.endpoint} using ${config.modelName}.",
            suggestedTitle = candidate.title
        )
    }
}

fun BillRecord.summaryLine(categoryName: String): String {
    return "$title · $merchant · $categoryName · ¥$amount"
}

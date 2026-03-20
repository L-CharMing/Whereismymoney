package com.example.whereismymoney.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.whereismymoney.data.model.CaptureCandidate
import com.example.whereismymoney.data.model.CaptureMode
import com.example.whereismymoney.data.repository.InMemoryLedgerRepository
import java.math.BigDecimal
import java.time.LocalDateTime

@Composable
fun CaptureScreen(repository: InMemoryLedgerRepository, paddingValues: PaddingValues) {
    val demoCandidate = CaptureCandidate(
        title = "",
        merchant = "Starbucks Reserve",
        amount = BigDecimal("36.00"),
        detectedAt = LocalDateTime.now(),
        rawText = "微信支付成功 Starbucks Reserve ¥36.00",
        source = CaptureMode.ACCESSIBILITY
    )
    val action = repository.shouldRecord(demoCandidate)
    val classified = repository.classify(demoCandidate)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("自动记账策略", style = MaterialTheme.typography.headlineSmall)
                Text("Android 15+，优先通过无障碍识别支付成功页；如果用户愿意，也可接 ADB/桌面桥接。")
                Text("当前演示命中动作：$action")
                Text("自动分类结果：${classified.categoryId ?: "未分类"}")
                Text("是否需要人工复核：${if (classified.needsReview) "需要" else "不需要"}")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("准备接入的采集链路", style = MaterialTheme.typography.titleMedium)
                Text("• 支付结果页文本抓取（无障碍）")
                Text("• 设备连接后的 ADB 日志 / UIAutomator 辅助")
                Text("• 用户手动补录")
                Text("• 保留通知镜像 / OCR 扩展点")
            }
        }
    }
}

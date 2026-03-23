package com.example.whereismymoney.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.whereismymoney.data.model.CaptureCandidate
import com.example.whereismymoney.data.model.CaptureMode
import com.example.whereismymoney.ui.state.LedgerUiState
import com.example.whereismymoney.ui.state.LedgerViewModel
import java.math.BigDecimal
import java.time.LocalDateTime

@Composable
fun CaptureScreen(state: LedgerUiState, paddingValues: PaddingValues, viewModel: LedgerViewModel) {
    val demoCandidate = CaptureCandidate(
        title = "",
        merchant = "Starbucks Reserve",
        amount = BigDecimal("36.00"),
        detectedAt = LocalDateTime.now(),
        rawText = "微信支付成功 Starbucks Reserve ¥36.00",
        source = CaptureMode.ACCESSIBILITY
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("自动记账链路", style = MaterialTheme.typography.headlineSmall)
                    Text("现实可行方案：优先无障碍抓取支付成功页；无线 ADB 更适合作为调试/高级模式辅助，而不是长期后台依赖。")
                    Text("无障碍：${if (state.settings.useAccessibilityService) "已启用" else "未启用"}")
                    Switch(
                        checked = state.settings.useAccessibilityService,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(useAccessibilityService = checked) }
                        }
                    )
                    Text("无线 ADB 辅助：${if (state.settings.useAdbBridge) "已启用" else "未启用"}")
                    Switch(
                        checked = state.settings.useAdbBridge,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(useAdbBridge = checked) }
                        }
                    )
                    Text("通知镜像：${if (state.settings.enableNotificationMirror) "已启用" else "未启用"}")
                    Switch(
                        checked = state.settings.enableNotificationMirror,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(enableNotificationMirror = checked) }
                        }
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("模拟导入支付账单", style = MaterialTheme.typography.titleMedium)
                    Text("点击后按当前规则把一条支付成功记录写入本地账本。")
                    OutlinedButton(onClick = { viewModel.importCandidate(demoCandidate) }, modifier = Modifier.fillMaxWidth()) {
                        Text("导入一条模拟支付账单")
                    }
                }
            }
        }
        item {
            Text("无线 ADB 使用建议", style = MaterialTheme.typography.titleMedium)
        }
        items(state.wirelessAdbHints) { hint ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(hint, modifier = Modifier.padding(16.dp))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("建议的自动记账流程", style = MaterialTheme.typography.titleMedium)
                    Text("1. 监听微信 / 支付宝支付结果页")
                    Text("2. 解析金额、商户、时间并本地去重")
                    Text("3. 按规则决定记录 / 忽略 / 询问")
                    Text("4. 仅把必要字段发送给大模型做分类（可选）")
                }
            }
        }
    }
}

package com.example.whereismymoney.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("自动记账链路", style = MaterialTheme.typography.headlineSmall)
                Text("本地账本默认离线保存；只有你接入大模型分类 API 时，才需要联网。")
                Text("无障碍：${if (state.settings.useAccessibilityService) "已启用" else "未启用"}")
                Switch(
                    checked = state.settings.useAccessibilityService,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(useAccessibilityService = checked) }
                    }
                )
                Text("ADB 辅助：${if (state.settings.useAdbBridge) "已启用" else "未启用"}")
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

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("模拟导入支付账单", style = MaterialTheme.typography.titleMedium)
                Text("点击后按当前规则把一条支付成功记录写入本地账本。")
                OutlinedButton(onClick = { viewModel.importCandidate(demoCandidate) }, modifier = Modifier.fillMaxWidth()) {
                    Text("导入一条模拟支付账单")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("后续实现建议", style = MaterialTheme.typography.titleMedium)
                Text("• 接 AccessibilityEvent 文本解析与包名白名单")
                Text("• 对同一笔支付做去重和延迟确认")
                Text("• 增加本地 OCR 作为无障碍备选")
                Text("• 联网仅用于大模型分类，不上传完整账本")
            }
        }
    }
}

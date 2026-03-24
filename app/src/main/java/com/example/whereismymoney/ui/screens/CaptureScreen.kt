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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var allowedPackagesInput by remember(state.settings.allowedPackageNames) {
        mutableStateOf(state.settings.allowedPackageNames.joinToString(","))
    }
    var dedupeWindowInput by remember(state.settings.dedupeWindowMinutes) {
        mutableStateOf(state.settings.dedupeWindowMinutes.toString())
    }

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
                    Text("默认以无障碍作为支付识别主链路；ADB 仅作为高级功能开放给有能力的用户。")
                    Text("无障碍：${if (state.settings.useAccessibilityService) "已启用" else "未启用"}")
                    Switch(
                        checked = state.settings.useAccessibilityService,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(useAccessibilityService = checked) }
                        }
                    )
                    Text("无线 ADB 高级功能：${if (state.settings.useAdbBridge) "已启用" else "未启用"}")
                    Switch(
                        checked = state.settings.useAdbBridge,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(useAdbBridge = checked) }
                        }
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("无障碍权限获取引导", style = MaterialTheme.typography.titleMedium)
                    Text("1. 打开系统设置。")
                    Text("2. 进入“无障碍”或“辅助功能”。")
                    Text("3. 找到 Where Is My Money。")
                    Text("4. 开启服务，并确认允许读取屏幕内容。")
                    Text("5. 回到 App 后再测试自动抓取。")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("白名单与去重", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = allowedPackagesInput,
                        onValueChange = { allowedPackagesInput = it },
                        label = { Text("允许自动抓取的包名，逗号分隔") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dedupeWindowInput,
                        onValueChange = { dedupeWindowInput = it },
                        label = { Text("去重时间窗（分钟）") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            viewModel.updateSettings {
                                it.copy(
                                    allowedPackageNames = allowedPackagesInput.split(',').map(String::trim).filter(String::isNotBlank),
                                    dedupeWindowMinutes = dedupeWindowInput.toIntOrNull() ?: it.dedupeWindowMinutes
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存白名单 / 去重设置")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("模拟导入支付账单", style = MaterialTheme.typography.titleMedium)
                    Text("点击后按当前规则、白名单和去重策略把一条支付成功记录写入本地账本。")
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
                    Text("1. 先完成无障碍授权")
                    Text("2. 监听白名单内支付 App 的支付结果页")
                    Text("3. 解析金额、商户、时间并在本地按时间窗去重")
                    Text("4. 按规则决定记录 / 忽略 / 询问")
                    Text("5. 仅把必要字段发送给大模型做分类（可选）")
                }
            }
        }
    }
}

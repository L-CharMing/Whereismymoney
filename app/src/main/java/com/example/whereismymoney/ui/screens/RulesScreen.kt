package com.example.whereismymoney.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.whereismymoney.data.model.CaptureCandidate
import com.example.whereismymoney.data.model.CaptureMode
import com.example.whereismymoney.ui.state.LedgerUiState
import com.example.whereismymoney.ui.state.LedgerViewModel
import java.math.BigDecimal
import java.time.LocalDateTime

@Composable
fun RulesScreen(state: LedgerUiState, paddingValues: PaddingValues, viewModel: LedgerViewModel) {
    val context = LocalContext.current
    var aiEndpoint by remember(state.settings.aiEndpoint) { mutableStateOf(state.settings.aiEndpoint) }
    var apiKeyPlaceholder by remember(state.settings.aiApiKeyPlaceholder) { mutableStateOf(state.settings.aiApiKeyPlaceholder) }
    var allowedPackagesInput by remember(state.settings.allowedPackageNames) { mutableStateOf(state.settings.allowedPackageNames.joinToString(",")) }
    var dedupeWindowInput by remember(state.settings.dedupeWindowMinutes) { mutableStateOf(state.settings.dedupeWindowMinutes.toString()) }
    var voiceInput by remember { mutableStateOf("") }

    val demoCandidate = CaptureCandidate(
        title = "",
        merchant = "WeChat Pay",
        amount = BigDecimal("22.00"),
        detectedAt = LocalDateTime.now(),
        rawText = "微信支付成功 ¥22.00",
        source = CaptureMode.ACCESSIBILITY
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("自动记账设置", style = MaterialTheme.typography.headlineSmall)
                    Switch(checked = state.settings.useAccessibilityService, onCheckedChange = { checked -> viewModel.updateSettings { it.copy(useAccessibilityService = checked) } })
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("打开系统无障碍设置") }
                    Text("ADB 高级功能")
                    Switch(checked = state.settings.useAdbBridge, onCheckedChange = { checked -> viewModel.updateSettings { it.copy(useAdbBridge = checked) } })
                    OutlinedTextField(value = allowedPackagesInput, onValueChange = { allowedPackagesInput = it }, label = { Text("白名单包名（逗号分隔）") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dedupeWindowInput, onValueChange = { dedupeWindowInput = it }, label = { Text("去重时间窗（分钟）") }, modifier = Modifier.fillMaxWidth())
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
                    ) { Text("保存自动记账设置") }
                    OutlinedButton(onClick = { viewModel.importCandidate(demoCandidate) }, modifier = Modifier.fillMaxWidth()) { Text("模拟导入一条自动账单") }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AI 记账设置", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(value = aiEndpoint, onValueChange = { aiEndpoint = it }, label = { Text("AI Endpoint") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = apiKeyPlaceholder, onValueChange = { apiKeyPlaceholder = it }, label = { Text("API Key / Token") }, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = { viewModel.updateSettings { it.copy(aiEndpoint = aiEndpoint, aiApiKeyPlaceholder = apiKeyPlaceholder) } }, modifier = Modifier.fillMaxWidth()) {
                        Text("保存 AI 设置")
                    }
                    Text("AI 语音记账（文本模拟）")
                    OutlinedTextField(value = voiceInput, onValueChange = { voiceInput = it }, label = { Text("例如：午饭 26") }, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = { viewModel.addAiVoiceBill(voiceInput) }, modifier = Modifier.fillMaxWidth()) { Text("调用 AI 语音记账") }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("导入 / 导出", style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = viewModel::exportSelectedMonthCsv, modifier = Modifier.fillMaxWidth()) { Text("导出当前月份 CSV") }
                }
            }
        }

        item { Text("ADB 使用提示", style = MaterialTheme.typography.titleMedium) }
        items(state.wirelessAdbHints) { hint ->
            Card(modifier = Modifier.fillMaxWidth()) { Text(hint, modifier = Modifier.padding(16.dp)) }
        }
    }
}

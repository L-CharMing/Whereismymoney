package com.example.whereismymoney.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.whereismymoney.data.model.CaptureCandidate
import com.example.whereismymoney.data.model.CaptureMode
import com.example.whereismymoney.ui.state.LedgerUiState
import com.example.whereismymoney.ui.state.LedgerViewModel
import java.math.BigDecimal
import java.time.LocalDateTime

private enum class SettingRoute(val title: String, val subtitle: String) {
    ROOT("设置", "选择一个设置模块"),
    AUTO_CAPTURE("自动记账", "无障碍、ADB、去重设置"),
    AI("AI记账", "Endpoint、Token、语音记账"),
    IMPORT_EXPORT("导入导出", "CSV 导出功能"),
    ADB_HINTS("ADB 使用提示", "无线调试与连接步骤")
}

@Composable
fun RulesScreen(state: LedgerUiState, paddingValues: PaddingValues, viewModel: LedgerViewModel) {
    val context = LocalContext.current
    var route by remember { mutableStateOf(SettingRoute.ROOT) }
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
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF8FBFF), Color(0xFFF1F6FF))))
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeaderCard(
                title = if (route == SettingRoute.ROOT) SettingRoute.ROOT.title else route.title,
                subtitle = if (route == SettingRoute.ROOT) SettingRoute.ROOT.subtitle else route.subtitle,
                showBack = route != SettingRoute.ROOT,
                onBack = { route = SettingRoute.ROOT }
            )
        }

        when (route) {
            SettingRoute.ROOT -> {
                items(
                    listOf(
                        SettingRoute.AUTO_CAPTURE,
                        SettingRoute.AI,
                        SettingRoute.IMPORT_EXPORT,
                        SettingRoute.ADB_HINTS
                    )
                ) { item ->
                    SettingsEntry(title = item.title, subtitle = item.subtitle, onClick = { route = item })
                }
            }

            SettingRoute.AUTO_CAPTURE -> {
                item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("无障碍记账", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("启用无障碍自动记账")
                                Switch(
                                    checked = state.settings.useAccessibilityService,
                                    onCheckedChange = { checked -> viewModel.updateSettings { it.copy(useAccessibilityService = checked) } }
                                )
                            }
                            OutlinedButton(
                                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("打开系统无障碍设置") }

                            Text("ADB 高级功能", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("启用 ADB 辅助")
                                Switch(
                                    checked = state.settings.useAdbBridge,
                                    onCheckedChange = { checked -> viewModel.updateSettings { it.copy(useAdbBridge = checked) } }
                                )
                            }
                            OutlinedTextField(
                                value = allowedPackagesInput,
                                onValueChange = { allowedPackagesInput = it },
                                label = { Text("白名单包名（逗号分隔）") },
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
                            ) { Text("保存自动记账设置") }

                            OutlinedButton(onClick = { viewModel.importCandidate(demoCandidate) }, modifier = Modifier.fillMaxWidth()) {
                                Text("模拟导入一条自动账单")
                            }
                        }
                    }
                }
            }

            SettingRoute.AI -> {
                item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("AI 配置", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(value = aiEndpoint, onValueChange = { aiEndpoint = it }, label = { Text("AI Endpoint") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = apiKeyPlaceholder, onValueChange = { apiKeyPlaceholder = it }, label = { Text("API Key / Token") }, modifier = Modifier.fillMaxWidth())
                            OutlinedButton(onClick = { viewModel.updateSettings { it.copy(aiEndpoint = aiEndpoint, aiApiKeyPlaceholder = apiKeyPlaceholder) } }, modifier = Modifier.fillMaxWidth()) {
                                Text("保存 AI 设置")
                            }

                            Text("AI 语音记账（文本模拟）", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(value = voiceInput, onValueChange = { voiceInput = it }, label = { Text("例如：午饭 26") }, modifier = Modifier.fillMaxWidth())
                            OutlinedButton(onClick = { viewModel.addAiVoiceBill(voiceInput) }, modifier = Modifier.fillMaxWidth()) { Text("调用 AI 语音记账") }
                        }
                    }
                }
            }

            SettingRoute.IMPORT_EXPORT -> {
                item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("导入 / 导出", style = MaterialTheme.typography.titleMedium)
                            Text("当前支持导出当月 CSV 文件。")
                            OutlinedButton(onClick = viewModel::exportSelectedMonthCsv, modifier = Modifier.fillMaxWidth()) { Text("导出当前月份 CSV") }
                        }
                    }
                }
            }

            SettingRoute.ADB_HINTS -> {
                items(state.wirelessAdbHints) { hint ->
                    GlassCard { Text(hint) }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(title: String, subtitle: String, showBack: Boolean, onBack: () -> Unit) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(subtitle, color = Color.Gray)
            if (showBack) {
                OutlinedButton(onClick = onBack) { Text("返回设置首页") }
            }
        }
    }
}

@Composable
private fun SettingsEntry(title: String, subtitle: String, onClick: () -> Unit) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = Color.Gray)
            OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text("进入")
            }
        }
    }
}

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

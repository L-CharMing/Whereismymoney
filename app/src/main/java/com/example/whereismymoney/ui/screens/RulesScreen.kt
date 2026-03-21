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
import com.example.whereismymoney.data.model.RecordRuleAction
import com.example.whereismymoney.ui.state.LedgerUiState
import com.example.whereismymoney.ui.state.LedgerViewModel

@Composable
fun RulesScreen(state: LedgerUiState, paddingValues: PaddingValues, viewModel: LedgerViewModel) {
    var matcher by remember { mutableStateOf("") }
    var action by remember { mutableStateOf(RecordRuleAction.ALWAYS_RECORD) }
    var categoryId by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var aiEndpoint by remember(state.settings.aiEndpoint) { mutableStateOf(state.settings.aiEndpoint) }
    var apiKeyPlaceholder by remember(state.settings.aiApiKeyPlaceholder) { mutableStateOf(state.settings.aiApiKeyPlaceholder) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("规则与 AI 接口", style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(value = aiEndpoint, onValueChange = { aiEndpoint = it }, label = { Text("AI Endpoint") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = apiKeyPlaceholder, onValueChange = { apiKeyPlaceholder = it }, label = { Text("API Key / Token 占位") }, modifier = Modifier.fillMaxWidth())
                    Text("未知账单是否复核：${if (state.settings.reviewUnknownBills) "是" else "否"}")
                    Switch(
                        checked = state.settings.reviewUnknownBills,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings {
                                it.copy(
                                    reviewUnknownBills = checked,
                                    aiEndpoint = aiEndpoint,
                                    aiApiKeyPlaceholder = apiKeyPlaceholder
                                )
                            }
                        }
                    )
                    OutlinedButton(
                        onClick = {
                            viewModel.updateSettings {
                                it.copy(aiEndpoint = aiEndpoint, aiApiKeyPlaceholder = apiKeyPlaceholder)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存 AI 配置到本地")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("新增规则", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = matcher, onValueChange = { matcher = it }, label = { Text("匹配关键字 / 商户") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = action.name, onValueChange = {
                        action = runCatching { RecordRuleAction.valueOf(it.uppercase()) }.getOrDefault(action)
                    }, label = { Text("动作：ALWAYS_RECORD / IGNORE / ASK_EVERY_TIME") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = categoryId, onValueChange = { categoryId = it }, label = { Text("分类ID") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(
                        onClick = {
                            viewModel.addRule(matcher, action, categoryId.ifBlank { null }, notes)
                            matcher = ""
                            categoryId = ""
                            notes = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存规则到本地")
                    }
                }
            }
        }
        items(state.rules) { rule ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(rule.matcher, style = MaterialTheme.typography.titleMedium)
                    Text("动作：${rule.action}")
                    Text("分类：${rule.categoryId ?: "未指定"}")
                    Text("说明：${rule.notes}")
                }
            }
        }
    }
}

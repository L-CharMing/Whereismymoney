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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.whereismymoney.data.repository.InMemoryLedgerRepository

@Composable
fun RulesScreen(repository: InMemoryLedgerRepository, paddingValues: PaddingValues) {
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
                    Text("AI Endpoint: ${repository.settings.aiEndpoint}")
                    Text("API Key: ${repository.settings.aiApiKeyPlaceholder}")
                    Text("未知账单是否复核：${if (repository.settings.reviewUnknownBills) "是" else "否"}")
                    Text("你后续可以把本地关键词分类器替换成任意大模型网关。")
                }
            }
        }
        items(repository.rules) { rule ->
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

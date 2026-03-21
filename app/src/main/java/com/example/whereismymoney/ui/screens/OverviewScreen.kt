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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.whereismymoney.ui.state.LedgerUiState
import com.example.whereismymoney.ui.state.LedgerViewModel

@Composable
fun OverviewScreen(state: LedgerUiState, paddingValues: PaddingValues, viewModel: LedgerViewModel) {
    var title by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<String?>(state.categories.firstOrNull()?.id) }

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
                    Text("本月总览", style = MaterialTheme.typography.headlineSmall)
                    Text("本月总支出：¥${state.thisMonthTotal}")
                    Text("支出最高类别：${state.thisMonthTopCategory}")
                    Text("建议：${state.thisMonthSuggestions}")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("手动记一笔", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = merchant, onValueChange = { merchant = it }, label = { Text("商户") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("金额") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = categoryId ?: "",
                        onValueChange = { categoryId = it.ifBlank { null } },
                        label = { Text("分类ID（food / transport 等）") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            viewModel.addManualRecord(title, merchant, amount, categoryId)
                            title = ""
                            merchant = ""
                            amount = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存到本地账本")
                    }
                }
            }
        }
        item {
            Text("本月分类统计", style = MaterialTheme.typography.titleMedium)
        }
        items(state.monthlyBreakdown) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.categoryName, style = MaterialTheme.typography.titleMedium)
                    Text("金额：¥${item.totalAmount}")
                    Text("占比：${(item.percentOfMonth * 100).toInt()}%")
                }
            }
        }
        item {
            Text("最近账单", style = MaterialTheme.typography.titleMedium)
        }
        items(state.records.take(8)) { record ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(record.title, style = MaterialTheme.typography.titleMedium)
                    Text("商户：${record.merchant}")
                    Text("金额：¥${record.amount}")
                    Text("来源：${record.source}")
                    Text("时间：${record.occurredAt}")
                }
            }
        }
    }
}

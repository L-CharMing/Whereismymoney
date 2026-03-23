package com.example.whereismymoney.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
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
import com.example.whereismymoney.data.model.BillRecord
import com.example.whereismymoney.ui.state.LedgerUiState
import com.example.whereismymoney.ui.state.LedgerViewModel

@Composable
fun OverviewScreen(state: LedgerUiState, paddingValues: PaddingValues, viewModel: LedgerViewModel) {
    var title by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<String?>(state.categories.firstOrNull()?.id) }
    var editingRecordId by remember { mutableStateOf<String?>(null) }
    var editingTitle by remember { mutableStateOf("") }
    var editingMerchant by remember { mutableStateOf("") }
    var editingAmount by remember { mutableStateOf("") }
    var editingCategoryId by remember { mutableStateOf("") }

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
                    Text("${state.selectedMonth.year}年${state.selectedMonth.monthValue}月总览", style = MaterialTheme.typography.headlineSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { viewModel.selectPreviousMonth() }, modifier = Modifier.weight(1f)) { Text("上个月") }
                        OutlinedButton(onClick = { viewModel.selectNextMonth() }, modifier = Modifier.weight(1f)) { Text("下个月") }
                    }
                    Text("本月总支出：¥${state.thisMonthTotal}")
                    Text("支出最高类别：${state.thisMonthTopCategory}")
                    Text("建议：${state.thisMonthSuggestions}")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("搜索账单", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        label = { Text("按标题 / 商户 / 分类搜索") },
                        modifier = Modifier.fillMaxWidth()
                    )
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
            Text("当月分类统计", style = MaterialTheme.typography.titleMedium)
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
            Text("账单列表（可搜索 / 编辑 / 删除）", style = MaterialTheme.typography.titleMedium)
        }
        items(state.filteredRecords, key = { it.id }) { record ->
            RecordEditorCard(
                record = record,
                isEditing = editingRecordId == record.id,
                editingTitle = editingTitle,
                editingMerchant = editingMerchant,
                editingAmount = editingAmount,
                editingCategoryId = editingCategoryId,
                onEditStart = {
                    editingRecordId = record.id
                    editingTitle = record.title
                    editingMerchant = record.merchant
                    editingAmount = record.amount.toPlainString()
                    editingCategoryId = record.categoryId.orEmpty()
                },
                onCancel = { editingRecordId = null },
                onTitleChange = { editingTitle = it },
                onMerchantChange = { editingMerchant = it },
                onAmountChange = { editingAmount = it },
                onCategoryChange = { editingCategoryId = it },
                onSave = {
                    viewModel.updateRecord(record.id, editingTitle, editingMerchant, editingAmount, editingCategoryId.ifBlank { null })
                    editingRecordId = null
                },
                onDelete = {
                    viewModel.deleteRecord(record.id)
                    if (editingRecordId == record.id) editingRecordId = null
                }
            )
        }
    }
}

@Composable
private fun RecordEditorCard(
    record: BillRecord,
    isEditing: Boolean,
    editingTitle: String,
    editingMerchant: String,
    editingAmount: String,
    editingCategoryId: String,
    onEditStart: () -> Unit,
    onCancel: () -> Unit,
    onTitleChange: (String) -> Unit,
    onMerchantChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!isEditing) {
                Text(record.title, style = MaterialTheme.typography.titleMedium)
                Text("商户：${record.merchant}")
                Text("金额：¥${record.amount}")
                Text("分类：${record.categoryId ?: "未分类"}")
                Text("来源：${record.source}")
                Text("时间：${record.occurredAt}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onEditStart, modifier = Modifier.weight(1f)) {
                        Text("编辑")
                    }
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                        Text("删除")
                    }
                }
            } else {
                Text("编辑账单", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = editingTitle, onValueChange = onTitleChange, label = { Text("标题") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = editingMerchant, onValueChange = onMerchantChange, label = { Text("商户") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = editingAmount, onValueChange = onAmountChange, label = { Text("金额") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = editingCategoryId, onValueChange = onCategoryChange, label = { Text("分类ID") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) {
                        Text("保存修改")
                    }
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text("取消")
                    }
                }
            }
        }
    }
}

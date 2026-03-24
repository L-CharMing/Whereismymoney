package com.example.whereismymoney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.whereismymoney.data.model.BillRecord
import com.example.whereismymoney.ui.state.LedgerUiState
import com.example.whereismymoney.ui.state.LedgerViewModel
import java.math.BigDecimal
import kotlin.math.roundToInt

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
    var pendingDeleteRecord by remember { mutableStateOf<BillRecord?>(null) }
    var pendingSaveRecordId by remember { mutableStateOf<String?>(null) }

    if (pendingDeleteRecord != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteRecord = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除“${pendingDeleteRecord?.title}”这笔账单吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecord(pendingDeleteRecord!!.id)
                    if (editingRecordId == pendingDeleteRecord!!.id) editingRecordId = null
                    pendingDeleteRecord = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRecord = null }) { Text("取消") }
            }
        )
    }

    if (pendingSaveRecordId != null) {
        AlertDialog(
            onDismissRequest = { pendingSaveRecordId = null },
            title = { Text("确认修改") },
            text = { Text("确定保存这笔账单的修改吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateRecord(pendingSaveRecordId!!, editingTitle, editingMerchant, editingAmount, editingCategoryId.ifBlank { null })
                    editingRecordId = null
                    pendingSaveRecordId = null
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { pendingSaveRecordId = null }) { Text("取消") }
            }
        )
    }

    if (state.exportMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearExportMessage,
            title = { Text("提示") },
            text = { Text(state.exportMessage ?: "") },
            confirmButton = {
                TextButton(onClick = viewModel::clearExportMessage) { Text("知道了") }
            }
        )
    }

    val budgetRows = state.monthlyBreakdown.mapNotNull { summary ->
        val category = state.categories.firstOrNull { it.name == summary.categoryName }
        val budget = category?.monthlyBudgetHint ?: return@mapNotNull null
        val ratio = if (budget.compareTo(BigDecimal.ZERO) == 0) 0f else summary.totalAmount.divide(budget, 4, java.math.RoundingMode.HALF_UP).toFloat()
        BudgetRow(summary.categoryName, summary.totalAmount, budget, ratio)
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${state.selectedMonth.year}年${state.selectedMonth.monthValue}月总览", style = MaterialTheme.typography.headlineSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { viewModel.selectPreviousMonth() }, modifier = Modifier.weight(1f)) { Text("上个月") }
                        OutlinedButton(onClick = { viewModel.selectNextMonth() }, modifier = Modifier.weight(1f)) { Text("下个月") }
                    }
                    OutlinedButton(onClick = viewModel::exportSelectedMonthCsv, modifier = Modifier.fillMaxWidth()) {
                        Text("导出当前月份 CSV")
                    }
                    Text("本月总支出：¥${state.thisMonthTotal}")
                    Text("支出最高类别：${state.thisMonthTopCategory}")
                    Text("建议：${state.thisMonthSuggestions}")
                }
            }
        }
        if (budgetRows.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("预算提醒", style = MaterialTheme.typography.titleMedium)
                        budgetRows.forEach { row ->
                            val percent = (row.ratio * 100).roundToInt()
                            val levelText = when {
                                row.ratio >= 1f -> "已超预算"
                                row.ratio >= 0.8f -> "接近预算"
                                else -> "正常"
                            }
                            Text("${row.categoryName}：¥${row.spent} / ¥${row.budget}（$percent%）- $levelText")
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("分类统计图表", style = MaterialTheme.typography.titleMedium)
                        budgetRows.forEach { row ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(row.categoryName, fontWeight = FontWeight.Medium)
                                    Text("¥${row.spent}")
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .background(Color(0xFFEAEAEA))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(row.ratio.coerceIn(0f, 1f))
                                            .height(12.dp)
                                            .background(if (row.ratio >= 1f) Color(0xFFE57373) else Color(0xFF64B5F6))
                                    )
                                }
                            }
                        }
                    }
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
                onSave = { pendingSaveRecordId = record.id },
                onDelete = { pendingDeleteRecord = record }
            )
        }
    }
}

data class BudgetRow(
    val categoryName: String,
    val spent: BigDecimal,
    val budget: BigDecimal,
    val ratio: Float
)

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

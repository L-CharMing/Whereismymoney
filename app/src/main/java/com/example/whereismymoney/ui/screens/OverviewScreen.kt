package com.example.whereismymoney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.whereismymoney.ui.state.LedgerUiState
import com.example.whereismymoney.ui.state.LedgerViewModel
import kotlin.math.roundToInt

@Composable
fun OverviewScreen(state: LedgerUiState, paddingValues: PaddingValues, viewModel: LedgerViewModel) {
    var manualDialogVisible by remember { mutableStateOf(false) }
    var amountInput by remember { mutableStateOf("") }
    var purposeInput by remember { mutableStateOf("") }

    if (manualDialogVisible) {
        AlertDialog(
            onDismissRequest = { manualDialogVisible = false },
            title = { Text("手动记账") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = amountInput, onValueChange = { amountInput = it }, label = { Text("金额") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = purposeInput, onValueChange = { purposeInput = it }, label = { Text("用途") }, modifier = Modifier.fillMaxWidth())
                    Text("常用用途")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("餐饮", "交通", "购物", "娱乐", "居住", "医疗").forEach { common ->
                            OutlinedButton(onClick = { purposeInput = common }) { Text(common) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addManualRecord(
                        title = purposeInput.ifBlank { "手动记录" },
                        merchant = "",
                        amountInput = amountInput,
                        categoryId = null
                    )
                    amountInput = ""
                    purposeInput = ""
                    manualDialogVisible = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { manualDialogVisible = false }) { Text("取消") } }
        )
    }

    if (state.exportMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearExportMessage,
            title = { Text("提示") },
            text = { Text(state.exportMessage ?: "") },
            confirmButton = { TextButton(onClick = viewModel::clearExportMessage) { Text("知道了") } }
        )
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
                    Text("${state.selectedMonth.year}年${state.selectedMonth.monthValue}月", style = MaterialTheme.typography.headlineSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = viewModel::selectPreviousMonth, modifier = Modifier.width(120.dp)) { Text("上个月") }
                        OutlinedButton(onClick = viewModel::selectNextMonth, modifier = Modifier.width(120.dp)) { Text("下个月") }
                        OutlinedButton(onClick = { manualDialogVisible = true }, modifier = Modifier.width(120.dp)) { Text("手动记账") }
                    }
                    Text("当月总支出：¥${state.thisMonthTotal}")
                    Text("主要支出用途：${state.thisMonthTopCategory}")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("当月统计", style = MaterialTheme.typography.titleMedium)
                    state.monthlyBreakdown.forEach { row ->
                        val percent = (row.percentOfMonth * 100).roundToInt()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(row.categoryName, fontWeight = FontWeight.Medium)
                                Text("¥${row.totalAmount} (${percent}%)")
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(Color(0xFFEAEAEA))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(row.percentOfMonth.coerceIn(0f, 1f))
                                        .height(10.dp)
                                        .background(Color(0xFF64B5F6))
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Text("当月明细", style = MaterialTheme.typography.titleMedium) }
        items(state.filteredRecords, key = { it.id }) { record ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(record.title, style = MaterialTheme.typography.titleMedium)
                    Text("金额：¥${record.amount}")
                    Text("日期：${record.occurredAt.toLocalDate()}")
                    Text("来源：${record.source}")
                }
            }
        }
    }
}

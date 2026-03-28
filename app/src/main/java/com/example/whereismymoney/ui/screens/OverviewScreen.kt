package com.example.whereismymoney.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.whereismymoney.data.model.BillRecord
import com.example.whereismymoney.ui.state.LedgerUiState
import com.example.whereismymoney.ui.state.LedgerViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(state: LedgerUiState, paddingValues: PaddingValues, viewModel: LedgerViewModel) {
    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    var manualDialogVisible by remember { mutableStateOf(false) }
    var amountInput by remember { mutableStateOf("") }
    var purposeInput by remember { mutableStateOf("") }
    var occurredDate by remember { mutableStateOf(LocalDate.now()) }

    var editingRecord by remember { mutableStateOf<BillRecord?>(null) }
    var showAnnualView by remember { mutableStateOf(false) }

    if (showAnnualView) {
        AnnualLedgerScreen(
            records = state.allRecords,
            selectedMonth = state.selectedMonth,
            onBack = { showAnnualView = false },
            onSelectMonth = {
                viewModel.selectMonth(it)
                showAnnualView = false
            }
        )
        return
    }

    val recordsByDay = remember(state.filteredRecords) {
        state.filteredRecords.groupBy { it.occurredAt.toLocalDate() }.toList().sortedByDescending { it.first }
    }

    if (manualDialogVisible) {
        AlertDialog(
            onDismissRequest = { manualDialogVisible = false },
            title = { Text("手动记账") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = amountInput, onValueChange = { amountInput = it }, label = { Text("金额") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = purposeInput, onValueChange = { purposeInput = it }, label = { Text("用途") }, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth -> occurredDate = LocalDate.of(year, month + 1, dayOfMonth) },
                                occurredDate.year,
                                occurredDate.monthValue - 1,
                                occurredDate.dayOfMonth
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("记账日期：${occurredDate.format(dateFormatter)}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addManualRecord(
                        title = purposeInput.ifBlank { "手动记录" },
                        merchant = "",
                        amountInput = amountInput,
                        categoryId = null,
                        occurredDateInput = occurredDate.format(dateFormatter)
                    )
                    amountInput = ""
                    purposeInput = ""
                    occurredDate = LocalDate.now()
                    manualDialogVisible = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { manualDialogVisible = false }) { Text("取消") } }
        )
    }

    editingRecord?.let { record ->
        var editTitle by remember(record.id) { mutableStateOf(record.title) }
        var editAmount by remember(record.id) { mutableStateOf(record.amount.toPlainString()) }
        AlertDialog(
            onDismissRequest = { editingRecord = null },
            title = { Text("编辑账单") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("用途") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editAmount, onValueChange = { editAmount = it }, label = { Text("金额") }, modifier = Modifier.fillMaxWidth())
                    Text("日期：${record.occurredAt.toLocalDate()}")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateRecord(record.id, editTitle, editAmount)
                    editingRecord = null
                }) { Text("保存") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        viewModel.importRecordToProduct(record.id)
                        editingRecord = null
                    }) { Text("导入日均") }
                    TextButton(onClick = {
                        viewModel.deleteRecord(record.id)
                        editingRecord = null
                    }) { Text("删除") }
                    TextButton(onClick = { editingRecord = null }) { Text("取消") }
                }
            }
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

    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showAnnualView = true }) {
                            Text("${state.selectedMonth.year}年${state.selectedMonth.monthValue}月（点击查看年度账单）")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            FilledTonalButton(onClick = viewModel::selectPreviousMonth, modifier = Modifier.weight(1f)) { Text("上个月") }
                            FilledTonalButton(onClick = viewModel::selectNextMonth, modifier = Modifier.weight(1f)) { Text("下个月") }
                        }
                        Text("当月总支出：¥${state.thisMonthTotal}", style = MaterialTheme.typography.titleMedium)
                        Text("主要支出用途：${state.thisMonthTopCategory}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("当月统计", style = MaterialTheme.typography.titleMedium)
                        state.monthlyBreakdown.forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(row.categoryName)
                                Text("¥${row.totalAmount}")
                            }
                            LinearProgressIndicator(
                                progress = { row.percentOfMonth.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item { Text("当月明细（点按可编辑）", style = MaterialTheme.typography.titleMedium) }
            items(recordsByDay, key = { it.first.toString() }) { group ->
                DayRecordSection(day = group.first, records = group.second, onRecordClick = { editingRecord = it })
            }
            item { Box(modifier = Modifier.height(88.dp)) }
        }

        FloatingActionButton(
            onClick = { manualDialogVisible = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text("记")
        }
    }
}

@Composable
private fun DayRecordSection(day: LocalDate, records: List<BillRecord>, onRecordClick: (BillRecord) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${day.monthValue}月${day.dayOfMonth}日", style = MaterialTheme.typography.titleMedium)
            records.forEach { record ->
                Card(onClick = { onRecordClick(record) }, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(record.title, style = MaterialTheme.typography.bodyLarge)
                            Text(record.occurredAt.toLocalTime().toString(), style = MaterialTheme.typography.bodySmall)
                        }
                        Text("¥${record.amount}", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnualLedgerScreen(
    records: List<BillRecord>,
    selectedMonth: YearMonth,
    onBack: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit
) {
    val year = selectedMonth.year
    val monthTotals = remember(records, year) {
        records
            .filter { it.occurredAt.year == year }
            .groupBy { YearMonth.from(it.occurredAt) }
            .map { (month, monthRecords) ->
                month to monthRecords.fold(java.math.BigDecimal.ZERO) { acc, item -> acc + item.amount }
            }
            .sortedByDescending { it.first }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${year}年度账单") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (monthTotals.isEmpty()) {
                item { Text("暂无年度账单数据") }
            } else {
                items(monthTotals, key = { it.first.toString() }) { row ->
                    Card(onClick = { onSelectMonth(row.first) }, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${row.first.monthValue}月")
                            Text("¥${row.second}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

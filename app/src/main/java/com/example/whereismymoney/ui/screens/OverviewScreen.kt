package com.example.whereismymoney.ui.screens
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.whereismymoney.data.model.BillRecord
import com.example.whereismymoney.ui.state.LedgerUiState
import com.example.whereismymoney.ui.state.LedgerViewModel
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun OverviewScreen(state: LedgerUiState, paddingValues: PaddingValues, viewModel: LedgerViewModel) {
    var manualDialogVisible by remember { mutableStateOf(false) }
    var amountInput by remember { mutableStateOf("") }
    var purposeInput by remember { mutableStateOf("") }
    val recordsByDay = remember(state.filteredRecords) {
        state.filteredRecords
            .groupBy { it.occurredAt.toLocalDate() }
            .toList()
            .sortedByDescending { it.first }
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFFF5FAFF), Color(0xFFEAF1FF))))
            .padding(paddingValues)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { MonthHeroCard(state = state, onPrev = viewModel::selectPreviousMonth, onNext = viewModel::selectNextMonth) }
            item { StatisticsCard(state = state) }
            item { Text("当月明细（按天）", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp)) }
            items(recordsByDay, key = { it.first.toString() }) { group ->
                DayRecordSection(group.first, group.second)
            }
            item { Box(modifier = Modifier.height(88.dp)) }
        }

        GlassFab(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            onClick = { manualDialogVisible = true }
        )
    }
}

@Composable
private fun MonthHeroCard(state: LedgerUiState, onPrev: () -> Unit, onNext: () -> Unit) {
    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${state.selectedMonth.year}年${state.selectedMonth.monthValue}月", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onPrev) { Text("‹") }
                Text("月度总览", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onNext) { Text("›") }
            }
            Text("当月总支出", color = Color(0xFF5F6E8C))
            Text("¥${state.thisMonthTotal}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("主要支出：${state.thisMonthTopCategory}", color = Color(0xFF5F6E8C))
        }
    }
}

@Composable
private fun StatisticsCard(state: LedgerUiState) {
    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("当月统计", style = MaterialTheme.typography.titleMedium)
            if (state.monthlyBreakdown.isEmpty()) {
                Text("暂无统计数据")
            } else {
                state.monthlyBreakdown.forEachIndexed { index, row ->
                    val percent = (row.percentOfMonth * 100).roundToInt()
                    val barColors = when (index % 4) {
                        0 -> listOf(Color(0xFFAAF1FF), Color(0xFF64C9FF))
                        1 -> listOf(Color(0xFFC7FFD8), Color(0xFF6EDB9A))
                        2 -> listOf(Color(0xFFFFE1B8), Color(0xFFFFB869))
                        else -> listOf(Color(0xFFE7D2FF), Color(0xFFBE9BFF))
                    }
                    val barBrush = Brush.horizontalGradient(colors = barColors)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(row.categoryName, fontWeight = FontWeight.Medium)
                            Text("¥${row.totalAmount} · ${percent}%")
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(color = Color.White.copy(alpha = 0.55f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(row.percentOfMonth.coerceIn(0f, 1f))
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(brush = barBrush)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayRecordSection(day: LocalDate, records: List<BillRecord>) {
    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${day.monthValue}月${day.dayOfMonth}日", style = MaterialTheme.typography.titleMedium)
            records.forEach { record ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(record.title, style = MaterialTheme.typography.bodyLarge)
                        Text(record.source.name, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A859C))
                    }
                    Text("¥${record.amount}", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun GlassSurface(modifier: Modifier = Modifier, content: @Composable Column.() -> Unit) {
    Box(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x33577AFF), spotColor = Color(0x33577AFF))
            .clip(RoundedCornerShape(26.dp))
            .background(
                brush = Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = 0.68f), Color.White.copy(alpha = 0.4f)))
            )
            .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun GlassFab(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(58.dp)
            .shadow(14.dp, CircleShape, ambientColor = Color(0x446A90FF), spotColor = Color(0x446A90FF))
            .clip(CircleShape)
            .background(brush = Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = 0.8f), Color(0xFFCFE3FF).copy(alpha = 0.85f))))
            .border(1.dp, Color.White.copy(alpha = 0.75f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = onClick) {
            Text("＋", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF365DA8))
        }
    }
}

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
import java.time.YearMonth

@Composable
fun OverviewScreen(repository: InMemoryLedgerRepository, paddingValues: PaddingValues) {
    val month = YearMonth.now()
    val summary = repository.monthlySummary(month)
    val insight = repository.monthlyInsight(month)

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
                    Text("${month.year}年${month.monthValue}月总结", style = MaterialTheme.typography.headlineSmall)
                    Text("本月总支出：¥${insight.totalSpent}")
                    Text("支出最高类别：${insight.topCategory}")
                    Text("花钱最多的一天：${insight.biggestExpenseDay}")
                    Text("建议：${insight.suggestion}")
                }
            }
        }
        items(summary) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.categoryName, style = MaterialTheme.typography.titleMedium)
                    Text("金额：¥${item.totalAmount}")
                    Text("占比：${(item.percentOfMonth * 100).toInt()}%")
                }
            }
        }
    }
}

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
fun ProductCostScreen(state: LedgerUiState, paddingValues: PaddingValues, viewModel: LedgerViewModel) {
    var name by remember { mutableStateOf("") }
    var totalPrice by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("") }

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
                    Text("产品日均成本", style = MaterialTheme.typography.headlineSmall)
                    Text("记录一个产品花了多少钱、买了多少天，自动算每日平均花费。")
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("产品名称") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = totalPrice, onValueChange = { totalPrice = it }, label = { Text("总金额") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = days, onValueChange = { days = it }, label = { Text("购买天数") }, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(
                        onClick = {
                            viewModel.addProductCost(name, totalPrice, days)
                            name = ""
                            totalPrice = ""
                            days = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存产品记录")
                    }
                }
            }
        }

        items(state.productCosts, key = { it.id }) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text("金额：¥${item.totalPrice}")
                    Text("日期：${item.createdAt.toLocalDate()}")
                    Text("天数：${item.days} 天")
                    Text("日均花费：¥${item.dailyPrice}")
                }
            }
        }
    }
}

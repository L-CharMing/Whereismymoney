package com.example.whereismymoney.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.whereismymoney.data.model.BillRecord
import com.example.whereismymoney.ui.state.LedgerUiState
import com.example.whereismymoney.ui.state.LedgerViewModel

@Composable
fun ProductCostScreen(state: LedgerUiState, paddingValues: PaddingValues, viewModel: LedgerViewModel) {
    var name by remember { mutableStateOf("") }
    var totalPrice by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf("") }
    var importDialogVisible by remember { mutableStateOf(false) }

    if (importDialogVisible) {
        BillImportDialog(
            bills = state.allRecords,
            onDismiss = { importDialogVisible = false },
            onImport = { bill ->
                name = bill.title
                totalPrice = bill.amount.toPlainString()
                purchaseDate = bill.occurredAt.toLocalDate().toString()
                importDialogVisible = false
            }
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
                    Text("产品日均成本", style = MaterialTheme.typography.headlineSmall)
                    Text("输入产品名称、总金额、购买日期（yyyy-MM-dd），系统自动计算持有天数与日均花费。")
                    OutlinedButton(onClick = { importDialogVisible = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("从首页账单导入")
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("产品名称") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = totalPrice, onValueChange = { totalPrice = it }, label = { Text("总金额") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = purchaseDate, onValueChange = { purchaseDate = it }, label = { Text("购买日期（yyyy-MM-dd）") }, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(
                        onClick = {
                            viewModel.addProductCost(name, totalPrice, purchaseDate)
                            name = ""
                            totalPrice = ""
                            purchaseDate = ""
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
                    Text("购买日期：${item.purchaseDate}")
                    Text("已持有：${item.ownedDays} 天")
                    Text("日均花费：¥${item.dailyPrice}", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun BillImportDialog(bills: List<BillRecord>, onDismiss: () -> Unit, onImport: (BillRecord) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择一条账单导入") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(bills.take(30), key = { it.id }) { bill ->
                    Card(onClick = { onImport(bill) }, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(bill.title)
                                Text(bill.occurredAt.toLocalDate().toString(), style = MaterialTheme.typography.bodySmall)
                            }
                            Text("¥${bill.amount}")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

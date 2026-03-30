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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
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
import com.example.whereismymoney.data.model.ProductCostRecord
import com.example.whereismymoney.ui.state.LedgerUiState
import com.example.whereismymoney.ui.state.LedgerViewModel

@Composable
fun ProductCostScreen(state: LedgerUiState, paddingValues: PaddingValues, viewModel: LedgerViewModel) {
    var name by remember { mutableStateOf("") }
    var totalPrice by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf("") }
    var importDialogVisible by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ProductCostRecord?>(null) }

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

    editingItem?.let { item ->
        var editName by remember(item.id) { mutableStateOf(item.name) }
        var editPrice by remember(item.id) { mutableStateOf(item.totalPrice.toPlainString()) }
        var editDate by remember(item.id) { mutableStateOf(item.purchaseDate.toString()) }
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("编辑日均产品") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("产品名称") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editPrice, onValueChange = { editPrice = it }, label = { Text("总金额") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editDate, onValueChange = { editDate = it }, label = { Text("购买日期（yyyy-MM-dd）") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateProductCost(item.id, editName, editPrice, editDate)
                    editingItem = null
                }) { Text("保存") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        viewModel.deleteProductCost(item.id)
                        editingItem = null
                    }) { Text("删除") }
                    TextButton(onClick = { editingItem = null }) { Text("取消") }
                }
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
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("产品日均成本", style = MaterialTheme.typography.headlineSmall)
                    Text("输入产品名称、总金额、购买日期（yyyy-MM-dd），系统自动计算持有天数与日均花费。")
                    FilledTonalButton(onClick = { importDialogVisible = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("从首页账单导入")
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("产品名称") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = totalPrice, onValueChange = { totalPrice = it }, label = { Text("总金额") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = purchaseDate, onValueChange = { purchaseDate = it }, label = { Text("购买日期（yyyy-MM-dd）") }, modifier = Modifier.fillMaxWidth())
                    FilledTonalButton(
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

        item { Text("已记录产品（点按可编辑）", style = MaterialTheme.typography.titleMedium) }
        items(state.productCosts, key = { it.id }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { editingItem = item },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("金额：¥${item.totalPrice}")
                        Text("日均：¥${item.dailyPrice}", fontWeight = FontWeight.SemiBold)
                    }
                    Text("购买日期：${item.purchaseDate}")
                    Text("已持有：${item.ownedDays} 天")
                    Text("点击卡片可编辑/删除", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BillImportDialog(bills: List<BillRecord>, onDismiss: () -> Unit, onImport: (BillRecord) -> Unit) {
    var keyword by remember { mutableStateOf("") }
    val filtered = remember(bills, keyword) {
        val key = keyword.trim()
        if (key.isBlank()) bills.take(50) else bills.filter {
            it.title.contains(key, ignoreCase = true) || it.merchant.contains(key, ignoreCase = true)
        }.take(50)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择一条账单导入") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("搜索标题/商户") },
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.id }) { bill ->
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
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

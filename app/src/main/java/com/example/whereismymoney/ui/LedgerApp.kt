package com.example.whereismymoney.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.whereismymoney.ui.screens.OverviewScreen
import com.example.whereismymoney.ui.screens.ProductCostScreen
import com.example.whereismymoney.ui.screens.RulesScreen
import com.example.whereismymoney.ui.state.LedgerViewModel

private enum class LedgerTab(val title: String) {
    HOME("首页"),
    PRODUCT("产品日均"),
    SETTINGS("设置")
}

@Composable
fun LedgerApp(viewModel: LedgerViewModel) {
    var currentTab by remember { mutableStateOf(LedgerTab.HOME) }
    val state = viewModel.uiState

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                LedgerTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        label = { Text(tab.title) },
                        icon = { Text(tab.title.take(1)) }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (currentTab) {
            LedgerTab.HOME -> OverviewScreen(state, paddingValues, viewModel)
            LedgerTab.PRODUCT -> ProductCostScreen(state, paddingValues, viewModel)
            LedgerTab.SETTINGS -> RulesScreen(state, paddingValues, viewModel)
        }
    }
}

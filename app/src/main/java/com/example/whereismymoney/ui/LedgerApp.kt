package com.example.whereismymoney.ui

import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.whereismymoney.ui.screens.CaptureScreen
import com.example.whereismymoney.ui.screens.OverviewScreen
import com.example.whereismymoney.ui.screens.RulesScreen
import com.example.whereismymoney.ui.state.LedgerViewModel

private enum class LedgerTab(val title: String) {
    OVERVIEW("总览"),
    CAPTURE("自动记账"),
    RULES("规则 / AI")
}

@Composable
fun LedgerApp(viewModel: LedgerViewModel) {
    var currentTab by remember { mutableStateOf(LedgerTab.OVERVIEW) }
    val state = viewModel.uiState
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
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
            LedgerTab.OVERVIEW -> OverviewScreen(state, paddingValues, viewModel)
            LedgerTab.CAPTURE -> CaptureScreen(state, paddingValues, viewModel)
            LedgerTab.RULES -> RulesScreen(state, paddingValues, viewModel)
        }
    }
}

package com.suseoaa.projectoaa.home.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suseoaa.projectoaa.home.data.User

// 这是一个“智能”的 Composable，它知道如何获取 ViewModel
@Composable
fun HomeScreenRoute(
    // 使用 viewModel() 函数自动获取或创建 ViewModel 实例
    viewModel: HomeViewModel = viewModel()
) {
    // 1. 将 ViewModel 的 StateFlow 转为 Compose 的 State
    val uiState by viewModel.uiState.collectAsState()

    // 2. 把状态和事件传给纯 UI 函数
    HomeScreen(
        uiState = uiState,
        onUserClick = { user -> viewModel.onUserClicked(user) },
        onRefresh = { viewModel.loadData() }
    )
}

// 这是一个“纯” UI Composable，它完全不依赖 ViewModel，方便预览和复用
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onUserClick: (User) -> Unit, // 事件回调
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("状态消息: ${uiState.message}", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onRefresh) { Text("刷新数据") }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn {
                items(uiState.userList) { user ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onUserClick(user) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(user.name, style = MaterialTheme.typography.bodyLarge)
                            Text(user.bio, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
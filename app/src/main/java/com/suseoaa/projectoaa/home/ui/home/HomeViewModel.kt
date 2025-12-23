package com.suseoaa.projectoaa.home.ui.home


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.home.data.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 定义 UI 状态：首页长什么样，由这个类决定
data class HomeUiState(
    val isLoading: Boolean = false,
    val userList: List<User> = emptyList(),
    val message: String = "" // 用于显示欢迎语等
)

class HomeViewModel : ViewModel() {
    // 1. 内部可变状态 (MutableStateFlow)
    private val _uiState = MutableStateFlow(HomeUiState())
    // 2. 暴露给 UI 的只读状态 (StateFlow)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    // 模拟从网络加载数据
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 模拟获取到的数据
            val mockUsers = List(10) { index ->
                User(index, "用户 $index", "这是第 $index 个用户的个人简介")
            }

            _uiState.value = HomeUiState(
                isLoading = false,
                userList = mockUsers,
                message = "加载完成！"
            )
        }
    }

    // 处理用户点击事件
    fun onUserClicked(user: User) {
        _uiState.value = _uiState.value.copy(message = "你点击了: ${user.name}")
    }
}
package com.suseoaa.projectoaa.feature.register

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.network.model.register.RegisterRequest
import com.suseoaa.projectoaa.core.network.register.RegisterClient
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {
    var studentID by mutableStateOf("")
    var realName by mutableStateOf("")
    var userName by mutableStateOf("")
    var password by mutableStateOf("")
    var role by mutableStateOf("")
    var isLoading by mutableStateOf(false)

    fun register(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (listOf(studentID, realName, userName, password, role).any { it.isBlank() }) {
            onError("请填写所有选项")
            return
        }
        viewModelScope.launch {
            isLoading = true
            try {
                val request = RegisterRequest(
                    studentId = studentID,
                    name = realName,
                    username = userName,
                    password = password,
                    role = role
                )
                val response = RegisterClient.apiService.register(request)
                if (response.code == "200") {
                    onSuccess(response.message)
                }
            } catch (e: Exception) {
                onError("${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
}
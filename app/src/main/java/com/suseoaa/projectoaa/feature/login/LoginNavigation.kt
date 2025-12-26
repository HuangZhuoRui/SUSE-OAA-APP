package com.suseoaa.projectoaa.feature.login

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val LOGIN_ROUTE = "login_route"

fun NavController.navigateToLogin(navOptions: NavOptions? = null) {
    this.navigate(LOGIN_ROUTE, navOptions)
}

//注册登录页
fun NavGraphBuilder.loginScreen(
    onLoginSuccess: () -> Unit
) {
    composable(route = LOGIN_ROUTE) {
        LoginScreen(onLoginSuccess = onLoginSuccess)
    }
}
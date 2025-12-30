package com.suseoaa.projectoaa.app

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.suseoaa.projectoaa.feature.home.MAIN_SCREEN_ROUTE
import com.suseoaa.projectoaa.feature.home.MainScreen
import com.suseoaa.projectoaa.feature.login.LOGIN_ROUTE
import com.suseoaa.projectoaa.feature.login.loginScreen
import com.suseoaa.projectoaa.feature.register.navigateToRegister
import com.suseoaa.projectoaa.feature.register.registerScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    windowSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        // 这里根据你的需求，如果是已登录状态，可以设为 MAIN_SCREEN_ROUTE
        // 如果需要先登录，设为 LOGIN_ROUTE
        startDestination = LOGIN_ROUTE,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        // 1. 登录页
        loginScreen(
            onLoginSuccess = {
                // 登录成功 -> 跳转到主容器页
                navController.navigate(MAIN_SCREEN_ROUTE) {
                    popUpTo(LOGIN_ROUTE) { inclusive = true }
                }
            },
            onNavigateToRegister = {
                navController.navigateToRegister()
            }
        )

        // 2. 注册页
        registerScreen(
            onRegisterSuccess = {
                navController.popBackStack()
            }
        )

        // 3. 主容器页 (包含 4 个 Tab 的 Pager)
        composable(MAIN_SCREEN_ROUTE) {
            MainScreen(windowSizeClass = windowSizeClass)
        }
    }
}
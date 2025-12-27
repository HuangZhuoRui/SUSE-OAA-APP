package com.suseoaa.projectoaa.app

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.suseoaa.projectoaa.feature.chat.chatScreen
import com.suseoaa.projectoaa.feature.course.courseScreen
import com.suseoaa.projectoaa.feature.home.HOME_ROUTE
import com.suseoaa.projectoaa.feature.home.homeScreen
import com.suseoaa.projectoaa.feature.login.LOGIN_ROUTE
import com.suseoaa.projectoaa.feature.login.loginScreen
import com.suseoaa.projectoaa.feature.person.personScreen
import com.suseoaa.projectoaa.feature.register.navigateToRegister
import com.suseoaa.projectoaa.feature.register.registerScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
//        此处临时改动，测试完后要改回LOGIN_ROUTE
        startDestination = HOME_ROUTE,
        modifier = modifier,
//        修改动画，将原有的渐变动画关闭
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        loginScreen(
            onLoginSuccess = {
                // 登录成功的跳转逻辑：
                // 跳转到首页，并且把“登录页”从返回栈里弹出去
                navController.navigate(HOME_ROUTE) {
                    popUpTo(LOGIN_ROUTE) { inclusive = true }
                }
            },
            onNavigateToRegister = {
                navController.navigateToRegister()
            }
        )
        // 注册首页
        homeScreen()

        // 注册个人页
        personScreen()

//        注册课程页
        courseScreen()
//        注册协会日记页
        chatScreen()
//        注册注册页
        registerScreen(
            onRegisterSuccess = {
                navController.popBackStack()
            },
            onBackClick = {
                navController.popBackStack()
            }
        )
    }
}
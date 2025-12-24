package com.suseoaa.projectoaa.feature.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

// 定义路由名称
const val HOME_ROUTE = "home_route"
const val PERSON_ROUTE = "my_route"
const val COURSE_ROUTE = "course_route"
const val CHAT_ROUTE = "course_route"

// 扩展函数：供外部调用“跳转到首页”
fun NavController.navigateToHome(navOptions: NavOptions? = null) {
    this.navigate(HOME_ROUTE, navOptions)
}

// 扩展函数：供 NavHost 调用“注册首页”
fun NavGraphBuilder.homeScreen(
    onNavigateToMy: () -> Unit
) {
    composable(route = HOME_ROUTE) {
        HomeScreen(onMyClick = onNavigateToMy)
    }
}
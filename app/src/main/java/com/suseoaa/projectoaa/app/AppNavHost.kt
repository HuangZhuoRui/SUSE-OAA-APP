package com.suseoaa.projectoaa.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.suseoaa.projectoaa.feature.home.HOME_ROUTE
import com.suseoaa.projectoaa.feature.home.homeScreen
import com.suseoaa.projectoaa.feature.my.MyScreen
import com.suseoaa.projectoaa.feature.my.navigateToMy

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HOME_ROUTE,
        modifier = modifier
    ) {
        // 1. 注册首页
        homeScreen(
            onNavigateToMy = {
                navController.navigateToMy()
            }
        )

        // 2. 注册设置页
        MyScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )
    }
}